package ai.orhan

import ai.orhan.app.OrhanApp
import ai.orhan.app.OrhanNavHost
import ai.orhan.design.theme.ThemeMode
import ai.orhan.design.theme.isDark
import ai.orhan.design.theme.themePreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Entry point: persisted theme + app NavHost (Home, Chat, Settings, Appearance). */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val preferences = remember { applicationContext.themePreferences() }
            val mode by preferences.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            OrhanApp(themeMode = mode) {
                OrhanNavHost(isDark = mode.isDark())
            }
        }
    }
}
