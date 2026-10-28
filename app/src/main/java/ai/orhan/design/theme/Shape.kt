package ai.orhan.design.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp

/* ============================================================================
 *  Orhan AI — Shape tokens
 *
 *  M3 [Shapes] covers the five component slots. Everything the design uses
 *  beyond those lives in [OrhanShapes] and is read via LocalOrhanShapes.
 *
 *  Radii come from the shipped mockup:
 *      card / group container   20dp   (M3 "large" slot is 16dp — see note)
 *      stat tile                18dp
 *      avatar (list)            17dp
 *      avatar (chat header)     14dp
 *      avatar (message)          9dp
 *      composer field           28dp
 *      bottom sheet             28dp top corners
 *      chip / pill              fully rounded
 *      nav indicator pill       58 x 30, fully rounded
 *      message bubble           20dp, with the tail corner at 6dp
 * ==========================================================================*/

/**
 * M3 component slots.
 *
 * `large` is 16dp rather than the mockup's 20dp on purpose: M3 uses `large` for
 * dialogs and bottom sheets, and 16dp is the value the design system's own
 * component spec expects there. Cards in this app use [OrhanShapes.card]
 * (20dp) explicitly, so the two never fight.
 */
val OrhanShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * Brand radii that have no M3 slot.
 * Read with `LocalOrhanShapes.current`.
 */
@Immutable
data class OrhanShapeTokens(
    /** Cards, list groups, agent cards — 20dp. */
    val card: RoundedCornerShape,
    /** Stat tiles on Profile — 18dp. */
    val statTile: RoundedCornerShape,
    /** List avatar — 17dp. */
    val avatarList: RoundedCornerShape,
    /** Chat-header avatar — 14dp. */
    val avatarHeader: RoundedCornerShape,
    /** In-thread message avatar — 9dp. */
    val avatarMessage: RoundedCornerShape,
    /** Large profile avatar — 26dp. */
    val avatarProfile: RoundedCornerShape,
    /** Composer text field — 28dp. */
    val composer: RoundedCornerShape,
    /** Bottom sheet — 28dp on the top corners only. */
    val sheet: RoundedCornerShape,
    /** Filter chip — fully rounded. */
    val chip: RoundedCornerShape,
    /** Generic pill (badges, "mini" tags, segmented control) — fully rounded. */
    val pill: RoundedCornerShape,
    /** Bottom-nav active indicator — 58 x 30, fully rounded. */
    val navIndicator: RoundedCornerShape,
    /** FAB — 19dp squircle, not a circle. */
    val fab: RoundedCornerShape,
    /** Inline artifact card inside a bubble — 14dp. */
    val artifactCard: RoundedCornerShape,
    /** Text field inside a form (Edit profile) — 14dp. */
    val textField: RoundedCornerShape,
    /** Theme preview thumbnail — 14dp. */
    val preview: RoundedCornerShape,
) {
    /** Incoming (agent) bubble: 20dp, tail at bottom-start = 6dp. */
    val agentBubble: RoundedCornerShape
        get() = RoundedCornerShape(
            topStart = 20.dp,
            topEnd = 20.dp,
            bottomEnd = 20.dp,
            bottomStart = 6.dp,
        )

    /** Outgoing (user) bubble: 20dp, tail at bottom-end = 6dp. */
    val userBubble: RoundedCornerShape
        get() = RoundedCornerShape(
            topStart = 20.dp,
            topEnd = 20.dp,
            bottomEnd = 6.dp,
            bottomStart = 20.dp,
        )

    companion object {
        val Default = OrhanShapeTokens(
            card = RoundedCornerShape(20.dp),
            statTile = RoundedCornerShape(18.dp),
            avatarList = RoundedCornerShape(17.dp),
            avatarHeader = RoundedCornerShape(14.dp),
            avatarMessage = RoundedCornerShape(9.dp),
            avatarProfile = RoundedCornerShape(26.dp),
            composer = RoundedCornerShape(28.dp),
            sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            chip = RoundedCornerShape(percent = 50),
            pill = RoundedCornerShape(percent = 50),
            navIndicator = RoundedCornerShape(percent = 50),
            fab = RoundedCornerShape(19.dp),
            artifactCard = RoundedCornerShape(14.dp),
            textField = RoundedCornerShape(14.dp),
            preview = RoundedCornerShape(14.dp),
        )
    }
}
