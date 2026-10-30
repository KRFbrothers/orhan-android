package ai.orhan.feature.settings.detail

import ai.orhan.data.prefs.ReplyStyle
import ai.orhan.data.prefs.replyStyleStore
import ai.orhan.design.theme.OrhanTextStyles
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
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
    val styleStore = remember(context) { context.replyStyleStore() }
    val style by styleStore.style.collectAsState(initial = ReplyStyle.Balanced)
    val styleOptions = listOf(
        ReplyStyle.Short to "Brief answers",
        ReplyStyle.Balanced to "Default length",
        ReplyStyle.Detailed to "More explanation",
    )

    SettingsDetailScaffold(title = "Chat preferences", onBack = onBack) {
        SectionLabel("Agent replies · Reply style")
        Column(Modifier.selectableGroup()) {
            GroupCard(styleOptions.map { (option, subtitle) ->
                @Composable {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .selectable(selected = style == option, role = Role.RadioButton) { scope.launch { styleStore.setStyle(option) } }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(option.name, style = OrhanTextStyles.rowTitle, color = MaterialTheme.colorScheme.onSurface)
                            Text(subtitle, style = OrhanTextStyles.snippet, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        RadioButton(selected = style == option, onClick = null)
                    }
                }
            })
        }
        SectionLabel("Composer")
        GroupCard(listOf(
            { SwitchRow("Send with Enter", "Off means Enter adds a new line", prefs.sendWithEnter, { v -> set { it.copy(sendWithEnter = v) } }) },
            { SwitchRow("Preview before sending", "Confirm links and files first", prefs.previewBeforeSending, { v -> set { it.copy(previewBeforeSending = v) } }) },
        ))
        SectionLabel("Thread")
        GroupCard(listOf(
            { SwitchRow("Typing indicators", "Show when an agent is responding", prefs.typingIndicators, { v -> set { it.copy(typingIndicators = v) } }) },
            { SwitchRow("Agent activity cards", "Inline artifact previews in replies", prefs.agentActivityCards, { v -> set { it.copy(agentActivityCards = v) } }) },
        ))
        // holdToRecord and autoPlayMedia stay in the serializer. Chat does not record or auto-play, so they are not shown.
    }
}
