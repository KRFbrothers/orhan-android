package ai.orhan.feature.profile

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import ai.orhan.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/* ============================================================================
 *  Orhan AI — Profile persistence (mirrors ThemePreferences)
 *
 *  One DataStore file holding the Edit Profile fields. Stored as a versioned
 *  sequence of DataOutputStream fields (no serialization plugin needed).
 * ==========================================================================*/

private const val PROFILE_DATASTORE_FILE = "orhan_profile.pb"
private const val PROFILE_FORMAT_VERSION = 1

/** Avatars the user may pick from. Persisted avatar ids outside this list fall back to the first. */
val ProfileAvatars: List<Int> = listOf(R.drawable.user_alex, R.drawable.user_alice, R.drawable.user_david)

data class UserProfile(
    val name: String = "Fareed Khan",
    val email: String = "fareed.khan@gmail.com",
    val handle: String = "@fareed",
    val bio: String = "Building AI agents at Orhan. Mostly on Android, always in dark mode.",
    @DrawableRes val avatarResId: Int = ProfileAvatars.first(),
) {
    /** Avatar guaranteed to be a current drawable (res ids can shift between builds). */
    @get:DrawableRes
    val safeAvatarResId: Int get() = avatarResId.takeIf { it in ProfileAvatars } ?: ProfileAvatars.first()

    val initials: String
        get() = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2)
            .joinToString("") { it.first().uppercase() }.ifEmpty { "?" }
}

object UserProfileSerializer : Serializer<UserProfile> {
    override val defaultValue: UserProfile = UserProfile()

    override suspend fun readFrom(input: InputStream): UserProfile {
        val bytes = try {
            input.readBytes()
        } catch (e: IOException) {
            throw CorruptionException("Cannot read profile", e)
        }
        if (bytes.isEmpty()) return defaultValue
        return try {
            DataInputStream(bytes.inputStream()).use { d ->
                if (d.readInt() != PROFILE_FORMAT_VERSION) return defaultValue
                UserProfile(
                    name = d.readUTF(),
                    email = d.readUTF(),
                    handle = d.readUTF(),
                    bio = d.readUTF(),
                    avatarResId = d.readInt(),
                )
            }
        } catch (e: IOException) {
            defaultValue
        }
    }

    override suspend fun writeTo(t: UserProfile, output: OutputStream) {
        val d = DataOutputStream(output)
        d.writeInt(PROFILE_FORMAT_VERSION)
        d.writeUTF(t.name)
        d.writeUTF(t.email)
        d.writeUTF(t.handle)
        d.writeUTF(t.bio)
        d.writeInt(t.avatarResId)
        d.flush()
    }
}

/** Single DataStore instance for the process — always reach it through this extension. */
val Context.profileDataStore: DataStore<UserProfile> by dataStore(
    fileName = PROFILE_DATASTORE_FILE,
    serializer = UserProfileSerializer,
)

class ProfilePreferences(
    private val dataStore: DataStore<UserProfile>,
) {
    /** The persisted profile; disk read errors degrade to the default. */
    val profile: Flow<UserProfile> = dataStore.data
        .catch { error -> if (error is IOException) emit(UserProfile()) else throw error }

    suspend fun setProfile(profile: UserProfile) {
        dataStore.updateData { profile }
    }
}

fun Context.profilePreferences(): ProfilePreferences = ProfilePreferences(profileDataStore)
