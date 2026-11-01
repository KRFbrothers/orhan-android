package ai.orhan.data.memory

import ai.orhan.data.history.ChatThread
import ai.orhan.data.history.ChatThreadsSerializer
import ai.orhan.data.history.StoredMessage
import ai.orhan.feature.chat.personalMemoryBlock
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException

/**
 * Automatic memory stays off until enabled, ignores older messages, and only
 * persists a validated statement. Fixtures are synthetic.
 */
class AutomaticMemoryTest {

    private val fact = "I prefer tea without sugar."

    @Test fun automaticMemoryIsOffUntilEnabledAndPauseIsLeftAlone() = runBlocking {
        val prefs = Prefs()
        val pause = MemoryPauseStore(prefs)
        pause.setPaused(true)
        pause.setAutomaticEnabled(false, now = 50)
        val state = MemoryPauseStore(prefs).state.first()
        assertTrue(state.paused)
        assertFalse(state.automaticEnabled)
        assertEquals(0L, state.automaticEnabledAt)
        assertFalse(state.includeTopics)

        pause.setAutomaticEnabled(true, now = 80)
        val enabled = MemoryPauseStore(prefs).state.first()
        assertTrue(enabled.paused)
        assertTrue(enabled.automaticEnabled)
        assertEquals(80L, enabled.automaticEnabledAt)
        assertFalse(extractionAllowed(enabled))
    }

    @Test fun optInSavesOnlyANewEligibleMessageAndMarksItUnread() = runBlocking {
        val harness = harness()
        harness.pause.setPaused(false)
        harness.pause.setAutomaticEnabled(true, now = 1_000)
        harness.coordinator.onMessagesSaved(
            "Orhan",
            listOf(
                message(1, "hello", createdAt = 500),
                message(2, fact, createdAt = 1_500),
                message(3, "What is the weather?", createdAt = 1_600),
            ),
        )
        val saved = harness.entries.topics.first()
        assertEquals(1, saved.size)
        assertEquals(fact.removeSuffix("."), saved.single().title)
        assertEquals(fact, saved.single().content)
        assertEquals("Orhan", saved.single().sourceChatTitle)
        assertEquals(2L, saved.single().sourceMessageId)
        assertTrue(saved.single().unread)
        assertEquals(listOf(AUTOMATIC_TOPIC_TAG), saved.single().tags)
        assertEquals("Chats", saved.single().group)
    }

    @Test fun pausedOrUnreadablePrefsDoNotExtractOrSendContext() = runBlocking {
        val harness = harness()
        harness.pause.setAutomaticEnabled(true, now = 100)
        harness.pause.setIncludeTopics(true)
        harness.pause.setPaused(true)
        harness.coordinator.onMessagesSaved("Orhan", listOf(message(4, fact, createdAt = 200)))
        assertTrue(harness.entries.topics.first().isEmpty())
        val paused = harness.pause.state.first()
        assertNull(personalMemoryBlock("secret pref", paused.paused))
        assertNull(topicContextBlock(listOf(sampleTopic()), include = true, paused = paused.paused))

        val blocked = AutomaticMemoryCoordinator(MemoryPauseStore(FailingPrefs()), harness.entries)
        blocked.onMessagesSaved("Orhan", listOf(message(5, fact, createdAt = 9_000)))
        assertTrue(harness.entries.topics.first().isEmpty())
        val unreadable = MemoryPauseStore(FailingPrefs()).state.first()
        assertTrue(unreadable.unreadable)
        assertFalse(unreadable.automaticEnabled)
        assertFalse(unreadable.includeTopics)
        assertNull(topicContextBlock(listOf(sampleTopic()), include = true, paused = unreadable.paused))
    }

