package ai.orhan.data.workflows

import ai.orhan.BuildConfig
import ai.orhan.data.agents.SavedAgent
import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

private val Context.workflowSubmissions by preferencesDataStore(name = "orhan_workflow_submissions")
private val submissionMutex = Mutex()

enum class ExecutionStatus(val wire: String) {
    QUEUED("queued"), RUNNING("running"), SUCCEEDED("succeeded"), FAILED("failed"), CANCELLED("cancelled");
    companion object {
        fun fromWire(value: String): ExecutionStatus = entries.firstOrNull { it.wire == value }
            ?: throw IOException("Unsupported workflow status. Update the app/service before controlling this run.")
    }
}
data class ExecutionStep(val id: String, val agentName: String, val status: ExecutionStatus, val output: String, val error: String?)
data class WorkflowExecution(
    val id: String, val status: ExecutionStatus, val attempt: Int, val cancelRequested: Boolean,
    val createdAt: String, val dueAt: String, val error: String?, val steps: List<ExecutionStep>,
) {
    val active: Boolean get() = status == ExecutionStatus.QUEUED || status == ExecutionStatus.RUNNING
}
data class WorkflowAutomation(val id: String, val title: String, val kind: String, val revision: Int, val enabled: Boolean,
    val intervalSeconds: Int?, val nextDue: String?, val lastSkippedAt: String?)

