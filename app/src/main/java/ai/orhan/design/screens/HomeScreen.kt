package ai.orhan.design.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.remember
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import ai.orhan.feature.profile.UserProfile
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ai.orhan.R
import ai.orhan.feature.agents.AgentsScreen
import ai.orhan.feature.agents.InstalledAgent
import ai.orhan.feature.agents.SampleInstalledAgents
import ai.orhan.feature.discover.DiscoverScreen
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
    onOpenAppearance: () -> Unit = onOpenSettings,
    onCreateAgent: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onOpenMemory: () -> Unit = {},
    onOpenArtifacts: () -> Unit = {},
    artifactCount: Int = 0,
    /** Agent behaviour knobs (reply style, preview) live in Chat preferences. */
    onOpenChatPreferences: () -> Unit = onOpenSettings,
    /** Null when no session exists; Profile then hides the Sign out row. */
    onSignOut: (() -> Unit)? = null,
    /** Opens a settings sub-page by route (notifications, language, privacy, about). */
    onOpenSettingsPage: (String) -> Unit = { onOpenSettings() },
    isDark: Boolean = isSystemInDarkTheme(),
    profile: UserProfile = UserProfile(),
    /** Recently opened chats (local history), newest first; shown above the samples. */
    recentChats: List<OrhanChat> = emptyList(),
    /** Titles of pinned chats (persisted locally). */
    pinnedTitles: Set<String> = emptySet(),
    onTogglePin: (OrhanChat) -> Unit = {},
    /** Removes a chat from local recent history (and unpins it). */
    onRemoveRecent: (OrhanChat) -> Unit = {},
    /** Agents tab list: user-created agents first, then samples. */
    agents: List<InstalledAgent> = SampleInstalledAgents,
    deletableAgentNames: Set<String> = emptySet(),
    onDeleteAgent: ((InstalledAgent) -> Unit)? = null,
    workflows: List<ai.orhan.data.workflows.Workflow> = emptyList(),
    onOpenWorkflow: (ai.orhan.data.workflows.Workflow) -> Unit = {},
    onCreateWorkflow: () -> Unit = {},
    onEditWorkflow: (ai.orhan.data.workflows.Workflow) -> Unit = {},
    onDeleteWorkflow: ((ai.orhan.data.workflows.Workflow) -> Unit)? = null,
    onOpenPlaybook: (ai.orhan.feature.discover.Playbook) -> Unit = {},
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
                    recentChats = recentChats,
                    pinnedTitles = pinnedTitles,
                    onTogglePin = onTogglePin,
                    onRemoveRecent = onRemoveRecent,
                    modifier = Modifier.fillMaxSize(),
                )

                OrhanTab.Agents -> AgentsScreen(
                    agents = agents,
                    onOpenAgent = { onOpenChat(OrhanChat(it.name, it.blurb, "", isAgent = true)) },
                    onCreateAgent = onCreateAgent,
                    deletableNames = deletableAgentNames,
                    onDeleteAgent = onDeleteAgent,
                    workflows = workflows,
                    onOpenWorkflow = onOpenWorkflow,
                    onCreateWorkflow = onCreateWorkflow,
                    onEditWorkflow = onEditWorkflow,
                    onDeleteWorkflow = onDeleteWorkflow,
                    modifier = Modifier.fillMaxSize(),
                )
                OrhanTab.Discover -> DiscoverScreen(
                    onOpenPlaybook = onOpenPlaybook,
                    modifier = Modifier.fillMaxSize(),
                )
                OrhanTab.Profile -> ProfileTab(
                    onOpenSettings = onOpenSettings,
                    onOpenAppearance = onOpenAppearance,
                    onEditProfile = onEditProfile,
                    onOpenMemory = onOpenMemory,
                    onOpenArtifacts = onOpenArtifacts,
                    onOpenChatPreferences = onOpenChatPreferences,
                    onOpenAgents = { tab = OrhanTab.Agents },
                    onSignOut = onSignOut,
                    onOpenSettingsPage = onOpenSettingsPage,
                    onOpenDiscover = { tab = OrhanTab.Discover },
                    artifactCount = artifactCount,
                    agentCount = agents.size,
                    profile = profile,
                    modifier = Modifier.fillMaxSize(),
                )
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
    recentChats: List<OrhanChat> = emptyList(),
    pinnedTitles: Set<String> = emptySet(),
    onTogglePin: (OrhanChat) -> Unit = {},
    onRemoveRecent: (OrhanChat) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val recentTitles = recentChats.map { it.title }.toSet()
    val chats = recentChats + SampleChats.filterNot { it.title in recentTitles }
    var filter by rememberSaveable { mutableStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    val visible = chats
        .filter { chat ->
            when (filter) {
                1 -> chat.unread > 0
                2 -> chat.members > 0
                3 -> recentTitles.isEmpty() || chat.title in recentTitles
                else -> true
            }
        }
        .filter { chat ->
            val q = query.trim()
            q.isEmpty() || chat.title.contains(q, ignoreCase = true) ||
                chat.snippet.contains(q, ignoreCase = true)
        }
    val pinnedVisible = visible.filter { it.title in pinnedTitles }
    val unpinnedVisible = visible.filterNot { it.title in pinnedTitles }
    val pinnedCount = pinnedTitles.size

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(top = 4.dp, bottom = 88.dp),
    ) {
        item(key = "search") {
            SearchField(
                query = query,
                onQueryChange = { query = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
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

        if (pinnedVisible.isNotEmpty()) {
            item(key = "pinned-header") { SectionHeader("Pinned", "$pinnedCount pinned") }
            items(items = pinnedVisible, key = { "pinned:" + it.title }) { chat ->
                ChatRowItem(
                    chat = chat,
                    pinned = true,
                    onClick = { onOpenChat(chat) },
                    onTogglePin = { onTogglePin(chat) },
                    onRemoveRecent = if (chat.title in recentTitles) ({ onRemoveRecent(chat) }) else null,
                )
            }
        }

        item(key = "recent") {
            SectionHeader("Recent", if (pinnedVisible.isEmpty()) "$pinnedCount pinned" else null)
        }

        if (visible.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = "No chats match",
                    style = OrhanTextStyles.snippet,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                )
            }
        }

        items(items = unpinnedVisible, key = { it.title }) { chat ->
            ChatRowItem(
                chat = chat,
                pinned = false,
                onClick = { onOpenChat(chat) },
                onTogglePin = { onTogglePin(chat) },
                onRemoveRecent = if (chat.title in recentTitles) ({ onRemoveRecent(chat) }) else null,
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, meta: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = OrhanTextStyles.kicker,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.weight(1f))
        if (meta != null) {
            Text(
                text = meta,
                style = OrhanTextStyles.meta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
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
        androidx.compose.foundation.text.BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = OrhanTextStyles.snippet.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        text = "Search chats, agents, people",
                        style = OrhanTextStyles.snippet,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                inner()
            },
        )
    }
}

/* -------------------------------- chat row -------------------------------- */

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatRowItem(
    chat: OrhanChat,
    onClick: () -> Unit,
    pinned: Boolean = false,
    onTogglePin: () -> Unit = {},
    onRemoveRecent: (() -> Unit)? = null,
) {
    val colors = OrhanTheme.colors
    val shapes = OrhanTheme.shapes
    var menuOpen by remember { mutableStateOf(false) }

    Box {
    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
        DropdownMenuItem(
            text = { Text(if (pinned) "Unpin chat" else "Pin chat") },
            onClick = { menuOpen = false; onTogglePin() },
        )
        if (onRemoveRecent != null) {
            DropdownMenuItem(
                text = { Text("Remove from recent") },
                onClick = { menuOpen = false; onRemoveRecent() },
            )
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .combinedClickable(onClick = onClick, onLongClick = { menuOpen = true })
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

/* ------------------------------- Profile tab ----------------------------- */

@Composable
private fun ProfileTab(
    onOpenSettings: () -> Unit = {},
    onOpenAppearance: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onOpenMemory: () -> Unit = {},
    onOpenArtifacts: () -> Unit = {},
    onOpenChatPreferences: () -> Unit = {},
    onOpenAgents: () -> Unit = {},
    onSignOut: (() -> Unit)? = null,
    onOpenSettingsPage: (String) -> Unit = {},
    onOpenDiscover: () -> Unit = {},
    artifactCount: Int = 0,
    agentCount: Int = 0,
    appVersion: String = ai.orhan.BuildConfig.VERSION_NAME,
    profile: UserProfile = UserProfile(),
    modifier: Modifier = Modifier,
) {
    val colors = OrhanTheme.colors
    val shapes = OrhanTheme.shapes

    // Profile = "main kaun hoon + Orhan mujhe kaise samajhta hai + settings".
    // No social bits (handle, member-since, fake stat chips) — Orhan is an agent app.
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 1) Account card: avatar + name + email (tap to edit), then honest account rows.
        OrhanCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onEditProfile)
                        .padding(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(colors.brandSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(profile.safeAvatarResId),
                            contentDescription = profile.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(profile.name, style = OrhanTextStyles.rowTitle, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(profile.email, style = OrhanTextStyles.meta, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.Rounded.KeyboardArrowRight, contentDescription = "Edit profile", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                ProfileRow("Memory", "What Orhan remembers about you", onClick = onOpenMemory)
                ProfileRow("Artifacts", if (artifactCount > 0) "$artifactCount saved" else "Saved replies", onClick = onOpenArtifacts)
            }
        }

        // 3) Bot: how Orhan behaves for you.
        ProfileSectionLabel("Bot")
        OrhanCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(4.dp)) {
                ProfileRow("Agent defaults", "Reply style & preview before sending", onClick = onOpenChatPreferences)
                ProfileRow("Your agents", if (agentCount > 0) "$agentCount agents" else "Create or manage agents", onClick = onOpenAgents)
                ProfileRow("Privacy & permissions", "What Orhan stores and can do", onClick = { onOpenSettingsPage("settings/privacy") })
            }
        }

        // 4) Preferences.
        ProfileSectionLabel("Preferences")
        OrhanCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(4.dp)) {
                ProfileRow("Notifications", "", onClick = { onOpenSettingsPage("settings/notifications") })
                ProfileRow("Appearance", "Theme: System, Light or Dark", onClick = onOpenAppearance)
                ProfileRow("Language", "", onClick = { onOpenSettingsPage("settings/language") })
                ProfileRow("All settings", "", onClick = onOpenSettings)
            }
        }

        // 5) Help + sign out.
        OrhanCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(4.dp)) {
                ProfileRow("About Orhan", "Version, licences", onClick = { onOpenSettingsPage("settings/about") })
                if (onSignOut != null) {
                    ProfileRow("Sign out", profile.email, danger = true, onClick = onSignOut)
                }
            }
        }

        Text(
            text = "Orhan AI \u00b7 v$appVersion",
            style = OrhanTextStyles.meta,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 8.dp),
        )
    }
}

@Composable
private fun ProfileSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = OrhanTextStyles.kicker,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
    )
}

@Composable
private fun ProfileRow(title: String, subtitle: String, danger: Boolean = false, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = OrhanTextStyles.rowTitle,
                color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
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
