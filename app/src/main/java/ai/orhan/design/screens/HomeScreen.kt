package ai.orhan.design.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ai.orhan.R
import ai.orhan.design.theme.OrhanTextStyles
import ai.orhan.design.theme.OrhanTheme
import ai.orhan.design.theme.ThemeMode

/* ============================================================================
 *  Orhan AI \u2014 Home / Chats  (Material 3, 390dp base)
 *
 *  Token usage \u2014 nothing here invents a value:
 *      MaterialTheme.colorScheme  M3 slots: background, surface, onSurface,
 *                                 onSurfaceVariant, outline, primary, onPrimary
 *      OrhanTheme.colors        brand extras: brandSoft, onBrandSoft, logoMint,
 *                                 field, fieldEdge, hairline, navIndicator,
 *                                 navIndicatorInk, navInactiveInk, navBackground,
 *                                 primaryGradient, onGradient
 *      OrhanTheme.shapes        card 20, avatarList 17, avatarHeader 14,
 *                                 statTile 18, chip/pill/navIndicator (full),
 *                                 fab 19
 *      OrhanTextStyles          rowTitle, snippet, meta, kicker, eyebrow,
 *                                 statNumber, navLabel, button
 *
 *  Icons used: Add, Chat, Person, Search, Menu, KeyboardArrowRight and Star are
 *  in the core icon set; SmartToy, Explore, DarkMode and LightMode ship in
 *  material-icons-extended. Add
 *      implementation("androidx.compose.material:material-icons-extended")
 *  or swap those four for your own drawables.
 * ==========================================================================*/

/* --------------------------------- data ---------------------------------- */

/**
 * One chat-list row.
 * [members] > 0 marks a group; [unread] > 0 shows the badge; [working] puts the
 * mint live dot on the avatar.
 */
data class OrhanChat(
    val title: String,
    val snippet: String,
    val time: String,
    val isAgent: Boolean,
    val members: Int = 0,
    val unread: Int = 0,
    val working: Boolean = false,
)

/** An agent card \u2014 name, what it does, its current state. */
data class OrhanAgent(
    val name: String,
    val role: String,
    val blurb: String,
    val status: String,
    val working: Boolean,
)

/** Chat list, verbatim from the shipped mockup. */
private val SampleChats = listOf(
    OrhanChat(
        title = "Webpage Developer",
        snippet = "Landing page draft is ready \u2014 want me to publish it?",
        time = "2m",
        isAgent = true,
        unread = 1,
        working = true,
    ),
    OrhanChat(
        title = "Design Team",
        snippet = "Alice: the new M3 tokens look great",
        time = "12m",
        isAgent = false,
        members = 5,
        unread = 3,
    ),
    OrhanChat(
        title = "Slides Assistant",
        snippet = "Q4 deck exported \u2014 12 slides, brand theme applied",
        time = "26m",
        isAgent = true,
        working = true,
    ),
    OrhanChat(
        title = "David Park",
        snippet = "Sent you the repo link for review",
        time = "1h",
        isAgent = false,
        unread = 1,
    ),
    OrhanChat(
        title = "Market Research",
        snippet = "Competitor brief finished \u2014 8 sources cited",
        time = "Yest.",
        isAgent = true,
    ),
    OrhanChat(
        title = "Accounting Assistant",
        snippet = "Invoice #2211 flagged for your review",
        time = "Yest.",
        isAgent = true,
    ),
    OrhanChat(
        title = "Family",
        snippet = "Mum: dinner at 8? Alex is bringing dessert",
        time = "Sun",
        isAgent = false,
        members = 8,
    ),
)

