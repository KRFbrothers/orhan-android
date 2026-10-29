package ai.orhan.data.history

import ai.orhan.design.screens.ChatMessage
import ai.orhan.design.screens.ChatRole
import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/*
 * Local per-chat message threads keyed by chat title (same string as AppRoutes.chat(title)).
 * Device-only; no cloud sync.
 */
private const val THREAD_FORMAT_VERSION = 1
const val MAX_THREAD_MESSAGES = 100
const val MAX_THREADS = 40

data class StoredMessage(val id: Long, val role: String, val text: String, val time: String)
data class ChatThread(val title: String, val updatedAt: Long, val messages: List<StoredMessage>)

object ChatThreadsSerializer : Serializer<List<ChatThread>> {
    override val defaultValue: List<ChatThread> = emptyList()

    override suspend fun readFrom(input: InputStream): List<ChatThread> {
        val bytes = try { input.readBytes() } catch (e: IOException) { throw CorruptionException("read", e) }
        if (bytes.isEmpty()) return defaultValue
        return try {
            DataInputStream(bytes.inputStream()).use { d ->
                if (d.readInt() != THREAD_FORMAT_VERSION) return defaultValue
                List(d.readInt()) {
                    val title = d.readUTF()
                    val updatedAt = d.readLong()
                    val msgs = List(d.readInt()) { StoredMessage(d.readLong(), d.readUTF(), readLongString(d), d.readUTF()) }
                    ChatThread(title, updatedAt, msgs)
                }
            }
        } catch (e: IOException) { defaultValue }
    }

    override suspend fun writeTo(t: List<ChatThread>, output: OutputStream) {
        val d = DataOutputStream(output)
        d.writeInt(THREAD_FORMAT_VERSION)
        d.writeInt(t.size)
        t.forEach { th ->
            d.writeUTF(th.title); d.writeLong(th.updatedAt); d.writeInt(th.messages.size)
            th.messages.forEach { m -> d.writeLong(m.id); d.writeUTF(m.role); writeLongString(d, m.text); d.writeUTF(m.time) }
        }
        d.flush()
    }

    // writeUTF caps at 64KB; agent replies can be longer.
    private fun writeLongString(d: DataOutputStream, s: String) {
        val b = s.toByteArray(Charsets.UTF_8); d.writeInt(b.size); d.write(b)
    }
    private fun readLongString(d: DataInputStream): String {
        val b = ByteArray(d.readInt()); d.readFully(b); return String(b, Charsets.UTF_8)
    }
}

val Context.chatThreadsDataStore: DataStore<List<ChatThread>> by dataStore(
    fileName = "orhan_chat_threads.pb",
    serializer = ChatThreadsSerializer,
)

internal fun ChatMessage.toStored() =
    StoredMessage(id, if (role == ChatRole.User) "user" else "agent", text, time)

internal fun StoredMessage.toUi() =
    ChatMessage(id, if (role == "user") ChatRole.User else ChatRole.Agent, text, time)

/** Pure helper: upsert a thread, cap messages and thread count (drop oldest by updatedAt). */
fun List<ChatThread>.withSaved(title: String, messages: List<ChatMessage>, now: Long): List<ChatThread> {
    val rest = filterNot { it.title == title }
    if (messages.isEmpty()) return rest
    val th = ChatThread(title, now, messages.takeLast(MAX_THREAD_MESSAGES).map { it.toStored() })
    return (rest + th).sortedByDescending { it.updatedAt }.take(MAX_THREADS)
}

class ChatThreadStore(private val dataStore: DataStore<List<ChatThread>>) {
    val threads: Flow<List<ChatThread>> = dataStore.data
        .catch { if (it is IOException) emit(emptyList()) else throw it }

    suspend fun load(title: String): List<ChatMessage> =
        threads.first().firstOrNull { it.title == title }?.messages?.map { it.toUi() }.orEmpty()

    suspend fun save(title: String, messages: List<ChatMessage>, now: Long = System.currentTimeMillis()) {
        if (title.isBlank()) return
        dataStore.updateData { it.withSaved(title, messages, now) }
    }

    suspend fun remove(title: String) {
        dataStore.updateData { list -> list.filterNot { it.title == title } }
    }

    suspend fun clear() {
        dataStore.updateData { emptyList() }
    }
}

fun Context.chatThreads(): ChatThreadStore = ChatThreadStore(chatThreadsDataStore)
