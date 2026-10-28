package ai.orhan.feature.settings.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/** Settings → Notifications. Local state only; no real push registration. */
@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    var push by rememberSaveable { mutableStateOf(true) }
    var dms by rememberSaveable { mutableStateOf(true) }
    var agentReplies by rememberSaveable { mutableStateOf(true) }
    var suggestions by rememberSaveable { mutableStateOf(false) }
    var community by rememberSaveable { mutableStateOf(true) }
    var digest by rememberSaveable { mutableStateOf(true) }
    var news by rememberSaveable { mutableStateOf(false) }
    var quiet by rememberSaveable { mutableStateOf(false) }
    var sound by rememberSaveable { mutableStateOf(true) }

    SettingsDetailScaffold(title = "Notifications", onBack = onBack) {
        SectionLabel("Push")
        GroupCard(listOf(
            { SwitchRow("Push notifications", "Allow Orhan to send notifications", push, { push = it }) },
            { SwitchRow("Direct messages", checked = dms, onCheckedChange = { dms = it }, enabled = push) },
            { SwitchRow("Agent replies", "When an agent finishes a task", agentReplies, { agentReplies = it }, push) },
            { SwitchRow("Agent suggestions", checked = suggestions, onCheckedChange = { suggestions = it }, enabled = push) },
            { SwitchRow("Community activity", checked = community, onCheckedChange = { community = it }, enabled = push) },
        ))
        SectionLabel("Email")
        GroupCard(listOf(
            { SwitchRow("Weekly digest", checked = digest, onCheckedChange = { digest = it }) },
            { SwitchRow("Product news", checked = news, onCheckedChange = { news = it }) },
        ))
        SectionLabel("Behavior")
        GroupCard(listOf(
            { SwitchRow("Quiet hours", "10:00 PM – 7:00 AM", quiet, { quiet = it }) },
            { SwitchRow("Sound", checked = sound, onCheckedChange = { sound = it }) },
        ))
    }
}
