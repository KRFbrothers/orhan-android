@file:OptIn(ExperimentalLayoutApi::class)

package ai.orhan.feature.memory

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ai.orhan.app.relativeTime
import ai.orhan.data.artifacts.Artifact
import ai.orhan.data.artifacts.artifactStore
import ai.orhan.data.memory.ActionSaveResult
import ai.orhan.data.memory.ArtifactLink
import ai.orhan.data.memory.DEFAULT_TOPIC_GROUP
import ai.orhan.data.memory.MAX_ENTRY_TITLE
import ai.orhan.data.memory.artifactLink
import ai.orhan.data.memory.MAX_GROUP_NAME
import ai.orhan.data.memory.MAX_NOTE_CONTENT
import ai.orhan.data.memory.MAX_SUMMARY
import ai.orhan.data.memory.MAX_TODO_CONTENT
import ai.orhan.data.memory.MEMORY_MAX_CHARS
import ai.orhan.data.memory.MemoryEntry
import ai.orhan.data.memory.MemoryPause
import ai.orhan.data.memory.MemoryTopic
import ai.orhan.data.memory.allCount
import ai.orhan.data.memory.groupTopics
import ai.orhan.data.memory.matching
import ai.orhan.data.memory.memoryFeed
import ai.orhan.data.memory.memoryEntriesStore
import ai.orhan.data.memory.memoryPauseStore
import ai.orhan.data.memory.memoryStore
import ai.orhan.data.memory.memoryTimestamp
import ai.orhan.data.memory.normalizeTag
import ai.orhan.data.memory.updatesCount
import ai.orhan.design.theme.OrhanTextStyles
import ai.orhan.design.theme.OrhanTheme
import ai.orhan.feature.profile.UserProfile
import ai.orhan.feature.profile.profilePreferences
import ai.orhan.feature.settings.detail.FootNote
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val CardShape = RoundedCornerShape(20.dp)

private val StringListSaver = listSaver<List<String>, String>(
    save = { it },
    restore = { it.toList() },
)

private data class Shortcut(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val tint: Color,
    val onTint: Color,
)

private val Shortcuts = listOf(
    Shortcut("overview", "Overview", "Your topics and groups", Icons.Rounded.GridView, Color(0xFFD63A86), Color.White),
    Shortcut("profile", "My Profile", "Your saved profile", Icons.Rounded.Person, Color(0xFF1493B8), Color.White),
    Shortcut("actions", "Next Actions", "To-dos saved on this device", Icons.Rounded.Event, Color(0xFFE6B000), Color(0xFF2A2200)),
    Shortcut("artifacts", "My Artifacts", "Saved documents on this device", Icons.Rounded.Inventory2, Color(0xFF7C4DDB), Color.White),
    Shortcut("meetings", "My Meetings", "No recordings in Orhan", Icons.Rounded.GraphicEq, Color(0xFF1F9D55), Color.White),
)

