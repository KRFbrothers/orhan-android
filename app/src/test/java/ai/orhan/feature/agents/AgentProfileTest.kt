package ai.orhan.feature.agents

import ai.orhan.data.agents.SavedAgent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class AgentProfileTest {
    @Test fun builtInOrhanHasAProfileWithoutASavedRecord() {
        val profile = resolveAgentProfile("orhan", emptyList())!!
        assertEquals("Orhan", profile.agent.name)
        assertNull(profile.savedAgent)
    }

    @Test fun savedAgentKeepsItsIdentityInstructionsAndAvatar() {
        val agent = SavedAgent("Writer / 100%", "Writes notes", "Keep it brief", createdAt = 1,
            avatarFileName = "123e4567-e89b-12d3-a456-426614174000.img")
        val profile = resolveAgentProfile("writer / 100%", listOf(agent))!!
        assertSame(agent, profile.savedAgent)
        assertEquals(agent.name, profile.agent.name)
    }

    @Test fun savedAgentShadowsTheBuiltInAsItDoesInTheAgentsList() {
        val agent = SavedAgent("Orhan", "My assistant", "My instructions", createdAt = 1)
        val profile = resolveAgentProfile("Orhan", listOf(agent))!!
        assertSame(agent, profile.savedAgent)
        assertEquals("My assistant", profile.agent.blurb)
    }

    @Test fun missingCustomAgentDoesNotResolveToAnotherAgent() {
        val other = SavedAgent("Editor", "Edits notes", createdAt = 1)
        assertNull(resolveAgentProfile("Deleted writer", listOf(other)))
        assertNull(resolveAgentProfile("Deleted writer", emptyList()))
    }
}
