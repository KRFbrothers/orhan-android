package ai.orhan.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.replyStyleDataStore: DataStore<Preferences> by preferencesDataStore(name = "orhan_reply_style_prefs")

internal val REPLY_STYLE_KEY = stringPreferencesKey("reply_style")

/** How long agent replies should be. Balanced is the default. */
enum class ReplyStyle { Short, Balanced, Detailed;
    companion object {
        fun parse(raw: String?): ReplyStyle = entries.firstOrNull { it.name == raw } ?: Balanced
    }
}

/** Local-only agent reply-style preference. */
class ReplyStyleStore(private val dataStore: DataStore<Preferences>) {
    val style: Flow<ReplyStyle> = dataStore.data.map { ReplyStyle.parse(it[REPLY_STYLE_KEY]) }

    suspend fun setStyle(value: ReplyStyle) {
        dataStore.edit { it[REPLY_STYLE_KEY] = value.name }
    }
}

fun Context.replyStyleStore(): ReplyStyleStore = ReplyStyleStore(applicationContext.replyStyleDataStore)
