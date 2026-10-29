package ai.orhan.feature.artifacts

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ai.orhan.data.artifacts.Artifact
import ai.orhan.data.artifacts.MAX_ARTIFACT_CONTENT
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.layout.heightIn
import java.util.UUID
import ai.orhan.design.theme.OrhanTextStyles
import ai.orhan.feature.settings.detail.SettingsDetailScaffold
import java.text.DateFormat
import java.util.Date

/** List of saved replies; tapping one shows its detail in the same screen. */
@Composable
fun ArtifactsScreen(
    artifacts: List<Artifact>,
    onUpsert: (Artifact) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit,
) {
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    // Id being edited; may be a fresh id not yet in the store (new document).
    var editId by rememberSaveable { mutableStateOf<String?>(null) }
    val editing = editId
    if (editing != null) {
        val existing = artifacts.firstOrNull { it.id == editing }
        val close = { editId = null }
        BackHandler { close() }
        ArtifactEditor(
            initial = existing,
            onCancel = close,
            onSave = { title, content ->
                onUpsert(
                    Artifact(editing, title, content, existing?.createdAt ?: System.currentTimeMillis()),
                )
                openId = editing
                editId = null
            },
        )
        return
    }
    val open = artifacts.firstOrNull { it.id == openId }
    if (open != null) {
        BackHandler { openId = null }
        ArtifactDetail(
            open,
            onBack = { openId = null },
            onEdit = { editId = open.id },
            onDelete = { onDelete(open.id); openId = null },
        )
        return
    }
    SettingsDetailScaffold(title = "Artifacts", onBack = onBack) {
        OutlinedButton(
            onClick = { editId = UUID.randomUUID().toString() },
            modifier = Modifier.padding(vertical = 8.dp),
        ) { Text("New document") }
        if (artifacts.isEmpty()) {
            Text(
                text = "No artifacts yet \u2014 save a reply from chat, or create a document.",
                style = OrhanTextStyles.snippet,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp, start = 4.dp),
            )
        }
        artifacts.sortedByDescending { it.createdAt }.forEach { a ->
            Column(
                Modifier.fillMaxWidth().clickable { openId = a.id }.padding(vertical = 12.dp, horizontal = 4.dp),
            ) {
                Text(a.title, style = OrhanTextStyles.rowTitle, color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(a.content, style = OrhanTextStyles.meta, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(formatDate(a.createdAt), style = OrhanTextStyles.meta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun ArtifactDetail(a: Artifact, onBack: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    var confirm by rememberSaveable { mutableStateOf(false) }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Delete artifact?") },
            text = { Text("\u201c${a.title}\u201d will be removed from this device.") },
            confirmButton = { TextButton(onClick = { confirm = false; onDelete() }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } },
        )
    }
    SettingsDetailScaffold(title = a.title, onBack = onBack) {
        Text(formatDate(a.createdAt), style = OrhanTextStyles.meta,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            OutlinedButton(onClick = onEdit) { Text("Edit") }
            OutlinedButton(onClick = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, a.title)
                    putExtra(Intent.EXTRA_TEXT, a.content)
                }
                context.startActivity(Intent.createChooser(send, "Share artifact").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }) { Text("Share") }
            OutlinedButton(onClick = { confirm = true }) { Text("Delete") }
        }
        Spacer(Modifier.height(4.dp))
        SelectionContainer {
            Text(a.content, style = OrhanTextStyles.bubble, color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 4.dp))
        }
    }
}

@Composable
private fun ArtifactEditor(initial: Artifact?, onCancel: () -> Unit, onSave: (String, String) -> Unit) {
    var title by rememberSaveable { mutableStateOf(initial?.title.orEmpty()) }
    var content by rememberSaveable { mutableStateOf(initial?.content.orEmpty()) }
    SettingsDetailScaffold(title = if (initial == null) "New document" else "Edit document", onBack = onCancel) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        )
        OutlinedTextField(
            value = content,
            onValueChange = { content = it.take(MAX_ARTIFACT_CONTENT) },
            label = { Text("Content") },
            supportingText = { Text("${content.length} / $MAX_ARTIFACT_CONTENT") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp).padding(vertical = 4.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            Button(onClick = { onSave(title, content) }, enabled = content.isNotBlank()) { Text("Save") }
            OutlinedButton(onClick = onCancel) { Text("Cancel") }
        }
    }
}

private fun formatDate(t: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(t))
