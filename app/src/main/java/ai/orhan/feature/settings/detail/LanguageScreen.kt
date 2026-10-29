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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import ai.orhan.design.theme.OrhanTextStyles

/**
 * Settings → Language. Saved on-device. Applied to agent replies in chat (when
 * "Always reply in app language" is on); app UI strings are not localized yet.
 */
@Composable
fun LanguageScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember(context) { context.settingsDetailPreferences() }
    val prefs by repo.language.collectAsStateWithLifecycle(initialValue = LanguagePrefs())
    val scope = rememberCoroutineScope()
    val selected = prefs.code

    SettingsDetailScaffold(title = "Language", onBack = onBack) {
        SectionLabel("App language")
        Column(Modifier.selectableGroup()) {
            GroupCard(SupportedLanguages.map { (code, names) ->
                @Composable {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .selectable(selected = selected == code, role = Role.RadioButton) { scope.launch { repo.setLanguage(code) } }
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
            { SwitchRow("Always reply in app language", "Agents answer in your selected language. App text stays English for now.", prefs.agentsUseAppLanguage, { on -> scope.launch { repo.setAgentsUseAppLanguage(on) } }) },
        ))
    }
}
