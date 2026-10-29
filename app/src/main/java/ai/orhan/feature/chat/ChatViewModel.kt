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
import kotlinx.coroutines.FlowPreview
import ai.orhan.data.history.ChatThreadStore
import ai.orhan.data.history.chatThreads
import kotlinx.coroutines.launch

/** A specialist agent Orhan can delegate to. */
data class RosterAgent(val name: String, val blurb: String, val instructions: String, val tools: List<String> = emptyList())

/** Visible label prefix on a specialist reply inside the Orhan thread. */
const val SPECIALIST_PREFIX = "\uD83E\uDD1D "

/** Label line that opens a specialist bubble, e.g. "🤝 Writer\n\n". */
internal fun specialistHeader(agentName: String): String = "$SPECIALIST_PREFIX$agentName\n\n"

private val HANDOFF_RE = Regex("""^\s*\**HANDOFF\**\s*:\s*(.+?)\s*\|\s*(.+?)\s*$""", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))

/** Finds a `HANDOFF: Name | task` line naming a roster agent (case-insensitive). */
internal fun parseHandoff(reply: String, roster: List<RosterAgent>): Pair<RosterAgent, String>? {
    val m = HANDOFF_RE.findAll(reply).lastOrNull() ?: return null
    val name = m.groupValues[1].trim().trim('*', '"', '<', '>')
    val agent = roster.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: return null
    val task = m.groupValues[2].trim()
    return if (task.isEmpty()) null else agent to task
}

