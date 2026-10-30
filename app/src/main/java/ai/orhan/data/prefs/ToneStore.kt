package ai.orhan.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.toneDataStore: DataStore<Preferences> by preferencesDataStore(name = "orhan_agent_tone_prefs")

internal val AGENT_TONE_KEY = stringPreferencesKey("agent_tone")

/** Agent tone. Friendly is the default. */
enum class AgentTone { Friendly, Direct, Professional;
    companion object {
        fun parse(raw: String?): AgentTone = entries.firstOrNull { it.name == raw } ?: Friendly
    }
}

/** Local-only agent tone preference. */
class ToneStore(private val dataStore: DataStore<Preferences>) {
    val tone: Flow<AgentTone> = dataStore.data.map { AgentTone.parse(it[AGENT_TONE_KEY]) }

    suspend fun setTone(value: AgentTone) {
        dataStore.edit { it[AGENT_TONE_KEY] = value.name }
    }
}

fun Context.toneStore(): ToneStore = ToneStore(applicationContext.toneDataStore)
