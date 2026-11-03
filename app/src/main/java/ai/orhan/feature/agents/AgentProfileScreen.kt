package ai.orhan.feature.agents

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ai.orhan.data.agents.agentAvatarFiles
import ai.orhan.feature.settings.detail.FootNote
import ai.orhan.feature.settings.detail.SectionLabel
import ai.orhan.feature.settings.detail.SettingsDetailScaffold
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember

@Composable
fun AgentProfileScreen(
    profile: ResolvedAgentProfile?,
    loading: Boolean,
    onBack: () -> Unit,
    onChat: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
) {
    val context = LocalContext.current
    val avatarFiles = remember(context) { context.agentAvatarFiles() }
    val agent = profile?.agent
    val saved = profile?.savedAgent
    SettingsDetailScaffold(title = if (loading) "Agent" else agent?.name ?: "Agent unavailable", onBack = onBack) {
        if (loading) {
            Text("Loading agent…", modifier = Modifier.padding(20.dp))
            return@SettingsDetailScaffold
        }
        if (agent == null) {
            Text("This agent was deleted or is no longer available.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(20.dp))
            return@SettingsDetailScaffold
        }
        AgentAvatarImage(avatarFiles.path(saved?.avatarFileName) ?: agent.avatarPath, agent.avatar, 88.dp, "${agent.name} avatar")
        Spacer(Modifier.height(16.dp))
        Text(agent.name, style = MaterialTheme.typography.headlineSmall)
        if (agent.blurb.isNotBlank()) Text(agent.blurb, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (saved != null) {
            SectionLabel("Instructions")
            Text(saved.instructions.ifBlank { "No custom instructions." }, style = MaterialTheme.typography.bodyMedium)
        }
        SectionLabel("Capabilities")
        if (saved?.toolsRestricted == true) {
            Text(saved.tools.joinToString(", ").ifBlank { "No tools selected." }, style = MaterialTheme.typography.bodyMedium)
        }
        FootNote("Tool availability is checked by the connected service when you send a message.")
        Spacer(Modifier.height(20.dp))
        Button(onClick = onChat, modifier = Modifier.fillMaxWidth()) { Text("Open agent chat") }
        if (onEdit != null) TextButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("Edit profile and instructions") }
        if (onDelete != null) TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) { Text("Delete agent", color = MaterialTheme.colorScheme.error) }
    }
}
