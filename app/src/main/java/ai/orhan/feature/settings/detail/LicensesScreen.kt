package ai.orhan.feature.settings.detail

import android.content.Context
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Asset paths, packaged at build time from repo-root THIRD-PARTY-NOTICES.md and licenses/. */
internal object LicenseAssets {
    const val DIR = "oss"
    const val NOTICES = "$DIR/THIRD-PARTY-NOTICES.md"
    const val LICENSES_DIR = "$DIR/licenses"

    /** Returns (title, text) sections; missing files are skipped, never invented. */
    fun load(context: Context): List<Pair<String, String>> {
        val am = context.assets
        fun read(path: String): String? =
            runCatching { am.open(path).bufferedReader().use { it.readText() } }.getOrNull()
        val out = mutableListOf<Pair<String, String>>()
        read(NOTICES)?.let { out += "Third-party notices" to it }
        am.list(LICENSES_DIR).orEmpty().sorted().filter { it.endsWith(".txt") }.forEach { name ->
            read("$LICENSES_DIR/$name")?.let { out += sectionTitle(name) to it }
        }
        return out
    }

    /** "Lato-OFL.txt" -> "Lato — OFL". */
    fun sectionTitle(fileName: String): String =
        fileName.removeSuffix(".txt").replace("-", " — ")
}

/** Settings → About → Open-source licences. Text read offline from bundled assets. */
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var sections by remember { mutableStateOf<List<Pair<String, String>>?>(null) }
    LaunchedEffect(Unit) {
        sections = withContext(Dispatchers.IO) { LicenseAssets.load(context.applicationContext) }
    }
    SettingsDetailScaffold(title = "Open-source licences", onBack = onBack) {
        val list = sections
        when {
            list == null -> FootNote("Loading…")
            list.isEmpty() -> FootNote("License notices are unavailable in this build.")
            else -> list.forEach { (title, text) ->
                SectionLabel(title)
                SelectionContainer {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = if (title == "Third-party notices") null else FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
