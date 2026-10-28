package ai.orhan.app

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ai.orhan.BuildConfig
import ai.orhan.data.auth.AuthRepository
import ai.orhan.design.screens.ChatScreen
import ai.orhan.feature.auth.SignInScreen
import ai.orhan.feature.auth.SignInViewModel
import ai.orhan.feature.chat.ChatViewModel
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ai.orhan.design.screens.HomeScreen
import ai.orhan.design.theme.ThemeMode
import ai.orhan.design.theme.themePreferences
import ai.orhan.feature.settings.SettingsRoutes
import ai.orhan.feature.settings.SettingsScreen
import ai.orhan.feature.settings.appearanceViewModel
import ai.orhan.feature.settings.appearance.AppearanceScreen
import ai.orhan.feature.settings.openAppearance
import ai.orhan.feature.settings.openSettingsPage
import ai.orhan.feature.settings.settingsDetailPages
import kotlinx.coroutines.launch

/** Top-level routes. Settings routes live in [SettingsRoutes]. */
object AppRoutes {
    const val HOME = "home"
    const val CHAT = "chat/{title}"
    fun chat(title: String) = "chat/${Uri.encode(title)}"
}

/**
 * App navigation: Home -> Chat, Home (overflow / Profile) -> Settings -> Appearance.
 * Theme toggle on Home writes the same DataStore that Appearance edits.
 */
@Composable
fun OrhanNavHost(isDark: Boolean) {
    val navController = rememberNavController()
    val context = LocalContext.current.applicationContext
    val preferences = remember(context) { context.themePreferences() }
    val scope = rememberCoroutineScope()

    NavHost(navController = navController, startDestination = AppRoutes.HOME) {
        composable(AppRoutes.HOME) {
            HomeScreen(
                isDark = isDark,
                onOpenChat = { navController.navigate(AppRoutes.chat(it.title)) },
                onOpenSettings = {
                    navController.navigate(SettingsRoutes.SETTINGS) { launchSingleTop = true }
                },
                onOpenAppearance = { navController.openAppearance() },
                onToggleTheme = {
                    scope.launch {
                        preferences.setThemeMode(if (isDark) ThemeMode.Light else ThemeMode.Dark)
                    }
                },
            )
        }
        composable(
            AppRoutes.CHAT,
            arguments = listOf(navArgument("title") { type = NavType.StringType }),
        ) { entry ->
            val auth = remember(context) { AuthRepository.get(context) }
            val session by auth.session.collectAsStateWithLifecycle(initialValue = null)
            val hasDevJwt = BuildConfig.DUA_DEV_JWT.isNotBlank()
            if (session == null && !hasDevJwt) {
                val signIn: SignInViewModel = viewModel()
                val signInState by signIn.state.collectAsStateWithLifecycle()
                SignInScreen(
                    loading = signInState.loading,
                    error = signInState.error,
                    configured = signIn.isConfigured,
                    onSignIn = signIn::signIn,
                    onBack = { navController.popBackStack() },
                )
                return@composable
            }
            val chatViewModel: ChatViewModel = viewModel { ChatViewModel(context as Application) }
            val chat by chatViewModel.state.collectAsStateWithLifecycle()
            ChatScreen(
                agentName = entry.arguments?.getString("title").orEmpty(),
                onBack = { navController.popBackStack() },
                messagesOverride = chat.messages,
                loading = chat.loading,
                error = chat.error,
                onSendMessage = chatViewModel::send,
            )
        }
        composable(SettingsRoutes.SETTINGS) {
            val auth = remember(context) { AuthRepository.get(context) }
            val session by auth.session.collectAsStateWithLifecycle(initialValue = null)
            SettingsScreen(
                onBack = { navController.popBackStack() },
onOpenAppearance = { navController.openAppearance() },
                onOpenNotifications = { navController.openSettingsPage(SettingsRoutes.NOTIFICATIONS) },
                onOpenPrivacy = { navController.openSettingsPage(SettingsRoutes.PRIVACY) },
                onOpenStorage = { navController.openSettingsPage(SettingsRoutes.STORAGE) },
                onOpenLanguage = { navController.openSettingsPage(SettingsRoutes.LANGUAGE) },
                onOpenAbout = { navController.openSettingsPage(SettingsRoutes.ABOUT) },
                signedInEmail = session?.email,
                // Only offered while a session exists; clears it via AuthRepository.signOut().
                onSignOut = if (session != null) {
                    {
                        scope.launch {
                            auth.signOut()
                            navController.navigate(AppRoutes.HOME) {
                                popUpTo(AppRoutes.HOME) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                } else null,
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
