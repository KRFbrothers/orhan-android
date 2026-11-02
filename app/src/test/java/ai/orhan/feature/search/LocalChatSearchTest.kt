package ai.orhan.feature.search

import ai.orhan.design.screens.OrhanChat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalChatSearchTest {
    private val chat = OrhanChat(
        title = "Research",
        snippet = "Latest summary",
        time = "today",
        isAgent = true,
        searchableMessages = listOf("first note", "We chose the blue option after review."),
    )

    @Test fun matchesSavedMessageTextCaseInsensitively() {
        assertTrue(chat.matchesLocalQuery("BLUE option"))
        assertFalse(chat.matchesLocalQuery("missing phrase"))
    }

    @Test fun matchingMessageProvidesResultContextAndTitleSearchKeepsLatestSnippet() {
        assertEquals("We chose the blue option after review.", chat.localSearchSnippet("blue"))
        assertEquals("Latest summary", chat.localSearchSnippet("Research"))
    }
}
