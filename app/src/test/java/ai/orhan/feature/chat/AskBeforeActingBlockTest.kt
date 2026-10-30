package ai.orhan.feature.chat

import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AskBeforeActingBlockTest {
    @Test fun presentWhenOn() {
        assertTrue(askBeforeActingBlock(true)!!.contains("Ask the user before taking actions"))
    }

    @Test fun absentWhenOff() {
        assertNull(askBeforeActingBlock(false))
    }
}
