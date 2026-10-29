package ai.orhan.data.history

import ai.orhan.design.screens.ChatMessage
import ai.orhan.design.screens.ChatRole
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatThreadStoreTest {
    private fun msgs(n: Int) = List(n) { ChatMessage(it.toLong(), if (it % 2 == 0) ChatRole.User else ChatRole.Agent, "m$it", "now") }

    @Test fun capsMessagesAndRoundTripsRoles() {
        val t = emptyList<ChatThread>().withSaved("a", msgs(150), 1).single()
        assertEquals(MAX_THREAD_MESSAGES, t.messages.size)
        assertEquals(msgs(150).takeLast(100), t.messages.map { it.toUi() })
    }

    @Test fun capsThreadsDroppingOldest() {
        var l = emptyList<ChatThread>()
        repeat(45) { l = l.withSaved("t$it", msgs(1), it.toLong()) }
        assertEquals(MAX_THREADS, l.size)
        assertEquals(false, l.any { it.title == "t0" })
    }
}

class ChatAttachmentPersistenceTest {
    private val att = ai.orhan.data.chat.ChatAttachment("note.m4a", "audio/mp4", "AQID", 3)
    private fun dec(s: String) = java.util.Base64.getDecoder().decode(s)
    private fun enc(b: ByteArray) = java.util.Base64.getEncoder().encodeToString(b)

    @Test fun serializerRoundTripsAttachmentMetadata() = kotlinx.coroutines.runBlocking {
        val m = StoredMessage(1, "user", "\uD83D\uDCCE note.m4a", "now", StoredAttachment("note.m4a", "audio/mp4", 3, "a_1.bin"))
        val list = listOf(ChatThread("t", 5, listOf(m, StoredMessage(2, "agent", "hi", "now"))))
        val out = java.io.ByteArrayOutputStream()
        ChatThreadsSerializer.writeTo(list, out)
        assertEquals(list, ChatThreadsSerializer.readFrom(out.toByteArray().inputStream()))
    }

    @Test fun filesRoundTripAndPrune() {
        val dir = java.nio.file.Files.createTempDirectory("att").toFile()
        val files = AttachmentFiles(dir)
        val s = files.put("t", 1, att, ::dec)!!
        assertEquals(att, files.read(s, ::enc))
        files.prune(emptySet())
        assertEquals(null, files.read(s, ::enc))
    }

    @Test fun pruneEnforcesDiskBudget() {
        val dir = java.nio.file.Files.createTempDirectory("att").toFile()
        val files = AttachmentFiles(dir, maxBytes = 4)
        val a = files.put("t", 1, att, ::dec)!!
        val b = files.put("t", 2, att, ::dec)!!
        java.io.File(dir, a.fileName).setLastModified(1000)
        files.prune(setOf(a.fileName, b.fileName))
        assertEquals(null, files.read(a, ::enc))
        assertEquals(att, files.read(b, ::enc))
    }

    @Test fun totalBytesAndCount() {
        val dir = java.nio.file.Files.createTempDirectory("att").toFile()
        val files = AttachmentFiles(dir)
        assertEquals(0L, files.totalBytes()); assertEquals(0, files.fileCount())
        files.put("t", 1, att, ::dec); files.put("t", 2, att, ::dec)
        assertEquals(6L, files.totalBytes()); assertEquals(2, files.fileCount())
        assertEquals(0L, AttachmentFiles(null).totalBytes())
    }

    @Test fun clearAttachmentsKeepsTextDropsFilesAndMetadata() = kotlinx.coroutines.runBlocking {
        val dir = java.nio.file.Files.createTempDirectory("att").toFile()
        val files = AttachmentFiles(dir)
        val s = files.put("t", 1, att, ::dec)!!
        val initial = listOf(ChatThread("t", 5, listOf(StoredMessage(1, "user", "hello", "now", s), StoredMessage(2, "agent", "hi", "now"))))
        val ds = object : androidx.datastore.core.DataStore<List<ChatThread>> {
            val state = kotlinx.coroutines.flow.MutableStateFlow(initial)
            override val data = state
            override suspend fun updateData(transform: suspend (List<ChatThread>) -> List<ChatThread>) =
                transform(state.value).also { state.value = it }
        }
        val store = ChatThreadStore(ds, files)
        assertEquals(3L to 1, store.attachmentUsage())
        store.clearAttachments()
        val after = ds.state.value.single()
        assertEquals("t", after.title); assertEquals(5L, after.updatedAt)
        assertEquals(listOf("hello", "hi"), after.messages.map { it.text })
        assertEquals(true, after.messages.all { it.attachment == null })
        assertEquals(0L to 0, store.attachmentUsage())
    }
}

class AttachmentFileNameTest {
    @Test fun collidingHashCodesGetDistinctNames() {
        org.junit.Assert.assertEquals("Aa".hashCode(), "BB".hashCode())
        org.junit.Assert.assertNotEquals(AttachmentFiles(null).fileNameFor("Aa", 1), AttachmentFiles(null).fileNameFor("BB", 1))
    }
    @Test fun nameIsStable() {
        assertEquals(AttachmentFiles(null).fileNameFor("Writer", 7), AttachmentFiles(null).fileNameFor("Writer", 7))
        assertEquals(16, AttachmentFiles(null).titleHash("Writer").length)
        assertEquals("a_" + AttachmentFiles(null).titleHash("x") + "_7.bin", AttachmentFiles(null).fileNameFor("x", 7))
    }
}
