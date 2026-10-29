package ai.orhan.feature.chat

/**
 * Turns Create Agent tool chips (Web / Files / Code / Image) into concrete guidance for the model.
 * This is guidance only: the shared engine still exposes its full tool set in agent mode.
 */
object AgentToolGuidance {
    private val GUIDE = mapOf(
        "web" to "use web search and fetch pages when facts may be stale or need sources; cite what you found.",
        "files" to "use notes/documents tools when relevant; never invent file contents you have not read.",
        "code" to "use sandboxed code execution / coding tools when calculation, data work or code is needed.",
        "image" to "use image tools when the user asks to create or edit images."
    )

    const val NO_TOOLS =
        "Tools: none selected for this agent. Answer from your knowledge and the conversation; do not claim you ran tools you did not run."

    /** Returns the preface text for [tools]; blank labels dropped, unknown labels passed through safely. */
    fun text(tools: List<String>): String {
        val labels = tools.map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }
        if (labels.isEmpty()) return NO_TOOLS
        val lines = labels.map { label ->
            val safe = label.replace(Regex("[\\r\\n]+"), " ").take(40)
            "- $safe: " + (GUIDE[safe.lowercase()] ?: "use matching tools only when clearly relevant.")
        }
        return "Tools this agent should prefer (guidance, use when helpful):\n" + lines.joinToString("\n")
    }
}
