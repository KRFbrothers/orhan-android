package ai.orhan.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

class RelativeTimeTest {
    private val zone = ZoneId.systemDefault()

    /** An hour after local midnight, so a DST gap cannot move the calendar day. */
    private fun millis(date: LocalDate): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli() + 3_600_000L

    @Test fun withinSixDaysUsesShortWeekday() {
        val today = LocalDate.of(2026, 10, 29)
        val opened = today.minusDays(2)
        assertEquals(
            opened.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
            relativeTime(millis(opened), millis(today)),
        )
    }

    @Test fun sixDaysAgoStillUsesWeekday() {
        val today = LocalDate.of(2026, 10, 29)
        val opened = today.minusDays(6)
        assertFalse(relativeTime(millis(opened), millis(today)).contains("/"))
    }

    @Test fun sevenDaysAgoUsesNumericUsDate() {
        val today = LocalDate.of(2026, 10, 29)
        val opened = today.minusDays(7)
        assertEquals("10/22/26", relativeTime(millis(opened), millis(today)))
    }
}
