package ai.orhan.feature.settings

import ai.orhan.data.history.chatThreads
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ai.orhan.data.history.pinnedChats
import ai.orhan.data.history.recentChats
import ai.orhan.design.theme.themePreferences
import ai.orhan.feature.settings.appearance.AppearanceScreen
import ai.orhan.feature.settings.appearance.AppearanceViewModel
import ai.orhan.feature.settings.detail.AboutScreen
import ai.orhan.feature.settings.detail.ChatPreferencesScreen
import ai.orhan.feature.settings.detail.LanguageScreen
import ai.orhan.feature.settings.detail.NotificationsScreen
import ai.orhan.feature.settings.detail.PrivacyScreen
import ai.orhan.feature.settings.detail.StorageScreen

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
    const val NOTIFICATIONS = "settings/notifications"
    const val PRIVACY = "settings/privacy"
    const val CHAT_PREFERENCES = "settings/chat-preferences"
    const val STORAGE = "settings/storage"
    const val LANGUAGE = "settings/language"
    const val ABOUT = "settings/about"
}

/** Navigate to any Settings sub-route without stacking duplicates. */
fun NavHostController.openSettingsPage(route: String) {
    navigate(route) { launchSingleTop = true }
}

/** Registers the five Settings detail pages (Notifications … About). */
fun NavGraphBuilder.settingsDetailPages(navController: NavHostController) {
    val back: () -> Unit = { navController.popBackStack() }
    composable(SettingsRoutes.NOTIFICATIONS) { NotificationsScreen(onBack = back) }
    composable(SettingsRoutes.PRIVACY) {
        val context = LocalContext.current.applicationContext
        val recentRepo = remember(context) { context.recentChats() }
        val pinRepo = remember(context) { context.pinnedChats() }
        val threadStore = remember(context) { context.chatThreads() }
        PrivacyScreen(
            onBack = back,
            onClearChatHistory = {
                recentRepo.clear()
                pinRepo.clear()
                threadStore.clear()
            },
        )
    }
    composable(SettingsRoutes.CHAT_PREFERENCES) { ChatPreferencesScreen(onBack = back) }
    composable(SettingsRoutes.STORAGE) { StorageScreen(onBack = back) }
    composable(SettingsRoutes.LANGUAGE) { LanguageScreen(onBack = back) }
    composable(SettingsRoutes.ABOUT) { AboutScreen(onBack = back) }
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
                onOpenNotifications = { navController.openSettingsPage(SettingsRoutes.NOTIFICATIONS) },
                onOpenPrivacy = { navController.openSettingsPage(SettingsRoutes.PRIVACY) },
                onOpenChatPreferences = { navController.openSettingsPage(SettingsRoutes.CHAT_PREFERENCES) },
                onOpenStorage = { navController.openSettingsPage(SettingsRoutes.STORAGE) },
                onOpenLanguage = { navController.openSettingsPage(SettingsRoutes.LANGUAGE) },
                onOpenAbout = { navController.openSettingsPage(SettingsRoutes.ABOUT) },
            )
        }

        composable(SettingsRoutes.APPEARANCE) {
            AppearanceScreen(
                viewModel = appearanceViewModel(),
                onBack = { navController.popBackStack() },
            )
        }
        settingsDetailPages(navController)
    }
}
