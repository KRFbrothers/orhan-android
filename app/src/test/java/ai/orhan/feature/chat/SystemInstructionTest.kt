package ai.orhan.feature.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemInstructionTest {
    @Test fun joinsNonBlankBlocks() {
        assertEquals("identity\n\nprofile", systemInstructionOf(listOf(" identity ", "", "profile")))
    }

    @Test fun profileLandsInSystemInstruction() {
        val s = systemInstructionOf(listOf("identity", userProfileSummary("Fareed", "Bio")!!))
        assertTrue(s.contains("User's name: Fareed"))
    }

    @Test fun systemInstructionModeEnabled() {
        assertTrue(USE_SYSTEM_INSTRUCTION)
    }
}
