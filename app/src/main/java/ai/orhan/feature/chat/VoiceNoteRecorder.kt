package ai.orhan.feature.chat

import ai.orhan.data.chat.ChatAttachment
import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import java.io.File

/**
 * Records a short voice note (AAC in .m4a) into the app cache and turns it into a
 * [ChatAttachment] for the same inline-file path the Attach button uses.
 * Capped at [MAX_DURATION_MS] and well under the engine's 4 MB per-file limit (~32 kbps ≈ 240 KB/min).
 */
class VoiceNoteRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var file: File? = null
    private var startedAt = 0L

    val isRecording: Boolean get() = recorder != null

    /** Starts recording; [onMaxReached] fires on the main thread when the duration cap hits. Returns false on failure. */
    fun start(onMaxReached: () -> Unit): Boolean {
        if (recorder != null) return true
        val out = File(context.cacheDir, "voice-note-${System.currentTimeMillis()}.m4a")
        @Suppress("DEPRECATION")
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()
        return try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(32_000)
            r.setAudioSamplingRate(16_000)
            r.setAudioChannels(1)
            r.setMaxDuration(MAX_DURATION_MS)
            r.setMaxFileSize(ChatAttachment.MAX_BYTES.toLong())
            r.setOnInfoListener { _, what, _ ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED ||
                    what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED
                ) onMaxReached()
            }
            r.setOutputFile(out.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            file = out
            startedAt = System.currentTimeMillis()
            true
        } catch (_: Exception) {
            runCatching { r.release() }
            out.delete()
            false
        }
    }

    /** Stops and returns the note, or null if it was too short / failed. */
    fun stop(): ChatAttachment? {
        val r = recorder ?: return null
        val f = file
        val elapsed = System.currentTimeMillis() - startedAt
        recorder = null
        file = null
        val ok = runCatching { r.stop() }.isSuccess
        runCatching { r.release() }
        if (f == null) return null
        try {
            if (!ok || elapsed < MIN_DURATION_MS || !f.exists() || f.length() == 0L) return null
            if (f.length() > ChatAttachment.MAX_BYTES) return null
            val bytes = f.readBytes()
            val secs = (elapsed / 1000).coerceAtLeast(1)
            return ChatAttachment(
                name = "Voice note (${secs / 60}:${"%02d".format(secs % 60)}).m4a",
                mimeType = "audio/mp4",
                base64 = Base64.encodeToString(bytes, Base64.NO_WRAP),
                sizeBytes = bytes.size.toLong(),
            )
        } finally {
            f.delete()
        }
    }

    /** Discards the current recording. */
    fun cancel() {
        val r = recorder ?: return
        recorder = null
        runCatching { r.stop() }
        runCatching { r.release() }
        file?.delete()
        file = null
    }

    companion object {
        const val MAX_DURATION_MS = 60_000
        const val MIN_DURATION_MS = 700L
    }
}
