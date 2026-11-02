package ai.orhan.feature.agents

import ai.orhan.R
import ai.orhan.data.workflows.Workflow
import ai.orhan.design.theme.OrhanTextStyles
import ai.orhan.design.theme.OrhanTheme
import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** An agent the user has installed (design kit screen 03). */
data class InstalledAgent(
    val name: String,
    val blurb: String,
    val category: String,
    val working: Boolean,
    @DrawableRes val avatar: Int,
    val avatarPath: String? = null,
)

/** The single built-in agent so a new chat always has a target. User agents are added on top. */
val SampleInstalledAgents = listOf(
    InstalledAgent("Orhan", "Your general-purpose assistant.", "General", false, R.drawable.agent_webpage),
)

/** Section label row: "YOUR AGENTS ............ 4 active" */
@Composable
internal fun OrhanSectionLabel(title: String, trailing: String? = null, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title.uppercase(), style = OrhanTextStyles.kicker, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        if (trailing != null) {
            Text(trailing, style = OrhanTextStyles.meta, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AgentsScreen(
    modifier: Modifier = Modifier,
    agents: List<InstalledAgent> = SampleInstalledAgents,
    onOpenAgent: (InstalledAgent) -> Unit = {},
    onViewAgent: (InstalledAgent) -> Unit = {},
    onCreateAgent: () -> Unit = {},
    /** Opens the editor for a user-saved agent. Samples are not editable. */
    onEditAgent: ((InstalledAgent) -> Unit)? = null,
    /** Names of user-saved agents that may be removed. Samples are never in this set. */
    deletableNames: Set<String> = emptySet(),
    /** When null, no delete affordance is shown. */
    onDeleteAgent: (suspend (InstalledAgent) -> Boolean)? = null,
    workflows: List<Workflow> = emptyList(),
    onOpenWorkflow: (Workflow) -> Unit = {},
    onRunWorkflow: (Workflow) -> Unit = {},
    onCreateWorkflow: () -> Unit = {},
    onEditWorkflow: (Workflow) -> Unit = {},
    onDeleteWorkflow: (suspend (Workflow) -> Boolean)? = null,
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var pendingWorkflowDelete by remember { mutableStateOf<Workflow?>(null) }
    var workflowDeleteError by remember { mutableStateOf<String?>(null) }
    var deletingWorkflow by remember { mutableStateOf(false) }
    pendingWorkflowDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { if (!deletingWorkflow) { pendingWorkflowDelete = null; workflowDeleteError = null } },
            title = { Text("Delete ${target.title}?") },
            text = { Text(workflowDeleteError ?: "This workflow will be removed.") },
            confirmButton = {
                TextButton(enabled = !deletingWorkflow, onClick = {
                    if (!deletingWorkflow) scope.launch {
                        deletingWorkflow = true
                        val removed = try { onDeleteWorkflow?.invoke(target) == true }
                        catch (cancel: CancellationException) { throw cancel }
                        catch (_: Exception) { false }
                        finally { deletingWorkflow = false }
                        if (removed) { pendingWorkflowDelete = null; workflowDeleteError = null }
                        else workflowDeleteError = "Couldn't delete this workflow. Your saved workflow is unchanged."
                    }
                }) { Text(if (deletingWorkflow) "Deleting…" else "Delete") }
            },
            dismissButton = {
                TextButton(enabled = !deletingWorkflow, onClick = { pendingWorkflowDelete = null; workflowDeleteError = null }) { Text("Cancel") }
            },
        )
    }
    var pendingDelete by remember { mutableStateOf<InstalledAgent?>(null) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf(false) }
    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remove ${target.name}?") },
            text = { Text(deleteError ?: "This agent will be removed from your agents.") },
            confirmButton = {
                TextButton(onClick = {
                    if (!deleting) scope.launch {
                        deleting = true
                        val removed = try { onDeleteAgent?.invoke(target) == true } catch (_: Exception) { false }
                        deleting = false
                        if (removed) { pendingDelete = null; deleteError = null }
                        else deleteError = "Couldn't remove this agent. Your saved agent is unchanged."
                    }
                }, enabled = !deleting) { Text(if (deleting) "Removing…" else "Remove") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null; deleteError = null }) { Text("Cancel") }
            },
        )
    }

    var section by rememberSaveable { mutableIntStateOf(0) }
    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionTab("Agents", selected = section == 0, onClick = { section = 0 })
            SectionTab("Workflows", selected = section == 1, onClick = { section = 1 })
        }
        if (section == 0) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
            ) {
                if (agents.isEmpty()) {
                    item(key = "agents_empty") {
                        Text(
                            "No agents yet.",
                            style = OrhanTextStyles.snippet,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        )
                    }
                }
                items(agents, key = { it.name }) { agent ->
                    val canDelete = onDeleteAgent != null && agent.name in deletableNames
                    AgentCard(
                        agent,
                        onOpen = { onOpenAgent(agent) },
                        onProfile = { onViewAgent(agent) },
                        onEdit = if (canDelete && onEditAgent != null) ({ onEditAgent(agent) }) else null,
                        onLongPress = if (canDelete) ({ pendingDelete = agent }) else null,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    )
                }
                item(key = "new_agent") {
                    AddRow(
                        "Create a new agent",
                        onCreateAgent,
                        Modifier.padding(start = 12.dp, end = 12.dp, top = 9.dp),
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
            ) {
                if (workflows.isEmpty()) {
                    item(key = "wf_empty") {
                        Text(
                            "Save a prompt you reuse often.",
                            style = OrhanTextStyles.snippet,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        )
                    }
                }
                items(workflows, key = { "wf_" + it.id }) { wf ->
                    WorkflowRow(
                        wf,
                        onOpen = { onOpenWorkflow(wf) },
                        onRun = { onRunWorkflow(wf) },
                        onEdit = { onEditWorkflow(wf) },
                        onLongPress = if (onDeleteWorkflow != null) ({ workflowDeleteError = null; pendingWorkflowDelete = wf }) else null,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    )
                }
                item(key = "wf_new") {
                    AddRow(
                        "New workflow",
                        onCreateWorkflow,
                        Modifier.padding(start = 12.dp, end = 12.dp, top = 9.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Text(
            text = label,
            style = OrhanTextStyles.rowTitle,
            color = if (selected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .height(2.dp)
                .width(28.dp)
                .background(
                    if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                ),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AgentCard(
    agent: InstalledAgent,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null,
    onProfile: () -> Unit = {},
    onLongPress: (() -> Unit)? = null,
) {
    val colors = OrhanTheme.colors
    val shapes = OrhanTheme.shapes
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.card)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, colors.hairline, shapes.card)
            .combinedClickable(
                role = Role.Button,
                onClickLabel = "Chat with ${agent.name}",
                onClick = onOpen,
                onLongClick = { onLongPress?.invoke() },
                onLongClickLabel = if (onLongPress != null) "Remove agent" else null,
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AgentAvatarImage(agent.avatarPath, agent.avatar, 48.dp, null)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                agent.name,
                style = OrhanTextStyles.rowTitle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                agent.blurb,
                style = OrhanTextStyles.snippet,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        if (onEdit != null) {
            TextButton(onClick = onEdit, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Edit", style = OrhanTextStyles.meta)
            }
        }
        TextButton(onClick = onProfile, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("Profile", style = OrhanTextStyles.meta)
        }
        Box(
            modifier = Modifier
                .heightIn(min = 40.dp)
                .widthIn(min = 72.dp)
                .clip(shapes.pill)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(role = Role.Button, onClick = onOpen)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Chat", style = OrhanTextStyles.meta, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
internal fun AgentAvatarImage(path: String?, fallback: Int, size: Dp, description: String?) {
    var bitmap by remember(path) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(path) {
        bitmap = if (path == null) null else withContext(Dispatchers.IO) {
            BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = 4 })?.asImageBitmap()
        }
    }
    if (bitmap != null) {
        Image(bitmap!!, description, contentScale = ContentScale.Crop, modifier = Modifier.size(size).clip(CircleShape))
    } else {
        Image(painterResource(fallback), description, contentScale = ContentScale.Crop, modifier = Modifier.size(size).clip(CircleShape))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WorkflowRow(
    wf: Workflow,
    onOpen: () -> Unit,
    onRun: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null,
) {
    val colors = OrhanTheme.colors
    val shapes = OrhanTheme.shapes
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.card)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, colors.hairline, shapes.card)
            .combinedClickable(
                onClick = onOpen,
                onClickLabel = "Open workflow",
                onLongClick = onLongPress,
                onLongClickLabel = if (onLongPress != null) "Delete workflow" else null,
            )
            .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                wf.title,
                style = OrhanTextStyles.rowTitle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                wf.content.replace('\n', ' '),
                style = OrhanTextStyles.snippet,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
            if (wf.agentName.isNotBlank()) {
                Text(
                    wf.agentName,
                    style = OrhanTextStyles.meta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        TextButton(onClick = onRun, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("Run", style = OrhanTextStyles.meta)
        }
        TextButton(onClick = onEdit, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("Edit", style = OrhanTextStyles.meta)
        }
    }
}

@Composable
private fun AddRow(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = OrhanTheme.colors
    val shapes = OrhanTheme.shapes
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(shapes.card)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, colors.hairline, shapes.card)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(colors.brandSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = null,
                tint = colors.onBrandSoft,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(label, style = OrhanTextStyles.rowTitle, color = MaterialTheme.colorScheme.onSurface)
    }
}
