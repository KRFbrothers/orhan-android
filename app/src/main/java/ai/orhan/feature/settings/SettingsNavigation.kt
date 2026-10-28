package ai.orhan.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ai.orhan.design.theme.themePreferences
import ai.orhan.feature.settings.appearance.AppearanceScreen
import ai.orhan.feature.settings.appearance.AppearanceViewModel

/* ============================================================================
 *  Settings → Appearance navigation hook
 *
 *  Two things are wired here:
 *    1. the route constant + a typed navigate() helper, so no screen has to
 *       hard-code the string;
 *    2. the ViewModel factory, built from the application Context's DataStore.
 *
 *  If the project already uses Hilt, delete [appearanceViewModel] and annotate
 *  AppearanceViewModel with @HiltViewModel + @Inject constructor — the screen
 *  signature does not change.
 * ==========================================================================*/

/** Route ids. Kept in one object so a typo is a compile error, not a crash. */
object SettingsRoutes {
    const val SETTINGS = "settings"
    const val APPEARANCE = "settings/appearance"
}

/** Navigate from the Settings list to Appearance. */
fun NavHostController.openAppearance() {
    navigate(SettingsRoutes.APPEARANCE) {
        // Do not stack a second Appearance screen if the user double-taps.
        launchSingleTop = true
    }
}

/**
 * Builds the Appearance ViewModel from the app's DataStore.
 *
 * `LocalContext.current.applicationContext` is used on purpose: the DataStore
 * must outlive any single Activity, and the delegate is keyed by file name, so
 * every caller gets the same instance.
 */
@Composable
fun appearanceViewModel(): AppearanceViewModel {
    val context = LocalContext.current.applicationContext
    val preferences = remember(context) { context.themePreferences() }
    return viewModel(factory = AppearanceViewModel.Factory(preferences))
}

/**
 * The Settings graph. Drop this into your existing NavHost, or copy the two
 * `composable { }` blocks into it.
 */
@Composable
fun SettingsNavHost(
    navController: NavHostController = rememberNavController(),
    onBackFromSettings: () -> Unit = {},
) {
    NavHost(
        navController = navController,
        startDestination = SettingsRoutes.SETTINGS,
    ) {
        composable(SettingsRoutes.SETTINGS) {
            SettingsScreen(
                onBack = onBackFromSettings,
                onOpenAppearance = { navController.openAppearance() },
            )
        }

        composable(SettingsRoutes.APPEARANCE) {
            AppearanceScreen(
                viewModel = appearanceViewModel(),
                onBack = { navController.popBackStack() },
            )
        }
    }
}
