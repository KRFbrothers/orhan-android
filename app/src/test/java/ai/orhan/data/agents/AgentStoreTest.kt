package ai.orhan.data.agents

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

class AgentStoreTest {
    @Test fun cappedLimitsFields() {
        val a = SavedAgent(" " + "n".repeat(200), "b".repeat(500), "i".repeat(20_000), createdAt = 1).capped()
        assertEquals(MAX_AGENT_NAME, a.name.length)
        assertEquals(MAX_AGENT_BLURB, a.blurb.length)
        assertEquals(MAX_AGENT_INSTRUCTIONS, a.instructions.length)
    }

    @Test fun longInstructionsRoundTrip() = runBlocking {
        val big = "é".repeat(40_000) // > 64KB in UTF-8
        val out = ByteArrayOutputStream()
        SavedAgentsSerializer.writeTo(listOf(SavedAgent("W", "b", big, listOf("web"), 5)), out)
        val back = SavedAgentsSerializer.readFrom(out.toByteArray().inputStream())
        assertEquals(big, back.single().instructions)
        assertEquals(listOf("web"), back.single().tools)
    }

    @Test fun readsV1Format() = runBlocking {
        val out = ByteArrayOutputStream()
        DataOutputStream(out).apply {
            writeInt(1); writeInt(1); writeUTF("Writer"); writeUTF("blurb"); writeUTF("instr")
            writeInt(0); writeLong(9); flush()
        }
        val back = SavedAgentsSerializer.readFrom(out.toByteArray().inputStream()).single()
        assertEquals("instr", back.instructions)
        assertEquals("Writer", back.name)
    }

    @Test fun canonicalTitleIsCaseInsensitive() {
        assertEquals("Writer", canonicalAgentTitle("writer", listOf("Writer", "Coder")))
        assertEquals("Unknown", canonicalAgentTitle("Unknown", listOf("Writer")))
    }
}
