package ai.orhan.data.memory

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

/* Local notes, to-dos and topics. On-device only; never sent to the engine. */
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
    val topics: List<MemoryTopic> = emptyList(),
)

object MemoryEntriesSerializer : Serializer<MemoryEntries> {
    override val defaultValue = MemoryEntries()

    override suspend fun readFrom(input: InputStream): MemoryEntries {
        val bytes = try { input.readBytes() } catch (e: IOException) { throw CorruptionException("read", e) }
        if (bytes.isEmpty()) return defaultValue
        return decodeMemoryEntries(bytes.inputStream())
    }

    override suspend fun writeTo(t: MemoryEntries, output: OutputStream) {
        encodeMemoryEntries(t, output)
    }
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
    val topics: Flow<List<MemoryTopic>> = data.map { d -> d.withNotesMigrated().topics.sortedByDescending { it.updatedAt } }

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

    /**
     * Insert or replace a topic. Returns false when the title is blank or the
     * write fails. A failed write leaves the saved topic unchanged.
     */
    suspend fun upsertTopic(
        id: String?,
        title: String,
        summary: String,
        content: String,
        group: String,
        tags: List<String>,
    ): Boolean {
        if (title.trim().isEmpty()) return false
        return try {
            dataStore.updateData { raw ->
                val current = raw.withNotesMigrated()
                val existing = current.topics.firstOrNull { it.id == id }
                val topic = prepareTopic(
                    existing = existing,
                    title = title,
                    summary = summary,
                    content = content,
                    group = group,
                    tags = tags,
                    now = System.currentTimeMillis(),
                    newId = UUID.randomUUID().toString(),
                ) ?: return@updateData current
                current.copy(topics = current.topics.upsertedTopic(topic))
            }
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Removes a topic. Returns false when the write fails; the saved list is
     * left as it was so the editor can stay open.
     */
    suspend fun deleteTopic(id: String): Boolean {
        return try {
            dataStore.updateData { raw ->
                val current = raw.withNotesMigrated()
                current.copy(topics = current.topics.filterNot { it.id == id })
            }
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Opening a topic clears its unread flag without changing the edited time.
     * Returns false when that write fails; the topic stays unread. An already-read
     * topic does not write.
     */
    suspend fun markTopicRead(id: String): Boolean {
        val current = try {
            dataStore.data.first()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return false
        }
        val topic = current.withNotesMigrated().topics.firstOrNull { it.id == id } ?: return true
        if (!topic.unread) return true
        return try {
            dataStore.updateData { raw ->
                val latest = raw.withNotesMigrated()
                latest.copy(topics = latest.topics.map { if (it.id == id && it.unread) it.copy(unread = false) else it })
            }
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        }
    }
}

private fun List<MemoryTopic>.upsertedTopic(topic: MemoryTopic): List<MemoryTopic> =
    if (any { it.id == topic.id }) map { if (it.id == topic.id) topic else it }
    else (listOf(topic) + this).take(MAX_ENTRIES)

fun Context.memoryEntriesStore(): MemoryEntriesStore = MemoryEntriesStore(applicationContext.memoryEntriesDataStore)
