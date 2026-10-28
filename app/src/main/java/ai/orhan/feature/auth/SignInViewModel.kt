package ai.orhan.feature.auth

import ai.orhan.data.auth.AuthRepository
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SignInUiState(
    val loading: Boolean = false,
    val error: String? = null,
)

class SignInViewModel(app: Application) : AndroidViewModel(app) {
    private val auth = AuthRepository.get(app)
    private val _state = MutableStateFlow(SignInUiState())
    val state: StateFlow<SignInUiState> = _state.asStateFlow()
    val isConfigured: Boolean get() = auth.isConfigured

    fun signIn(email: String, password: String) {
        if (_state.value.loading) return
        if (email.isBlank() || password.isEmpty()) {
            _state.update { it.copy(error = "Enter email and password") }
            return
        }
        _state.update { SignInUiState(loading = true) }
        viewModelScope.launch {
            auth.signIn(email, password)
                .onSuccess { _state.update { SignInUiState() } }
                .onFailure { e -> _state.update { SignInUiState(error = e.message ?: "Sign-in failed") } }
        }
    }
}
