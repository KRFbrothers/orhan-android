package ai.orhan.feature.workflows

import ai.orhan.data.agents.SavedAgent
import ai.orhan.data.workflows.Workflow
import ai.orhan.data.workflows.WorkflowExecution
import ai.orhan.data.workflows.WorkflowExecutionRepository
import ai.orhan.feature.settings.detail.FootNote
import ai.orhan.feature.settings.detail.SectionLabel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Renders only server-confirmed lifecycle state. Leaving the screen does not cancel a run. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkflowExecutionPanel(
    workflow: Workflow,
    agents: List<SavedAgent>,
    repository: WorkflowExecutionRepository,
    signedIn: Boolean,
    onSignIn: () -> Unit,
    draftChanged: Boolean,
) {
    var runs by remember(workflow.id, signedIn) { mutableStateOf<List<WorkflowExecution>>(emptyList()) }
    var error by remember(workflow.id, signedIn) { mutableStateOf<String?>(null) }
    var historyError by remember(workflow.id, signedIn) { mutableStateOf<String?>(null) }
    var loaded by remember(workflow.id, signedIn) { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var handoffs by rememberSaveable(workflow.id) { mutableStateOf(arrayListOf<String>()) }
    var schedule by rememberSaveable(workflow.id) { mutableStateOf("") }
    var retry by remember { mutableStateOf<WorkflowExecution?>(null) }
    var expanded by remember(workflow.id, signedIn) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val lock = remember { Mutex() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    suspend fun refresh() {
        runs = repository.history(workflow.id)
        expanded?.let { id ->
            if (runs.any { it.id == id }) {
                val detail = repository.detail(id)
                runs = runs.map { if (it.id == id) detail else it }
            }
        }
        loaded = true
        historyError = null
    }
    fun action(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            try { lock.withLock { block() } }
            catch (cancel: CancellationException) { throw cancel }
            catch (e: Exception) { error = e.message ?: "Workflow request failed. Refresh history before retrying." }
            finally { busy = false }
        }
    }
    LaunchedEffect(workflow.id, signedIn, lifecycle) {
        if (signedIn) lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                try { lock.withLock { refresh() } }
                catch (cancel: CancellationException) { throw cancel }
                catch (e: Exception) { historyError = e.message ?: "Workflow history could not be refreshed." }
                delay(5000)
            }
        }
    }
    retry?.let { run ->
        AlertDialog(onDismissRequest = { retry = null }, title = { Text("Retry this run?") },
            text = { Text("Successful steps keep their outputs. Failed or cancelled steps run again. A previous provider call may have finished before its result was saved; review its output first.") },
            confirmButton = { TextButton(onClick = {
                retry = null
                action { repository.control(run, "retry"); refresh() }
            }) { Text("Retry remaining steps") } },
            dismissButton = { TextButton(onClick = { retry = null }) { Text("Back") } })
    }
    SectionLabel("Server execution")
    FootNote("Runs the saved prompt. Each agent receives the previous step's saved output. Save edits before starting. Run snapshots include agent instructions and selected tools; personal Memory is not added.")
    if (!signedIn) {
        OutlinedButton(onClick = onSignIn, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Sign in to run workflows") }
        return
    }
    Text("Step 1: ${workflow.agentName.ifBlank { "Orhan" }}")
    handoffs.forEachIndexed { index, id ->
        val name = agents.firstOrNull { it.id == id }?.name ?: "Unavailable agent"
        TextButton(enabled = !busy, onClick = { handoffs = ArrayList(handoffs.filterIndexed { i, _ -> i != index }) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Step ${index + 2}: $name · Remove") }
    }
    if (handoffs.size < 7) {
        FootNote("Add the next agent in execution order. Each step uses the saved workflow prompt and its agent's instructions.")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            agents.forEach { agent ->
                FilterChip(selected = false, enabled = !busy,
                    onClick = { handoffs = ArrayList(handoffs + agent.id) }, label = { Text("Add ${agent.name}") },
                    modifier = Modifier.heightIn(min = 48.dp))
            }
        }
    }
    FootNote("Only supported read tools can run here. Legacy inactive tool preferences stay inactive; approval-dependent writes require normal Agent chat.")
    OutlinedTextField(value = schedule, onValueChange = { schedule = it.take(40) }, enabled = !busy,
        label = { Text("One-shot time (optional, UTC)") }, supportingText = { Text("Example: 2026-12-01T09:00:00Z. Blank runs now.") },
        singleLine = true, modifier = Modifier.fillMaxWidth())
    val active = runs.any { it.active }
    Button(enabled = !busy && loaded && !active && !draftChanged && historyError == null,
        onClick = { action { repository.start(workflow, agents, handoffs, schedule.trim()); refresh() } },
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(if (busy) "Waiting for service…" else "Start server run") }
    if (draftChanged) FootNote("Save your workflow edits before starting a server run.")
    if (active) FootNote("This workflow has an active run. Leaving this screen does not stop it.")
    error?.let { FootNote(it) }
    historyError?.let { FootNote("History may be out of date. $it") }
    OutlinedButton(enabled = !busy, onClick = { action { refresh() } }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Refresh server history") }
    if (error != null && loaded) {
        TextButton(enabled = !busy && !active, onClick = { action { refresh(); repository.discardPending(workflow.id) } },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Discard unconfirmed submission after refresh") }
    }
    if (loaded && runs.isEmpty()) FootNote("No server executions recorded for this workflow.")
    runs.take(20).forEach { run ->
        SectionLabel("${run.status} · attempt ${run.attempt}")
        FootNote("Run ${run.id}\nCreated ${run.createdAt}\nDue ${run.dueAt}")
        if (run.cancelRequested && run.active) FootNote("Cancellation requested; waiting for worker acknowledgement.")
        run.error?.let { FootNote(it) }
        OutlinedButton(enabled = !busy, onClick = { action {
            expanded = if (expanded == run.id) null else run.id
            refresh()
        } }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(if (expanded == run.id) "Hide saved outputs" else "View saved outputs")
        }
        run.steps.forEach { step ->
            Text("${step.agentName}: ${step.status}")
            step.error?.let { FootNote(it) }
            if (step.output.isNotBlank()) Text(step.output)
        }
        if (run.active) OutlinedButton(enabled = !busy && !run.cancelRequested,
            onClick = { action { repository.control(run, "cancel"); refresh() } }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Cancel run") }
        if (run.status == "failed" || run.status == "cancelled") OutlinedButton(enabled = !busy && !active,
            onClick = { retry = run }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Review and retry run") }
    }
}
