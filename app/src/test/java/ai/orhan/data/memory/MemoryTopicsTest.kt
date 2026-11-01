package ai.orhan.data.memory

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException

/**
 * Topic storage and chat-context rules. Fixtures are synthetic. Nothing here is
 * sent to an agent except the personal-context string, and only when pause is off.
 */
class MemoryTopicsTest {

    @Test fun pauseKeyMatchesInstalledPreferencesFile() {
        assertEquals("orhan_memory_prefs", MEMORY_PAUSE_FILE)
        assertEquals("memory_paused", MEMORY_PAUSED_KEY.name)
    }

    @Test fun deleteTopicKeepsTheSavedListWhenTheWriteFails() = runBlocking {
        val kept = topic("1", "Keep me")
        val storeData = TopicDataStore(MemoryEntries(topics = listOf(kept)))
        val store = MemoryEntriesStore(storeData)
        storeData.failWrites = true
        assertFalse(store.deleteTopic("1"))
        assertEquals(listOf(kept), storeData.state.value.topics)
        storeData.failWrites = false
        assertTrue(store.deleteTopic("1"))
        assertTrue(storeData.state.value.topics.isEmpty())
    }

    @Test fun version1NotesBecomeTopicsOnceAndTodosStay() {
        val note = entry("n1", "Old note", "First line\nrest of the note", createdAt = 10)
        val todo = entry("t1", "Call back", "details", done = true, createdAt = 11)
        val once = decodeMemoryEntries(encodeV1(listOf(note), listOf(todo)).inputStream())

        assertTrue(once.notes.isEmpty())
        assertEquals(1, once.topics.size)
        assertEquals("n1", once.topics[0].id)
        assertEquals("Old note", once.topics[0].title)
        assertEquals("First line", once.topics[0].summary)
        assertEquals("First line\nrest of the note", once.topics[0].content)
        assertEquals(MIGRATED_NOTES_GROUP, once.topics[0].group)
        assertFalse(once.topics[0].unread)
        assertEquals(10L, once.topics[0].createdAt)
        assertEquals(listOf(todo), once.todos)

        val again = decodeMemoryEntries(encodeMemoryEntries(once).inputStream())
        assertEquals(once.topics, again.topics)
        assertEquals(once.todos, again.todos)
        assertTrue(again.notes.isEmpty())
    }

    @Test fun version2KeepsTopicsAndMergesOnlyNewNotes() {
        val kept = topic("same", "Kept title", content = "kept body", updatedAt = 4)
        val extra = entry("new-note", "From notes", "body", createdAt = 8)
        val duplicate = entry("same", "Should not replace", "other", createdAt = 9)
        val decoded = decodeMemoryEntries(
            encodeMemoryEntries(
                MemoryEntries(notes = listOf(extra, duplicate), todos = emptyList(), topics = listOf(kept)),
            ).inputStream(),
        )
        assertTrue(decoded.notes.isEmpty())
        assertEquals(listOf("same", "new-note"), decoded.topics.map { it.id })
        assertEquals("Kept title", decoded.topics.first { it.id == "same" }.title)
        assertEquals(MIGRATED_NOTES_GROUP, decoded.topics.first { it.id == "new-note" }.group)
    }

    @Test fun roundTripPreservesTagsUnreadAndGroup() {
        val original = MemoryEntries(
            todos = listOf(entry("todo", "Ship", done = false, createdAt = 3)),
            topics = listOf(
                topic(
                    id = "p1",
                    title = "Café plan",
                    summary = "short",
                    content = "é".repeat(20),
                    group = "Plans",
                    tags = listOf("#Work", "work", "HOME"),
                    unread = true,
                    createdAt = 1,
                    updatedAt = 2,
                ),
            ),
        )
        val back = decodeMemoryEntries(encodeMemoryEntries(original).inputStream())
        val saved = back.topics.single()
        assertEquals("Café plan", saved.title)
        assertEquals("short", saved.summary)
        assertEquals("é".repeat(20), saved.content)
        assertEquals("Plans", saved.group)
        assertEquals(listOf("work", "home"), saved.tags)
        assertTrue(saved.unread)
        assertEquals(1L, saved.createdAt)
        assertEquals(2L, saved.updatedAt)
        assertEquals(original.todos, back.todos)
    }

