package ai.orhan.data.workflows

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

class WorkflowSerializerTest {
    @Test fun v2PersistsStableAgentIdAndV1ReadsWithoutOne() = kotlinx.coroutines.runBlocking {
        val current = Workflow("w1", "Draft", "Write a draft", "Writer", 9L, "agent-id-1")
        val out = ByteArrayOutputStream()
        WorkflowsSerializer.writeTo(listOf(current), out)
        assertEquals(current, WorkflowsSerializer.readFrom(out.toByteArray().inputStream()).single())

        val legacy = ByteArrayOutputStream()
        DataOutputStream(legacy).apply {
            writeInt(1); writeInt(1); writeUTF("w1"); writeUTF("Draft"); writeUTF("Write a draft"); writeUTF("Writer"); writeLong(9L); flush()
        }
        val migrated = WorkflowsSerializer.readFrom(legacy.toByteArray().inputStream()).single()
        assertEquals("Writer", migrated.agentName)
        assertEquals(null, migrated.agentId)
    }
}
