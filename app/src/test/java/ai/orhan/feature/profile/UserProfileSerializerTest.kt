package ai.orhan.feature.profile

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

class UserProfileSerializerTest {
    @Test fun roundTrip_v2_keepsRoleAndTimezone() = runBlocking {
        val p = UserProfile(name = "Fareed", email = "f@x.io", bio = "Builds agents", role = "Android designer", timezone = "Asia/Kolkata")
        val out = ByteArrayOutputStream()
        UserProfileSerializer.writeTo(p, out)
        assertEquals(p, UserProfileSerializer.readFrom(ByteArrayInputStream(out.toByteArray())))
    }

    @Test fun oldV1File_loadsWithBlankNewFields() = runBlocking {
        val out = ByteArrayOutputStream()
        DataOutputStream(out).apply {
            writeInt(1); writeUTF("Fareed"); writeUTF("f@x.io"); writeUTF(""); writeUTF("Old bio")
            writeInt(ProfileAvatars.first()); flush()
        }
        val p = UserProfileSerializer.readFrom(ByteArrayInputStream(out.toByteArray()))
        assertEquals("Fareed", p.name); assertEquals("Old bio", p.bio)
        assertEquals("", p.role); assertEquals("", p.timezone)
    }

    @Test fun unknownVersion_fallsBackToDefault() = runBlocking {
        val out = ByteArrayOutputStream()
        DataOutputStream(out).apply { writeInt(99); flush() }
        assertEquals(UserProfile(), UserProfileSerializer.readFrom(ByteArrayInputStream(out.toByteArray())))
    }
}
