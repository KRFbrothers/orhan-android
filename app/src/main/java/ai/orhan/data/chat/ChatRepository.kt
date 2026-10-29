package ai.orhan.data.chat

import ai.orhan.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** One turn sent to the Dua engine. [role] is "user" or "model". */
data class ChatTurn(val role: String, val text: String)

/**
 * Talks to the Dua AI engine: POST {DUA_ENGINE_URL}/api/chat (non-streaming).
 * Request:  {"stream":false,"mode":"agent","messages":[{"role":"user|model","text":"..."}]}
 * Response: {"text":"..."} or {"error":"..."}
 *
 * [stream] sends `"stream":true` and parses the engine's SSE events
 * (`event: text` → `{"type":"text","delta":"..."}`, `error`, `done`).
 */
class ChatRepository(
    private val baseUrl: String = BuildConfig.DUA_ENGINE_URL,
    private val appToken: String = BuildConfig.DUA_APP_TOKEN,
    /** Supplies the Supabase user JWT (signed-in session, or DUA_DEV_JWT fallback in debug). */
    private val tokenProvider: suspend () -> String? = { BuildConfig.DUA_DEV_JWT.ifBlank { null } },
    /** Called when the engine rejects the token (HTTP 401), e.g. to clear the session. */
    private val onUnauthorized: suspend () -> Unit = {},
) {
    /** Thrown when streaming fails in a way a non-stream retry can't fix (e.g. 401). */
    class FatalChatException(message: String) : IOException(message)

    private fun body(turns: List<ChatTurn>, stream: Boolean) = JSONObject()
        .put("stream", stream)
        .put("mode", "agent") // always Dua Agent pipeline (server uses AGENT_ENGINE=adk when configured)
        .put("messages", JSONArray().apply {
            turns.takeLast(20).forEach { put(JSONObject().put("role", it.role).put("text", it.text)) }
        })

    private suspend fun open(accept: String): HttpURLConnection {
        require(baseUrl.isNotBlank()) { "DUA_ENGINE_URL is not set in local.properties" }
        val jwt = tokenProvider() ?: throw FatalChatException("Sign in required (no Supabase JWT)")
        return (URL(baseUrl.trimEnd('/') + "/api/chat").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 120_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", accept)
            setRequestProperty("Authorization", "Bearer $jwt")
            if (appToken.isNotBlank()) setRequestProperty("X-Dua-App-Token", appToken)
        }
    }

    /**
     * Streams a reply. [onDelta] is called (on IO) with each text chunk. Returns the full text.
     * Fails with [FatalChatException] for auth problems; any other failure is fallback-eligible.
     */
    suspend fun stream(turns: List<ChatTurn>, onDelta: suspend (String) -> Unit): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val conn = open("text/event-stream")
                try {
                    conn.outputStream.use { it.write(body(turns, true).toString().toByteArray()) }
                    val code = conn.responseCode
                    if (code == 401) {
                        onUnauthorized()
                        throw FatalChatException("Session expired — please sign in again")
                    }
                    if (code !in 200..299) error("HTTP $code")
                    if (conn.contentType?.contains("text/event-stream") != true) error("Not an SSE response")
                    val out = StringBuilder()
                    var done = false
                    val data = StringBuilder()
                    conn.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                        while (!done) {
                            val line = reader.readLine() ?: break
                            when {
                                line.startsWith("data:") -> {
                                    if (data.isNotEmpty()) data.append('\n')
                                    data.append(line.removePrefix("data:").removePrefix(" "))
                                }
                                line.isEmpty() && data.isNotEmpty() -> {
                                    val ev = runCatching { JSONObject(data.toString()) }.getOrNull()
                                    data.setLength(0)
                                    when (ev?.optString("type")) {
                                        "text" -> ev.optString("delta").takeIf { it.isNotEmpty() }?.let {
                                            out.append(it); onDelta(it)
                                        }
                                        "error" -> error(ev.optString("message").ifBlank { "Stream error" })
                                        "done" -> done = true
                                    }
                                }
                            }
                        }
                    }
                    if (out.isEmpty()) error("Empty reply")
                    out.toString()
                } finally {
                    conn.disconnect()
                }
            }
        }

    suspend fun send(turns: List<ChatTurn>): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val conn = open("application/json")
            val body = body(turns, false)
            try {
                conn.outputStream.use { it.write(body.toString().toByteArray()) }
                val code = conn.responseCode
                val raw = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                val json = runCatching { JSONObject(raw) }.getOrNull()
                if (code == 401) {
                    onUnauthorized()
                    error("Session expired — please sign in again")
                }
                if (code !in 200..299) error(json?.optString("error")?.ifBlank { null } ?: "HTTP $code")
                json?.optString("text")?.ifBlank { null } ?: error("Empty reply")
            } finally {
                conn.disconnect()
            }
        }
    }
}
