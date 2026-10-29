package ai.orhan.data.workflows

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

/* Local store for reusable saved prompts ("workflows"). No backend, no scheduling. */
private const val FORMAT_VERSION = 1
const val WORKFLOW_MAX_CONTENT = 4000
const val WORKFLOW_MAX_ITEMS = 200
private const val TITLE_FALLBACK_LEN = 42

data class Workflow(
    val id: String,
    val title: String,
    val content: String,
    /** Blank = default assistant. */
    val agentName: String = "",
    val createdAt: Long,
)

object WorkflowsSerializer : Serializer<List<Workflow>> {
    override val defaultValue: List<Workflow> = emptyList()

    override suspend fun readFrom(input: InputStream): List<Workflow> {
        val bytes = try { input.readBytes() } catch (e: IOException) { throw CorruptionException("read", e) }
        if (bytes.isEmpty()) return defaultValue
        return try {
            DataInputStream(bytes.inputStream()).use { d ->
                if (d.readInt() != FORMAT_VERSION) return defaultValue
                List(d.readInt()) {
                    Workflow(
                        id = d.readUTF(),
                        title = d.readUTF(),
                        content = d.readUTF(),
                        agentName = d.readUTF(),
                        createdAt = d.readLong(),
                    )
                }
            }
        } catch (e: IOException) { defaultValue }
    }

    override suspend fun writeTo(t: List<Workflow>, output: OutputStream) {
        val d = DataOutputStream(output)
        d.writeInt(FORMAT_VERSION)
        d.writeInt(t.size)
        t.forEach { w ->
            d.writeUTF(w.id); d.writeUTF(w.title); d.writeUTF(w.content); d.writeUTF(w.agentName)
            d.writeLong(w.createdAt)
        }
        d.flush()
    }
}

val Context.workflowsDataStore: DataStore<List<Workflow>> by dataStore(
    fileName = "orhan_workflows.pb",
    serializer = WorkflowsSerializer,
)

/** Pure helper: trim/cap fields and derive a title when blank. */
fun Workflow.normalized(): Workflow {
    val body = content.trim().take(WORKFLOW_MAX_CONTENT)
    val t = title.trim().ifBlank { body.replace('\n', ' ').take(TITLE_FALLBACK_LEN).trim() }
    return copy(id = id.ifBlank { UUID.randomUUID().toString() }, title = t, content = body, agentName = agentName.trim())
}

/** Pure helper: replace by id (keeping position), else insert first; capped. */
fun List<Workflow>.upserted(w: Workflow): List<Workflow> {
    val i = indexOfFirst { it.id == w.id }
    val next = if (i >= 0) toMutableList().also { it[i] = w } else listOf(w) + this
    return next.take(WORKFLOW_MAX_ITEMS)
}

class WorkflowStore(private val dataStore: DataStore<List<Workflow>>) {
    /** Newest first. */
    val workflows: Flow<List<Workflow>> = dataStore.data
        .catch { if (it is IOException) emit(emptyList()) else throw it }
        .map { list -> list.sortedByDescending { it.createdAt } }

    suspend fun upsert(workflow: Workflow) {
        val w = workflow.normalized()
        if (w.content.isBlank()) return
        dataStore.updateData { it.upserted(w) }
    }

    suspend fun delete(id: String) {
        dataStore.updateData { list -> list.filterNot { it.id == id } }
    }
}

fun Context.workflowStore(): WorkflowStore = WorkflowStore(workflowsDataStore)
