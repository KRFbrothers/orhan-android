package ai.orhan.feature.artifacts

import ai.orhan.data.artifacts.Artifact
import ai.orhan.data.artifacts.ArtifactType
import ai.orhan.design.theme.OrhanTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic callbacks only: no failed writes are injected into the user's real artifact store. */
@RunWith(AndroidJUnit4::class)
class ArtifactFailureUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun failedSaveKeepsEditorTitleSourceAndTypeThenAllowsRetry() {
        var attempted: Artifact? = null
        var attempts = 0
        compose.setContent {
            OrhanTheme(darkTheme = false) {
                ArtifactsScreen(emptyList(), onUpsert = { attempted = it; attempts++; false }, onDelete = { false }, onBack = {})
            }
        }
        compose.onNodeWithContentDescription("New document").performClick()
        compose.onNodeWithText("Markdown", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Title").performTextInput("Failure fixture")
        compose.onNodeWithText("Markdown source").performTextInput("# Draft survives")
        compose.onNodeWithText("Save").performScrollTo().performClick()
        compose.onNodeWithText("Couldn't save this document. Your edits are still here.").assertIsDisplayed()
        compose.onNodeWithText("Failure fixture").assertTextContains("Failure fixture")
        compose.onNodeWithText("# Draft survives").assertTextContains("# Draft survives")
        compose.onNodeWithText("Save").assertIsEnabled().performClick()
        compose.runOnIdle {
            org.junit.Assert.assertEquals(2, attempts)
            org.junit.Assert.assertEquals(ArtifactType.MARKDOWN, attempted?.type)
            org.junit.Assert.assertEquals("# Draft survives", attempted?.content)
        }
    }

    @Test fun failedDeleteKeepsDetailAndConfirmation() {
        val artifact = Artifact("failure-fixture", "Delete fixture", "Original stays", 1)
        compose.setContent {
            OrhanTheme(darkTheme = true) {
                ArtifactsScreen(listOf(artifact), onUpsert = { false }, onDelete = { false }, onBack = {})
            }
        }
        compose.onNodeWithText("Delete fixture").performClick()
        compose.onNodeWithText("Delete").performClick()
        compose.onNode(hasText("Delete") and hasAnyAncestor(isDialog())).performClick()
        compose.onNodeWithText("Couldn't delete this document. It is still saved on this device.").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Original stays").assertIsDisplayed()
    }
}
