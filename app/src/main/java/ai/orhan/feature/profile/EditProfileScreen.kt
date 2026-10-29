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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ai.orhan.design.theme.OrhanTextStyles
import ai.orhan.feature.settings.detail.FootNote
import ai.orhan.feature.settings.detail.SectionLabel
import ai.orhan.feature.settings.detail.SettingsDetailScaffold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Avatars = ProfileAvatars

/** Edit profile (kit panel p-editprofile). Loads from and saves to [ProfilePreferences] (DataStore). */
@Composable
fun EditProfileScreen(onBack: () -> Unit, onSaved: () -> Unit = onBack) {
    val context = LocalContext.current.applicationContext
    val preferences = remember(context) { context.profilePreferences() }
    val stored by preferences.profile.collectAsStateWithLifecycle(initialValue = null)
    var loaded by rememberSaveable { mutableStateOf(false) }
    var avatar by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var handle by rememberSaveable { mutableStateOf("") }
    var bio by rememberSaveable { mutableStateOf("") }
    var nameError by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Seed the form once from disk; later emissions must not clobber in-progress edits.
    LaunchedEffect(stored) {
        val p = stored
        if (p != null && !loaded) {
            avatar = Avatars.indexOf(p.safeAvatarResId).coerceAtLeast(0)
            name = p.name; email = p.email; handle = p.handle; bio = p.bio
            loaded = true
        }
    }

    val save: () -> Unit = {
        if (name.isBlank()) {
            nameError = true
        } else {
            scope.launch {
                val ok = runCatching {
                    preferences.setProfile(
                        UserProfile(
                            name = name.trim(),
                            email = email.trim(),
                            handle = handle.trim(),
                            bio = bio,
                            avatarResId = Avatars[avatar],
                        ),
                    )
                }.isSuccess
                if (!ok) {
                    snackbar.showSnackbar("Could not save profile")
                    return@launch
                }
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
            supportingText = { Text(if (nameError) "Name is required" else "Orhan uses this name when it talks to you.") },
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
        // Public handle removed: Orhan is an agent app, not a social network. Stored value is kept as-is.
        SectionLabel("About you")
        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            minLines = 3,
            modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
        )
        FootNote("Saved on this device.")
        Spacer(Modifier.height(24.dp))
        Button(onClick = save, enabled = loaded, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Save changes") }
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
