package ai.orhan.feature.settings.detail

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/* ============================================================================
 *  Orhan AI — Notifications + Language persistence (mirrors ThemePreferences)
 *
 *  Local, on-device preferences only. Nothing here registers for push (there
 *  is no FCM yet) or subscribes to email; the toggles record the user's choice
 *  so a future delivery layer can honour it.
 * ==========================================================================*/

private val Context.settingsDetailDataStore: DataStore<Preferences> by preferencesDataStore(name = "orhan_settings_detail")

data class NotificationPrefs(
    val push: Boolean = true,
    val dms: Boolean = true,
    val agentReplies: Boolean = true,
    val suggestions: Boolean = false,
    val community: Boolean = true,
    val digest: Boolean = true,
    val news: Boolean = false,
    val quiet: Boolean = false,
    val sound: Boolean = true,
)

data class LanguagePrefs(
    val code: String = DEFAULT_LANGUAGE,
    val agentsUseAppLanguage: Boolean = true,
)

const val DEFAULT_LANGUAGE = "en-US"

/** Code → English display name; used both by the screen and the chat reply hint. */
val SupportedLanguages: List<Pair<String, Pair<String, String>>> = listOf(
    "en-US" to ("English (US)" to "English"),
    "en-GB" to ("English (UK)" to "English"),
    "hi" to ("Hindi" to "हिन्दी"),
    "es" to ("Spanish" to "Español"),
    "ja" to ("Japanese" to "日本語"),
    "ar" to ("Arabic" to "العربية"),
)

fun languageName(code: String): String =
    SupportedLanguages.firstOrNull { it.first == code }?.second?.first ?: "English (US)"

class SettingsDetailPreferences(private val dataStore: DataStore<Preferences>) {

    private object K {
        val push = booleanPreferencesKey("notif_push")
        val dms = booleanPreferencesKey("notif_dms")
        val agentReplies = booleanPreferencesKey("notif_agent_replies")
        val suggestions = booleanPreferencesKey("notif_suggestions")
        val community = booleanPreferencesKey("notif_community")
        val digest = booleanPreferencesKey("notif_digest")
        val news = booleanPreferencesKey("notif_news")
        val quiet = booleanPreferencesKey("notif_quiet")
        val sound = booleanPreferencesKey("notif_sound")
        val language = stringPreferencesKey("language_code")
        val agentsUseAppLanguage = booleanPreferencesKey("language_agents_use_app")
    }

    private val data: Flow<Preferences> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }

    val notifications: Flow<NotificationPrefs> = data.map { p ->
        val d = NotificationPrefs()
        NotificationPrefs(
            push = p[K.push] ?: d.push,
            dms = p[K.dms] ?: d.dms,
            agentReplies = p[K.agentReplies] ?: d.agentReplies,
            suggestions = p[K.suggestions] ?: d.suggestions,
            community = p[K.community] ?: d.community,
            digest = p[K.digest] ?: d.digest,
            news = p[K.news] ?: d.news,
            quiet = p[K.quiet] ?: d.quiet,
            sound = p[K.sound] ?: d.sound,
        )
    }

    val language: Flow<LanguagePrefs> = data.map { p ->
        LanguagePrefs(
            code = p[K.language]?.takeIf { c -> SupportedLanguages.any { it.first == c } } ?: DEFAULT_LANGUAGE,
            agentsUseAppLanguage = p[K.agentsUseAppLanguage] ?: true,
        )
    }

    suspend fun setNotifications(n: NotificationPrefs) {
        dataStore.edit { p ->
            p[K.push] = n.push; p[K.dms] = n.dms; p[K.agentReplies] = n.agentReplies
            p[K.suggestions] = n.suggestions; p[K.community] = n.community
            p[K.digest] = n.digest; p[K.news] = n.news; p[K.quiet] = n.quiet; p[K.sound] = n.sound
        }
    }

    suspend fun setLanguage(code: String) { dataStore.edit { it[K.language] = code } }

    suspend fun setAgentsUseAppLanguage(on: Boolean) { dataStore.edit { it[K.agentsUseAppLanguage] = on } }
}

fun Context.settingsDetailPreferences(): SettingsDetailPreferences =
    SettingsDetailPreferences(applicationContext.settingsDetailDataStore)
