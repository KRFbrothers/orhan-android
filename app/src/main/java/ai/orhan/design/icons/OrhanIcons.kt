package ai.orhan.design.icons

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Orhan's own icon set — the five symbols that are hand-drawn for this product
 * and have no Material Symbols equivalent.
 *
 * Source of truth: `design/index.html`, the `<symbol>` block at line 824 onward.
 * Every `pathData` string below is copied **verbatim** from that file's `d="…"`
 * attribute — no re-formatting, no re-numbering, no rounding. The only values that
 * are not verbatim are the `<circle>` elements, which are converted to an equivalent
 * two-arc path (same centre, same radius) because [ImageVector] paths are path-based.
 *
 * ### Why only five
 *
 * The design file defines 46 symbols. The other 41 are Material Symbols redrawn in
 * the same 24dp / 1.9-stroke house style — `i-search`, `i-menu`, `i-plus`, `i-mic`,
 * `i-send`, `i-settings`, `i-bell`, `i-lock`, `i-trash`, `i-star`, `i-globe`, …
 * Those are **not** duplicated here: use `Icons.Rounded.*` (or the 10 XML drawables
 * in `assets/icons/material-symbols/`) so the app tracks upstream Material updates
 * instead of freezing a copy. See `compose/icons/README.md` for the full mapping.
 *
 * ### Usage
 *
 * ```kotlin
 * Icon(
 *     imageVector = OrhanIcons.Spark,
 *     contentDescription = null,
 *     tint = OrhanTheme.colors.logoMint,
 * )
 * ```
 *
 * The vectors are built with [SolidColor] `Color.Black` as their paint, which is the
 * same convention `materialIcon {}` uses: [Icon]'s `tint` applies a `ColorFilter`
 * over the whole vector, so the black is replaced by whatever tint you pass. Leave
 * `tint` at its default (`LocalContentColor`) to inherit the surrounding text colour.
 *
 * ### Stroke weight
 *
 * Stroke icons are baked at **1.9dp**, the design's `.ic` default. The mockup bumps
 * this in a few places (`.mt .ic` 2.4, `.fab .ic` 2.1, `.pbadge .ic` 3). Rather than
 * ship a variant per weight, scale the icon: `Modifier.size(13.dp)` keeps the 1.9
 * ratio visually correct at small sizes, and for the rare heavy case use
 * [OrhanIcons.Spark] (filled, weight-independent) or a `Modifier.scale()`.
 */
object OrhanIcons {

    /* ------------------------------------------------------------------ *
     *  Path data — verbatim from design/index.html
     * ------------------------------------------------------------------ */

    /** `#i-spark` path 1 of 2 — the four-point star. */
    private const val SPARK_STAR =
        "M12 3.2 13.9 8.4 19 10.3 13.9 12.2 12 17.4 10.1 12.2 5 10.3 10.1 8.4z"

    /** `#i-spark` path 2 of 2 — the small satellite spark, bottom-right. */
    private const val SPARK_SATELLITE =
        "M18.4 15.6 19.2 18 21.5 18.8 19.2 19.6 18.4 22 17.6 19.6 15.3 18.8 17.6 18z"

    /** `#i-chats` — nav "Chats": rounded bubble with a bottom-left tail. */
    private const val CHATS_BUBBLE =
        "M4 7.5A2.5 2.5 0 0 1 6.5 5h11A2.5 2.5 0 0 1 20 7.5v6a2.5 2.5 0 0 1-2.5 2.5H9.5L4 20z"

    /** `#i-agents` — nav "Agents": front figure head. `<circle cx=9 cy=8 r=3.1>`. */
    private const val AGENTS_HEAD_FRONT =
        "M5.9 8a3.1 3.1 0 1 0 6.2 0a3.1 3.1 0 1 0-6.2 0"

    /** `#i-agents` — front figure shoulders. */
    private const val AGENTS_SHOULDERS_FRONT =
        "M3.4 19.2c0-3.1 2.5-5.2 5.6-5.2s5.6 2.1 5.6 5.2"

    /** `#i-agents` — back figure head. `<circle cx=17.2 cy=9.4 r=2.4>`. */
    private const val AGENTS_HEAD_BACK =
        "M14.8 9.4a2.4 2.4 0 1 0 4.8 0a2.4 2.4 0 1 0-4.8 0"

    /** `#i-agents` — back figure shoulder arc. */
    private const val AGENTS_SHOULDER_BACK =
        "M16.4 14.2c2.4.2 4.2 2 4.2 4.4"

    /** `#i-discover` — nav "Discover": compass ring. `<circle cx=12 cy=12 r=8.6>`. */
    private const val DISCOVER_RING =
        "M3.4 12a8.6 8.6 0 1 0 17.2 0a8.6 8.6 0 1 0-17.2 0"

