package ai.orhan.data.memory

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/* Local notes and to-dos for the Memory screen. On-device only; never sent to the engine. */
private const val ENTRIES_FORMAT_VERSION = 1
const val MAX_NOTE_CONTENT = 8_000
const val MAX_TODO_CONTENT = 2_000
const val MAX_ENTRY_TITLE = 200
const val MAX_ENTRIES = 200

data class MemoryEntry(
    val id: String,
    val title: String,
    val content: String,
    val done: Boolean,
    val createdAt: Long,
)

data class MemoryEntries(
    val notes: List<MemoryEntry> = emptyList(),
    val todos: List<MemoryEntry> = emptyList(),
)

object MemoryEntriesSerializer : Serializer<MemoryEntries> {
    override val defaultValue = MemoryEntries()

    override suspend fun readFrom(input: InputStream): MemoryEntries {
        val bytes = try { input.readBytes() } catch (e: IOException) { throw CorruptionException("read", e) }
        if (bytes.isEmpty()) return defaultValue
        return try {
            DataInputStream(bytes.inputStream()).use { d ->
                if (d.readInt() != ENTRIES_FORMAT_VERSION) return defaultValue
                MemoryEntries(notes = d.readList(), todos = d.readList())
            }
        } catch (e: IOException) { defaultValue }
    }

    override suspend fun writeTo(t: MemoryEntries, output: OutputStream) {
        val d = DataOutputStream(output)
        d.writeInt(ENTRIES_FORMAT_VERSION)
        d.writeList(t.notes); d.writeList(t.todos)
        d.flush()
    }
}

private fun DataInputStream.readList(): List<MemoryEntry> {
    val n = readInt()
    if (n < 0 || n > MAX_ENTRIES * 2) throw IOException("bad count")
    return List(n) {
        MemoryEntry(readUTF(), readUTF(), readStr(), readBoolean(), readLong())
    }
}

private fun DataOutputStream.writeList(list: List<MemoryEntry>) {
    writeInt(list.size)
    list.forEach { e ->
        writeUTF(e.id); writeUTF(e.title.take(MAX_ENTRY_TITLE))
        val b = e.content.toByteArray(Charsets.UTF_8); writeInt(b.size); write(b)
        writeBoolean(e.done); writeLong(e.createdAt)
    }
}

private fun DataInputStream.readStr(): String {
    val n = readInt()
    if (n < 0 || n > MAX_NOTE_CONTENT * 4) throw IOException("bad length")
    val b = ByteArray(n); readFully(b)
    return String(b, Charsets.UTF_8)
}

private val Context.memoryEntriesDataStore: DataStore<MemoryEntries> by dataStore(
    fileName = "orhan_memory_entries.pb",
    serializer = MemoryEntriesSerializer,
)

private fun List<MemoryEntry>.upsertedEntry(e: MemoryEntry): List<MemoryEntry> =
    if (any { it.id == e.id }) map { if (it.id == e.id) e else it }
    else (listOf(e) + this).take(MAX_ENTRIES)

class MemoryEntriesStore(private val dataStore: DataStore<MemoryEntries>) {
    private val data = dataStore.data.catch { if (it is IOException) emit(MemoryEntries()) else throw it }

    val notes: Flow<List<MemoryEntry>> = data.map { d -> d.notes.sortedByDescending { it.createdAt } }
    val todos: Flow<List<MemoryEntry>> = data.map { d -> d.todos.sortedByDescending { it.createdAt } }

    /** Upsert a note; title required. Returns false if rejected. */
    suspend fun upsertNote(id: String?, title: String, content: String): Boolean {
        val t = title.trim().take(MAX_ENTRY_TITLE)
        if (t.isBlank()) return false
        dataStore.updateData { d ->
            val old = d.notes.firstOrNull { it.id == id }
            val e = MemoryEntry(old?.id ?: UUID.randomUUID().toString(), t, content.trim().take(MAX_NOTE_CONTENT), false, old?.createdAt ?: System.currentTimeMillis())
            d.copy(notes = d.notes.upsertedEntry(e))
        }
        return true
    }

    suspend fun upsertTodo(id: String?, title: String, content: String = ""): Boolean {
        val t = title.trim().take(MAX_ENTRY_TITLE)
        if (t.isBlank()) return false
        dataStore.updateData { d ->
            val old = d.todos.firstOrNull { it.id == id }
            val e = MemoryEntry(old?.id ?: UUID.randomUUID().toString(), t, content.trim().take(MAX_TODO_CONTENT), old?.done ?: false, old?.createdAt ?: System.currentTimeMillis())
            d.copy(todos = d.todos.upsertedEntry(e))
        }
        return true
    }

    suspend fun toggleDone(id: String) {
        dataStore.updateData { d -> d.copy(todos = d.todos.map { if (it.id == id) it.copy(done = !it.done) else it }) }
    }

    suspend fun deleteNote(id: String) {
        dataStore.updateData { d -> d.copy(notes = d.notes.filterNot { it.id == id }) }
    }

    suspend fun deleteTodo(id: String) {
        dataStore.updateData { d -> d.copy(todos = d.todos.filterNot { it.id == id }) }
    }
}

fun Context.memoryEntriesStore(): MemoryEntriesStore = MemoryEntriesStore(applicationContext.memoryEntriesDataStore)
