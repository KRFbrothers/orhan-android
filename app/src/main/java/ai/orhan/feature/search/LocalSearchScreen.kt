package ai.orhan.feature.search

import ai.orhan.data.artifacts.Artifact
import ai.orhan.design.screens.OrhanChat
import ai.orhan.design.theme.OrhanTextStyles
import ai.orhan.design.theme.OrhanTheme
import ai.orhan.feature.agents.InstalledAgent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date

private enum class SearchTab(val label: String) { All("All"), Chats("Chats"), Agents("Agents"), Artifacts("Artifacts") }

/**
 * Local search over saved chats, agents, and artifacts.
 * Message bodies are not stored on the chat row, so only the title and snippet are matched.
 */
@Composable
fun LocalSearchScreen(
    chats: List<OrhanChat>,
    agents: List<InstalledAgent>,
    artifacts: List<Artifact>,
    onCancel: () -> Unit,
    onOpenChat: (OrhanChat) -> Unit,
    onOpenAgent: (InstalledAgent) -> Unit,
    onOpenArtifact: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OrhanTheme.colors
    var query by rememberSaveable { mutableStateOf("") }
    var tab by rememberSaveable { mutableStateOf(SearchTab.All) }
    val focus = FocusRequester()
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) { focus.requestFocus() }
    val q = query.trim()
    val matchedChats = chats.filter { q.isEmpty() || it.title.contains(q, true) || it.snippet.contains(q, true) }
    val matchedAgents = agents.filter { q.isEmpty() || it.name.contains(q, true) || it.blurb.contains(q, true) }
    val matchedArtifacts = artifacts.filter { q.isEmpty() || it.title.contains(q, true) || it.content.contains(q, true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(
                WindowInsets.ime.union(WindowInsets.navigationBars).only(WindowInsetsSides.Bottom),
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(OrhanTheme.shapes.pill)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, colors.fieldEdge, OrhanTheme.shapes.pill)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.Icon(
                    Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.height(18.dp).width(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f).focusRequester(focus),
                    textStyle = OrhanTextStyles.snippet.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text(
                                "Search",
                                style = OrhanTextStyles.snippet,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        inner()
                    },
                )
            }
            TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Cancel", color = MaterialTheme.colorScheme.primary, style = OrhanTextStyles.rowTitle)
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
            SearchTab.entries.forEach { item ->
                val selected = tab == item
                Column(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clickable(role = Role.Tab, onClick = { tab = item })
                        .padding(horizontal = 12.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                ) {
                    Text(
                        item.label,
                        style = OrhanTextStyles.meta,
                        color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    androidx.compose.foundation.layout.Box(
                        Modifier
                            .height(2.dp)
                            .width(24.dp)
                            .background(if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent),
                    )
                }
            }
        }

        val showChats = tab == SearchTab.All || tab == SearchTab.Chats
        val showAgents = tab == SearchTab.All || tab == SearchTab.Agents
        val showArtifacts = tab == SearchTab.All || tab == SearchTab.Artifacts
        val nothing = (if (showChats) matchedChats else emptyList()).isEmpty() &&
            (if (showAgents) matchedAgents else emptyList()).isEmpty() &&
            (if (showArtifacts) matchedArtifacts else emptyList()).isEmpty()
        if (nothing) {
            Text(
                if (q.isEmpty()) "Nothing saved yet." else "No matches",
                style = OrhanTextStyles.snippet,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
            )
            return@Column
        }

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (showChats && matchedChats.isNotEmpty()) {
                item(key = "h-chats") { SectionLabel("Chats", matchedChats.size > 3 && tab == SearchTab.All) { tab = SearchTab.Chats } }
                items(if (tab == SearchTab.All) matchedChats.take(3) else matchedChats, key = { "c:" + it.title }) { chat ->
                    ResultRow(chat.title, chat.snippet.ifBlank { "Recent chat" }) { onOpenChat(chat) }
                }
            }
            if (showAgents && matchedAgents.isNotEmpty()) {
                item(key = "h-agents") { SectionLabel("Agents", matchedAgents.size > 3 && tab == SearchTab.All) { tab = SearchTab.Agents } }
                items(if (tab == SearchTab.All) matchedAgents.take(3) else matchedAgents, key = { "a:" + it.name }) { agent ->
                    ResultRow(agent.name, agent.blurb) { onOpenAgent(agent) }
                }
            }
            if (showArtifacts && matchedArtifacts.isNotEmpty()) {
                item(key = "h-artifacts") {
                    SectionLabel("Artifacts", matchedArtifacts.size > 3 && tab == SearchTab.All) { tab = SearchTab.Artifacts }
                }
                val rows = if (tab == SearchTab.All) matchedArtifacts.take(3) else matchedArtifacts
                items(rows, key = { "f:" + it.id }) { artifact ->
                    ResultRow(artifact.title, formatWhen(artifact.createdAt)) { onOpenArtifact(artifact.id) }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(title: String, viewAll: Boolean, onViewAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title.uppercase(), style = OrhanTextStyles.kicker, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        if (viewAll) {
            TextButton(onClick = onViewAll) {
                Text("View all", color = MaterialTheme.colorScheme.primary, style = OrhanTextStyles.meta)
            }
        }
    }
}

@Composable
private fun ResultRow(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(title, style = OrhanTextStyles.rowTitle, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (subtitle.isNotBlank()) {
            Text(
                subtitle,
                style = OrhanTextStyles.snippet,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

private fun formatWhen(t: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(t))
