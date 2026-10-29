package ai.orhan.feature.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HandoffParseTest {
    private val roster = listOf(RosterAgent("Slides", "Makes decks", ""))

    @Test fun parsesHandoffLine() {
        val r = parseHandoff("Sure, handing off.\nHANDOFF: slides | 5-slide deck on solar", roster)!!
        assertEquals("Slides", r.first.name)
        assertEquals("5-slide deck on solar", r.second)
        assertEquals("Sure, handing off.", stripHandoff("Sure, handing off.\nHANDOFF: slides | x"))
    }

    @Test fun ignoresUnknownAgent() {
        assertNull(parseHandoff("HANDOFF: Ghost | task", roster))
        assertNull(parseHandoff("no handoff here", roster))
    }
}
