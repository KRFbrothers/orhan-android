package ai.orhan.data.artifacts

import androidx.datastore.core.DataStoreFactory
import java.io.DataOutputStream
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ArtifactDiskMigrationTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun legacyFileSurvivesTypedSaveReopenEditAndDelete() = runBlocking {
        val file = File(temporary.root, "artifacts.pb")
        DataOutputStream(file.outputStream()).use {
            it.writeInt(1); it.writeInt(1); it.writeUTF("legacy"); it.writeUTF("Old document")
            val source = "# Keep as plain text".toByteArray(Charsets.UTF_8)
            it.writeInt(source.size); it.write(source); it.writeLong(9)
        }
        suspend fun <T> reopen(action: suspend (ArtifactStore) -> T): T {
            val job = SupervisorJob()
            val data = DataStoreFactory.create(ArtifactsSerializer,
                scope = CoroutineScope(Dispatchers.IO + job), produceFile = { file })
            return try { action(ArtifactStore(data)) } finally { job.cancelAndJoin() }
        }
        val typed = Artifact("markdown", "Notes", "# Heading\n\n**Bold**", 12, ArtifactType.MARKDOWN)
        reopen { store ->
            val old = store.artifacts.first().single()
            assertEquals(ArtifactType.PLAIN_TEXT, old.type)
            assertEquals(9L, old.createdAt)
            assertTrue(store.upsert(typed))
        }
        reopen { store ->
            assertEquals(2, store.artifacts.first().size)
            assertEquals("# Keep as plain text", store.artifacts.first().first { it.id == "legacy" }.content)
            assertTrue(store.upsert(typed.copy(content = "# Edited")))
        }
        reopen { store ->
            assertEquals("# Edited", store.artifacts.first().first { it.id == "markdown" }.content)
            assertEquals(ArtifactType.MARKDOWN, store.artifacts.first().first { it.id == "markdown" }.type)
            assertTrue(store.delete("markdown"))
        }
        reopen { store ->
            val old = store.artifacts.first().single()
            assertEquals("legacy", old.id)
            assertEquals(ArtifactType.PLAIN_TEXT, old.type)
            assertEquals("# Keep as plain text", old.content)
        }
    }
}
