package ai.orhan.feature.chat

import ai.orhan.data.chat.ChatAttachment
import ai.orhan.design.screens.ChatMessage
import ai.orhan.design.screens.ChatRole
import org.junit.Assert.assertEquals
import org.junit.Test

class TurnsForApiTest {
    private fun msg(id: Long, role: ChatRole, text: String = "t$id", attachment: ChatAttachment? = null) = ChatMessage(id, role, text, "", attachment = attachment)
    private fun att(n: String) = ChatAttachment(n, "image/png", "AAAA", 3)

    @Test fun noAttachments_allEmpty() {
        val t = turnsForApi(listOf(msg(1, ChatRole.User), msg(2, ChatRole.Agent), msg(3, ChatRole.User)), emptyMap())
        assertEquals(listOf("user", "model", "user"), t.map { it.role })
        assert(t.all { it.attachments.isEmpty() })
    }

    @Test fun olderAttachment_notSent() {
        val t = turnsForApi(listOf(msg(1, ChatRole.User, "📎 a.png"), msg(2, ChatRole.Agent), msg(3, ChatRole.User)), mapOf(1L to att("a")))
        assert(t.all { it.attachments.isEmpty() })
        assertEquals("📎 a.png", t[0].text)
    }

    @Test fun latestAttachment_sent() {
        val a = att("b")
        val t = turnsForApi(listOf(msg(1, ChatRole.User), msg(2, ChatRole.Agent), msg(3, ChatRole.User, attachment = a)), mapOf(3L to a))
        assertEquals(listOf(a), t[2].attachments)
    }

    @Test fun mixed_onlyLatest() {
        val b = att("b")
        val t = turnsForApi(
            listOf(msg(1, ChatRole.User), msg(2, ChatRole.Agent), msg(3, ChatRole.User, attachment = b), msg(4, ChatRole.Agent)),
            mapOf(1L to att("a"), 3L to b),
        )
        assertEquals(listOf(emptyList(), emptyList(), listOf(b), emptyList()), t.map { it.attachments })
    }

    @Test fun staleMapEntry_doesNotAttachToPlainMessageWithReusedId() {
        val turns = turnsForApi(listOf(msg(3, ChatRole.User)), mapOf(3L to att("deleted")))
        assertEquals(emptyList<ChatAttachment>(), turns.single().attachments)
    }

    @Test fun differentAttachmentUnderSameId_isNotSent() {
        val turns = turnsForApi(
            listOf(msg(3, ChatRole.User, attachment = att("current"))),
            mapOf(3L to att("old")),
        )
        assertEquals(emptyList<ChatAttachment>(), turns.single().attachments)
    }

    @Test fun duplicateLegacyAgentId_neverReceivesUserAttachment() {
        val a = att("current")
        val turns = turnsForApi(
            listOf(msg(3, ChatRole.User, attachment = a), msg(3, ChatRole.Agent)),
            mapOf(3L to a),
        )
        assertEquals(listOf(listOf(a), emptyList()), turns.map { it.attachments })
    }
}
