package ai.orhan.data.artifacts

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.artifactListDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "orhan_artifact_list_prefs",
)

internal val ARTIFACT_LARGE_PREVIEW_KEY = booleanPreferencesKey("large_preview")

/**
 * List layout for saved plain-text documents. Compact rows are the default.
 * A failed write returns false and leaves the stored choice unchanged.
 * A failed read stays on compact rows.
 */
class ArtifactListPreferences(private val dataStore: DataStore<Preferences>) {
    val largePreview: Flow<Boolean> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { it[ARTIFACT_LARGE_PREVIEW_KEY] ?: false }

    suspend fun setLargePreview(value: Boolean): Boolean {
        return try {
            dataStore.edit { it[ARTIFACT_LARGE_PREVIEW_KEY] = value }
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        }
    }
}

fun Context.artifactListPreferences(): ArtifactListPreferences =
    ArtifactListPreferences(applicationContext.artifactListDataStore)
