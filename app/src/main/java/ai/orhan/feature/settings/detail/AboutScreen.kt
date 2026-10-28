package ai.orhan.feature.settings.detail

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import ai.orhan.BuildConfig
import kotlinx.coroutines.launch

/** Settings → About. Links open placeholder https pages. */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val toast: (String) -> Unit = { msg -> scope.launch { snackbar.showSnackbar(msg) } }
    val open: (String) -> Unit = { url ->
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            toast("No browser available")
        }
    }

    SettingsDetailScaffold(title = "About", onBack = onBack, snackbarHostState = snackbar) {
        SectionLabel("Orhan")
        GroupCard(listOf(
            { NavRow("Version", value = BuildConfig.VERSION_NAME) },
            { NavRow("Check for updates", onClick = { toast("You're on the latest version") }) },
        ))
        FootNote("Orhan brings your chats and AI agents together in one place.")
        SectionLabel("Legal")
        GroupCard(listOf(
            { NavRow("Terms of service", onClick = { open("https://orhan.ai/terms") }) },
            { NavRow("Privacy policy", onClick = { open("https://orhan.ai/privacy") }) },
            { NavRow("Open-source licenses", onClick = { toast("Coming soon") }) },
        ))
        SectionLabel("Help")
        GroupCard(listOf(
            { NavRow("Contact support", onClick = { open("mailto:support@orhan.ai") }) },
        ))
    }
}
