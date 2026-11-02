package ai.orhan.data.workflows

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkflowAgentResolutionTest {
    private val available = listOf("Orhan", "Writer", "Slides")

    @Test fun blankAgentResolvesToDefaultAssistant() {
        assertEquals("Orhan", resolveWorkflowAgentName("  ", available))
    }

    @Test fun nameResolvesCaseInsensitivelyToCanonicalAgent() {
        assertEquals("Writer", resolveWorkflowAgentName(" writer ", available))
    }

    @Test fun missingAgentIsNotSilentlyConvertedToWorkflowTitleOrDefault() {
        assertNull(resolveWorkflowAgentName("Deleted Agent", available))
    }
}
