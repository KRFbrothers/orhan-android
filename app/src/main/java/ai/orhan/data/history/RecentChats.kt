package ai.orhan.data.history

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

/*
 * Local chat history stub: most-recent-first chat titles in DataStore.
 * Dua API has no chat-history list endpoint yet; swap this source when it does.
 */
private const val FORMAT_VERSION = 1
const val MAX_RECENT_CHATS = 20

data class RecentChat(val title: String, val openedAt: Long)

object RecentChatsSerializer : Serializer<List<RecentChat>> {
    override val defaultValue: List<RecentChat> = emptyList()

    override suspend fun readFrom(input: InputStream): List<RecentChat> {
        val bytes = try { input.readBytes() } catch (e: IOException) { throw CorruptionException("read", e) }
        if (bytes.isEmpty()) return defaultValue
        return try {
            DataInputStream(bytes.inputStream()).use { d ->
                if (d.readInt() != FORMAT_VERSION) return defaultValue
                List(d.readInt()) { RecentChat(d.readUTF(), d.readLong()) }
            }
        } catch (e: IOException) { defaultValue }
    }

    override suspend fun writeTo(t: List<RecentChat>, output: OutputStream) {
        val d = DataOutputStream(output)
        d.writeInt(FORMAT_VERSION)
        d.writeInt(t.size)
        t.forEach { d.writeUTF(it.title); d.writeLong(it.openedAt) }
        d.flush()
    }
}

val Context.recentChatsDataStore: DataStore<List<RecentChat>> by dataStore(
    fileName = "orhan_recent_chats.pb",
    serializer = RecentChatsSerializer,
)

/** Pure helper: move/insert [title] to the front, dedupe, cap size. */
fun List<RecentChat>.withOpened(title: String, now: Long): List<RecentChat> =
    (listOf(RecentChat(title, now)) + filterNot { it.title == title }).take(MAX_RECENT_CHATS)

class RecentChatsRepository(private val dataStore: DataStore<List<RecentChat>>) {
    val recent: Flow<List<RecentChat>> = dataStore.data
        .catch { if (it is IOException) emit(emptyList()) else throw it }

    suspend fun recordOpened(title: String, now: Long = System.currentTimeMillis()) {
        if (title.isBlank()) return
        dataStore.updateData { it.withOpened(title.trim(), now) }
    }
}

fun Context.recentChats(): RecentChatsRepository = RecentChatsRepository(recentChatsDataStore)
