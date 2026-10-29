package ai.orhan.feature.settings.detail

import ai.orhan.app.PinField
import ai.orhan.data.applock.AppLockStore
import ai.orhan.data.applock.PinHash
import ai.orhan.data.applock.appLockStore
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val context = LocalContext.current.applicationContext
    val lockStore = remember(context) { context.appLockStore() }
    val lockEnabled by lockStore.isEnabled.collectAsStateWithLifecycle(initialValue = false)
    var pinDialog by remember { mutableStateOf<PinDialog?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    var clearing by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()


    SettingsDetailScaffold(title = "Privacy", onBack = onBack, snackbarHostState = snackbar) {
        SectionLabel("App lock")
        val lockRows = buildList<@Composable () -> Unit> {
            add {
                SwitchRow(
                    title = "Require PIN when opening Orhan",
                    subtitle = "Asks for a 4–6 digit PIN on open and when you come back",
                    checked = lockEnabled,
                    onCheckedChange = { on -> pinDialog = if (on) PinDialog.Set else PinDialog.TurnOff },
                )
            }
            if (lockEnabled) {
                add { NavRow("Change PIN", onClick = { pinDialog = PinDialog.Change }) }
                add { NavRow("Turn off App lock", destructive = true, showChevron = false, onClick = { pinDialog = PinDialog.TurnOff }) }
            }
        }
        GroupCard(lockRows)

        SectionLabel("Data")
        GroupCard(listOf(
            { NavRow("Clear chat history", destructive = true, showChevron = false, onClick = { if (!clearing) confirmClear = true }) },
        ))
    }

    pinDialog?.let { mode ->
        AppLockPinDialog(
            mode = mode,
            store = lockStore,
            onDismiss = { pinDialog = null },
            onDone = { msg -> pinDialog = null; scope.launch { snackbar.showSnackbar(msg) } },
        )
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

private enum class PinDialog { Set, Change, TurnOff }

/** One dialog for set / change / turn off. Cancel never changes anything. */
@Composable
private fun AppLockPinDialog(mode: PinDialog, store: AppLockStore, onDismiss: () -> Unit, onDone: (String) -> Unit) {
    var current by remember { mutableStateOf("") }
    var new1 by remember { mutableStateOf("") }
    var new2 by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val needsCurrent = mode != PinDialog.Set
    val needsNew = mode != PinDialog.TurnOff

    fun submit() {
        if (busy) return
        if (needsCurrent && !PinHash.isValidPin(current)) { error = "Enter your current PIN"; return }
        if (needsNew) {
            if (!PinHash.isValidPin(new1)) { error = "PIN must be 4–6 digits"; return }
            if (new1 != new2) { error = "PINs don't match"; return }
        }
        busy = true
        scope.launch {
            try {
                when (mode) {
                    PinDialog.Set -> { store.setPin(new1); onDone("App lock is on") }
                    PinDialog.Change ->
                        if (store.changePin(current, new1)) onDone("PIN changed") else { error = "Current PIN is wrong"; current = "" }
                    PinDialog.TurnOff ->
                        if (store.verify(current)) { store.disable(); onDone("App lock is off") } else { error = "Current PIN is wrong"; current = "" }
                }
            } finally {
                busy = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (mode) {
                    PinDialog.Set -> "Set a PIN"
                    PinDialog.Change -> "Change PIN"
                    PinDialog.TurnOff -> "Turn off App lock?"
                },
            )
        },
        text = {
            Column {
                if (mode == PinDialog.TurnOff) {
                    Text("Enter your current PIN. Orhan will open without a PIN after this.")
                    Spacer(Modifier.height(12.dp))
                }
                if (needsCurrent) {
                    PinField(current, { current = it; error = null }, "Current PIN", Modifier.fillMaxWidth(), isError = error != null, onDone = ::submit)
                }
                if (needsNew) {
                    if (needsCurrent) Spacer(Modifier.height(8.dp))
                    PinField(new1, { new1 = it; error = null }, "New PIN (4–6 digits)", Modifier.fillMaxWidth(), isError = error != null)
                    Spacer(Modifier.height(8.dp))
                    PinField(new2, { new2 = it; error = null }, "Confirm new PIN", Modifier.fillMaxWidth(), isError = error != null, onDone = ::submit)
                }
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = ::submit, enabled = !busy) {
                Text(if (mode == PinDialog.TurnOff) "Turn off" else "Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
