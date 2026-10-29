package ai.orhan.data.agents

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import ai.orhan.R
import ai.orhan.feature.agents.InstalledAgent
import ai.orhan.feature.agents.SampleInstalledAgents
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/* Local store for user-created agents (no backend). Name (case-insensitive) is the key. */
private const val FORMAT_VERSION = 2
private const val FORMAT_VERSION_V1 = 1

const val MAX_AGENT_NAME = 80
const val MAX_AGENT_BLURB = 200
const val MAX_AGENT_INSTRUCTIONS = 8_000

/** Trims and caps user-entered fields so storage and writeUTF stay bounded. */
fun SavedAgent.capped(): SavedAgent = copy(
    name = name.trim().take(MAX_AGENT_NAME).trim(),
    blurb = blurb.trim().take(MAX_AGENT_BLURB),
    instructions = instructions.trim().take(MAX_AGENT_INSTRUCTIONS),
)

/** Returns the saved/sample agent's canonical name if [title] matches case-insensitively, else [title]. */
fun canonicalAgentTitle(title: String, agentNames: List<String>): String =
    agentNames.firstOrNull { it.equals(title.trim(), ignoreCase = true) } ?: title

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
                val version = d.readInt()
                if (version != FORMAT_VERSION && version != FORMAT_VERSION_V1) return defaultValue
                List(d.readInt()) {
                    SavedAgent(
                        name = d.readUTF(),
                        blurb = d.readUTF(),
                        instructions = if (version >= 2) readLongString(d) else d.readUTF(),
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
            d.writeUTF(a.name); d.writeUTF(a.blurb); writeLongString(d, a.instructions)
            d.writeInt(a.tools.size); a.tools.forEach(d::writeUTF)
            d.writeLong(a.createdAt)
        }
        d.flush()
    }

    private fun writeLongString(d: DataOutputStream, s: String) {
        val b = s.toByteArray(Charsets.UTF_8); d.writeInt(b.size); d.write(b)
    }
    private fun readLongString(d: DataInputStream): String {
        val n = d.readInt()
        if (n < 0 || n > 4 * 1024 * 1024) throw IOException("bad length")
        val b = ByteArray(n); d.readFully(b); return String(b, Charsets.UTF_8)
    }
}

val Context.agentsDataStore: DataStore<List<SavedAgent>> by dataStore(
    fileName = "orhan_agents.pb",
    serializer = SavedAgentsSerializer,
)

/** Pure helper: insert/replace by name, newest first. */
fun List<SavedAgent>.upserted(agent: SavedAgent): List<SavedAgent> =
    listOf(agent) + filterNot { it.name.equals(agent.name, ignoreCase = true) }

sealed class AgentWrite {
    data class Ok(val agents: List<SavedAgent>) : AgentWrite()
    data class Rejected(val message: String) : AgentWrite()
}

/**
 * One list transformation for create, edit, and rename.
 * A rename removes [previousName] and inserts [raw] together, so a rejected write leaves the list untouched.
 * Repeating a successful write is a no-op.
 */
fun List<SavedAgent>.replacing(raw: SavedAgent, previousName: String?): AgentWrite {
    val agent = raw.capped()
    if (agent.name.isBlank()) return AgentWrite.Rejected("Name is required")

    val previous = previousName?.takeIf { it.isNotBlank() }?.let { key ->
        firstOrNull { it.name.equals(key, ignoreCase = true) }
    }
    val occupant = firstOrNull { it.name.equals(agent.name, ignoreCase = true) }
    if (occupant != null && occupant != previous && occupant != agent) {
        return AgentWrite.Rejected("An agent with this name already exists")
    }

    val oldRowRemains = previous != null && previous != occupant
    if (occupant == agent && !oldRowRemains) return AgentWrite.Ok(this)

    val kept = filterNot { existing ->
        existing == previous || existing.name.equals(agent.name, ignoreCase = true)
    }
    return AgentWrite.Ok(listOf(agent) + kept)
}

sealed class AgentSaveResult {
    data class Saved(val agent: SavedAgent) : AgentSaveResult()
    data class Rejected(val message: String) : AgentSaveResult()
    data class Failed(val message: String) : AgentSaveResult()
}

sealed class AgentSaveNav {
    data class OpenChat(val name: String) : AgentSaveNav()
    data object Close : AgentSaveNav()
    data class Stay(val message: String) : AgentSaveNav()
}

/** Create opens the new chat. Edit closes. A rejected or failed save stays on the draft. */
fun navigationAfterAgentSave(creating: Boolean, result: AgentSaveResult): AgentSaveNav = when (result) {
    is AgentSaveResult.Saved -> if (creating) AgentSaveNav.OpenChat(result.agent.name) else AgentSaveNav.Close
    is AgentSaveResult.Rejected -> AgentSaveNav.Stay(result.message)
    is AgentSaveResult.Failed -> AgentSaveNav.Stay(result.message)
}

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
        val a = agent.capped()
        if (a.name.isBlank()) return
        dataStore.updateData { it.upserted(a) }
    }

    /**
     * Create, edit, or rename in one DataStore transaction.
     * [previousName] is the stored name being replaced. Null means create.
     * On rejection or a write failure the stored list is left as it was.
     */
    suspend fun replace(previousName: String?, agent: SavedAgent): AgentSaveResult {
        val capped = agent.capped()
        val written = try {
            dataStore.updateData { current ->
                when (val result = current.replacing(capped, previousName)) {
                    is AgentWrite.Ok -> result.agents
                    is AgentWrite.Rejected -> current
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return AgentSaveResult.Failed("Couldn't save this agent. Your previous agent is unchanged.")
        }
        return when (val result = written.replacing(capped, previousName)) {
            is AgentWrite.Ok -> {
                val stored = written.firstOrNull { it == capped }
                    ?: written.firstOrNull { it.name.equals(capped.name, ignoreCase = true) && it.blurb == capped.blurb && it.instructions == capped.instructions }
                if (stored == null) {
                    AgentSaveResult.Failed("Couldn't save this agent. Your previous agent is unchanged.")
                } else {
                    AgentSaveResult.Saved(stored)
                }
            }
            is AgentWrite.Rejected -> AgentSaveResult.Rejected(result.message)
        }
    }

    suspend fun delete(name: String) {
        dataStore.updateData { list -> list.filterNot { it.name.equals(name, ignoreCase = true) } }
    }
}

fun Context.agentStore(): AgentStore = AgentStore(agentsDataStore)
