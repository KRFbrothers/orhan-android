package ai.orhan.data.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SseActivityTest {
    private fun f(m: Map<String, String>): (String) -> String = { m[it].orEmpty() }

    @Test fun thinkingDelta() =
        assertEquals("Planning the answer", SseActivity.label("thinking", f(mapOf("delta" to "Planning the answer"))))

    @Test fun progressMessage() =
        assertEquals("Searching web", SseActivity.label("progress", f(mapOf("message" to " Searching web "))))

    @Test fun messageWinsOverDelta() =
        assertEquals("A", SseActivity.label("progress", f(mapOf("message" to "A", "delta" to "B"))))

    @Test fun nestedData() =
        assertEquals("Reading file", SseActivity.label("progress", f(emptyMap()), f(mapOf("label" to "Reading file"))))

    @Test fun emptyFallsBackToDefaults() {
        assertEquals(SseActivity.THINKING_DEFAULT, SseActivity.label("thinking", f(emptyMap())))
        assertEquals(SseActivity.PROGRESS_DEFAULT, SseActivity.label("progress", f(emptyMap())))
    }

    @Test fun firstLineAndTruncate() {
        assertEquals("line1", SseActivity.label("thinking", f(mapOf("delta" to "\nline1\nline2"))))
        val long = "x".repeat(200)
        assertEquals(80, SseActivity.label("thinking", f(mapOf("delta" to long)))!!.length)
    }

    @Test fun otherTypesIgnored() {
        assertNull(SseActivity.label("text", f(mapOf("delta" to "hi"))))
        assertNull(SseActivity.label("done", f(emptyMap())))
        assertNull(SseActivity.label(null, f(emptyMap())))
    }
}
