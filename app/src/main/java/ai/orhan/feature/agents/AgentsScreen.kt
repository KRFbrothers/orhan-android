package ai.orhan.feature.agents

import ai.orhan.R
import ai.orhan.design.icons.OrhanIcons
import ai.orhan.design.theme.OrhanTextStyles
import ai.orhan.design.theme.OrhanTheme
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import ai.orhan.data.workflows.Workflow

/** An agent the user has installed (design kit screen 03). */
data class InstalledAgent(
    val name: String,
    val blurb: String,
    val category: String,
    val working: Boolean,
    @DrawableRes val avatar: Int,
)

val SampleInstalledAgents = listOf(
    InstalledAgent("Webpage Developer", "Builds and publishes landing pages, docs and dashboards from a single prompt.", "Web", true, R.drawable.agent_webpage),
    InstalledAgent("Slides Assistant", "Turns a chat into a branded deck \u2014 outline, design and export in minutes.", "Slides", true, R.drawable.agent_slides),
    InstalledAgent("Market Research", "Researches competitors with cited sources and keeps the brief up to date.", "Research", false, R.drawable.agent_market),
    InstalledAgent("Accounting Assistant", "Reconciles invoices and flags anything that needs a human decision.", "Finance", false, R.drawable.agent_accounting),
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
    onCreateAgent: () -> Unit = {},
    /** Names of user-saved agents that may be removed. Samples are never in this set. */
    deletableNames: Set<String> = emptySet(),
    /** When null, no delete affordance is shown. */
    onDeleteAgent: ((InstalledAgent) -> Unit)? = null,
    workflows: List<Workflow> = emptyList(),
    onOpenWorkflow: (Workflow) -> Unit = {},
    onCreateWorkflow: () -> Unit = {},
    onEditWorkflow: (Workflow) -> Unit = {},
    onDeleteWorkflow: ((Workflow) -> Unit)? = null,
) {
    var pendingWorkflowDelete by remember { mutableStateOf<Workflow?>(null) }
    pendingWorkflowDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingWorkflowDelete = null },
            title = { Text("Delete ${target.title}?") },
            text = { Text("This workflow will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingWorkflowDelete = null
                    onDeleteWorkflow?.invoke(target)
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingWorkflowDelete = null }) { Text("Cancel") }
            },
        )
    }
    var pendingDelete by remember { mutableStateOf<InstalledAgent?>(null) }
    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remove ${target.name}?") },
            text = { Text("This agent will be removed from your agents.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    onDeleteAgent?.invoke(target)
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(top = 2.dp, bottom = 16.dp),
    ) {
        item(key = "head") {
            OrhanSectionLabel("Your agents", "${agents.size} active")
        }
        items(agents, key = { it.name }) { agent ->
            val canDelete = onDeleteAgent != null && agent.name in deletableNames
            AgentCard(
                agent,
                onOpen = { onOpenAgent(agent) },
                onLongPress = if (canDelete) ({ pendingDelete = agent }) else null,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }
        item(key = "new") {
            NewAgentButton(onCreateAgent, Modifier.padding(start = 12.dp, end = 12.dp, top = 9.dp))
        }
        item(key = "wf_head") {
            OrhanSectionLabel("Workflows", "${workflows.size}", Modifier.padding(top = 16.dp))
        }
        if (workflows.isEmpty()) {
            item(key = "wf_empty") {
                Text(
                    "Save a prompt you reuse often.",
                    style = OrhanTextStyles.snippet,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }
        }
        items(workflows, key = { "wf_" + it.id }) { wf ->
            WorkflowRow(
                wf,
                onOpen = { onOpenWorkflow(wf) },
                onEdit = { onEditWorkflow(wf) },
                onLongPress = if (onDeleteWorkflow != null) ({ pendingWorkflowDelete = wf }) else null,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }
        item(key = "wf_new") {
            DashedAddButton("New workflow", onCreateWorkflow, Modifier.padding(start = 12.dp, end = 12.dp, top = 9.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AgentCard(
    agent: InstalledAgent,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null,
) {
    val colors = OrhanTheme.colors
    val shapes = OrhanTheme.shapes
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.card)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, colors.hairline, shapes.card)
            .then(
                if (onLongPress != null) {
                    Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = onLongPress,
                        onLongClickLabel = "Remove agent",
                    )
                } else {
                    Modifier
                },
            )
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Image(
            painter = painterResource(agent.avatar),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)),
        )
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(agent.name, style = OrhanTextStyles.rowTitle, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                Spacer(Modifier.width(7.dp))
                Icon(OrhanIcons.Spark, contentDescription = null, tint = colors.logoMint, modifier = Modifier.size(14.dp))
            }
            Text(agent.blurb, style = OrhanTextStyles.snippet, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
            Row(Modifier.padding(top = 9.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                PillTag(if (agent.working) "Working" else "Idle", on = agent.working)
                PillTag(agent.category, on = false)
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .widthIn(min = 64.dp)
                        .clip(shapes.pill)
                        .border(1.dp, MaterialTheme.colorScheme.outline, shapes.pill)
                        .clickable(role = Role.Button, onClick = onOpen)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Open", style = OrhanTextStyles.meta, color = colors.onBrandSoft)
                }
            }
        }
    }
}

@Composable
internal fun PillTag(text: String, on: Boolean) {
    val colors = OrhanTheme.colors
    Text(
        text = text,
        style = OrhanTextStyles.meta,
        color = if (on) colors.onBrandSoft else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(OrhanTheme.shapes.pill)
            .background(if (on) colors.brandSoft else MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 9.dp, vertical = 4.dp),
    )
}

@Composable
private fun NewAgentButton(onClick: () -> Unit, modifier: Modifier = Modifier) =
    DashedAddButton("Create a new agent", onClick, modifier)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WorkflowRow(
    wf: Workflow,
    onOpen: () -> Unit,
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
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
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
            Text(wf.title, style = OrhanTextStyles.rowTitle, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            Text(
                wf.content.replace('\n', ' '),
                style = OrhanTextStyles.snippet,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                wf.agentName.ifBlank { "Default" },
                style = OrhanTextStyles.meta,
                color = colors.onBrandSoft,
                modifier = Modifier.padding(top = 5.dp),
            )
        }
        TextButton(onClick = onEdit, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("Edit", style = OrhanTextStyles.meta)
        }
    }
}

@Composable
private fun DashedAddButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = OrhanTheme.colors
    val shapes = OrhanTheme.shapes
    val outline = MaterialTheme.colorScheme.outline
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clip(shapes.card)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawRoundRect(
                color = outline,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx()),
                style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
            )
        }
        Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(colors.brandSoft), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = colors.onBrandSoft, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(label, style = OrhanTextStyles.rowTitle, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
