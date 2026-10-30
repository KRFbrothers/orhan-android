package ai.orhan.data.memory

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.memoryPauseDataStore: DataStore<Preferences> by preferencesDataStore(name = "orhan_memory_prefs")

internal val MEMORY_PAUSED_KEY = booleanPreferencesKey("memory_paused")

/** Separate prefs file so the existing `orhan_memory.pb` string store is untouched. */
class MemoryPauseStore(private val dataStore: DataStore<Preferences>) {
    val paused: Flow<Boolean> = dataStore.data.map { it[MEMORY_PAUSED_KEY] ?: false }

    suspend fun setPaused(value: Boolean) {
        dataStore.edit { it[MEMORY_PAUSED_KEY] = value }
    }
}

fun Context.memoryPauseStore(): MemoryPauseStore = MemoryPauseStore(applicationContext.memoryPauseDataStore)
