package ai.orhan.data.memory

import ai.orhan.data.artifacts.Artifact
import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

/**
 * Next Actions and topic-to-artifact links. Fixtures are synthetic.
 * Saving an action writes a to-do only. It does not call a chat or a tool.
 */
class MemoryActionsTest {

    @Test fun version4RoundTripKeepsActionSourceArtifactIdAndDismissedKeys() {
        val original = MemoryEntries(
            todos = listOf(
                MemoryEntry("t1", "Call back", "details", false, 11, sourceChatTitle = "Orhan", sourceMessageId = 44),
            ),
            topics = listOf(
                MemoryTopic(
                    id = "p1",
                    title = "Plan",
                    summary = "short",
                    content = "topic body",
                    group = "Plans",
                    tags = listOf("home"),
                    unread = true,
                    createdAt = 1,
                    updatedAt = 2,
                    sourceChatTitle = "Orhan",
                    sourceMessageId = 9,
                    artifactId = "art-1",
                ),
            ),
            dismissedActionSources = listOf(sourceKey("Orhan", 3)),
        )
        val bytes = encodeMemoryEntries(original)
        assertEquals(TOPICS_FORMAT_VERSION, DataInputStream(bytes.inputStream()).readInt())
        val back = decodeMemoryEntries(bytes.inputStream())
        assertEquals(original.todos, back.todos)
        assertEquals("art-1", back.topics.single().artifactId)
        assertEquals(original.topics, back.topics)
        assertEquals(original.dismissedActionSources, back.dismissedActionSources)
    }

    @Test fun olderFilesLeaveNewFieldsEmpty() {
        val v3 = encodeLegacy(
            version = 3,
            todo = MemoryEntry("t1", "Call back", "details", true, 11),
            withTopicSource = true,
        )
        val fromV3 = decodeMemoryEntries(v3.inputStream())
        assertEquals("", fromV3.todos.single().sourceChatTitle)
        assertEquals(0L, fromV3.todos.single().sourceMessageId)
        assertTrue(fromV3.todos.single().done)
        assertEquals("Orhan", fromV3.topics.single().sourceChatTitle)
        assertEquals(9L, fromV3.topics.single().sourceMessageId)
        assertEquals("", fromV3.topics.single().artifactId)
        assertTrue(fromV3.dismissedActionSources.isEmpty())

        val v2 = encodeLegacy(version = 2, todo = null, withTopicSource = false)
        val fromV2 = decodeMemoryEntries(v2.inputStream())
        assertEquals("", fromV2.topics.single().sourceChatTitle)
        assertEquals("", fromV2.topics.single().artifactId)
        assertTrue(fromV2.dismissedActionSources.isEmpty())

        val emptyV3 = ByteArrayOutputStream().also { out ->
            DataOutputStream(out).use { it.writeInt(3); it.writeInt(0); it.writeInt(0); it.writeInt(0) }
        }.toByteArray()
        assertEquals(16, emptyV3.size)
        assertEquals(MemoryEntries(), decodeMemoryEntries(emptyV3.inputStream()))
    }

    @Test fun brokenDismissedListDoesNotThrow() {
        val broken = ByteArrayOutputStream().also { out ->
            DataOutputStream(out).use {
                it.writeInt(TOPICS_FORMAT_VERSION)
                it.writeInt(0)
                it.writeInt(0)
                it.writeInt(0)
                it.writeInt(MAX_DISMISSED_ACTIONS * 2 + 1)
            }
        }.toByteArray()
        assertEquals(MemoryEntries(), decodeMemoryEntries(broken.inputStream()))
    }

