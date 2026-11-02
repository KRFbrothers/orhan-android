package ai.orhan.design.screens

import ai.orhan.data.chat.ChatAttachment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceAttachmentTest {
    @Test fun recordsBecomeReviewableBoundedAudioAttachments() {
        val bytes = byteArrayOf(1, 2, 3)
        val attachment = voiceAttachmentFromBytes(bytes)!!
        assertEquals("voice-message.m4a", attachment.name)
        assertEquals("audio/mp4", attachment.mimeType)
        assertEquals(3L, attachment.sizeBytes)
        assertEquals(java.util.Base64.getEncoder().encodeToString(bytes), attachment.base64)
    }

    @Test fun emptyAndOversizedRecordingsAreRejected() {
        assertNull(voiceAttachmentFromBytes(byteArrayOf()))
        assertNull(voiceAttachmentFromBytes(ByteArray(ChatAttachment.MAX_BYTES.toInt() + 1)))
    }
}
