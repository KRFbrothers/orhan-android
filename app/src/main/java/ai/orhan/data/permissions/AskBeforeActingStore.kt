package ai.orhan.data.permissions

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.permissionsDataStore: DataStore<Preferences> by preferencesDataStore(name = "orhan_permissions_prefs")

internal val ASK_BEFORE_ACTING_KEY = booleanPreferencesKey("ask_before_acting")

/** Local-only "Ask before acting" preference. Defaults to ON (safer). */
class AskBeforeActingStore(private val dataStore: DataStore<Preferences>) {
    val enabled: Flow<Boolean> = dataStore.data.map { it[ASK_BEFORE_ACTING_KEY] ?: true }

    suspend fun setEnabled(value: Boolean) {
        dataStore.edit { it[ASK_BEFORE_ACTING_KEY] = value }
    }
}

fun Context.askBeforeActingStore(): AskBeforeActingStore = AskBeforeActingStore(applicationContext.permissionsDataStore)
