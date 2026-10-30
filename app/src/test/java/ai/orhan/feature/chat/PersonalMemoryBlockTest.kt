package ai.orhan.feature.chat

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalMemoryBlockTest {
    @Test fun active_included() {
        assertTrue(personalMemoryBlock(" vegetarian ", paused = false)!!.contains("vegetarian"))
    }

    @Test fun paused_omitted() {
        assertNull(personalMemoryBlock("vegetarian", paused = true))
    }

    @Test fun blank_omitted() {
        assertNull(personalMemoryBlock("  ", paused = false))
    }

    @Test fun preface_skipsMemoryWhenPaused() {
        val blocks = listOfNotNull("identity", personalMemoryBlock("secret pref", paused = true))
        val p = prefacePair(blocks, "ok")
        assertFalse(p[0].text.contains("secret pref"))
        assertFalse(p[0].text.contains("Personal context"))
    }
}
