package ai.orhan.feature.profile

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ai.orhan.R
import ai.orhan.design.theme.OrhanTextStyles
import ai.orhan.feature.settings.detail.FootNote
import ai.orhan.feature.settings.detail.SectionLabel
import ai.orhan.feature.settings.detail.SettingsDetailScaffold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Avatars = listOf(R.drawable.user_alex, R.drawable.user_alice, R.drawable.user_david)

/** Edit profile (kit panel p-editprofile). Local state only — nothing is persisted yet. */
@Composable
fun EditProfileScreen(onBack: () -> Unit, onSaved: () -> Unit = onBack) {
    var avatar by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("Fareed Khan") }
    var email by rememberSaveable { mutableStateOf("fareed.khan@gmail.com") }
    var handle by rememberSaveable { mutableStateOf("@fareed") }
    var bio by rememberSaveable { mutableStateOf("Building AI agents at Orhan. Mostly on Android, always in dark mode.") }
    var nameError by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val save: () -> Unit = {
        if (name.isBlank()) {
            nameError = true
        } else {
            scope.launch {
                launch { snackbar.showSnackbar("Profile saved") }
                delay(700)
                onSaved()
            }
        }
    }

    SettingsDetailScaffold(title = "Edit profile", onBack = onBack, snackbarHostState = snackbar) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            AvatarImage(Avatars[avatar], 72)
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Profile photo", style = OrhanTextStyles.rowTitle, color = MaterialTheme.colorScheme.onSurface)
                Text("Tap a face below", style = OrhanTextStyles.snippet, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Avatars.forEachIndexed { i, res ->
                        val on = i == avatar
                        AvatarImage(
                            res, 40,
                            Modifier
                                .border(2.dp, if (on) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                                .clickable { avatar = i },
                        )
                    }
                }
            }
        }

        SectionLabel("Full name")
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; if (it.isNotBlank()) nameError = false },
            singleLine = true,
            isError = nameError,
            supportingText = { Text(if (nameError) "Name is required" else "Shown to people and agents you chat with.") },
            modifier = Modifier.fillMaxWidth(),
        )
        SectionLabel("Email")
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            supportingText = { Text("Used for sign-in and receipts. We'll send a code to confirm a change.") },
            modifier = Modifier.fillMaxWidth(),
        )
        SectionLabel("Orhan ID")
        OutlinedTextField(
            value = handle,
            onValueChange = { handle = it },
            singleLine = true,
            supportingText = { Text("Your handle is unique — 30 days between changes.") },
            modifier = Modifier.fillMaxWidth(),
        )
        SectionLabel("About you")
        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            minLines = 3,
            modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
        )
        FootNote("Changes stay on this device for now.")
        Spacer(Modifier.height(24.dp))
        Button(onClick = save, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Save changes") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Cancel") }
    }
}

@Composable
private fun AvatarImage(@DrawableRes res: Int, sizeDp: Int, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(res),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.size(sizeDp.dp).clip(CircleShape),
    )
}
