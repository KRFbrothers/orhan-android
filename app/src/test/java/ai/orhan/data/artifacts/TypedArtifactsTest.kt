package ai.orhan.data.artifacts

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class TypedArtifactsTest {
    @Test fun legacyDocumentsStayPlainTextAfterMigrationAndEdit() = runBlocking {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).apply {
            writeInt(1); writeInt(1); writeUTF("old"); writeUTF("Kept")
            val body = "# Literally plain text".toByteArray(Charsets.UTF_8)
            writeInt(body.size); write(body); writeLong(42)
        }
        val migrated = ArtifactsSerializer.readFrom(bytes.toByteArray().inputStream()).single()
        assertEquals(ArtifactType.PLAIN_TEXT, migrated.type)
        assertEquals("# Literally plain text", migrated.content)
        val encoded = ByteArrayOutputStream()
        ArtifactsSerializer.writeTo(listOf(migrated.copy(title = "Edited")), encoded)
        val restored = ArtifactsSerializer.readFrom(encoded.toByteArray().inputStream()).single()
        assertEquals(migrated.copy(title = "Edited"), restored)
    }

    @Test fun allTypesRoundTripWithoutInferringFromContent() = runBlocking {
        val docs = ArtifactType.entries.map { Artifact(it.name, "Typed", "<h1>नमस्ते</h1>", 7, it) }
        val bytes = ByteArrayOutputStream()
        ArtifactsSerializer.writeTo(docs, bytes)
        assertEquals(docs, ArtifactsSerializer.readFrom(bytes.toByteArray().inputStream()))
    }

    @Test fun damagedAndFutureFilesCannotBecomeAnEmptyWritableStore() = runBlocking {
        for (bytes in listOf(byteArrayOf(0, 0, 0, 99), byteArrayOf(0, 0, 0, 2, 0, 0, 0, 1))) {
            try { ArtifactsSerializer.readFrom(bytes.inputStream()); fail("Unreadable data must fail") }
            catch (_: IOException) { }
        }
    }

    @Test fun exportUsesActualTypeAndKeepsSourceBytes() {
        val source = "# Heading\n\n**नमस्ते**"
        for ((type, mime, ext) in listOf(
            Triple(ArtifactType.PLAIN_TEXT, "text/plain", "txt"),
            Triple(ArtifactType.MARKDOWN, "text/markdown", "md"),
            Triple(ArtifactType.HTML, "text/html", "html"),
        )) {
            val file = Artifact("id", "../../Bad:name", source, 1, type).exportDocument()
            assertEquals(mime, file.mimeType)
            assertTrue(file.fileName.endsWith(".$ext"))
            assertFalse(file.fileName.contains('/'))
            assertFalse(file.fileName.contains(':'))
            assertArrayEquals(source.toByteArray(Charsets.UTF_8), file.bytes)
        }
    }
}
