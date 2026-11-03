package ai.orhan.feature.workflows

import ai.orhan.data.agents.SavedAgent
import ai.orhan.data.workflows.Workflow
import ai.orhan.data.workflows.WorkflowCapacityException
import ai.orhan.feature.settings.detail.FootNote
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun AutomationsScreen(
    workflows: List<Workflow>,
    agents: List<SavedAgent>,
    onBack: () -> Unit,
    onSetUpManually: () -> Unit,
    onEditWorkflow: (Workflow) -> Unit,
    onGenerate: suspend (Workflow, Boolean) -> Boolean,
) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var savedTab by rememberSaveable { mutableStateOf(false) }
    val selected = WorkAutomationTemplates.firstOrNull { it.id == selectedId }
    if (selected != null) {
        AutomationPromptBuilder(selected, agents, onBack = { selectedId = null }, onGenerate = onGenerate)
        return
    }
    Scaffold { insets ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(insets),
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item("header") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") }
                    Text("Automations", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).padding(top = 12.dp))
                    TextButton(onClick = onSetUpManually, modifier = Modifier.heightIn(min = 48.dp)) { Text("Set up manually") }
                }
            }
            item("categories") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(selected = savedTab, onClick = { savedTab = true }, label = { Text("My Automations") }, modifier = Modifier.heightIn(min = 48.dp))
                    FilterChip(selected = !savedTab, onClick = { savedTab = false }, label = { Text("Work") }, modifier = Modifier.heightIn(min = 48.dp))
                }
            }
            if (savedTab) {
                item("saved_explanation") { FootNote("Saved workflow prompts. Open one to edit it, view real execution history or configure scheduled delivery.") }
                if (workflows.isEmpty()) item("empty") { Text("No saved automations yet. Choose a template or set one up manually.") }
                items(workflows, key = { "saved_${it.id}" }) { workflow ->
                    OutlinedCard(onClick = { onEditWorkflow(workflow) }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(workflow.title, style = MaterialTheme.typography.titleMedium)
                            Text(workflow.agentName.ifBlank { "Orhan" }, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            } else {
                items(WorkAutomationTemplates, key = { it.id }) { template ->
                    OutlinedCard(onClick = { selectedId = template.id }, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Icon(Icons.Rounded.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(template.title, style = MaterialTheme.typography.titleMedium)
                                Text(template.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AutomationPromptBuilder(
    template: AutomationTemplate,
    agents: List<SavedAgent>,
    onBack: () -> Unit,
    onGenerate: suspend (Workflow, Boolean) -> Boolean,
) {
    var task by rememberSaveable(template.id) { mutableStateOf("") }
    var intentId by rememberSaveable(template.id) { mutableStateOf(template.intent.name) }
    var agentId by rememberSaveable(template.id) { mutableStateOf<String?>(null) }
    val workflowId = rememberSaveable(template.id) { UUID.randomUUID().toString() }
    val createdAt = rememberSaveable(template.id) { System.currentTimeMillis() }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var discard by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val intent = AutomationIntent.valueOf(intentId)
    val agent = agents.firstOrNull { it.id == agentId }
    val missingAgent = agentId != null && agent == null
    val prompt = remember(intent, task, agent?.name) { compileAutomationPrompt(intent, task, agent?.name.orEmpty()) }
    fun save(configureExecution: Boolean) {
        if (busy || task.isBlank() || missingAgent) return
        busy = true
        error = null
        scope.launch {
            try {
                val saved = onGenerate(Workflow(workflowId, template.title, prompt, agent?.name.orEmpty(), createdAt, agent?.id), configureExecution)
                if (!saved) error = "Couldn't save your prompt. Your draft is still here."
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) { error = if (failure is WorkflowCapacityException) failure.message else "Couldn't save your prompt. Your draft is still here." }
            finally { busy = false }
        }
    }
    fun back() { if (!busy) { if (task.isNotBlank()) discard = true else onBack() } }
    BackHandler { back() }
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("Discard this draft?") },
        text = { Text("Your saved automations will remain unchanged.") },
        confirmButton = { TextButton(onClick = onBack) { Text("Discard") } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text("Keep editing") } })
    Scaffold(bottomBar = {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(enabled = !busy && task.isNotBlank() && !missingAgent, onClick = { save(false) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(if (busy) "Saving prompt…" else "Generate Prompt") }
            OutlinedButton(enabled = !busy && task.isNotBlank() && !missingAgent, onClick = { save(true) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Save & configure execution") }
            Text("Generate opens a chat draft. Configure opens run, handoff and schedule controls. Neither button starts a run.", style = MaterialTheme.typography.bodySmall)
        }
    }) { insets ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(insets), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item("heading") {
                Row {
                    IconButton(onClick = ::back) { Icon(Icons.Rounded.ArrowBack, "Back") }
                    Column(Modifier.padding(top = 8.dp)) {
                        Text(template.title, style = MaterialTheme.typography.titleLarge)
                        Text("Edit your task — the prompt updates below.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item("compiled") {
                OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("COMPILED PROMPT", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(value = task, onValueChange = { task = it.take(1500); error = null },
                            label = { Text("${intent.prefix}…") }, minLines = 3, enabled = !busy, modifier = Modifier.fillMaxWidth())
                        Text(prompt, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            item("intent") {
                Text("INTENT · what", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AutomationIntent.entries.forEach { option ->
                        FilterChip(selected = option == intent, onClick = { intentId = option.name }, enabled = !busy,
                            label = { Text(option.label) }, modifier = Modifier.heightIn(min = 48.dp))
                    }
                }
            }
            item("schedule") {
                Text("SCHEDULE · when", style = MaterialTheme.typography.labelLarge)
                Text("Run now · once. Review and send the generated prompt in chat.")
            }
            item("team") {
                Text("TEAM · who", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = agentId == null, onClick = { agentId = null }, enabled = !busy,
                        label = { Text("Orhan") }, modifier = Modifier.heightIn(min = 48.dp))
                    agents.forEach { saved ->
                        FilterChip(selected = saved.id == agentId, onClick = { agentId = saved.id }, enabled = !busy,
                            label = { Text(saved.name) }, modifier = Modifier.heightIn(min = 48.dp))
                    }
                }
                if (missingAgent) Text("The selected agent is unavailable. Choose an available agent before generating.", color = MaterialTheme.colorScheme.error)
            }
            item("scope") {
                Text("SCOPE", style = MaterialTheme.typography.labelLarge)
                Text("One-time request. Generating a prompt does not start background work.")
            }
        }
    }
}
