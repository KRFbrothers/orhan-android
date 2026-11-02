package ai.orhan.data.workflows

import ai.orhan.BuildConfig
import ai.orhan.data.auth.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Owner-authenticated mirror for manual workflow run metadata. Chat content stays in chat history. */
class WorkflowRunService(
    private val auth: AuthRepository,
    private val baseUrl: String = BuildConfig.DUA_API_URL,
    private val unauthorized: suspend () -> Unit = { auth.clear() },
) {
    suspend fun sync(localRuns: List<WorkflowRun>): Result<List<WorkflowRun>> = withContext(Dispatchers.IO) {
        try {
            require(baseUrl.startsWith("https://")) { "Workflow history service requires HTTPS." }
            val token = auth.accessToken() ?: throw IOException("Sign in to synchronize workflow history.")
            if (localRuns.isNotEmpty()) {
                val response = request(token, "PUT", JSONObject().put("runs", JSONArray().apply {
                    localRuns.take(100).forEach { run -> put(run.toJson()) }
                }))
                if (response.first == 401) unauthorized()
                if (response.first !in 200..299) throw IOException(response.second?.optString("error")?.ifBlank { null } ?: "Workflow history sync failed (HTTP ${response.first}).")
            }
            val (code, body) = request(token, "GET")
            if (code == 401) unauthorized()
            if (code !in 200..299) throw IOException(body?.optString("error")?.ifBlank { null } ?: "Workflow history could not be loaded (HTTP $code).")
            val rows = body?.optJSONArray("runs") ?: throw IOException("Workflow history returned an invalid response.")
            Result.success(List(rows.length()) { index -> rows.getJSONObject(index).toWorkflowRun() })
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (failure: Exception) {
            Result.failure(failure)
        }
    }

    suspend fun save(run: WorkflowRun) = withContext(Dispatchers.IO) {
        require(baseUrl.startsWith("https://")) { "Workflow history service requires HTTPS." }
        val token = auth.accessToken() ?: throw IOException("Sign in to synchronize workflow history.")
        val (code, response) = request(token, "PUT", run.toJson())
        if (code == 401) unauthorized()
        if (code !in 200..299) throw IOException(response?.optString("error")?.ifBlank { null } ?: "Workflow history sync failed (HTTP $code).")
    }

    private fun request(token: String, method: String, body: JSONObject? = null): Pair<Int, JSONObject?> {
        val connection = (URL(baseUrl.trimEnd('/') + "/api/workflow-runs").openConnection() as HttpURLConnection).apply {
            requestMethod = method; instanceFollowRedirects = false; connectTimeout = 15_000; readTimeout = 30_000
            setRequestProperty("Accept", "application/json"); setRequestProperty("Authorization", "Bearer $token")
            if (body != null) { doOutput = true; setRequestProperty("Content-Type", "application/json") }
        }
        return try {
            if (body != null) connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val raw = (if (code in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            code to runCatching { JSONObject(raw) }.getOrNull()
        } finally { connection.disconnect() }
    }

    private fun JSONObject.toWorkflowRun(): WorkflowRun {
        val status = runCatching { WorkflowRunStatus.valueOf(getString("status").uppercase()) }.getOrElse { WorkflowRunStatus.INTERRUPTED }
        return WorkflowRun(getString("runId"), getString("workflowId"), getString("workflowTitle"), getString("chatTitle"),
            optString("agentId").takeUnless { isNull("agentId") || it == "" }, getString("agentName"), status,
            getLong("startedAtMs"), optLong("finishedAtMs"), optString("error").takeUnless { isNull("error") || it == "" },
            optString("engineRunId").takeUnless { isNull("engineRunId") || it == "" })
    }

    private fun WorkflowRun.toJson() = JSONObject().apply {
        put("runId", id); put("workflowId", workflowId); put("workflowTitle", workflowTitle)
        put("chatTitle", chatTitle); put("agentId", agentId ?: JSONObject.NULL); put("agentName", agentName)
        put("status", status.name.lowercase()); put("startedAtMs", startedAt); put("finishedAtMs", finishedAt)
        put("error", error ?: JSONObject.NULL); put("engineRunId", engineRunId ?: JSONObject.NULL)
    }
}
