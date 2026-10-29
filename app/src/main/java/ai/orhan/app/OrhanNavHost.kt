package ai.orhan.app

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
import ai.orhan.data.memory.memoryStore
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
import ai.orhan.data.agents.SavedAgent
import ai.orhan.data.agents.agentStore
import ai.orhan.data.agents.mergeWithSamples
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
import kotlinx.coroutines.launch
import ai.orhan.data.artifacts.artifactStore
import ai.orhan.feature.artifacts.ArtifactsScreen
import ai.orhan.data.workflows.workflowStore
import ai.orhan.feature.workflows.WorkflowEditorScreen

/** Top-level routes. Settings routes live in [SettingsRoutes]. */
object AppRoutes {
    const val HOME = "home"
    const val CREATE_AGENT = "create_agent"
    const val EDIT_PROFILE = "edit_profile"
    const val MEMORY = "memory"
    const val ARTIFACTS = "artifacts"
    const val CHAT = "chat/{title}?draft={draft}"
    fun chat(title: String, draft: String = "") =
        "chat/${Uri.encode(title)}" + if (draft.isNotBlank()) "?draft=${Uri.encode(draft)}" else ""
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
    val memoryStore = remember(context) { context.memoryStore() }
    val artifactStore = remember(context) { context.artifactStore() }
    val artifacts by artifactStore.artifacts.collectAsStateWithLifecycle(initialValue = emptyList())
    val workflowStore = remember(context) { context.workflowStore() }
    val workflows by workflowStore.workflows.collectAsStateWithLifecycle(initialValue = emptyList())
    val personalMemory by memoryStore.memory.collectAsStateWithLifecycle(initialValue = "")

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
                    scope.launch { recentRepo.recordOpened(it.title) }
                    navController.navigate(AppRoutes.chat(it.title))
                },
                onOpenSettings = {
                    navController.navigate(SettingsRoutes.SETTINGS) { launchSingleTop = true }
                },
                onOpenAppearance = { navController.openAppearance() },
                onCreateAgent = {
                    navController.navigate(AppRoutes.CREATE_AGENT) { launchSingleTop = true }
                },
                onEditProfile = {
                    navController.navigate(AppRoutes.EDIT_PROFILE) { launchSingleTop = true }
                },
                onOpenMemory = {
                    navController.navigate(AppRoutes.MEMORY) { launchSingleTop = true }
                },
                onOpenArtifacts = {
                    navController.navigate(AppRoutes.ARTIFACTS) { launchSingleTop = true }
                },
                artifactCount = artifacts.size,
                onOpenChatPreferences = { navController.openSettingsPage(SettingsRoutes.CHAT_PREFERENCES) },
                onOpenSettingsPage = { route -> navController.openSettingsPage(route) },
                onSignOut = if (homeSession != null) {
                    { scope.launch { homeAuth.signOut() } }
                } else null,
                workflows = workflows,
                onOpenWorkflow = { wf ->
                    val chatTitle = wf.agentName.ifBlank { wf.title }
                    scope.launch { recentRepo.recordOpened(chatTitle) }
                    navController.navigate(AppRoutes.chat(chatTitle, wf.content))
                },
                onOpenPlaybook = { pb ->
                    // Chat route resolves saved/sample agent instructions by title, same as workflows.
                    val chatTitle = pb.agentName.ifBlank { pb.title }
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
            MemoryScreen(onBack = { navController.popBackStack() })
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
        composable(AppRoutes.CREATE_AGENT) {
            CreateAgentScreen(
                onBack = { navController.popBackStack() },
                onCreated = { name, blurb, instructions, tools ->
                    scope.launch {
                        agentStore.upsert(
                            SavedAgent(
                                name = name,
                                blurb = blurb,
                                instructions = instructions,
                                tools = tools,
                                createdAt = System.currentTimeMillis(),
                            ),
                        )
                        navController.navigate(AppRoutes.chat(name)) {
                            popUpTo(AppRoutes.CREATE_AGENT) { inclusive = true }
                        }
                    }
                },
            )
        }
        composable(
            AppRoutes.CHAT,
            arguments = listOf(
                navArgument("title") { type = NavType.StringType },
                navArgument("draft") { type = NavType.StringType; defaultValue = "" },
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
            val title = entry.arguments?.getString("title").orEmpty()
            LaunchedEffect(title) { chatViewModel.loadThread(title) }
            val agent = savedAgents.firstOrNull { it.name.equals(title, ignoreCase = true) }
            chatViewModel.setAgent(agent?.name ?: title, agent?.instructions.orEmpty(), agent?.tools.orEmpty())
            chatViewModel.setPersonalMemory(personalMemory)
            val chatPrefsRepo = remember(context) { context.chatPreferences() }
            val chatPrefs by chatPrefsRepo.prefs.collectAsStateWithLifecycle(initialValue = ChatPrefs())
            ChatScreen(
                agentName = title,
                onBack = { navController.popBackStack() },
                messagesOverride = chat.messages,
                loading = chat.loading,
                busy = chat.busy,
                error = chat.error,
                onSendMessage = chatViewModel::send,
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
                onOpenChatPreferences = { navController.openSettingsPage(SettingsRoutes.CHAT_PREFERENCES) },
            )
        }
        composable(SettingsRoutes.SETTINGS) {
            val auth = remember(context) { AuthRepository.get(context) }
            val session by auth.session.collectAsStateWithLifecycle(initialValue = null)
            SettingsScreen(
                onBack = { navController.popBackStack() },
onOpenAppearance = { navController.openAppearance() },
                onOpenNotifications = { navController.openSettingsPage(SettingsRoutes.NOTIFICATIONS) },
                onOpenPrivacy = { navController.openSettingsPage(SettingsRoutes.PRIVACY) },
                onOpenChatPreferences = { navController.openSettingsPage(SettingsRoutes.CHAT_PREFERENCES) },
                onOpenStorage = { navController.openSettingsPage(SettingsRoutes.STORAGE) },
                onOpenLanguage = { navController.openSettingsPage(SettingsRoutes.LANGUAGE) },
                onOpenAbout = { navController.openSettingsPage(SettingsRoutes.ABOUT) },
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

/** Compact "now / 5m / 3h / 2d" label for a chat-list row. */
internal fun relativeTime(then: Long, now: Long = System.currentTimeMillis()): String {
    val m = ((now - then).coerceAtLeast(0)) / 60_000
    return when {
        m < 1 -> "now"
        m < 60 -> "${m}m"
        m < 1440 -> "${m / 60}h"
        else -> "${m / 1440}d"
    }
}

/** Single-line preview of a saved message for Home rows; falls back to "Recent chat". */
internal fun previewSnippet(text: String?, max: Int = 100): String {
    val clean = text?.replace(Regex("\\s+"), " ")?.trim().orEmpty()
    if (clean.isEmpty()) return "Recent chat"
    return if (clean.length <= max) clean else clean.take(max).trimEnd() + "…"
}
