package ai.orhan.feature.artifacts

import ai.orhan.data.artifacts.ArtifactType
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ArtifactEditorStateTest {
    @Test fun failedSaveKeepsEveryDraftFieldAndAllowsRetry() = runBlocking {
        val draft = ArtifactEditorState("Changed", "# Kept source", ArtifactType.MARKDOWN)
        assertFalse(draft.save { _, _, _ -> false })
        assertEquals("Changed", draft.title)
        assertEquals("# Kept source", draft.content)
        assertEquals(ArtifactType.MARKDOWN, draft.type)
        assertNotNull(draft.error)
        assertFalse(draft.saving)
        assertTrue(draft.save { title, content, type ->
            title == "Changed" && content == "# Kept source" && type == ArtifactType.MARKDOWN
        })
        assertNull(draft.error)
    }

    @Test fun thrownFailureKeepsDraftAndCancellationDoesNotBecomeSuccess() = runBlocking {
        val draft = ArtifactEditorState("HTML", "<h1>Kept</h1>", ArtifactType.HTML)
        assertFalse(draft.save { _, _, _ -> throw IOException("disk") })
        assertEquals("<h1>Kept</h1>", draft.content)
        assertFalse(draft.saving)
        try { draft.save { _, _, _ -> throw CancellationException() }; fail("Cancelled") }
        catch (_: CancellationException) { }
        assertFalse(draft.saving)
        assertEquals(ArtifactType.HTML, draft.type)
    }

    @Test fun deleteFailureKeepsConfirmationAndOnlySuccessClosesIt() = runBlocking {
        val state = ArtifactDeleteState().apply { confirming = true }
        assertFalse(state.delete { false })
        assertTrue(state.confirming)
        assertNotNull(state.error)
        assertFalse(state.deleting)
        assertFalse(state.delete { throw IOException("disk") })
        assertTrue(state.confirming)
        assertTrue(state.delete { true })
        assertFalse(state.confirming)
        assertNull(state.error)
    }
}
