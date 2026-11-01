package ai.orhan.data.memory

import ai.orhan.data.history.StoredMessage
import kotlinx.coroutines.flow.first
import kotlin.coroutines.cancellation.CancellationException

/**
 * Automatic memory is off until the user turns it on.
 *
 * Enabling records [MemoryPause.automaticEnabledAt]. Only a user message saved
 * after that instant is eligible. Older chats are not scanned. Pause, or a
 * preference file that cannot be read, stops extraction and stops topic text
 * from being added to a chat. A message saved while paused stays unsaved after
 * pause is turned off. Topics are still omitted unless the user also turns on
 * "Use topics in chats".
 */

const val AUTOMATIC_TOPIC_GROUP = "Chats"
const val AUTOMATIC_TOPIC_TAG = "auto"
internal const val MIN_DURABLE_CHARS = 12
internal const val MAX_PROCESSED_MESSAGES = 400

private val DURABLE_STATEMENT = Regex(
    """(?i)(\bi(?:'m| am| live| work| prefer| like| use| have| don't| do not| need)\b|\bmy (?:name|email|phone|job|role|timezone|city)\b|\bremember (?:that|this)\b)""",
)

internal data class ExtractedMemory(
    val title: String,
    val summary: String,
    val content: String,
)

enum class AutomaticSaveResult { Saved, Duplicate, NotUseful, WriteFailed }

/** True only when the user opted in, pause is off, and the preference file was read. */
internal fun extractionAllowed(state: MemoryPause): Boolean =
    state.automaticEnabled &&
        !state.paused &&
        !state.unreadable &&
        state.automaticEnabledAt > 0L

internal fun sourceKey(chatTitle: String, messageId: Long): String =
    chatTitle.replace('\n', ' ').replace('\r', ' ') + "\u001f" + messageId

internal fun rememberProcessedLine(existing: String, key: String, max: Int = MAX_PROCESSED_MESSAGES): String {
    val lines = existing.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
    if (key in lines) return lines.joinToString("\n")
    lines.add(key)
    return lines.takeLast(max).joinToString("\n")
}

/** User messages created after the enable instant. A missing timestamp is never eligible. */
internal fun eligibleUserMessages(messages: List<StoredMessage>, enabledAt: Long): List<StoredMessage> =
    messages.filter { it.role == "user" && it.id != 0L && it.createdAt > enabledAt }

/** Drops a leading attachment label so the saved fact is the user's words. */
internal fun chatTextForMemory(shown: String): String {
    val lines = shown.lines()
    val body = if (lines.firstOrNull()?.startsWith("\uD83D\uDCCE ") == true) {
        lines.drop(1).joinToString("\n")
    } else {
        shown
    }
    return body.trim()
}

internal fun normalizeMemoryText(raw: String): String =
    raw.trim().replace(Regex("\\s+"), " ").lowercase()

/**
 * Keeps a durable first-person statement copied from [shown]. Returns null
 * when the text is a question, too short, or not a statement. The result never
 * adds words that were not in the message.
 */
internal fun extractDurableStatement(shown: String): ExtractedMemory? {
    val content = chatTextForMemory(shown)
    if (content.length < MIN_DURABLE_CHARS || content.length > MAX_NOTE_CONTENT) return null
    if ('?' in content) return null
    if (!DURABLE_STATEMENT.containsMatchIn(content)) return null
    val cut = content.indexOfAny(charArrayOf('.', '!', '\n'))
    val title = (if (cut <= 0) content else content.substring(0, cut)).trim().take(MAX_ENTRY_TITLE)
    if (title.isBlank() || !content.startsWith(title)) return null
    val extracted = ExtractedMemory(
        title = title,
        summary = title.take(MAX_SUMMARY),
        content = content,
    )
    return extracted.takeIf { isValidExtraction(content, it) }
}

/** Rejects a candidate whose text is not exactly the user's cleaned message. */
internal fun isValidExtraction(userText: String, extracted: ExtractedMemory): Boolean {
    if (extracted.title.isBlank() || extracted.summary.isBlank() || extracted.content.isBlank()) return false
    if (extracted.content != userText) return false
    if ('?' in extracted.content) return false
    if (!extracted.content.startsWith(extracted.title)) return false
    if (extracted.summary != extracted.title.take(MAX_SUMMARY)) return false
    return true
}

