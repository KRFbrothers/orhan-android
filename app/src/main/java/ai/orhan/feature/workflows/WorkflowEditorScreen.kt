package ai.orhan.feature.workflows

import ai.orhan.data.workflows.WORKFLOW_MAX_CONTENT
import ai.orhan.data.workflows.Workflow
import ai.orhan.data.workflows.WorkflowRun
import ai.orhan.feature.settings.detail.FootNote
import ai.orhan.feature.settings.detail.SectionLabel
import ai.orhan.feature.settings.detail.SettingsDetailScaffold
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Create or edit a workflow (saved prompt). [existing] null = create. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkflowEditorScreen(
    existing: Workflow?,
    runs: List<WorkflowRun> = emptyList(),
    agentNames: List<String>,
    onBack: () -> Unit,
    onOpenRunChat: (String) -> Unit = {},
    onSave: suspend (Workflow) -> Boolean,
    onDelete: (suspend (Workflow) -> Boolean)? = null,
) {
    var title by rememberSaveable(existing?.id) { mutableStateOf(existing?.title.orEmpty()) }
    var content by rememberSaveable(existing?.id) { mutableStateOf(existing?.content.orEmpty()) }
    var agent by rememberSaveable(existing?.id) { mutableStateOf(existing?.agentName.orEmpty()) }
    var showError by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var writeError by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    if (confirmDelete && existing != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { if (!deleting) confirmDelete = false },
            title = { Text("Delete ${existing.title}?") },
            text = { Text(writeError ?: "This workflow will be removed.") },
            confirmButton = { TextButton(enabled = !deleting, onClick = {
                if (!deleting) scope.launch {
                    deleting = true
                    val removed = try { onDelete(existing) }
                    catch (cancel: CancellationException) { throw cancel }
                    catch (_: Exception) { false }
                    finally { deleting = false }
                    if (removed) { confirmDelete = false; writeError = null; onBack() }
                    else writeError = "Couldn't delete this workflow. Your saved workflow is unchanged."
                }
            }) { Text(if (deleting) "Deleting…" else "Delete") } },
            dismissButton = { TextButton(enabled = !deleting, onClick = { confirmDelete = false; writeError = null }) { Text("Cancel") } },
        )
    }

    SettingsDetailScaffold(title = if (existing == null) "New workflow" else "Edit workflow", onBack = onBack) {
        SectionLabel("Title")
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        SectionLabel("Prompt")
        OutlinedTextField(
            value = content,
            onValueChange = { content = it.take(WORKFLOW_MAX_CONTENT); if (it.isNotBlank()) showError = false },
            label = { Text("Prompt") },
            minLines = 5,
            isError = showError,
            supportingText = {
                Text(if (showError) "Prompt is required" else "${content.length}/$WORKFLOW_MAX_CONTENT")
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
        )
        SectionLabel("Agent")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = agent.isBlank(), onClick = { agent = "" }, label = { Text("Default") })
            agentNames.forEach { name ->
                FilterChip(
                    selected = agent.equals(name, ignoreCase = true),
                    onClick = { agent = name },
                    label = { Text(name) },
                )
            }
        }
        if (agent.isNotBlank() && agentNames.none { it.equals(agent, ignoreCase = true) }) {
            FootNote("Saved agent \"$agent\" is unavailable. Choose Default or an available agent before opening this workflow.")
        }
        FootNote("Open prepares a chat draft. Run sends the prompt immediately and records its outcome in run history.")
        if (existing != null && runs.isNotEmpty()) {
            SectionLabel("Run history")
            runs.take(10).forEach { run ->
                TextButton(onClick = { onOpenRunChat(run.chatTitle) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("${run.status.name.lowercase().replace('_', ' ')} · ${run.agentName} · ${java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT).format(java.util.Date(run.startedAt))}")
                }
                run.error?.let { FootNote(it) }
            }
        }
        writeError?.takeIf { !confirmDelete }?.let { FootNote(it) }
        Spacer(Modifier.height(24.dp))
        Button(
            enabled = !saving,
            onClick = {
                if (content.isBlank()) {
                    showError = true
                } else if (!saving) {
                    scope.launch {
                        saving = true
                        writeError = null
                        val saved = try {
                            onSave(Workflow(
                            id = existing?.id.orEmpty(),
                            title = title,
                            content = content,
                            agentName = agent,
                            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                            ))
                        } catch (cancel: CancellationException) { throw cancel }
                        catch (_: Exception) { false }
                        finally { saving = false }
                        if (saved) onBack() else writeError = "Couldn't save this workflow. Your draft is still here."
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { Text(if (saving) "Saving…" else "Save") }
        if (existing != null && onDelete != null) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { writeError = null; confirmDelete = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text("Delete") }
        }
    }
}
