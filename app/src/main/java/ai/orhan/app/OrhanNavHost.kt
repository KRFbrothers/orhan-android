package ai.orhan.app

import ai.orhan.feature.settings.detail.LanguagePrefs
import ai.orhan.feature.settings.detail.languageName
import ai.orhan.feature.settings.detail.settingsDetailPreferences
import ai.orhan.data.history.chatThreads
import android.app.Application
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ai.orhan.BuildConfig
import ai.orhan.data.auth.AuthRepository
import ai.orhan.design.screens.ChatScreen
import ai.orhan.feature.agents.CreateAgentScreen
import ai.orhan.feature.profile.EditProfileScreen
import ai.orhan.feature.memory.MemoryScreen
import ai.orhan.data.memory.memoryEntriesStore
import ai.orhan.data.memory.memoryPauseStore
import ai.orhan.data.memory.memoryStore
import ai.orhan.data.memory.topicContextBlock
import ai.orhan.data.memory.updatesCount
import ai.orhan.data.permissions.askBeforeActingStore
import ai.orhan.data.prefs.replyStyleStore
import ai.orhan.data.prefs.toneStore
import ai.orhan.feature.profile.UserProfile
import ai.orhan.feature.profile.profilePreferences
import ai.orhan.feature.settings.detail.ChatPrefs
import ai.orhan.feature.settings.detail.chatPreferences
import ai.orhan.feature.auth.SignInScreen
import ai.orhan.feature.auth.SignInViewModel
import ai.orhan.feature.chat.ChatViewModel
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ai.orhan.design.screens.HomeScreen
import ai.orhan.design.screens.OrhanChat
import ai.orhan.data.history.recentChats
import ai.orhan.data.history.pinnedChats
import ai.orhan.data.agents.AgentSaveNav
import ai.orhan.data.agents.SavedAgent
import ai.orhan.data.agents.agentStore
import ai.orhan.data.agents.mergeWithSamples
import ai.orhan.data.agents.canonicalAgentTitle
import ai.orhan.data.agents.navigationAfterAgentSave
import ai.orhan.data.agents.toInstalledAgent
import ai.orhan.design.theme.ThemeMode
import ai.orhan.design.theme.themePreferences
import ai.orhan.feature.settings.SettingsRoutes
import ai.orhan.feature.settings.SettingsScreen
import ai.orhan.feature.settings.appearanceViewModel
import ai.orhan.feature.settings.appearance.AppearanceScreen
import ai.orhan.feature.settings.openAppearance
import ai.orhan.feature.settings.openSettingsPage
import ai.orhan.feature.settings.settingsDetailPages
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import ai.orhan.data.artifacts.artifactStore
import ai.orhan.feature.artifacts.ArtifactsScreen
import ai.orhan.data.workflows.workflowStore
import ai.orhan.feature.workflows.WorkflowEditorScreen

/** Top-level routes. Settings routes live in [SettingsRoutes]. */
object AppRoutes {
    const val HOME = "home"
    /** Optional `name` opens the same screen in edit mode for a saved agent. */
    const val CREATE_AGENT = "create_agent?name={name}"
    fun createAgent(name: String? = null): String =
        if (name.isNullOrBlank()) "create_agent" else "create_agent?name=${Uri.encode(name)}"
    const val EDIT_PROFILE = "edit_profile"
    const val MEMORY = "memory"
    const val ARTIFACTS = "artifacts"
    const val CHAT = "chat/{title}?draft={draft}&message={message}"
    fun chat(title: String, draft: String = "", messageId: Long = 0L) =
        "chat/${Uri.encode(title)}?draft=${Uri.encode(draft)}&message=$messageId"
    const val WORKFLOW_EDIT = "workflow_edit?id={id}"
    fun workflowEdit(id: String? = null) = if (id == null) "workflow_edit" else "workflow_edit?id=${Uri.encode(id)}"
}

/**
 * App navigation: Home -> Chat, Home (overflow / Profile) -> Settings -> Appearance.
 * Theme toggle on Home writes the same DataStore that Appearance edits.
 */
