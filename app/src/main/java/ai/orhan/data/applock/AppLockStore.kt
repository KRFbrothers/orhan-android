package ai.orhan.data.applock

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/*
 * App lock (PIN) persistence. The PIN itself is never stored: only a random
 * salt and PBKDF2-HMAC-SHA256(pin, salt), both hex. No master PIN, no bypass.
 *
 * Format v2 (PBKDF2 + lockout counters). Older v1 data (single SHA-256) is NOT
 * migrated: it reads as "lock off, no PIN" so the user re-enables with a new hash.
 */

private const val APP_LOCK_DATASTORE_FILE = "orhan_app_lock.pb"
internal const val APP_LOCK_FORMAT_VERSION = 2

data class AppLockPrefs(
    val enabled: Boolean = false,
    val saltHex: String = "",
    val hashHex: String = "",
    val failedAttempts: Int = 0,
    val lockoutUntilEpochMs: Long = 0L,
) {
    val hasPin: Boolean get() = saltHex.isNotEmpty() && hashHex.isNotEmpty()
}

/** Pure helpers — no Android, unit-testable. */
object PinHash {
    const val MIN_LEN = 4
    const val MAX_LEN = 6
    const val ITERATIONS = 120_000
    const val KEY_BITS = 256

    fun isValidPin(pin: String): Boolean = pin.length in MIN_LEN..MAX_LEN && pin.all { it in '0'..'9' }

    fun newSaltHex(random: SecureRandom = SecureRandom()): String =
        ByteArray(16).also(random::nextBytes).toHex()

    fun hashPin(saltHex: String, pin: String, iterations: Int = ITERATIONS): String {
        val spec = PBEKeySpec(pin.toCharArray(), saltHex.hexToBytes(), iterations, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded.toHex()
        } finally {
            spec.clearPassword()
        }
    }

    fun verifyPin(saltHex: String, hashHex: String, pin: String): Boolean {
        if (saltHex.isEmpty() || hashHex.isEmpty() || !isValidPin(pin)) return false
        return MessageDigest.isEqual(
            hashPin(saltHex, pin).toByteArray(Charsets.US_ASCII),
            hashHex.toByteArray(Charsets.US_ASCII),
        )
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
    private fun String.hexToBytes(): ByteArray =
        if (length % 2 == 0 && all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' })
            chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        else toByteArray(Charsets.UTF_8)
}

object AppLockPrefsSerializer : Serializer<AppLockPrefs> {
    override val defaultValue: AppLockPrefs = AppLockPrefs()

    override suspend fun readFrom(input: InputStream): AppLockPrefs {
        val bytes = try {
            input.readBytes()
        } catch (e: IOException) {
            throw CorruptionException("Cannot read app lock prefs", e)
        }
        if (bytes.isEmpty()) return defaultValue
        return try {
            DataInputStream(bytes.inputStream()).use { d ->
                // v1 (weak SHA-256) or unknown → treat as no lock; user must set a new PIN.
                if (d.readInt() != APP_LOCK_FORMAT_VERSION) return defaultValue
                AppLockPrefs(
                    enabled = d.readBoolean(),
                    saltHex = d.readUTF(),
                    hashHex = d.readUTF(),
                    failedAttempts = d.readInt(),
                    lockoutUntilEpochMs = d.readLong(),
                )
            }
        } catch (e: IOException) {
            defaultValue
        }
    }

    override suspend fun writeTo(t: AppLockPrefs, output: OutputStream) {
        val d = DataOutputStream(output)
        d.writeInt(APP_LOCK_FORMAT_VERSION)
        d.writeBoolean(t.enabled)
        d.writeUTF(t.saltHex)
        d.writeUTF(t.hashHex)
        d.writeInt(t.failedAttempts)
        d.writeLong(t.lockoutUntilEpochMs)
        d.flush()
    }
}

val Context.appLockDataStore: DataStore<AppLockPrefs> by dataStore(
    fileName = APP_LOCK_DATASTORE_FILE,
    serializer = AppLockPrefsSerializer,
)

sealed interface PinAttempt {
    data object Success : PinAttempt
    data class Wrong(val attemptsLeft: Int) : PinAttempt
    data class LockedOut(val remainingMs: Long) : PinAttempt
}

class AppLockStore(
    private val dataStore: DataStore<AppLockPrefs>,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    companion object {
        const val MAX_ATTEMPTS = 5
        const val LOCKOUT_MS = 30_000L
    }

    private val prefs: Flow<AppLockPrefs> = dataStore.data
        .catch { e -> if (e is IOException) emit(AppLockPrefs()) else throw e }

    /** True only when enabled AND a PIN hash exists. */
    val isEnabled: Flow<Boolean> = prefs.map { it.enabled && it.hasPin }

    suspend fun hasPin(): Boolean = prefs.first().hasPin

    /** Remaining lockout in ms (0 when attempts are allowed). */
    suspend fun lockoutRemainingMs(): Long = (prefs.first().lockoutUntilEpochMs - clock()).coerceAtLeast(0L)

    /** Sets a new PIN and turns the lock on. Throws on invalid PIN. */
    suspend fun setPin(pin: String) {
        require(PinHash.isValidPin(pin)) { "PIN must be 4–6 digits" }
        val salt = PinHash.newSaltHex()
        val hash = withContext(Dispatchers.Default) { PinHash.hashPin(salt, pin) }
        dataStore.updateData { AppLockPrefs(enabled = true, saltHex = salt, hashHex = hash) }
    }

    /**
     * Checks [pin] with persisted rate limiting: after [MAX_ATTEMPTS] wrong PINs, all attempts
     * are refused for [LOCKOUT_MS]. Success resets the counter.
     */
    suspend fun attempt(pin: String): PinAttempt {
        val p = prefs.first()
        val now = clock()
        if (p.lockoutUntilEpochMs > now) return PinAttempt.LockedOut(p.lockoutUntilEpochMs - now)
        val ok = withContext(Dispatchers.Default) { PinHash.verifyPin(p.saltHex, p.hashHex, pin) }
        if (ok) {
            dataStore.updateData { it.copy(failedAttempts = 0, lockoutUntilEpochMs = 0L) }
            return PinAttempt.Success
        }
        val updated = dataStore.updateData {
            val fails = it.failedAttempts + 1
            if (fails >= MAX_ATTEMPTS) it.copy(failedAttempts = 0, lockoutUntilEpochMs = clock() + LOCKOUT_MS)
            else it.copy(failedAttempts = fails)
        }
        return if (updated.lockoutUntilEpochMs > now) PinAttempt.LockedOut(updated.lockoutUntilEpochMs - clock())
        else PinAttempt.Wrong(MAX_ATTEMPTS - updated.failedAttempts)
    }

    suspend fun verify(pin: String): Boolean = attempt(pin) == PinAttempt.Success

    /** Turns the lock off and clears the hash + salt. */
    suspend fun disable() {
        dataStore.updateData { AppLockPrefs() }
    }

    /** Returns false (and changes nothing) if [old] is wrong or [new] invalid. */
    suspend fun changePin(old: String, new: String): Boolean {
        if (!PinHash.isValidPin(new) || !verify(old)) return false
        setPin(new)
        return true
    }
}

fun Context.appLockStore(): AppLockStore = AppLockStore(appLockDataStore)