private val SampleAgents = listOf(
    OrhanAgent(
        name = "Webpage Developer",
        role = "Web",
        blurb = "Builds and publishes landing pages, docs and dashboards from a single prompt.",
        status = "Working",
        working = true,
    ),
    OrhanAgent(
        name = "Slides Assistant",
        role = "Slides",
        blurb = "Turns a chat into a branded deck \u2014 outline, design and export in minutes.",
        status = "Working",
        working = true,
    ),
    OrhanAgent(
        name = "Market Research",
        role = "Research",
        blurb = "Researches competitors with cited sources and keeps the brief up to date.",
        status = "Idle",
        working = false,
    ),
    OrhanAgent(
        name = "Accounting Assistant",
        role = "Finance",
        blurb = "Reconciles invoices and flags anything that needs a human decision.",
        status = "Idle",
        working = false,
    ),
)

private val ChatFilters = listOf("All", "Unread", "Groups", "Recent")

/** Bottom-navigation destinations, in the order the design uses them. */
enum class OrhanTab(val label: String, val icon: ImageVector) {
    Chats("Chats", Icons.Rounded.Chat),
    Agents("Agents", Icons.Rounded.SmartToy),
    Discover("Discover", Icons.Rounded.Explore),
    Profile("Profile", Icons.Rounded.Person),
}

/* -------------------------------- helpers -------------------------------- */

/** "Webpage Developer" -> "WD", "Family" -> "F". Shared with ChatScreen.kt. */
internal fun initialsOf(name: String): String =
    name.split(' ')
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString(separator = "") { it.first().uppercase() }

/** Thin hairline card used by every list group on these screens. */
@Composable
internal fun OrhanCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = OrhanTheme.colors
    val shapes = OrhanTheme.shapes
    Box(
        modifier = modifier
            .clip(shapes.card)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, colors.hairline, shapes.card),
    ) { content() }
}

/* ------------------------------ entry point ------------------------------ */

/**
 * Home / Chats.
 *
 * @param onOpenChat fires when a row or the FAB is tapped; the host switches to
 *   [ChatScreen] and passes the title through.
 * @param onOpenSettings fires from the overflow menu.
 * @param onToggleTheme flips [ThemeMode] between Light and Dark at the app root.
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onOpenChat: (OrhanChat) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onToggleTheme: () -> Unit = {},
    isDark: Boolean = isSystemInDarkTheme(),
) {
    var tab by rememberSaveable { mutableStateOf(OrhanTab.Chats) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        HomeTopBar(
            isDark = isDark,
            onToggleTheme = onToggleTheme,
            onOpenSettings = onOpenSettings,
        )

        Box(modifier = Modifier.weight(1f)) {
            when (tab) {
                OrhanTab.Chats -> ChatsTab(
                    onOpenChat = onOpenChat,
                    modifier = Modifier.fillMaxSize(),
                )

                OrhanTab.Agents -> AgentsTab(modifier = Modifier.fillMaxSize())
                OrhanTab.Discover -> DiscoverTab(modifier = Modifier.fillMaxSize())
                OrhanTab.Profile -> ProfileTab(modifier = Modifier.fillMaxSize())
            }

            if (tab == OrhanTab.Chats) {
                NewChatFab(
                    onClick = { onOpenChat(SampleChats.first()) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp),
                )
            }
        }

        OrhanBottomBar(selected = tab, onSelect = { tab = it })
    }
}

/* -------------------------------- top bar -------------------------------- */

