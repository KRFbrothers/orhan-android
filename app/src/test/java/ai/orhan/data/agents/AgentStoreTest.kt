package ai.orhan.data.agents

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException

private class MemoryAgents(initial: List<SavedAgent>) : DataStore<List<SavedAgent>> {
    val state = MutableStateFlow(initial)
    var updates = 0
    var failWrites = false
    override val data: Flow<List<SavedAgent>> = state
    override suspend fun updateData(transform: suspend (t: List<SavedAgent>) -> List<SavedAgent>): List<SavedAgent> {
        updates++
        val next = transform(state.value)
        if (failWrites) throw IOException("disk full")
        state.value = next
        return next
    }
}

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

    private fun agent(name: String, blurb: String = "blurb", instructions: String = "do it", createdAt: Long = 10) =
        SavedAgent(name, blurb, instructions, listOf("Web"), createdAt)

    @Test fun createEditAndRenameAreOneUpdate() = runBlocking {
        val memory = MemoryAgents(emptyList())
        val store = AgentStore(memory)

        val created = store.replace(null, agent(" Writer ", instructions = "i".repeat(20_000), createdAt = 4))
        val saved = (created as AgentSaveResult.Saved).agent
        assertEquals(MAX_AGENT_NAME.coerceAtMost("Writer".length), saved.name.trim().length)
        assertEquals("Writer", saved.name)
        assertEquals(MAX_AGENT_INSTRUCTIONS, saved.instructions.length)
        assertEquals(4L, saved.createdAt)
        assertEquals(1, memory.updates)

        val edited = store.replace("Writer", saved.copy(blurb = "sharper")) as AgentSaveResult.Saved
        assertEquals("sharper", edited.agent.blurb)
        assertEquals(4L, edited.agent.createdAt)
        assertEquals(listOf(edited.agent), memory.state.value)
        assertEquals(2, memory.updates)

        val coder = agent("Coder", createdAt = 3)
        memory.state.value = listOf(edited.agent, coder)
        val renamed = store.replace("Writer", edited.agent.copy(name = "Editor")) as AgentSaveResult.Saved
        assertEquals("Editor", renamed.agent.name)
        assertEquals(4L, renamed.agent.createdAt)
        assertEquals(listOf(renamed.agent, coder), memory.state.value)
        assertEquals(3, memory.updates)
        assertEquals(memory.state.value, (memory.state.value.replacing(renamed.agent, "Writer") as AgentWrite.Ok).agents)
    }

    @Test fun duplicateNameKeepsBothAgents() = runBlocking {
        val writer = agent("Writer", blurb = "original", createdAt = 1)
        val coder = agent("Coder", createdAt = 2)
        val memory = MemoryAgents(listOf(writer, coder))
        val store = AgentStore(memory)

        val created = store.replace(null, agent("writer", blurb = "other", createdAt = 9))
        assertEquals("An agent with this name already exists", (created as AgentSaveResult.Rejected).message)
        assertEquals(listOf(writer, coder), memory.state.value)

        val renamed = store.replace("Coder", coder.copy(name = "Writer", blurb = "taken"))
        assertTrue(renamed is AgentSaveResult.Rejected)
        assertEquals(listOf(writer, coder), memory.state.value)
        assertEquals(AgentSaveNav.Stay("An agent with this name already exists"), navigationAfterAgentSave(false, renamed))
    }

    @Test fun failedSavePreservesOriginalAndDraftNavigation() = runBlocking {
        val writer = agent("Writer", blurb = "keep me", createdAt = 8)
        val memory = MemoryAgents(listOf(writer))
        memory.failWrites = true
        val store = AgentStore(memory)

        val result = store.replace("Writer", writer.copy(name = "Editor", blurb = "draft"))
        assertEquals(
            "Couldn't save this agent. Your previous agent is unchanged.",
            (result as AgentSaveResult.Failed).message,
        )
        assertEquals(listOf(writer), memory.state.value)
        assertEquals(1, memory.updates)
        assertEquals(AgentSaveNav.Stay(result.message), navigationAfterAgentSave(creating = false, result))
        assertEquals(AgentSaveNav.Stay(result.message), navigationAfterAgentSave(creating = true, result))
    }

    @Test fun blankNameDoesNotWrite() = runBlocking {
        val writer = agent("Writer")
        val memory = MemoryAgents(listOf(writer))
        val result = AgentStore(memory).replace("Writer", writer.copy(name = "   "))
        assertEquals("Name is required", (result as AgentSaveResult.Rejected).message)
        assertEquals(listOf(writer), memory.state.value)
    }

    @Test fun createOpensChatAndEditCloses() {
        val saved = AgentSaveResult.Saved(agent("Editor"))
        assertEquals(AgentSaveNav.OpenChat("Editor"), navigationAfterAgentSave(creating = true, saved))
        assertEquals(AgentSaveNav.Close, navigationAfterAgentSave(creating = false, saved))
    }
}
