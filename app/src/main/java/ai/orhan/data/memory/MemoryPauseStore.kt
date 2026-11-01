package ai.orhan.data.memory

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Same file the installed app already uses (`orhan_memory_prefs.preferences_pb`). */
internal const val MEMORY_PAUSE_FILE = "orhan_memory_prefs"

private val Context.memoryPauseDataStore: DataStore<Preferences> by preferencesDataStore(name = MEMORY_PAUSE_FILE)

internal val MEMORY_PAUSED_KEY = booleanPreferencesKey("memory_paused")

/**
 * Pause flag for the personal-context string.
 * A missing key is not paused. [unreadable] means the preference file could not
 * be read, so chats must omit the context anyway.
 */
data class MemoryPause(val paused: Boolean = false, val unreadable: Boolean = false)

/** Separate prefs file so the existing `orhan_memory.pb` string store is untouched. */
class MemoryPauseStore(private val dataStore: DataStore<Preferences>) {
    val state: Flow<MemoryPause> = dataStore.data
        .map { prefs -> MemoryPause(paused = prefs[MEMORY_PAUSED_KEY] ?: false) }
        .catch { error ->
            if (error is IOException) emit(MemoryPause(paused = true, unreadable = true))
            else throw error
        }

    /** True when chats must omit personal context: user pause, or an unreadable file. */
    val paused: Flow<Boolean> = state.map { it.paused }

    suspend fun setPaused(value: Boolean) {
        dataStore.edit { it[MEMORY_PAUSED_KEY] = value }
    }
}

fun Context.memoryPauseStore(): MemoryPauseStore = MemoryPauseStore(applicationContext.memoryPauseDataStore)
