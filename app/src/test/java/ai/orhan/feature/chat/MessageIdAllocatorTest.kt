package ai.orhan.feature.chat

import java.util.concurrent.Executors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageIdAllocatorTest {
    @Test fun reservationsAreDistinctBeforeAnyBubbleIsAppended() {
        val ids = MessageIdAllocator(0)
        assertEquals(listOf(1L, 2L, 3L), List(3) { ids.reserveAfter(0) })
    }

    @Test fun deletionAndDelayedReplyDoNotReleaseReservations() {
        val ids = MessageIdAllocator(0)
        val user = ids.reserveAfter(0)
        val delayedReply = ids.reserveAfter(user)
        // Both visible messages were removed before the reply arrived.
        val nextUser = ids.reserveAfter(0)
        val nextReply = ids.reserveAfter(nextUser)
        assertEquals(listOf(1L, 2L, 3L, 4L), listOf(user, delayedReply, nextUser, nextReply))
    }

    @Test fun restoredMaximumNotLastItemIsTheFloorAndNeverRewinds() {
        val ids = MessageIdAllocator(0)
        val restored = listOf(81L, 4L, 12L)
        assertEquals(82L, ids.reserveAfter(restored.max()))
        assertEquals(83L, ids.reserveAfter(0))
        assertEquals(101L, ids.reserveAfter(100))
    }

    @Test fun restoredIdsWinEvenWhenTheNewProcessClockSeedIsLower() {
        val ids = MessageIdAllocator(10L)
        assertEquals(50_001L, ids.reserveAfter(50_000L))
        assertEquals(50_002L, ids.reserveAfter(0L))
    }

    @Test fun concurrentReservationsAreUnique() {
        val ids = MessageIdAllocator(0)
        val pool = Executors.newFixedThreadPool(4)
        try {
            val results = (1..200).map { pool.submit<Long> { ids.reserveAfter(0) } }.map { it.get() }
            assertEquals(200, results.toSet().size)
            assertTrue(results.all { it > 0 })
        } finally {
            pool.shutdownNow()
        }
    }

    @Test(expected = IllegalStateException::class)
    fun exhaustedLongRangeFailsRatherThanReusingAnId() {
        MessageIdAllocator(0).reserveAfter(Long.MAX_VALUE)
    }
}
