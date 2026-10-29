package ai.orhan.feature.settings.detail

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/* Chat preferences persistence — mirrors ProfilePreferences / ThemePreferences. */

private const val CHAT_PREFS_DATASTORE_FILE = "orhan_chat_prefs.pb"
private const val CHAT_PREFS_FORMAT_VERSION = 1

data class ChatPrefs(
    val sendWithEnter: Boolean = true,
    val holdToRecord: Boolean = true,
    val previewBeforeSending: Boolean = false,
    val typingIndicators: Boolean = true,
    val autoPlayMedia: Boolean = false,
    val agentActivityCards: Boolean = true,
)

object ChatPrefsSerializer : Serializer<ChatPrefs> {
    override val defaultValue: ChatPrefs = ChatPrefs()

    override suspend fun readFrom(input: InputStream): ChatPrefs {
        val bytes = try {
            input.readBytes()
        } catch (e: IOException) {
            throw CorruptionException("Cannot read chat prefs", e)
        }
        if (bytes.isEmpty()) return defaultValue
        return try {
            DataInputStream(bytes.inputStream()).use { d ->
                if (d.readInt() != CHAT_PREFS_FORMAT_VERSION) return defaultValue
                ChatPrefs(
                    sendWithEnter = d.readBoolean(),
                    holdToRecord = d.readBoolean(),
                    previewBeforeSending = d.readBoolean(),
                    typingIndicators = d.readBoolean(),
                    autoPlayMedia = d.readBoolean(),
                    agentActivityCards = d.readBoolean(),
                )
            }
        } catch (e: IOException) {
            defaultValue
        }
    }

    override suspend fun writeTo(t: ChatPrefs, output: OutputStream) {
        val d = DataOutputStream(output)
        d.writeInt(CHAT_PREFS_FORMAT_VERSION)
        d.writeBoolean(t.sendWithEnter)
        d.writeBoolean(t.holdToRecord)
        d.writeBoolean(t.previewBeforeSending)
        d.writeBoolean(t.typingIndicators)
        d.writeBoolean(t.autoPlayMedia)
        d.writeBoolean(t.agentActivityCards)
        d.flush()
    }
}

val Context.chatPrefsDataStore: DataStore<ChatPrefs> by dataStore(
    fileName = CHAT_PREFS_DATASTORE_FILE,
    serializer = ChatPrefsSerializer,
)

class ChatPreferences(private val dataStore: DataStore<ChatPrefs>) {
    val prefs: Flow<ChatPrefs> = dataStore.data
        .catch { error -> if (error is IOException) emit(ChatPrefs()) else throw error }

    suspend fun update(transform: (ChatPrefs) -> ChatPrefs) {
        dataStore.updateData(transform)
    }
}

fun Context.chatPreferences(): ChatPreferences = ChatPreferences(chatPrefsDataStore)
