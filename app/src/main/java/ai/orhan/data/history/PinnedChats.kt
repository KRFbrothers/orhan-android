package ai.orhan.data.history

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/* Local pinned chat titles (newest pin first), kept separate from recent-chats v1 format. */
private const val PINNED_FORMAT_VERSION = 1
const val MAX_PINNED_CHATS = 50

object PinnedChatsSerializer : Serializer<List<String>> {
    override val defaultValue: List<String> = emptyList()

    override suspend fun readFrom(input: InputStream): List<String> {
        val bytes = try { input.readBytes() } catch (e: IOException) { throw CorruptionException("read", e) }
        if (bytes.isEmpty()) return defaultValue
        return try {
            DataInputStream(bytes.inputStream()).use { d ->
                if (d.readInt() != PINNED_FORMAT_VERSION) return defaultValue
                List(d.readInt()) { d.readUTF() }
            }
        } catch (e: IOException) { defaultValue }
    }

    override suspend fun writeTo(t: List<String>, output: OutputStream) {
        val d = DataOutputStream(output)
        d.writeInt(PINNED_FORMAT_VERSION)
        d.writeInt(t.size)
        t.forEach { d.writeUTF(it) }
        d.flush()
    }
}

val Context.pinnedChatsDataStore: DataStore<List<String>> by dataStore(
    fileName = "orhan_pinned_chats.pb",
    serializer = PinnedChatsSerializer,
)

/** Pure helper: unpin if present, else pin at front; capped. */
fun List<String>.withToggled(title: String): List<String> =
    if (title in this) filterNot { it == title } else (listOf(title) + this).take(MAX_PINNED_CHATS)

class PinnedChatsRepository(private val dataStore: DataStore<List<String>>) {
    val pinned: Flow<Set<String>> = dataStore.data
        .catch { if (it is IOException) emit(emptyList()) else throw it }
        .map { it.toCollection(LinkedHashSet()) }

    suspend fun toggle(title: String) {
        val t = title.trim()
        if (t.isEmpty()) return
        dataStore.updateData { it.withToggled(t) }
    }

    suspend fun unpin(title: String) {
        val t = title.trim()
        dataStore.updateData { list -> list.filterNot { it == t } }
    }

    suspend fun clear() {
        dataStore.updateData { emptyList() }
    }

    suspend fun isPinned(title: String): Boolean = title.trim() in pinned.first()
}

fun Context.pinnedChats(): PinnedChatsRepository = PinnedChatsRepository(pinnedChatsDataStore)
