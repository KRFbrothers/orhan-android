package ai.orhan.feature.search

import ai.orhan.data.history.ChatThread
import ai.orhan.data.history.StoredMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalMessageSearchTest {
    @Test fun duplicateBodiesKeepDistinctConversationAndMessageIdentity() {
        val threads = listOf(
            ChatThread("A", 20, listOf(StoredMessage(11, "user", "same text", "now"))),
            ChatThread("B", 10, listOf(StoredMessage(22, "agent", "same text", "now"))),
        )

        val hits = findMessageHits(threads, "same")

        assertEquals(listOf("A" to 11L, "B" to 22L), hits.map { it.chatTitle to it.messageId })
    }

    @Test fun persistedTranscriptIsSearchableAndMarkedAsTranscript() {
        val thread = ChatThread("Voice", 1, listOf(StoredMessage(7, "user", "voice.m4a", "now", transcript = "The launch is tomorrow.")))

        val hits = findMessageHits(listOf(thread), "launch")

        assertEquals(1, hits.size)
        assertEquals(MessageMatchKind.Transcript, hits.single().kind)
        assertTrue(hits.single().snippet.contains("launch"))
    }

    @Test fun blankQueryAndRemovedSourceThreadsProduceNoMessageHits() {
        val removedSource = ChatThread("Removed", 1, listOf(StoredMessage(9, "user", "secret term", "now")))

        assertTrue(findMessageHits(listOf(removedSource), " ").isEmpty())
        assertTrue(findMessageHits(emptyList(), "secret").isEmpty())
    }
}
