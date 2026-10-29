package ai.orhan.feature.discover

import ai.orhan.R
import ai.orhan.design.theme.OrhanTextStyles
import ai.orhan.design.theme.OrhanTheme
import ai.orhan.feature.agents.OrhanSectionLabel
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Fixed brand hero colours from the design kit (`.discover-feature`), identical in day + night.
private val HeroGradient = Brush.linearGradient(listOf(Color(0xFF0B8249), Color(0xFF087C45), Color(0xFF06512F)))
private val HeroInk = Color(0xFFEAFFF2)
private val HeroDeep = Color(0xFF06512F)
private val HeroGlow = Color(0xFF42D982)

@Composable
fun DiscoverScreen(
    modifier: Modifier = Modifier,
    playbooks: List<Playbook> = StarterPlaybooks,
    onOpenPlaybook: (Playbook) -> Unit = {},
) {
    val featured = playbooks.firstOrNull()
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(top = 2.dp, bottom = 16.dp)) {
        if (featured != null) {
            item(key = "feature") {
                FeaturedCard(featured, { onOpenPlaybook(featured) }, Modifier.padding(start = 12.dp, end = 12.dp, bottom = 16.dp))
            }
        }
        item(key = "playbooks-head") { OrhanSectionLabel("Starter playbooks", playbooks.size.toString()) }
        items(playbooks.chunked(2), key = { row -> row.first().id }) { row ->
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { p -> PlaybookCard(p, { onOpenPlaybook(p) }, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FeaturedCard(playbook: Playbook, onStart: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 176.dp)
            .clip(shape)
            .background(HeroGradient),
    ) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = (-50).dp)
                .size(200.dp)
                .background(Brush.radialGradient(listOf(HeroGlow.copy(alpha = 0.55f), Color.Transparent)), CircleShape),
        )
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painterResource(R.drawable.orhan_mark), contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(13.dp))
                        .border(2.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(13.dp)),
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("TRY THIS PLAYBOOK", style = OrhanTextStyles.eyebrow, color = HeroInk.copy(alpha = 0.9f))
                    Text(playbook.title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, fontSize = 19.sp), color = HeroInk)
                }
            }
            Text(
                playbook.blurb,
                style = OrhanTextStyles.snippet, color = HeroInk,
                modifier = Modifier.padding(top = 12.dp).fillMaxWidth(0.8f),
            )
            Row(
                Modifier.padding(top = 12.dp).heightIn(min = 48.dp).clip(OrhanTheme.shapes.pill).background(HeroInk)
                    .clickable(role = Role.Button, onClick = onStart).padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Use playbook", style = OrhanTextStyles.button.copy(fontWeight = FontWeight.Black), color = HeroDeep)
                Spacer(Modifier.width(7.dp))
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = HeroDeep, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun PlaybookCard(playbook: Playbook, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = OrhanTheme.colors
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .heightIn(min = 136.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, colors.hairline, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(playbook.tag.uppercase(), style = OrhanTextStyles.eyebrow, color = colors.onBrandSoft)
        Text(playbook.title, style = OrhanTextStyles.rowTitle.copy(fontSize = 13.5.sp), color = MaterialTheme.colorScheme.onSurface)
        Text(playbook.blurb, style = OrhanTextStyles.meta.copy(fontWeight = FontWeight.Normal), color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (playbook.agentName.isNotBlank()) {
            Spacer(Modifier.weight(1f, fill = false))
            Text(playbook.agentName, style = OrhanTextStyles.meta, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
