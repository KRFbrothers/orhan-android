package ai.orhan.data.memory

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Same file the installed app already uses (`orhan_memory_prefs.preferences_pb`). */
internal const val MEMORY_PAUSE_FILE = "orhan_memory_prefs"

private val Context.memoryPauseDataStore: DataStore<Preferences> by preferencesDataStore(name = MEMORY_PAUSE_FILE)

internal val MEMORY_PAUSED_KEY = booleanPreferencesKey("memory_paused")
internal val AUTOMATIC_MEMORY_KEY = booleanPreferencesKey("automatic_memory")
internal val AUTOMATIC_MEMORY_ENABLED_AT_KEY = longPreferencesKey("automatic_memory_enabled_at")
internal val INCLUDE_TOPICS_KEY = booleanPreferencesKey("include_topics_in_chats")
internal val AUTOMATIC_PROCESSED_KEY = stringPreferencesKey("automatic_processed_messages")
internal val AUTOMATIC_LAST_ERROR_KEY = stringPreferencesKey("automatic_last_error")

/**
 * Pause flag for the personal-context string, plus the automatic-memory choice.
 * A missing pause key is not paused. A missing automatic key is off.
 * [unreadable] means the preference file could not be read, so chats must omit
 * context and automatic extraction must not run.
 */
data class MemoryPause(
    val paused: Boolean = false,
    val unreadable: Boolean = false,
    val automaticEnabled: Boolean = false,
    /** Epoch millis when automatic memory was last turned on. Zero while never enabled. */
    val automaticEnabledAt: Long = 0L,
    /** Saved topic text is added to chats only when this is on and pause is off. */
    val includeTopics: Boolean = false,
    val processedMessages: Set<String> = emptySet(),
    val lastError: String? = null,
)

/** Separate prefs file so the existing `orhan_memory.pb` string store is untouched. */
class MemoryPauseStore(private val dataStore: DataStore<Preferences>) {
    val state: Flow<MemoryPause> = dataStore.data
        .map { prefs ->
            val processed = prefs[AUTOMATIC_PROCESSED_KEY].orEmpty()
                .lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()
            MemoryPause(
                paused = prefs[MEMORY_PAUSED_KEY] ?: false,
                automaticEnabled = prefs[AUTOMATIC_MEMORY_KEY] ?: false,
                automaticEnabledAt = prefs[AUTOMATIC_MEMORY_ENABLED_AT_KEY] ?: 0L,
                includeTopics = prefs[INCLUDE_TOPICS_KEY] ?: false,
                processedMessages = processed,
                lastError = prefs[AUTOMATIC_LAST_ERROR_KEY]?.takeIf { it.isNotBlank() },
            )
        }
        .catch { error ->
            if (error is IOException) emit(MemoryPause(paused = true, unreadable = true))
            else throw error
        }

    /** True when chats must omit personal context: user pause, or an unreadable file. */
    val paused: Flow<Boolean> = state.map { it.paused }

    suspend fun setPaused(value: Boolean) {
        dataStore.edit { it[MEMORY_PAUSED_KEY] = value }
    }

    /**
     * Turns automatic extraction on or off. Turning it on records [now] as the
     * watermark. Messages saved before that are not read. The pause flag is left
     * as the user set it.
     */
    suspend fun setAutomaticEnabled(enabled: Boolean, now: Long = System.currentTimeMillis()) {
        dataStore.edit { prefs ->
            prefs[AUTOMATIC_MEMORY_KEY] = enabled
            if (enabled) prefs[AUTOMATIC_MEMORY_ENABLED_AT_KEY] = now
            if (!enabled) prefs.remove(AUTOMATIC_LAST_ERROR_KEY)
        }
    }

    /** Opt in to sending saved topic titles and summaries with chats. Does not change pause. */
    suspend fun setIncludeTopics(enabled: Boolean) {
        dataStore.edit { it[INCLUDE_TOPICS_KEY] = enabled }
    }

    suspend fun rememberProcessed(key: String) {
        dataStore.edit { prefs ->
            prefs[AUTOMATIC_PROCESSED_KEY] = rememberProcessedLine(prefs[AUTOMATIC_PROCESSED_KEY].orEmpty(), key)
        }
    }

    suspend fun setLastError(message: String) {
        dataStore.edit { it[AUTOMATIC_LAST_ERROR_KEY] = message }
    }

    suspend fun clearLastError() {
        dataStore.edit { it.remove(AUTOMATIC_LAST_ERROR_KEY) }
    }
}

fun Context.memoryPauseStore(): MemoryPauseStore = MemoryPauseStore(applicationContext.memoryPauseDataStore)
