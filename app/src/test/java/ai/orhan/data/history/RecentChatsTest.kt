package ai.orhan.data.history

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class RecentChatsTest {
    @Test fun openedMovesToFrontAndDedupes() {
        val l = emptyList<RecentChat>().withOpened("A", 1).withOpened("B", 2).withOpened("A", 3)
        assertEquals(listOf("A", "B"), l.map { it.title })
    }

    @Test fun capsSize() {
        var l = emptyList<RecentChat>()
        repeat(30) { l = l.withOpened("c$it", it.toLong()) }
        assertEquals(MAX_RECENT_CHATS, l.size)
        assertEquals("c29", l.first().title)
    }

    @Test fun serializerRoundTrip() = runBlocking {
        val l = listOf(RecentChat("Hi", 5), RecentChat("Yo", 9))
        val out = ByteArrayOutputStream()
        RecentChatsSerializer.writeTo(l, out)
        assertEquals(l, RecentChatsSerializer.readFrom(ByteArrayInputStream(out.toByteArray())))
    }
}
