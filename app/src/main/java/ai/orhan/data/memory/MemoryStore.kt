package ai.orhan.data.memory

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.flow.Flow

/** Max stored characters for personal memory. */
const val MEMORY_MAX_CHARS = 4000

internal object MemorySerializer : Serializer<String> {
    override val defaultValue: String = ""
    override suspend fun readFrom(input: InputStream): String = try {
        input.readBytes().toString(Charsets.UTF_8)
    } catch (e: Exception) {
        throw CorruptionException("Cannot read memory", e)
    }
    override suspend fun writeTo(t: String, output: OutputStream) {
        output.write(t.toByteArray(Charsets.UTF_8))
    }
}

private val Context.memoryDataStore: DataStore<String> by dataStore(
    fileName = "orhan_memory.pb",
    serializer = MemorySerializer,
)

/** Single on-device "personal memory" string sent as hidden context with chat requests. */
class MemoryStore(private val dataStore: DataStore<String>) {
    val memory: Flow<String> = dataStore.data

    suspend fun set(text: String) {
        val value = text.trim().take(MEMORY_MAX_CHARS)
        dataStore.updateData { value }
    }
}

fun Context.memoryStore(): MemoryStore = MemoryStore(applicationContext.memoryDataStore)
