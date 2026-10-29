package ai.orhan.feature.settings.detail

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch

/** Settings → Privacy & security. Local state only. */
@Composable
fun PrivacyScreen(
    onBack: () -> Unit,
    /** Clears local recent + pinned chats and saved message threads. Returns once the stores are updated. */
    onClearChatHistory: suspend () -> Unit = {},
) {
    var confirmClear by remember { mutableStateOf(false) }
    var clearing by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val toast: (String) -> Unit = { msg -> scope.launch { snackbar.showSnackbar(msg) } }

    var appLock by rememberSaveable { mutableStateOf(false) }
    var readReceipts by rememberSaveable { mutableStateOf(true) }
    var shareHistory by rememberSaveable { mutableStateOf(false) }
    var improve by rememberSaveable { mutableStateOf(true) }
    val blocked = remember { mutableStateListOf("David Park", "Unknown contact") }

    SettingsDetailScaffold(title = "Privacy", onBack = onBack, snackbarHostState = snackbar) {
        SectionLabel("Security")
        GroupCard(listOf(
            { SwitchRow("App lock", "Require passcode or biometrics", appLock, { appLock = it }) },
            { NavRow("Change passcode", onClick = { toast("Coming soon") }) },
            { NavRow("Two-step verification", value = "Off", onClick = { toast("Coming soon") }) },
        ))
        SectionLabel("Chats")
        GroupCard(listOf(
            { SwitchRow("Read receipts", checked = readReceipts, onCheckedChange = { readReceipts = it }) },
            { SwitchRow("Share chat history with agents", checked = shareHistory, onCheckedChange = { shareHistory = it }) },
            { SwitchRow("Help improve Orhan", "Send anonymous usage data", improve, { improve = it }) },
        ))
        SectionLabel("Blocked users")
        if (blocked.isEmpty()) {
            FootNote("No blocked users.")
        } else {
            GroupCard(blocked.toList().map { name ->
                @Composable {
                    NavRow(name, trailing = {
                        OutlinedButton(onClick = {
                            blocked.remove(name)
                            toast("Unblocked $name")
                        }) { Text("Unblock") }
                    })
                }
            })
        }
        SectionLabel("Data")
        GroupCard(listOf(
            { NavRow("Download my data", onClick = { toast("We'll email you when your export is ready") }) },
            { NavRow("Clear chat history", destructive = true, showChevron = false, onClick = { if (!clearing) confirmClear = true }) },
        ))
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear chat history?") },
            text = { Text("This removes your recent and pinned chats and their saved messages from this device. Agents, memory and artifacts are kept.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    clearing = true
                    scope.launch {
                        try {
                            onClearChatHistory()
                            snackbar.showSnackbar("Chat history cleared")
                        } catch (e: Exception) {
                            snackbar.showSnackbar("Couldn't clear chat history")
                        } finally {
                            clearing = false
                        }
                    }
                }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}
