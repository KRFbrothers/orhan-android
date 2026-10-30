package ai.orhan.data.history

import ai.orhan.data.chat.ChatAttachment
import ai.orhan.design.screens.ChatMessage
import ai.orhan.design.screens.ChatRole
import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import android.util.Base64
import java.io.DataInputStream
import java.io.File
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/*
 * Local per-chat message threads keyed by chat title (same string as AppRoutes.chat(title)).
 * Device-only; no cloud sync.
 */
private const val THREAD_FORMAT_VERSION = 2
private const val THREAD_FORMAT_V1 = 1
/** Total on-disk budget for persisted chat attachment files; oldest files are pruned beyond it. */
const val MAX_ATTACHMENT_DISK_BYTES = 40L * 1024 * 1024
const val MAX_THREAD_MESSAGES = 100
const val MAX_THREADS = 40

/** Attachment metadata; the bytes live on disk at filesDir/chat_attachments/[fileName]. */
data class StoredAttachment(val name: String, val mimeType: String, val sizeBytes: Long, val fileName: String)

data class StoredMessage(
    val id: Long, val role: String, val text: String, val time: String,
    val attachment: StoredAttachment? = null,
)
data class ChatThread(val title: String, val updatedAt: Long, val messages: List<StoredMessage>)

object ChatThreadsSerializer : Serializer<List<ChatThread>> {
    override val defaultValue: List<ChatThread> = emptyList()

