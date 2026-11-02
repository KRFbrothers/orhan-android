package ai.orhan.data.memory

import ai.orhan.data.artifacts.Artifact

/** How a topic's saved artifact id relates to the artifact store. The topic never copies the body. */
internal sealed class ArtifactLink {
    data object None : ArtifactLink()
    data object Missing : ArtifactLink()
    data class Ready(val id: String, val title: String) : ArtifactLink()
}

internal fun artifactLink(artifactId: String, artifacts: List<Artifact>): ArtifactLink {
    val id = artifactId.trim()
    if (id.isEmpty()) return ArtifactLink.None
    val found = artifacts.firstOrNull { it.id == id } ?: return ArtifactLink.Missing
    return ArtifactLink.Ready(found.id, found.title)
}

internal fun normalizedActionTitle(raw: String): String =
    raw.trim().replace(Regex("\\s+"), " ").lowercase()

/** First line of a chat message, kept as the action title. Blank when there is nothing to save. */
internal fun actionTitleFromMessage(text: String): String =
    text.trim().lineSequence().firstOrNull().orEmpty().trim().take(MAX_ENTRY_TITLE)

internal fun isDuplicateAction(todos: List<MemoryEntry>, title: String, ignoreId: String? = null): Boolean {
    val key = normalizedActionTitle(title)
    if (key.isEmpty()) return false
    return todos.any { it.id != ignoreId && normalizedActionTitle(it.title) == key }
}

internal const val MAX_DISMISSED_ACTIONS = 400

internal fun rememberDismissed(existing: List<String>, key: String, max: Int = MAX_DISMISSED_ACTIONS): List<String> {
    val clean = key.trim()
    if (clean.isEmpty() || clean in existing) return existing
    return (existing + clean).takeLast(max)
}
