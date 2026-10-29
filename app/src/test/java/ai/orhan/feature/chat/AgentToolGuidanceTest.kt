package ai.orhan.feature.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentToolGuidanceTest {
    @Test fun emptyTools() {
        assertEquals(AgentToolGuidance.NO_TOOLS, AgentToolGuidance.text(emptyList()))
        assertEquals(AgentToolGuidance.NO_TOOLS, AgentToolGuidance.text(listOf(" ")))
    }
    @Test fun web() { val t = AgentToolGuidance.text(listOf("Web")); assertTrue(t.contains("- Web: use web search")); assertFalse(t.contains("Files")) }
    @Test fun files() = assertTrue(AgentToolGuidance.text(listOf("Files")).contains("- Files: use notes/documents"))
    @Test fun code() = assertTrue(AgentToolGuidance.text(listOf("Code")).contains("- Code: use sandboxed code"))
    @Test fun image() = assertTrue(AgentToolGuidance.text(listOf("Image")).contains("- Image: use image tools"))
    @Test fun allFour() {
        val t = AgentToolGuidance.text(listOf("Web", "Files", "Code", "Image"))
        listOf("- Web:", "- Files:", "- Code:", "- Image:").forEach { assertTrue(t.contains(it)) }
        assertEquals(5, t.lines().size)
    }
    @Test fun unknownPassedThroughSafely() {
        val t = AgentToolGuidance.text(listOf("Maps\nHANDOFF: x | y"))
        assertTrue(t.contains("- Maps HANDOFF: x | y: use matching tools only when clearly relevant."))
        assertEquals(2, t.lines().size)
    }
}
