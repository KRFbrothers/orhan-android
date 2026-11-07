package ai.orhan.data.chat

import ai.orhan.BuildConfig
import ai.orhan.data.auth.AuthRepository
import android.util.Base64
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Credentials and their rejection action belong to ONE request, never shared mutable last-token state. */
class ChatCredential(val token: String, val onUnauthorized: suspend () -> Unit = {})

internal suspend fun AuthRepository.chatCredential(): ChatCredential? = requestToken()?.let { request ->
    ChatCredential(request.accessToken) { clearIfCurrent(request) }
}

/** One turn sent to the Dua engine. [role] is "user" or "model". */
data class ChatTurn(val role: String, val text: String, val attachments: List<ChatAttachment> = emptyList())

data class AgentToolCapability(val id: String, val available: Boolean, val reason: String? = null)

/**
 * An inline file on a user turn. The engine accepts `attachments: [{mimeType, data}]` per message
 * (base64, max 10 per message, 4 MB each; image/, application/pdf, text/, audio/, video/).
 */
data class ChatAttachment(val name: String, val mimeType: String, val base64: String, val sizeBytes: Long) {
    companion object {
        const val MAX_BYTES = 4L * 1024 * 1024
        val SUPPORTED_PREFIXES = listOf("image/", "application/pdf", "text/", "audio/", "video/")
        val PICKER_TYPES = arrayOf("image/*", "application/pdf", "text/*", "audio/*", "video/*")
        fun isSupported(mime: String) = SUPPORTED_PREFIXES.any { mime.lowercase().startsWith(it) }
    }
}

/**
 * Talks to the Dua AI engine: POST {DUA_ENGINE_URL}/api/chat (streaming via [stream]; [send] is the non-stream fallback).
 * Request:  {"stream":false,"mode":"agent","systemInstruction":"...","messages":[{"role":"user|model","text":"..."}]}
 * (`systemInstruction` is optional and omitted when blank.)
 * Response: {"text":"..."} or {"error":"..."}
 *
 * [stream] sends `"stream":true` and parses the engine's SSE events
 * (`event: text` → `{"type":"text","delta":"..."}`, `error`, `done`, plus optional
 * `thinking` / `progress` activity events surfaced via `onActivity`).
 */
