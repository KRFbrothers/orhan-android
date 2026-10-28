package ai.orhan.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ai.orhan.design.theme.OrhanTheme
import ai.orhan.design.theme.ThemeMode
import ai.orhan.design.theme.isDark

/* ============================================================================
 *  App root — the one place the persisted ThemeMode meets OrhanTheme.
 *
 *  Data flow:
 *
 *      DataStore ──Flow<ThemeMode>──▶ collectAsStateWithLifecycle
 *                                          │
 *                                          ▼
 *                                   ThemeMode.isDark()
 *                                          │
 *                                          ▼
 *                                   OrhanTheme(darkTheme = …)
 *
 *  System  -> isSystemInDarkTheme() (recomposes when the OS setting changes)
 *  Light   -> false
 *  Dark    -> true
 *
 *  Because the mode is read from DataStore, the very first frame already uses
 *  the saved theme — there is no light flash before the dark theme applies.
 * ==========================================================================*/

/**
 * @param themeMode the persisted mode, normally
 *   `context.themePreferences().themeMode.collectAsStateWithLifecycle(ThemeMode.System)`.
 *   Passed in rather than read here so this composable stays previewable.
 */
@Composable
fun OrhanApp(
    themeMode: ThemeMode,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    // isDark() is @Composable: in System mode it subscribes to the OS setting,
    // so a device-level dark-mode change recomposes the whole tree.
    val darkTheme = themeMode.isDark()

    OrhanTheme(darkTheme = darkTheme) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            content()
        }
    }
}

/**
 * Convenience overload for the common case: hand it the Flow and it does the
 * collection. Use this from `setContent { }`.
 *
 *     val preferences = remember { applicationContext.themePreferences() }
 *     OrhanApp(themeModeFlow = preferences.themeMode) {
 *         OrhanNavHost()
 *     }
 */
@Composable
fun OrhanApp(
    themeModeFlow: kotlinx.coroutines.flow.Flow<ThemeMode>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val mode by themeModeFlow.collectAsStateWithLifecycle(initialValue = ThemeMode.System)
    OrhanApp(themeMode = mode, modifier = modifier, content = content)
}
