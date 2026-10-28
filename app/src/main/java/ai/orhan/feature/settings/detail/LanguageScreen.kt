package ai.orhan.feature.settings.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import ai.orhan.design.theme.OrhanTextStyles

private val Languages = listOf(
    "en-US" to ("English (US)" to "English"),
    "en-GB" to ("English (UK)" to "English"),
    "hi" to ("Hindi" to "हिन्दी"),
    "es" to ("Spanish" to "Español"),
    "ja" to ("Japanese" to "日本語"),
    "ar" to ("Arabic" to "العربية"),
)

/** Settings → Language. Local selection only. */
@Composable
fun LanguageScreen(onBack: () -> Unit) {
    var selected by rememberSaveable { mutableStateOf("en-US") }
    var alwaysAppLang by rememberSaveable { mutableStateOf(true) }

    SettingsDetailScaffold(title = "Language", onBack = onBack) {
        SectionLabel("App language")
        Column(Modifier.selectableGroup()) {
            GroupCard(Languages.map { (code, names) ->
                @Composable {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .selectable(selected = selected == code, role = Role.RadioButton) { selected = code }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(names.first, style = OrhanTextStyles.rowTitle, color = MaterialTheme.colorScheme.onSurface)
                            Text(names.second, style = OrhanTextStyles.snippet, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        RadioButton(selected = selected == code, onClick = null)
                    }
                }
            })
        }
        SectionLabel("Agent replies")
        GroupCard(listOf(
            { SwitchRow("Always reply in app language", "Agents answer in your selected language", alwaysAppLang, { alwaysAppLang = it }) },
        ))
    }
}
