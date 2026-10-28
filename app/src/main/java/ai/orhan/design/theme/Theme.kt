package ai.orhan.design.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/* ============================================================================
 *  Orhan AI — Theme
 *
 *  OrhanTheme(darkTheme = ...) wraps MaterialTheme with the brand's
 *  ColorScheme, Typography and Shapes, and additionally provides the tokens
 *  Material 3 has no slot for:
 *
 *      LocalOrhanColors.current   -> OrhanColors   (bubbles, gradient, nav…)
 *      LocalOrhanShapes.current   -> OrhanShapeTokens (card 20, bubble 20/6…)
 *
 *  Dynamic colour is OFF by default. The brand green is the product's identity;
 *  letting the wallpaper repaint it would break the design. Pass
 *  dynamicColor = true only for a user-facing "use wallpaper colours" opt-in.
 * ==========================================================================*/

val LocalOrhanColors = staticCompositionLocalOf { OrhanColors.Light }
val LocalOrhanShapes = staticCompositionLocalOf { OrhanShapeTokens.Default }

/**
 * The three states the Settings → Appearance screen offers.
 * Persist [ThemeMode.name] (or its ordinal) in DataStore and feed it back in.
 */
enum class ThemeMode { System, Light, Dark }

/** Resolve a [ThemeMode] against the OS setting. */
@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}

@Composable
fun OrhanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    /** Opt-in only — see the note above. */
    dynamicColor: Boolean = false,
    /**
     * Defaults to [OrhanTypography] (Lato), so every existing call site keeps
     * the brand typeface. Pass [OrhanTypographyInter] to opt into the Inter
     * fallback — see FallbackFonts.kt.
     */
    typography: Typography = OrhanTypography,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> OrhanDarkColorScheme
        else -> OrhanLightColorScheme
    }

    val orhanColors = if (darkTheme) OrhanColors.Dark else OrhanColors.Light

    // Keep the system bars in step with the app surface, and make the icons
    // legible against it. Without this the status bar keeps the previous
    // theme's icon colour after a toggle.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            // The view's context is not always the Activity itself (it can be a
            // ContextThemeWrapper), so walk up instead of casting blindly.
            val activity = view.context.findActivity() ?: return@SideEffect
            WindowCompat.getInsetsController(activity.window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalOrhanColors provides orhanColors,
        LocalOrhanShapes provides OrhanShapeTokens.Default,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = OrhanShapes,
            content = content,
        )
    }
}

/* ---------- Convenience accessors ---------- */

/** `OrhanTheme.colors.primaryGradient` reads better than the long local name. */
object OrhanTheme {
    val colors: OrhanColors
        @Composable get() = LocalOrhanColors.current

    val shapes: OrhanShapeTokens
        @Composable get() = LocalOrhanShapes.current
}

/* ---------- Persisted theme state ---------- */

/**
 * Small immutable holder so the theme choice survives configuration changes
 * without a ViewModel. Pair it with a DataStore write in the setter.
 *
 *     var mode by rememberSaveable(stateSaver = ThemeModeSaver) {
 *         mutableStateOf(ThemeMode.System)
 *     }
 */
@Immutable
data class ThemeState(val mode: ThemeMode = ThemeMode.System)

/** Walk a ContextWrapper chain up to the hosting Activity. */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
