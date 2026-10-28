package ai.orhan.data.auth

import ai.orhan.BuildConfig
import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
class AuthRepository private constructor(
    private val context: Context,
    private val supabaseUrl: String = BuildConfig.SUPABASE_URL,
    private val anonKey: String = BuildConfig.SUPABASE_ANON_KEY,
) {
    private val mutex = Mutex()

    val isConfigured: Boolean get() = supabaseUrl.isNotBlank() && anonKey.isNotBlank()

    val session: Flow<AuthSession?> = context.authDataStore.data.map { it.toSession() }

    suspend fun signIn(email: String, password: String): Result<AuthSession> = withContext(Dispatchers.IO) {
        runCatching {
            check(isConfigured) { "SUPABASE_URL / SUPABASE_ANON_KEY missing in local.properties" }
            val json = post("/auth/v1/token?grant_type=password",
                JSONObject().put("email", email.trim()).put("password", password))
            parseSession(json).also { save(it) }
        }
    }

    /** Returns a valid access token, refreshing if needed; null when signed out or refresh fails. */
    suspend fun accessToken(): String? = mutex.withLock {
        val current = session.first() ?: return@withLock null
        val now = System.currentTimeMillis() / 1000
        if (current.expiresAt - now > 60) return@withLock current.accessToken
        withContext(Dispatchers.IO) {
            runCatching {
                val json = post("/auth/v1/token?grant_type=refresh_token",
                    JSONObject().put("refresh_token", current.refreshToken))
                parseSession(json).also { save(it) }.accessToken
            }.getOrElse { e ->
                if (e is AuthHttpException && e.code in 400..499) clear()
                null
            }
        }
    }

    suspend fun signOut() {
        val token = session.first()?.accessToken
        clear()
        if (token != null && isConfigured) withContext(Dispatchers.IO) {
            runCatching { post("/auth/v1/logout", JSONObject(), bearer = token) }
        }
    }

    /** Drop the local session (e.g. after a 401 from the engine). */
    suspend fun clear() {
        context.authDataStore.edit { it.clear() }
    }

    private suspend fun save(s: AuthSession) {
        context.authDataStore.edit {
            it[KEY_ACCESS] = s.accessToken
            it[KEY_REFRESH] = s.refreshToken
            it[KEY_EXPIRES] = s.expiresAt
            if (s.email != null) it[KEY_EMAIL] = s.email else it.remove(KEY_EMAIL)
        }
    }

    private fun parseSession(json: JSONObject): AuthSession {
        val access = json.optString("access_token").ifBlank { error("No access_token in response") }
        val refresh = json.optString("refresh_token").ifBlank { error("No refresh_token in response") }
        val now = System.currentTimeMillis() / 1000
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

    private fun Preferences.toSession(): AuthSession? {
        val access = this[KEY_ACCESS] ?: return null
        val refresh = this[KEY_REFRESH] ?: return null
        return AuthSession(access, refresh, this[KEY_EXPIRES] ?: 0L, this[KEY_EMAIL])
    }

    class AuthHttpException(val code: Int, message: String) : Exception(message)

    companion object {
        private val KEY_ACCESS = stringPreferencesKey("access_token")
        private val KEY_REFRESH = stringPreferencesKey("refresh_token")
        private val KEY_EXPIRES = longPreferencesKey("expires_at")
        private val KEY_EMAIL = stringPreferencesKey("email")

        @Volatile private var instance: AuthRepository? = null
        fun get(context: Context): AuthRepository =
            instance ?: synchronized(this) {
                instance ?: AuthRepository(context.applicationContext).also { instance = it }
            }
    }
}