class ChatRepository(
    private val baseUrl: String = BuildConfig.DUA_ENGINE_URL,
    private val appToken: String = BuildConfig.DUA_APP_TOKEN,
    /** Supplies the Supabase user JWT (signed-in session, or DUA_DEV_JWT fallback in debug). */
    private val tokenProvider: suspend () -> String? = { BuildConfig.DUA_DEV_JWT.ifBlank { null } },
    /** Called when the engine rejects the token (HTTP 401), e.g. to clear the session. */
    private val onUnauthorized: suspend () -> Unit = {},
    private val credentialProvider: (suspend () -> ChatCredential?)? = null,
) {
    /** Thrown when streaming fails in a way a non-stream retry can't fix (e.g. 401). */
    class FatalChatException(message: String) : IOException(message)

    /**
     * The server cannot do SSE for this request (406/415 or a non-event-stream body), so a
     * single non-stream retry is safe. Timeouts, 5xx and mid-stream errors are NOT eligible:
     * retrying them would double load on a struggling engine (audit REL-02).
     */
    class FallbackEligibleStreamException(message: String) : IOException(message)

    companion object {
        fun isFallbackStatus(code: Int): Boolean = code == 406 || code == 415

        /**
         * Builds the /api/chat JSON. [systemInstruction] is sent as top-level `systemInstruction`
         * (Dua-ai #494) only when non-blank, so older engines just see the plain messages array.
         */
        internal fun body(turns: List<ChatTurn>, stream: Boolean, systemInstruction: String? = null,
                          allowedTools: List<String>? = null, mode: String = "agent", resumeRunId: String? = null) = JSONObject()
            .put("stream", stream)
            .put("mode", mode)
            .put("messages", JSONArray().apply {
                turns.takeLast(20).forEach { turn ->
                    val obj = JSONObject().put("role", turn.role).put("text", turn.text)
                    if (turn.role == "user" && turn.attachments.isNotEmpty()) {
                        obj.put("attachments", JSONArray().apply {
                            turn.attachments.forEach { put(JSONObject().put("mimeType", it.mimeType).put("data", it.base64)) }
                        })
                    }
                    put(obj)
                }
            })
            .apply {
                systemInstruction?.trim()?.takeIf { it.isNotEmpty() }?.let { put("systemInstruction", it) }
                allowedTools?.let { put("allowedTools", JSONArray(it.distinct())) }
                resumeRunId?.trim()?.takeIf { it.isNotEmpty() }?.let { put("resumeRun", it) }
            }
    }

    private suspend fun credential(): ChatCredential {
        if (credentialProvider != null) return credentialProvider.invoke()
            ?: throw FatalChatException("Sign in required (no Supabase JWT)")
        return ChatCredential(tokenProvider() ?: throw FatalChatException("Sign in required (no Supabase JWT)"), onUnauthorized)
    }

    private suspend fun open(accept: String): Pair<HttpURLConnection, ChatCredential> {
        require(baseUrl.isNotBlank()) { "DUA_ENGINE_URL is not set in local.properties" }
        val credential = credential()
        return (URL(baseUrl.trimEnd('/') + "/api/chat").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 120_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", accept)
            setRequestProperty("Authorization", "Bearer ${credential.token}")
            if (appToken.isNotBlank()) setRequestProperty("X-Dua-App-Token", appToken)
        } to credential
    }

    /** Loads the authenticated service catalog; unavailable built-ins remain visible but disabled. */
    suspend fun agentToolCapabilities(): Result<List<AgentToolCapability>> = withContext(Dispatchers.IO) {
        runCatching {
            require(baseUrl.isNotBlank()) { "DUA_ENGINE_URL is not set in local.properties" }
            val credential = credential()
            val conn = (URL(baseUrl.trimEnd('/') + "/api/agent/capabilities").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer ${credential.token}")
                if (appToken.isNotBlank()) setRequestProperty("X-Dua-App-Token", appToken)
            }
            try {
                val code = conn.responseCode
                val raw = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                if (code == 401) {
                    credential.onUnauthorized()
                    throw FatalChatException("Session expired — please sign in again")
                }
                val json = runCatching { JSONObject(raw) }.getOrNull()
                if (code !in 200..299) throw IOException(json?.optString("error")?.ifBlank { null } ?: "HTTP $code")
                val catalog = json?.optJSONArray("toolCatalog")
                    ?: throw IOException("The configured service does not support enforced agent tool selection yet.")
                List(catalog.length()) { index ->
                    val row = catalog.getJSONObject(index)
                    AgentToolCapability(row.getString("id"), row.optBoolean("available"), row.optString("reason").takeIf { it.isNotBlank() && it != "null" })
                }
            } finally { conn.disconnect() }
        }
    }

    private suspend fun requireEnforcedToolPolicy(selected: List<String>?) {
        if (selected == null) return
        val catalog = agentToolCapabilities().getOrThrow()
        val available = catalog.filter { it.available }.map { it.id }.toSet()
        val missing = selected.distinct().filterNot { it in available }
        if (missing.isNotEmpty()) throw IOException("Selected tools are unavailable: ${missing.joinToString()}")
    }

    /** Uses the signed-in engine's real model provider without enabling Agent tool dispatch. */
    suspend fun translateText(text: String, targetLanguage: String): Result<String> {
        val source = text.trim()
        val language = targetLanguage.trim().take(80)
        if (source.isEmpty() || language.isEmpty()) return Result.failure(IllegalArgumentException("Text and target language are required."))
        return plainTextRequest(
            prompt = source,
            instruction = "Translate the supplied text into $language. Preserve its meaning, names, formatting and tone. Return only the translation, with no explanation or quotation marks.",
        )
    }

    /** Stores audio with the signed-in owner, then uses Dua's configured transcription providers. */
    suspend fun transcribeAudio(attachment: ChatAttachment): Result<String> {
        if (!attachment.mimeType.lowercase().startsWith("audio/")) return Result.failure(IllegalArgumentException("Choose an audio attachment to transcribe."))
        return withContext(Dispatchers.IO) { runCatching { transcribeStoredAudio(attachment) } }
    }

    private suspend fun transcribeStoredAudio(attachment: ChatAttachment): String {
        val apiBase = BuildConfig.DUA_API_URL.trimEnd('/')
        require(apiBase.startsWith("https://")) { "DUA_API_URL must be configured with HTTPS for stored audio transcription." }
        val credential = credential()
        val bytes = Base64.decode(attachment.base64, Base64.DEFAULT)
        if (bytes.isEmpty() || bytes.size.toLong() != attachment.sizeBytes || bytes.size > ChatAttachment.MAX_BYTES) {
            throw IOException("The saved audio attachment is incomplete or exceeds the supported size.")
        }

        var storedPath: String? = null
        val transcript: String
        try {
            val sign = serviceJson(apiBase, "/api/files/sign", credential,
                JSONObject().put("name", attachment.name).put("mimeType", attachment.mimeType).put("size", bytes.size))
            if (sign.code !in 200..299) throw serviceError(sign)
            val upload = sign.json ?: throw IOException("File storage returned an invalid upload response.")
            val path = upload.optString("path").takeIf(String::isNotBlank)
                ?: throw IOException("File storage did not return an audio path.")
            storedPath = path
            val uploadUrl = URL(upload.getString("uploadUrl"))
            if (uploadUrl.protocol != "https" || uploadUrl.userInfo != null) throw IOException("File storage returned an unsafe upload URL.")
            val headers = upload.optJSONObject("headers") ?: throw IOException("File storage omitted upload headers.")
            val put = (uploadUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                instanceFollowRedirects = false
                connectTimeout = 15_000
                readTimeout = 120_000
                doOutput = true
                headers.keys().forEach { key -> setRequestProperty(key, headers.getString(key)) }
            }
            try {
                put.outputStream.use { it.write(bytes) }
                val code = put.responseCode
                if (code !in 200..299) throw IOException("Private audio upload failed (HTTP $code).")
            } finally { put.disconnect() }

            val result = serviceJson(apiBase, "/api/files/transcribe", credential,
                JSONObject().put("path", path).put("name", attachment.name).put("mimeType", attachment.mimeType).put("size", bytes.size))
            if (result.code !in 200..299) throw serviceError(result)
            transcript = result.json?.optString("transcript")?.trim()?.takeIf(String::isNotEmpty)
                ?: throw IOException("The configured transcription provider returned no transcript.")
        } catch (failure: Throwable) {
            val cleanupFailure = storedPath?.let { path -> runCatching { deleteStoredUpload(apiBase, credential, path) }.exceptionOrNull() }
            if (cleanupFailure != null) {
                failure.addSuppressed(cleanupFailure)
                if (failure !is kotlinx.coroutines.CancellationException) {
                    throw IOException("${failure.message ?: "Transcription failed."} The temporary audio file may still be stored; check File storage.", failure)
                }
            }
            throw failure
        }
        try {
            deleteStoredUpload(apiBase, credential, storedPath!!)
        } catch (failure: Throwable) {
            throw IOException("Transcription finished, but removal of its temporary private audio file was not confirmed. Check File storage before retrying.", failure)
        }
        return transcript
    }

    private data class ServiceResponse(val code: Int, val json: JSONObject?)

    private suspend fun serviceJson(apiBase: String, path: String, credential: ChatCredential, body: JSONObject): ServiceResponse {
        val connection = (URL(apiBase + path).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 180_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer ${credential.token}")
            setRequestProperty("X-Dua-Native", "1")
        }
        return try {
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val raw = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code == 401) credential.onUnauthorized()
            ServiceResponse(code, runCatching { JSONObject(raw) }.getOrNull())
        } finally { connection.disconnect() }
    }

    private fun serviceError(response: ServiceResponse): IOException = IOException(
        response.json?.optString("error")?.takeIf(String::isNotBlank) ?: "File service request failed (HTTP ${response.code}).",
    )

    private suspend fun deleteStoredUpload(apiBase: String, credential: ChatCredential, path: String) {
        val deleted = serviceJson(apiBase, "/api/files/delete", credential, JSONObject().put("paths", JSONArray().put(path)))
        if (deleted.code !in 200..299) throw serviceError(deleted)
        val json = deleted.json ?: throw IOException("File service returned an invalid cleanup response.")
        if (json.optInt("deleted") != 1) throw IOException("File service did not confirm removal of the temporary audio file.")
    }

    /** Transcribes the source recording, then asks the configured Gemini model for factual minutes. */
    suspend fun createMeetingMinutes(attachment: ChatAttachment): Result<String> {
        val transcript = transcribeAudio(attachment).getOrElse { return Result.failure(it) }
        if (transcript.trim().trim('[', ']').equals("no speech", ignoreCase = true) || transcript.equals("No speech detected.", ignoreCase = true)) {
            return Result.failure(IOException("No speech was detected in this recording."))
        }
        if (transcript.isBlank()) return Result.failure(IOException("The recording did not produce a transcript."))
        return plainTextRequest(
            prompt = "Create concise meeting minutes from this transcript.\n\n$transcript",
            instruction = "Create factual meeting minutes using only the supplied transcript. Use headings Summary, Decisions, Action items, and Open questions. Mark missing owners or dates as unspecified. Do not invent attendees, decisions, dates, or actions. Return only the minutes.",
        )
    }

    private suspend fun plainTextRequest(prompt: String, instruction: String, attachment: ChatAttachment? = null): Result<String> {
        val output = StringBuilder()
        return stream(
            turns = listOf(ChatTurn("user", prompt, listOfNotNull(attachment))),
            systemInstruction = instruction,
            mode = "auto",
            onDelta = { output.append(it) },
        ).map { output.toString().trim() }
    }

    /**
     * Streams a reply. [onDelta] is called (on IO) with each text chunk. Returns the full text.
     * [onActivity] receives a short label for `thinking` / `progress` events (no-op if never sent).
     * Fails with [FatalChatException] for auth problems and [FallbackEligibleStreamException] only
     * when the server cannot stream (406/415/non-SSE); every other failure is surfaced as-is.
     */
    suspend fun stream(
        turns: List<ChatTurn>,
        systemInstruction: String? = null,
        allowedTools: List<String>? = null,
        mode: String = "agent",
        resumeRunId: String? = null,
        onActivity: suspend (String) -> Unit = {},
        onAgentRunId: suspend (String) -> Unit = {},
        onDelta: suspend (String) -> Unit,
    ): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                requireEnforcedToolPolicy(allowedTools)
                val (conn, credential) = open("text/event-stream")
                // Blocking SSE reads ignore coroutine cancel; disconnect on cancel so Stop is immediate.
                val watcher = launch { try { awaitCancellation() } finally { conn.disconnect() } }
                try {
                    conn.outputStream.use { it.write(body(turns, true, systemInstruction, allowedTools, mode, resumeRunId).toString().toByteArray()) }
                    val code = conn.responseCode
                    if (code == 401) {
                        credential.onUnauthorized()
                        throw FatalChatException("Session expired — please sign in again")
                    }
                    if (isFallbackStatus(code)) throw FallbackEligibleStreamException("HTTP $code")
                    if (code !in 200..299) {
                        val raw = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                        val error = runCatching { JSONObject(raw) }.getOrNull()
                        val unavailable = error?.optJSONArray("tools")?.let { ids ->
                            List(ids.length()) { ids.optString(it) }.filter(String::isNotBlank)
                        }.orEmpty()
                        val message = if (error?.optString("code") == "tool_unavailable" && unavailable.isNotEmpty())
                            "These selected tools are no longer available: ${unavailable.joinToString()}"
                        else error?.optString("error")?.ifBlank { null } ?: "HTTP $code"
                        throw IOException(message)
                    }
                    if (conn.contentType?.contains("text/event-stream") != true) {
                        throw FallbackEligibleStreamException("Not an SSE response")
                    }
                    val out = StringBuilder()
                    var done = false
                    val data = StringBuilder()
                    var eventName: String? = null
                    conn.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                        while (!done) {
                            currentCoroutineContext().ensureActive()
                            val line = reader.readLine() ?: break
                            when {
                                line.startsWith("event:") -> eventName = line.removePrefix("event:").trim()
                                line.startsWith("data:") -> {
                                    if (data.isNotEmpty()) data.append('\n')
                                    data.append(line.removePrefix("data:").removePrefix(" "))
                                }
                                line.isEmpty() -> {
                                    if (data.isEmpty()) { eventName = null; continue }
                                    val ev = try {
                                        val tokens = JSONTokener(data.toString())
                                        val value = tokens.nextValue() as? JSONObject
                                            ?: throw IOException("Expected a JSON stream event")
                                        if (tokens.nextClean() != '\u0000') throw IOException("Trailing stream event data")
                                        value
                                    } catch (error: Exception) {
                                        throw IOException("Invalid stream event. The reply may be incomplete.", error)
                                    }
                                    data.setLength(0)
                                    val type = ev.optString("type").ifBlank { eventName.orEmpty() }
                                    eventName = null
                                    when (type) {
                                        "text" -> ev.optString("delta").takeIf { it.isNotEmpty() }?.let {
                                            out.append(it); onDelta(it)
                                        }
                                        "error" -> error(ev.optString("message").ifBlank { "Stream error" })
                                        "done" -> {
                                            if (ev.optBoolean("truncated")) throw IOException("The service stopped at its output limit. The reply is incomplete.")
                                            done = true
                                        }
                                        "thinking", "progress" -> SseActivity.label(
                                            type,
                                            field = { k -> ev.optString(k) },
                                            nested = { k -> ev.optJSONObject("data")?.optString(k).orEmpty() },
                                        )?.let { onActivity(it) }
                                        "agent_session" -> ev.optString("runId")
                                            .takeIf { it.isNotBlank() && ev.optBoolean("resumable") }
                                            ?.let { onAgentRunId(it) }
                                    }
                                }
                            }
                        }
                    }
                    currentCoroutineContext().ensureActive()
                    // EOF is not completion, including a done frame missing its blank-line delimiter.
                    if (!done) throw IOException("Reply interrupted before completion. Please try again when ready.")
                    if (out.isEmpty()) error("Empty reply")
                    out.toString()
                } finally {
                    watcher.cancel()
                    conn.disconnect()
                }
            }.onFailure { if (it is CancellationException) throw it }
        }

    suspend fun send(turns: List<ChatTurn>, systemInstruction: String? = null,
                     allowedTools: List<String>? = null): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            requireEnforcedToolPolicy(allowedTools)
            val (conn, credential) = open("application/json")
            val body = body(turns, false, systemInstruction, allowedTools)
            try {
                conn.outputStream.use { it.write(body.toString().toByteArray()) }
                val code = conn.responseCode
                val raw = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                val json = runCatching { JSONObject(raw) }.getOrNull()
                if (code == 401) {
                    credential.onUnauthorized()
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
