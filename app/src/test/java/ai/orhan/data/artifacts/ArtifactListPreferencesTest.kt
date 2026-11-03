package ai.orhan.data.artifacts

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
import java.io.IOException

/**
 * Compact versus large preview is a local list preference. It does not change
 * the saved document.
 */
class ArtifactListPreferencesTest {

    @Test fun defaultsToCompactRows() = runBlocking {
        assertFalse(ArtifactListPreferences(MemoryPrefs()).largePreview.first())
    }

    @Test fun choiceSurvivesANewReader() = runBlocking {
        val data = MemoryPrefs()
        assertTrue(ArtifactListPreferences(data).setLargePreview(true))
        assertTrue(ArtifactListPreferences(data).largePreview.first())
        assertTrue(ArtifactListPreferences(data).setLargePreview(false))
        assertFalse(ArtifactListPreferences(data).largePreview.first())
    }

    @Test fun failedWriteKeepsCompactRows() = runBlocking {
        val data = MemoryPrefs()
        assertTrue(ArtifactListPreferences(data).setLargePreview(true))
        assertFalse(ArtifactListPreferences(ThrowingPrefs(data.state)).setLargePreview(false))
        assertTrue(ArtifactListPreferences(data).largePreview.first())
    }
}

private class MemoryPrefs : DataStore<Preferences> {
    val state = MutableStateFlow(emptyPreferences())
    override val data: Flow<Preferences> = state
    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        transform(state.value).also { state.value = it }
}

private class ThrowingPrefs(override val data: Flow<Preferences>) : DataStore<Preferences> {
    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        throw IOException("disk")
    }
}
