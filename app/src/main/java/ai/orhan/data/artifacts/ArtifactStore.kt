package ai.orhan.data.artifacts

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/* Local store for agent replies saved as artifacts (no backend). */
private const val FORMAT_VERSION = 3
const val MAX_ARTIFACT_CONTENT = 64_000
const val MAX_ARTIFACTS = 500

enum class ArtifactType(val wireName: String, val label: String, val mimeType: String, val extension: String) {
    PLAIN_TEXT("text", "Plain text", "text/plain", "txt"),
    MARKDOWN("markdown", "Markdown", "text/markdown", "md"),
    HTML("html", "Web page (HTML)", "text/html", "html"),
}

enum class ArtifactKind(val wireName: String, val label: String) {
    DOCUMENT("document", "Document"),
    SAVED_MESSAGE("saved_message", "Saved message"),
    MEETING_MINUTES("meeting_minutes", "Meeting minutes"),
}

data class Artifact(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: Long,
    val type: ArtifactType = ArtifactType.PLAIN_TEXT,
    val kind: ArtifactKind = ArtifactKind.DOCUMENT,
    /** Existing chat-thread key (canonical chat title) used by ChatThreadStore. */
    val sourceChatId: String? = null,
    val sourceMessageId: Long? = null,
    val author: String? = null,
)

object ArtifactsSerializer : Serializer<List<Artifact>> {
    override val defaultValue: List<Artifact> = emptyList()

    override suspend fun readFrom(input: InputStream): List<Artifact> {
        val bytes = try { input.readBytes() } catch (e: IOException) { throw CorruptionException("read", e) }
        if (bytes.isEmpty()) return defaultValue
        return try {
            DataInputStream(bytes.inputStream()).use { d ->
                val version = d.readInt()
                if (version !in 1..FORMAT_VERSION) throw IOException("Unsupported artifact format")
                val count = d.readInt()
                if (count !in 0..MAX_ARTIFACTS) throw IOException("Invalid artifact count")
                List(count) {
                    Artifact(
                        id = d.readUTF(),
                        title = d.readUTF(),
                        content = d.readLongString(),
                        createdAt = d.readLong(),
                        type = if (version == 1) ArtifactType.PLAIN_TEXT else {
                            val name = d.readUTF()
                            ArtifactType.entries.firstOrNull { it.wireName == name }
                                ?: throw IOException("Unsupported artifact type")
                        },
                        kind = if (version >= 3) {
                            val kindName = d.readUTF()
                            ArtifactKind.entries.firstOrNull { it.wireName == kindName }
                                ?: throw IOException("Unsupported artifact kind")
                        } else ArtifactKind.DOCUMENT,
                        sourceChatId = if (version >= 3) d.readNullableUTF() else null,
                        sourceMessageId = if (version >= 3 && d.readBoolean()) d.readLong() else null,
                        author = if (version >= 3) d.readNullableUTF() else null,
                    )
                }
            }
        } catch (e: IOException) { throw CorruptionException("Cannot read saved artifacts", e) }
    }

    override suspend fun writeTo(t: List<Artifact>, output: OutputStream) {
        val d = DataOutputStream(output)
        d.writeInt(FORMAT_VERSION)
        d.writeInt(t.size)
        t.forEach { a ->
            d.writeUTF(a.id); d.writeUTF(a.title.take(200))
            d.writeLongString(a.content)
            d.writeLong(a.createdAt)
            d.writeUTF(a.type.wireName)
            d.writeUTF(a.kind.wireName)
            d.writeNullableUTF(a.sourceChatId)
            d.writeBoolean(a.sourceMessageId != null)
            a.sourceMessageId?.let(d::writeLong)
            d.writeNullableUTF(a.author)
        }
        d.flush()
    }
}

private fun DataOutputStream.writeNullableUTF(value: String?) {
    writeBoolean(value != null)
    if (value != null) writeUTF(value.take(200))
}

private fun DataInputStream.readNullableUTF(): String? = if (readBoolean()) readUTF() else null

// writeUTF caps at 64KB of encoded bytes; content may exceed that with non-ASCII text.
private fun DataOutputStream.writeLongString(s: String) {
    val b = s.toByteArray(Charsets.UTF_8)
    writeInt(b.size); write(b)
}

private fun DataInputStream.readLongString(): String {
    val n = readInt()
    if (n < 0 || n > MAX_ARTIFACT_CONTENT * 4) throw IOException("bad length")
    val b = ByteArray(n); readFully(b)
    return String(b, Charsets.UTF_8)
}

val Context.artifactsDataStore: DataStore<List<Artifact>> by dataStore(
    fileName = "orhan_artifacts.pb",
    serializer = ArtifactsSerializer,
)

/** Pure helper: insert/replace by id, newest first. */
fun List<Artifact>.upserted(a: Artifact): List<Artifact> =
    (listOf(a) + filterNot { it.id == a.id }).take(MAX_ARTIFACTS)

/** Case-insensitive title and content search. A blank query keeps the current order. */
fun List<Artifact>.matching(query: String): List<Artifact> {
    val needle = query.trim()
    if (needle.isEmpty()) return this
    return filter {
        it.title.contains(needle, ignoreCase = true) || it.content.contains(needle, ignoreCase = true) ||
            it.author?.contains(needle, ignoreCase = true) == true || it.sourceChatId?.contains(needle, ignoreCase = true) == true
    }
}

class ArtifactStore(private val dataStore: DataStore<List<Artifact>>) {
    val artifacts: Flow<List<Artifact>> = dataStore.data
        .catch { if (it is IOException) emit(emptyList()) else throw it }
        .map { list -> list.sortedByDescending { it.createdAt } }

    /**
     * Inserts or replaces one artifact by id. Blank content is rejected and
     * nothing is written. A failed write returns false and leaves the saved
     * list unchanged. The id and created time passed in are kept.
     */
    suspend fun upsert(artifact: Artifact): Boolean {
        if (artifact.content.isBlank()) return false
        val clean = artifact.copy(
            title = artifact.title.trim().ifBlank { artifact.content.trim().take(42) }.take(200),
            content = artifact.content.take(MAX_ARTIFACT_CONTENT),
        )
        return write { it.upserted(clean) }
    }

    suspend fun upsert(title: String, content: String): Boolean = upsert(
        Artifact(UUID.randomUUID().toString(), title, content, System.currentTimeMillis()),
    )

    /** Removes one artifact. A failed write returns false and keeps the list. */
    suspend fun delete(id: String): Boolean = write { list -> list.filterNot { it.id == id } }

    private suspend fun write(transform: (List<Artifact>) -> List<Artifact>): Boolean {
        return try {
            dataStore.updateData(transform)
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        }
    }
}

fun Context.artifactStore(): ArtifactStore = ArtifactStore(artifactsDataStore)
