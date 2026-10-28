package ai.orhan.design.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/* ============================================================================
 *  Orhan AI — Colour tokens
 *  Source of truth: the shipped mockup (webpages/orhan-android-app_v2).
 *  Every literal below is copied from that file's CSS custom properties.
 *  Nothing here is invented; derived values are marked "DERIVED".
 * ==========================================================================*/

/* ---------- raw brand palette (exactly as in the mockup) ---------- */

// Brand green
val Green700 = Color(0xFF087C45)   // --primary (light)
val Green750 = Color(0xFF0B8249)   // --primary-grad start
val Green800 = Color(0xFF066A3B)   // --primary-ink (light)
val Green900 = Color(0xFF06603A)   // artifact thumb mid stop
val Green950 = Color(0xFF083F28)   // artifact thumb end stop
val Green50 = Color(0xFFEEF8F2)    // --nv-green-50 (light)
val GreenSoft = Color(0xFFDDF3E5)  // --primary-soft (light)
val Mint = Color(0xFF42D982)       // --mint / logo mint
val MintInk = Color(0xFF06291A)    // badge text on mint
val BrandSoft = Color(0xFFCBE7D2)  // --brand-soft (light) / --primary (dark)
val BrandSoftDark = Color(0xFF183826) // --brand-soft (dark)
val BrandSoftBright = Color(0xFFDAF0DF) // --primary-grad start (dark)

// Neutrals — light
val Neutral0 = Color(0xFFFFFFFF)   // --surface (light)
val Neutral50 = Color(0xFFF6F8F5)  // --surface-2 (light)
val Neutral100 = Color(0xFFEEF2EF) // --canvas / --surface-3 (light)
val Neutral150 = Color(0xFFE9EFEA) // --line-soft (light)
val Neutral200 = Color(0xFFDDE6E0) // --line (light)
val Neutral250 = Color(0xFFD7E2DB) // --field-edge (light)

// M3 `outline` — neutral-variant tone 50. A *visible* border, not a whisper.
// The mockup's --line (#DDE6E0) is tone ~90 and only reaches 1.28:1 on white,
// so every outlined component would read as disabled. This value keeps the
// brand's green-tinted neutral ramp and clears the 3:1 UI threshold on both
// the white card surface (3.60:1) and the canvas (3.19:1).
// Brand hairline borders keep using OrhanColors.hairline (#E9EFEA) — thin
// in-card rules are decorative, not component outlines, and stay as they are.
val Neutral300 = Color(0xFF7A8B80) // M3 outline (light)
val Ink900 = Color(0xFF182B23)     // --ink (light)
val Ink600 = Color(0xFF4F6157)     // --ink-2 (light)
val Ink500 = Color(0xFF5C6D63)     // --ink-3 (light)

// Neutrals — dark
val DarkCanvas = Color(0xFF141816) // --canvas (dark)
val DarkSurface = Color(0xFF1D2220) // --surface (dark)
val DarkSurface2 = Color(0xFF272E2B) // --surface-2 (dark)
val DarkSurface3 = Color(0xFF323B36) // --surface-3 (dark)
val DarkLine = Color(0xFF3D4842)   // --line (dark)
val DarkLineSoft = Color(0xFF2B3330) // --line-soft (dark)

// M3 `outline` (dark): #3D4842 only reached 1.69:1 on the dark surface. This
// is the lightest green-tinted value that clears 3:1 against the surface
// (4.20:1), the canvas (4.67:1) and surfaceContainerHighest (3.02:1).
val DarkLineStrong = Color(0xFF72877C) // M3 outline (dark)
val DarkInk = Color(0xFFF7F9F7)    // --ink (dark)
val DarkInk2 = Color(0xFFC3CDC7)   // --ink-2 (dark)
val DarkInk3 = Color(0xFF93A199)   // --ink-3 (dark)
val DarkPrimarySoft = Color(0xFF235039) // --primary-soft (dark)
val DarkPrimaryInk = Color(0xFFA6E6B9)  // --primary-ink (dark)
val DarkOnPrimary = Color(0xFF17201B)   // --on-primary (dark)
val DarkNavIndicator = Color(0xFF2C4A3A) // nav active pill (dark)
val DarkNavIndicatorInk = Color(0xFFBDE9CB) // nav active label (dark)

