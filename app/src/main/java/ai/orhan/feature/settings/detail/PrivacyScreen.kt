package ai.orhan.feature.settings.detail

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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


    SettingsDetailScaffold(title = "Privacy", onBack = onBack, snackbarHostState = snackbar) {
        SectionLabel("Data")
        GroupCard(listOf(
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
