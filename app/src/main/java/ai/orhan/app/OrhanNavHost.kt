package ai.orhan.app

import ai.orhan.feature.settings.detail.LanguagePrefs
import ai.orhan.feature.settings.detail.languageName
import ai.orhan.feature.settings.detail.settingsDetailPreferences
import ai.orhan.data.history.chatThreads
import android.app.Application
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import ai.orhan.feature.agents.AgentProfileScreen
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
import ai.orhan.feature.chat.explicitAgentMention
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ai.orhan.design.screens.HomeScreen
import ai.orhan.design.screens.OrhanChat
import ai.orhan.data.history.recentChats
import ai.orhan.data.history.pinnedChats
import ai.orhan.data.agents.AgentSaveNav
import ai.orhan.data.agents.SavedAgent
import ai.orhan.data.agents.agentStore
import ai.orhan.data.agents.agentAvatarFiles
import ai.orhan.data.agents.mergeWithSamples
import ai.orhan.data.agents.canonicalAgentTitle
import ai.orhan.data.agents.chatPersonaName
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import ai.orhan.data.artifacts.artifactStore
import ai.orhan.data.chat.ChatRepository
import ai.orhan.feature.artifacts.ArtifactsScreen
import ai.orhan.data.workflows.workflowStore
import ai.orhan.data.workflows.resolveWorkflowAgentName
import ai.orhan.data.workflows.WorkflowRunStatus
import ai.orhan.data.workflows.workflowRunStore
import ai.orhan.data.workflows.WorkflowRunService
import ai.orhan.feature.workflows.WorkflowEditorScreen

