package ai.orhan.data.memory

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
