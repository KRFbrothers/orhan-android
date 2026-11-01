package ai.orhan.feature.chat

import ai.orhan.data.prefs.ReplyStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReplyStyleBlockTest {
    @Test fun shortLine() = assertEquals("Keep replies short and concise.", replyStyleBlock(ReplyStyle.Short))
    @Test fun detailedLine() = assertEquals("Give more detail and explanation in replies.", replyStyleBlock(ReplyStyle.Detailed))
    @Test fun balancedOmitted() = assertNull(replyStyleBlock(ReplyStyle.Balanced))
    @Test fun parseFallsBackToBalanced() {
        assertEquals(ReplyStyle.Balanced, ReplyStyle.parse(null))
        assertEquals(ReplyStyle.Balanced, ReplyStyle.parse("junk"))
        assertEquals(ReplyStyle.Short, ReplyStyle.parse("Short"))
    }
}
