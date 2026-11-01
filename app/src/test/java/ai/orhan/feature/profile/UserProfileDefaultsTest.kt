package ai.orhan.feature.profile

import org.junit.Assert.assertEquals
import org.junit.Test

class UserProfileDefaultsTest {
    @Test fun defaultsAreBlank() {
        val p = UserProfile()
        assertEquals("", p.name); assertEquals("", p.email); assertEquals("", p.handle); assertEquals("", p.bio)
    }
    @Test fun blankNameUsesPlaceholders() {
        val p = UserProfile()
        assertEquals("You", p.displayName); assertEquals("?", p.initials)
    }
    @Test fun setNameShown() {
        val p = UserProfile(name = "Ali Raza")
        assertEquals("Ali Raza", p.displayName); assertEquals("AR", p.initials)
    }
}