// Chat bubbles
val AgentBubbleLight = Color(0xFFFFFFFF) // --agent-bubble (light)
val AgentBubbleDark = Color(0xFF303433)  // --agent-bubble (dark)
val UserBubbleLight = Color(0xFF087C45)  // --user-bubble (light)
val UserBubbleDark = Color(0xFF505752)   // --user-bubble (dark)

// Semantic
val DangerLight = Color(0xFFA4271E) // --danger (light)
val DangerDark = Color(0xFFF2B8B5)  // --danger (dark)

// DERIVED — danger tinted over the surface at 12% (light) / 16% (dark).
// Only used for M3's errorContainer slot, which the mockup never renders.
val ErrorContainerLight = Color(0xFFF4E5E4)
val ErrorContainerDark = Color(0xFF3F3A38)

/* ---------- Material 3 ColorScheme — light ---------- */

val OrhanLightColorScheme = lightColorScheme(
    primary = Green700,
    onPrimary = Neutral0,
    primaryContainer = GreenSoft,
    onPrimaryContainer = Green800,
    inversePrimary = BrandSoft,

    secondary = Mint,
    onSecondary = MintInk,
    secondaryContainer = BrandSoft,
    onSecondaryContainer = Ink900,

    tertiary = Green800,
    onTertiary = Neutral0,
    tertiaryContainer = Green50,
    onTertiaryContainer = Green800,

    background = Neutral100,
    onBackground = Ink900,
    surface = Neutral0,
    onSurface = Ink900,
    // M3 deprecates `surfaceVariant` in favour of `surfaceContainerHighest`;
    // the two must carry the SAME tone or a component that reads the old slot
    // renders darker than one that reads the new slot. Both are Neutral100.
    surfaceVariant = Neutral100,
    onSurfaceVariant = Ink600,
    surfaceTint = Green700,

    inverseSurface = Ink900,
    inverseOnSurface = DarkInk,

    error = DangerLight,
    onError = Neutral0,
    errorContainer = ErrorContainerLight,
    onErrorContainer = DangerLight,

    outline = Neutral300,
    outlineVariant = Neutral150,
    // M3 spec: the scrim is pure black. The mockup's rgba(24,43,35,.10) alpha
    // already lives in OrhanColors.scrim; the ColorScheme slot is what M3
    // components (ModalBottomSheet, Dialog) darken the page with, and a green
    // cast there tints every dialog behind it.
    scrim = Color(0xFF000000),

    surfaceBright = Neutral0,
    surfaceDim = Neutral100,
    surfaceContainerLowest = Neutral0,
    surfaceContainerLow = Neutral50,
    surfaceContainer = Neutral50,
    surfaceContainerHigh = Neutral100,
    surfaceContainerHighest = Neutral100,
)

/* ---------- Material 3 ColorScheme — dark ---------- */

val OrhanDarkColorScheme = darkColorScheme(
    primary = BrandSoft,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimarySoft,
    onPrimaryContainer = DarkPrimaryInk,
    inversePrimary = Green700,

    secondary = Mint,
    onSecondary = MintInk,
    secondaryContainer = BrandSoftDark,
    onSecondaryContainer = BrandSoft,

    tertiary = DarkPrimaryInk,
    onTertiary = DarkOnPrimary,
    tertiaryContainer = DarkPrimarySoft,
    onTertiaryContainer = DarkPrimaryInk,

    background = DarkCanvas,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    // Aligned with surfaceContainerHighest (DarkSurface3) — see the light note.
    surfaceVariant = DarkSurface3,
    onSurfaceVariant = DarkInk2,
    surfaceTint = BrandSoft,

    inverseSurface = DarkInk,
    inverseOnSurface = Ink900,

    error = DangerDark,
    onError = DarkOnPrimary,
    errorContainer = ErrorContainerDark,
    onErrorContainer = DangerDark,

    outline = DarkLineStrong,
    outlineVariant = DarkLineSoft,
    scrim = Color(0xFF000000),

    surfaceBright = DarkSurface3,
    surfaceDim = DarkCanvas,
    surfaceContainerLowest = DarkCanvas,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkSurface2,
    surfaceContainerHighest = DarkSurface3,
)

