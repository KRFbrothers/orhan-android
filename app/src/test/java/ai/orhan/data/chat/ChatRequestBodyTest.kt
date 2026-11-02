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

    @Test fun explicitToolPolicyIsSentAndEmptyMeansNoTools() {
        val selected = ChatRepository.body(turns, true, allowedTools = listOf("google_search", "calculator"))
        assertEquals("google_search", selected.getJSONArray("allowedTools").getString(0))
        assertEquals(2, selected.getJSONArray("allowedTools").length())
        assertEquals(0, ChatRepository.body(turns, true, allowedTools = emptyList()).getJSONArray("allowedTools").length())
        assertFalse(ChatRepository.body(turns, true).has("allowedTools"))
    }

    @Test fun durableResumeUsesTheExistingResumeRunContract() {
        val body = ChatRepository.body(emptyList(), stream = true, mode = "agent", resumeRunId = "engine-task-1")
        assertEquals("engine-task-1", body.getString("resumeRun"))
        assertEquals("agent", body.getString("mode"))
        assertEquals(true, body.getBoolean("stream"))
    }

    @Test fun plainServiceActionsAreToolFreeAndAudioStaysOnItsUserTurn() {
        val audio = ChatAttachment("voice.m4a", "audio/mp4", "AQID", 3)
        val body = ChatRepository.body(
            listOf(ChatTurn("user", "Transcribe this.", listOf(audio))),
            stream = true,
            systemInstruction = "Transcribe only.",
            mode = "auto",
        )
        assertEquals("auto", body.getString("mode"))
        val turn = body.getJSONArray("messages").getJSONObject(0)
        assertEquals("audio/mp4", turn.getJSONArray("attachments").getJSONObject(0).getString("mimeType"))
        assertFalse(body.has("allowedTools"))
    }
}