    @Test fun emptyUnknownAndTruncatedFilesDoNotThrow() {
        assertEquals(MemoryEntries(), decodeMemoryEntries(ByteArray(0).inputStream()))
        val unknown = ByteArrayOutputStream().also { DataOutputStream(it).writeInt(99) }.toByteArray()
        assertEquals(MemoryEntries(), decodeMemoryEntries(unknown.inputStream()))
        val truncated = ByteArrayOutputStream().also { DataOutputStream(it).writeInt(TOPICS_FORMAT_VERSION) }.toByteArray()
        assertEquals(MemoryEntries(), decodeMemoryEntries(truncated.inputStream()))
    }

    @Test fun userSaveClearsUnreadAndBlankTitleIsRejected() {
        assertNull(prepareTopic(null, "  ", "s", "c", "g", emptyList(), now = 5, newId = "x"))
        val created = prepareTopic(null, " Title ", " Sum ", " Body ", "  ", listOf("#Work", "work", "a", "b"), now = 5, newId = "x")
        assertEquals("Title", created!!.title)
        assertEquals("Sum", created.summary)
        assertEquals("Body", created.content)
        assertEquals(DEFAULT_TOPIC_GROUP, created.group)
        assertEquals(listOf("work", "a", "b"), created.tags)
        assertFalse(created.unread)
        assertEquals(5L, created.updatedAt)

        val reread = prepareTopic(created.copy(unread = true), "Title", "Sum", "Body", DEFAULT_TOPIC_GROUP, listOf("work", "a", "b"), now = 90, newId = "y")
        assertTrue(reread!!.unread)
        assertEquals(5L, reread.updatedAt)
        assertEquals("x", reread.id)

        val edited = prepareTopic(reread, "Title", "Sum", "Changed", DEFAULT_TOPIC_GROUP, reread.tags, now = 90, newId = "y")
        assertFalse(edited!!.unread)
        assertEquals(90L, edited.updatedAt)
    }

    @Test fun searchMatchesTitleContentAndTagsButNotGroup() {
        val topics = listOf(
            topic("1", "Alpha title", content = "plain", tags = listOf("home"), group = "Zebra", unread = true, updatedAt = 3),
            topic("2", "Other", summary = "summary-token", content = "body-token sits here", tags = emptyList(), unread = false, updatedAt = 2),
            topic("3", "Third", content = "plain", tags = listOf("tagtoken"), unread = true, updatedAt = 1),
        )
        assertEquals(listOf("1"), topics.matching("alpha", emptySet(), false).map { it.id })
        assertEquals(listOf("2"), topics.matching("BODY-token", emptySet(), false).map { it.id })
        assertEquals(listOf("2"), topics.matching("summary-token", emptySet(), false).map { it.id })
        assertEquals(listOf("3"), topics.matching("tagtoken", emptySet(), false).map { it.id })
        assertTrue(topics.matching("zebra", emptySet(), false).isEmpty())
        assertEquals(0, allCount(topics, "zzzqq", emptySet()))
        assertEquals(2, updatesCount(topics))
        assertEquals(1, allCount(topics, "", setOf("home")))
        assertEquals(2, updatesCount(topics))
        assertEquals(listOf("1"), topics.matching("", setOf("HOME"), true).map { it.id })
    }

    @Test fun timestampsUseAboutHoursThenDayAgoThenTheOlderLabel() {
        val now = 10_000_000L
        val day = 86_400_000L
        assertEquals("less than a minute ago", memoryTimestamp(now - 30_000L, now) { _, _ -> "older" })
        assertEquals("about 1 minute ago", memoryTimestamp(now - 60_000L, now) { _, _ -> "older" })
        assertEquals("about 2 minutes ago", memoryTimestamp(now - 120_000L, now) { _, _ -> "older" })
        assertEquals("about 1 hour ago", memoryTimestamp(now - 3_600_000L, now) { _, _ -> "older" })
        assertEquals("about 3 hours ago", memoryTimestamp(now - 3 * 3_600_000L, now) { _, _ -> "older" })
        assertEquals("1 day ago", memoryTimestamp(now - 25 * 3_600_000L, now) { _, _ -> "older" })
        assertEquals("5 days ago", memoryTimestamp(now - 5 * day, now) { _, _ -> "older" })
        assertEquals("6 days ago", memoryTimestamp(now - 6 * day, now) { _, _ -> "older" })
        assertEquals("older", memoryTimestamp(now - 7 * day, now) { _, _ -> "older" })
        assertEquals("older", memoryTimestamp(now + 5_000L, now) { _, _ -> "older" })
    }

