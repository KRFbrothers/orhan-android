package ai.orhan.feature.artifacts

import android.content.Intent
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.saveable.listSaver
import ai.orhan.data.artifacts.ArtifactType
import ai.orhan.data.artifacts.ArtifactKind
import ai.orhan.data.artifacts.exportDocument
import ai.orhan.data.artifacts.previewText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
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
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
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
import ai.orhan.data.artifacts.artifactListPreferences
import ai.orhan.data.artifacts.matching
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
    onUpsert: suspend (Artifact) -> Boolean,
    onDelete: suspend (String) -> Boolean,
    onBack: () -> Unit,
    /** True when shown under the home tabs, which already inset the status bar. */
    embedded: Boolean = false,
    /** Opens this artifact once, for example from search. Cleared via [onPendingOpenConsumed]. */
    pendingOpenId: String? = null,
    onPendingOpenConsumed: () -> Unit = {},
    onOpenSourceMessage: (String, Long) -> Unit = { _, _ -> },
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
            onSave = { title, content, type ->
                val saved = onUpsert(
                    (existing ?: Artifact(editing, title, content, System.currentTimeMillis())).copy(
                        title = title, content = content, type = type,
                        createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                    ),
                )
                if (saved) {
                    openId = editing
                    editId = null
                }
                saved
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
            onDelete = {
                val removed = onDelete(open.id)
                if (removed) openId = null
                removed
            },
            onOpenSourceMessage = onOpenSourceMessage,
            applyStatusBarPadding = !embedded,
        )
        return
    }
    val context = LocalContext.current
    val listPrefs = remember(context) { context.artifactListPreferences() }
    val largePreview by listPrefs.largePreview.collectAsStateWithLifecycle(initialValue = false)
    val scope = rememberCoroutineScope()
    var switchingLayout by remember { mutableStateOf(false) }
    var layoutError by rememberSaveable { mutableStateOf<String?>(null) }
    SettingsDetailScaffold(
        title = "Artifacts",
        onBack = onBack,
        showBack = !embedded,
        applyStatusBarPadding = !embedded,
    ) {
        val colors = OrhanTheme.colors
        val shown = artifacts.matching(filter).sortedByDescending { it.createdAt }
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
                    .semantics { stateDescription = if (largePreview) "Large preview" else "Compact" }
                    .clickable(role = Role.Button, enabled = !switchingLayout) {
                        if (switchingLayout) return@clickable
                        switchingLayout = true
                        layoutError = null
                        val next = !largePreview
                        scope.launch {
                            try {
                                val saved = listPrefs.setLargePreview(next)
                                if (!saved) layoutError = "Couldn't change the layout."
                            } finally {
                                switchingLayout = false
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.GridView, contentDescription = "Switch layout", tint = MaterialTheme.colorScheme.onSurface)
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
        val layoutMessage = layoutError
        if (layoutMessage != null) {
            Text(
                layoutMessage,
                style = OrhanTextStyles.snippet,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
            )
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
            if (largePreview) {
                LargeArtifactPreview(a, onOpen = { openId = a.id })
            } else {
                CompactArtifactRow(a, onOpen = { openId = a.id })
            }
        }
    }
}

@Composable
private fun CompactArtifactRow(a: Artifact, onOpen: () -> Unit) {
    val colors = OrhanTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, colors.hairline, RoundedCornerShape(16.dp))
            .clickable(onClick = onOpen)
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
            Text(
                if (a.kind == ArtifactKind.SAVED_MESSAGE) "${a.kind.label} · ${a.author.orEmpty()}" else a.type.label,
                style = OrhanTextStyles.meta, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Bounded text excerpt from the actual type; full formatting is rendered in detail. */
@Composable
private fun LargeArtifactPreview(a: Artifact, onOpen: () -> Unit) {
    val colors = OrhanTheme.colors
    val excerpt = remember(a) { a.previewText() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .heightIn(min = 148.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, colors.hairline, RoundedCornerShape(20.dp))
            .clickable(onClick = onOpen)
            .padding(16.dp),
    ) {
        Text(
            a.title.ifBlank { "Untitled" },
            style = OrhanTextStyles.rowTitle,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            formatDate(a.createdAt),
            style = OrhanTextStyles.meta,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
        )
        Text(
            excerpt,
            style = OrhanTextStyles.snippet,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 6,
            overflow = TextOverflow.Ellipsis,
        )
        Text("${if (a.kind == ArtifactKind.SAVED_MESSAGE) a.kind.label else a.type.label} · Text preview", style = OrhanTextStyles.meta,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ArtifactDetail(
    a: Artifact,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: suspend () -> Boolean,
    onOpenSourceMessage: (String, Long) -> Unit,
    applyStatusBarPadding: Boolean = true,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val deletion = remember(a.id) { ArtifactDeleteState() }
    var transferMessage by rememberSaveable(a.id) { mutableStateOf<String?>(null) }
    var transferring by remember { mutableStateOf(false) }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        if (result.resultCode == Activity.RESULT_OK && uri != null) {
            transferring = true
            scope.launch {
                try {
                    val document = a.exportDocument()
                    withContext(Dispatchers.IO) {
                        val output = context.contentResolver.openOutputStream(uri, "wt")
                            ?: error("Cannot open export destination")
                        output.use { it.write(document.bytes) }
                    }
                    transferMessage = "Exported ${document.fileName}"
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    transferMessage = "Couldn't export this document. The saved original is unchanged."
                } finally { transferring = false }
            }
        }
    }
    if (deletion.confirming) {
        AlertDialog(
            onDismissRequest = { if (!deletion.deleting) deletion.confirming = false },
            title = { Text("Delete artifact?") },
            text = {
                Text(deletion.error ?: "\u201c${a.title}\u201d will be removed from this device.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch { deletion.delete(onDelete) }
                    },
                    enabled = !deletion.deleting,
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deletion.confirming = false }, enabled = !deletion.deleting) { Text("Cancel") }
            },
        )
    }
    SettingsDetailScaffold(title = a.title, onBack = onBack, applyStatusBarPadding = applyStatusBarPadding) {
        Text(formatDate(a.createdAt), style = OrhanTextStyles.meta,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
        Text(a.type.label, style = OrhanTextStyles.meta, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (a.kind == ArtifactKind.SAVED_MESSAGE) {
            Text("${a.kind.label} · ${a.author.orEmpty()}", style = OrhanTextStyles.meta,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (a.sourceChatId != null && a.sourceMessageId != null) {
                TextButton(onClick = { onOpenSourceMessage(a.sourceChatId, a.sourceMessageId) }) {
                    Text("Open source chat")
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            OutlinedButton(onClick = onEdit) { Text("Edit") }
            OutlinedButton(enabled = !transferring, onClick = {
                transferring = true
                transferMessage = null
                scope.launch {
                    try {
                        val send = artifactShareIntent(context, a)
                        context.startActivity(Intent.createChooser(send, "Share artifact"))
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        transferMessage = "Couldn't share this document. The saved original is unchanged."
                    } finally { transferring = false }
                }
            }) { Text("Share") }
            OutlinedButton(enabled = !transferring, onClick = {
                val document = a.exportDocument()
                transferMessage = null
                try {
                    exporter.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = document.mimeType
                        putExtra(Intent.EXTRA_TITLE, document.fileName)
                    })
                } catch (_: Exception) { transferMessage = "No document picker is available." }
            }) { Text("Export") }
            OutlinedButton(onClick = { deletion.confirming = true }) { Text("Delete") }
        }
        transferMessage?.let { Text(it, style = OrhanTextStyles.snippet) }
        Spacer(Modifier.height(4.dp))
        if (a.type == ArtifactType.PLAIN_TEXT) SelectionContainer {
            Text(a.content, style = OrhanTextStyles.bubble, color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 4.dp))
        } else {
            Text("Offline preview · links and external resources are disabled.",
                style = OrhanTextStyles.meta, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ArtifactPreview(a)
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ArtifactEditor(
    initial: Artifact?,
    onCancel: () -> Unit,
    onSave: suspend (String, String, ArtifactType) -> Boolean,
    applyStatusBarPadding: Boolean = true,
) {
    val scope = rememberCoroutineScope()
    val draft = rememberSaveable(initial?.id, saver = listSaver<ArtifactEditorState, String>(
        save = { listOf(it.title, it.content, it.type.wireName) },
        restore = { values -> ArtifactEditorState(values[0], values[1],
            ArtifactType.entries.first { it.wireName == values[2] }) },
    )) { ArtifactEditorState(initial?.title.orEmpty(), initial?.content.orEmpty(), initial?.type ?: ArtifactType.PLAIN_TEXT) }
    BackHandler(enabled = draft.saving) { }
    SettingsDetailScaffold(
        title = if (initial == null) "New document" else "Edit document",
        onBack = { if (!draft.saving) onCancel() },
        applyStatusBarPadding = applyStatusBarPadding,
    ) {
        if (initial == null) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ArtifactType.entries.forEach { type ->
                FilterChip(selected = draft.type == type, onClick = { draft.type = type },
                    enabled = !draft.saving, label = { Text(type.label) })
            }
        } else Text(draft.type.label, style = OrhanTextStyles.meta)
        OutlinedTextField(
            value = draft.title,
            onValueChange = { draft.title = it.take(200) },
            enabled = !draft.saving,
            label = { Text("Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        )
        OutlinedTextField(
            value = draft.content,
            onValueChange = { draft.content = it.take(MAX_ARTIFACT_CONTENT) },
            enabled = !draft.saving,
            label = { Text(when (draft.type) { ArtifactType.PLAIN_TEXT -> "Content"; ArtifactType.MARKDOWN -> "Markdown source"; ArtifactType.HTML -> "HTML source" }) },
            supportingText = { Text("${draft.content.length} / $MAX_ARTIFACT_CONTENT") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp).padding(vertical = 4.dp),
        )
        val message = draft.error
        if (message != null) {
            Text(
                message,
                style = OrhanTextStyles.snippet,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            Button(
                onClick = {
                    scope.launch { draft.save(onSave) }
                },
                enabled = draft.content.isNotBlank() && !draft.saving,
            ) { Text("Save") }
            OutlinedButton(onClick = onCancel, enabled = !draft.saving) { Text("Cancel") }
        }
    }
}

private fun formatDate(t: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(t))
