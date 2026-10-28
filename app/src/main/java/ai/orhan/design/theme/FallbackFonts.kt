package ai.orhan.design.theme

import android.content.Context
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import ai.orhan.R

/* ============================================================================
 *  Orhan AI — Optional fallback typeface (Inter)
 *
 *  Lato is the brand typeface and stays the default everywhere. This file adds
 *  Inter as an OPT-IN alternative — nothing here is wired into OrhanTheme, so
 *  importing it changes no existing screen.
 *
 *  Why Inter exists here at all:
 *    - Lato ships 400 / 700 / 900 only. Inter is a variable font with a real
 *      100–900 axis, so it is the escape hatch if a future screen needs a
 *      genuine Medium (500) or SemiBold (600) instead of the Bold alias.
 *    - Inter has a much wider glyph + script coverage than Lato, so it is the
 *      sensible fallback for user-generated content in scripts Lato lacks.
 *
 *  Licence: SIL Open Font License 1.1 — see assets/fonts/inter/OFL.txt and
 *  THIRD-PARTY-NOTICES.md. Redistribution is permitted.
 *
 *  ---------------------------------------------------------------------------
 *  WHERE THE FILES GO
 *
 *  assets/fonts/inter/Inter[opsz,wght].ttf
 *      -> app/src/main/res/font/inter_variable.ttf
 *  assets/fonts/inter/Inter-Italic[opsz,wght].ttf
 *      -> app/src/main/res/font/inter_italic_variable.ttf
 *
 *  Android resource names must be lowercase a-z, 0-9 and underscore only —
 *  the brackets and the comma in the upstream filenames are illegal, which is
 *  why they are renamed. The `[opsz,wght]` suffix means these are VARIABLE
 *  fonts: one file carries every weight, selected through FontVariation.
 * ==========================================================================*/

/**
 * Inter, exposed as a [FontFamily] but never applied by default.
 *
 * Both files are variable fonts, so each weight is the SAME file registered
 * with a different `wght` axis value. Registering the file once and letting
 * Compose synthesise the rest would render every weight at 400 — the axis has
 * to be set explicitly, which is what [FontVariation.weight] does.
 *
 * Requires API 26+ for variable-font axes; the project's `minSdk` is 26.
 */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
object OrhanFallbackFonts {

    /** res/font name of the upright variable file. */
    const val RES_UPRIGHT = "inter_variable"

    /** res/font name of the italic variable file. */
    const val RES_ITALIC = "inter_italic_variable"

    private fun upright(weight: Int) = Font(
        resId = R.font.inter_variable,
        weight = FontWeight(weight),
        style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
    )

    private fun italic(weight: Int) = Font(
        resId = R.font.inter_italic_variable,
        weight = FontWeight(weight),
        style = FontStyle.Italic,
        variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
    )

    /**
     * Inter with the full 100–900 axis plus italics.
     *
     * The nine upright weights below cover the full 100–900 axis, so the
     * family resolves every slot in [OrhanTypography] without synthesis.
     */
    val Inter: FontFamily = FontFamily(
        upright(100), upright(200), upright(300),
        upright(400), upright(500), upright(600),
        upright(700), upright(800), upright(900),
        italic(400), italic(700),
    )

    /**
     * True when the Inter files are actually present in this build.
     *
     * This is a RUNTIME guard, not a compile-time one: `R.font.inter_variable`
     * must exist for this file to compile at all. The check matters for build
     * variants that strip the font (e.g. a size-constrained APK split) — call
     * it before opting in so such a build degrades to Lato instead of crashing
     * with `Resources.NotFoundException` on first composition.
     */
    fun isAvailable(context: Context): Boolean {
        val res = context.resources
        val uprightId = res.getIdentifier(RES_UPRIGHT, "font", context.packageName)
        val italicId = res.getIdentifier(RES_ITALIC, "font", context.packageName)
        return uprightId != 0 && italicId != 0
    }
}

/**
 * Returns a copy of this [Typography] with every slot re-pointed at [family].
 *
 * Sizes, line-heights, tracking and weights are carried over untouched — only
 * the family changes. Building the Inter variant this way (instead of writing a
 * second 15-slot scale by hand) means the two can never drift apart: fix a size
 * in [OrhanTypography] and the Inter variant inherits it automatically.
 */
fun Typography.withFontFamily(family: FontFamily): Typography = copy(
    displayLarge = displayLarge.copy(fontFamily = family),
    displayMedium = displayMedium.copy(fontFamily = family),
    displaySmall = displaySmall.copy(fontFamily = family),
    headlineLarge = headlineLarge.copy(fontFamily = family),
    headlineMedium = headlineMedium.copy(fontFamily = family),
    headlineSmall = headlineSmall.copy(fontFamily = family),
    titleLarge = titleLarge.copy(fontFamily = family),
    titleMedium = titleMedium.copy(fontFamily = family),
    titleSmall = titleSmall.copy(fontFamily = family),
    bodyLarge = bodyLarge.copy(fontFamily = family),
    bodyMedium = bodyMedium.copy(fontFamily = family),
    bodySmall = bodySmall.copy(fontFamily = family),
    labelLarge = labelLarge.copy(fontFamily = family),
    labelMedium = labelMedium.copy(fontFamily = family),
    labelSmall = labelSmall.copy(fontFamily = family),
)

/**
 * The Orhan type scale rendered in Inter — identical metrics to
 * [OrhanTypography], different typeface.
 *
 * Opt in explicitly; [OrhanTheme] keeps using Lato:
 *
 *     // default — Lato, unchanged
 *     OrhanTheme { App() }
 *
 *     // opt-in — Inter, same sizes
 *     OrhanTheme(typography = OrhanTypographyInter) { App() }
 *
 *     // opt-in with a runtime guard for size-constrained variants
 *     val ctx = LocalContext.current
 *     OrhanTheme(
 *         typography = if (OrhanFallbackFonts.isAvailable(ctx))
 *             OrhanTypographyInter else OrhanTypography,
 *     ) { App() }
 */
val OrhanTypographyInter: Typography by lazy {
    OrhanTypography.withFontFamily(OrhanFallbackFonts.Inter)
}

/**
 * Per-text override, for the one-off case where a single string needs Inter
 * (e.g. user content in a script Lato does not cover) while the rest of the
 * screen stays on Lato:
 *
 *     Text(
 *         text = userMessage,
 *         style = OrhanTextStyles.bubble.withFontFamily(OrhanFallbackFonts.Inter),
 *     )
 */
fun androidx.compose.ui.text.TextStyle.withFontFamily(family: FontFamily) =
    copy(fontFamily = family)
