package ai.orhan.feature.settings.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ai.orhan.design.theme.OrhanTextStyles
import ai.orhan.design.theme.OrhanTheme
import kotlinx.coroutines.launch

/** Settings → Storage. Fake numbers; clean-up actions only show a snackbar. */
@Composable
fun StorageScreen(onBack: () -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val toast: (String) -> Unit = { msg -> scope.launch { snackbar.showSnackbar(msg) } }
    val used = 3.2f
    val total = 15f

    SettingsDetailScaffold(title = "Storage", onBack = onBack, snackbarHostState = snackbar) {
        SectionLabel("Usage")
        Column(
            Modifier
                .fillMaxWidth()
                .clip(OrhanTheme.shapes.card)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, OrhanTheme.colors.hairline, OrhanTheme.shapes.card)
                .padding(16.dp),
        ) {
            Text("$used GB of ${total.toInt()} GB used", style = OrhanTextStyles.rowTitle, color = MaterialTheme.colorScheme.onSurface)
            Box(
                Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(OrhanTheme.shapes.pill)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(used / total)
                        .fillMaxHeight()
                        .clip(OrhanTheme.shapes.pill)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
        SectionLabel("Breakdown")
        GroupCard(listOf(
            { NavRow("Chat media", value = "1.6 GB") },
            { NavRow("Artifacts", value = "0.9 GB") },
            { NavRow("Cached previews", value = "0.5 GB") },
            { NavRow("Agent logs", value = "0.2 GB") },
        ))
        SectionLabel("Clean up")
        GroupCard(listOf(
            { NavRow("Cached previews", "0.5 GB", trailing = { TextButton({ toast("Cleared cached previews") }) { Text("Clear") } }) },
            { NavRow("Old chat media", "Older than 90 days", trailing = { TextButton({ toast("Removed old chat media") }) { Text("Clear") } }) },
            { NavRow("Agent logs", "0.2 GB", trailing = { TextButton({ toast("Cleared agent logs") }) { Text("Clear") } }) },
        ))
        SectionLabel("Library")
        GroupCard(listOf(
            { NavRow("Artifact library", "Files and outputs saved by agents", onClick = { toast("Coming soon") }) },
        ))
    }
}