    @Test fun saveEditCompleteReopenAndDeletePersistUntilWriteFails() = runBlocking {
        val data = ActionDataStore(MemoryEntries())
        val store = MemoryEntriesStore(data)
        assertEquals(ActionSaveResult.Blank, store.saveTodo(null, "  "))
        assertTrue(data.state.value.todos.isEmpty())

        assertEquals(ActionSaveResult.Saved, store.saveTodo(null, " Call home ", " tonight "))
        val saved = data.state.value.todos.single()
        assertEquals("Call home", saved.title)
        assertEquals("tonight", saved.content)
        assertFalse(saved.done)
        assertEquals("", saved.sourceChatTitle)

        assertEquals(ActionSaveResult.Saved, store.saveTodo(saved.id, "Call home", "tomorrow"))
        assertEquals("tomorrow", data.state.value.todos.single().content)
        assertEquals(saved.createdAt, data.state.value.todos.single().createdAt)

        assertTrue(store.toggleDone(saved.id))
        assertTrue(data.state.value.todos.single().done)
        assertTrue(store.toggleDone(saved.id))
        assertFalse(data.state.value.todos.single().done)

        data.failWrites = true
        assertEquals(ActionSaveResult.WriteFailed, store.saveTodo(saved.id, "Changed", "x"))
        assertEquals("Call home", data.state.value.todos.single().title)
        assertFalse(store.toggleDone(saved.id))
        assertFalse(data.state.value.todos.single().done)
        assertFalse(store.deleteTodo(saved.id))
        assertEquals(listOf(saved.id), data.state.value.todos.map { it.id })
        assertTrue(data.state.value.dismissedActionSources.isEmpty())

        data.failWrites = false
        assertTrue(store.deleteTodo(saved.id))
        assertTrue(data.state.value.todos.isEmpty())
    }

    @Test fun duplicateTitlesAreRejectedWhileTheActionExists() = runBlocking {
        val data = ActionDataStore(MemoryEntries())
        val store = MemoryEntriesStore(data)
        assertEquals(ActionSaveResult.Saved, store.saveTodo(null, "Buy milk"))
        val first = data.state.value.todos.single()
        assertEquals(ActionSaveResult.Duplicate, store.saveTodo(null, "  buy   milk "))
        assertEquals(listOf(first), data.state.value.todos)
        assertEquals(ActionSaveResult.Saved, store.saveTodo(null, "Buy bread"))
        val bread = data.state.value.todos.first { it.title == "Buy bread" }
        assertEquals(ActionSaveResult.Duplicate, store.saveTodo(bread.id, "Buy milk", "nope"))
        assertEquals("Buy bread", data.state.value.todos.first { it.id == bread.id }.title)
        assertEquals(ActionSaveResult.Saved, store.saveTodo(first.id, "Buy milk", "same"))
        assertEquals(2, data.state.value.todos.size)
    }

    @Test fun chatActionKeepsSourceAndIsNotRecreatedAfterDelete() = runBlocking {
        val existingTopic = MemoryTopic(
            id = "kept",
            title = "Kept",
            summary = "",
            content = "stay",
            group = DEFAULT_TOPIC_GROUP,
            tags = emptyList(),
            unread = false,
            createdAt = 1,
            updatedAt = 1,
        )
        val data = ActionDataStore(MemoryEntries(topics = listOf(existingTopic)))
        val store = MemoryEntriesStore(data)
        val message = "Buy milk\nfrom the shop"
        assertEquals(ActionSaveResult.Blank, store.addActionFromChat("  ", "Orhan", 7))
        assertEquals(ActionSaveResult.Blank, store.addActionFromChat(message, "Orhan", 0))
        assertEquals(ActionSaveResult.Saved, store.addActionFromChat(message, " Orhan ", 7))
        val action = data.state.value.todos.single()
        assertEquals("Buy milk", action.title)
        assertEquals(message, action.content)
        assertEquals("Orhan", action.sourceChatTitle)
        assertEquals(7L, action.sourceMessageId)
        assertFalse(action.done)
        assertEquals(listOf(existingTopic), data.state.value.topics)

        assertEquals(ActionSaveResult.Duplicate, store.addActionFromChat(message, "Orhan", 7))
        assertEquals(ActionSaveResult.Duplicate, store.saveTodo(null, "Buy milk"))
        assertEquals(1, data.state.value.todos.size)

        assertEquals(ActionSaveResult.Saved, store.saveTodo(action.id, "Buy oat milk", action.content))
        assertEquals("Orhan", data.state.value.todos.single().sourceChatTitle)
        assertEquals(7L, data.state.value.todos.single().sourceMessageId)

        data.failWrites = true
        assertEquals(ActionSaveResult.WriteFailed, store.addActionFromChat("Other task", "Orhan", 8))
        assertFalse(store.deleteTodo(action.id))
        assertTrue(data.state.value.dismissedActionSources.isEmpty())
        assertEquals(1, data.state.value.todos.size)
        data.failWrites = false

        assertTrue(store.deleteTodo(action.id))
        assertTrue(data.state.value.todos.isEmpty())
        assertEquals(listOf(sourceKey("Orhan", 7)), data.state.value.dismissedActionSources)
        assertEquals(ActionSaveResult.Dismissed, store.addActionFromChat(message, "Orhan", 7))
        assertTrue(data.state.value.todos.isEmpty())

        assertEquals(ActionSaveResult.Saved, store.saveTodo(null, "Buy milk", "typed again"))
        val typed = data.state.value.todos.single()
        assertEquals("", typed.sourceChatTitle)
        assertEquals(0L, typed.sourceMessageId)
        assertEquals(ActionSaveResult.Dismissed, store.addActionFromChat("Buy milk again", "Orhan", 7))
        assertEquals(listOf(typed.id), data.state.value.todos.map { it.id })
        assertEquals(listOf(existingTopic), data.state.value.topics)
    }

