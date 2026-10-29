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
