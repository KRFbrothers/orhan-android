package ai.orhan.feature.memory

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ai.orhan.data.memory.MEMORY_MAX_CHARS
import ai.orhan.data.memory.memoryStore
import ai.orhan.feature.settings.detail.FootNote
import ai.orhan.feature.settings.detail.SectionLabel
import ai.orhan.feature.settings.detail.SettingsDetailScaffold
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Personal memory editor: shared context injected into every agent chat. */
@Composable
fun MemoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { context.memoryStore() }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var text by rememberSaveable { mutableStateOf("") }
    var loaded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(store) {
        if (!loaded) { text = store.memory.first(); loaded = true }
    }

    SettingsDetailScaffold(title = "Memory", onBack = onBack, snackbarHostState = snackbar) {
        SectionLabel("What should your agents know?")
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.take(MEMORY_MAX_CHARS) },
            placeholder = { Text("e.g. I'm a student in Delhi, prefer short answers, vegetarian…") },
            supportingText = { Text("${text.length} / $MEMORY_MAX_CHARS") },
            minLines = 6,
            modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp),
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                scope.launch {
                    store.set(text)
                    snackbar.showSnackbar("Memory saved")
                }
            },
            enabled = loaded,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save") }
        FootNote("Memory stays on this device. It's sent along with your chats to the engine so agents can use it.")
    }
}
