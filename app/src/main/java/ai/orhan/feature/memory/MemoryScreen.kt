package ai.orhan.feature.memory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ai.orhan.data.memory.MAX_ENTRY_TITLE
import ai.orhan.data.memory.MAX_NOTE_CONTENT
import ai.orhan.data.memory.MEMORY_MAX_CHARS
import ai.orhan.data.memory.MemoryEntry
import ai.orhan.data.memory.memoryEntriesStore
import ai.orhan.data.memory.memoryStore
import ai.orhan.feature.settings.detail.FootNote
import ai.orhan.feature.settings.detail.SectionLabel
import ai.orhan.feature.settings.detail.SettingsDetailScaffold
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val SECTIONS = listOf("Personal context", "Notes", "To-dos")

/** Memory: personal context (injected into chats), plus local-only notes and to-dos. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(onBack: () -> Unit, embedded: Boolean = false) {
    val snackbar = remember { SnackbarHostState() }
    var section by rememberSaveable { mutableIntStateOf(0) }

    SettingsDetailScaffold(
        title = "Memory",
        onBack = onBack,
        snackbarHostState = snackbar,
        showBack = !embedded,
        applyStatusBarPadding = !embedded,
    ) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            SECTIONS.forEachIndexed { i, label ->
                SegmentedButton(
                    selected = section == i,
                    onClick = { section = i },
                    shape = SegmentedButtonDefaults.itemShape(i, SECTIONS.size),
                ) { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
        when (section) {
            0 -> PersonalContextSection(snackbar)
            1 -> EntriesSection(isTodo = false)
            else -> EntriesSection(isTodo = true)
        }
    }
}

@Composable
private fun PersonalContextSection(snackbar: SnackbarHostState) {
    val context = LocalContext.current
    val store = remember(context) { context.memoryStore() }
    val scope = rememberCoroutineScope()
    var text by rememberSaveable { mutableStateOf("") }
    var loaded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(store) {
        if (!loaded) { text = store.memory.first(); loaded = true }
    }
    SectionLabel("What should your agents know?")
    OutlinedTextField(
        value = text,
        onValueChange = { text = it.take(MEMORY_MAX_CHARS) },
        placeholder = { Text("e.g. I'm a student in Delhi, prefer short answers, vegetarian…") },
        supportingText = { Text("${text.length} / $MEMORY_MAX_CHARS") },
        minLines = 6,
        modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp),
    )
    Spacer(Modifier.height(12.dp))
    Button(
        onClick = {
            scope.launch {
                store.set(text)
                snackbar.showSnackbar("Memory saved")
            }
        },
        enabled = loaded,
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Save") }
    FootNote("Memory stays on this device. It's sent along with your chats to the engine so agents can use it.")
}

/** Editing target: null = closed, entry with blank id = new. */
private data class EditState(val id: String?, val title: String, val content: String)

@Composable
private fun EntriesSection(isTodo: Boolean) {
    val context = LocalContext.current
    val store = remember(context) { context.memoryEntriesStore() }
    val scope = rememberCoroutineScope()
    val items by (if (isTodo) store.todos else store.notes).collectAsState(initial = emptyList())
    var editing by remember(isTodo) { mutableStateOf<EditState?>(null) }
    var deleting by remember(isTodo) { mutableStateOf<MemoryEntry?>(null) }
    val noun = if (isTodo) "to-do" else "note"

    OutlinedButton(
        onClick = { editing = EditState(null, "", "") },
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    ) { Text(if (isTodo) "Add to-do" else "Add note") }

    if (items.isEmpty()) {
        Text(
            if (isTodo) "No to-dos yet. Add one to keep track." else "No notes yet. Jot something down.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 24.dp),
        )
    }
    items.forEach { e ->
        Row(
            modifier = Modifier.fillMaxWidth().clickable { editing = EditState(e.id, e.title, e.content) }.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isTodo) Checkbox(checked = e.done, onCheckedChange = { scope.launch { store.toggleDone(e.id) } })
            Column(Modifier.weight(1f).padding(start = if (isTodo) 0.dp else 4.dp)) {
                Text(
                    e.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (isTodo && e.done) TextDecoration.LineThrough else null,
                    color = if (isTodo && e.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (e.content.isNotBlank()) Text(
                    e.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = { deleting = e }) { Icon(Icons.Rounded.Delete, "Delete $noun") }
        }
        HorizontalDivider()
    }
    FootNote(if (isTodo) "To-dos stay on this device only." else "Notes stay on this device and aren't sent to your agents.")

    editing?.let { st ->
        var title by remember(st) { mutableStateOf(st.title) }
        var content by remember(st) { mutableStateOf(st.content) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(if (st.id == null) "New $noun" else "Edit $noun") },
            text = {
                Column {
                    OutlinedTextField(
                        value = title, onValueChange = { title = it.take(MAX_ENTRY_TITLE) },
                        label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = content, onValueChange = { content = it.take(MAX_NOTE_CONTENT) },
                        label = { Text(if (isTodo) "Details (optional)" else "Note (optional)") },
                        minLines = if (isTodo) 2 else 5, maxLines = 10, modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(enabled = title.isNotBlank(), onClick = {
                    scope.launch {
                        if (isTodo) store.upsertTodo(st.id, title, content) else store.upsertNote(st.id, title, content)
                    }
                    editing = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } },
        )
    }
    deleting?.let { e ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete $noun?") },
            text = { Text("\"${e.title}\" will be removed from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { if (isTodo) store.deleteTodo(e.id) else store.deleteNote(e.id) }
                    deleting = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}