    @Test fun artifactLinkUsesTheIdAndDoesNotCopyTheBody() = runBlocking {
        val body = "artifact body that must stay in the artifact store"
        val data = ActionDataStore(MemoryEntries())
        val store = MemoryEntriesStore(data)
        assertEquals(ArtifactLink.None, artifactLink("  ", listOf(Artifact("art-1", "Doc", body, 1))))
        assertEquals(ArtifactLink.Missing, artifactLink("missing", emptyList()))

        assertTrue(store.upsertTopic(null, "Plan", "sum", "topic body", "Plans", emptyList(), "art-1"))
        val topic = data.state.value.topics.single()
        assertEquals("art-1", topic.artifactId)
        assertEquals("topic body", topic.content)
        assertFalse(topic.content.contains("artifact body"))

        val renamed = Artifact(topic.artifactId, "After rename", body, 1)
        val ready = artifactLink(topic.artifactId, listOf(renamed))
        assertEquals(ArtifactLink.Ready("art-1", "After rename"), ready)

        val bytes = encodeMemoryEntries(data.state.value)
        val restored = decodeMemoryEntries(bytes.inputStream())
        assertEquals("art-1", restored.topics.single().artifactId)
        assertEquals("topic body", restored.topics.single().content)

        assertTrue(store.upsertTopic(topic.id, "Plan", "sum", "topic body", "Plans", emptyList()))
        assertEquals("art-1", data.state.value.topics.single().artifactId)
        assertTrue(store.upsertTopic(topic.id, "Plan", "sum", "topic body", "Plans", emptyList(), ""))
        assertEquals("", data.state.value.topics.single().artifactId)
        assertEquals(ArtifactLink.Missing, artifactLink("art-1", emptyList()))
    }

    private class ActionDataStore(initial: MemoryEntries) : DataStore<MemoryEntries> {
        val state = MutableStateFlow(initial)
        var failWrites = false
        override val data: Flow<MemoryEntries> = state
        override suspend fun updateData(transform: suspend (t: MemoryEntries) -> MemoryEntries): MemoryEntries {
            val next = transform(state.value)
            if (failWrites) throw IOException("disk full")
            state.value = next
            return next
        }
    }

    private fun encodeLegacy(version: Int, todo: MemoryEntry?, withTopicSource: Boolean): ByteArray {
        val out = ByteArrayOutputStream()
        DataOutputStream(out).use { data ->
            data.writeInt(version)
            data.writeInt(0)
            if (todo == null) {
                data.writeInt(0)
            } else {
                data.writeInt(1)
                data.writeUTF(todo.id)
                data.writeUTF(todo.title)
                val body = todo.content.toByteArray(Charsets.UTF_8)
                data.writeInt(body.size)
                data.write(body)
                data.writeBoolean(todo.done)
                data.writeLong(todo.createdAt)
            }
            data.writeInt(1)
            data.writeUTF("p1")
            data.writeUTF("Plan")
            data.writeBounded("short")
            data.writeBounded("topic body")
            data.writeUTF("Plans")
            data.writeInt(1)
            data.writeUTF("home")
            data.writeBoolean(true)
            data.writeLong(1)
            data.writeLong(2)
            if (withTopicSource) {
                data.writeUTF("Orhan")
                data.writeLong(9)
            }
        }
        return out.toByteArray()
    }

    private fun DataOutputStream.writeBounded(value: String) {
        val body = value.toByteArray(Charsets.UTF_8)
        writeInt(body.size)
        write(body)
    }
}
