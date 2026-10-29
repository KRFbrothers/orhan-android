package ai.orhan.data.agents

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import ai.orhan.R
import ai.orhan.feature.agents.InstalledAgent
import ai.orhan.feature.agents.SampleInstalledAgents
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/* Local store for user-created agents (no backend). Name (case-insensitive) is the key. */
private const val FORMAT_VERSION = 1

data class SavedAgent(
    val name: String,
    val blurb: String,
    val instructions: String = "",
    val tools: List<String> = emptyList(),
    val createdAt: Long,
)

object SavedAgentsSerializer : Serializer<List<SavedAgent>> {
    override val defaultValue: List<SavedAgent> = emptyList()

    override suspend fun readFrom(input: InputStream): List<SavedAgent> {
        val bytes = try { input.readBytes() } catch (e: IOException) { throw CorruptionException("read", e) }
        if (bytes.isEmpty()) return defaultValue
        return try {
            DataInputStream(bytes.inputStream()).use { d ->
                if (d.readInt() != FORMAT_VERSION) return defaultValue
                List(d.readInt()) {
                    SavedAgent(
                        name = d.readUTF(),
                        blurb = d.readUTF(),
                        instructions = d.readUTF(),
                        tools = List(d.readInt()) { d.readUTF() },
                        createdAt = d.readLong(),
                    )
                }
            }
        } catch (e: IOException) { defaultValue }
    }

    override suspend fun writeTo(t: List<SavedAgent>, output: OutputStream) {
        val d = DataOutputStream(output)
        d.writeInt(FORMAT_VERSION)
        d.writeInt(t.size)
        t.forEach { a ->
            d.writeUTF(a.name); d.writeUTF(a.blurb); d.writeUTF(a.instructions)
            d.writeInt(a.tools.size); a.tools.forEach(d::writeUTF)
            d.writeLong(a.createdAt)
        }
        d.flush()
    }
}

val Context.agentsDataStore: DataStore<List<SavedAgent>> by dataStore(
    fileName = "orhan_agents.pb",
    serializer = SavedAgentsSerializer,
)

/** Pure helper: insert/replace by name, newest first. */
fun List<SavedAgent>.upserted(agent: SavedAgent): List<SavedAgent> =
    listOf(agent) + filterNot { it.name.equals(agent.name, ignoreCase = true) }

fun SavedAgent.toInstalledAgent(): InstalledAgent = InstalledAgent(
    name = name,
    blurb = blurb,
    category = tools.firstOrNull() ?: "Custom",
    working = false,
    avatar = R.drawable.agent_webpage,
)

/** User agents first (newest first), then samples not shadowed by name. */
fun mergeWithSamples(
    user: List<InstalledAgent>,
    samples: List<InstalledAgent> = SampleInstalledAgents,
): List<InstalledAgent> =
    user + samples.filter { s -> user.none { it.name.equals(s.name, ignoreCase = true) } }

class AgentStore(private val dataStore: DataStore<List<SavedAgent>>) {
    val agents: Flow<List<SavedAgent>> = dataStore.data
        .catch { if (it is IOException) emit(emptyList()) else throw it }

    suspend fun upsert(agent: SavedAgent) {
        if (agent.name.isBlank()) return
        dataStore.updateData { it.upserted(agent.copy(name = agent.name.trim())) }
    }

    suspend fun delete(name: String) {
        dataStore.updateData { list -> list.filterNot { it.name.equals(name, ignoreCase = true) } }
    }
}

fun Context.agentStore(): AgentStore = AgentStore(agentsDataStore)
