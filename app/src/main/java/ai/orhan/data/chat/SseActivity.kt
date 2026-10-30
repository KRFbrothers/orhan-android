package ai.orhan.data.chat

/**
 * Pure (JVM-testable) helpers for SSE activity events (`thinking` / `progress`).
 * [field] looks up a string field of the decoded event (blank/absent = "").
 * [nested] optionally looks up a field inside a nested `data` object.
 */
object SseActivity {
    const val THINKING_DEFAULT = "Thinking…"
    const val PROGRESS_DEFAULT = "Working…"
    private const val MAX_LEN = 80

    fun isActivity(type: String?): Boolean = type == "thinking" || type == "progress"

    /** Returns a short label for an activity event, or null if [type] isn't thinking/progress. */
    fun label(type: String?, field: (String) -> String, nested: (String) -> String = { "" }): String? {
        if (!isActivity(type)) return null
        val keys = listOf("message", "label", "status", "step", "delta", "text")
        val raw = keys.firstNotNullOfOrNull { k -> field(k).trim().ifEmpty { null } }
            ?: keys.firstNotNullOfOrNull { k -> nested(k).trim().ifEmpty { null } }
        val text = raw?.lineSequence()?.firstOrNull { it.isNotBlank() }?.trim()
        return when {
            text.isNullOrEmpty() -> if (type == "thinking") THINKING_DEFAULT else PROGRESS_DEFAULT
            text.length > MAX_LEN -> text.take(MAX_LEN - 1).trimEnd() + "…"
            else -> text
        }
    }
}
