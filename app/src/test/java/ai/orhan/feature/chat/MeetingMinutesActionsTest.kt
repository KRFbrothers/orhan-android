package ai.orhan.feature.chat

import org.junit.Assert.assertEquals
import org.junit.Test

class MeetingMinutesActionsTest {
    @Test fun extractsOnlyBulletsFromActionItemsSection() {
        val minutes = """
            Summary
            - This is not a task.

            ## Action items:
            - Send the revised plan by Friday.
            2. Ask Maya to confirm the venue.
            Unspecified

            ## Open questions
            - This is not an action item.
        """.trimIndent()
        assertEquals(listOf("Send the revised plan by Friday.", "Ask Maya to confirm the venue."), meetingActionItems(minutes))
    }

    @Test fun emptyOrUnstructuredActionSectionDoesNotCreateActions() {
        assertEquals(emptyList<String>(), meetingActionItems("Summary\nNo action items were stated."))
        assertEquals(emptyList<String>(), meetingActionItems("Action items\nNone\nOpen questions\n- unresolved"))
    }
}
