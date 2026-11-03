package ai.orhan.feature.artifacts

import ai.orhan.data.artifacts.Artifact
import ai.orhan.data.artifacts.ArtifactType
import android.content.Intent
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArtifactSharingTest {
    @Test fun typedShareGrantsOnlyTheSourceFileWithCorrectMimeAndBytes() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        for (type in listOf(ArtifactType.MARKDOWN, ArtifactType.HTML)) {
            val source = if (type == ArtifactType.HTML) "<h1>Saved HTML</h1>" else "# Saved Markdown"
            val intent = artifactShareIntent(context, Artifact("share-fixture", "Synthetic", source, 1, type))
            assertEquals(type.mimeType, intent.type)
            assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
            assertEquals(0, intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            @Suppress("DEPRECATION")
            val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)!!
            assertEquals("${context.packageName}.artifact-exports", uri.authority)
            assertEquals(uri, intent.clipData!!.getItemAt(0).uri)
            val bytes = context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
            assertArrayEquals(source.toByteArray(Charsets.UTF_8), bytes)
        }
    }

    @Test fun oldPlainTextShareStillSendsText() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = artifactShareIntent(context, Artifact("plain-fixture", "Synthetic", "Unchanged text", 1))
        assertEquals("text/plain", intent.type)
        assertEquals("Unchanged text", intent.getStringExtra(Intent.EXTRA_TEXT))
        assertFalse(intent.hasExtra(Intent.EXTRA_STREAM))
    }
}
