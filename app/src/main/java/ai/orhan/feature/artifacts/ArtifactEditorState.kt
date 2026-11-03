package ai.orhan.feature.artifacts

import ai.orhan.data.artifacts.ArtifactType
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException

/** Draft and mutation state used directly by the editor; failed writes never replace the draft. */
internal class ArtifactEditorState(title: String, content: String, type: ArtifactType) {
    var title by mutableStateOf(title)
    var content by mutableStateOf(content)
    var type by mutableStateOf(type)
    var saving by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    suspend fun save(write: suspend (String, String, ArtifactType) -> Boolean): Boolean {
        if (saving || content.isBlank()) return false
        saving = true
        error = null
        return try {
            write(title, content, type).also { saved ->
                if (!saved) error = "Couldn't save this document. Your edits are still here."
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            error = "Couldn't save this document. Your edits are still here."
            false
        } finally {
            saving = false
        }
    }
}

internal class ArtifactDeleteState {
    var confirming by mutableStateOf(false)
    var deleting by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    suspend fun delete(write: suspend () -> Boolean): Boolean {
        if (deleting || !confirming) return false
        deleting = true
        error = null
        return try {
            write().also { removed ->
                if (removed) confirming = false
                else error = "Couldn't delete this document. It is still saved on this device."
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            error = "Couldn't delete this document. It is still saved on this device."
            false
        } finally {
            deleting = false
        }
    }
}