/** Top-level routes. Settings routes live in [SettingsRoutes]. */
object AppRoutes {
    const val HOME = "home"
    /** Optional `name` opens the same screen in edit mode for a saved agent. */
    const val CREATE_AGENT = "create_agent?name={name}"
    const val AGENT_PROFILE = "agent_profile/{name}"
    fun agentProfile(name: String) = "agent_profile/${Uri.encode(name)}"
    fun createAgent(name: String? = null): String =
        if (name.isNullOrBlank()) "create_agent" else "create_agent?name=${Uri.encode(name)}"
    const val EDIT_PROFILE = "edit_profile"
    const val MEMORY = "memory"
    const val ARTIFACTS = "artifacts?open={open}"
    fun artifacts(openId: String = ""): String =
        if (openId.isBlank()) "artifacts" else "artifacts?open=${Uri.encode(openId)}"
    const val CHAT = "chat/{title}?draft={draft}&message={message}&run={run}"
    fun chat(title: String, draft: String = "", messageId: Long = 0L, runId: String = "") =
        "chat/${Uri.encode(title)}?draft=${Uri.encode(draft)}&message=$messageId&run=${Uri.encode(runId)}"
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
    val authRepository = remember(context) { AuthRepository.get(context) }
    val authSession by authRepository.session.collectAsStateWithLifecycle(initialValue = null)
    val agentToolRepository = remember(context) {
        ChatRepository(
            tokenProvider = { authRepository.accessToken() },
            onUnauthorized = { authRepository.clear() },
        )
    }
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
    val agentAvatars = remember(context) { context.agentAvatarFiles() }
    val savedAgents by agentStore.agents.collectAsStateWithLifecycle(initialValue = emptyList())
    // Canonical names (saved + samples) so "writer" and "Writer" share one thread/recent/pin key.
    val canon: (String) -> String = { t -> canonicalAgentTitle(t, mergeWithSamples(savedAgents.map { it.toInstalledAgent(agentAvatars) }).map { it.name }) }
    val memoryStore = remember(context) { context.memoryStore() }
    val memoryEntries = remember(context) { context.memoryEntriesStore() }
    val memoryTopics by memoryEntries.topics.collectAsStateWithLifecycle(initialValue = emptyList())
    val artifactStore = remember(context) { context.artifactStore() }
    val artifacts by artifactStore.artifacts.collectAsStateWithLifecycle(initialValue = emptyList())
    val workflowStore = remember(context) { context.workflowStore() }
    val workflows by workflowStore.workflows.collectAsStateWithLifecycle(initialValue = emptyList())
    val workflowRunService = remember(authRepository) { WorkflowRunService(authRepository) }
    val workflowRunStore = remember(context, workflowRunService) { context.workflowRunStore(workflowRunService) }
    val workflowRuns by workflowRunStore.runs.collectAsStateWithLifecycle(initialValue = emptyList())
    var workflowRunsInitialized by remember(workflowRunStore) { mutableStateOf(false) }
    LaunchedEffect(workflowRunStore) {
        workflowRunStore.interruptOrphanedRuns()
        workflowRunsInitialized = true
    }
    LaunchedEffect(workflowRunStore, authSession?.accessToken, workflowRunsInitialized) {
        if (workflowRunsInitialized && authSession != null) workflowRunStore.syncRemote()
    }
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
                agents = mergeWithSamples(savedAgents.map { it.toInstalledAgent(agentAvatars) }),
                deletableAgentNames = savedAgents.map { it.name }.toSet(),
                onDeleteAgent = { agent -> agentStore.delete(agent.name) },
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
                onViewAgent = { agent -> navController.navigate(AppRoutes.agentProfile(agent.name)) },
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
                onOpenArtifactSource = { chatId, messageId ->
                    val t = canon(chatId)
                    scope.launch { recentRepo.recordOpened(t) }
                    navController.navigate(AppRoutes.chat(t, messageId = messageId))
                },
                onOpenArtifacts = {
                    navController.navigate(AppRoutes.artifacts()) { launchSingleTop = true }
                },
                artifacts = artifacts,
                onUpsertArtifact = { item -> artifactStore.upsert(item) },
                onDeleteArtifact = { id -> artifactStore.delete(id) },
                artifactCount = artifacts.size,
                memoryUpdates = updatesCount(memoryTopics),
                onOpenChatPreferences = { navController.openSettingsPage(SettingsRoutes.CHAT_PREFERENCES) },
                onOpenSettingsPage = { route -> navController.openSettingsPage(route) },
                onSignOut = if (homeSession != null) {
                    { scope.launch { homeAuth.signOut() } }
                } else null,
                workflows = workflows,
                onRunWorkflow = { wf ->
                    val requested = wf.agentName.trim()
                    val linked = wf.agentId?.let { id -> savedAgents.firstOrNull { it.id == id } }
                    val legacyMatch = if (wf.agentId == null) savedAgents.firstOrNull { it.name.equals(requested, ignoreCase = true) } else null
                    val selected = linked ?: legacyMatch
                    val isDefault = wf.agentId == null && (requested.isBlank() || requested.equals("Orhan", ignoreCase = true))
                    val chatTitle = selected?.name ?: if (isDefault) "Orhan" else requested
                    scope.launch {
                        val run = try { workflowRunStore.start(if (selected != null) wf.copy(agentId = selected.id) else wf, chatTitle, chatTitle) }
                        catch (cancel: CancellationException) { throw cancel }
                        catch (_: Exception) {
                            android.widget.Toast.makeText(context, "Workflow run could not be saved.", android.widget.Toast.LENGTH_LONG).show()
                            return@launch
                        }
                        if (!isDefault && selected == null) {
                            workflowRunStore.update(run.id, WorkflowRunStatus.FAILED, "The selected agent is missing or was deleted. Edit this workflow to choose an available saved agent.")
                            android.widget.Toast.makeText(context, "Workflow could not run: selected agent is unavailable.", android.widget.Toast.LENGTH_LONG).show()
                        } else {
                            if (wf.agentId == null && selected != null) runCatching { workflowStore.upsert(wf.copy(agentId = selected.id)) }
                            scope.launch { recentRepo.recordOpened(chatTitle) }
                            navController.navigate(AppRoutes.chat(chatTitle, wf.content, runId = run.id))
                        }
                    }
                },
                onOpenWorkflow = { wf ->
                    val available = mergeWithSamples(savedAgents.map { it.toInstalledAgent(agentAvatars) }).map { it.name }
                    val titleMatchedAgent = savedAgents.firstOrNull { it.name.equals(wf.agentName, ignoreCase = true) }
                    if (wf.agentId == null && titleMatchedAgent != null) {
                        scope.launch { workflowStore.upsert(wf.copy(agentId = titleMatchedAgent.id)) }
                    }
                    val resolved = if (wf.agentId != null) {
                        savedAgents.firstOrNull { it.id == wf.agentId }?.name
                    } else resolveWorkflowAgentName(wf.agentName, available)
                    if (resolved == null) {
                        val missing = wf.agentName.trim()
                        android.widget.Toast.makeText(
                            context,
                            "Agent \"$missing\" is unavailable. Edit this workflow to choose another agent.",
                            android.widget.Toast.LENGTH_LONG,
                        ).show()
                    } else {
                        val chatTitle = canon(resolved)
                        scope.launch { recentRepo.recordOpened(chatTitle) }
                        navController.navigate(AppRoutes.chat(chatTitle, wf.content))
                    }
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
                onDeleteWorkflow = { wf -> workflowStore.delete(wf.id); true },
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
                onOpenArtifact = { id ->
                    navController.navigate(AppRoutes.artifacts(id))
                },
            )
        }
        composable(
            AppRoutes.ARTIFACTS,
            arguments = listOf(
                navArgument("open") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { entry ->
            val requested = entry.arguments?.getString("open").orEmpty()
            var pendingOpen by rememberSaveable(requested) { mutableStateOf(requested.ifBlank { null }) }
            ArtifactsScreen(
                artifacts = artifacts,
                onUpsert = { a -> artifactStore.upsert(a) },
                onDelete = { id -> artifactStore.delete(id) },
                onBack = { navController.popBackStack() },
                onOpenSourceMessage = { chatId, messageId ->
                    val t = canon(chatId)
                    scope.launch { recentRepo.recordOpened(t) }
                    navController.navigate(AppRoutes.chat(t, messageId = messageId))
                },
                pendingOpenId = pendingOpen,
                onPendingOpenConsumed = { pendingOpen = null },
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
                runs = workflowRuns.filter { it.workflowId == existing?.id },
                agentNames = savedAgents.map { it.name },
                onBack = { navController.popBackStack() },
                onOpenRunChat = { runChat -> navController.navigate(AppRoutes.chat(runChat)) },
                onResumeRun = { run ->
                    scope.launch {
                        workflowRunStore.update(run.id, WorkflowRunStatus.QUEUED)
                        navController.navigate(AppRoutes.chat(run.chatTitle, runId = run.id))
                    }
                },
                onSave = { wf ->
                    val selected = savedAgents.firstOrNull { it.name.equals(wf.agentName, ignoreCase = true) }
                    workflowStore.upsert(wf.copy(agentId = if (wf.agentName.isBlank()) null else selected?.id ?: wf.agentId))
                    true
                },
                onDelete = { wf ->
                    workflowStore.delete(wf.id)
                    true
                },
            )
        }
        composable(
            AppRoutes.AGENT_PROFILE,
            arguments = listOf(navArgument("name") { type = NavType.StringType }),
        ) { entry ->
            val name = entry.arguments?.getString("name").orEmpty()
            val saved = savedAgents.firstOrNull { it.name.equals(name, ignoreCase = true) }
            AgentProfileScreen(
                agent = saved,
                onBack = { navController.popBackStack() },
                onChat = { navController.navigate(AppRoutes.chat(saved?.name ?: name)) },
                onEdit = saved?.let { current -> ({ navController.navigate(AppRoutes.createAgent(current.name)) }) },
                onDelete = saved?.let { current -> ({
                    scope.launch {
                        if (agentStore.delete(current.name)) navController.popBackStack()
                        else android.widget.Toast.makeText(context, "Couldn't remove this agent. Your saved agent is unchanged.", android.widget.Toast.LENGTH_LONG).show()
                    }
                }) },
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
                repository = agentToolRepository,
                takenNames = savedAgents.map { it.name }.toSet(),
                onCreated = { name, blurb, instructions, tools, toolsRestricted, avatarFileName ->
                    val previous = existing
                    val result = agentStore.replace(
                        previousName = previous?.name,
                        agent = SavedAgent(
                            name = name,
                            blurb = blurb,
                            instructions = instructions,
                            tools = tools,
                            toolsRestricted = toolsRestricted,
                            createdAt = previous?.createdAt ?: System.currentTimeMillis(),
                            avatarFileName = avatarFileName,
                            id = previous?.id ?: java.util.UUID.randomUUID().toString(),
                        ),
                    )
                    if (result is ai.orhan.data.agents.AgentSaveResult.Saved && previous != null && !previous.name.equals(result.agent.name, ignoreCase = true)) {
                        var migrationFailed = false
                        try {
                            threadStore.associateLegacyChatsForRename(previous.name, result.agent.id)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            migrationFailed = true
                        }
                        try {
                            workflowStore.relinkAgentRename(previous.name, result.agent.name, result.agent.id)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            migrationFailed = true
                        }
                        if (migrationFailed) android.widget.Toast.makeText(
                            context,
                            "Agent renamed, but some older chat or workflow links could not be updated. Existing content is unchanged.",
                            android.widget.Toast.LENGTH_LONG,
                        ).show()
                    }
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
                navArgument("run") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { entry ->
            val auth = remember(context) { AuthRepository.get(context) }
            val session by auth.session.collectAsStateWithLifecycle(initialValue = null)
            val hasDevJwt = BuildConfig.DUA_DEV_JWT.isNotBlank()
            val workflowRunId = entry.arguments?.getString("run").orEmpty()
            if (session == null && (!hasDevJwt || workflowRunId.isNotBlank())) {
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
            val workflowRun = workflowRuns.firstOrNull { it.id == workflowRunId }
            var observedWorkflowBusy by rememberSaveable(workflowRunId) { mutableStateOf(false) }
            var workflowCancelRequested by rememberSaveable(workflowRunId) { mutableStateOf(false) }
            LaunchedEffect(title) { chatViewModel.loadThread(title) }
            LaunchedEffect(workflowRunId, chat.threadRestored, workflowRun?.status) {
                if (workflowRunId.isNotBlank() && chat.threadRestored && workflowRun?.status == WorkflowRunStatus.QUEUED) {
                    val run = workflowRun ?: return@LaunchedEffect
                    val currentAgentExists = (run.agentId == null && run.agentName.equals("Orhan", ignoreCase = true)) ||
                        savedAgents.any { saved ->
                            if (run.agentId != null) saved.id == run.agentId
                            else saved.name.equals(run.agentName, ignoreCase = true)
                        }
                    when {
                        !currentAgentExists -> workflowRunStore.update(workflowRunId, WorkflowRunStatus.FAILED, "The selected agent is missing or was deleted before this run started.")
                        chat.busy -> workflowRunStore.update(workflowRunId, WorkflowRunStatus.FAILED, "This chat already has a request in progress. Run the workflow again when it finishes.")
                        else -> {
                            workflowRunStore.update(workflowRunId, WorkflowRunStatus.RUNNING)
                            val saveEngineRunId: suspend (String) -> Unit = { remoteId -> workflowRunStore.saveEngineRunId(workflowRunId, remoteId) }
                            if (run.engineRunId != null) chatViewModel.resumeWorkflowRun(run.engineRunId, saveEngineRunId)
                            else chatViewModel.send(entry.arguments?.getString("draft").orEmpty(), null, saveEngineRunId)
                        }
                    }
                }
            }
            LaunchedEffect(workflowRunId, chat.busy, chat.error, workflowRun?.status) {
                if (workflowRunId.isBlank() || workflowRun == null || workflowRun.status in setOf(
                        WorkflowRunStatus.SUCCEEDED, WorkflowRunStatus.FAILED, WorkflowRunStatus.CANCELLED, WorkflowRunStatus.INTERRUPTED,
                    )) return@LaunchedEffect
                if (chat.busy) observedWorkflowBusy = true
                else if (observedWorkflowBusy) {
                    val status = when {
                        workflowCancelRequested -> WorkflowRunStatus.CANCELLED
                        chat.error != null -> WorkflowRunStatus.FAILED
                        else -> WorkflowRunStatus.SUCCEEDED
                    }
                    workflowRunStore.update(workflowRunId, status, chat.error)
                    observedWorkflowBusy = false
                }
            }
            val threadAgentId = workflowRun?.agentId ?: threads.firstOrNull { it.title == title }?.agentId ?: chat.agentId
            val agent = if (threadAgentId != null) savedAgents.firstOrNull { it.id == threadAgentId }
                else savedAgents.firstOrNull { it.name.equals(title, ignoreCase = true) }
            val missingSavedAgent = !title.equals("Orhan", ignoreCase = true) && agent == null
            val persona = if (threadAgentId != null && agent == null) "Orhan"
                else chatPersonaName(title, savedAgents.map { it.name })
            val mentionRoster = savedAgents.map { savedAgent ->
                ai.orhan.feature.chat.RosterAgent(savedAgent.name, savedAgent.blurb, savedAgent.instructions,
                    savedAgent.tools.takeIf { savedAgent.toolsRestricted })
            }
            val effectivePersona = agent?.name ?: persona
            val allowedTools = when {
                missingSavedAgent -> emptyList()
                agent?.toolsRestricted == true -> agent.tools
                else -> null
            }
            chatViewModel.setAgent(effectivePersona, agent?.instructions.orEmpty(), agent?.id, allowedTools)
            chatViewModel.setRoster(
                if (agent != null && !missingSavedAgent) emptyList()
                else mentionRoster,
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
                agentName = effectivePersona,
                sourceChatId = title,
                onBack = { navController.popBackStack() },
                messagesOverride = chat.messages,
                loading = chat.loading,
                busy = chat.busy,
                activityLabel = chat.activityLabel,
                error = chat.error,
                systemNotice = when {
                    missingSavedAgent -> "This saved agent is unavailable; new replies in this retained conversation use Orhan. The existing history is preserved."
                    agent != null && !agent.name.equals(title, ignoreCase = true) -> "This conversation was started with $title and is now linked to ${agent.name}'s saved profile and current instructions. The history remains stored under its original title."
                    else -> null
                },
                agentMentions = if (title.equals("Orhan", ignoreCase = true)) mentionRoster.map { it.name } else emptyList(),
                onSendMessage = { text ->
                    val mention = explicitAgentMention(text, mentionRoster)
                    if (title.equals("Orhan", true) && mention != null) chatViewModel.sendToAgent(text, mention.first, mention.second)
                    else chatViewModel.send(text)
                },
                onSendAttachment = { text, file ->
                    val mention = explicitAgentMention(text, mentionRoster)
                    if (title.equals("Orhan", true) && mention != null) chatViewModel.sendToAgent(text, mention.first, mention.second, file)
                    else chatViewModel.send(text, file)
                },
                onRetry = chatViewModel::retry,
                onStopGeneration = {
                    if (workflowRunId.isNotBlank() && workflowRun?.status == WorkflowRunStatus.RUNNING) {
                        workflowCancelRequested = true
                        scope.launch { workflowRunStore.update(workflowRunId, WorkflowRunStatus.CANCELLED, "Cancelled by the user.") }
                    }
                    chatViewModel.stop()
                },
                sendWithEnter = chatPrefs.sendWithEnter,
                showTypingIndicator = chatPrefs.typingIndicators,
                showActivityCards = chatPrefs.agentActivityCards,
                previewBeforeSending = chatPrefs.previewBeforeSending,
                onSaveArtifact = { artTitle, content, chatId, messageId, author ->
                    scope.launch {
                        val saved = artifactStore.upsert(
                            ai.orhan.data.artifacts.Artifact(
                                id = java.util.UUID.randomUUID().toString(),
                                title = artTitle,
                                content = content,
                                createdAt = System.currentTimeMillis(),
                                kind = ai.orhan.data.artifacts.ArtifactKind.SAVED_MESSAGE,
                                sourceChatId = chatId,
                                sourceMessageId = messageId,
                                author = author,
                            ),
                        )
                        val notice = if (saved) "Saved to Artifacts" else "Couldn't save this artifact."
                        android.widget.Toast.makeText(context, notice, android.widget.Toast.LENGTH_LONG).show()
                    }
                },
                onSaveAction = { messageId, text ->
                    scope.launch {
                        val result = memoryEntries.addActionFromChat(text, title, messageId)
                        val notice = when (result) {
                            ai.orhan.data.memory.ActionSaveResult.Saved -> "Saved as an action. Nothing was run."
                            ai.orhan.data.memory.ActionSaveResult.Duplicate -> "That action is already saved."
                            ai.orhan.data.memory.ActionSaveResult.Dismissed -> "This action was deleted. It was not added again."
                            ai.orhan.data.memory.ActionSaveResult.Blank,
                            ai.orhan.data.memory.ActionSaveResult.WriteFailed,
                            -> "Couldn't save this action."
                        }
                        android.widget.Toast.makeText(context, notice, android.widget.Toast.LENGTH_LONG).show()
                    }
                },
                onTranslateMessage = { text -> chatViewModel.translateMessage(text, languageName(languagePrefs.code)) },
                onTranscribeAudio = chatViewModel::transcribeAudio,
                onSaveTranscript = chatViewModel::saveTranscript,
                onCreateMeetingMinutes = chatViewModel::createMeetingMinutes,
                onSaveMeetingMinutes = { artifactTitle, content, chatId, messageId, author ->
                    artifactStore.upsert(
                        ai.orhan.data.artifacts.Artifact(
                            id = java.util.UUID.randomUUID().toString(),
                            title = artifactTitle,
                            content = content,
                            createdAt = System.currentTimeMillis(),
                            kind = ai.orhan.data.artifacts.ArtifactKind.MEETING_MINUTES,
                            sourceChatId = chatId,
                            sourceMessageId = messageId,
                            author = author,
                        ),
                    )
                },
                onSaveMeetingActions = { items, chatId, messageId ->
                    val result = memoryEntries.addMeetingActionsFromChat(items, chatId, messageId)
                    when {
                        result.dismissed -> Result.failure(IllegalStateException("These source actions were previously dismissed and were not restored."))
                        result.saved == 0 && result.duplicates > 0 -> Result.success("All action items already exist.")
                        result.notAdded > 0 -> Result.success("Saved ${result.saved}; ${result.notAdded} could not be added because Memory is full.")
                        else -> Result.success("Saved ${result.saved} action items to Memory.")
                    }
                },
                initialComposerText = if (workflowRunId.isBlank()) entry.arguments?.getString("draft").orEmpty() else "",
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
