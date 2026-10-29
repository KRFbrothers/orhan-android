package ai.orhan.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ai.orhan.design.theme.OrhanTextStyles
import ai.orhan.design.theme.OrhanTheme

/* ============================================================================
 *  Settings — the list that hosts the Appearance entry point.
 *
 *  Only the Appearance row is wired; the rest are the rows the design already
 *  shows, kept as plain callbacks so this file has no dependency on the other
 *  feature modules.
 *
 *  Rows are real Material 3 [ListItem]s. The previous hand-rolled Row gave us
 *  no M3 semantics (no Role, no state layer, no 72dp two-line spec) and had to
 *  re-implement padding, alignment and ellipsis by hand.
 * ==========================================================================*/

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onOpenAppearance: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onOpenChatPreferences: () -> Unit = {},
    onOpenStorage: () -> Unit = {},
    onOpenLanguage: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    signedInEmail: String? = null,
    onSignOut: (() -> Unit)? = null,
) {
    val colors = OrhanTheme.colors
    val shapes = OrhanTheme.shapes

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 4.dp),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = "PREFERENCES",
                style = OrhanTextStyles.kicker,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.card)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, colors.hairline, shapes.card),
            ) {
                SettingsRow(
                    title = "Appearance",
                    subtitle = "Theme, dark mode",
                    icon = { tint ->
                        Icon(
                            imageVector = Icons.Rounded.DarkMode,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    onClick = onOpenAppearance,
                )
                RowDivider()
                SettingsRow(
                    title = "Notifications",
                    subtitle = "Push, sounds, badges",
                    onClick = onOpenNotifications,
                )
                RowDivider()
                SettingsRow(
                    title = "Privacy",
                    subtitle = "Data, history, blocking",
                    onClick = onOpenPrivacy,
                )
                RowDivider()
                SettingsRow(
                    title = "Chat preferences",
                    subtitle = "Send with Enter, typing indicators",
                    onClick = onOpenChatPreferences,
                )
                RowDivider()
                SettingsRow(
                    title = "Storage",
                    subtitle = "Cache and downloads",
                    onClick = onOpenStorage,
                )
                RowDivider()
                SettingsRow(
                    title = "Language",
                    subtitle = "English (US)",
                    onClick = onOpenLanguage,
                )
                RowDivider()
                SettingsRow(
                    title = "About",
                    subtitle = "Version, terms, licences",
                    onClick = onOpenAbout,
                )
            }

            if (onSignOut != null) {
                Text(
                    text = "ACCOUNT",
                    style = OrhanTextStyles.kicker,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 8.dp),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.card)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, colors.hairline, shapes.card),
                ) {
                    SignOutRow(email = signedInEmail, onClick = onSignOut)
                }
            }
        }
    }
}

/** Destructive "Sign out" row; tinted with the error colour, no chevron. */
@Composable
private fun SignOutRow(email: String?, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick),
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            headlineColor = MaterialTheme.colorScheme.error,
            supportingColor = MaterialTheme.colorScheme.onSurfaceVariant,
            leadingIconColor = MaterialTheme.colorScheme.error,
        ),
        headlineContent = { Text("Sign out", style = OrhanTextStyles.rowTitle) },
        supportingContent = email?.let { { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
        leadingContent = {
            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(20.dp))
        },
    )
}

/**
 * 1dp hairline between rows.
 *
 * Deliberately *outside* the [ListItem]s and drawn with
 * [OrhanColors.hairline] rather than `ColorScheme.outline`: this is a
 * decorative rule inside a card, not a component border, so it stays at the
 * mockup's light value. M3's `outline` is now a 3:1 component border and would
 * be far too heavy here.
 */
@Composable
private fun RowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
            .height(1.dp)
            .background(OrhanTheme.colors.hairline),
    )
}

/**
 * One settings row, built on M3 [ListItem].
 *
 * Height: `heightIn(min = 72.dp)` — the M3 two-line spec height. A *fixed*
 * `height(72.dp)` would clip the two text lines once the user raises the system
 * font scale past ~1.4x, so the minimum is used instead: it renders at exactly
 * 72dp at normal scale and grows only when the text genuinely needs it.
 *
 * @param subtitle optional second line. When null the row collapses to the
 *   one-line layout and [ListItem] drops `supportingContent` entirely.
 * @param icon optional leading tile. Receives the tint to draw with.
 */
@Composable
private fun SettingsRow(
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    icon: (@Composable (Color) -> Unit)? = null,
) {
    val colors = OrhanTheme.colors

    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            // Two-line rows get the M3 72dp spec height; one-line rows keep
            // M3's own 56dp. `heightIn` (not `height`) so a large system font
            // scale grows the row instead of clipping the second line.
            .then(if (subtitle != null) Modifier.heightIn(min = 72.dp) else Modifier)
            .clickable(role = Role.Button, onClick = onClick),
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            headlineColor = MaterialTheme.colorScheme.onSurface,
            supportingColor = MaterialTheme.colorScheme.onSurfaceVariant,
            leadingIconColor = colors.onBrandSoft,
            trailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        headlineContent = {
            Text(
                text = title,
                style = OrhanTextStyles.rowTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = subtitle?.let { text ->
            {
                Text(
                    text = text,
                    style = OrhanTextStyles.snippet,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        leadingContent = icon?.let { draw ->
            {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(OrhanTheme.shapes.avatarMessage)
                        .background(colors.brandSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    draw(colors.onBrandSoft)
                }
            }
        },
        trailingContent = {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        },
    )
}

@Preview(name = "Settings — light", showBackground = true)
@Composable
private fun SettingsLightPreview() {
    OrhanTheme(darkTheme = false) { SettingsScreen(signedInEmail = "you@example.com", onSignOut = {}) }
}

@Preview(name = "Settings — dark", showBackground = true)
@Composable
private fun SettingsDarkPreview() {
    OrhanTheme(darkTheme = true) { SettingsScreen() }
}

/**
 * Row variants side by side: with icon + subtitle, subtitle only, and title
 * only. Proves the nullable slots collapse correctly.
 */
@Preview(name = "Settings row variants", showBackground = true, heightDp = 260)
@Composable
private fun SettingsRowVariantsPreview() {
    OrhanTheme(darkTheme = false) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .clip(OrhanTheme.shapes.card)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, OrhanTheme.colors.hairline, OrhanTheme.shapes.card),
        ) {
            SettingsRow(
                title = "Appearance",
                subtitle = "Theme, dark mode",
                icon = { tint ->
                    Icon(
                        imageVector = Icons.Rounded.DarkMode,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(20.dp),
                    )
                },
                onClick = {},
            )
            RowDivider()
            SettingsRow(title = "Notifications", subtitle = "Push, sounds, badges", onClick = {})
            RowDivider()
            SettingsRow(title = "Language", onClick = {})
        }
    }
}
