package ai.orhan.data.artifacts

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * Local artifact documents. Fixtures are synthetic. A save stores the text
 * only; it does not call a chat or a tool.
 */
class ArtifactStoreTest {

    @Test fun roundTripKeepsIdTitleContentAndCreatedTime() = runBlocking {
        val original = listOf(Artifact("doc-1", "Notes", "hello", 40L))
        val bytes = ByteArrayOutputStream()
        ArtifactsSerializer.writeTo(original, bytes)
        val restored = ArtifactsSerializer.readFrom(bytes.toByteArray().inputStream())
        assertEquals(original, restored)
    }

    @Test fun emptyOrUnknownFileOpensAsNoDocuments() = runBlocking {
        assertEquals(emptyList<Artifact>(), ArtifactsSerializer.readFrom(ByteArray(0).inputStream()))
        val unknown = ByteArrayOutputStream()
        java.io.DataOutputStream(unknown).use { it.writeInt(99) }
        assertEquals(emptyList<Artifact>(), ArtifactsSerializer.readFrom(unknown.toByteArray().inputStream()))
    }

    @Test fun saveEditAndDeleteKeepTheIdAndCreatedTime() = runBlocking {
        val store = ArtifactStore(ArtifactDataStore(emptyList()))
        assertTrue(store.upsert(Artifact("doc-1", "  ", "alpha", 10L)))
        val created = store.artifacts.first().single()
        assertEquals("alpha", created.title)
        assertEquals(10L, created.createdAt)

        assertTrue(store.upsert(created.copy(title = "Renamed", content = "beta")))
        val edited = store.artifacts.first().single()
        assertEquals("doc-1", edited.id)
        assertEquals("Renamed", edited.title)
        assertEquals("beta", edited.content)
        assertEquals(10L, edited.createdAt)

        assertTrue(store.delete("missing"))
        assertEquals(1, store.artifacts.first().size)
        assertTrue(store.delete("doc-1"))
        assertTrue(store.artifacts.first().isEmpty())
    }

    @Test fun blankContentAndFailedWriteLeaveTheSavedList() = runBlocking {
        val data = ArtifactDataStore(listOf(Artifact("doc-1", "Kept", "body", 3L)))
        val store = ArtifactStore(data)
        assertFalse(store.upsert(Artifact("doc-2", "New", "   ", 4L)))
        assertEquals(listOf("doc-1"), store.artifacts.first().map { it.id })

        data.failWrites = true
        assertFalse(store.upsert(Artifact("doc-1", "Changed", "next", 3L)))
        assertFalse(store.delete("doc-1"))
        assertEquals("Kept", store.artifacts.first().single().title)
    }

    @Test fun searchMatchesTitleOrContentAndIgnoresBlankQueries() {
        val docs = listOf(
            Artifact("a", "Blue folder", "desk", 2L),
            Artifact("b", "Other", "BLUE note", 1L),
        )
        assertEquals(listOf("a", "b"), docs.matching("  ").map { it.id })
        assertEquals(listOf("a", "b"), docs.matching("blue").map { it.id })
        assertEquals(listOf("a"), docs.matching("folder").map { it.id })
        assertTrue(docs.matching("missing").isEmpty())
    }

    private class ArtifactDataStore(initial: List<Artifact>) : DataStore<List<Artifact>> {
        val state = MutableStateFlow(initial)
        var failWrites = false
        override val data: Flow<List<Artifact>> = state
        override suspend fun updateData(transform: suspend (t: List<Artifact>) -> List<Artifact>): List<Artifact> {
            val next = transform(state.value)
            if (failWrites) throw IOException("disk full")
            state.value = next
            return next
        }
    }
}
