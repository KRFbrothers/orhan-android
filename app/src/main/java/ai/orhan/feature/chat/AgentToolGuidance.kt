package ai.orhan.feature.chat

/**
 * Converts recognized legacy agent preferences into prompt guidance.
 * ChatRepository does not send a per-agent tool allowlist; this is not enforcement.
 */
object AgentToolGuidance {
    private val GUIDE = mapOf(
        "web" to "use web search and fetch pages when facts may be stale or need sources; cite what you found.",
        "files" to "use notes/documents tools when relevant; never invent file contents you have not read.",
        "code" to "use sandboxed code execution / coding tools when calculation, data work or code is needed.",
        "image" to "use image tools when the user asks to create or edit images."
    )

    const val CONTRACT_NOTICE =
        "The request does not include a per-agent tool allowlist. These preferences cannot restrict tools exposed by the service. Never claim a tool ran unless the service actually reports that it ran."

    const val NO_TOOLS = "$CONTRACT_NOTICE No legacy tool preferences are saved for this agent."

    const val UNKNOWN_TOOLS = "$CONTRACT_NOTICE No recognized legacy tool preferences are saved for this agent."

    /** Known legacy preferences may guide wording; unknown labels are not sent as tool names. */
    fun text(tools: List<String>): String {
        val labels = tools.map { it.trim() }
            .filter { it.isNotEmpty() && it.lowercase() in GUIDE }
            .distinctBy { it.lowercase() }
        if (labels.isEmpty()) return if (tools.any { it.isNotBlank() }) UNKNOWN_TOOLS else NO_TOOLS
        val lines = labels.map { label ->
            "- $label: " + GUIDE.getValue(label.lowercase())
        }
        return "$CONTRACT_NOTICE\nLegacy preferences (guidance only):\n" + lines.joinToString("\n")
    }
}

/** Keeps a specialist's instruction and legacy guidance together for both direct chats and handoffs. */
internal fun agentConfigurationBlock(name: String, instructions: String, tools: List<String>): String? {
    val blocks = buildList {
        instructions.trim().takeIf { it.isNotEmpty() }?.let {
            add("You are the agent named \"$name\". Follow these system instructions for the whole conversation:\n$it")
        }
        tools.takeIf { it.isNotEmpty() }?.let { add(AgentToolGuidance.text(it)) }
    }
    return blocks.takeIf { it.isNotEmpty() }?.joinToString("\n\n")
}