/**
 * Memory tab. Profile text is sent only while Pause memory is off.
 * Automatic memory stays off until the user enables it. Topic text is sent
 * only when "Use topics in chats" is on, and never while memory is paused.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MemoryScreen(
    onBack: () -> Unit,
    embedded: Boolean = false,
    onEditProfile: () -> Unit = {},
    onOpenSource: (String, Long) -> Unit = { _, _ -> },
    onOpenArtifact: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val entries = remember(context) { context.memoryEntriesStore() }
    val artifactStore = remember(context) { context.artifactStore() }
    val topics by entries.topics.collectAsState(initial = emptyList())
    val artifacts by artifactStore.artifacts.collectAsState(initial = null as List<Artifact>?)
    val snackbar = remember { SnackbarHostState() }
    var page by rememberSaveable { mutableStateOf("home") }
    var openTopicId by rememberSaveable { mutableStateOf<String?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }
    val inChild = creating || openTopicId != null || page != "home"
    BackHandler(enabled = inChild || !embedded) {
        when {
            creating || openTopicId != null -> {
                creating = false
                openTopicId = null
            }
            page != "home" -> page = "home"
            else -> onBack()
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .then(if (embedded) Modifier else Modifier.statusBarsPadding())
            .then(if (embedded) Modifier else Modifier.navigationBarsPadding())
            .imePadding(),
    ) {
        when {
            creating || openTopicId != null -> TopicEditor(
                topic = topics.firstOrNull { it.id == openTopicId },
                groups = topics.map { it.group }.distinct(),
                onBack = { creating = false; openTopicId = null },
                onSave = { title, summary, content, group, tags, artifactId ->
                    entries.upsertTopic(
                        if (creating) null else openTopicId,
                        title,
                        summary,
                        content,
                        group,
                        tags,
                        artifactId,
                    )
                },
                onDelete = { id -> entries.deleteTopic(id) },
                onOpenSource = onOpenSource,
                onOpenArtifact = onOpenArtifact,
                artifacts = artifacts,
                snackbar = snackbar,
            )
            page == "overview" -> OverviewPage(topics, onBack = { page = "home" }, onOpen = { openTopicId = it })
            page == "profile" -> ProfilePage(onBack = { page = "home" }, onEditProfile = onEditProfile, snackbar = snackbar)
            page == "actions" -> ActionsPage(
                onBack = { page = "home" },
                onOpenSource = onOpenSource,
                snackbar = snackbar,
            )
            page == "artifacts" -> ArtifactsPage(
                artifacts = artifacts,
                onBack = { page = "home" },
                onOpen = onOpenArtifact,
            )
            page == "meetings" -> MeetingsPage(onBack = { page = "home" })
            else -> TopicHome(
                topics = topics,
                onOpenShortcut = { page = it },
                onOpenTopic = { id ->
                    openTopicId = id
                },
                onCreate = { creating = true },
            )
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
    LaunchedEffect(openTopicId) {
        val id = openTopicId ?: return@LaunchedEffect
        if (!entries.markTopicRead(id)) {
            snackbar.showSnackbar("Couldn't update this topic. It is still in Updates.")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TopicHome(
    topics: List<MemoryTopic>,
    onOpenShortcut: (String) -> Unit,
    onOpenTopic: (String) -> Unit,
    onCreate: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var updatesOnly by rememberSaveable { mutableStateOf(false) }
    var tagsOpen by rememberSaveable { mutableStateOf(false) }
    var selectedTags by rememberSaveable(stateSaver = StringListSaver) { mutableStateOf(emptyList<String>()) }
    var collapsed by rememberSaveable(stateSaver = StringListSaver) { mutableStateOf(emptyList<String>()) }
    val selected = selectedTags.toSet()
    val visible = topics.matching(query, selected, updatesOnly)
    val showShortcuts = query.isBlank() && selected.isEmpty() && !updatesOnly
    val knownTags = topics.flatMap { it.tags }.distinct().sorted()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        MemoryTitle("Memory", showBack = false, onBack = {})
        SearchField(query, onQuery = { query = it })
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FlowRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    label = "All",
                    count = allCount(topics, query, selected),
                    selected = !updatesOnly,
                    onClick = { updatesOnly = false },
                )
                FilterChip(
                    label = "Updates",
                    count = updatesCount(topics),
                    selected = updatesOnly,
                    onClick = { updatesOnly = true },
                )
                TextChip(
                    label = if (selected.isEmpty()) "Tags" else "Tags ${selected.size}",
                    onClick = { tagsOpen = !tagsOpen },
                )
            }
            TextButton(onClick = onCreate, modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("New", color = MaterialTheme.colorScheme.primary, style = OrhanTextStyles.button)
            }
        }
        if (tagsOpen) {
            if (knownTags.isEmpty()) {
                Text(
                    "No tags yet. Add some when you save a topic.",
                    style = OrhanTextStyles.snippet,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp),
                ) {
                    knownTags.forEach { tag ->
                        val on = selected.any { it.equals(tag, ignoreCase = true) }
                        TextChip(
                            label = "#$tag",
                            selected = on,
                            onClick = {
                                selectedTags = if (on) selectedTags.filterNot { it.equals(tag, true) }
                                else selectedTags + tag
                            },
                        )
                    }
                }
            }
        }
        if (showShortcuts) {
            Shortcuts.forEach { shortcut ->
                ShortcutCard(shortcut, onClick = { onOpenShortcut(shortcut.id) })
            }
            Spacer(Modifier.height(8.dp))
        }
        when {
            topics.isEmpty() -> EmptyCopy("No topics yet. Tap New to add one.")
            visible.isEmpty() && updatesOnly && query.isBlank() && selected.isEmpty() -> EmptyCopy("No updates")
            visible.isEmpty() -> EmptyCopy("No matches")
            else -> {
                memoryFeed(visible, updatesOnly).forEach { group ->
                    val showHeader = group.name.isNotBlank()
                    val closed = showHeader && group.name in collapsed
                    if (showHeader) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable {
                                    collapsed = if (closed) collapsed - group.name else collapsed + group.name
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                if (closed) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess,
                                contentDescription = if (closed) "Expand ${group.name}" else "Collapse ${group.name}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                group.name.uppercase(),
                                style = OrhanTextStyles.kicker,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp).weight(1f, fill = false),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                group.topics.size.toString(),
                                style = OrhanTextStyles.kicker,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                    if (!closed) {
                        group.topics.forEach { topic ->
                            TopicRow(topic, onClick = { onOpenTopic(topic.id) })
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TopicRow(topic: MemoryTopic, onClick: () -> Unit) {
    val colors = OrhanTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, colors.hairline, CardShape)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(colors.brandSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Description, contentDescription = null, tint = colors.onBrandSoft, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                topic.title,
                style = OrhanTextStyles.rowTitle,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (topic.unread) {
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .semantics { contentDescription = "Unread" },
                )
                Spacer(Modifier.width(4.dp))
                Text("NEW", style = OrhanTextStyles.eyebrow, color = MaterialTheme.colorScheme.primary)
            }
        }
        val snippet = topic.summary.ifBlank { topic.content }
        if (snippet.isNotBlank()) {
            Text(
                snippet,
                style = OrhanTextStyles.snippet,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 48.dp, top = 4.dp),
            )
        }
        Row(
            modifier = Modifier.padding(start = 48.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
            Text(
                memoryTimestamp(topic.updatedAt, System.currentTimeMillis(), ::relativeTime),
                style = OrhanTextStyles.meta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (topic.tags.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.padding(start = 48.dp, top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                topic.tags.forEach { tag ->
                    Text("#$tag", style = OrhanTextStyles.meta, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun ShortcutCard(shortcut: Shortcut, onClick: () -> Unit) {
    val colors = OrhanTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .heightIn(min = 72.dp)
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, colors.hairline, CardShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(shortcut.tint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(shortcut.icon, contentDescription = null, tint = shortcut.onTint, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(shortcut.title, style = OrhanTextStyles.rowTitle, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(shortcut.subtitle, style = OrhanTextStyles.snippet, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun OverviewPage(topics: List<MemoryTopic>, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val groups = groupTopics(topics)
    PageColumn("Overview", onBack) {
        Text(
            "A map of the topics saved on this device. Orhan does not build this from your chats.",
            style = OrhanTextStyles.snippet,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Text("${topics.size} topics", style = OrhanTextStyles.rowTitle, color = MaterialTheme.colorScheme.onSurface)
        Text("${updatesCount(topics)} unread", style = OrhanTextStyles.snippet, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("${groups.size} groups", style = OrhanTextStyles.snippet, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (topics.isEmpty()) {
            EmptyCopy("No topics yet. Save one from Memory and it will show up here.")
        } else {
            Text("Groups", style = OrhanTextStyles.kicker, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            groups.forEach { group ->
                val unreadInGroup = group.topics.count { it.unread }
                TopicRow(
                    MemoryTopic(
                        id = group.name,
                        title = group.name,
                        summary = "${group.topics.size} topics" + if (unreadInGroup > 0) " · $unreadInGroup unread" else "",
                        content = "",
                        group = group.name,
                        tags = emptyList(),
                        unread = false,
                        createdAt = group.topics.maxOf { it.updatedAt },
                        updatedAt = group.topics.maxOf { it.updatedAt },
                    ),
                    onClick = { onOpen(group.topics.first().id) },
                )
            }
            Text("Recent", style = OrhanTextStyles.kicker, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            topics.sortedByDescending { it.updatedAt }.take(8).forEach { topic ->
                TopicRow(topic, onClick = { onOpen(topic.id) })
            }
        }
    }
}

@Composable
private fun ProfilePage(onBack: () -> Unit, onEditProfile: () -> Unit, snackbar: SnackbarHostState) {
    val context = LocalContext.current
    val profiles = remember(context) { context.profilePreferences() }
    val profile by profiles.profile.collectAsState(initial = UserProfile())
    val memory = remember(context) { context.memoryStore() }
    val pauseStore = remember(context) { context.memoryPauseStore() }
    val pause by pauseStore.state.collectAsState(initial = MemoryPause())
    val scope = rememberCoroutineScope()
    var text by rememberSaveable { mutableStateOf("") }
    var loaded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(memory) {
        if (!loaded) {
            text = memory.memory.first()
            loaded = true
        }
    }
    PageColumn("My Profile", onBack) {
        Text(
            profile.displayName,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (profile.handle.isNotBlank()) {
            Text(profile.handle, style = OrhanTextStyles.snippet, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (profile.role.isNotBlank() || profile.timezone.isNotBlank()) {
            Text(
                listOf(profile.role, profile.timezone).filter { it.isNotBlank() }.joinToString(" · "),
                style = OrhanTextStyles.snippet,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (profile.bio.isNotBlank()) {
            Text(profile.bio, style = OrhanTextStyles.snippet, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 8.dp))
        }
        TextButton(onClick = onEditProfile, modifier = Modifier.heightIn(min = 48.dp)) { Text("Edit profile") }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Pause memory", style = OrhanTextStyles.rowTitle, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "When paused, Orhan won't use this context in chats. Your text stays saved.",
                    style = OrhanTextStyles.snippet,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = pause.paused,
                onCheckedChange = { scope.launch { pauseStore.setPaused(it) } },
                enabled = !pause.unreadable,
                modifier = Modifier.semantics { contentDescription = "Pause memory" },
            )
        }
        if (pause.unreadable) {
            Text(
                "Pause setting couldn't be read. Chats won't include this context, and automatic memory stays off, until it can be read again.",
                style = OrhanTextStyles.snippet,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        } else if (pause.paused) {
            Text(
                "Memory is paused — chats won't include this context, and new messages won't be saved, until you turn it back on.",
                style = OrhanTextStyles.snippet,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = 8.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Automatic memory", style = OrhanTextStyles.rowTitle, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "Off until you turn it on. Then a durable fact in a message sent after that can be saved. Older chats are left alone.",
                    style = OrhanTextStyles.snippet,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = pause.automaticEnabled && !pause.unreadable,
                onCheckedChange = { enabled -> scope.launch { pauseStore.setAutomaticEnabled(enabled) } },
                enabled = !pause.unreadable,
                modifier = Modifier.semantics { contentDescription = "Automatic memory" },
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = 8.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Use topics in chats", style = OrhanTextStyles.rowTitle, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "Off by default. When on, saved topic titles and summaries are included in chats. Pause memory stops that.",
                    style = OrhanTextStyles.snippet,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = pause.includeTopics && !pause.unreadable,
                onCheckedChange = { enabled -> scope.launch { pauseStore.setIncludeTopics(enabled) } },
                enabled = !pause.unreadable && !pause.paused,
                modifier = Modifier.semantics { contentDescription = "Use topics in chats" },
            )
        }
        pause.lastError?.let { message ->
            Text(
                message,
                style = OrhanTextStyles.snippet,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Text(
            "WHAT SHOULD YOUR AGENTS KNOW?",
            style = OrhanTextStyles.kicker,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        )
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.take(MEMORY_MAX_CHARS) },
            placeholder = { Text("e.g. I'm a student in Delhi, prefer short answers…") },
            supportingText = { Text("${text.length} / $MEMORY_MAX_CHARS") },
            minLines = 5,
            modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
        )
        TextButton(
            onClick = {
                scope.launch {
                    memory.set(text)
                    snackbar.showSnackbar("Memory saved")
                }
            },
            enabled = loaded,
            modifier = Modifier.heightIn(min = 48.dp),
        ) { Text("Save") }
        FootNote("Profile text is sent only while Pause memory is off. Automatic memory does not read older chats. Topics are sent only when Use topics in chats is on, and never while memory is paused.")
    }
}

@Composable
private fun ActionsPage(
    onBack: () -> Unit,
    onOpenSource: (String, Long) -> Unit,
    snackbar: SnackbarHostState,
) {
    val context = LocalContext.current
    val store = remember(context) { context.memoryEntriesStore() }
    val todos by store.todos.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<MemoryEntry?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<MemoryEntry?>(null) }
    var deletingNow by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    PageColumn("Next Actions", onBack) {
        Text(
            "To-dos you saved on this device. Orhan does not invent upcoming actions from chats.",
            style = OrhanTextStyles.snippet,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = { creating = true }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Add to-do") }
        if (todos.isEmpty()) EmptyCopy("No next actions yet.")
        todos.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = item.done,
                    onCheckedChange = {
                        scope.launch {
                            if (!store.toggleDone(item.id)) {
                                snackbar.showSnackbar("Couldn't update this action.")
                            }
                        }
                    },
                    modifier = Modifier.semantics {
                        contentDescription = if (item.done) "Reopen action" else "Complete action"
                    },
                )
                Column(Modifier.weight(1f).clickable { editing = item }) {
                    Text(
                        item.title,
                        style = OrhanTextStyles.rowTitle,
                        textDecoration = if (item.done) TextDecoration.LineThrough else null,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (item.content.isNotBlank()) {
                        Text(item.content, style = OrhanTextStyles.snippet, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    if (item.sourceChatTitle.isNotBlank()) {
                        Text(
                            "From ${item.sourceChatTitle}",
                            style = OrhanTextStyles.meta,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        TextButton(
                            onClick = { onOpenSource(item.sourceChatTitle, item.sourceMessageId) },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .semantics { contentDescription = "Open source chat" },
                        ) { Text("Open source chat") }
                    }
                }
                IconButton(onClick = {
                    deleteError = null
                    deleting = item
                }) { Icon(Icons.Rounded.Delete, "Delete to-do") }
            }
        }
        FootNote("Saving an action keeps it on this device. It does not run the action, and it is not sent to your agents.")
    }
    val draft = if (creating) MemoryEntry("", "", "", false, 0L) else editing
    if (draft != null) {
        TodoDialog(
            initial = draft,
            isNew = creating,
            onDismiss = { creating = false; editing = null },
            onSave = { title, content ->
                store.saveTodo(if (creating) null else draft.id, title, content)
            },
        )
    }
    deleting?.let { item ->
        ConfirmDelete(
            title = "Delete to-do?",
            body = deleteError ?: "\"${item.title}\" will be removed from this device.",
            confirmEnabled = !deletingNow,
            onDismiss = {
                if (!deletingNow) {
                    deleting = null
                    deleteError = null
                }
            },
            onConfirm = {
                if (deletingNow) return@ConfirmDelete
                deletingNow = true
                scope.launch {
                    val removed = store.deleteTodo(item.id)
                    deletingNow = false
                    if (removed) {
                        deleting = null
                        deleteError = null
                    } else {
                        deleteError = "Couldn't delete this action. It is still saved on this device."
                        snackbar.showSnackbar("Couldn't delete this action. It is still saved on this device.")
                    }
                }
            },
        )
    }
}

@Composable
private fun ArtifactsPage(
    artifacts: List<Artifact>?,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    PageColumn("My Artifacts", onBack) {
        Text(
            "Documents saved in Orhan. Nothing here is generated from Memory topics.",
            style = OrhanTextStyles.snippet,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (artifacts == null) {
            EmptyCopy("Checking the saved documents…")
        } else if (artifacts.isEmpty()) {
            EmptyCopy("No artifacts yet — save a reply from chat, or create a document from the Artifacts tab.")
        }
        artifacts.orEmpty().forEach { artifact ->
            TopicRow(
                MemoryTopic(
                    id = artifact.id,
                    title = artifact.title,
                    summary = artifact.content,
                    content = artifact.content,
                    group = "",
                    tags = emptyList(),
                    unread = false,
                    createdAt = artifact.createdAt,
                    updatedAt = artifact.createdAt,
                ),
                onClick = { onOpen(artifact.id) },
            )
        }
    }
}

@Composable
private fun MeetingsPage(onBack: () -> Unit) {
    PageColumn("My Meetings", onBack) {
        Text(
            "Orhan does not record meetings, and it does not keep minutes or playback. Chat can attach an audio file you already have, but that file is not a meeting.",
            style = OrhanTextStyles.snippet,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(12.dp))
        FootNote("There is no player on this screen. Nothing is recorded when you open it.")
    }
}

@Composable
private fun TopicEditor(
    topic: MemoryTopic?,
    groups: List<String>,
    artifacts: List<Artifact>?,
    onBack: () -> Unit,
    onSave: suspend (String, String, String, String, List<String>, String) -> Boolean,
    onDelete: suspend (String) -> Boolean,
    onOpenSource: (String, Long) -> Unit,
    onOpenArtifact: (String) -> Unit,
    snackbar: SnackbarHostState,
) {
    val scope = rememberCoroutineScope()
    var title by rememberSaveable(topic?.id) { mutableStateOf(topic?.title.orEmpty()) }
    var summary by rememberSaveable(topic?.id) { mutableStateOf(topic?.summary.orEmpty()) }
    var content by rememberSaveable(topic?.id) { mutableStateOf(topic?.content.orEmpty()) }
    var group by rememberSaveable(topic?.id) { mutableStateOf(topic?.group ?: DEFAULT_TOPIC_GROUP) }
    var tagDraft by rememberSaveable(topic?.id) { mutableStateOf("") }
    var tags by rememberSaveable(topic?.id, stateSaver = StringListSaver) { mutableStateOf(topic?.tags.orEmpty()) }
    var artifactId by rememberSaveable(topic?.id) { mutableStateOf(topic?.artifactId.orEmpty()) }
    var pickingArtifact by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    var deleteError by rememberSaveable { mutableStateOf<String?>(null) }
    var saveError by rememberSaveable { mutableStateOf<String?>(null) }
    PageColumn(
        title = "Memory",
        onBack = onBack,
        actions = {
            if (topic != null) {
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Rounded.Delete, "Delete topic", tint = MaterialTheme.colorScheme.error)
                }
            }
            IconButton(
                onClick = {
                    scope.launch {
                        val pending = if (tagDraft.isNotBlank()) tags + tagDraft else tags
                        if (onSave(title, summary, content, group, pending, artifactId)) {
                            saveError = null
                            onBack()
                        } else {
                            val message = "Couldn't save this topic. Your edits are still here."
                            saveError = message
                            snackbar.showSnackbar(message)
                        }
                    }
                },
                enabled = title.isNotBlank(),
            ) {
                Icon(Icons.Rounded.Check, "Save topic", tint = MaterialTheme.colorScheme.primary)
            }
        },
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it.take(MAX_ENTRY_TITLE) },
            placeholder = { Text("Untitled") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = summary,
            onValueChange = { summary = it.take(MAX_SUMMARY) },
            placeholder = { Text("Add a summary") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        OutlinedTextField(
            value = group,
            onValueChange = { group = it.take(MAX_GROUP_NAME) },
            label = { Text("Group") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        if (groups.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                groups.forEach { name ->
                    TextChip(label = name, selected = name.equals(group, true), onClick = { group = name })
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            OutlinedTextField(
                value = tagDraft,
                onValueChange = { tagDraft = it.take(32) },
                placeholder = { Text("add tag") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = {
                    val tag = normalizeTag(tagDraft)
                    if (tag.isNotEmpty() && tags.none { it.equals(tag, true) }) tags = tags + tag
                    tagDraft = ""
                },
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text("Add") }
        }
        if (tags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                tags.forEach { tag ->
                    TextChip(label = "#$tag", onClick = { tags = tags.filterNot { it == tag } })
                }
            }
        }
        OutlinedTextField(
            value = content,
            onValueChange = { content = it.take(MAX_NOTE_CONTENT) },
            placeholder = { Text("Write something…") },
            minLines = 6,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).heightIn(min = 160.dp),
        )
        saveError?.let { message ->
            Text(
                message,
                style = OrhanTextStyles.snippet,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        val sourceTitle = topic?.sourceChatTitle.orEmpty()
        if (sourceTitle.isNotBlank()) {
            TextButton(
                onClick = { onOpenSource(sourceTitle, topic?.sourceMessageId ?: 0L) },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .semantics { contentDescription = "Open source chat" },
            ) { Text("Open source chat") }
        }
        ArtifactLinkRow(
            artifactId = artifactId,
            artifacts = artifacts,
            onPick = { pickingArtifact = true },
            onOpen = onOpenArtifact,
            onUnlink = { artifactId = "" },
        )
        FootNote("Topics stay on this device unless Use topics in chats is on. Pause memory stops that. A source chat opens the message this topic came from. A linked artifact opens the saved document. This topic does not keep a second copy.")
    }
    if (pickingArtifact) {
        AlertDialog(
            onDismissRequest = { pickingArtifact = false },
            title = { Text("Link an artifact") },
            text = {
                if (artifacts.isNullOrEmpty()) {
                    Text("No artifacts yet. Save one from a chat reply first.")
                } else {
                    Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                        artifacts.forEach { artifact ->
                            TextButton(
                                onClick = {
                                    artifactId = artifact.id
                                    pickingArtifact = false
                                },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            ) {
                                Text(artifact.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { pickingArtifact = false }) { Text("Cancel") }
            },
        )
    }
    if (confirmDelete && topic != null) {
        ConfirmDelete(
            title = "Delete topic?",
            body = deleteError ?: "\"${topic.title}\" will be removed from this device.",
            confirmEnabled = !deleting,
            onDismiss = {
                if (!deleting) {
                    confirmDelete = false
                    deleteError = null
                }
            },
            onConfirm = {
                if (deleting) return@ConfirmDelete
                val id = topic.id
                deleting = true
                scope.launch {
                    val removed = onDelete(id)
                    deleting = false
                    if (removed) {
                        confirmDelete = false
                        deleteError = null
                        onBack()
                    } else {
                        deleteError = "Couldn't delete this topic. It is still saved on this device."
                        snackbar.showSnackbar("Couldn't delete this topic. It is still saved on this device.")
                    }
                }
            },
        )
    }
}

@Composable
private fun ArtifactLinkRow(
    artifactId: String,
    artifacts: List<Artifact>?,
    onPick: () -> Unit,
    onOpen: (String) -> Unit,
    onUnlink: () -> Unit,
) {
    if (artifactId.isBlank()) {
        TextButton(onClick = onPick, modifier = Modifier.heightIn(min = 48.dp)) { Text("Link an artifact") }
        return
    }
    if (artifacts == null) {
        Text(
            "Checking the saved document…",
            style = OrhanTextStyles.snippet,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        return
    }
    when (val link = artifactLink(artifactId, artifacts)) {
        ArtifactLink.None -> TextButton(onClick = onPick, modifier = Modifier.heightIn(min = 48.dp)) { Text("Link an artifact") }
        is ArtifactLink.Ready -> {
            Text(link.title, style = OrhanTextStyles.rowTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row {
                TextButton(
                    onClick = { onOpen(link.id) },
                    modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Open artifact" },
                ) { Text("Open artifact") }
                TextButton(onClick = onUnlink, modifier = Modifier.heightIn(min = 48.dp)) { Text("Unlink") }
            }
        }
        ArtifactLink.Missing -> {
            Text(
                "This artifact is no longer on this device.",
                style = OrhanTextStyles.snippet,
                color = MaterialTheme.colorScheme.error,
            )
            TextButton(onClick = onUnlink, modifier = Modifier.heightIn(min = 48.dp)) { Text("Unlink") }
        }
    }
}

@Composable
private fun TodoDialog(
    initial: MemoryEntry,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: suspend (String, String) -> ActionSaveResult,
) {
    val scope = rememberCoroutineScope()
    var title by remember(initial.id, isNew) { mutableStateOf(initial.title) }
    var content by remember(initial.id, isNew) { mutableStateOf(initial.content) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(if (isNew) "New to-do" else "Edit to-do") },
        text = {
            Column {
                OutlinedTextField(value = title, onValueChange = { title = it.take(MAX_ENTRY_TITLE) }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = content, onValueChange = { content = it.take(MAX_TODO_CONTENT) }, label = { Text("Details (optional)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                error?.let { message ->
                    Text(message, style = OrhanTextStyles.snippet, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank() && !saving,
                onClick = {
                    scope.launch {
                        saving = true
                        error = null
                        when (onSave(title, content)) {
                            ActionSaveResult.Saved -> onDismiss()
                            ActionSaveResult.Duplicate -> {
                                error = "That action is already saved."
                                saving = false
                            }
                            else -> {
                                error = "Couldn't save this action. Your edits are still here."
                                saving = false
                            }
                        }
                    }
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") } },
    )
}

@Composable
private fun ConfirmDelete(
    title: String,
    body: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = onConfirm, enabled = confirmEnabled) { Text("Delete") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = confirmEnabled) { Text("Cancel") } },
    )
}

@Composable
private fun PageColumn(
    title: String,
    onBack: () -> Unit,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
        MemoryTitle(title, showBack = true, onBack = onBack, actions = actions)
        content()
    }
}

@Composable
private fun MemoryTitle(
    title: String,
    showBack: Boolean,
    onBack: () -> Unit,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
) {
    Box(Modifier.fillMaxWidth().heightIn(min = 56.dp), contentAlignment = Alignment.Center) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 96.dp),
        )
        if (showBack) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart).size(48.dp)) {
                Icon(Icons.Rounded.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
        }
        Row(Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

@Composable
private fun SearchField(value: String, onQuery: (String) -> Unit) {
    val colors = OrhanTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(OrhanTheme.shapes.pill)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, colors.fieldEdge, OrhanTheme.shapes.pill)
            .semantics { contentDescription = "Search topics" }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onQuery,
            modifier = Modifier.weight(1f).padding(vertical = 12.dp),
            singleLine = true,
            textStyle = OrhanTextStyles.snippet.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { inner ->
                if (value.isEmpty()) Text("Search topics", style = OrhanTextStyles.snippet, color = MaterialTheme.colorScheme.onSurfaceVariant)
                inner()
            },
        )
    }
}

@Composable
private fun FilterChip(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    val colors = OrhanTheme.colors
    val background = if (selected) colors.brandSoft else MaterialTheme.colorScheme.surface
    val ink = if (selected) colors.onBrandSoft else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(CircleShape)
            .background(background)
            .border(1.dp, if (selected) Color.Transparent else colors.fieldEdge, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = OrhanTextStyles.button, color = ink)
        Spacer(Modifier.width(6.dp))
        Text(count.toString(), style = OrhanTextStyles.meta, color = ink)
    }
}

@Composable
private fun TextChip(label: String, selected: Boolean = false, onClick: () -> Unit) {
    val colors = OrhanTheme.colors
    Text(
        label,
        style = OrhanTextStyles.snippet,
        color = if (selected) colors.onBrandSoft else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(CircleShape)
            .background(if (selected) colors.brandSoft else MaterialTheme.colorScheme.surface)
            .border(1.dp, colors.fieldEdge, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun EmptyCopy(text: String) {
    Text(text, style = OrhanTextStyles.snippet, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 24.dp))
}
