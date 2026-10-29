package ai.orhan.feature.agents

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ai.orhan.feature.settings.detail.FootNote
import ai.orhan.feature.settings.detail.SectionLabel
import ai.orhan.feature.settings.detail.SettingsDetailScaffold

private val AgentTools = listOf("Web", "Files", "Code", "Image")

/** Create agent: name it, give it tools, done. Saved locally via AgentStore. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateAgentScreen(
    onBack: () -> Unit,
    onCreated: (name: String, blurb: String, instructions: String, tools: List<String>) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var blurb by rememberSaveable { mutableStateOf("") }
    var instructions by rememberSaveable { mutableStateOf("") }
    var tools by rememberSaveable { mutableStateOf(setOf<String>()) }
    var showError by rememberSaveable { mutableStateOf(false) }

    SettingsDetailScaffold(title = "Create agent", onBack = onBack) {
        SectionLabel("Identity")
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; if (it.isNotBlank()) showError = false },
            label = { Text("Name") },
            singleLine = true,
            isError = showError,
            supportingText = if (showError) { { Text("Name is required") } } else null,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = blurb,
            onValueChange = { blurb = it },
            label = { Text("Short blurb") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        SectionLabel("Instructions")
        OutlinedTextField(
            value = instructions,
            onValueChange = { instructions = it },
            label = { Text("System instructions (optional)") },
            minLines = 4,
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
        )
        SectionLabel("Tools")
        FlowRow(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            AgentTools.forEach { tool ->
                FilterChip(
                    selected = tool in tools,
                    onClick = { tools = if (tool in tools) tools - tool else tools + tool },
                    label = { Text(tool) },
                )
            }
        }
        FootNote("Selected tools guide what this agent should use. The shared engine still has its full tool set.")
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                if (name.isBlank()) showError = true else onCreated(name.trim(), blurb.trim(), instructions.trim(), AgentTools.filter { it in tools })
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { Text("Create") }
    }
}
