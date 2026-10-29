package ai.orhan.feature.settings.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

/** Settings → Chat preferences (design kit #11, data-go="chatprefs"). Toggles persist in DataStore. */
@Composable
fun ChatPreferencesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { context.applicationContext.chatPreferences() }
    val prefs by store.prefs.collectAsState(initial = ChatPrefs())
    val scope = rememberCoroutineScope()
    val set: ((ChatPrefs) -> ChatPrefs) -> Unit = { t -> scope.launch { store.update(t) } }

    SettingsDetailScaffold(title = "Chat preferences", onBack = onBack) {
        SectionLabel("Composer")
        GroupCard(listOf(
            { SwitchRow("Send with Enter", "Off means Enter adds a new line", prefs.sendWithEnter, { v -> set { it.copy(sendWithEnter = v) } }) },
            { SwitchRow("Hold to record voice", "Mic button records instead of dictating", prefs.holdToRecord, { v -> set { it.copy(holdToRecord = v) } }) },
            { SwitchRow("Preview before sending", "Confirm links and files first", prefs.previewBeforeSending, { v -> set { it.copy(previewBeforeSending = v) } }) },
        ))
        SectionLabel("Thread")
        GroupCard(listOf(
            { SwitchRow("Typing indicators", "Show when an agent is responding", prefs.typingIndicators, { v -> set { it.copy(typingIndicators = v) } }) },
            { SwitchRow("Auto-play media", "GIFs and video previews in chat", prefs.autoPlayMedia, { v -> set { it.copy(autoPlayMedia = v) } }) },
            { SwitchRow("Agent activity cards", "Inline artifact previews in replies", prefs.agentActivityCards, { v -> set { it.copy(agentActivityCards = v) } }) },
        ))
    }
}
