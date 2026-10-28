package ai.orhan.feature.settings.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ai.orhan.design.theme.ThemeMode
import ai.orhan.design.theme.ThemePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/* ============================================================================
 *  Appearance — UI state + ViewModel
 *
 *  The screen never touches DataStore directly. It renders [AppearanceUiState]
 *  and calls [AppearanceViewModel.setThemeMode]; the ViewModel owns the write
 *  and the resulting state.
 * ==========================================================================*/

/**
 * Everything the Appearance screen needs to draw itself.
 *
 * [selected] is the persisted value. [pending] is non-null only while a write is
 * in flight, so the radio can show the new choice immediately (optimistic) while
 * still being able to roll back if the write fails.
 */
data class AppearanceUiState(
    val selected: ThemeMode = ThemeMode.System,
    val pending: ThemeMode? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
) {
    /** What the radio group should show as checked right now. */
    val displayed: ThemeMode get() = pending ?: selected
}

class AppearanceViewModel(
    private val preferences: ThemePreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppearanceUiState())
    val uiState: StateFlow<AppearanceUiState> = _uiState.asStateFlow()

    init {
        // Mirror the persisted value into the UI. collect is scoped to
        // viewModelScope, so it stops when the ViewModel is cleared.
        viewModelScope.launch {
            preferences.themeMode.collect { mode ->
                _uiState.update { state ->
                    // A write in flight wins until it lands, otherwise the
                    // radio would flick back to the old value mid-save.
                    if (state.isSaving) state.copy(selected = mode) else state.copy(selected = mode, pending = null)
                }
            }
        }
    }

    /**
     * Persist [mode]. Tapping the already-selected option is a no-op, so the
     * screen does not write to disk on every tap of the same row.
     */
    fun setThemeMode(mode: ThemeMode) {
        val current = _uiState.value
        if (mode == current.selected && !current.isSaving) return

        _uiState.update {
            it.copy(pending = mode, isSaving = true, errorMessage = null)
        }

        viewModelScope.launch {
            runCatching { preferences.setThemeMode(mode) }
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false, pending = null) }
                }
                .onFailure { error ->
                    // Roll the radio back to the value that is actually on disk.
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            pending = null,
                            errorMessage = error.message ?: "Could not save your theme choice.",
                        )
                    }
                }
        }
    }

    /** Dismiss the inline error after it has been shown. */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Manual factory — no Hilt needed. Swap for @HiltViewModel + @Inject when
     * the project already uses Hilt; the constructor signature stays the same.
     */
    class Factory(
        private val preferences: ThemePreferences,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(AppearanceViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return AppearanceViewModel(preferences) as T
        }
    }
}
