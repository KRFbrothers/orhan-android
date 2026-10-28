package ai.orhan.feature.discover

import ai.orhan.R
import ai.orhan.design.theme.OrhanTextStyles
import ai.orhan.design.theme.OrhanTheme
import ai.orhan.feature.agents.OrhanSectionLabel
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
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class CommunityAgent(
    val name: String,
    val subtitle: String,
    val tag: String,
    @DrawableRes val avatar: Int?,
)

data class SocialActivity(
    val person: String,
    val action: String,
    val meta: String,
    @DrawableRes val avatar: Int,
)

val SampleTrending = listOf(
    CommunityAgent("Slides Assistant", "Deck in 5 prompts \u00b7 2.1k users", "Productivity", R.drawable.agent_slides),
    CommunityAgent("Market Research", "Cited briefs \u00b7 1.4k users", "Research", R.drawable.agent_market),
    CommunityAgent("Accounting Assistant", "Invoice triage \u00b7 980 users", "Finance", R.drawable.agent_accounting),
    CommunityAgent("Design Critic", "M3 + a11y review \u00b7 760 users", "Design", null),
)

val SampleSocial = listOf(
    SocialActivity("Alice Chen", "shared Webpage Developer with the Design Team", "12 minutes ago \u00b7 3 replies", R.drawable.user_alice),
    SocialActivity("David Park", "published a research agent with web + arXiv tools", "3 hours ago \u00b7 41 installs", R.drawable.user_david),
    SocialActivity("Alex Rivera", "rated Slides Assistant 5 stars", "Yesterday \u00b7 \u201cdeck looked on-brand in one shot\u201d", R.drawable.user_alex),
)

// Fixed brand hero colours from the design kit (`.discover-feature`), identical in day + night.
private val HeroGradient = Brush.linearGradient(listOf(Color(0xFF0B8249), Color(0xFF087C45), Color(0xFF06512F)))
private val HeroInk = Color(0xFFEAFFF2)
private val HeroDeep = Color(0xFF06512F)
private val HeroGlow = Color(0xFF42D982)

@Composable
fun DiscoverScreen(
    modifier: Modifier = Modifier,
    trending: List<CommunityAgent> = SampleTrending,
    social: List<SocialActivity> = SampleSocial,
    onStartFeatured: () -> Unit = {},
    onAddAgent: (CommunityAgent) -> Unit = {},
    onOpenActivity: (SocialActivity) -> Unit = {},
) {
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(top = 2.dp, bottom = 16.dp)) {
        item(key = "feature") { FeaturedCard(onStartFeatured, Modifier.padding(start = 12.dp, end = 12.dp, bottom = 16.dp)) }
        item(key = "trending-head") { OrhanSectionLabel("Trending in community", "24") }
        items(trending.chunked(2), key = { row -> row.first().name }) { row ->
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { a -> TrendingCard(a, { onAddAgent(a) }, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        item(key = "social-head") { OrhanSectionLabel("From people you follow", modifier = Modifier.padding(top = 9.dp)) }
        items(social, key = { it.person }) { s ->
            SocialRow(s, { onOpenActivity(s) }, Modifier.padding(horizontal = 8.dp))
        }
    }
}

@Composable
private fun FeaturedCard(onStart: () -> Unit, modifier: Modifier = Modifier) {
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
                    Text("FEATURED AGENT", style = OrhanTextStyles.eyebrow, color = HeroInk.copy(alpha = 0.9f))
                    Text("Webpage Developer", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, fontSize = 19.sp), color = HeroInk)
                }
            }
            Text(
                "Prompt a full landing page and get a published link in under two minutes.",
                style = OrhanTextStyles.snippet, color = HeroInk,
                modifier = Modifier.padding(top = 12.dp).fillMaxWidth(0.8f),
            )
            Row(
                Modifier.padding(top = 12.dp).heightIn(min = 48.dp).clip(OrhanTheme.shapes.pill).background(HeroInk)
                    .clickable(role = Role.Button, onClick = onStart).padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Start a chat", style = OrhanTextStyles.button.copy(fontWeight = FontWeight.Black), color = HeroDeep)
                Spacer(Modifier.width(7.dp))
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = HeroDeep, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun TrendingCard(agent: CommunityAgent, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = OrhanTheme.colors
    val shape = RoundedCornerShape(18.dp)
    val avatarShape = RoundedCornerShape(13.dp)
    Column(
        modifier
            .heightIn(min = 136.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, colors.hairline, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (agent.avatar != null) {
            Image(painterResource(agent.avatar), null, contentScale = ContentScale.Crop, modifier = Modifier.size(40.dp).clip(avatarShape))
        } else {
            Box(Modifier.size(40.dp).clip(avatarShape).background(HeroGradient), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Palette, null, tint = HeroInk, modifier = Modifier.size(20.dp))
            }
        }
        Text(agent.name, style = OrhanTextStyles.rowTitle.copy(fontSize = 13.5.sp), color = MaterialTheme.colorScheme.onSurface)
        Text(agent.subtitle, style = OrhanTextStyles.meta.copy(fontWeight = FontWeight.Normal), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f, fill = false))
        Text(agent.tag.uppercase(), style = OrhanTextStyles.eyebrow, color = colors.onBrandSoft)
    }
}

@Composable
private fun SocialRow(s: SocialActivity, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    Row(
        modifier.fillMaxWidth().heightIn(min = 64.dp).clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(s.avatar), null, contentScale = ContentScale.Crop, modifier = Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)))
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = onSurface, fontWeight = FontWeight.Bold)) { append(s.person) }
                    append(" ")
                    append(s.action)
                },
                style = OrhanTextStyles.snippet, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(s.meta, style = OrhanTextStyles.meta.copy(fontWeight = FontWeight.Normal), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
