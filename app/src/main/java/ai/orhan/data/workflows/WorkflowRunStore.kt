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

enum class WorkflowRunStatus { QUEUED, RUNNING, SUCCEEDED, FAILED, CANCELLED, INTERRUPTED }

data class WorkflowRun(
    val id: String,
    val workflowId: String,
    val workflowTitle: String,
    val chatTitle: String,
    val agentId: String?,
    val agentName: String,
    val status: WorkflowRunStatus,
    val startedAt: Long,
    val finishedAt: Long = 0L,
    val error: String? = null,
    val engineRunId: String? = null,
)

private const val RUNS_VERSION = 2
private const val MAX_RUNS = 100

object WorkflowRunsSerializer : Serializer<List<WorkflowRun>> {
    override val defaultValue: List<WorkflowRun> = emptyList()

    override suspend fun readFrom(input: InputStream): List<WorkflowRun> {
        val bytes = try { input.readBytes() } catch (e: IOException) { throw CorruptionException("read", e) }
        if (bytes.isEmpty()) return defaultValue
        return try {
            DataInputStream(bytes.inputStream()).use { d ->
                val version = d.readInt()
                if (version !in 1..RUNS_VERSION) return defaultValue
                val count = d.readInt()
                if (count !in 0..MAX_RUNS) throw IOException("invalid workflow run count")
                List(count) {
                    val id = d.readUTF()
                    val workflowId = d.readUTF()
                    val title = d.readUTF()
                    val chat = d.readUTF()
                    val agentId = if (d.readBoolean()) d.readUTF() else null
                    val agent = d.readUTF()
                    val status = WorkflowRunStatus.entries.getOrNull(d.readInt()) ?: WorkflowRunStatus.INTERRUPTED
                    val started = d.readLong()
                    val finished = d.readLong()
                    val error = if (d.readBoolean()) d.readUTF() else null
                    val engineRunId = if (version >= 2 && d.readBoolean()) d.readUTF() else null
                    WorkflowRun(id, workflowId, title, chat, agentId, agent, status, started, finished, error, engineRunId)
                }
            }
        } catch (e: IOException) { defaultValue }
    }

    override suspend fun writeTo(t: List<WorkflowRun>, output: OutputStream) {
        DataOutputStream(output).apply {
            writeInt(RUNS_VERSION)
            writeInt(t.size.coerceAtMost(MAX_RUNS))
            t.take(MAX_RUNS).forEach { run ->
                writeUTF(run.id); writeUTF(run.workflowId); writeUTF(run.workflowTitle); writeUTF(run.chatTitle)
                writeBoolean(run.agentId != null); run.agentId?.let(::writeUTF)
                writeUTF(run.agentName); writeInt(run.status.ordinal); writeLong(run.startedAt); writeLong(run.finishedAt)
                writeBoolean(run.error != null); run.error?.take(300)?.let(::writeUTF)
                writeBoolean(run.engineRunId != null); run.engineRunId?.take(100)?.let(::writeUTF)
            }
            flush()
        }
    }
}

val Context.workflowRunsDataStore: DataStore<List<WorkflowRun>> by dataStore(
    fileName = "orhan_workflow_runs.pb",
    serializer = WorkflowRunsSerializer,
)

class WorkflowRunStore(private val dataStore: DataStore<List<WorkflowRun>>) {
    val runs: Flow<List<WorkflowRun>> = dataStore.data
        .catch { if (it is IOException) emit(emptyList()) else throw it }
        .map { list -> list.sortedByDescending { it.startedAt } }

    suspend fun start(workflow: Workflow, chatTitle: String, agentName: String): WorkflowRun {
        val run = WorkflowRun(
            id = UUID.randomUUID().toString(), workflowId = workflow.id, workflowTitle = workflow.title.take(100),
            chatTitle = chatTitle.take(100), agentId = workflow.agentId, agentName = agentName.take(100),
            status = WorkflowRunStatus.QUEUED, startedAt = System.currentTimeMillis(),
        )
        dataStore.updateData { (listOf(run) + it).take(MAX_RUNS) }
        return run
    }

    suspend fun update(id: String, status: WorkflowRunStatus, error: String? = null) {
        val finished = if (status in setOf(WorkflowRunStatus.SUCCEEDED, WorkflowRunStatus.FAILED, WorkflowRunStatus.CANCELLED, WorkflowRunStatus.INTERRUPTED)) System.currentTimeMillis() else 0L
        dataStore.updateData { runs -> runs.map { if (it.id == id) it.copy(status = status, finishedAt = finished, error = error?.take(300)) else it } }
    }

    suspend fun saveEngineRunId(id: String, engineRunId: String) {
        val safeId = engineRunId.take(100)
        if (safeId.isBlank()) return
        dataStore.updateData { runs -> runs.map { if (it.id == id) it.copy(engineRunId = safeId) else it } }
    }

    suspend fun interruptOrphanedRuns() {
        dataStore.updateData { runs -> runs.map { run ->
            if (run.status == WorkflowRunStatus.QUEUED || run.status == WorkflowRunStatus.RUNNING) {
                run.copy(status = WorkflowRunStatus.INTERRUPTED, finishedAt = System.currentTimeMillis(), error = "App stopped before the workflow finished.")
            } else run
        } }
    }
}

fun Context.workflowRunStore(): WorkflowRunStore = WorkflowRunStore(workflowRunsDataStore)