@Composable
fun OrhanNavHost(isDark: Boolean) {
    val navController = rememberNavController()
    val context = LocalContext.current.applicationContext
    val preferences = remember(context) { context.themePreferences() }
    val profilePreferences = remember(context) { context.profilePreferences() }
    val profile by profilePreferences.profile.collectAsStateWithLifecycle(initialValue = UserProfile())
    val scope = rememberCoroutineScope()
    val recentRepo = remember(context) { context.recentChats() }
    val recent by recentRepo.recent.collectAsStateWithLifecycle(initialValue = emptyList())
    val threadStore = remember(context) { context.chatThreads() }
    val threads by threadStore.threads.collectAsStateWithLifecycle(initialValue = emptyList())
    val pinRepo = remember(context) { context.pinnedChats() }
    val pinnedTitles by pinRepo.pinned.collectAsStateWithLifecycle(initialValue = emptySet())
    val agentStore = remember(context) { context.agentStore() }
    val savedAgents by agentStore.agents.collectAsStateWithLifecycle(initialValue = emptyList())
    // Canonical names (saved + samples) so "writer" and "Writer" share one thread/recent/pin key.
    val canon: (String) -> String = { t -> canonicalAgentTitle(t, mergeWithSamples(savedAgents.map { it.toInstalledAgent() }).map { it.name }) }
    val memoryStore = remember(context) { context.memoryStore() }
    val memoryEntries = remember(context) { context.memoryEntriesStore() }
    val memoryTopics by memoryEntries.topics.collectAsStateWithLifecycle(initialValue = emptyList())
    val artifactStore = remember(context) { context.artifactStore() }
    val artifacts by artifactStore.artifacts.collectAsStateWithLifecycle(initialValue = emptyList())
    val workflowStore = remember(context) { context.workflowStore() }
    val workflows by workflowStore.workflows.collectAsStateWithLifecycle(initialValue = emptyList())
    val personalMemory by memoryStore.memory.collectAsStateWithLifecycle(initialValue = "")
    // Missing key stays not-paused, matching the installed preference. A failed
    // read makes [MemoryPauseStore.paused] true so personal context is withheld.
    val memoryPauseStore = remember(context) { context.memoryPauseStore() }
    val memoryPaused by memoryPauseStore.paused.collectAsStateWithLifecycle(initialValue = false)
    val memoryChoices by memoryPauseStore.state.collectAsStateWithLifecycle(initialValue = ai.orhan.data.memory.MemoryPause())
    val askBeforeActingStore = remember(context) { context.askBeforeActingStore() }
    val askBeforeActing by askBeforeActingStore.enabled.collectAsStateWithLifecycle(initialValue = true)
    val replyStyleStore = remember(context) { context.replyStyleStore() }
    val replyStyle by replyStyleStore.style.collectAsStateWithLifecycle(initialValue = ai.orhan.data.prefs.ReplyStyle.Balanced)
    val toneStore = remember(context) { context.toneStore() }
    val agentTone by toneStore.tone.collectAsStateWithLifecycle(initialValue = ai.orhan.data.prefs.AgentTone.Friendly)

    NavHost(navController = navController, startDestination = AppRoutes.HOME) {
        composable(AppRoutes.HOME) {
            val homeAuth = remember(context) { AuthRepository.get(context) }
            val homeSession by homeAuth.session.collectAsStateWithLifecycle(initialValue = null)
            HomeScreen(
                isDark = isDark,
                profile = profile,
                agents = mergeWithSamples(savedAgents.map { it.toInstalledAgent() }),
                deletableAgentNames = savedAgents.map { it.name }.toSet(),
                onDeleteAgent = { agent -> scope.launch { agentStore.delete(agent.name) } },
                recentChats = recent.map {
                    val lastText = threads.firstOrNull { t -> t.title == it.title }?.messages?.lastOrNull()?.text
                    OrhanChat(it.title, previewSnippet(lastText), relativeTime(it.openedAt), isAgent = true)
                },
                pinnedTitles = pinnedTitles,
                onTogglePin = { chat -> scope.launch { pinRepo.toggle(chat.title) } },
                onRemoveRecent = { chat ->
                    scope.launch {
                        recentRepo.remove(chat.title)
                        pinRepo.unpin(chat.title)
                        threadStore.remove(chat.title)
                    }
                },
                onOpenChat = {
                    val t = canon(it.title)
                    scope.launch { recentRepo.recordOpened(t) }
                    navController.navigate(AppRoutes.chat(t))
                },
                onOpenSettings = {
                    navController.navigate(SettingsRoutes.SETTINGS) { launchSingleTop = true }
                },
                onOpenAppearance = { navController.openAppearance() },
                onCreateAgent = {
                    navController.navigate(AppRoutes.createAgent()) { launchSingleTop = true }
                },
                onEditAgent = { agent ->
                    navController.navigate(AppRoutes.createAgent(agent.name)) { launchSingleTop = true }
                },
                onEditProfile = {
                    navController.navigate(AppRoutes.EDIT_PROFILE) { launchSingleTop = true }
                },
                onOpenMemory = {
                    navController.navigate(AppRoutes.MEMORY) { launchSingleTop = true }
                },
                onOpenMemorySource = { title, messageId ->
                    val t = canon(title)
                    scope.launch { recentRepo.recordOpened(t) }
                    navController.navigate(AppRoutes.chat(t, messageId = messageId))
                },
                onOpenArtifacts = {
                    navController.navigate(AppRoutes.ARTIFACTS) { launchSingleTop = true }
                },
                artifacts = artifacts,
                onUpsertArtifact = { item -> scope.launch { artifactStore.upsert(item) } },
                onDeleteArtifact = { id -> scope.launch { artifactStore.delete(id) } },
                artifactCount = artifacts.size,
                memoryUpdates = updatesCount(memoryTopics),
                onOpenChatPreferences = { navController.openSettingsPage(SettingsRoutes.CHAT_PREFERENCES) },
                onOpenSettingsPage = { route -> navController.openSettingsPage(route) },
                onSignOut = if (homeSession != null) {
                    { scope.launch { homeAuth.signOut() } }
                } else null,
                workflows = workflows,
                onOpenWorkflow = { wf ->
                    val chatTitle = canon(wf.agentName.ifBlank { wf.title })
                    scope.launch { recentRepo.recordOpened(chatTitle) }
                    navController.navigate(AppRoutes.chat(chatTitle, wf.content))
                },
                onOpenPlaybook = { pb ->
                    // Chat route resolves saved/sample agent instructions by title, same as workflows.
                    val chatTitle = canon(pb.agentName.ifBlank { pb.title })
                    scope.launch { recentRepo.recordOpened(chatTitle) }
                    navController.navigate(AppRoutes.chat(chatTitle, pb.prompt))
                },
                onCreateWorkflow = {
                    navController.navigate(AppRoutes.workflowEdit()) { launchSingleTop = true }
                },
                onEditWorkflow = { wf ->
                    navController.navigate(AppRoutes.workflowEdit(wf.id)) { launchSingleTop = true }
                },
                onDeleteWorkflow = { wf -> scope.launch { workflowStore.delete(wf.id) } },
                onToggleTheme = {
                    scope.launch {
                        preferences.setThemeMode(if (isDark) ThemeMode.Light else ThemeMode.Dark)
                    }
                },
            )
        }
        composable(AppRoutes.EDIT_PROFILE) {
            EditProfileScreen(onBack = { navController.popBackStack() })
        }
        composable(AppRoutes.MEMORY) {
            MemoryScreen(
                onBack = { navController.popBackStack() },
                onEditProfile = {
                    navController.navigate(AppRoutes.EDIT_PROFILE) { launchSingleTop = true }
                },
                onOpenSource = { title, messageId ->
                    val t = canon(title)
                    scope.launch { recentRepo.recordOpened(t) }
                    navController.navigate(AppRoutes.chat(t, messageId = messageId))
                },
            )
        }
        composable(AppRoutes.ARTIFACTS) {
            ArtifactsScreen(
                artifacts = artifacts,
                onUpsert = { a -> scope.launch { artifactStore.upsert(a) } },
                onDelete = { id -> scope.launch { artifactStore.delete(id) } },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            AppRoutes.WORKFLOW_EDIT,
            arguments = listOf(navArgument("id") { type = NavType.StringType; nullable = true; defaultValue = null }),
        ) { entry ->
            val id = entry.arguments?.getString("id")
            val existing = id?.let { wid -> workflows.firstOrNull { it.id == wid } }
            WorkflowEditorScreen(
                existing = existing,
                agentNames = mergeWithSamples(savedAgents.map { it.toInstalledAgent() }).map { it.name },
                onBack = { navController.popBackStack() },
                onSave = { wf ->
                    scope.launch {
                        workflowStore.upsert(wf)
                        navController.popBackStack()
                    }
                },
                onDelete = { wf ->
                    scope.launch {
                        workflowStore.delete(wf.id)
                        navController.popBackStack()
                    }
                },
            )
        }
        composable(
            AppRoutes.CREATE_AGENT,
            arguments = listOf(
                navArgument("name") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            val editName = entry.arguments?.getString("name")
            val existing = editName?.let { n -> savedAgents.firstOrNull { it.name.equals(n, ignoreCase = true) } }
            if (editName != null && existing == null) {
                // DataStore's first frame is empty. Pop only after the store has been read.
                LaunchedEffect(editName) {
                    val loaded = agentStore.agents.first()
                    val found = loaded.any { it.name.equals(editName, ignoreCase = true) }
                    if (!found) navController.popBackStack()
                }
                return@composable
            }
            CreateAgentScreen(
                onBack = { navController.popBackStack() },
                existing = existing,
                takenNames = savedAgents.map { it.name }.toSet(),
                onCreated = { name, blurb, instructions, tools ->
                    val previous = existing
                    val result = agentStore.replace(
                        previousName = previous?.name,
                        agent = SavedAgent(
                            name = name,
                            blurb = blurb,
                            instructions = instructions,
                            tools = tools,
                            createdAt = previous?.createdAt ?: System.currentTimeMillis(),
                        ),
                    )
                    when (val nav = navigationAfterAgentSave(previous == null, result)) {
                        is AgentSaveNav.OpenChat -> navController.navigate(AppRoutes.chat(canon(nav.name))) {
                            popUpTo(AppRoutes.CREATE_AGENT) { inclusive = true }
                        }
                        AgentSaveNav.Close -> navController.popBackStack()
                        is AgentSaveNav.Stay -> Unit
                    }
                    result
                },
            )
        }
        composable(
            AppRoutes.CHAT,
            arguments = listOf(
                navArgument("title") { type = NavType.StringType },
                navArgument("draft") { type = NavType.StringType; defaultValue = "" },
                navArgument("message") { type = NavType.StringType; defaultValue = "0" },
            ),
        ) { entry ->
            val auth = remember(context) { AuthRepository.get(context) }
            val session by auth.session.collectAsStateWithLifecycle(initialValue = null)
            val hasDevJwt = BuildConfig.DUA_DEV_JWT.isNotBlank()
            if (session == null && !hasDevJwt) {
                val signIn: SignInViewModel = viewModel()
                val signInState by signIn.state.collectAsStateWithLifecycle()
                SignInScreen(
                    loading = signInState.loading,
                    error = signInState.error,
                    configured = signIn.isConfigured,
                    onSignIn = signIn::signIn,
                    onBack = { navController.popBackStack() },
                )
                return@composable
            }
            val chatViewModel: ChatViewModel = viewModel { ChatViewModel(context as Application) }
            val chat by chatViewModel.state.collectAsStateWithLifecycle()
            val title = canon(entry.arguments?.getString("title").orEmpty())
            LaunchedEffect(title) { chatViewModel.loadThread(title) }
            val agent = savedAgents.firstOrNull { it.name.equals(title, ignoreCase = true) }
            chatViewModel.setAgent(agent?.name ?: title, agent?.instructions.orEmpty(), agent?.tools.orEmpty())
            chatViewModel.setRoster(
                if (agent != null) emptyList()
                else savedAgents.map { ai.orhan.feature.chat.RosterAgent(it.name, it.blurb, it.instructions, it.tools) },
            )
            chatViewModel.setPersonalMemory(personalMemory, memoryPaused)
            chatViewModel.setTopicContext(
                topicContextBlock(
                    memoryTopics,
                    include = memoryChoices.includeTopics,
                    paused = memoryChoices.paused || memoryChoices.unreadable,
                ),
            )
            chatViewModel.setAskBeforeActing(askBeforeActing)
            chatViewModel.setReplyStyle(replyStyle)
            chatViewModel.setTone(agentTone)
            chatViewModel.setUserProfile(profile.name, profile.bio, profile.role, profile.timezone)
            val languageRepo = remember(context) { context.settingsDetailPreferences() }
            val languagePrefs by languageRepo.language.collectAsStateWithLifecycle(initialValue = LanguagePrefs())
            chatViewModel.setReplyLanguage(if (languagePrefs.agentsUseAppLanguage) languageName(languagePrefs.code) else null)
            val chatPrefsRepo = remember(context) { context.chatPreferences() }
            val chatPrefs by chatPrefsRepo.prefs.collectAsStateWithLifecycle(initialValue = ChatPrefs())
            ChatScreen(
                agentName = title,
                onBack = { navController.popBackStack() },
                messagesOverride = chat.messages,
                loading = chat.loading,
                busy = chat.busy,
                activityLabel = chat.activityLabel,
                error = chat.error,
                onSendMessage = { chatViewModel.send(it) },
                onSendAttachment = { text, file -> chatViewModel.send(text, file) },
                onRetry = chatViewModel::retry,
                onStopGeneration = { chatViewModel.stop() },
                sendWithEnter = chatPrefs.sendWithEnter,
                showTypingIndicator = chatPrefs.typingIndicators,
                showActivityCards = chatPrefs.agentActivityCards,
                previewBeforeSending = chatPrefs.previewBeforeSending,
                onSaveArtifact = { artTitle, content ->
                    scope.launch { artifactStore.upsert(artTitle, content) }
                },
                initialComposerText = entry.arguments?.getString("draft").orEmpty(),
                focusMessageId = entry.arguments?.getString("message")?.toLongOrNull() ?: 0L,
                threadRestored = chat.threadRestored,
                onOpenChatPreferences = { navController.openSettingsPage(SettingsRoutes.CHAT_PREFERENCES) },
            )
        }
        composable(SettingsRoutes.SETTINGS) {
            val auth = remember(context) { AuthRepository.get(context) }
            val session by auth.session.collectAsStateWithLifecycle(initialValue = null)
            val languageRepo = remember(context) { context.settingsDetailPreferences() }
            val languagePrefs by languageRepo.language.collectAsStateWithLifecycle(initialValue = LanguagePrefs())
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenAppearance = { navController.openAppearance() },
                onOpenNotifications = { navController.openSettingsPage(SettingsRoutes.NOTIFICATIONS) },
                onOpenPrivacy = { navController.openSettingsPage(SettingsRoutes.PRIVACY) },
                onOpenChatPreferences = { navController.openSettingsPage(SettingsRoutes.CHAT_PREFERENCES) },
                onOpenStorage = { navController.openSettingsPage(SettingsRoutes.STORAGE) },
                onOpenLanguage = { navController.openSettingsPage(SettingsRoutes.LANGUAGE) },
                onOpenAbout = { navController.openSettingsPage(SettingsRoutes.ABOUT) },
                languageSubtitle = languageName(languagePrefs.code),
                signedInEmail = session?.email,
                // Only offered while a session exists; clears it via AuthRepository.signOut().
                onSignOut = if (session != null) {
                    {
                        scope.launch {
                            auth.signOut()
                            navController.navigate(AppRoutes.HOME) {
                                popUpTo(AppRoutes.HOME) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                } else null,
            )
        }
        composable(SettingsRoutes.APPEARANCE) {
            AppearanceScreen(
                viewModel = appearanceViewModel(),
                onBack = { navController.popBackStack() },
            )
        }
        settingsDetailPages(navController)
    }
}

/**
 * Chat-list date. Within the last six days this is a short weekday ("Tue");
 * older rows use a numeric month/day/year ("10/21/26").
 */
internal fun relativeTime(then: Long, now: Long = System.currentTimeMillis()): String {
    val zone = ZoneId.systemDefault()
    val opened = Instant.ofEpochMilli(then).atZone(zone).toLocalDate()
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val days = ChronoUnit.DAYS.between(opened, today)
    return if (days in 0..6) {
        opened.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    } else {
        DateTimeFormatter.ofPattern("M/d/yy", Locale.US).format(opened)
    }
}

/** Single-line preview of a saved message for Home rows; falls back to "Recent chat". */
internal fun previewSnippet(text: String?, max: Int = 100): String {
    val clean = text?.replace(Regex("\\s+"), " ")?.trim().orEmpty()
    if (clean.isEmpty()) return "Recent chat"
    return if (clean.length <= max) clean else clean.take(max).trimEnd() + "…"
}
