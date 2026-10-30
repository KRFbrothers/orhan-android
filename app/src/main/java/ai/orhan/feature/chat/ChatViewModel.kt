package ai.orhan.feature.chat

import ai.orhan.BuildConfig
import ai.orhan.data.auth.AuthRepository
import ai.orhan.data.chat.ChatRepository
import android.app.Application
import ai.orhan.data.chat.ChatTurn
import ai.orhan.data.chat.ChatAttachment
import ai.orhan.design.screens.ChatMessage
import ai.orhan.design.screens.ChatRole
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.FlowPreview
import ai.orhan.data.history.ChatThreadStore
import ai.orhan.data.history.chatThreads
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** A specialist agent Orhan can delegate to. */
data class RosterAgent(val name: String, val blurb: String, val instructions: String, val tools: List<String> = emptyList())

/** Visible label prefix on a specialist reply inside the Orhan thread. */
const val SPECIALIST_PREFIX = "\uD83E\uDD1D "

/** Label line that opens a specialist bubble, e.g. "🤝 Writer\n\n". */
internal fun specialistHeader(agentName: String): String = "$SPECIALIST_PREFIX$agentName\n\n"

private val HANDOFF_RE = Regex("""^\s*\**HANDOFF\**\s*:\s*(.+?)\s*\|\s*(.+?)\s*$""", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))

/** Finds a `HANDOFF: Name | task` line naming a roster agent (case-insensitive). */
/** Only the last non-blank line may carry HANDOFF (a quoted mid-body line is ignored). */
private fun lastLine(reply: String): String = reply.trimEnd().lines().lastOrNull { it.isNotBlank() } ?: ""

internal fun parseHandoff(reply: String, roster: List<RosterAgent>): Pair<RosterAgent, String>? {
    val m = HANDOFF_RE.matchEntire(lastLine(reply)) ?: return null
    val name = m.groupValues[1].trim().trim('*', '"', '<', '>')
    val agent = roster.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: return null
    val task = m.groupValues[2].trim()
    return if (task.isEmpty()) null else agent to task
}

internal fun stripHandoff(reply: String): String {
    val trimmed = reply.trimEnd()
    val last = lastLine(trimmed)
    if (!HANDOFF_RE.matches(last)) return reply.trim()
    return trimmed.substring(0, trimmed.lastIndexOf(last)).trim()
}

/** Folds preface blocks into one hidden user turn + one model ack (2 turns total). */
/** Builds the hidden "who the user is" block from profile name/bio/role/timezone; null when all blank. */
internal fun userProfileSummary(
    name: String?,
    bio: String?,
    role: String? = null,
    timezone: String? = null,
): String? {
    val lines = buildList {
        name?.trim()?.takeIf { it.isNotEmpty() }?.let { add("User's name: $it") }
        role?.trim()?.takeIf { it.isNotEmpty() }?.let { add("User's role: $it") }
        timezone?.trim()?.takeIf { it.isNotEmpty() }?.let { add("User's timezone: $it") }
        bio?.trim()?.takeIf { it.isNotEmpty() }?.let { add("About user: $it") }
    }
    return if (lines.isEmpty()) null else "User profile (use when relevant):\n" + lines.joinToString("\n")
}

/**
 * Send hidden context as top-level `systemInstruction` (Dua-ai #494). Flip to false only for an
 * engine older than #494; the app then falls back to hidden user/model preface turns.
 */
internal const val USE_SYSTEM_INSTRUCTION = true

/** Joins non-blank context blocks into one systemInstruction string. */
internal fun systemInstructionOf(blocks: List<String>): String =
    blocks.map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n\n")

internal fun prefacePair(blocks: List<String>, ack: String): List<ChatTurn> =
    listOf(ChatTurn("user", blocks.filter { it.isNotBlank() }.joinToString("\n\n---\n\n")), ChatTurn("model", ack))

internal fun currentTimeString(): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    /** True while a reply is in flight (typing indicator or streaming bubble). */
    val busy: Boolean = false,
    /** Transient status from SSE `thinking`/`progress` events; shown with the typing indicator. */
    val activityLabel: String? = null,
)

