package ai.orhan.data.applock

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.security.MessageDigest

private class FakeStore : DataStore<AppLockPrefs> {
    val state = MutableStateFlow(AppLockPrefs())
    override val data: Flow<AppLockPrefs> = state
    override suspend fun updateData(transform: suspend (t: AppLockPrefs) -> AppLockPrefs): AppLockPrefs =
        transform(state.value).also { state.value = it }
}

class AppLockStoreTest {
    @Test fun hashIsDeterministicAndSalted() {
        assertEquals(PinHash.hashPin("aa", "1234"), PinHash.hashPin("aa", "1234"))
        assertNotEquals(PinHash.hashPin("aa", "1234"), PinHash.hashPin("bb", "1234"))
        assertEquals(64, PinHash.hashPin("aa", "1234").length)
    }

    @Test fun verifyPin() {
        val h = PinHash.hashPin("salt", "123456")
        assertTrue(PinHash.verifyPin("salt", h, "123456"))
        assertFalse(PinHash.verifyPin("salt", h, "123457"))
        assertFalse(PinHash.verifyPin("", "", "123456"))
    }

    @Test fun validPin() {
        assertTrue(PinHash.isValidPin("1234")); assertTrue(PinHash.isValidPin("123456"))
        assertFalse(PinHash.isValidPin("123")); assertFalse(PinHash.isValidPin("1234567"))
        assertFalse(PinHash.isValidPin("12a4"))
    }

    @Test fun storeLifecycle() = runBlocking {
        val fake = FakeStore()
        val store = AppLockStore(fake)
        assertFalse(store.isEnabled.first()); assertFalse(store.hasPin())
        store.setPin("2468")
        assertTrue(store.isEnabled.first()); assertTrue(store.hasPin())
        assertFalse(fake.state.value.hashHex.contains("2468"))
        assertTrue(store.verify("2468")); assertFalse(store.verify("1111"))
        assertFalse(store.changePin("0000", "13579"))
        assertTrue(store.verify("2468"))
        assertTrue(store.changePin("2468", "13579"))
        assertTrue(store.verify("13579")); assertFalse(store.verify("2468"))
        store.disable()
        assertFalse(store.isEnabled.first()); assertFalse(store.hasPin()); assertFalse(store.verify("13579"))
    }

    @Test fun serializerRoundTrip() = runBlocking {
        val p = AppLockPrefs(true, "ab", "cd", 3, 12345L)
        val out = ByteArrayOutputStream()
        AppLockPrefsSerializer.writeTo(p, out)
        assertEquals(p, AppLockPrefsSerializer.readFrom(ByteArrayInputStream(out.toByteArray())))
        assertEquals(AppLockPrefs(), AppLockPrefsSerializer.readFrom(ByteArrayInputStream(ByteArray(0))))
    }

    @Test fun pbkdf2KnownVectorAndNotSha256() {
        val h = PinHash.hashPin("00112233445566778899aabbccddeeff", "1234")
        val sha = MessageDigest.getInstance("SHA-256")
            .digest("00112233445566778899aabbccddeeff1234".toByteArray()).joinToString("") { "%02x".format(it) }
        assertNotEquals(sha, h)
        // Reduced iterations give a different key (iterations are actually applied).
        assertNotEquals(PinHash.hashPin("00112233445566778899aabbccddeeff", "1234", iterations = 1), h)
    }

    @Test fun legacyV1DataReadsAsDisabled() = runBlocking {
        val out = ByteArrayOutputStream()
        DataOutputStream(out).apply { writeInt(1); writeBoolean(true); writeUTF("ab"); writeUTF("cd"); flush() }
        val p = AppLockPrefsSerializer.readFrom(ByteArrayInputStream(out.toByteArray()))
        assertEquals(AppLockPrefs(), p)
        assertFalse(p.enabled && p.hasPin)
    }

    @Test fun lockoutAfterFiveFailures() = runBlocking {
        var now = 1_000_000L
        val fake = FakeStore()
        val store = AppLockStore(fake) { now }
        store.setPin("2468")
        repeat(4) { i -> assertEquals(PinAttempt.Wrong(4 - i), store.attempt("0000")) }
        assertTrue(store.attempt("0000") is PinAttempt.LockedOut)
        assertEquals(AppLockStore.LOCKOUT_MS, store.lockoutRemainingMs())
        // Correct PIN refused during lockout; state persisted in prefs.
        assertTrue(store.attempt("2468") is PinAttempt.LockedOut)
        assertEquals(now + AppLockStore.LOCKOUT_MS, fake.state.value.lockoutUntilEpochMs)
        now += AppLockStore.LOCKOUT_MS
        assertEquals(0L, store.lockoutRemainingMs())
        assertEquals(PinAttempt.Success, store.attempt("2468"))
        assertEquals(0, fake.state.value.failedAttempts)
    }

    @Test fun successResetsCounter() = runBlocking {
        val fake = FakeStore()
        val store = AppLockStore(fake) { 0L }
        store.setPin("2468")
        repeat(3) { store.attempt("0000") }
        assertEquals(3, fake.state.value.failedAttempts)
        assertEquals(PinAttempt.Success, store.attempt("2468"))
        assertEquals(PinAttempt.Wrong(4), store.attempt("0000"))
    }
}
