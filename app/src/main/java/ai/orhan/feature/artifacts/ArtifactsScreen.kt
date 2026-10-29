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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.Search
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import ai.orhan.design.theme.OrhanTheme
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
    /** True when shown under the home tabs, which already inset the status bar. */
    embedded: Boolean = false,
    /** Opens this artifact once, for example from search. Cleared via [onPendingOpenConsumed]. */
    pendingOpenId: String? = null,
    onPendingOpenConsumed: () -> Unit = {},
) {
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    // Id being edited; may be a fresh id not yet in the store (new document).
    var editId by rememberSaveable { mutableStateOf<String?>(null) }
    var filter by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(pendingOpenId, artifacts) {
        val id = pendingOpenId ?: return@LaunchedEffect
        if (artifacts.any { it.id == id }) {
            editId = null
            openId = id
            onPendingOpenConsumed()
        } else if (artifacts.isNotEmpty()) {
            onPendingOpenConsumed()
        }
    }
    val editing = editId
    if (editing != null) {
        val existing = artifacts.firstOrNull { it.id == editing }
        val close = { editId = null }
        BackHandler { close() }
        ArtifactEditor(
            initial = existing,
            onCancel = close,
            applyStatusBarPadding = !embedded,
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
            applyStatusBarPadding = !embedded,
        )
        return
    }
    SettingsDetailScaffold(
        title = "Artifacts",
        onBack = onBack,
        showBack = !embedded,
        applyStatusBarPadding = !embedded,
    ) {
        val colors = OrhanTheme.colors
        val shown = artifacts
            .filter {
                filter.isBlank() || it.title.contains(filter, ignoreCase = true) ||
                    it.content.contains(filter, ignoreCase = true)
            }
            .sortedByDescending { it.createdAt }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(OrhanTheme.shapes.pill)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, colors.fieldEdge, OrhanTheme.shapes.pill)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = filter,
                    onValueChange = { filter = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = OrhanTextStyles.snippet.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { inner ->
                        if (filter.isEmpty()) {
                            Text("Search", style = OrhanTextStyles.snippet, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        inner()
                    },
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, colors.fieldEdge, CircleShape)
                    .clickable { editId = UUID.randomUUID().toString() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "New document", tint = MaterialTheme.colorScheme.onSurface)
            }
        }
        if (artifacts.isEmpty()) {
            Text(
                text = "No artifacts yet — save a reply from chat, or create a document.",
                style = OrhanTextStyles.snippet,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp, start = 4.dp),
            )
        } else if (shown.isEmpty()) {
            Text(
                text = "No artifacts match",
                style = OrhanTextStyles.snippet,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp, start = 4.dp),
            )
        }
        shown.forEach { a ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, colors.hairline, RoundedCornerShape(16.dp))
                    .clickable { openId = a.id }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.brandSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Description, contentDescription = null, tint = colors.onBrandSoft)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        a.title.ifBlank { "Untitled" },
                        style = OrhanTextStyles.rowTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        formatDate(a.createdAt),
                        style = OrhanTextStyles.meta,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtifactDetail(
    a: Artifact,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    applyStatusBarPadding: Boolean = true,
) {
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
    SettingsDetailScaffold(title = a.title, onBack = onBack, applyStatusBarPadding = applyStatusBarPadding) {
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
private fun ArtifactEditor(
    initial: Artifact?,
    onCancel: () -> Unit,
    onSave: (String, String) -> Unit,
    applyStatusBarPadding: Boolean = true,
) {
    var title by rememberSaveable { mutableStateOf(initial?.title.orEmpty()) }
    var content by rememberSaveable { mutableStateOf(initial?.content.orEmpty()) }
    SettingsDetailScaffold(
        title = if (initial == null) "New document" else "Edit document",
        onBack = onCancel,
        applyStatusBarPadding = applyStatusBarPadding,
    ) {
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
