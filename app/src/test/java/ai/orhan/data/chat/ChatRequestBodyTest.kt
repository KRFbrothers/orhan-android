package ai.orhan.data.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ChatRequestBodyTest {
    private val turns = (1..25).map { ChatTurn(if (it % 2 == 1) "user" else "model", "t$it") }

    @Test fun systemInstruction_topLevel_andMessagesAreRealHistory() {
        val b = ChatRepository.body(turns, true, "  You are Orhan.  ")
        assertEquals("You are Orhan.", b.getString("systemInstruction"))
        assertEquals(true, b.getBoolean("stream"))
        assertEquals("agent", b.getString("mode"))
        val msgs = b.getJSONArray("messages")
        assertEquals(20, msgs.length())
        assertEquals("t6", msgs.getJSONObject(0).getString("text"))
        assertEquals("t25", msgs.getJSONObject(19).getString("text"))
    }

    @Test fun blankOrNullSystemInstruction_omitted() {
        assertFalse(ChatRepository.body(turns, false, " ").has("systemInstruction"))
        assertFalse(ChatRepository.body(turns, false, null).has("systemInstruction"))
    }
}
