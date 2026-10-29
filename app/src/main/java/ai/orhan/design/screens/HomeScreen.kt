package ai.orhan.design.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.ui.semantics.Role
import ai.orhan.data.artifacts.Artifact
import ai.orhan.feature.artifacts.ArtifactsScreen
import ai.orhan.feature.memory.MemoryScreen
import ai.orhan.feature.search.LocalSearchScreen
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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


/** Bottom-navigation destinations. Profile opens from the header avatar. */
enum class OrhanTab(val label: String, val icon: ImageVector) {
    Chats("Chats", Icons.Rounded.Chat),
    Agents("Agents", Icons.Rounded.SmartToy),
    Discover("Discover", Icons.Rounded.Explore),
    Memory("Memory", Icons.Rounded.Psychology),
    Artifacts("Artifacts", Icons.Rounded.Layers),
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
 * @param onOpenChat fires when a row or the header + is tapped; the host switches to
 *   [ChatScreen] and passes the title through.
 * @param onOpenSettings fires from Profile.
 * @param onToggleTheme flips [ThemeMode] between Light and Dark at the app root.
 *   The header no longer shows the toggle; Appearance still owns it.
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
    artifacts: List<Artifact> = emptyList(),
    onUpsertArtifact: (Artifact) -> Unit = {},
    onDeleteArtifact: (String) -> Unit = {},
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
    var profileOpen by rememberSaveable { mutableStateOf(false) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var artifactToOpen by rememberSaveable { mutableStateOf<String?>(null) }
    if (searchOpen) {
        BackHandler { searchOpen = false }
        LocalSearchScreen(
            chats = recentChats,
            agents = agents,
            artifacts = artifacts,
            onCancel = { searchOpen = false },
            onOpenChat = { chat ->
                searchOpen = false
                onOpenChat(chat)
            },
            onOpenAgent = { agent ->
                searchOpen = false
                onOpenChat(OrhanChat(agent.name, agent.blurb, "", isAgent = true))
            },
            onOpenArtifact = { id ->
                searchOpen = false
                artifactToOpen = id
                tab = OrhanTab.Artifacts
            },
            modifier = modifier,
        )
        return
    }
    if (profileOpen) {
        BackHandler { profileOpen = false }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        if (!profileOpen && tab == OrhanTab.Chats) {
            HomeTopBar(
                profile = profile,
                onOpenProfile = { profileOpen = true },
                onNewChat = {
                    onOpenChat(OrhanChat(title = "Orhan", snippet = "", time = "", isAgent = true))
                },
            )
        }
        if (profileOpen) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { profileOpen = false }, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            if (profileOpen) {
                ProfileTab(
                    onOpenSettings = onOpenSettings,
                    onOpenAppearance = onOpenAppearance,
                    onEditProfile = onEditProfile,
                    onOpenMemory = {
                        profileOpen = false
                        tab = OrhanTab.Memory
                    },
                    onOpenArtifacts = {
                        profileOpen = false
                        tab = OrhanTab.Artifacts
                    },
                    onOpenChatPreferences = onOpenChatPreferences,
                    onOpenAgents = {
                        profileOpen = false
                        tab = OrhanTab.Agents
                    },
                    onSignOut = onSignOut,
                    onOpenSettingsPage = onOpenSettingsPage,
                    artifactCount = artifactCount,
                    agentCount = agents.size,
                    profile = profile,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                when (tab) {
                    OrhanTab.Chats -> ChatsTab(
                        onOpenChat = onOpenChat,
                        onOpenSearch = { searchOpen = true },
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
                    OrhanTab.Memory -> MemoryScreen(
                        onBack = { tab = OrhanTab.Chats },
                        embedded = true,
                    )
                    OrhanTab.Artifacts -> ArtifactsScreen(
                        artifacts = artifacts,
                        onUpsert = onUpsertArtifact,
                        onDelete = onDeleteArtifact,
                        onBack = { tab = OrhanTab.Chats },
                        embedded = true,
                        pendingOpenId = artifactToOpen,
                        onPendingOpenConsumed = { artifactToOpen = null },
                    )
                }
            }
        }

        if (!profileOpen) {
            OrhanBottomBar(
                selected = tab,
                onSelect = { tab = it },
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
            )
        }
    }
}

/* -------------------------------- top bar -------------------------------- */

@Composable
private fun HomeTopBar(
    profile: UserProfile,
    onOpenProfile: () -> Unit,
    onNewChat: () -> Unit,
) {
    val colors = OrhanTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(profile.safeAvatarResId),
            contentDescription = profile.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = onOpenProfile),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.orhan_mark),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Orhan",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, colors.fieldEdge, CircleShape)
                .clickable(onClick = onNewChat),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "New chat",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/* ------------------------------- Chats tab ------------------------------- */

@Composable
private fun ChatsTab(
    onOpenChat: (OrhanChat) -> Unit,
    onOpenSearch: () -> Unit,
    recentChats: List<OrhanChat> = emptyList(),
    pinnedTitles: Set<String> = emptySet(),
    onTogglePin: (OrhanChat) -> Unit = {},
    onRemoveRecent: (OrhanChat) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val recentTitles = recentChats.map { it.title }.toSet()
    val chats = recentChats
    var pinnedExpanded by rememberSaveable { mutableStateOf(true) }
    var chatsExpanded by rememberSaveable { mutableStateOf(true) }
    val hub = chats.firstOrNull { it.title.equals("Orhan", ignoreCase = true) }
    // Orhan is the hub (card above the sections); keep it out of the rows.
    val visible = chats.filterNot { it.title.equals("Orhan", ignoreCase = true) }
    val pinnedVisible = visible.filter { it.title in pinnedTitles }
    val unpinnedVisible = visible.filterNot { it.title in pinnedTitles }
    val hubSnippet = hub?.snippet
        ?.takeIf { it.isNotBlank() && !it.equals("Recent chat", ignoreCase = true) }
        ?: "Your main agent"

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp),
    ) {
        item(key = "search") {
            SearchPill(
                onClick = onOpenSearch,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        item(key = "orhan-hub") {
            OrhanHubCard(
                snippet = hubSnippet,
                time = hub?.time.orEmpty(),
                onClick = { onOpenChat(OrhanChat(title = "Orhan", snippet = "", time = "", isAgent = true)) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        if (pinnedVisible.isNotEmpty()) {
            item(key = "pinned-header") {
                SectionHeader(
                    title = "PINNED · ${pinnedVisible.size}",
                    icon = Icons.Rounded.PushPin,
                    expanded = pinnedExpanded,
                    onToggle = { pinnedExpanded = !pinnedExpanded },
                )
            }
            if (pinnedExpanded) {
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
        }

        item(key = "recent") {
            SectionHeader(
                title = "CHATS · ${unpinnedVisible.size}",
                icon = Icons.Rounded.Chat,
                expanded = chatsExpanded,
                onToggle = { chatsExpanded = !chatsExpanded },
            )
        }

        if (chatsExpanded && visible.isEmpty() && chats.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = "No chats yet — tap + to start one",
                    style = OrhanTextStyles.snippet,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                )
            }
        }

        if (chatsExpanded) {
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
}

/** Main controller entry. Mint fill and a primary border match the observed hub row. */
@Composable
private fun OrhanHubCard(
    snippet: String,
    time: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OrhanTheme.colors
    val shape = OrhanTheme.shapes.card
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.brandSoft)
            .border(1.dp, MaterialTheme.colorScheme.primary, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.orhan_mark),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Orhan",
                    style = OrhanTextStyles.rowTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                if (time.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = time,
                        style = OrhanTextStyles.meta,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = snippet,
                style = OrhanTextStyles.snippet,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = OrhanTextStyles.kicker,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = if (expanded) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess,
            contentDescription = if (expanded) "Collapse" else "Expand",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(OrhanTheme.colors.hairline),
    )
    }
}

@Composable
private fun SearchPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OrhanTheme.colors
    val shapes = OrhanTheme.shapes
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(shapes.pill)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, colors.fieldEdge, shapes.pill)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Search",
            style = OrhanTextStyles.snippet,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
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
    Column {
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
                    .clip(CircleShape)
                    .background(if (chat.isAgent) colors.brandSoft else colors.field),
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
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = chat.title,
                        style = OrhanTextStyles.rowTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (!chat.isAgent && chat.members > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "\u00b7 ${chat.members}",
                            style = OrhanTextStyles.meta,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = chat.time,
                    style = OrhanTextStyles.meta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = chat.snippet,
                    style = OrhanTextStyles.snippet,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (chat.unread > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
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
    Box(
        modifier = Modifier
            .padding(start = 74.dp, end = 16.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(colors.hairline),
    )
    }
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
    artifactCount: Int = 0,
    agentCount: Int = 0,
    appVersion: String = ai.orhan.BuildConfig.VERSION_NAME,
    profile: UserProfile = UserProfile(),
    modifier: Modifier = Modifier,
) {
    val colors = OrhanTheme.colors

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
 * Floating pill. Selected ink uses [OrhanColors.navIndicatorInk]; inactive uses
 * [OrhanColors.navInactiveInk]. No indicator blob — the observed bar is icon plus label.
 */
@Composable
fun OrhanBottomBar(
    selected: OrhanTab,
    onSelect: (OrhanTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OrhanTheme.colors
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = colors.navBackground,
        shadowElevation = 8.dp,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OrhanTab.entries.forEach { tab ->
                val isSelected = tab == selected
                val tint = if (isSelected) colors.navIndicatorInk else colors.navInactiveInk
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .selectable(
                            selected = isSelected,
                            onClick = { onSelect(tab) },
                            role = Role.Tab,
                        )
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tab.label,
                        style = OrhanTextStyles.navLabel,
                        color = tint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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
