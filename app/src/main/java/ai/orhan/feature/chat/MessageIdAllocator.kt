package ai.orhan.feature.chat

import java.util.concurrent.atomic.AtomicLong

/**
 * Reserves Long IDs independently of the visible list: deletion, retries and delayed
 * streaming bubbles cannot release an ID. The time-based seed separates normal process
 * restarts without changing the persisted history format; restored IDs form a lower bound.
 * This is not a durable high-water mark for deleted history after a clock rollback.
 */
internal class MessageIdAllocator(initialId: Long = System.currentTimeMillis() * 1_000L) {
    private val lastId = AtomicLong(initialId)

    fun reserveAfter(restoredMaxId: Long): Long = lastId.updateAndGet { reserved ->
        val floor = maxOf(reserved, restoredMaxId)
        check(floor < Long.MAX_VALUE) { "Message ID range exhausted" }
        floor + 1L
    }
}