/* ============================================================================
 *  Brand extras — the tokens Material 3 has no slot for.
 *  Read them with LocalOrhanColors.current.
 * ==========================================================================*/

@Immutable
data class OrhanColors(
    /** Logo mint — badges, live dots, agent avatar ring. */
    val logoMint: Color,
    /** Text/icon colour that sits on [logoMint]. */
    val onLogoMint: Color,
    /** Soft brand wash — chips, active nav pill, "mini" pills. */
    val brandSoft: Color,
    /** Ink that sits on [brandSoft]. */
    val onBrandSoft: Color,
    /** Primary gradient start (top). */
    val gradientStart: Color,
    /** Primary gradient end (bottom). */
    val gradientEnd: Color,
    /** Ink that sits on the primary gradient (FAB, send button, CTA). */
    val onGradient: Color,
    /** Incoming (agent) message bubble fill. */
    val agentBubble: Color,
    /** Incoming (agent) message bubble text. */
    val agentBubbleInk: Color,
    /** Outgoing (user) message bubble fill. */
    val userBubble: Color,
    /** Outgoing (user) message bubble text. */
    val userBubbleInk: Color,
    /** Composer / text-field fill. */
    val field: Color,
    /** Composer / text-field 1dp border. */
    val fieldEdge: Color,
    /** Bottom navigation bar fill (translucent in the mockup). */
    val navBackground: Color,
    /** Active bottom-nav indicator pill. */
    val navIndicator: Color,
    /** Active bottom-nav icon + label. */
    val navIndicatorInk: Color,
    /** Inactive bottom-nav icon + label. */
    val navInactiveInk: Color,
    /** Modal scrim. */
    val scrim: Color,
    /** Hairline used inside cards (lighter than [ColorScheme.outline]). */
    val hairline: Color,
) {
    /** The brand's primary fill: 180° top-to-bottom green gradient. */
    val primaryGradient: Brush
        get() = Brush.verticalGradient(listOf(gradientStart, gradientEnd))

    companion object {
        val Light = OrhanColors(
            logoMint = Mint,
            onLogoMint = MintInk,
            brandSoft = GreenSoft,
            onBrandSoft = Green800,
            gradientStart = Green750,
            gradientEnd = Green700,
            onGradient = Neutral0,
            agentBubble = AgentBubbleLight,
            agentBubbleInk = Ink900,
            userBubble = UserBubbleLight,
            userBubbleInk = Neutral0,
            field = Neutral100,
            fieldEdge = Neutral250,
            navBackground = Color(0xF0FFFFFF), // rgba(255,255,255,.94)
            navIndicator = GreenSoft,
            navIndicatorInk = Green800,
            navInactiveInk = Ink500,
            scrim = Color(0x1A182B23),         // rgba(24,43,35,.10)
            hairline = Neutral150,
        )

        val Dark = OrhanColors(
            logoMint = Mint,
            onLogoMint = MintInk,
            brandSoft = DarkPrimarySoft,
            onBrandSoft = DarkPrimaryInk,
            gradientStart = BrandSoftBright,
            gradientEnd = BrandSoft,
            onGradient = DarkOnPrimary,
            agentBubble = AgentBubbleDark,
            agentBubbleInk = DarkInk,
            userBubble = UserBubbleDark,
            userBubbleInk = DarkInk,
            field = DarkSurface2,
            fieldEdge = DarkLine,
            navBackground = Color(0xF21D2220), // rgba(29,34,32,.95)
            navIndicator = DarkNavIndicator,
            navIndicatorInk = DarkNavIndicatorInk,
            navInactiveInk = DarkInk3,
            scrim = Color(0x73000000),         // rgba(0,0,0,.45)
            hairline = DarkLineSoft,
        )
    }
}
