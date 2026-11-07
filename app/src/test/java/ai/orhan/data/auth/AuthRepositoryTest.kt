package ai.orhan.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import ai.orhan.data.chat.chatCredential
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class AuthRepositoryTest {
    private val access = stringPreferencesKey("access_token")
    private val refresh = stringPreferencesKey("refresh_token")
    private val expires = longPreferencesKey("expires_at")
    private val email = stringPreferencesKey("email")
    private val unrelated = stringPreferencesKey("unrelated")

    private fun json(a: String = "access-new", r: String = "refresh-new", until: Long = 5_000, who: String = "new@test.invalid") =
        JSONObject().put("access_token", a).put("refresh_token", r).put("expires_at", until)
            .put("user", JSONObject().put("email", who))

    private fun prefs(crypto: TestCrypto, legacy: Boolean = false, until: Long = 5_000, a: String = "access-old"): Preferences =
        emptyPreferences().toMutablePreferences().apply {
            this[access] = if (legacy) a else sealToken(crypto, a)
            this[refresh] = if (legacy) "refresh-old" else sealToken(crypto, "refresh-old")
            this[expires] = until
            this[email] = "old@test.invalid"
            this[unrelated] = "keep"
        }

    private fun repo(store: TestStore, crypto: TestCrypto = TestCrypto(), clock: () -> Long = { 1_000 },
                     request: suspend (String, JSONObject, String?) -> JSONObject = { _, _, _ -> json() }) =
        AuthRepository(store, "https://auth.test.invalid", "public-test-key", crypto, request, clock)

    private fun assertEncryptedWrites(store: TestStore) {
        store.writes.forEach { write ->
            write[access]?.let { assertTrue(TokenFormat.isEncrypted(it)) }
            write[refresh]?.let { assertTrue(TokenFormat.isEncrypted(it)) }
            assertEquals(write[access] == null, write[refresh] == null)
        }
    }

    @Test fun signInEncryptsBothTokensAtomicallyAndReloadsExistingFormat() = runTest {
        val store = TestStore()
        val auth = repo(store)
        val result = auth.signIn("new@test.invalid", "synthetic-password").getOrThrow()
        assertEquals(result, auth.session.first())
        assertEquals(result, repo(store).session.first())
        assertEquals(1, store.writes.size)
        assertEncryptedWrites(store)
    }

    @Test fun accessEncryptionFailureNeverPersistsOrPublishesSession() = runTest {
        val store = TestStore()
        val auth = repo(store, TestCrypto(failPlain = "access-new"))
        val result = auth.signIn("new@test.invalid", "synthetic-password")
        assertTrue(result.exceptionOrNull() is AuthStorageException)
        assertNull(auth.session.first())
        assertTrue(store.writes.isEmpty())
    }

    @Test fun refreshEncryptionFailureCannotPersistAHalfSession() = runTest {
        val store = TestStore()
        val crypto = TestCrypto(failPlain = "refresh-new")
        val auth = repo(store, crypto)
        assertTrue(auth.signIn("new@test.invalid", "synthetic-password").isFailure)
        assertEquals(2, crypto.encryptCalls)
        assertTrue(store.writes.isEmpty())
        assertNull(auth.session.first())
    }

    @Test fun cryptoReturningPlaintextIsAlsoRejected() = runTest {
        val store = TestStore()
        val auth = repo(store, TestCrypto(passthrough = true))
        assertTrue(auth.signIn("new@test.invalid", "synthetic-password").isFailure)
        assertNull(auth.session.first())
        assertTrue(store.writes.isEmpty())
    }

    @Test fun failedNewAccountSaveKeepsOnlyThePriorSafeEncryptedSession() = runTest {
        val crypto = TestCrypto()
        val original = prefs(crypto)
        val store = TestStore(original)
        val auth = repo(store, crypto)
        val prior = auth.session.first()
        crypto.failPlain = "refresh-new"
        assertTrue(auth.signIn("new@test.invalid", "synthetic-password").isFailure)
        assertEquals(prior, auth.session.first())
        assertEquals(original, store.state.value)
        assertTrue(store.writes.isEmpty())
    }

    @Test fun encryptedRestoreDoesNotRewriteTokens() = runTest {
        val crypto = TestCrypto()
        val store = TestStore(prefs(crypto))
        assertEquals("access-old", repo(store, crypto).accessToken())
        assertTrue(store.writes.isEmpty())
    }

    @Test fun legacyTokensAreResealedBeforeTrustedSessionPublication() = runTest {
        val crypto = TestCrypto()
        val store = TestStore(prefs(crypto, legacy = true))
        val auth = repo(store, crypto)
        assertEquals("access-old", auth.session.first()!!.accessToken)
        assertEquals(1, store.writes.size)
        assertEquals("access-old", repo(store).session.first()!!.accessToken)
        assertEncryptedWrites(store)
        assertEquals("keep", store.state.value[unrelated])
    }

    @Test fun failedLegacyMigrationRequiresReloginAndNeverWritesPlaintext() = runTest {
        val crypto = TestCrypto()
        val store = TestStore(prefs(crypto, legacy = true))
        crypto.failPlain = "refresh-old"
        val auth = repo(store, crypto)
        assertNull(auth.session.first())
        assertNull(auth.accessToken())
        assertNull(store.state.value[access])
        assertNull(store.state.value[refresh])
        assertEquals("keep", store.state.value[unrelated])
        assertEncryptedWrites(store)
    }

    @Test fun lostKeystoreKeyRequiresReloginWithoutTouchingUnrelatedData() = runTest {
        val crypto = TestCrypto()
        val store = TestStore(prefs(crypto))
        crypto.failDecrypt = true
        assertNull(repo(store, crypto).session.first())
        assertEquals("keep", store.state.value[unrelated])
        assertNull(store.state.value[access])
        assertEncryptedWrites(store)
    }

    @Test fun unknownEncryptedVersionIsNotTreatedAsLegacyPlaintext() = runTest {
        val store = TestStore(emptyPreferences().toMutablePreferences().apply {
            this[access] = "enc:v99:unsupported"
            this[refresh] = "enc:v99:unsupported"
        })
        assertNull(repo(store).session.first())
        assertNull(store.state.value[access])
    }

    @Test fun failedPersistenceAfterRotatingRefreshRequiresRelogin() = runTest {
        val crypto = TestCrypto()
        val store = TestStore(prefs(crypto, until = 1_000))
        val auth = repo(store, crypto)
        crypto.failPlain = "refresh-new"
        assertNull(auth.accessToken())
        assertNull(auth.session.first())
        assertNull(store.state.value[access])
        assertEncryptedWrites(store)
    }

    @Test fun delayedRefreshCannotUndoLogoutAndLogoutDoesNotWaitForNetwork() = runTest {
        val entered = CompletableDeferred<Unit>()
        val response = CompletableDeferred<JSONObject>()
        val crypto = TestCrypto()
        val store = TestStore(prefs(crypto, until = 1_000))
        val auth = repo(store, crypto) { path, _, _ ->
            if (path.contains("refresh_token")) { entered.complete(Unit); response.await() } else JSONObject()
        }
        val pending = async { auth.accessToken() }
        entered.await()
        auth.signOut() // Must finish BEFORE releasing the outstanding refresh response.
        assertNull(auth.session.first())
        assertNull(auth.accessToken()) // Signed-out callers must not wait for the old refresh either.
        response.complete(json())
        assertNull(pending.await())
        assertNull(auth.session.first())
        assertNull(store.state.value[access])
    }

    @Test fun oldRefresh401CannotClearNewerLogin() = runTest {
        val entered = CompletableDeferred<Unit>()
        val response = CompletableDeferred<JSONObject>()
        val crypto = TestCrypto()
        val store = TestStore(prefs(crypto, until = 1_000))
        val auth = repo(store, crypto) { path, _, _ ->
            if (path.contains("refresh_token")) { entered.complete(Unit); response.await() } else json()
        }
        val pending = async { auth.accessToken() }
        entered.await()
        val newer = auth.signIn("new@test.invalid", "synthetic-password").getOrThrow()
        response.completeExceptionally(AuthRepository.AuthHttpException(401, "synthetic rejected refresh"))
        assertNull(pending.await())
        assertEquals(newer, auth.session.first())
        assertEquals(newer, repo(store).session.first())
    }

    @Test fun oldRefreshSuccessCannotOverwriteNewerLogin() = runTest {
        val entered = CompletableDeferred<Unit>()
        val response = CompletableDeferred<JSONObject>()
        val crypto = TestCrypto()
        val store = TestStore(prefs(crypto, until = 1_000))
        val auth = repo(store, crypto) { path, _, _ ->
            if (path.contains("refresh_token")) { entered.complete(Unit); response.await() } else json()
        }
        val pending = async { auth.accessToken() }
        entered.await()
        val newer = auth.signIn("new@test.invalid", "synthetic-password").getOrThrow()
        assertEquals(newer.accessToken, auth.accessToken()) // New account is usable before old I/O finishes.
        response.complete(json("late-access", "late-refresh"))
        assertNull(pending.await())
        assertEquals(newer, auth.session.first())
    }

    @Test fun delayedSignInCannotRestoreSessionAfterLogout() = runTest {
        val entered = CompletableDeferred<Unit>()
        val response = CompletableDeferred<JSONObject>()
        val store = TestStore()
        val auth = repo(store) { _, _, _ -> entered.complete(Unit); response.await() }
        val pending = async { auth.signIn("new@test.invalid", "synthetic-password") }
        entered.await()
        auth.signOut()
        response.complete(json())
        assertTrue(pending.await().isFailure)
        assertNull(auth.session.first())
        assertNull(store.state.value[access])
    }

    @Test fun latestStartedSignInWinsOverAnOlderResponse() = runTest {
        val entered = CompletableDeferred<Unit>()
        val response = CompletableDeferred<JSONObject>()
        var calls = 0
        val auth = repo(TestStore()) { _, _, _ ->
            if (++calls == 1) { entered.complete(Unit); response.await() } else json()
        }
        val old = async { auth.signIn("old@test.invalid", "synthetic-password") }
        entered.await()
        val newer = auth.signIn("new@test.invalid", "synthetic-password").getOrThrow()
        assertEquals(newer.accessToken, auth.accessToken()) // New account is usable before old I/O finishes.
        response.complete(json("late-access", "late-refresh"))
        assertTrue(old.await().isFailure)
        assertEquals(newer, auth.session.first())
    }

    @Test fun concurrentRefreshesAreDeduplicated() = runTest {
        val entered = CompletableDeferred<Unit>()
        val response = CompletableDeferred<JSONObject>()
        var calls = 0
        val crypto = TestCrypto()
        val auth = repo(TestStore(prefs(crypto, until = 1_000)), crypto) { _, _, _ ->
            calls++; entered.complete(Unit); response.await()
        }
        val first = async { auth.accessToken() }
        entered.await()
        val second = async { auth.accessToken() }
        response.complete(json())
        assertEquals("access-new", first.await())
        assertEquals("access-new", second.await())
        assertEquals(1, calls)
    }

    @Test fun definitiveRefreshFailureClearsOnlyAuthStorage() = runTest {
        val crypto = TestCrypto()
        val store = TestStore(prefs(crypto, until = 1_000))
        val auth = repo(store, crypto) { _, _, _ -> throw AuthRepository.AuthHttpException(400, "invalid refresh") }
        assertNull(auth.accessToken())
        assertNull(auth.session.first())
        assertEquals("keep", store.state.value[unrelated])
    }

    @Test fun transientRefreshFailurePreservesSafePriorSession() = runTest {
        val crypto = TestCrypto()
        val original = prefs(crypto, until = 1_000)
        val store = TestStore(original)
        val auth = repo(store, crypto) { _, _, _ -> throw AuthRepository.AuthHttpException(503, "temporarily unavailable") }
        assertNull(auth.accessToken())
        assertEquals("access-old", auth.session.first()!!.accessToken)
        assertEquals(original, store.state.value)
    }

    @Test fun old401CannotClearNewLoginEvenIfAccessTokenStringsMatch() = runTest {
        val crypto = TestCrypto()
        val store = TestStore(prefs(crypto, a = "same-access"))
        val auth = repo(store, crypto) { _, _, _ -> json("same-access", "new-refresh") }
        val oldRequest = auth.requestToken()!!
        val newer = auth.signIn("new@test.invalid", "synthetic-password").getOrThrow()
        auth.clearIfCurrent(oldRequest)
        assertEquals(newer, auth.session.first())
    }

    @Test fun old401CannotClearARefreshedSessionWithTheSameJwtString() = runTest {
        var now = 0L
        val crypto = TestCrypto()
        val auth = repo(TestStore(prefs(crypto, until = 70, a = "same-access")), crypto, { now }) { _, _, _ -> json("same-access", "new-refresh") }
        val oldRequest = auth.requestToken()!!
        now = 60
        assertEquals("same-access", auth.accessToken())
        auth.clearIfCurrent(oldRequest)
        assertEquals("new-refresh", auth.session.first()!!.refreshToken)
    }

    @Test fun currentRequest401ClearsItsOwnSession() = runTest {
        val crypto = TestCrypto()
        val auth = repo(TestStore(prefs(crypto)), crypto)
        auth.clearIfCurrent(auth.requestToken()!!)
        assertNull(auth.session.first())
    }

    @Test fun requestCredentialClosureRejectsOnlyItsCapturedGeneration() = runTest {
        val crypto = TestCrypto()
        val auth = repo(TestStore(prefs(crypto)), crypto)
        val oldCredential = auth.chatCredential()!!
        val newer = auth.signIn("new@test.invalid", "synthetic-password").getOrThrow()
        oldCredential.onUnauthorized()
        assertEquals(newer, auth.session.first())
        auth.chatCredential()!!.onUnauthorized()
        assertNull(auth.session.first())
    }

    @Test fun delayedLogoutResponseDoesNotOverwriteNewLocalLogin() = runTest {
        val entered = CompletableDeferred<Unit>()
        val finishLogout = CompletableDeferred<JSONObject>()
        val crypto = TestCrypto()
        val auth = repo(TestStore(prefs(crypto)), crypto) { path, _, _ ->
            if (path.contains("logout")) { entered.complete(Unit); finishLogout.await() } else json()
        }
        val pending = async { auth.signOut() }
        entered.await()
        assertNull(auth.session.first())
        val newer = auth.signIn("new@test.invalid", "synthetic-password").getOrThrow()
        finishLogout.complete(JSONObject())
        pending.await()
        assertEquals(newer, auth.session.first())
    }

    @Test fun failedLogoutWriteIsHonestAndRetryableWithoutResurrectingRefresh() = runTest {
        val crypto = TestCrypto()
        val store = TestStore(prefs(crypto))
        val auth = repo(store, crypto)
        val prior = auth.session.first()
        store.failWrites = true
        assertTrue(runCatching { auth.signOut() }.exceptionOrNull() is AuthStorageException)
        assertEquals(prior, auth.session.first())
        store.failWrites = false
        auth.signOut()
        assertNull(auth.session.first())
    }

    @Test fun cancelledRefreshDoesNotClearSafeStorageAndReleasesTheMutex() = runTest {
        val entered = CompletableDeferred<Unit>()
        var calls = 0
        val crypto = TestCrypto()
        val store = TestStore(prefs(crypto, until = 1_000))
        val auth = repo(store, crypto) { _, _, _ ->
            if (++calls == 1) { entered.complete(Unit); awaitCancellation() } else json()
        }
        val pending = async { auth.accessToken() }
        entered.await()
        pending.cancelAndJoin()
        assertEquals("access-old", auth.session.first()!!.accessToken)
        assertEquals("access-new", auth.accessToken())
    }

    @Test fun cancellationAfterDiskCommitCannotLeaveOldMemoryWithNewTokens() = runTest {
        val committed = CompletableDeferred<Unit>()
        val finishWrite = CompletableDeferred<Unit>()
        val store = TestStore()
        val auth = repo(store)
        store.afterWrite = { committed.complete(Unit); finishWrite.await() }
        val pending = async { auth.signIn("new@test.invalid", "synthetic-password") }
        committed.await()
        pending.cancel()
        finishWrite.complete(Unit)
        pending.join()
        assertEquals("access-new", auth.session.first()!!.accessToken)
        assertEquals(auth.session.first(), repo(store).session.first())
        assertEncryptedWrites(store)
    }

    @Test fun cancellationAfterLogoutDiskCommitStillPublishesSignedOut() = runTest {
        val committed = CompletableDeferred<Unit>()
        val finishWrite = CompletableDeferred<Unit>()
        val crypto = TestCrypto()
        val store = TestStore(prefs(crypto))
        val auth = repo(store, crypto)
        auth.session.first()
        store.afterWrite = { committed.complete(Unit); finishWrite.await() }
        val pending = async { auth.clear() }
        committed.await()
        pending.cancel()
        finishWrite.complete(Unit)
        pending.join()
        assertNull(auth.session.first())
        assertNull(store.state.value[access])
    }

    @Test fun old401DuringPendingSignInCannotCancelThatNewLogin() = runTest {
        val entered = CompletableDeferred<Unit>()
        val response = CompletableDeferred<JSONObject>()
        val crypto = TestCrypto()
        val auth = repo(TestStore(prefs(crypto)), crypto) { _, _, _ -> entered.complete(Unit); response.await() }
        val oldRequest = auth.requestToken()!!
        val pending = async { auth.signIn("new@test.invalid", "synthetic-password") }
        entered.await()
        auth.clearIfCurrent(oldRequest)
        response.complete(json())
        assertTrue(pending.await().isSuccess)
        assertEquals("access-new", auth.session.first()!!.accessToken)
    }

    @Test fun failedAtomicStoreWriteDoesNotPublishNewAccountOrPartialTokens() = runTest {
        val crypto = TestCrypto()
        val original = prefs(crypto)
        val store = TestStore(original)
        val auth = repo(store, crypto)
        val prior = auth.session.first()
        store.failWrites = true
        assertTrue(auth.signIn("new@test.invalid", "synthetic-password").exceptionOrNull() is AuthStorageException)
        assertEquals(prior, auth.session.first())
        assertEquals(original, store.state.value)
        assertTrue(store.writes.isEmpty())
    }
}

private class TestStore(initial: Preferences = emptyPreferences()) : DataStore<Preferences> {
    val state = MutableStateFlow(initial)
    val writes = mutableListOf<Preferences>()
    var failWrites = false
    var afterWrite: (suspend () -> Unit)? = null
    private val mutex = Mutex()
    override val data: Flow<Preferences> = state
    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = mutex.withLock {
        if (failWrites) throw IOException("Synthetic storage failure")
        val next = transform(state.value)
        writes += next
        state.value = next
        afterWrite?.invoke()
        next
    }
}

private class TestCrypto(var failPlain: String? = null, val passthrough: Boolean = false) : TokenCrypto {
    var encryptCalls = 0
    var failDecrypt = false
    override fun encrypt(plain: String): String {
        encryptCalls++
        if (plain == failPlain) throw IllegalStateException("Synthetic Keystore failure")
        if (passthrough) return plain
        return TokenFormat.encode(byteArrayOf(1, 2, 3), plain.toByteArray().reversedArray())
    }
    override fun decrypt(stored: String): String? {
        if (failDecrypt) return null
        return TokenFormat.decryptWith(stored) { _, ciphertext -> ciphertext.reversedArray() }
    }
}
