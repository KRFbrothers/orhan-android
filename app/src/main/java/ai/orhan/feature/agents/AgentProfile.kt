package ai.orhan.feature.agents

import ai.orhan.data.agents.SavedAgent
import ai.orhan.data.agents.toInstalledAgent

/** A profile from the same saved-first roster used by the Agents list. */
data class ResolvedAgentProfile(
    val agent: InstalledAgent,
    val savedAgent: SavedAgent? = null,
)

fun resolveAgentProfile(name: String, savedAgents: List<SavedAgent>): ResolvedAgentProfile? {
    savedAgents.firstOrNull { it.name.equals(name, ignoreCase = true) }?.let {
        return ResolvedAgentProfile(it.toInstalledAgent(), it)
    }
    return SampleInstalledAgents.firstOrNull { it.name.equals(name, ignoreCase = true) }
        ?.let { ResolvedAgentProfile(it) }
}
