package ai.orhan.feature.agents

import ai.orhan.R
import ai.orhan.design.icons.OrhanIcons
import ai.orhan.design.theme.OrhanTextStyles
import ai.orhan.design.theme.OrhanTheme
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(top = 2.dp, bottom = 16.dp),
    ) {
        item(key = "head") {
            OrhanSectionLabel("Your agents", "${agents.size} active")
        }
        items(agents, key = { it.name }) { agent ->
            AgentCard(agent, onOpen = { onOpenAgent(agent) }, modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp))
        }
        item(key = "new") {
            NewAgentButton(onCreateAgent, Modifier.padding(start = 12.dp, end = 12.dp, top = 9.dp))
        }
    }
}

@Composable
private fun AgentCard(agent: InstalledAgent, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val colors = OrhanTheme.colors
    val shapes = OrhanTheme.shapes
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.card)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, colors.hairline, shapes.card)
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
private fun NewAgentButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
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
            Text("Create a new agent", style = OrhanTextStyles.rowTitle, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
