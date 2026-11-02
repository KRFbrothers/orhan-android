package ai.orhan.feature.artifacts

import ai.orhan.data.artifacts.Artifact
import ai.orhan.data.artifacts.ArtifactType
import ai.orhan.data.artifacts.exportDocument
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Only this dedicated cache directory is visible to the read-grant FileProvider. */
internal suspend fun artifactShareIntent(context: Context, artifact: Artifact): Intent = withContext(Dispatchers.IO) {
    Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_SUBJECT, artifact.title)
        if (artifact.type == ArtifactType.PLAIN_TEXT) {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, artifact.content)
        } else {
            val export = artifact.exportDocument()
            val directory = File(context.cacheDir, "artifact_exports").apply {
                check(isDirectory || mkdirs()) { "Cannot prepare sharing" }
            }
            // Previously granted files remain available for a day; no app records are touched.
            directory.listFiles()?.filter { it.isFile && it.lastModified() < System.currentTimeMillis() - 86_400_000 }
                ?.forEach { it.delete() }
            val file = File.createTempFile(export.fileName.substringBeforeLast('.').take(40).padEnd(3, '_') + "-", ".${artifact.type.extension}", directory)
            file.outputStream().use { it.write(export.bytes) }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.artifact-exports", file)
            type = export.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(export.fileName, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
