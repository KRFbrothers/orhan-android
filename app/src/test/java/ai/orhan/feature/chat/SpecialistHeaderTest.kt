package ai.orhan.feature.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpecialistHeaderTest {
    @Test fun headerHasLabelAndBlankLine() {
        assertEquals("\uD83E\uDD1D Writer\n\n", specialistHeader("Writer"))
    }

    @Test fun headerStartsWithPrefix() {
        assertTrue(specialistHeader("X").startsWith(SPECIALIST_PREFIX))
    }
}
