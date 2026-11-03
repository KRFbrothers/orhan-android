package ai.orhan.feature.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentInstructionsBlockTest {
    @Test fun blankInstructionsAddNoAgentSpecificBlock() {
        assertEquals(null, agentInstructionsBlock("Specialist", "  "))
    }

    @Test fun agentBlockContainsOnlyInstructionsAndNotLegacyToolNames() {
        val block = agentInstructionsBlock("Specialist", "Use concise language")!!
        assertTrue(block.contains("Specialist"))
        assertTrue(block.contains("Use concise language"))
        assertFalse(block.contains("Web"))
        assertFalse(block.contains("Files"))
        assertFalse(block.contains("Code"))
        assertFalse(block.contains("Image"))
    }
}
