package ai.orhan.data.workflows

import ai.orhan.BuildConfig
import ai.orhan.data.agents.SavedAgent
import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
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

data class ExecutionStep(val id: String, val agentName: String, val status: String, val output: String, val error: String?)
data class WorkflowExecution(
    val id: String, val status: String, val attempt: Int, val cancelRequested: Boolean,
    val createdAt: String, val dueAt: String, val error: String?, val steps: List<ExecutionStep>,
) {
    val active: Boolean get() = status == "queued" || status == "running"
}

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

    suspend fun start(workflow: Workflow, agents: List<SavedAgent>, handoffIds: List<String>, at: String): WorkflowExecution = submissionMutex.withLock {
        val token = tokenProvider() ?: throw IOException("Sign in from Profile to run a workflow.")
        // Decoding is ONLY a local cache namespace, never an authorization decision.
        val ownerKey = JSONObject(String(Base64.decode(token.split('.')[1], Base64.URL_SAFE or Base64.NO_WRAP), Charsets.UTF_8)).getString("sub")
        val key = stringPreferencesKey("$ownerKey:${workflow.id}")
        val pending = context.workflowSubmissions.data.first()[key]
        val payload = if (pending != null) JSONObject(pending) else {
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
            JSONObject().put("workflowId", workflow.id).put("title", workflow.title.take(120))
                .put("requestKey", UUID.randomUUID().toString()).put("steps", steps)
                .put("trigger", if (at.isBlank()) JSONObject().put("type", "manual") else JSONObject().put("type", "once").put("at", at))
                .also { body -> context.workflowSubmissions.edit { it[key] = body.toString() } }
        }
        // Persist BEFORE dispatch. A timeout/restart retries the exact same snapshot and key.
        val result = request("POST", "", payload)
        val run = parse(result.getJSONObject("run"))
        context.workflowSubmissions.edit { it.remove(key) }
        run
    }

    suspend fun control(run: WorkflowExecution, action: String): WorkflowExecution {
        require(action == "retry" || action == "cancel")
        return parse(request("POST", "/${run.id}/$action", JSONObject().put("expectedAttempt", run.attempt)).getJSONObject("run"))
    }

    /** Explicitly discard an unconfirmed request after checking server history. */
    suspend fun discardPending(workflowId: String) {
        val token = tokenProvider() ?: throw IOException("Sign in first.")
        val owner = JSONObject(String(Base64.decode(token.split('.')[1], Base64.URL_SAFE or Base64.NO_WRAP), Charsets.UTF_8)).getString("sub")
        context.workflowSubmissions.edit { it.remove(stringPreferencesKey("$owner:$workflowId")) }
    }

    private suspend fun request(method: String, suffix: String, body: JSONObject? = null): JSONObject = withContext(Dispatchers.IO) {
        require(baseUrl.startsWith("https://")) { "Workflow API URL must use HTTPS." }
        val token = tokenProvider() ?: throw IOException("Sign in from Profile to use server workflows.")
        val conn = URL(baseUrl.trimEnd('/') + "/api/workflow-executions" + suffix).openConnection() as HttpURLConnection
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
        return WorkflowExecution(row.getString("id"), row.getString("status"), row.getInt("attempt"), row.optBoolean("cancelRequested"),
            row.getString("createdAt"), row.getString("dueAt"), row.optionalText("error"),
            List(steps.length()) { index -> steps.getJSONObject(index).let { s ->
                ExecutionStep(s.getString("id"), s.getString("agentName"), s.getString("status"), s.optString("output"), s.optionalText("error"))
            } })
    }
}

private fun JSONObject.optionalText(key: String): String? = if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
