package ai.orhan.feature.settings.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ai.orhan.design.theme.OrhanTextStyles
import ai.orhan.design.theme.OrhanTheme

/** Shared scaffold for Settings detail pages: same top bar as AppearanceScreen. */
@Composable
internal fun SettingsDetailScaffold(
    title: String,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { snackbarHostState?.let { SnackbarHost(it) } },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(bottom = inner.calculateBottomPadding()),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground)
                }
                Text(
                    text = title,
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                content = content,
            )
        }
    }
}

@Composable
internal fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = OrhanTextStyles.kicker,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp),
    )
}

@Composable
internal fun FootNote(text: String) {
    Text(
        text = text,
        style = OrhanTextStyles.snippet,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
    )
}

/** Card group with hairline dividers between children. */
@Composable
internal fun GroupCard(rows: List<@Composable () -> Unit>) {
    val colors = OrhanTheme.colors
    val shapes = OrhanTheme.shapes
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.card)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, colors.hairline, shapes.card),
    ) {
        rows.forEachIndexed { i, row ->
            row()
            if (i != rows.lastIndex) {
                Box(
                    Modifier.fillMaxWidth().padding(start = 16.dp).height(1.dp).background(colors.hairline),
                )
            }
        }
    }
}

@Composable
private fun RowTexts(title: String, subtitle: String?, titleColor: Color, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(title, style = OrhanTextStyles.rowTitle, color = titleColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (subtitle != null) {
            Text(
                subtitle,
                style = OrhanTextStyles.snippet,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun SwitchRow(title: String, subtitle: String? = null, checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowTexts(title, subtitle, MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
internal fun NavRow(
    title: String,
    subtitle: String? = null,
    value: String? = null,
    destructive: Boolean = false,
    showChevron: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowTexts(
            title,
            subtitle,
            if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            Modifier.weight(1f),
        )
        if (value != null) {
            Text(value, style = OrhanTextStyles.snippet, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp)); trailing()
        } else if (showChevron && onClick != null) {
            Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
