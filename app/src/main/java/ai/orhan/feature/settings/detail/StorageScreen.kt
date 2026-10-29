package ai.orhan.feature.settings.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ai.orhan.design.theme.OrhanTextStyles
import ai.orhan.design.theme.OrhanTheme
import kotlinx.coroutines.launch

private fun countLabel(n: Int?, one: String, many: String): String =
    when (n) { null -> "…"; 1 -> "1 $one"; else -> "$n $many" }

/**
 * Settings → Storage. Shows honest on-device counts (artifacts, saved chats).
 * Clean-up actions are still stubs and only show a snackbar.
 */
@Composable
fun StorageScreen(
    onBack: () -> Unit,
    artifactCount: Int? = null,
    chatThreadCount: Int? = null,
    onOpenArtifacts: () -> Unit = {},
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val toast: (String) -> Unit = { msg -> scope.launch { snackbar.showSnackbar(msg) } }
    val artifactsLabel = countLabel(artifactCount, "document", "documents")
    val chatsLabel = countLabel(chatThreadCount, "saved chat", "saved chats")

    SettingsDetailScaffold(title = "Storage", onBack = onBack, snackbarHostState = snackbar) {
        SectionLabel("On this device")
        Column(
            Modifier
                .fillMaxWidth()
                .clip(OrhanTheme.shapes.card)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, OrhanTheme.colors.hairline, OrhanTheme.shapes.card)
                .padding(16.dp),
        ) {
            Text("$artifactsLabel · $chatsLabel", style = OrhanTextStyles.rowTitle, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "Stored locally on this phone. Nothing here counts against a cloud quota.",
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SectionLabel("Breakdown")
        GroupCard(listOf(
            { NavRow("Artifacts", value = artifactsLabel) },
            { NavRow("Chat history", value = chatsLabel) },
        ))
        SectionLabel("Clean up")
        GroupCard(listOf(
            { NavRow("Cached previews", "Not available yet", trailing = { TextButton({ toast("Cache clearing isn't available yet") }) { Text("Clear") } }) },
            { NavRow("Old chat media", "Not available yet", trailing = { TextButton({ toast("Media clean-up isn't available yet") }) { Text("Clear") } }) },
            { NavRow("Agent logs", "Not available yet", trailing = { TextButton({ toast("Log clearing isn't available yet") }) { Text("Clear") } }) },
        ))
        SectionLabel("Library")
        GroupCard(listOf(
            { NavRow("Artifact library", if (artifactCount != null && artifactCount > 0) artifactsLabel else "Saved replies", onClick = onOpenArtifacts) },
        ))
    }
}