class ChatViewModel(
    private val repository: ChatRepository = ChatRepository(),
    private val threadStore: ChatThreadStore? = null,
) : ViewModel() {
    /** Uses the signed-in Supabase session; falls back to DUA_DEV_JWT (debug only) when signed out. */
    constructor(app: Application) : this(
        AuthRepository.get(app).let { auth ->
            ChatRepository(
                tokenProvider = { auth.accessToken() ?: BuildConfig.DUA_DEV_JWT.ifBlank { null } },
                onUnauthorized = { auth.clear() },
            )
        },
        app.chatThreads(),
    )

    private var threadTitle: String? = null

    /**
     * Restores the saved thread for [title] once, then persists message changes (debounced).
     * Only real turns are saved; the design-kit demo thread never flows through this VM.
     *
     * Disk writes are gated on `!busy` so partial SSE chunks never trigger mid-stream
     * serialization. Writes happen only after streaming completes, stops, or errors.
     */
    @OptIn(FlowPreview::class)
    fun loadThread(title: String) {
        val store = threadStore ?: return
        if (threadTitle != null || title.isBlank()) return
        threadTitle = title
        viewModelScope.launch {
            val saved = store.loadThread(title)
            if (saved.messages.isNotEmpty() && _state.value.messages.isEmpty()) {
                saved.attachments.forEach { (id, a) -> attachmentsById.putIfAbsent(id, a) }
                _state.update { it.copy(messages = saved.messages) }
            }
            _state.filter { !it.busy }
                .map { it.messages }
                .distinctUntilChanged()
                .drop(1)
                .debounce(300)
                .collect { msgs -> store.save(title, msgs, attachmentsById.toMap()) }
        }
    }

    override fun onCleared() {
        // Final flush: save immediately if the debounce window hadn't elapsed.
        // Uses viewModelScope (already cancelling but not yet cancelled) rather than
        // GlobalScope, so clearing history while this VM is on the back stack cannot
        // resurrect a deleted thread.
        val title = threadTitle ?: return
        val store = threadStore ?: return
        val msgs = _state.value.messages
        val atts = attachmentsById.toMap()
        if (msgs.isNotEmpty()) viewModelScope.launch { store.save(title, msgs, atts) }
    }

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var agentName: String = ""
    private var instructions: String = ""
    private var tools: List<String> = emptyList()

    /** Sets the agent personality used for API turns. Idempotent; safe to call on every recomposition. */
    fun setAgent(name: String, instructions: String, tools: List<String> = emptyList()) {
        agentName = name; this.instructions = instructions.trim(); this.tools = tools
    }

    /** Other agents Orhan may delegate to (only set when chatting as Orhan, not a specialist). */
    private var roster: List<RosterAgent> = emptyList()

    /** Sets the delegation roster. Pass empty when this chat is itself a specialist agent. Idempotent. */
    fun setRoster(agents: List<RosterAgent>) {
        roster = agents.filter { it.name.isNotBlank() && !it.name.equals("Orhan", ignoreCase = true) }
    }

    /** Hidden preface turn telling Orhan who it controls and how to hand off. */
    internal fun rosterPreface(): String? {
        if (roster.isEmpty()) return null
        val lines = roster.joinToString("\n") { a ->
            val about = (a.blurb.ifBlank { a.instructions }).replace('\n', ' ').take(160)
            "- ${a.name}: $about"
        }
        return "You are Orhan, the main controller agent. The user has these specialist agents you can assign work to:\n$lines\n" +
            "Rules: answer simple things yourself. When a task clearly fits one specialist, write one short sentence saying you're handing it off, " +
            "then on its own last line write exactly: HANDOFF: <AgentName> | <complete task for that agent, including details the user gave>. " +
            "Use only names from the list. At most one HANDOFF per reply. Never invent results for the specialist; the app runs that agent and shows its reply."
    }

    private var personalMemory: String = ""

    /** Hidden profile summary (name/bio/role/timezone), or null when unset/blank. */
    private var userProfile: String? = null

    /** Sets the user's profile summary sent as hidden context. Idempotent; safe on every recomposition. */
    fun setUserProfile(name: String?, bio: String?, role: String? = null, timezone: String? = null) {
        userProfile = userProfileSummary(name, bio, role, timezone)
    }

    /** Display name of the user's chosen reply language, or null to mirror the user's language. */
    private var replyLanguage: String? = null

    /** Sets the saved app language agents should reply in (null = follow the user). Idempotent. */
    fun setReplyLanguage(name: String?) {
        replyLanguage = name?.trim()?.takeIf { it.isNotEmpty() }
    }

    /** Sets the user's personal memory sent as hidden context. Idempotent; safe on every recomposition. */
    fun setPersonalMemory(text: String, paused: Boolean = false) {
        personalMemory = text.trim()
        memoryPaused = paused
    }

    private var memoryPaused: Boolean = false

    /** Identity layer sent on every request so the shared engine never answers as Dua AI or names its model vendor. */
    internal fun orhanIdentity(agent: String): String {
        val custom = agent.isNotBlank() && !agent.equals("Orhan", ignoreCase = true)
        val who = if (custom) "the agent \"$agent\" inside the Orhan app (a personal AI assistant)" else "Orhan, a personal AI assistant"
        return "Identity (always follow): You are $who. Always speak as ${if (custom) agent else "Orhan"}. " +
            "Never say you are Dua AI or Dua. Do not volunteer vendor names like Gemini, Google or OpenAI, and never invent model names. " +
            "If asked who you are, say you are ${if (custom) "$agent, an agent in Orhan" else "Orhan, your personal AI assistant"}. " +
            "If asked which model you use (e.g. 'kya model hai?'), answer honestly: 'Main Argon 4 model par chalta hoon.' / 'I run on the Argon 4 model.' Always say just 'Argon 4'. " +
            "Reply in the user's language (Hinglish if they use Hinglish)." +
            (replyLanguage?.let { " The user's chosen app language is $it: reply in $it unless they explicitly ask for another language." } ?: "")
    }

    /** Hidden context blocks (identity, profile, memory, roster, agent instructions + tools) for the main chat. */
    internal fun contextBlocks(): List<String> = buildList {
        add(orhanIdentity(agentName))
        userProfile?.let { add(it) }
        personalMemoryBlock(personalMemory, memoryPaused)?.let { add(it) }
        rosterPreface()?.let { add(it) }
        if (instructions.isNotBlank()) {
            add("You are the agent named \"$agentName\". Follow these system instructions for the whole conversation:\n$instructions\n\n" + AgentToolGuidance.text(tools))
        } else if (tools.isNotEmpty()) {
            add(AgentToolGuidance.text(tools))
        }
    }

    /** Top-level `systemInstruction` for /api/chat (Dua-ai #494): all hidden context in one string. */
    internal fun systemInstruction(): String = systemInstructionOf(contextBlocks())

    /**
     * Turns sent as `messages`. With [USE_SYSTEM_INSTRUCTION] the full 20-turn window is real chat;
     * otherwise (legacy engines) hidden user/model preface turns are prepended, never shown in UI.
     */
    internal fun withPreface(uiTurns: List<ChatTurn>): List<ChatTurn> {
        if (USE_SYSTEM_INSTRUCTION) return uiTurns.takeLast(20)
        val preface = prefacePair(contextBlocks(), "Understood. I'm Orhan. I'll follow all of that.")
        // 2 preface turns + 18 UI turns = 20, matching ChatRepository's takeLast(20) so the preface is never dropped.
        return preface + uiTurns.takeLast(20 - preface.size)
    }

    /** systemInstruction to send alongside [withPreface] turns, or null in legacy preface mode. */
    private fun systemInstructionOrNull(): String? = if (USE_SYSTEM_INSTRUCTION) systemInstruction() else null

    /**
     * File parts by user message id. Persisted to disk via [ChatThreadStore] and restored on load,
     * so turns within the last-20 window keep re-sending their files after an app restart.
     */
    private val attachmentsById = java.util.concurrent.ConcurrentHashMap<Long, ChatAttachment>()

    fun send(raw: String) = send(raw, null)

    /** Sends [raw] with an optional inline file. The bubble shows a "📎 name" line for the file. */
    fun send(raw: String, attachment: ChatAttachment?) {
        val text = raw.trim()
        if ((text.isEmpty() && attachment == null) || _state.value.busy) return
        val shown = if (attachment == null) text else ("\uD83D\uDCCE ${attachment.name}" + if (text.isEmpty()) "" else "\n$text")
        val user = ChatMessage(nextId(), ChatRole.User, shown, currentTimeString())
        if (attachment != null) attachmentsById[user.id] = attachment
        _state.update { it.copy(messages = it.messages + user, loading = true, activityLabel = null, busy = true, error = null) }
        streamReply()
    }

    /**
     * Re-sends the last user turn after a failure without adding another user bubble.
     * A trailing partial agent reply (from an interrupted stream) is dropped first.
     */
    fun retry() {
        val s = _state.value
        if (s.busy || s.error == null) return
        val msgs = s.messages.toMutableList()
        while (msgs.isNotEmpty() && msgs.last().role == ChatRole.Agent) msgs.removeAt(msgs.lastIndex)
        if (msgs.isEmpty()) { clearError(); return }
        _state.update { it.copy(messages = msgs, loading = true, activityLabel = null, busy = true, error = null) }
        streamReply()
    }

    private var streamJob: Job? = null

    /** Cancels the in-flight reply. Any partial agent bubble stays; a user cancel is not an error. */
    fun stop() {
        streamJob?.cancel()
        streamJob = null
        _state.update { it.copy(loading = false, activityLabel = null, busy = false) }
    }

    private fun streamReply() {
        streamJob?.cancel()
        val turns = withPreface(turnsForApi(_state.value.messages, attachmentsById))
        val system = systemInstructionOrNull()
        val replyId = nextId()
        streamJob = viewModelScope.launch {
            var streamed = false
            val result = repository.stream(turns, system, onActivity = { label -> _state.update { if (it.busy) it.copy(activityLabel = label) else it } }) { delta ->
                streamed = true
                _state.update { s ->
                    val existing = s.messages.lastOrNull()?.takeIf { it.id == replyId }
                    val msgs = if (existing == null) {
                        s.messages + ChatMessage(replyId, ChatRole.Agent, delta, currentTimeString())
                    } else {
                        s.messages.dropLast(1) + existing.copy(text = existing.text + delta)
                    }
                    // First delta replaces the typing indicator with the live bubble.
                    s.copy(messages = msgs, loading = false, activityLabel = null)
                }
            }.recoverCatching { e ->
                if (e is CancellationException) throw e
                // Fall back to non-streaming only if nothing was shown yet and the error isn't fatal (auth).
                if (streamed || e !is ChatRepository.FallbackEligibleStreamException) throw e
                repository.send(turns, system).getOrThrow().also { reply ->
                    _state.update { it.copy(messages = it.messages + ChatMessage(replyId, ChatRole.Agent, reply, currentTimeString())) }
                }
            }
            result.onSuccess { full ->
                val handoff = parseHandoff(full, roster)
                if (handoff == null) { _state.update { it.copy(loading = false, activityLabel = null, busy = false) }; return@onSuccess }
                runHandoff(replyId, handoff.first, handoff.second)
            }
                .onFailure { e ->
                    // User cancel (stop()) or scope teardown: state already reset; never surface as error.
                    if (e is CancellationException) return@launch
                    _state.update { it.copy(loading = false, activityLabel = null, busy = false, error = e.message ?: "Request failed") }
                }
        }
    }

    /**
     * Rewrites Orhan's HANDOFF line into a visible note, then runs a second /api/chat call with the
     * specialist's own identity + instructions and streams its reply into a bubble labeled with the agent name.
     */
    private suspend fun runHandoff(orhanReplyId: Long, agent: RosterAgent, task: String) {
        _state.update { s ->
            s.copy(messages = s.messages.map { m ->
                if (m.id == orhanReplyId) m.copy(text = stripHandoff(m.text) + "\n\n\u21AA Assigned to ${agent.name}: $task") else m
            }, loading = true, activityLabel = null)
        }
        val lastUser = turnsForApi(_state.value.messages, attachmentsById).lastOrNull { it.role == "user" }
        val blocks = buildList {
            add(orhanIdentity(agent.name))
            userProfile?.let { add(it) }
            personalMemoryBlock(personalMemory, memoryPaused)?.let { add(it) }
            if (agent.instructions.isNotBlank()) add("You are the agent named \"${agent.name}\". Follow these system instructions:\n${agent.instructions}\n\n" + AgentToolGuidance.text(agent.tools))
        }
        val system = if (USE_SYSTEM_INSTRUCTION) systemInstructionOf(blocks) else null
        val turns = buildList {
            if (!USE_SYSTEM_INSTRUCTION) addAll(prefacePair(blocks, "Understood. I'm ${agent.name}."))
            if (lastUser != null) {
                add(lastUser) // user's latest message, with its attachment bytes
                add(ChatTurn("model", "Noted."))
            }
            add(ChatTurn("user", "Orhan (the main agent) assigned you this task from the user. Do it fully and reply directly to the user:\n$task"))
        }
        // Stream into a labeled bubble (same SSE path as Orhan). Runs inside streamJob, so stop() cancels it.
        val bubbleId = nextId()
        val header = specialistHeader(agent.name)
        var streamed = false
        repository.stream(turns, system, onActivity = { label -> _state.update { if (it.busy) it.copy(activityLabel = label) else it } }) { delta ->
            streamed = true
            _state.update { s ->
                val existing = s.messages.lastOrNull()?.takeIf { it.id == bubbleId }
                val msgs = if (existing == null) s.messages + ChatMessage(bubbleId, ChatRole.Agent, header + delta, currentTimeString())
                else s.messages.dropLast(1) + existing.copy(text = existing.text + delta)
                s.copy(messages = msgs, loading = false, activityLabel = null)
            }
        }.recoverCatching { e ->
            if (e is CancellationException) throw e
            if (streamed || e !is ChatRepository.FallbackEligibleStreamException) throw e
            repository.send(turns, system).getOrThrow().also { reply ->
                _state.update { it.copy(messages = it.messages + ChatMessage(bubbleId, ChatRole.Agent, header + reply, currentTimeString())) }
            }
        }
            .onSuccess { _state.update { it.copy(loading = false, activityLabel = null, busy = false) } }
            .onFailure { e ->
                if (e is CancellationException) throw e
                _state.update { it.copy(loading = false, activityLabel = null, busy = false, error = "${agent.name}: ${e.message ?: "Request failed"}") }
            }
    }

    fun clearError() = _state.update { it.copy(error = null) }

    private fun nextId() = (_state.value.messages.lastOrNull()?.id ?: 0L) + 1L
}

/**
 * Maps UI messages to API turns. Only the latest user turn carries attachment bytes;
 * older turns keep their `📎 name` text but send no attachments (avoids re-uploading files every message).
 */
internal fun turnsForApi(messages: List<ChatMessage>, attachmentsById: Map<Long, ChatAttachment>): List<ChatTurn> {
    val lastUserId = messages.lastOrNull { it.role == ChatRole.User }?.id
    return messages.map {
        ChatTurn(
            if (it.role == ChatRole.User) "user" else "model",
            it.text,
            if (it.id == lastUserId) listOfNotNull(attachmentsById[it.id]) else emptyList(),
        )
    }
}

/** Hidden personal-memory block for the preface, or null when blank or paused by the user. */
internal fun personalMemoryBlock(text: String, paused: Boolean): String? {
    val t = text.trim()
    if (paused || t.isBlank()) return null
    return "Personal context about the user (use when relevant):\n$t"
}
