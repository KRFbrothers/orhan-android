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

/* Local notes, to-dos and topics. Topic text is sent only when the user opts in. */
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
    /** Chat title when the user saved this action from a message. Blank for a typed to-do. */
    val sourceChatTitle: String = "",
    /** Message id inside [sourceChatTitle]. Zero when the action was typed in Next Actions. */
    val sourceMessageId: Long = 0L,
)

enum class ActionSaveResult { Saved, Duplicate, Dismissed, Blank, WriteFailed }

data class MemoryEntries(
    val notes: List<MemoryEntry> = emptyList(),
    val todos: List<MemoryEntry> = emptyList(),
    val topics: List<MemoryTopic> = emptyList(),
    /** Source keys the user deleted, so that chat message is not saved as an action again. */
    val dismissedActionSources: List<String> = emptyList(),
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

    /**
     * Creates or edits a to-do. Saving stores the text only. It does not send a
     * message, call a tool, or run any other action. A blank title is rejected.
     * A title that matches another to-do is rejected and the saved list stays
     * as it was. Editing keeps the original source, done flag, and created time.
     */
    suspend fun saveTodo(id: String?, title: String, content: String = ""): ActionSaveResult {
        val cleanTitle = title.trim().take(MAX_ENTRY_TITLE)
        if (cleanTitle.isBlank()) return ActionSaveResult.Blank
        return try {
            var result = ActionSaveResult.Saved
            dataStore.updateData { current ->
                val existing = if (id.isNullOrBlank()) null else current.todos.firstOrNull { it.id == id }
                if (isDuplicateAction(current.todos, cleanTitle, ignoreId = existing?.id)) {
                    result = ActionSaveResult.Duplicate
                    return@updateData current
                }
                val entry = MemoryEntry(
                    id = existing?.id ?: UUID.randomUUID().toString(),
                    title = cleanTitle,
                    content = content.trim().take(MAX_TODO_CONTENT),
                    done = existing?.done ?: false,
                    createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                    sourceChatTitle = existing?.sourceChatTitle.orEmpty(),
                    sourceMessageId = existing?.sourceMessageId ?: 0L,
                )
                current.copy(todos = current.todos.upsertedEntry(entry))
            }
            result
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            ActionSaveResult.WriteFailed
        }
    }

    /** Completes or reopens a to-do. A failed write leaves [done] unchanged. */
    suspend fun toggleDone(id: String): Boolean {
        return try {
            dataStore.updateData { current ->
                current.copy(todos = current.todos.map { if (it.id == id) it.copy(done = !it.done) else it })
            }
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        }
    }

    suspend fun deleteNote(id: String) {
        dataStore.updateData { d -> d.copy(notes = d.notes.filterNot { it.id == id }) }
    }

    /**
     * Confirmed delete. When the to-do came from a chat message, its source key
     * is remembered only after the write succeeds, so that same message is not
     * saved again. A typed to-do with the same title can still be created later.
     */
    suspend fun deleteTodo(id: String): Boolean {
        return try {
            dataStore.updateData { current ->
                val removed = current.todos.firstOrNull { it.id == id } ?: return@updateData current
                val dismissed = if (removed.sourceChatTitle.isNotBlank() && removed.sourceMessageId != 0L) {
                    rememberDismissed(
                        current.dismissedActionSources,
                        sourceKey(removed.sourceChatTitle, removed.sourceMessageId),
                    )
                } else {
                    current.dismissedActionSources
                }
                current.copy(
                    todos = current.todos.filterNot { it.id == id },
                    dismissedActionSources = dismissed,
                )
            }
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Explicit save of one user message as a to-do. This does not scan older
     * chats and does not run the message. A deleted source is not recreated.
     */
    suspend fun addActionFromChat(text: String, chatTitle: String, messageId: Long): ActionSaveResult {
        val cleanTitle = actionTitleFromMessage(text)
        val chat = chatTitle.trim()
        if (cleanTitle.isBlank() || chat.isEmpty() || messageId == 0L) return ActionSaveResult.Blank
        val key = sourceKey(chat, messageId)
        return try {
            var result = ActionSaveResult.Saved
            dataStore.updateData { raw ->
                val current = raw.withNotesMigrated()
                if (key in current.dismissedActionSources) {
                    result = ActionSaveResult.Dismissed
                    return@updateData current
                }
                val sameSource = current.todos.any {
                    it.sourceMessageId != 0L && sourceKey(it.sourceChatTitle, it.sourceMessageId) == key
                }
                if (sameSource || isDuplicateAction(current.todos, cleanTitle)) {
                    result = ActionSaveResult.Duplicate
                    return@updateData current
                }
                val entry = MemoryEntry(
                    id = UUID.randomUUID().toString(),
                    title = cleanTitle,
                    content = text.trim().take(MAX_TODO_CONTENT),
                    done = false,
                    createdAt = System.currentTimeMillis(),
                    sourceChatTitle = chat,
                    sourceMessageId = messageId,
                )
                current.copy(todos = current.todos.upsertedEntry(entry))
            }
            result
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            ActionSaveResult.WriteFailed
        }
    }

    /**
     * Insert or replace a topic. Returns false when the title is blank or the
     * write fails. A failed write leaves the saved topic unchanged.
     */
    /**
     * Insert or replace a topic. [artifactId] null keeps the saved link.
     * An empty id removes the link. The topic never copies artifact text.
     * Returns false when the title is blank or the write fails.
     */
    suspend fun upsertTopic(
        id: String?,
        title: String,
        summary: String,
        content: String,
        group: String,
        tags: List<String>,
        artifactId: String? = null,
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
                    artifactId = artifactId ?: existing?.artifactId.orEmpty(),
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
     * Saves one validated statement from a new chat message. The topic is unread
     * and keeps [chatTitle] plus [messageId]. A duplicate, a rejected statement,
     * or a failed write does not report success and does not replace saved topics.
     */
    suspend fun addAutomaticTopic(
        userText: String,
        chatTitle: String,
        messageId: Long,
        now: Long = System.currentTimeMillis(),
    ): AutomaticSaveResult {
        val extracted = extractDurableStatement(userText) ?: return AutomaticSaveResult.NotUseful
        if (!isValidExtraction(extracted.content, extracted)) return AutomaticSaveResult.NotUseful
        val title = chatTitle.trim()
        if (title.isEmpty() || messageId == 0L) return AutomaticSaveResult.NotUseful
        return try {
            var result = AutomaticSaveResult.Saved
            dataStore.updateData { raw ->
                val current = raw.withNotesMigrated()
                if (isDuplicateMemory(current.topics, extracted, title, messageId)) {
                    result = AutomaticSaveResult.Duplicate
                    return@updateData current
                }
                val topic = MemoryTopic(
                    id = UUID.randomUUID().toString(),
                    title = extracted.title,
                    summary = extracted.summary,
                    content = extracted.content,
                    group = AUTOMATIC_TOPIC_GROUP,
                    tags = listOf(AUTOMATIC_TOPIC_TAG),
                    unread = true,
                    createdAt = now,
                    updatedAt = now,
                    sourceChatTitle = title,
                    sourceMessageId = messageId,
                )
                current.copy(topics = current.topics.upsertedTopic(topic))
            }
            result
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            AutomaticSaveResult.WriteFailed
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
