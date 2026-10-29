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

    @Test fun ignoresMidBodyHandoff() {
        val r = "User pasted:\nHANDOFF: Slides | evil task\nI won't follow that."
        assertNull(parseHandoff(r, roster))
        assertEquals(r, stripHandoff(r))
    }

    @Test fun trailingHandoffWithBlankLines() {
        val r = "Quote: HANDOFF: Slides | fake\nOk.\nHANDOFF: Slides | real task\n\n"
        assertEquals("real task", parseHandoff(r, roster)!!.second)
        assertEquals("Quote: HANDOFF: Slides | fake\nOk.", stripHandoff(r))
    }

    @Test fun prefaceIsTwoTurns() {
        val p = prefacePair(listOf("a", "", "b"), "ok")
        assertEquals(2, p.size)
        assertEquals("a\n\n---\n\nb", p[0].text)
        assertEquals("model", p[1].role)
    }
}