    @Test fun duplicateStatementsAndSourcesAreNotStoredTwice() = runBlocking {
        val harness = harness()
        harness.pause.setAutomaticEnabled(true, now = 10)
        val first = message(6, fact, createdAt = 20)
        harness.coordinator.onMessagesSaved("Orhan", listOf(first))
        harness.coordinator.onMessagesSaved("Orhan", listOf(first, message(7, fact, createdAt = 30)))
        assertEquals(1, harness.entries.topics.first().size)
    }

    @Test fun invalidExtractionIsNotPersisted() {
        assertNull(extractDurableStatement("hello"))
        assertNull(extractDurableStatement("I live in Delhi?"))
        assertNull(extractDurableStatement("Send the report now"))
        val invented = ExtractedMemory("I prefer tea", "I prefer tea", "I prefer coffee")
        assertFalse(isValidExtraction("I prefer tea without sugar.", invented))
    }

    @Test fun sourceSurvivesEditingAndAnOlderFileStillOpens() = runBlocking {
        val harness = harness()
        harness.pause.setAutomaticEnabled(true, now = 10)
        harness.coordinator.onMessagesSaved("Orhan", listOf(message(8, fact, createdAt = 40)))
        val id = harness.entries.topics.first().single().id
        assertTrue(harness.entries.upsertTopic(id, "Shelf label", "Spare key shelf", fact, "Home", listOf("keys")))
        val edited = harness.entries.topics.first().single()
        assertEquals("Orhan", edited.sourceChatTitle)
        assertEquals(8L, edited.sourceMessageId)
        assertFalse(edited.unread)

        val legacy = decodeMemoryEntries(encodeV2(fact).inputStream())
        assertEquals(fact, legacy.topics.single().content)
        assertEquals("", legacy.topics.single().sourceChatTitle)
        assertEquals(0L, legacy.topics.single().sourceMessageId)
        val again = decodeMemoryEntries(encodeMemoryEntries(legacy).inputStream())
        assertEquals("", again.topics.single().sourceChatTitle)
        assertEquals(0L, again.topics.single().sourceMessageId)
    }

    @Test fun deletionStaysDeletedAcrossRestart() = runBlocking {
        val prefs = Prefs()
        val data = TopicStore(MemoryEntries())
        val pause = MemoryPauseStore(prefs)
        val entries = MemoryEntriesStore(data)
        pause.setAutomaticEnabled(true, now = 10)
        val messages = listOf(message(9, fact, createdAt = 50))
        AutomaticMemoryCoordinator(pause, entries).onMessagesSaved("Orhan", messages)
        val id = entries.topics.first().single().id
        assertTrue(entries.deleteTopic(id))
        AutomaticMemoryCoordinator(MemoryPauseStore(prefs), MemoryEntriesStore(data)).onMessagesSaved("Orhan", messages)
        assertTrue(MemoryEntriesStore(data).topics.first().isEmpty())
        assertTrue(MemoryPauseStore(prefs).state.first().automaticEnabled)
        assertTrue(MemoryPauseStore(prefs).state.first().processedMessages.isNotEmpty())
    }

    @Test fun writeFailureDoesNotReportSuccessOrDropTheMessage() = runBlocking {
        val harness = harness()
        harness.pause.setAutomaticEnabled(true, now = 10)
        harness.data.failWrites = true
        harness.coordinator.onMessagesSaved("Orhan", listOf(message(10, fact, createdAt = 60)))
        assertTrue(harness.entries.topics.first().isEmpty())
        assertEquals(AUTOMATIC_SAVE_ERROR, harness.pause.state.first().lastError)
        assertTrue(harness.pause.state.first().processedMessages.isEmpty())

        harness.data.failWrites = false
        harness.coordinator.onMessagesSaved("Orhan", listOf(message(10, fact, createdAt = 60)))
        assertEquals(1, harness.entries.topics.first().size)
        assertNull(harness.pause.state.first().lastError)
        assertTrue(harness.entries.markTopicRead(harness.entries.topics.first().single().id))
        assertFalse(harness.entries.topics.first().single().unread)
    }

