package ai.orhan.feature.workflows

import ai.orhan.data.workflows.WORKFLOW_MAX_CONTENT
import ai.orhan.data.workflows.Workflow
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Create or edit a workflow (saved prompt). [existing] null = create. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkflowEditorScreen(
    existing: Workflow?,
    agentNames: List<String>,
    onBack: () -> Unit,
    onSave: (Workflow) -> Unit,
    onDelete: ((Workflow) -> Unit)? = null,
) {
    var title by rememberSaveable(existing?.id) { mutableStateOf(existing?.title.orEmpty()) }
    var content by rememberSaveable(existing?.id) { mutableStateOf(existing?.content.orEmpty()) }
    var agent by rememberSaveable(existing?.id) { mutableStateOf(existing?.agentName.orEmpty()) }
    var showError by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    if (confirmDelete && existing != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${existing.title}?") },
            text = { Text("This workflow will be removed.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete(existing) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
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
        FootNote("Opening a workflow starts a chat with the prompt ready to send.")
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                if (content.isBlank()) {
                    showError = true
                } else {
                    onSave(
                        Workflow(
                            id = existing?.id.orEmpty(),
                            title = title,
                            content = content,
                            agentName = agent,
                            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                        ),
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { Text("Save") }
        if (existing != null && onDelete != null) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { confirmDelete = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text("Delete") }
        }
    }
}
