package ai.orhan.feature.settings.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ai.orhan.design.theme.OrhanTextStyles
import ai.orhan.design.theme.OrhanTheme
import kotlinx.coroutines.launch

private fun countLabel(n: Int?, one: String, many: String): String =
    when (n) { null -> "…"; 1 -> "1 $one"; else -> "$n $many" }

/** Human-readable byte size; "…" while loading, "Empty" when nothing is stored. */
internal fun sizeLabel(bytes: Long?): String = when {
    bytes == null -> "…"
    bytes <= 0L -> "Empty"
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024 -> String.format(java.util.Locale.US, "%.1f KB", bytes / 1024.0)
    else -> String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024))
}

/**
 * Settings → Storage. Shows honest on-device counts (artifacts, saved chats) and the real size of
 * chat attachment files. "Clear" deletes those files and their metadata; chat text is kept.
 */
@Composable
fun StorageScreen(
    onBack: () -> Unit,
    artifactCount: Int? = null,
    chatThreadCount: Int? = null,
    onOpenArtifacts: () -> Unit = {},
    chatMediaBytes: Long? = null,
    onClearChatMedia: suspend () -> Unit = {},
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf(false) }
    var clearing by remember { mutableStateOf(false) }
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
            {
                NavRow("Chat media", if (clearing) "Clearing…" else sizeLabel(chatMediaBytes), trailing = {
                    TextButton(
                        onClick = { confirm = true },
                        enabled = !clearing && (chatMediaBytes ?: 0L) > 0L,
                    ) { Text("Clear") }
                })
            },
        ))
        SectionLabel("Library")
        GroupCard(listOf(
            { NavRow("Artifact library", if (artifactCount != null && artifactCount > 0) artifactsLabel else "Saved replies", onClick = onOpenArtifacts) },
        ))
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Clear chat media?") },
            text = { Text("Deletes photos, audio and files attached in chats from this phone. Your chat messages stay.") },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    clearing = true
                    scope.launch {
                        val ok = runCatching { onClearChatMedia() }.isSuccess
                        clearing = false
                        snackbar.showSnackbar(if (ok) "Chat media cleared" else "Couldn't clear chat media")
                    }
                }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } },
        )
    }
}
