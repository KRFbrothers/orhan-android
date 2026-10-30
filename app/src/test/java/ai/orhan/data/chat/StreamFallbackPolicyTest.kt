package ai.orhan.data.chat

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamFallbackPolicyTest {
    @Test fun onlyNotAcceptableAndUnsupportedMediaFallBack() {
        assertTrue(ChatRepository.isFallbackStatus(406))
        assertTrue(ChatRepository.isFallbackStatus(415))
        listOf(200, 400, 401, 403, 404, 408, 429, 500, 502, 503, 504).forEach {
            assertFalse("$it must not fall back", ChatRepository.isFallbackStatus(it))
        }
    }

    @Test fun fallbackEligibleIsDistinctFromFatal() {
        val e: Throwable = ChatRepository.FallbackEligibleStreamException("x")
        assertFalse(e is ChatRepository.FatalChatException)
        assertFalse(java.net.SocketTimeoutException() is ChatRepository.FallbackEligibleStreamException)
    }
}
