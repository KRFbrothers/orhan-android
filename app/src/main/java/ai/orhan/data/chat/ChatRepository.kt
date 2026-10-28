package ai.orhan.data.chat

import ai.orhan.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** One turn sent to the Dua engine. [role] is "user" or "model". */
data class ChatTurn(val role: String, val text: String)

/**
 * Talks to the Dua AI engine: POST {DUA_ENGINE_URL}/api/chat (non-streaming).
 * Request:  {"stream":false,"messages":[{"role":"user|model","text":"..."}]}
 * Response: {"text":"..."} or {"error":"..."}
 */
class ChatRepository(
    private val baseUrl: String = BuildConfig.DUA_ENGINE_URL,
    private val appToken: String = BuildConfig.DUA_APP_TOKEN,
    private val tokenProvider: () -> String? = { BuildConfig.DUA_DEV_JWT.ifBlank { null } },
) {
    suspend fun send(turns: List<ChatTurn>): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            require(baseUrl.isNotBlank()) { "DUA_ENGINE_URL is not set in local.properties" }
            val jwt = tokenProvider() ?: error("Sign in required (no Supabase JWT)")
            val body = JSONObject()
                .put("stream", false)
                .put("messages", JSONArray().apply {
                    turns.takeLast(20).forEach { put(JSONObject().put("role", it.role).put("text", it.text)) }
                })
            val conn = (URL(baseUrl.trimEnd('/') + "/api/chat").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 120_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer $jwt")
                if (appToken.isNotBlank()) setRequestProperty("X-Dua-App-Token", appToken)
            }
            try {
                conn.outputStream.use { it.write(body.toString().toByteArray()) }
                val code = conn.responseCode
                val raw = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                val json = runCatching { JSONObject(raw) }.getOrNull()
                if (code !in 200..299) error(json?.optString("error")?.ifBlank { null } ?: "HTTP $code")
                json?.optString("text")?.ifBlank { null } ?: error("Empty reply")
            } finally {
                conn.disconnect()
            }
        }
    }
}