    override suspend fun readFrom(input: InputStream): List<ChatThread> {
        val bytes = try { input.readBytes() } catch (e: IOException) { throw CorruptionException("read", e) }
        if (bytes.isEmpty()) return defaultValue
        return try {
            DataInputStream(bytes.inputStream()).use { d ->
                val version = d.readInt()
                if (version != THREAD_FORMAT_VERSION && version != THREAD_FORMAT_V1) return defaultValue
                val threadCount = d.readInt()
                if (threadCount < 0 || threadCount > 10_000) throw IOException("corrupt thread count $threadCount")
                List(threadCount) {
                    val title = d.readUTF()
                    val updatedAt = d.readLong()
                    val msgCount = d.readInt()
                    if (msgCount < 0 || msgCount > 100_000) throw IOException("corrupt message count $msgCount")
                    val msgs = List(msgCount) {
                        val m = StoredMessage(d.readLong(), d.readUTF(), readLongString(d), d.readUTF())
                        if (version >= 2 && d.readBoolean()) {
                            m.copy(attachment = StoredAttachment(d.readUTF(), d.readUTF(), d.readLong(), d.readUTF()))
                        } else m
                    }
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
            th.messages.forEach { m -> d.writeLong(m.id); d.writeUTF(m.role); writeLongString(d, m.text); d.writeUTF(m.time)
                val a = m.attachment
                d.writeBoolean(a != null)
                if (a != null) { d.writeUTF(a.name); d.writeUTF(a.mimeType); d.writeLong(a.sizeBytes); d.writeUTF(a.fileName) }
            }
        }
        d.flush()
    }

    // writeUTF caps at 64KB; agent replies can be longer.
    private fun writeLongString(d: DataOutputStream, s: String) {
        val b = s.toByteArray(Charsets.UTF_8); d.writeInt(b.size); d.write(b)
    }
    private fun readLongString(d: DataInputStream): String {
        val n = d.readInt()
        if (n < 0 || n > 4 * 1024 * 1024) throw IOException("corrupt string length $n")
        val b = ByteArray(n); d.readFully(b); return String(b, Charsets.UTF_8)
    }
}

val Context.chatThreadsDataStore: DataStore<List<ChatThread>> by dataStore(
    fileName = "orhan_chat_threads.pb",
    serializer = ChatThreadsSerializer,
)

internal fun ChatMessage.toStored(attachment: StoredAttachment? = null) =
    StoredMessage(id, if (role == ChatRole.User) "user" else "agent", text, time, attachment)

internal fun StoredMessage.toUi() =
    ChatMessage(id, if (role == "user") ChatRole.User else ChatRole.Agent, text, time)

/** Pure helper: upsert a thread, cap messages and thread count (drop oldest by updatedAt). */
fun List<ChatThread>.withSaved(
    title: String,
    messages: List<ChatMessage>,
    now: Long,
    attachments: Map<Long, StoredAttachment> = emptyMap(),
): List<ChatThread> {
    val rest = filterNot { it.title == title }
    if (messages.isEmpty()) return rest
    val th = ChatThread(title, now, messages.takeLast(MAX_THREAD_MESSAGES).map { it.toStored(attachments[it.id]) })
    return (rest + th).sortedByDescending { it.updatedAt }.take(MAX_THREADS)
}

/** Pure helper: drops attachment metadata from every message; text, titles and updatedAt are kept. */
fun List<ChatThread>.withoutAttachments(): List<ChatThread> =
    map { t -> t.copy(messages = t.messages.map { it.copy(attachment = null) }) }

/** A restored thread: UI messages plus re-sendable attachments keyed by user message id. */
data class LoadedThread(val messages: List<ChatMessage>, val attachments: Map<Long, ChatAttachment>)

/**
 * File-backed attachment bytes under [dir] (app filesDir/chat_attachments). Raw bytes, not base64,
 * so the DataStore file stays small. Pure java.io so it is unit-testable.
 */
class AttachmentFiles(private val dir: File?, private val maxBytes: Long = MAX_ATTACHMENT_DISK_BYTES) {
    /** First 16 hex chars of SHA-256(UTF-8 title); avoids 32-bit hashCode collisions. */
    fun titleHash(threadTitle: String): String =
        java.security.MessageDigest.getInstance("SHA-256")
            .digest(threadTitle.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }.take(16)

    fun fileNameFor(threadTitle: String, messageId: Long) =
        "a_" + titleHash(threadTitle) + "_" + messageId + ".bin"

    /** Writes the bytes if missing; returns metadata, or null if it could not be stored. */
    fun put(threadTitle: String, messageId: Long, a: ChatAttachment, decode: (String) -> ByteArray): StoredAttachment? {
        val d = dir ?: return null
        if (a.sizeBytes > ChatAttachment.MAX_BYTES) return null
        val name = fileNameFor(threadTitle, messageId)
        return try {
            d.mkdirs()
            val f = File(d, name)
            if (!f.exists()) {
                val tmp = File(d, "$name.tmp")
                tmp.writeBytes(decode(a.base64))
                if (!tmp.renameTo(f)) { tmp.delete(); return null }
            }
            StoredAttachment(a.name, a.mimeType, a.sizeBytes, name)
        } catch (e: Exception) { null }
    }

    fun read(s: StoredAttachment, encode: (ByteArray) -> String): ChatAttachment? {
        val f = File(dir ?: return null, s.fileName)
        if (!f.isFile) return null
        return try { ChatAttachment(s.name, s.mimeType, encode(f.readBytes()), s.sizeBytes) } catch (e: Exception) { null }
    }

    /** Total bytes of attachment files on disk (0 if the dir is missing). Ignores in-progress .tmp files. */
    fun totalBytes(): Long = liveFiles().sumOf { it.length() }

    /** Number of attachment files on disk. */
    fun fileCount(): Int = liveFiles().size

    private fun liveFiles(): List<File> =
        dir?.listFiles()?.filter { it.isFile && !it.name.endsWith(".tmp") }.orEmpty()

    /** Deletes files not referenced by [keep], then oldest files until under the disk budget. */
    fun prune(keep: Set<String>) {
        val files = dir?.listFiles()?.filter { it.isFile } ?: return
        files.filter { it.name !in keep }.forEach { it.delete() }
        var live = files.filter { it.name in keep && it.exists() }.sortedBy { it.lastModified() }
        var total = live.sumOf { it.length() }
        while (total > maxBytes && live.isNotEmpty()) {
            total -= live.first().length(); live.first().delete(); live = live.drop(1)
        }
    }
}

class ChatThreadStore(
    private val dataStore: DataStore<List<ChatThread>>,
    private val files: AttachmentFiles = AttachmentFiles(null),
) {
    val threads: Flow<List<ChatThread>> = dataStore.data
        .catch { if (it is IOException) emit(emptyList()) else throw it }

    suspend fun load(title: String): List<ChatMessage> = loadThread(title).messages

    /** Loads messages and any attachment files still on disk (missing files are skipped; the 📎 label stays). */
    suspend fun loadThread(title: String): LoadedThread = withContext(Dispatchers.IO) {
        val msgs = threads.first().firstOrNull { it.title == title }?.messages.orEmpty()
        val atts = msgs.mapNotNull { m -> m.attachment?.let { s -> files.read(s, ::b64encode)?.let { m.id to it } } }.toMap()
        LoadedThread(msgs.map { it.toUi() }, atts)
    }

    suspend fun save(
        title: String,
        messages: List<ChatMessage>,
        attachments: Map<Long, ChatAttachment> = emptyMap(),
        now: Long = System.currentTimeMillis(),
    ) {
        if (title.isBlank()) return
        withContext(Dispatchers.IO) {
            val kept = messages.takeLast(MAX_THREAD_MESSAGES).map { it.id }.toSet()
            val stored = attachments.filterKeys { it in kept }
                .mapNotNull { (id, a) -> files.put(title, id, a, ::b64decode)?.let { id to it } }.toMap()
            val all = dataStore.updateData { it.withSaved(title, messages, now, stored) }
            prune(all)
        }
    }

    suspend fun remove(title: String) {
        prune(dataStore.updateData { list -> list.filterNot { it.title == title } })
    }

    suspend fun clear() {
        dataStore.updateData { emptyList() }
        withContext(Dispatchers.IO) { files.prune(emptySet()) }
    }

    /** Disk usage of chat attachment files: bytes to file count. */
    suspend fun attachmentUsage(): Pair<Long, Int> = withContext(Dispatchers.IO) { files.totalBytes() to files.fileCount() }

    /** Deletes all chat attachment files and their metadata; chat text is kept. */
    suspend fun clearAttachments() {
        dataStore.updateData { it.withoutAttachments() }
        withContext(Dispatchers.IO) { files.prune(emptySet()) }
    }

    private suspend fun prune(all: List<ChatThread>) = withContext(Dispatchers.IO) {
        files.prune(all.flatMap { t -> t.messages.mapNotNull { it.attachment?.fileName } }.toSet())
    }
}

private fun b64encode(b: ByteArray): String = Base64.encodeToString(b, Base64.NO_WRAP)
private fun b64decode(s: String): ByteArray = Base64.decode(s, Base64.DEFAULT)

fun Context.chatThreads(): ChatThreadStore =
    ChatThreadStore(chatThreadsDataStore, AttachmentFiles(File(filesDir, "chat_attachments")))
