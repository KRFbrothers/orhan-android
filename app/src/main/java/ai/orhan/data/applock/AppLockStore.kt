package ai.orhan.data.applock

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.security.SecureRandom

/*
 * App lock (PIN) persistence. The PIN itself is never stored: only a random
 * salt and SHA-256(salt + pin), both hex. No master PIN, no bypass.
 */

private const val APP_LOCK_DATASTORE_FILE = "orhan_app_lock.pb"
private const val APP_LOCK_FORMAT_VERSION = 1

data class AppLockPrefs(
    val enabled: Boolean = false,
    val saltHex: String = "",
    val hashHex: String = "",
) {
    val hasPin: Boolean get() = saltHex.isNotEmpty() && hashHex.isNotEmpty()
}

/** Pure helpers — no Android, unit-testable. */
object PinHash {
    const val MIN_LEN = 4
    const val MAX_LEN = 6

    fun isValidPin(pin: String): Boolean = pin.length in MIN_LEN..MAX_LEN && pin.all { it in '0'..'9' }

    fun newSaltHex(random: SecureRandom = SecureRandom()): String =
        ByteArray(16).also(random::nextBytes).toHex()

    fun hashPin(saltHex: String, pin: String): String =
        MessageDigest.getInstance("SHA-256").digest((saltHex + pin).toByteArray(Charsets.UTF_8)).toHex()

    fun verifyPin(saltHex: String, hashHex: String, pin: String): Boolean {
        if (saltHex.isEmpty() || hashHex.isEmpty() || !isValidPin(pin)) return false
        return MessageDigest.isEqual(
            hashPin(saltHex, pin).toByteArray(Charsets.US_ASCII),
            hashHex.toByteArray(Charsets.US_ASCII),
        )
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
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
                if (d.readInt() != APP_LOCK_FORMAT_VERSION) return defaultValue
                AppLockPrefs(enabled = d.readBoolean(), saltHex = d.readUTF(), hashHex = d.readUTF())
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
        d.flush()
    }
}

val Context.appLockDataStore: DataStore<AppLockPrefs> by dataStore(
    fileName = APP_LOCK_DATASTORE_FILE,
    serializer = AppLockPrefsSerializer,
)

class AppLockStore(private val dataStore: DataStore<AppLockPrefs>) {
    private val prefs: Flow<AppLockPrefs> = dataStore.data
        .catch { e -> if (e is IOException) emit(AppLockPrefs()) else throw e }

    /** True only when enabled AND a PIN hash exists. */
    val isEnabled: Flow<Boolean> = prefs.map { it.enabled && it.hasPin }

    suspend fun hasPin(): Boolean = prefs.first().hasPin

    /** Sets a new PIN and turns the lock on. Throws on invalid PIN. */
    suspend fun setPin(pin: String) {
        require(PinHash.isValidPin(pin)) { "PIN must be 4–6 digits" }
        val salt = PinHash.newSaltHex()
        val hash = PinHash.hashPin(salt, pin)
        dataStore.updateData { AppLockPrefs(enabled = true, saltHex = salt, hashHex = hash) }
    }

    suspend fun verify(pin: String): Boolean {
        val p = prefs.first()
        return PinHash.verifyPin(p.saltHex, p.hashHex, pin)
    }

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