    @Test fun updatesFeedIsFlatAndTheTabBadgeHidesZero() {
        val topics = listOf(
            topic("1", "A", group = "Zebra", unread = true, updatedAt = 3),
            topic("2", "B", group = "Alpha", unread = true, updatedAt = 2),
            topic("3", "C", group = "Zebra", unread = false, updatedAt = 1),
        )
        val flat = memoryFeed(topics.matching("", emptySet(), true), updatesOnly = true)
        assertEquals(listOf(""), flat.map { it.name })
        assertEquals(listOf("1", "2"), flat.single().topics.map { it.id })
        val grouped = memoryFeed(topics.matching("", emptySet(), false), updatesOnly = false)
        assertEquals(listOf("Zebra", "Alpha"), grouped.map { it.name })
        assertNull(memoryTabBadge(0))
        assertNull(memoryTabBadge(-1))
        assertEquals("30", memoryTabBadge(30))
    }

    @Test fun saveAndMarkReadKeepTheSavedTopicWhenTheWriteFails() = runBlocking {
        val kept = topic("1", "Keep me", content = "old", unread = true)
        val storeData = TopicDataStore(MemoryEntries(topics = listOf(kept)))
        val store = MemoryEntriesStore(storeData)
        storeData.failWrites = true
        assertFalse(store.upsertTopic("1", "Keep me", "", "new", DEFAULT_TOPIC_GROUP, emptyList()))
        assertEquals("old", storeData.state.value.topics.single().content)
        assertTrue(storeData.state.value.topics.single().unread)
        assertFalse(store.markTopicRead("1"))
        assertTrue(storeData.state.value.topics.single().unread)

        storeData.failWrites = false
        assertTrue(store.upsertTopic("1", "Keep me", "", "new", DEFAULT_TOPIC_GROUP, emptyList()))
        assertEquals("new", storeData.state.value.topics.single().content)
        assertFalse(storeData.state.value.topics.single().unread)

        val read = topic("2", "Already", unread = false)
        val readStore = TopicDataStore(MemoryEntries(topics = listOf(read)))
        readStore.failWrites = true
        assertTrue(MemoryEntriesStore(readStore).markTopicRead("2"))
        assertEquals(read, readStore.state.value.topics.single())
    }

    private class TopicDataStore(initial: MemoryEntries) : DataStore<MemoryEntries> {
        val state = MutableStateFlow(initial)
        var failWrites = false
        override val data: Flow<MemoryEntries> = state
        override suspend fun updateData(transform: suspend (t: MemoryEntries) -> MemoryEntries): MemoryEntries {
            val next = transform(state.value)
            if (failWrites) throw IOException("disk full")
            state.value = next
            return next
        }
    }

    private fun encodeV1(notes: List<MemoryEntry>, todos: List<MemoryEntry>): ByteArray {
        val out = ByteArrayOutputStream()
        DataOutputStream(out).use { data ->
            data.writeInt(1)
            data.writeEntryList(notes)
            data.writeEntryList(todos)
        }
        return out.toByteArray()
    }

    private fun DataOutputStream.writeEntryList(list: List<MemoryEntry>) {
        writeInt(list.size)
        list.forEach { entry ->
            writeUTF(entry.id)
            writeUTF(entry.title)
            val body = entry.content.toByteArray(Charsets.UTF_8)
            writeInt(body.size)
            write(body)
            writeBoolean(entry.done)
            writeLong(entry.createdAt)
        }
    }

    private fun entry(
        id: String,
        title: String,
        content: String = "",
        done: Boolean = false,
        createdAt: Long = 1,
    ) = MemoryEntry(id, title, content, done, createdAt)

    private fun topic(
        id: String,
        title: String,
        summary: String = "",
        content: String = "",
        group: String = DEFAULT_TOPIC_GROUP,
        tags: List<String> = emptyList(),
        unread: Boolean = false,
        createdAt: Long = 1,
        updatedAt: Long = 1,
    ) = MemoryTopic(id, title, summary, content, group, tags, unread, createdAt, updatedAt)
}