internal fun stripHandoff(reply: String): String = reply.replace(HANDOFF_RE, "").trim()

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    /** True while a reply is in flight (typing indicator or streaming bubble). */
    val busy: Boolean = false,
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
            _state.map { it.messages }
                .distinctUntilChanged()
                .drop(1)
                .debounce(300)
                .collect { msgs -> store.save(title, msgs, attachmentsById.toMap()) }
        }
    }

    @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
    override fun onCleared() {
        // Flush the latest messages in case the debounce window hadn't elapsed.
        val title = threadTitle ?: return
        val store = threadStore ?: return
        val msgs = _state.value.messages
        val atts = attachmentsById.toMap()
        if (msgs.isNotEmpty()) kotlinx.coroutines.GlobalScope.launch { store.save(title, msgs, atts) }
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

    /** Display name of the user's chosen reply language, or null to mirror the user's language. */
    private var replyLanguage: String? = null

    /** Sets the saved app language agents should reply in (null = follow the user). Idempotent. */
    fun setReplyLanguage(name: String?) {
        replyLanguage = name?.trim()?.takeIf { it.isNotEmpty() }
    }

    /** Sets the user's personal memory sent as hidden context. Idempotent; safe on every recomposition. */
    fun setPersonalMemory(text: String) {
        personalMemory = text.trim()
    }

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

    /** Prepends hidden user/model preface turns (engine only supports those roles); never shown in UI. */
    internal fun withPreface(uiTurns: List<ChatTurn>): List<ChatTurn> {
        val preface = buildList {
            add(ChatTurn("user", orhanIdentity(agentName)))
            add(ChatTurn("model", "Understood. I'm Orhan."))
            if (personalMemory.isNotBlank()) {
                add(ChatTurn("user", "Personal context about the user (use when relevant):\n$personalMemory"))
                add(ChatTurn("model", "Understood. I'll use that context when helpful."))
            }
            rosterPreface()?.let {
                add(ChatTurn("user", it))
                add(ChatTurn("model", "Understood. I'll delegate with a HANDOFF line when a specialist fits."))
            }
            if (instructions.isNotBlank()) {
                val toolsLine = "\n\n" + AgentToolGuidance.text(tools)
                add(ChatTurn("user", "You are the agent named \"$agentName\". Follow these system instructions for the whole conversation:\n$instructions$toolsLine"))
                add(ChatTurn("model", "Understood. I'll follow those instructions."))
            } else if (tools.isNotEmpty()) {
                add(ChatTurn("user", AgentToolGuidance.text(tools)))
                add(ChatTurn("model", "Understood. I'll use those tools when helpful."))
            }
        }
        // preface + UI turns = 20, matching ChatRepository's takeLast(20) so the preface is never dropped.
        return preface + uiTurns.takeLast(20 - preface.size)
    }

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
        val user = ChatMessage(nextId(), ChatRole.User, shown, "now")
        if (attachment != null) attachmentsById[user.id] = attachment
        _state.update { it.copy(messages = it.messages + user, loading = true, busy = true, error = null) }
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
        _state.update { it.copy(messages = msgs, loading = true, busy = true, error = null) }
        streamReply()
    }

    private var streamJob: Job? = null

    /** Cancels the in-flight reply. Any partial agent bubble stays; a user cancel is not an error. */
    fun stop() {
        streamJob?.cancel()
        streamJob = null
        _state.update { it.copy(loading = false, busy = false) }
    }

    private fun streamReply() {
        streamJob?.cancel()
        val turns = withPreface(turnsForApi(_state.value.messages, attachmentsById))
        val replyId = nextId()
        streamJob = viewModelScope.launch {
            var streamed = false
            val result = repository.stream(turns) { delta ->
                streamed = true
                _state.update { s ->
                    val existing = s.messages.lastOrNull()?.takeIf { it.id == replyId }
                    val msgs = if (existing == null) {
                        s.messages + ChatMessage(replyId, ChatRole.Agent, delta, "now")
                    } else {
                        s.messages.dropLast(1) + existing.copy(text = existing.text + delta)
                    }
                    // First delta replaces the typing indicator with the live bubble.
                    s.copy(messages = msgs, loading = false)
                }
            }.recoverCatching { e ->
                if (e is CancellationException) throw e
                // Fall back to non-streaming only if nothing was shown yet and the error isn't fatal (auth).
                if (streamed || e is ChatRepository.FatalChatException) throw e
                repository.send(turns).getOrThrow().also { reply ->
                    _state.update { it.copy(messages = it.messages + ChatMessage(replyId, ChatRole.Agent, reply, "now")) }
                }
            }
            result.onSuccess { full ->
                val handoff = parseHandoff(full, roster)
                if (handoff == null) { _state.update { it.copy(loading = false, busy = false) }; return@onSuccess }
                runHandoff(replyId, handoff.first, handoff.second)
            }
                .onFailure { e ->
                    // User cancel (stop()) or scope teardown: state already reset; never surface as error.
                    if (e is CancellationException) return@launch
                    _state.update { it.copy(loading = false, busy = false, error = e.message ?: "Request failed") }
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
            }, loading = true)
        }
        val turns = buildList {
            add(ChatTurn("user", orhanIdentity(agent.name)))
            add(ChatTurn("model", "Understood. I'm ${agent.name}."))
            if (personalMemory.isNotBlank()) {
                add(ChatTurn("user", "Personal context about the user (use when relevant):\n$personalMemory"))
                add(ChatTurn("model", "Understood."))
            }
            if (agent.instructions.isNotBlank()) {
                val toolsLine = "\n\n" + AgentToolGuidance.text(agent.tools)
                add(ChatTurn("user", "You are the agent named \"${agent.name}\". Follow these system instructions:\n${agent.instructions}$toolsLine"))
                add(ChatTurn("model", "Understood. I'll follow those instructions."))
            }
            add(ChatTurn("user", "Orhan (the main agent) assigned you this task from the user. Do it fully and reply directly to the user:\n$task"))
        }
        // Stream into a labeled bubble (same SSE path as Orhan). Runs inside streamJob, so stop() cancels it.
        val bubbleId = nextId()
        val header = specialistHeader(agent.name)
        var streamed = false
        repository.stream(turns) { delta ->
            streamed = true
            _state.update { s ->
                val existing = s.messages.lastOrNull()?.takeIf { it.id == bubbleId }
                val msgs = if (existing == null) s.messages + ChatMessage(bubbleId, ChatRole.Agent, header + delta, "now")
                else s.messages.dropLast(1) + existing.copy(text = existing.text + delta)
                s.copy(messages = msgs, loading = false)
            }
        }.recoverCatching { e ->
            if (e is CancellationException) throw e
            if (streamed || e is ChatRepository.FatalChatException) throw e
            repository.send(turns).getOrThrow().also { reply ->
                _state.update { it.copy(messages = it.messages + ChatMessage(bubbleId, ChatRole.Agent, header + reply, "now")) }
            }
        }
            .onSuccess { _state.update { it.copy(loading = false, busy = false) } }
            .onFailure { e ->
                if (e is CancellationException) throw e
                _state.update { it.copy(loading = false, busy = false, error = "${agent.name}: ${e.message ?: "Request failed"}") }
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
