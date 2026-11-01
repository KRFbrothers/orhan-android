package ai.orhan.data.memory

import ai.orhan.feature.chat.personalMemoryBlock
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

private class FakePrefs : DataStore<Preferences> {
    val state = MutableStateFlow(emptyPreferences())
    override val data: Flow<Preferences> = state
    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        transform(state.value).also { state.value = it }
}

class MemoryPauseStoreTest {
    @Test fun defaultsToNotPaused() = runBlocking {
        assertFalse(MemoryPauseStore(FakePrefs()).paused.first())
    }

    @Test fun pauseAndResumePersist() = runBlocking {
        val ds = FakePrefs()
        MemoryPauseStore(ds).setPaused(true)
        assertTrue(MemoryPauseStore(ds).paused.first())
        MemoryPauseStore(ds).setPaused(false)
        assertFalse(MemoryPauseStore(ds).paused.first())
    }

    @Test fun readFailureWithholdsPersonalContext() = runBlocking {
        val state = MemoryPauseStore(FailingReadPrefs()).state.first()
        assertTrue(state.unreadable)
        assertTrue(state.paused)
        assertNull(personalMemoryBlock("secret pref", state.paused))
    }
}

private class FailingReadPrefs : DataStore<Preferences> {
    override val data: Flow<Preferences> = flow { throw IOException("unreadable prefs") }
    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        throw IOException("unreadable prefs")
    }
}
