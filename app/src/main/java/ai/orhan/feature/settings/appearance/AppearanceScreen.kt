package ai.orhan.feature.settings.appearance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ai.orhan.design.theme.DarkCanvas
import ai.orhan.design.theme.DarkSurface
import ai.orhan.design.theme.Neutral0
import ai.orhan.design.theme.Neutral100
import ai.orhan.design.theme.OrhanColors
import ai.orhan.design.theme.OrhanTextStyles
import ai.orhan.design.theme.OrhanTheme
import ai.orhan.design.theme.ThemeMode

/* ============================================================================
 *  Settings → Appearance
 *
 *  Three radio options. Tapping one writes to DataStore through the ViewModel;
 *  the checked state comes back from the same Flow, so the screen always shows
 *  what is actually persisted.
 *
 *  Every colour, radius and text style below comes from the existing theme:
 *      MaterialTheme.colorScheme.*   surfaces, ink, primary
 *      OrhanTheme.colors.*         brand extras (brandSoft, hairline, field…)
 *      OrhanTheme.shapes.*         card / pill / preview radii
 *      OrhanTextStyles.*           rowTitle, snippet, kicker, meta
 *  No new colour, size or radius is introduced.
 * ==========================================================================*/

/** One row's copy. Kept out of the composable so it is trivially testable. */
private data class ThemeOption(
    val mode: ThemeMode,
    val title: String,
    val description: String,
)

private val Options = listOf(
    ThemeOption(
        mode = ThemeMode.System,
        title = "System",
        description = "Follow your device's dark mode setting.",
    ),
    ThemeOption(
        mode = ThemeMode.Light,
        title = "Light",
        description = "Always use the light theme.",
    ),
    ThemeOption(
        mode = ThemeMode.Dark,
        title = "Dark",
        description = "Always use the dark theme.",
    ),
)

/**
 * @param onBack pops the screen.
 * @param viewModel supplies the state and owns the DataStore write.
 */
@Composable
fun AppearanceScreen(
    viewModel: AppearanceViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AppearanceContent(
        state = state,
        onSelect = viewModel::setThemeMode,
        onBack = onBack,
        modifier = modifier,
    )
}

/**
 * Stateless body — this is the overload the previews and UI tests drive, so the
 * screen can be rendered without a ViewModel or a DataStore.
 */
@Composable
fun AppearanceContent(
    state: AppearanceUiState,
    onSelect: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val colors = OrhanTheme.colors
    val shapes = OrhanTheme.shapes

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        /* ------------------------------ top bar ------------------------------ */
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
                text = "Appearance",
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
                text = "THEME",
                style = OrhanTextStyles.kicker,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp),
            )

            /* --------------------------- radio group --------------------------- */
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.card)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, colors.hairline, shapes.card)
                    .selectableGroup(),
            ) {
                Options.forEachIndexed { index, option ->
                    ThemeOptionRow(
                        option = option,
                        selected = state.displayed == option.mode,
                        enabled = !state.isSaving,
                        onClick = { onSelect(option.mode) },
                    )
                    if (index != Options.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 76.dp)
                                .height(1.dp)
                                .background(colors.hairline),
                        )
                    }
                }
            }

            /* ------------------------------ error ------------------------------ */
            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage,
                    style = OrhanTextStyles.snippet,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 4.dp, top = 12.dp),
                )
            }

            Text(
                text = "System follows your device. Light and Dark override it for this app only.",
                style = OrhanTextStyles.snippet,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 12.dp),
            )
        }
    }
}

/* ------------------------------- one row -------------------------------- */

@Composable
private fun ThemeOptionRow(
    option: ThemeOption,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = OrhanTheme.colors
    val shapes = OrhanTheme.shapes

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThemeSwatch(mode = option.mode)

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = option.title,
                style = OrhanTextStyles.rowTitle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = option.description,
                style = OrhanTextStyles.snippet,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // The radio is decorative here: the whole row is the selectable target,
        // so it must not be focusable on its own or TalkBack would announce it
        // twice. The row carries Role.RadioButton and the selected state.
        RadioButton(
            selected = selected,
            onClick = null,
            enabled = enabled,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor = MaterialTheme.colorScheme.outline,
            ),
        )
    }
}

/**
 * A 44 x 44 miniature of the theme, drawn from the real tokens rather than an
 * icon — so it stays correct if the palette ever changes.
 *
 * System shows a split swatch (light on the left, dark on the right).
 */
@Composable
private fun ThemeSwatch(mode: ThemeMode) {
    val shapes = OrhanTheme.shapes
    val light = OrhanColors.Light
    val dark = OrhanColors.Dark

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(shapes.preview)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, OrhanTheme.colors.hairline, shapes.preview),
    ) {
        when (mode) {
            ThemeMode.Light -> SwatchBody(
                canvas = Neutral100,
                surface = Neutral0,
                accent = light.gradientEnd,
            )

            ThemeMode.Dark -> SwatchBody(
                canvas = DarkCanvas,
                surface = DarkSurface,
                accent = dark.gradientEnd,
            )

            ThemeMode.System -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        SwatchBody(
                            canvas = Neutral100,
                            surface = Neutral0,
                            accent = light.gradientEnd,
                        )
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        SwatchBody(
                            canvas = DarkCanvas,
                            surface = DarkSurface,
                            accent = dark.gradientEnd,
                        )
                    }
                }
            }
        }
    }
}

/** Canvas + one surface bar + one accent dot, at miniature scale. */
@Composable
private fun SwatchBody(canvas: Color, surface: Color, accent: Color) {
    val shapes = OrhanTheme.shapes
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(canvas)
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(surface),
        )
        Box(
            modifier = Modifier
                .width(14.dp)
                .height(6.dp)
                .clip(shapes.pill)
                .background(accent),
        )
    }
}

/* -------------------------------- previews ------------------------------- */

@Preview(name = "Appearance — light", showBackground = true)
@Composable
private fun AppearanceLightPreview() {
    OrhanTheme(darkTheme = false) {
        AppearanceContent(
            state = AppearanceUiState(selected = ThemeMode.Light),
            onSelect = {},
        )
    }
}

@Preview(name = "Appearance — dark", showBackground = true)
@Composable
private fun AppearanceDarkPreview() {
    OrhanTheme(darkTheme = true) {
        AppearanceContent(
            state = AppearanceUiState(selected = ThemeMode.Dark),
            onSelect = {},
        )
    }
}
