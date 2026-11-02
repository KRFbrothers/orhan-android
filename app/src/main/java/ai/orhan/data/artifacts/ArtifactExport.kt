package ai.orhan.data.artifacts

data class ArtifactExport(val fileName: String, val mimeType: String, val bytes: ByteArray)

/** Exports the original UTF-8 source, never a plain-text substitute for a typed document. */
fun Artifact.exportDocument(): ArtifactExport {
    val safeTitle = title.replace(Regex("[^\\p{L}\\p{N} _-]"), "_")
        .trim().take(80).ifBlank { "Document" }
    return ArtifactExport("$safeTitle.${type.extension}", type.mimeType, content.toByteArray(Charsets.UTF_8))
}
