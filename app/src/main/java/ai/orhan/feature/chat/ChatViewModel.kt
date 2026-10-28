package ai.orhan.feature.chat

import ai.orhan.BuildConfig
import ai.orhan.data.auth.AuthRepository
import ai.orhan.data.chat.ChatRepository
import android.app.Application
import ai.orhan.data.chat.ChatTurn
import ai.orhan.design.screens.ChatMessage
import ai.orhan.design.screens.ChatRole
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    /** True while a reply is in flight (typing indicator or streaming bubble). */
    val busy: Boolean = false,
)

class ChatViewModel(
    private val repository: ChatRepository = ChatRepository(),
) : ViewModel() {
    /** Uses the signed-in Supabase session; falls back to DUA_DEV_JWT (debug only) when signed out. */
    constructor(app: Application) : this(
        AuthRepository.get(app).let { auth ->
            ChatRepository(
                tokenProvider = { auth.accessToken() ?: BuildConfig.DUA_DEV_JWT.ifBlank { null } },
                onUnauthorized = { auth.clear() },
            )
        },
    )

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    fun send(raw: String) {
        val text = raw.trim()
        if (text.isEmpty() || _state.value.busy) return
        val user = ChatMessage(nextId(), ChatRole.User, text, "now")
        _state.update { it.copy(messages = it.messages + user, loading = true, busy = true, error = null) }
        val turns = _state.value.messages.map {
            ChatTurn(if (it.role == ChatRole.User) "user" else "model", it.text)
        }
        val replyId = nextId()
        viewModelScope.launch {
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
                // Fall back to non-streaming only if nothing was shown yet and the error isn't fatal (auth).
                if (streamed || e is ChatRepository.FatalChatException) throw e
                repository.send(turns).getOrThrow().also { reply ->
                    _state.update { it.copy(messages = it.messages + ChatMessage(replyId, ChatRole.Agent, reply, "now")) }
                }
            }
            result.onSuccess { _state.update { it.copy(loading = false, busy = false) } }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, busy = false, error = e.message ?: "Request failed") }
                }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }

    private fun nextId() = (_state.value.messages.lastOrNull()?.id ?: 0L) + 1L
}
