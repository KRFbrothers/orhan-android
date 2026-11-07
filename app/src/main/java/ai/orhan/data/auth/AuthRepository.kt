package ai.orhan.data.auth

import ai.orhan.BuildConfig
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** A Supabase Auth session. [expiresAt] is epoch seconds. */
data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long,
    val email: String?,
)

private val Context.authDataStore by preferencesDataStore(name = "orhan_auth")

/**
 * Supabase email/password sign-in via the GoTrue REST API
 * (`{SUPABASE_URL}/auth/v1/token`), using only the public anon key.
 * No SDK dependency: HttpURLConnection + org.json, like ChatRepository.
 *
 * The session (access + refresh token) is kept in app-private DataStore.
 * [accessToken] refreshes automatically when the JWT is near expiry.
 */
class AuthRepository internal constructor(
    private val store: DataStore<Preferences>,
    private val supabaseUrl: String = BuildConfig.SUPABASE_URL,
    private val anonKey: String = BuildConfig.SUPABASE_ANON_KEY,
    private val crypto: TokenCrypto = KeystoreTokenCrypto(),
    private val requestOverride: (suspend (String, JSONObject, String?) -> JSONObject)? = null,
    private val nowSeconds: () -> Long = { System.currentTimeMillis() / 1000 },
) {
    // Network refreshes are de-duplicated separately; logout never waits for this mutex.
    private val refreshMutex = Mutex()
    private val mutationMutex = Mutex()
    private var generation = 0L
    private var loaded = false
    private val current = MutableStateFlow<AuthSession?>(null)

    val isConfigured: Boolean get() = supabaseUrl.isNotBlank() && anonKey.isNotBlank()

    // Publish only decrypted, successfully migrated/persisted sessions, not raw store emissions.
    val session: Flow<AuthSession?> = flow {
        initialize()
        emitAll(current)
    }

    private suspend fun initialize() = mutationMutex.withLock {
        if (loaded) return@withLock
        try {
            val prefs = store.data.first()
            val restored = withContext(Dispatchers.IO) { prefs.toSession() }
            if (restored == null) {
                if (prefs[KEY_ACCESS] != null || prefs[KEY_REFRESH] != null || prefs[KEY_EXPIRES] != null || prefs[KEY_EMAIL] != null) removeStoredSession()
            } else if (!TokenFormat.isEncrypted(prefs[KEY_ACCESS].orEmpty()) ||
                !TokenFormat.isEncrypted(prefs[KEY_REFRESH].orEmpty())) {
                // Legacy plaintext is never trusted by clients before BOTH tokens are resealed.
                persistAndPublish(restored)
            } else {
                current.value = restored
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            // Lost key, unsafe legacy migration or unreadable storage: require sign-in, no crash.
            current.value = null
            try { removeStoredSession() } catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { /* Keep unpublished if storage itself is unavailable. */ }
        }
        loaded = true
    }

    suspend fun signIn(email: String, password: String): Result<AuthSession> {
        initialize()
        val attempt = mutationMutex.withLock { ++generation }
        return runCatching {
            check(isConfigured) { "SUPABASE_URL / SUPABASE_ANON_KEY missing in local.properties" }
            val json = request("/auth/v1/token?grant_type=password",
                JSONObject().put("email", email.trim()).put("password", password))
            val next = parseSession(json)
            mutationMutex.withLock {
                check(generation == attempt) { "The session changed. Please sign in again." }
                persistAndPublish(next)
            }
            next
        }.onFailure { if (it is CancellationException) throw it }
    }

    /** Returns a trusted token; refresh I/O never blocks logout or a newer sign-in. */
    suspend fun accessToken(): String? {
        initialize()
        // A previous account's refresh must not delay signed-out or already-fresh callers.
        val cached = mutationMutex.withLock { current.value } ?: return null
        if (cached.expiresAt - nowSeconds() > 60) return cached.accessToken
        return refreshMutex.withLock refresh@{
            val snapshot = mutationMutex.withLock {
                current.value?.let { AuthRequestToken(it.accessToken, generation) to it }
            } ?: return@refresh null
            val (lease, saved) = snapshot
            if (saved.expiresAt - nowSeconds() > 60) return@refresh saved.accessToken
            val result = runCatching {
                parseSession(request("/auth/v1/token?grant_type=refresh_token",
                    JSONObject().put("refresh_token", saved.refreshToken)))
            }
            val failure = result.exceptionOrNull()
            if (failure is CancellationException) throw failure
            mutationMutex.withLock mutation@{
                if (!matches(lease)) return@mutation null
                if (failure != null) {
                    // Transient failures preserve the safe prior encrypted session.
                    if (failure is AuthHttpException && failure.code in listOf(400, 401, 403)) clearLocked()
                    null
                } else {
                    val next = result.getOrThrow()
                    try {
                        persistAndPublish(next)
                        next.accessToken
                    } catch (cancel: CancellationException) { throw cancel }
                    catch (_: Exception) {
                        // Refresh may have rotated the old token: do not publish an unsaved pair.
                        try { clearLocked() } catch (cancel: CancellationException) { throw cancel }
                        catch (_: Exception) { current.value = null }
                        null
                    }
                }
            }
        }
    }

    /** Request-scoped lease: an old 401 must not clear a refreshed/newer session, even for equal JWTs. */
    suspend fun requestToken(): AuthRequestToken? {
        val token = accessToken() ?: return null
        return mutationMutex.withLock {
            current.value?.takeIf { it.accessToken == token }?.let { AuthRequestToken(token, generation) }
        }
    }

    suspend fun clearIfCurrent(token: AuthRequestToken) {
        initialize()
        mutationMutex.withLock { if (matches(token)) clearLocked() }
    }

    private fun matches(token: AuthRequestToken) =
        generation == token.generation && current.value?.accessToken == token.accessToken

    suspend fun signOut() {
        initialize()
        val token = mutationMutex.withLock {
            val token = current.value?.accessToken
            clearLocked() // Invalidate in-flight sign-in/refresh BEFORE any logout network call.
            token
        }
        if (token != null && isConfigured) {
            runCatching { request("/auth/v1/logout", JSONObject(), bearer = token) }
                .onFailure { if (it is CancellationException) throw it }
        }
    }

    /** Explicit local sign-out only; request failures use [clearIfCurrent]. */
    suspend fun clear() {
        initialize()
        mutationMutex.withLock { clearLocked() }
    }

    private suspend fun clearLocked() {
        ++generation
        // On a storage failure, explicit logout fails honestly and can be retried from Settings.
        withContext(NonCancellable) {
            removeStoredSession()
            current.value = null
        }
    }

    private suspend fun removeStoredSession() {
        try {
            store.edit { it.remove(KEY_ACCESS); it.remove(KEY_REFRESH); it.remove(KEY_EXPIRES); it.remove(KEY_EMAIL) }
        } catch (cancel: CancellationException) { throw cancel }
        catch (error: Exception) { throw AuthStorageException(error) }
    }

    private suspend fun persistAndPublish(s: AuthSession) = withContext(Dispatchers.IO) {
        try {
            // Prepare BOTH ciphertexts before a single atomic DataStore update/publication.
            val access = sealToken(crypto, s.accessToken)
            val refresh = sealToken(crypto, s.refreshToken)
            currentCoroutineContext().ensureActive()
            // Finish a committed disk update and its publication together, even if the caller cancels.
            // Only local storage is non-cancellable; never the network or token encryption.
            withContext(NonCancellable) {
                store.edit {
                    it[KEY_ACCESS] = access
                    it[KEY_REFRESH] = refresh
                    it[KEY_EXPIRES] = s.expiresAt
                    if (s.email != null) it[KEY_EMAIL] = s.email else it.remove(KEY_EMAIL)
                }
                ++generation
                current.value = s
            }
        } catch (cancel: CancellationException) { throw cancel }
        catch (error: Exception) { throw AuthStorageException(error) }
    }

    private suspend fun request(path: String, body: JSONObject, bearer: String? = null): JSONObject =
        requestOverride?.invoke(path, body, bearer) ?: withContext(Dispatchers.IO) { post(path, body, bearer) }

    private fun parseSession(json: JSONObject): AuthSession {
        val access = json.optString("access_token").ifBlank { error("No access_token in response") }
        val refresh = json.optString("refresh_token").ifBlank { error("No refresh_token in response") }
        val now = nowSeconds()
        val expiresAt = json.optLong("expires_at").takeIf { it > 0 } ?: (now + json.optLong("expires_in", 3600))
        val email = json.optJSONObject("user")?.optString("email")?.ifBlank { null }
        return AuthSession(access, refresh, expiresAt, email)
    }

    private fun post(path: String, body: JSONObject, bearer: String? = null): JSONObject {
        val conn = (URL(supabaseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 30_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("apikey", anonKey)
            setRequestProperty("Authorization", "Bearer ${bearer ?: anonKey}")
        }
        try {
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = conn.responseCode
            val raw = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = runCatching { JSONObject(raw) }.getOrNull() ?: JSONObject()
            if (code !in 200..299) {
                val msg = listOf("error_description", "msg", "message", "error")
                    .firstNotNullOfOrNull { json.optString(it).ifBlank { null } } ?: "HTTP $code"
                throw AuthHttpException(code, msg)
            }
            return json
        } finally {
            conn.disconnect()
        }
    }

    private fun Preferences.toSession(): AuthSession? = sessionFromStored(
        crypto, this[KEY_ACCESS], this[KEY_REFRESH], this[KEY_EXPIRES], this[KEY_EMAIL],
    )

    class AuthHttpException(val code: Int, message: String) : Exception(message)

    companion object {
        private val KEY_ACCESS = stringPreferencesKey("access_token")
        private val KEY_REFRESH = stringPreferencesKey("refresh_token")
        private val KEY_EXPIRES = longPreferencesKey("expires_at")
        private val KEY_EMAIL = stringPreferencesKey("email")

        @Volatile private var instance: AuthRepository? = null
        fun get(context: Context): AuthRepository =
            instance ?: synchronized(this) {
                instance ?: AuthRepository(context.applicationContext.authDataStore).also { instance = it }
            }
    }
}

/** Fail closed: an encryption failure must never turn into a plaintext write. */
internal fun sealToken(crypto: TokenCrypto, plain: String): String = crypto.encrypt(plain).also {
    check(TokenFormat.isEncrypted(it) && TokenFormat.decode(it) != null) { "Secure token encryption failed" }
}

/** An opaque generation-bound request handle; never persist or log it. */
class AuthRequestToken internal constructor(val accessToken: String, internal val generation: Long)

class AuthStorageException(cause: Throwable) : java.io.IOException(
    "Secure sign-in storage is unavailable. Please try signing in or signing out again.", cause,
)

/**
 * Decode for initialization only. Legacy plaintext MUST be resealed by the repository before
 * trusted publication. Undecryptable tokens yield null and require sign-in again.
 */
internal fun sessionFromStored(
    crypto: TokenCrypto, access: String?, refresh: String?, expiresAt: Long?, email: String?,
): AuthSession? {
    val a = access?.let { runCatching { crypto.decrypt(it) }.getOrNull() }?.takeIf { it.isNotBlank() } ?: return null
    val r = refresh?.let { runCatching { crypto.decrypt(it) }.getOrNull() }?.takeIf { it.isNotBlank() } ?: return null
    return AuthSession(a, r, expiresAt ?: 0L, email)
}
