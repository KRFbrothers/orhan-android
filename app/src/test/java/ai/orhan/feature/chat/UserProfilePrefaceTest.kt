package ai.orhan.feature.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserProfilePrefaceTest {
    @Test fun nameAndBio_bothIncluded() {
        val s = userProfileSummary(" Fareed ", "Builds agents")!!
        assertTrue(s.contains("User's name: Fareed"))
        assertTrue(s.contains("About user: Builds agents"))
    }

    @Test fun blankBio_omitted() {
        val s = userProfileSummary("Fareed", "  ")!!
        assertTrue(s.contains("User's name: Fareed"))
        assertFalse(s.contains("About user"))
    }

    @Test fun blankName_omitted() {
        val s = userProfileSummary("", "Hi")!!
        assertFalse(s.contains("User's name"))
        assertTrue(s.contains("About user: Hi"))
    }

    @Test fun allBlank_null() {
        assertNull(userProfileSummary(" ", null))
    }

    @Test fun prefaceStaysTwoTurns_withProfile() {
        val p = prefacePair(listOf("identity", userProfileSummary("Fareed", "Bio")!!), "ok")
        assertEquals(listOf("user", "model"), p.map { it.role })
        assertTrue(p[0].text.contains("User's name: Fareed"))
        assertTrue(p[0].text.contains("About user: Bio"))
    }
}
