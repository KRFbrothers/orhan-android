package ai.orhan.feature.settings.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

/**
 * Settings → Notifications. Choices persist on-device (DataStore). Push and email
 * delivery are not connected yet (no FCM / mail backend), so the rows say so.
 */
@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember(context) { context.settingsDetailPreferences() }
    val n by repo.notifications.collectAsStateWithLifecycle(initialValue = NotificationPrefs())
    val scope = rememberCoroutineScope()
    fun save(update: NotificationPrefs) { scope.launch { repo.setNotifications(update) } }

    SettingsDetailScaffold(title = "Notifications", onBack = onBack) {
        SectionLabel("Push · saved on this device; delivery coming later")
        GroupCard(listOf(
            { SwitchRow("Push notifications", "Your preference is saved. Push delivery isn't connected yet.", n.push, { save(n.copy(push = it)) }) },
            { SwitchRow("Direct messages", checked = n.dms, onCheckedChange = { save(n.copy(dms = it)) }, enabled = n.push) },
            { SwitchRow("Agent replies", "When an agent finishes a task", n.agentReplies, { save(n.copy(agentReplies = it)) }, n.push) },
            { SwitchRow("Agent suggestions", checked = n.suggestions, onCheckedChange = { save(n.copy(suggestions = it)) }, enabled = n.push) },
            { SwitchRow("Community activity", checked = n.community, onCheckedChange = { save(n.copy(community = it)) }, enabled = n.push) },
        ))
        SectionLabel("Email · saved; emails not sent yet")
        GroupCard(listOf(
            { SwitchRow("Weekly digest", checked = n.digest, onCheckedChange = { save(n.copy(digest = it)) }) },
            { SwitchRow("Product news", checked = n.news, onCheckedChange = { save(n.copy(news = it)) }) },
        ))
        SectionLabel("Behavior")
        GroupCard(listOf(
            { SwitchRow("Quiet hours", "10:00 PM – 7:00 AM", n.quiet, { save(n.copy(quiet = it)) }) },
            { SwitchRow("Sound", checked = n.sound, onCheckedChange = { save(n.copy(sound = it)) }) },
        ))
    }
}
