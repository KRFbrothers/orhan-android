package ai.orhan.app

import ai.orhan.data.applock.AppLockStore
import ai.orhan.data.applock.PinAttempt
import ai.orhan.data.applock.PinHash
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Numeric, masked PIN input (max 6 digits). Shared by the lock screen and Privacy dialogs. */
@Composable
fun PinField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    onDone: () -> Unit = {},
) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onValueChange(v.filter { it in '0'..'9' }.take(PinHash.MAX_LEN)) },
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = modifier,
    )
}

/**
 * Full-screen lock. Covers the app until the correct PIN is entered.
 * Back exits the app ([onBackExit]) — it never reveals the content underneath.
 */
@Composable
fun AppLockScreen(store: AppLockStore, onUnlocked: () -> Unit, onBackExit: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    var lockoutMs by remember { mutableStateOf(0L) }
    val lockedOut = lockoutMs > 0

    BackHandler(onBack = onBackExit)

    fun submit() {
        if (checking || lockedOut) return
        if (!PinHash.isValidPin(pin)) { error = "Enter your 4–6 digit PIN"; return }
        checking = true
        val attempt = pin
        scope.launch {
            val result = store.attempt(attempt)
            checking = false
            pin = ""
            when (result) {
                PinAttempt.Success -> onUnlocked()
                is PinAttempt.Wrong -> error = "Wrong PIN. ${result.attemptsLeft} attempt(s) left."
                is PinAttempt.LockedOut -> { lockoutMs = result.remainingMs; error = null }
            }
        }
    }

    LaunchedEffect(Unit) {
        lockoutMs = store.lockoutRemainingMs()
        runCatching { focus.requestFocus() }
    }
    // Count down while locked out; refresh from the persisted deadline each tick.
    LaunchedEffect(lockedOut) {
        while (lockoutMs > 0) {
            delay(1_000)
            lockoutMs = store.lockoutRemainingMs()
        }
    }

    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding().imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Orhan is locked", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(8.dp))
            Text("Enter your PIN to continue", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            PinField(
                value = pin,
                onValueChange = {
                    pin = it; error = null
                    if (it.length == PinHash.MAX_LEN) submit()
                },
                label = "PIN",
                isError = error != null,
                onDone = ::submit,
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
            if (lockedOut) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Too many wrong PINs. Try again in ${(lockoutMs + 999) / 1000}s.",
                    color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                )
            } else if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = ::submit, enabled = !checking && !lockedOut, modifier = Modifier.fillMaxWidth()) { Text("Unlock") }
        }
    }
}
