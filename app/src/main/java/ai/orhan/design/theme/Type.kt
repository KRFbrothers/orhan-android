package ai.orhan.design.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import ai.orhan.R

/* ============================================================================
 *  Orhan AI — Typography
 *
 *  Typeface: Lato (the mockup's --font-sans), shipped at 400 / 700 / 900 only.
 *  Put the three files in app/src/main/res/font/:
 *      lato_regular.ttf   (400)
 *      lato_bold.ttf      (700)
 *      lato_black.ttf     (900)
 *
 *  Because the family has exactly three weights, Medium (500) and SemiBold (600)
 *  are aliased to Bold (700), and ExtraBold (800) to Black (900). Without those
 *  aliases Compose would synthesise a fake weight and the render would drift
 *  from the design.
 *
 *  OPTIONAL FALLBACK — Inter (see FallbackFonts.kt)
 *  Lato stays the brand typeface and the default for every screen. Inter is
 *  available as an opt-in alternative only: `OrhanTypographyInter` gives the
 *  exact same scale in Inter, and `OrhanFallbackFonts.Inter` can be applied
 *  to a single TextStyle. Nothing in this file references Inter, so the default
 *  render is byte-identical to before. Use it when a screen needs a real
 *  Medium (500) / SemiBold (600) instead of the Bold alias, or when rendering
 *  user content in scripts Lato does not cover.
 *
 *  Sizes are mapped from the mockup's real CSS values, not from a generic scale:
 *      21px   profile name        -> headlineSmall   22sp / 28
 *      20px   stat number         -> titleLarge      20sp / 26
*      17px   sheet title         -> titleLarge      20sp / 26  (same slot)
 *      16px   card title          -> titleMedium     16sp / 22
 *      15.5px list row name       -> titleMedium     16sp / 22
*      15px   chat title          -> titleSmall      15sp / 20
*      14.5px message bubble      -> bodyMedium      14.5sp / 21
 *      13.5px row snippet         -> bodySmall       13.5sp / 19
 *      12.5px helper text         -> bodySmall       13sp / 19
 *      11.5px meta / timestamp    -> labelMedium     12sp / 16
 *      11px   bottom-nav label    -> labelSmall      11sp / 14
 *      10.5px uppercase kicker    -> labelSmall      11sp / 14  (tracking .06em)
 * ==========================================================================*/

val Lato = FontFamily(
    Font(R.font.lato_regular, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.lato_bold, FontWeight.Medium, FontStyle.Normal),
    Font(R.font.lato_bold, FontWeight.SemiBold, FontStyle.Normal),
    Font(R.font.lato_bold, FontWeight.Bold, FontStyle.Normal),
    Font(R.font.lato_black, FontWeight.ExtraBold, FontStyle.Normal),
    Font(R.font.lato_black, FontWeight.Black, FontStyle.Normal),
)

/**
 * The mockup sets tight line-heights (1.25–1.45). Compose adds extra leading
 * above the first line and below the last by default, which pushes every block
 * a few dp lower than the design — so trim it.
 */
private val TrimmedLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun lato(
    size: Float,
    lineHeight: Float,
    weight: FontWeight,
    tracking: Float = 0f,
) = TextStyle(
    fontFamily = Lato,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp,
    lineHeightStyle = TrimmedLineHeight,
)

val OrhanTypography = Typography(
    /* ---------- Display — marketing / empty states only ---------- */
    displayLarge = lato(57f, 64f, FontWeight.Black, -0.25f),
    displayMedium = lato(45f, 52f, FontWeight.Black),
    displaySmall = lato(36f, 44f, FontWeight.Black),

    /* ---------- Headline — screen titles, profile name ---------- */
    headlineLarge = lato(32f, 40f, FontWeight.Black, -0.4f),
    headlineMedium = lato(28f, 36f, FontWeight.Black, -0.4f),
    headlineSmall = lato(22f, 28f, FontWeight.Black, -0.44f), // 21px profile name

    /* ---------- Title — stat numbers, sheet titles, list row names ---------- */
    titleLarge = lato(20f, 26f, FontWeight.Black, -0.4f),     // 20px stat, 17px sheet
    titleMedium = lato(16f, 22f, FontWeight.Bold, -0.16f),    // 16px card, 15.5px row
    titleSmall = lato(15f, 20f, FontWeight.Bold),             // 15px chat title

    /* ---------- Body — bubbles, snippets, helper copy ---------- */
    bodyLarge = lato(16f, 24f, FontWeight.Normal),
    bodyMedium = lato(14.5f, 21f, FontWeight.Normal),         // 14.5px bubble
    bodySmall = lato(13f, 19f, FontWeight.Normal),            // 13.5px snippet

    /* ---------- Label — buttons, meta, nav ---------- */
    labelLarge = lato(14f, 20f, FontWeight.Bold),
    labelMedium = lato(12f, 16f, FontWeight.Bold),            // 11.5px meta / time
    labelSmall = lato(11f, 14f, FontWeight.Bold),             // 11px nav label
)

/* ---------- Named styles for the pieces M3 has no slot for ---------- */

object OrhanTextStyles {

    /** 14.5sp / 21 — message bubble copy. */
    val bubble = lato(14.5f, 21f, FontWeight.Normal)

    /** 10.5sp / 14, .06em tracking, uppercase — "CHATS", "AGENTS" stat captions. */
    val kicker = lato(10.5f, 14f, FontWeight.Black, 0.63f)

    /** 10.5sp / 14, .09em tracking, uppercase — "PRO" badge, section eyebrows. */
    val eyebrow = lato(10.5f, 14f, FontWeight.Black, 0.95f)

    /** 11.5sp / 16 — timestamps, "3.2 GB of 15 GB". */
    val meta = lato(11.5f, 16f, FontWeight.Bold)

    /** 13.5sp / 19 — chat row snippet, settings row subtitle. */
    val snippet = lato(13.5f, 19f, FontWeight.Normal)

    /** 15.5sp / 20 — chat row name, settings row title. */
    val rowTitle = lato(15.5f, 20f, FontWeight.Bold)

    /** 20sp / 26, -.02em — profile stat numer. */
    val statNumber = lato(20f, 26f, FontWeight.Black, -0.4f)

    /** 11sp / 14 — bottom navigation label. */
    val navLabel = lato(11f, 14f, FontWeight.Bold)

    /** 15sp / 20 — primary button label. */
    val button = lato(15f, 20f, FontWeight.Bold)
}
