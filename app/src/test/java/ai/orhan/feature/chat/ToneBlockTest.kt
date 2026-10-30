package ai.orhan.feature.chat

import ai.orhan.data.prefs.AgentTone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ToneBlockTest {
    @Test fun directLine() = assertEquals("Keep a direct, no-fluff tone.", toneBlock(AgentTone.Direct))
    @Test fun professionalLine() = assertEquals("Keep a professional, polished tone.", toneBlock(AgentTone.Professional))
    @Test fun friendlyOmitted() = assertNull(toneBlock(AgentTone.Friendly))
    @Test fun parseFallsBackToFriendly() {
        assertEquals(AgentTone.Friendly, AgentTone.parse(null))
        assertEquals(AgentTone.Friendly, AgentTone.parse("junk"))
        assertEquals(AgentTone.Direct, AgentTone.parse("Direct"))
    }
}
