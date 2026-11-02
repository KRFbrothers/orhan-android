package ai.orhan.feature.agents

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ai.orhan.data.agents.AgentSaveResult
import ai.orhan.data.chat.AgentToolCapability
import ai.orhan.data.chat.ChatRepository
import ai.orhan.data.agents.MAX_AGENT_BLURB
import ai.orhan.data.agents.MAX_AGENT_INSTRUCTIONS
import ai.orhan.data.agents.MAX_AGENT_NAME
import ai.orhan.data.agents.SavedAgent
import ai.orhan.data.agents.agentAvatarFiles
import kotlinx.coroutines.launch
import ai.orhan.feature.settings.detail.FootNote
import ai.orhan.feature.settings.detail.SectionLabel
import ai.orhan.feature.settings.detail.SettingsDetailScaffold

/** Create or edit an agent. [existing] null means create. Saved locally via AgentStore. */
@Composable
fun CreateAgentScreen(
    onBack: () -> Unit,
    existing: SavedAgent? = null,
    repository: ChatRepository,
    /** Other saved names. The agent being edited may keep its own name. */
    takenNames: Set<String> = emptySet(),
    /** Returns the store result. A failure must leave this screen's fields as the user typed them. */
    onCreated: suspend (name: String, blurb: String, instructions: String, tools: List<String>, toolsRestricted: Boolean, avatarFileName: String?) -> AgentSaveResult,
) {
    var name by rememberSaveable(existing?.name) { mutableStateOf(existing?.name.orEmpty()) }
    var blurb by rememberSaveable(existing?.name) { mutableStateOf(existing?.blurb.orEmpty()) }
    var instructions by rememberSaveable(existing?.name) { mutableStateOf(existing?.instructions.orEmpty()) }
    var toolsRestricted by rememberSaveable(existing?.id) { mutableStateOf(existing?.toolsRestricted ?: false) }
    var selectedTools by rememberSaveable(existing?.id) { mutableStateOf(existing?.tools.orEmpty().toSet()) }
    var capabilities by remember(existing?.id) { mutableStateOf<List<AgentToolCapability>?>(null) }
    var capabilityError by remember(existing?.id) { mutableStateOf<String?>(null) }
    var avatarFileName by rememberSaveable(existing?.name) { mutableStateOf(existing?.avatarFileName) }
    var showError by rememberSaveable { mutableStateOf(false) }
    var showTaken by rememberSaveable { mutableStateOf(false) }
    var saveError by rememberSaveable { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val avatarFiles = remember(context) { context.agentAvatarFiles() }
    LaunchedEffect(existing?.id) {
        repository.agentToolCapabilities().onSuccess { capabilities = it }
            .onFailure { capabilityError = it.message ?: "Agent tools could not be loaded." }
    }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            avatarFileName = avatarFiles.copyFrom(context.contentResolver, uri)
                ?: run { saveError = "Couldn't save that image. Choose a smaller valid image."; avatarFileName }
        }
    }

    SettingsDetailScaffold(title = if (existing == null) "Create agent" else "Edit agent", onBack = onBack) {
        SectionLabel("Identity")
        AgentAvatarImage(avatarFiles.path(avatarFileName), ai.orhan.R.drawable.agent_webpage, 80.dp, "Agent avatar")
        OutlinedButton(onClick = { avatarPicker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(if (avatarFileName == null) "Choose avatar" else "Change avatar")
        }
        if (avatarFileName != null) TextButton(onClick = { avatarFileName = null }) { Text("Remove avatar") }
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it.take(MAX_AGENT_NAME)
                showError = false
                showTaken = false
                saveError = null
            },
            label = { Text("Name") },
            singleLine = true,
            isError = showError || showTaken,
            supportingText = when {
                showError -> {
                    { Text("Name is required") }
                }
                showTaken -> {
                    { Text("An agent with this name already exists") }
                }
                else -> null
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = blurb,
            onValueChange = {
                blurb = it.take(MAX_AGENT_BLURB)
                saveError = null
            },
            label = { Text("Short blurb") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        SectionLabel("Instructions")
        OutlinedTextField(
            value = instructions,
            onValueChange = {
                instructions = it.take(MAX_AGENT_INSTRUCTIONS)
                saveError = null
            },
            label = { Text("System instructions (optional)") },
            minLines = 4,
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
        )
        SectionLabel("Tools")
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Switch(checked = toolsRestricted, onCheckedChange = { toolsRestricted = it })
            Spacer(Modifier.width(8.dp))
            Text("Restrict this agent to selected tools", style = MaterialTheme.typography.bodyMedium)
        }
        FootNote(if (toolsRestricted) "Only checked, available tools can run. An empty selection disables every tool." else "Restriction is off. Legacy agents keep the service's default tool behavior.")
        if (toolsRestricted) {
            if (capabilities == null) {
                FootNote(capabilityError ?: "Loading tools from the signed-in service…")
            } else {
                capabilities.orEmpty().forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(
                            checked = item.id in selectedTools,
                            enabled = item.available || item.id in selectedTools,
                            onCheckedChange = { checked -> selectedTools = if (checked) selectedTools + item.id else selectedTools - item.id },
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(toolLabel(item.id), style = MaterialTheme.typography.bodyMedium)
                    }
                    if (!item.available) FootNote(item.reason ?: "Unavailable in the current service configuration.")
                }
                val knownIds = capabilities.orEmpty().map { it.id }.toSet()
                (selectedTools - knownIds).sorted().forEach { id ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(checked = true, onCheckedChange = { selectedTools = selectedTools - id })
                        Text("$id — unavailable", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    }
                    FootNote("This saved tool is no longer in the service catalog. Uncheck it to remove it.")
                }
            }
        }
        if (existing != null) {
            FootNote("Renaming does not move chats saved under the previous name.")
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                val trimmed = name.trim().take(MAX_AGENT_NAME).trim()
                val blurbCapped = blurb.trim().take(MAX_AGENT_BLURB)
                val instructionsCapped = instructions.trim().take(MAX_AGENT_INSTRUCTIONS)
                val taken = takenNames.any { it.equals(trimmed, ignoreCase = true) && !it.equals(existing?.name, ignoreCase = true) }
                when {
                    trimmed.isBlank() -> showError = true
                    taken -> showTaken = true
                    toolsRestricted && capabilities == null -> saveError = capabilityError
                        ?: "Sign in and connect to a service that supports tool restrictions before saving this selection."
                    toolsRestricted && selectedTools.any { id -> capabilities?.none { it.id == id && it.available } == true } ->
                        saveError = "Remove selected tools marked unavailable before saving."
                    else -> scope.launch {
                        saving = true
                        val result = onCreated(trimmed, blurbCapped, instructionsCapped, selectedTools.sorted(), toolsRestricted, avatarFileName)
                        saving = false
                        when (result) {
                            is AgentSaveResult.Saved -> Unit
                            is AgentSaveResult.Rejected -> saveError = result.message
                            is AgentSaveResult.Failed -> saveError = result.message
                        }
                    }
                }
            },
            enabled = !saving,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { Text(if (existing == null) "Create" else "Save") }
        if (saveError != null) {
            Spacer(Modifier.height(8.dp))
            Text(saveError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun toolLabel(id: String): String = when (id) {
    "google_search" -> "Web search"
    "fetch_url" -> "Open web pages"
    "search_documents" -> "Search files"
    "generate_image" -> "Create images"
    "remember", "recall" -> "Memory suggestions and lookup"
    "current_time" -> "Current time"
    else -> id.replace('_', ' ').replaceFirstChar { it.uppercase() }
}
