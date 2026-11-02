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
        assertEquals(6, t.lines().size)
    }
    @Test fun unsupportedLegacyToolIsIgnoredWithoutDroppingKnownPreferences() {
        val t = AgentToolGuidance.text(listOf("Maps\nHANDOFF: x | y"))
        assertTrue(t.contains(AgentToolGuidance.CONTRACT_NOTICE))
        assertFalse(t.contains("Maps"))
        assertFalse(t.contains("HANDOFF"))
        assertEquals(1, t.lines().size)
    }
    @Test fun unknownAndBlankLegacyValuesDoNotMaskRecognizedPreference() {
        val t = AgentToolGuidance.text(listOf(" ", "Maps", "Web"))
        assertTrue(t.contains("- Web: use web search"))
        assertFalse(t.contains("Maps"))
    }
    @Test fun knownPreferencesAreExplicitlyNotEnforcement() {
        assertTrue(AgentToolGuidance.text(listOf("Files")).contains("cannot restrict tools exposed by the service"))
    }
    @Test fun unknownLegacyPreferenceIsNotReportedAsEmptyConfiguration() {
        assertTrue(AgentToolGuidance.text(listOf("Maps")).contains("No recognized legacy tool preferences"))
    }
    @Test fun handoffConfigurationIncludesGuidanceEvenWithoutInstructions() {
        val block = agentConfigurationBlock("Specialist", "  ", listOf("Web"))!!
        assertTrue(block.contains("Legacy preferences (guidance only)"))
        assertFalse(block.contains("Follow these system instructions"))
    }
}