@Composable
private fun HomeTopBar(
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(start = 16.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The real Orhan wordmark \u2014 no placeholder text.
        // Drop orhan_wordmark.png (dark ink) and orhan_wordmark_dark.png
        // (mint, for night mode) into app/src/main/res/drawable-xxhdpi/.
        Icon(
            painter = painterResource(
                if (isDark) R.drawable.orhan_wordmark_dark else R.drawable.orhan_wordmark,
            ),
            contentDescription = "Orhan AI",
            tint = Color.Unspecified, // keep the PNG's own brand colours
            modifier = Modifier
                .height(26.dp)
                .widthIn(max = 132.dp),
        )

        Spacer(modifier = Modifier.weight(1f))

        IconButton(onClick = { /* the search field is the first list row */ }) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onToggleTheme) {
            Icon(
                imageVector = if (isDark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                contentDescription = "Switch theme",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onOpenSettings) {
            Icon(
                imageVector = Icons.Rounded.Menu,
                contentDescription = "Open settings",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/* ------------------------------- Chats tab ------------------------------- */

@Composable
private fun ChatsTab(
    onOpenChat: (OrhanChat) -> Unit,
    modifier: Modifier = Modifier,
) {
    var filter by rememberSaveable { mutableStateOf(0) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(top = 4.dp, bottom = 88.dp),
    ) {
        item(key = "search") {
            SearchField(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }

        item(key = "filters") {
            // M3 SingleChoiceSegmentedButtonRow rather than a row of chips: the
            // four options are mutually exclusive and always one-of, which is
            // exactly what the segmented-button container expresses. It also
            // brings the group's own 1dp outline, the shared divider between
            // options, and Role.RadioButton semantics for TalkBack for free.
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                ChatFilters.forEachIndexed { i, label ->
                    SegmentedButton(
                        selected = filter == i,
                        onClick = { filter = i },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = i,
                            count = ChatFilters.size,
                        ),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = OrhanTheme.colors.brandSoft,
                            activeContentColor = OrhanTheme.colors.onBrandSoft,
                            activeBorderColor = OrhanTheme.colors.brandSoft,
                            inactiveContainerColor = MaterialTheme.colorScheme.surface,
                            inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            inactiveBorderColor = MaterialTheme.colorScheme.outline,
                        ),
                        label = {
                            Text(
                                text = label,
                                style = OrhanTextStyles.meta,
                                maxLines = 1,
                            )
                        },
                    )
                }
            }
        }

        item(key = "recent") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Recent",
                    style = OrhanTextStyles.kicker,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "4 pinned",
                    style = OrhanTextStyles.meta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(items = SampleChats, key = { it.title }) { chat ->
            ChatRowItem(chat = chat, onClick = { onOpenChat(chat) })
        }
    }
}

@Composable
private fun SearchField(modifier: Modifier = Modifier) {
    val colors = OrhanTheme.colors
    val shapes = OrhanTheme.shapes

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(shapes.pill)
            .background(colors.field)
            .border(1.dp, colors.fieldEdge, shapes.pill)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Search chats, agents, people",
            style = OrhanTextStyles.snippet,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/* -------------------------------- chat row -------------------------------- */

@Composable
private fun ChatRowItem(chat: OrhanChat, onClick: () -> Unit) {
    val colors = OrhanTheme.colors
    val shapes = OrhanTheme.shapes

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(shapes.avatarList)
                    .background(if (chat.isAgent) colors.brandSoft else colors.field)
                    .border(
                        width = 1.dp,
                        color = if (chat.isAgent) colors.brandSoft else MaterialTheme.colorScheme.outline,
                        shape = shapes.avatarList,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initialsOf(chat.title),
                    style = OrhanTextStyles.rowTitle,
                    color = if (chat.isAgent) colors.onBrandSoft else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (chat.working) {
                // Mint live dot, ringed with the canvas colour so it reads on any row.
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(colors.logoMint),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = chat.title,
                    style = OrhanTextStyles.rowTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (chat.isAgent) {
                    Spacer(modifier = Modifier.width(6.dp))
                    AgentTag()
                } else if (chat.members > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "\u00b7 ${chat.members}",
                        style = OrhanTextStyles.meta,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = chat.snippet,
                style = OrhanTextStyles.snippet,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = chat.time,
                style = OrhanTextStyles.meta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (chat.unread > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = chat.unread.toString(),
                        style = OrhanTextStyles.kicker,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun AgentTag() {
    val colors = OrhanTheme.colors
    val shapes = OrhanTheme.shapes
    Box(
        modifier = Modifier
            .clip(shapes.pill)
            .background(colors.brandSoft)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = "AGENT",
            style = OrhanTextStyles.eyebrow,
            color = colors.onBrandSoft,
        )
    }
}

@Composable
private fun NewChatFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = OrhanTheme.colors
    val shapes = OrhanTheme.shapes

    Box(
        modifier = modifier
            .size(56.dp)
            .shadow(elevation = 8.dp, shape = shapes.fab)
            .clip(shapes.fab)
            .background(colors.primaryGradient)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = "New chat",
            tint = colors.onGradient,
            modifier = Modifier.size(24.dp),
        )
    }
}

/* ------------------------------- Agents tab ------------------------------ */

@Composable
private fun AgentsTab(modifier: Modifier = Modifier) {
    val colors = OrhanTheme.colors
    val shapes = OrhanTheme.shapes

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "agents-head") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Your agents",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "4 active",
                    style = OrhanTextStyles.meta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(items = SampleAgents, key = { it.name }) { agent ->
            OrhanCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(shapes.avatarHeader)
                                .background(colors.brandSoft),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = initialsOf(agent.name),
                                style = OrhanTextStyles.rowTitle,
                                color = colors.onBrandSoft,
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = agent.name,
                                style = OrhanTextStyles.rowTitle,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (agent.working) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(colors.logoMint),
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                }
                                Text(
                                    text = agent.status,
                                    style = OrhanTextStyles.meta,
                                    color = if (agent.working) colors.onBrandSoft
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Text(
                            text = agent.role,
                            style = OrhanTextStyles.eyebrow,
                            color = colors.onBrandSoft,
                            modifier = Modifier
                                .clip(shapes.pill)
                                .background(colors.brandSoft)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = agent.blurb,
                        style = OrhanTextStyles.snippet,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item(key = "new-agent") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clip(shapes.card)
                    .border(1.dp, MaterialTheme.colorScheme.outline, shapes.card)
                    .clickable { }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Create a new agent",
                    style = OrhanTextStyles.rowTitle,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/* ------------------------------ Discover tab ----------------------------- */

@Composable
private fun DiscoverTab(modifier: Modifier = Modifier) {
    val colors = OrhanTheme.colors
    val shapes = OrhanTheme.shapes

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "featured") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.card)
                    .background(colors.primaryGradient)
                    .padding(18.dp),
            ) {
                Text(
                    text = "FEATURED AGENT",
                    style = OrhanTextStyles.eyebrow,
                    color = colors.onGradient,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Prompt a full landing page and get a published link in under two minutes.",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onGradient,
                )
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(shapes.pill)
                        .background(colors.onGradient)
                        .clickable { }
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Start a chat",
                        style = OrhanTextStyles.button,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        item(key = "trending-head") {
            Text(
                text = "Trending in community",
                style = OrhanTextStyles.kicker,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        items(items = TrendingAgents, key = { it.first }) { row ->
            OrhanCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .heightIn(min = 60.dp)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = row.first,
                            style = OrhanTextStyles.rowTitle,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = row.second,
                            style = OrhanTextStyles.meta,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = colors.logoMint,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

private val TrendingAgents = listOf(
    "Deck in 5 prompts" to "Productivity \u00b7 2.1k users",
    "Cited briefs" to "Research \u00b7 1.4k users",
    "Invoice triage" to "Finance \u00b7 980 users",
    "Design Critic" to "Design \u00b7 760 users",
)

/* ------------------------------- Profile tab ----------------------------- */

@Composable
private fun ProfileTab(modifier: Modifier = Modifier) {
    val colors = OrhanTheme.colors
    val shapes = OrhanTheme.shapes

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(shapes.avatarProfile)
                    .background(colors.brandSoft),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "FK",
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.onBrandSoft,
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Fareed Khan",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "fareed.khan@gmail.com",
                    style = OrhanTextStyles.snippet,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(value = "128", label = "CHATS", modifier = Modifier.weight(1f))
            StatTile(value = "4", label = "AGENTS", modifier = Modifier.weight(1f))
            StatTile(value = "37", label = "ARTIFACTS", modifier = Modifier.weight(1f))
        }

        OrhanCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(4.dp)) {
                ProfileRow("Personal information", "Name, email, avatar")
                ProfileRow("Privacy & security", "Passcode, blocked users")
                ProfileRow("Subscription", "Pro \u00b7 renews 12 Nov")
                ProfileRow("All settings", "")
            }
        }
    }
}

@Composable
private fun ProfileRow(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable { }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = OrhanTextStyles.rowTitle,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = OrhanTextStyles.meta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            imageVector = Icons.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    val colors = OrhanTheme.colors
    val shapes = OrhanTheme.shapes

    Column(
        modifier = modifier
            .clip(shapes.statTile)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, colors.hairline, shapes.statTile)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = OrhanTextStyles.statNumber,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = OrhanTextStyles.kicker,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/* ------------------------------ bottom nav ------------------------------- */

/**
 * Real M3 [NavigationBar] instead of a hand-rolled Surface/Row.
 *
 * The brand look is preserved by feeding M3's own colour slots from the token
 * layer: the 58 x 30 mint pill is [NavigationBarItemDefaults.colors]'
 * `indicatorColor`, the active glyph is `selectedIconColor`, and the bar's
 * translucent fill plus hairline top edge come from a [Surface] wrapper that M3
 * still owns (NavigationBar paints nothing behind the items).
 *
 * M3's NavigationBar already enforces a 48dp minimum touch target per item and
 * its own 80dp container height, so no manual height is needed.
 */
@Composable
fun OrhanBottomBar(
    selected: OrhanTab,
    onSelect: (OrhanTab) -> Unit,
) {
    val colors = OrhanTheme.colors

    Surface(color = colors.navBackground) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.hairline),
            )
            NavigationBar(
                containerColor = Color.Transparent,
                tonalElevation = 0.dp,
            ) {
                OrhanTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == selected,
                        onClick = { onSelect(tab) },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null, // label already announced
                                modifier = Modifier.size(21.dp),
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                style = OrhanTextStyles.navLabel,
                                maxLines = 1,
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = colors.navIndicatorInk,
                            selectedTextColor = colors.navIndicatorInk,
                            indicatorColor = colors.navIndicator,
                            unselectedIconColor = colors.navInactiveInk,
                            unselectedTextColor = colors.navInactiveInk,
                        ),
                    )
                }
            }
        }
    }
}