    /** `#i-discover` — compass needle. */
    private const val DISCOVER_NEEDLE =
        "M15.6 8.4 13.7 13.7 8.4 15.6 10.3 10.3z"

    /** `#i-chatpref` — chat preferences: bubble with two text rules. */
    private const val CHATPREF_BUBBLE =
        "M4 7.4A2.4 2.4 0 0 1 6.4 5h11.2A2.4 2.4 0 0 1 20 7.4v6.2a2.4 2.4 0 0 1-2.4 2.4H9.6L4 20z"

    /** `#i-chatpref` — the two text rules inside the bubble. */
    private const val CHATPREF_RULES =
        "M8.4 9.6h7.2M8.4 12.6h4.4"

    /* ------------------------------------------------------------------ *
     *  Builder helpers
     * ------------------------------------------------------------------ */

    private const val VIEWPORT = 24f
    private const val STROKE = 1.9f

    /**
     * A filled path. `fill` is black so [Icon]'s tint can replace it; `stroke` is
     * null because the source symbol sets `stroke="none"`.
     */
    private fun ImageVector.Builder.filled(name: String, pathData: String) {
        addPath(
            pathData = addPathNodes(pathData),
            name = name,
            fill = SolidColor(Color.Black),
            stroke = null,
        )
    }

    /**
     * A stroked path, matching the design's `.ic` rule:
     * `fill:none; stroke:currentColor; stroke-width:1.9; linecap:round; linejoin:round`.
     */
    private fun ImageVector.Builder.stroked(name: String, pathData: String) {
        addPath(
            pathData = addPathNodes(pathData),
            name = name,
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = STROKE,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }

    private fun builder(name: String) = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = VIEWPORT,
        viewportHeight = VIEWPORT,
    )

    /* ------------------------------------------------------------------ *
     *  The icons
     * ------------------------------------------------------------------ */

    /**
     * Orhan's brand spark — the AI/agent mark.
     *
     * Used 13 times in the design: the "Agent" chip on chat rows, the message
     * timestamp glyph, the agent-card name suffix, and the assistant avatar.
     * Filled, so it reads correctly down to 13dp (`.agentsnip .ic`, `.mt .ic`).
     */
    val Spark: ImageVector by lazy {
        builder("Orhan.Spark").apply {
            filled("spark_star", SPARK_STAR)
            filled("spark_satellite", SPARK_SATELLITE)
        }.build()
    }

    /** Bottom-nav "Chats" — bubble with a bottom-left tail. Stroked. */
    val Chats: ImageVector by lazy {
        builder("Orhan.Chats").apply {
            stroked("chats_bubble", CHATS_BUBBLE)
        }.build()
    }

    /** Bottom-nav "Agents" — two overlapping figures. Stroked. */
    val Agents: ImageVector by lazy {
        builder("Orhan.Agents").apply {
            stroked("agents_head_front", AGENTS_HEAD_FRONT)
            stroked("agents_shoulders_front", AGENTS_SHOULDERS_FRONT)
            stroked("agents_head_back", AGENTS_HEAD_BACK)
            stroked("agents_shoulder_back", AGENTS_SHOULDER_BACK)
        }.build()
    }

    /** Bottom-nav "Discover" — compass ring with a needle. Stroked. */
    val Discover: ImageVector by lazy {
        builder("Orhan.Discover").apply {
            stroked("discover_ring", DISCOVER_RING)
            stroked("discover_needle", DISCOVER_NEEDLE)
        }.build()
    }

    /** Chat preferences — bubble with two text rules. Stroked. */
    val ChatPref: ImageVector by lazy {
        builder("Orhan.ChatPref").apply {
            stroked("chatpref_bubble", CHATPREF_BUBBLE)
            stroked("chatpref_rules", CHATPREF_RULES)
        }.build()
    }

    /**
     * Every Orhan-specific icon, keyed by its design symbol id.
     * Handy for a gallery screen or a screenshot test that renders the whole set.
     */
    val All: Map<String, ImageVector> by lazy {
        linkedMapOf(
            "i-spark" to Spark,
            "i-chats" to Chats,
            "i-agents" to Agents,
            "i-discover" to Discover,
            "i-chatpref" to ChatPref,
        )
    }
}

/**
 * Convenience wrapper so call sites read like the Material ones.
 *
 * ```kotlin
 * OrhanIcon(OrhanIcons.Spark, size = 13.dp, tint = OrhanTheme.colors.logoMint)
 * ```
 *
 * Defaults to [LocalContentColor] via [Icon]'s own default, so it inherits the
 * surrounding text colour when `tint` is omitted.
 */
@Composable
fun OrhanIcon(
    imageVector: ImageVector,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = Color.Unspecified,
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = tint,
    )
}
