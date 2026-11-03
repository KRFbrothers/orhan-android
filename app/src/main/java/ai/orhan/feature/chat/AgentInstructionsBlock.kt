package ai.orhan.feature.chat

/** Builds identity/instructions only. Legacy tool selections are persisted but never sent as guidance. */
internal fun agentInstructionsBlock(name: String, instructions: String): String? =
    instructions.trim().takeIf { it.isNotEmpty() }?.let {
        "You are the agent named \"$name\". Follow these system instructions for the whole conversation:\n$it"
    }
