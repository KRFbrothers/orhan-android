package ai.orhan.feature.chat

import ai.orhan.data.chat.ChatRepository
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
)

class ChatViewModel(
    private val repository: ChatRepository = ChatRepository(),
) : ViewModel() {
    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    fun send(raw: String) {
        val text = raw.trim()
        if (text.isEmpty() || _state.value.loading) return
        val user = ChatMessage(nextId(), ChatRole.User, text, "now")
        _state.update { it.copy(messages = it.messages + user, loading = true, error = null) }
        val turns = _state.value.messages.map {
            ChatTurn(if (it.role == ChatRole.User) "user" else "model", it.text)
        }
        viewModelScope.launch {
            repository.send(turns)
                .onSuccess { reply ->
                    _state.update {
                        it.copy(messages = it.messages + ChatMessage(nextId(), ChatRole.Agent, reply, "now"), loading = false)
                    }
                }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.message ?: "Request failed") } }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }

    private fun nextId() = (_state.value.messages.lastOrNull()?.id ?: 0L) + 1L
}
