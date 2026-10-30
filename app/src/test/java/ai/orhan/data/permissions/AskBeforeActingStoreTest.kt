package ai.orhan.data.permissions

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

class AskBeforeActingStoreTest {
    @Test fun defaultsToOn() = runBlocking {
        assertTrue(AskBeforeActingStore(FakePrefs()).enabled.first())
    }

    @Test fun toggleOffAndOnPersists() = runBlocking {
        val ds = FakePrefs()
        AskBeforeActingStore(ds).setEnabled(false)
        assertFalse(AskBeforeActingStore(ds).enabled.first())
        AskBeforeActingStore(ds).setEnabled(true)
        assertTrue(AskBeforeActingStore(ds).enabled.first())
    }
}