/* ------------------------------ app sample ------------------------------- */

/**
 * Minimal host that runs both screens as one app: tapping a row opens
 * [ChatScreen], back returns to the list, and the sun/moon button flips
 * [OrhanTheme] \u2014 the same flag Settings \u2192 Appearance persists as [ThemeMode].
 */
@Composable
fun OrhanAppSample() {
    val systemDark = isSystemInDarkTheme()
    var dark by rememberSaveable { mutableStateOf(systemDark) }
    var openChat by rememberSaveable { mutableStateOf<String?>(null) }

    OrhanTheme(darkTheme = dark) {
        val chat = openChat
        if (chat == null) {
            HomeScreen(
                onOpenChat = { openChat = it.title },
                onToggleTheme = { dark = !dark },
                isDark = dark,
            )
        } else {
            ChatScreen(
                agentName = chat,
                onBack = { openChat = null },
            )
        }
    }
}

@Preview(name = "Home \u2014 light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HomeLightPreview() {
    OrhanTheme(darkTheme = false) { HomeScreen(isDark = false) }
}

@Preview(name = "Home \u2014 dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HomeDarkPreview() {
    OrhanTheme(darkTheme = true) { HomeScreen(isDark = true) }
}

@Preview(name = "Home \u2014 360dp", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun HomeNarrowPreview() {
    OrhanTheme(darkTheme = true) { HomeScreen(isDark = true) }
}