/** Authoritative server executions stay separate from the pre-existing local chat ledger. */
class WorkflowExecutionRepository(
    private val context: Context,
    private val tokenProvider: suspend () -> String?,
    private val baseUrl: String = BuildConfig.DUA_API_URL,
) {
    suspend fun history(workflowId: String): List<WorkflowExecution> =
        request("GET", "?workflowId=${java.net.URLEncoder.encode(workflowId, "UTF-8")}").getJSONArray("runs").let { rows ->
            List(rows.length()) { parse(rows.getJSONObject(it)) }
        }

    suspend fun detail(id: String): WorkflowExecution = parse(request("GET", "/$id").getJSONObject("run"))

    suspend fun hasPending(workflowId: String): Boolean = context.workflowSubmissions.data.first()[submissionKey(workflowId)] != null

    suspend fun start(workflow: Workflow, agents: List<SavedAgent>, handoffIds: List<String>, at: String): WorkflowExecution = submissionMutex.withLock {
        val key = submissionKey(workflow.id)
        val pending = context.workflowSubmissions.data.first()[key]
        val payload = if (pending != null) JSONObject(pending) else buildSubmission(workflow, agents, handoffIds, at).also { body ->
            context.workflowSubmissions.edit { it[key] = body.toString() }
        }
        // Persist BEFORE dispatch. A timeout/restart retries the exact same snapshot and key.
        val result = request("POST", "", payload)
        val run = parse(result.getJSONObject("run"))
        context.workflowSubmissions.edit { it.remove(key) }
        run
    }

    private fun buildSubmission(workflow: Workflow, agents: List<SavedAgent>, handoffIds: List<String>, at: String): JSONObject {
        val primary = workflow.agentId?.let { id -> agents.firstOrNull { it.id == id } }
            ?: if (workflow.agentId == null) agents.firstOrNull { it.name.equals(workflow.agentName, true) } else null
        val default = workflow.agentId == null && (workflow.agentName.isBlank() || workflow.agentName.equals("Orhan", true))
        if (primary == null && !default) throw IOException("The workflow's saved agent is unavailable. Choose an available agent and save first.")
        val chain = listOf(primary) + handoffIds.map { id -> agents.firstOrNull { it.id == id } ?: throw IOException("A handoff agent was deleted. Update the step list.") }
        val steps = JSONArray()
        chain.forEachIndexed { index, agent ->
            steps.put(JSONObject().put("id", "step-${index + 1}").put("agentId", agent?.id ?: JSONObject.NULL)
                .put("agentName", agent?.name ?: "Orhan").put("instructions", agent?.instructions.orEmpty())
                .put("prompt", workflow.content)
                // Legacy preferences remain inactive; workflow opt-in never enables hidden tools.
                .put("allowedTools", JSONArray(if (agent?.toolsRestricted == true) agent.tools else emptyList<String>())))
        }
        return JSONObject().put("workflowId", workflow.id).put("title", workflow.title.take(120))
            .put("requestKey", UUID.randomUUID().toString()).put("steps", steps)
            .put("trigger", if (at.isBlank()) JSONObject().put("type", "manual") else JSONObject().put("type", "once").put("at", at))
    }

    suspend fun automations(workflowId: String): List<WorkflowAutomation> =
        request("GET", "?workflowId=${java.net.URLEncoder.encode(workflowId, "UTF-8")}", route = "/api/workflow-automation")
            .getJSONArray("registrations").let { rows -> List(rows.length()) { parseAutomation(rows.getJSONObject(it)) } }

    suspend fun hasPendingRecurring(workflowId: String): Boolean = context.workflowSubmissions.data.first()[submissionKey("recurring:$workflowId")] != null

    suspend fun createRecurring(workflow: Workflow, agents: List<SavedAgent>, handoffIds: List<String>, at: String, intervalSeconds: Int): WorkflowAutomation = submissionMutex.withLock {
        val key = submissionKey("recurring:${workflow.id}")
        val pending = context.workflowSubmissions.data.first()[key]
        val envelope = if (pending != null) JSONObject(pending) else {
            val body = buildSubmission(workflow, agents, handoffIds, at)
            body.remove("trigger")
            body.put("expectedRevision", 0).put("enabled", true).put("intervalSeconds", intervalSeconds).put("startAt", at)
            JSONObject().put("id", UUID.randomUUID().toString()).put("body", body).also { saved ->
                context.workflowSubmissions.edit { it[key] = saved.toString() }
            }
        }
        val json = request("PUT", "/schedules/${envelope.getString("id")}", envelope.getJSONObject("body"), "/api/workflow-automation")
        val result = parseAutomation(json.getJSONObject("registration"))
        context.workflowSubmissions.edit { it.remove(key) }
        result
    }

    suspend fun disableAutomation(item: WorkflowAutomation): WorkflowAutomation = parseAutomation(
        request("POST", "/${if (item.kind == "interval") "schedules" else "triggers"}/${item.id}/disable",
            JSONObject().put("expectedRevision", item.revision), "/api/workflow-automation").getJSONObject("registration"))

    suspend fun control(run: WorkflowExecution, action: String): WorkflowExecution {
        require(action == "retry" || action == "cancel")
        return parse(request("POST", "/${run.id}/$action", JSONObject().put("expectedAttempt", run.attempt)).getJSONObject("run"))
    }

    /** Explicitly discard an unconfirmed request after checking server history. */
    suspend fun discardPending(workflowId: String) {
        submissionMutex.withLock {
            val key = submissionKey(workflowId)
            context.workflowSubmissions.edit { it.remove(key) }
        }
    }

    private suspend fun submissionKey(workflowId: String): Preferences.Key<String> {
        val token = tokenProvider() ?: throw IOException("Sign in first.")
        // Decoding is ONLY a local cache namespace, never an authorization decision.
        val owner = JSONObject(String(Base64.decode(token.split('.')[1], Base64.URL_SAFE or Base64.NO_WRAP), Charsets.UTF_8)).getString("sub")
        return stringPreferencesKey("$owner:$workflowId")
    }

    private suspend fun request(method: String, suffix: String, body: JSONObject? = null, route: String = "/api/workflow-executions"): JSONObject = withContext(Dispatchers.IO) {
        val origin = URL(baseUrl)
        require(origin.protocol == "https" && origin.userInfo == null && origin.query == null && origin.ref == null && origin.path in listOf("", "/")) { "Workflow API URL must be an HTTPS origin." }
        val token = tokenProvider() ?: throw IOException("Sign in from Profile to use server workflows.")
        val conn = URL(baseUrl.trimEnd('/') + route + suffix).openConnection() as HttpURLConnection
        coroutineScope {
            val disconnect = launch(Dispatchers.IO) { try { awaitCancellation() } finally { conn.disconnect() } }
            try {
                conn.requestMethod = method
                conn.instanceFollowRedirects = false
                conn.connectTimeout = 15_000
                conn.readTimeout = 20_000
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.setRequestProperty("Accept", "application/json")
                if (body != null) {
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                }
                val code = conn.responseCode
                val bytes = (if (code in 200..299) conn.inputStream else conn.errorStream)?.use { input ->
                    val output = java.io.ByteArrayOutputStream()
                    val chunk = ByteArray(8192)
                    while (true) {
                        val count = input.read(chunk)
                        if (count < 0) break
                        if (output.size() + count > 2 * 1024 * 1024) throw IOException("Workflow response is too large.")
                        output.write(chunk, 0, count)
                    }
                    output.toByteArray()
                }
                    ?: throw IOException("Workflow service returned no response.")
                if (bytes.size > 2 * 1024 * 1024) throw IOException("Workflow response is too large.")
                val json = runCatching { JSONObject(String(bytes, Charsets.UTF_8)) }.getOrNull()
                if (code !in 200..299) throw IOException(json?.optString("error")?.takeIf { it.isNotBlank() }
                    ?: if (code == 404) "Workflow execution API is not deployed on this service." else "Workflow request failed (HTTP $code).")
                json ?: throw IOException("Workflow service returned an invalid response.")
            } finally { disconnect.cancel(); conn.disconnect() }
        }
    }

    private fun parse(row: JSONObject): WorkflowExecution {
        val steps = row.getJSONArray("steps")
        if (steps.length() !in 1..8 || row.getInt("attempt") < 1) throw IOException("Invalid workflow execution response.")
        return WorkflowExecution(row.getString("id"), ExecutionStatus.fromWire(row.getString("status")), row.getInt("attempt"), row.optBoolean("cancelRequested"),
            row.getString("createdAt"), row.getString("dueAt"), row.optionalText("error"),
            List(steps.length()) { index -> steps.getJSONObject(index).let { s ->
                ExecutionStep(s.getString("id"), s.getString("agentName"), ExecutionStatus.fromWire(s.getString("status")), s.optString("output"), s.optionalText("error"))
            } })
    }

    private fun parseAutomation(row: JSONObject): WorkflowAutomation {
        val kind = row.getString("kind")
        if (kind !in listOf("interval", "webhook") || row.getInt("revision") < 1) throw IOException("Unsupported workflow automation response.")
        return WorkflowAutomation(row.getString("id"), row.getString("title"), kind, row.getInt("revision"), row.getBoolean("enabled"),
            if (row.isNull("intervalSeconds")) null else row.getInt("intervalSeconds"), row.optionalText("nextDue"), row.optionalText("lastSkippedAt"))
    }
}

private fun JSONObject.optionalText(key: String): String? = if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
