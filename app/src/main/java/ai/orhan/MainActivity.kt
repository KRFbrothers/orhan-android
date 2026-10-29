package ai.orhan

import ai.orhan.app.OrhanApp
import ai.orhan.app.OrhanNavHost
import ai.orhan.design.theme.ThemeMode
import ai.orhan.design.theme.isDark
import ai.orhan.design.theme.themePreferences
import ai.orhan.app.AppLockScreen
import ai.orhan.data.applock.appLockStore
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Entry point: persisted theme + app NavHost (Home, Chat, Settings, Appearance). */
class MainActivity : ComponentActivity() {
    /**
     * App lock: true on cold start and after every onStop (app backgrounded).
     * Only shown when the user turned App lock on. Survives rotation only.
     */
    private var locked by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState?.getBoolean(KEY_UNLOCKED_ACROSS_CONFIG) == true) locked = false
        val lockStore = applicationContext.appLockStore()
        enableEdgeToEdge()
        setContent {
            val preferences = remember { applicationContext.themePreferences() }
            val mode by preferences.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            // null = not loaded yet: draw nothing so app content never flashes before the lock.
            val lockEnabled by lockStore.isEnabled.collectAsStateWithLifecycle(initialValue = null)
            OrhanApp(themeMode = mode) {
                val showLock = lockEnabled == true && locked
                if (lockEnabled != null) {
                    // NavHost stays composed under the lock so the user returns to the same screen,
                    // but it is hidden from accessibility while locked.
                    Box(Modifier.fillMaxSize().then(if (showLock) Modifier.clearAndSetSemantics { } else Modifier)) {
                        OrhanNavHost(isDark = mode.isDark())
                    }
                }
                if (lockEnabled == null || showLock) {
                    // Opaque layer that swallows all touches.
                    Box(
                        Modifier.fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } } },
                    ) {
                        if (showLock) AppLockScreen(
                            store = lockStore,
                            onUnlocked = { locked = false },
                            onBackExit = { finish() },
                        )
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) locked = true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Keep unlocked only across rotation/config change, never across process death.
        outState.putBoolean(KEY_UNLOCKED_ACROSS_CONFIG, isChangingConfigurations && !locked)
    }

    private companion object {
        const val KEY_UNLOCKED_ACROSS_CONFIG = "orhan.unlocked_config"
    }
}