internal fun isDuplicateMemory(
    topics: List<MemoryTopic>,
    extracted: ExtractedMemory,
    chatTitle: String,
    messageId: Long,
): Boolean {
    if (messageId != 0L && topics.any { it.sourceChatTitle == chatTitle && it.sourceMessageId == messageId }) {
        return true
    }
    val key = normalizeMemoryText(extracted.content)
    if (key.isEmpty()) return true
    return topics.any { topic ->
        normalizeMemoryText(topic.content) == key ||
            normalizeMemoryText(topic.summary) == key ||
            normalizeMemoryText(topic.title) == key
    }
}

/**
 * Topic titles and summaries for the agent prompt.
 * Null when the user has not opted in, when memory is paused, or when there is nothing to send.
 * Personal profile text stays on its own pause check and is not included here.
 */
const val AUTOMATIC_SAVE_ERROR = "Couldn't save a memory from this chat. It was not added."

/**
 * Runs after a chat thread is saved. Does nothing unless automatic memory is on
 * and the preference file was read. While pause is on, an eligible message is
 * marked processed and not saved, so turning pause off later does not mine it.
 * Each other eligible message is marked after a decision so a restart does not
 * extract it again. A failed topic write is left unmarked so a later save can
 * retry, and it records [AUTOMATIC_SAVE_ERROR] instead of reporting success.
 */
class AutomaticMemoryCoordinator(
    private val pauseStore: MemoryPauseStore,
    private val entries: MemoryEntriesStore,
) {
    suspend fun onMessagesSaved(chatTitle: String, messages: List<StoredMessage>) {
        try {
            val state = pauseStore.state.first()
            if (state.unreadable || !state.automaticEnabled || state.automaticEnabledAt <= 0L) return
            val eligible = eligibleUserMessages(messages, state.automaticEnabledAt)
            if (state.paused) {
                skipWhilePaused(chatTitle, eligible, state.processedMessages)
                return
            }
            val seen = state.processedMessages.toMutableSet()
            for (message in eligible) {
                val key = sourceKey(chatTitle, message.id)
                if (key in seen) continue
                when (entries.addAutomaticTopic(message.text, chatTitle, message.id, message.createdAt)) {
                    AutomaticSaveResult.Saved -> {
                        pauseStore.rememberProcessed(key)
                        pauseStore.clearLastError()
                        seen.add(key)
                    }
                    AutomaticSaveResult.Duplicate, AutomaticSaveResult.NotUseful -> {
                        pauseStore.rememberProcessed(key)
                        seen.add(key)
                    }
                    AutomaticSaveResult.WriteFailed -> pauseStore.setLastError(AUTOMATIC_SAVE_ERROR)
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            pauseStore.setLastError(AUTOMATIC_SAVE_ERROR)
        }
    }

    private suspend fun skipWhilePaused(
        chatTitle: String,
        eligible: List<StoredMessage>,
        already: Set<String>,
    ) {
        val seen = already.toMutableSet()
        for (message in eligible) {
            val key = sourceKey(chatTitle, message.id)
            if (key in seen) continue
            pauseStore.rememberProcessed(key)
            seen.add(key)
        }
    }
}

/**
 * Topic titles and summaries for the agent prompt.
 * Null when the user has not opted in, when memory is paused, or when there is nothing to send.
 * Personal profile text stays on its own pause check and is not included here.
 */
internal fun topicContextBlock(
    topics: List<MemoryTopic>,
    include: Boolean,
    paused: Boolean,
): String? {
    if (!include || paused) return null
    val lines = topics.mapNotNull { topic ->
        val title = topic.title.trim()
        if (title.isEmpty()) return@mapNotNull null
        val detail = topic.summary.trim().ifBlank { topic.content.trim() }.take(160)
        if (detail.isEmpty()) title else "$title: $detail"
    }.take(12)
    if (lines.isEmpty()) return null
    return "Saved topics (use when relevant):\n" + lines.joinToString("\n") { "- $it" }
}
