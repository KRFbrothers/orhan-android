package ai.orhan.data.memory

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Pause flag for the personal-context string.
 *
 * The installed app already stores this as a Preferences boolean `memory_paused`
 * in `orhan_memory_prefs.preferences_pb`. The name and key stay the same so an
 * update keeps the saved choice. The context text itself stays in [MemoryStore].
 */
const val MEMORY_PAUSE_FILE = "orhan_memory_prefs"
const val MEMORY_PAUSED_KEY = "memory_paused"

private val Context.memoryPauseDataStore: DataStore<Preferences> by preferencesDataStore(name = MEMORY_PAUSE_FILE)

private val pausedKey = booleanPreferencesKey(MEMORY_PAUSED_KEY)

data class MemoryPause(val loaded: Boolean = false, val paused: Boolean = true)

/**
 * Text actually sent with a chat. Until the pause flag has been read, and while
 * it is on, nothing from the saved personal context is included.
 */
fun chatPersonalContext(saved: String, loaded: Boolean, paused: Boolean): String =
    if (!loaded || paused) "" else saved.trim()

class MemoryPauseStore(private val dataStore: DataStore<Preferences>) {
    val state: Flow<MemoryPause> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { prefs -> MemoryPause(loaded = true, paused = prefs[pausedKey] ?: false) }

    suspend fun setPaused(paused: Boolean) {
        dataStore.edit { prefs -> prefs[pausedKey] = paused }
    }
}

fun Context.memoryPauseStore(): MemoryPauseStore = MemoryPauseStore(applicationContext.memoryPauseDataStore)