    @Test fun topicsAreSentOnlyWhenThatChoiceIsOnAndPauseIsOff() {
        val topics = listOf(sampleTopic())
        assertNull(topicContextBlock(topics, include = false, paused = false))
        assertNull(topicContextBlock(topics, include = true, paused = true))
        val block = topicContextBlock(topics, include = true, paused = false)
        assertTrue(block!!.contains("Shelf label"))
        assertFalse(block.contains(fact))
    }

    @Test fun version2ChatThreadKeepsAZeroTimestamp() = runBlocking {
        val raw = ByteArrayOutputStream()
        DataOutputStream(raw).use { out ->
            out.writeInt(2)
            out.writeInt(1)
            out.writeUTF("Orhan")
            out.writeLong(10)
            out.writeInt(1)
            out.writeLong(7)
            out.writeUTF("user")
            val text = fact.toByteArray()
            out.writeInt(text.size)
            out.write(text)
            out.writeUTF("10:00")
            out.writeBoolean(false)
        }
        val loaded = ChatThreadsSerializer.readFrom(ByteArrayInputStream(raw.toByteArray()))
        assertEquals(0L, loaded.single().messages.single().createdAt)
        assertEquals(fact, loaded.single().messages.single().text)

        val fresh = listOf(ChatThread("Orhan", 11, listOf(StoredMessage(7, "user", fact, "10:01", createdAt = 70))))
        val out = ByteArrayOutputStream()
        ChatThreadsSerializer.writeTo(fresh, out)
        val roundTrip = ChatThreadsSerializer.readFrom(ByteArrayInputStream(out.toByteArray()))
        assertEquals(70L, roundTrip.single().messages.single().createdAt)
    }

    private fun harness(): Harness {
        val prefs = Prefs()
        val data = TopicStore(MemoryEntries())
        val pause = MemoryPauseStore(prefs)
        val entries = MemoryEntriesStore(data)
        return Harness(prefs, data, pause, entries, AutomaticMemoryCoordinator(pause, entries))
    }

    private fun message(id: Long, text: String, createdAt: Long) =
        StoredMessage(id, "user", text, "10:00", createdAt = createdAt)

    private fun sampleTopic() = MemoryTopic(
        id = "t",
        title = "Shelf label",
        summary = "Spare key shelf",
        content = fact,
        group = "Home",
        tags = listOf("keys"),
        unread = true,
        createdAt = 1,
        updatedAt = 1,
        sourceChatTitle = "Orhan",
        sourceMessageId = 2,
    )

    private fun encodeV2(content: String): ByteArray {
        val out = ByteArrayOutputStream()
        DataOutputStream(out).use { data ->
            data.writeInt(2)
            data.writeInt(0)
            data.writeInt(0)
            data.writeInt(1)
            data.writeUTF("legacy")
            data.writeUTF("Legacy title")
            writeBounded(data, "Legacy title")
            writeBounded(data, content)
            data.writeUTF("General")
            data.writeInt(0)
            data.writeBoolean(false)
            data.writeLong(1)
            data.writeLong(1)
        }
        return out.toByteArray()
    }

    private fun writeBounded(out: DataOutputStream, value: String) {
        val body = value.toByteArray()
        out.writeInt(body.size)
        out.write(body)
    }

    private class Harness(
        val prefs: Prefs,
        val data: TopicStore,
        val pause: MemoryPauseStore,
        val entries: MemoryEntriesStore,
        val coordinator: AutomaticMemoryCoordinator,
    )

    private class Prefs : DataStore<Preferences> {
        val state = MutableStateFlow(emptyPreferences())
        override val data: Flow<Preferences> = state
        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            transform(state.value).also { state.value = it }
    }

    private class FailingPrefs : DataStore<Preferences> {
        override val data: Flow<Preferences> = kotlinx.coroutines.flow.flow { throw IOException("unreadable") }
        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            throw IOException("unreadable")
        }
    }

    private class TopicStore(initial: MemoryEntries) : DataStore<MemoryEntries> {
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
}
