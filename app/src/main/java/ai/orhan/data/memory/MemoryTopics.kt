package ai.orhan.data.memory

import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * On-device topics for the Memory screen.
 *
 * Version 1 files are notes + to-dos. Notes become topics in the "Notes" group
 * and are not written back, so an older file still opens and is not duplicated.
 * To-dos stay to-dos (Next Actions). Topics are never added to chat context.
 */

const val TOPICS_FORMAT_VERSION = 2
const val DEFAULT_TOPIC_GROUP = "General"
const val MIGRATED_NOTES_GROUP = "Notes"
const val MAX_SUMMARY = 280
const val MAX_GROUP_NAME = 48
const val MAX_TAGS = 12
const val MAX_TAG_LENGTH = 32

data class MemoryTopic(
    val id: String,
    val title: String,
    val summary: String,
    val content: String,
    val group: String,
    val tags: List<String>,
    val unread: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

internal fun MemoryEntries.withNotesMigrated(): MemoryEntries {
    if (notes.isEmpty()) return this
    val known = topics.map { it.id }.toSet()
    val added = notes.filter { it.id !in known }.map { it.toTopic() }
    return copy(notes = emptyList(), topics = topics + added)
}

internal fun MemoryEntry.toTopic(): MemoryTopic = MemoryTopic(
    id = id,
    title = title,
    summary = content.lineSequence().firstOrNull().orEmpty().trim().take(MAX_SUMMARY),
    content = content,
    group = MIGRATED_NOTES_GROUP,
    tags = emptyList(),
    unread = false,
    createdAt = createdAt,
    updatedAt = createdAt,
)

/** Blank titles are rejected. Tags are stored without a leading '#'. */
internal fun prepareTopic(
    existing: MemoryTopic?,
    title: String,
    summary: String,
    content: String,
    group: String,
    tags: List<String>,
    now: Long,
    newId: String,
): MemoryTopic? {
    val cleanTitle = title.trim().take(MAX_ENTRY_TITLE)
    if (cleanTitle.isBlank()) return null
    val cleanTags = tags.map { normalizeTag(it) }.filter { it.isNotEmpty() }.distinct().take(MAX_TAGS)
    val cleanGroup = group.trim().ifBlank { DEFAULT_TOPIC_GROUP }.take(MAX_GROUP_NAME)
    val cleanSummary = summary.trim().take(MAX_SUMMARY)
    val cleanContent = content.trim().take(MAX_NOTE_CONTENT)
    val changed = existing == null ||
        existing.title != cleanTitle ||
        existing.summary != cleanSummary ||
        existing.content != cleanContent ||
        existing.group != cleanGroup ||
        existing.tags != cleanTags
    return MemoryTopic(
        id = existing?.id ?: newId,
        title = cleanTitle,
        summary = cleanSummary,
        content = cleanContent,
        group = cleanGroup,
        tags = cleanTags,
        // A save the user just made is not an unread update.
        unread = if (changed) false else existing?.unread ?: false,
        createdAt = existing?.createdAt ?: now,
        updatedAt = if (changed) now else existing?.updatedAt ?: now,
    )
}

internal fun normalizeTag(raw: String): String =
    raw.trim().trimStart('#').trim().lowercase().take(MAX_TAG_LENGTH)

internal fun List<MemoryTopic>.matching(query: String, tags: Set<String>, unreadOnly: Boolean): List<MemoryTopic> {
    val q = query.trim()
    return filter { topic ->
        if (unreadOnly && !topic.unread) return@filter false
        if (tags.isNotEmpty() && topic.tags.none { tag -> tags.any { it.equals(tag, ignoreCase = true) } }) {
            return@filter false
        }
        if (q.isEmpty()) return@filter true
        topic.title.contains(q, ignoreCase = true) ||
            topic.summary.contains(q, ignoreCase = true) ||
            topic.content.contains(q, ignoreCase = true) ||
            topic.tags.any { it.contains(q, ignoreCase = true) }
    }.sortedByDescending { it.updatedAt }
}

/** Count behind the All chip: topics matching the current search and tags. */
internal fun allCount(topics: List<MemoryTopic>, query: String, tags: Set<String>): Int =
    topics.matching(query, tags, unreadOnly = false).size

/**
 * Count behind Updates. Observed on the reference screen: this number did not
 * change when a tag or a search narrowed All, so it is the global unread count.
 */
internal fun updatesCount(topics: List<MemoryTopic>): Int = topics.count { it.unread }

internal data class TopicGroup(val name: String, val topics: List<MemoryTopic>)

internal fun groupTopics(topics: List<MemoryTopic>): List<TopicGroup> =
    topics.groupBy { it.group.ifBlank { DEFAULT_TOPIC_GROUP } }
        .map { (name, items) -> TopicGroup(name, items.sortedByDescending { it.updatedAt }) }
        .sortedByDescending { it.topics.maxOf { topic -> topic.updatedAt } }

/** "about 2 hours ago" for the last day, otherwise the chat-list date. */
internal fun memoryTimestamp(then: Long, now: Long, older: (Long, Long) -> String): String {
    val delta = now - then
    if (delta < 0) return older(then, now)
    if (delta < 60_000L) return "less than a minute ago"
    val minutes = delta / 60_000L
    if (minutes < 60) return if (minutes == 1L) "about 1 minute ago" else "about $minutes minutes ago"
    val hours = delta / 3_600_000L
    if (hours < 24) return if (hours == 1L) "about 1 hour ago" else "about $hours hours ago"
    return older(then, now)
}

internal fun encodeMemoryEntries(entries: MemoryEntries): ByteArray {
    val migrated = entries.withNotesMigrated()
    val buffer = ByteArrayOutputStream()
    DataOutputStream(buffer).use { out ->
        out.writeInt(TOPICS_FORMAT_VERSION)
        out.writeEntryList(emptyList())
        out.writeEntryList(migrated.todos)
        out.writeTopicList(migrated.topics)
    }
    return buffer.toByteArray()
}

internal fun decodeMemoryEntries(input: InputStream): MemoryEntries {
    val bytes = input.readBytes()
    if (bytes.isEmpty()) return MemoryEntries()
    return try {
        DataInputStream(bytes.inputStream()).use { inputStream ->
            when (val version = inputStream.readInt()) {
                1 -> {
                    val notes = inputStream.readEntryList()
                    val todos = inputStream.readEntryList()
                    MemoryEntries(notes = notes, todos = todos).withNotesMigrated()
                }
                TOPICS_FORMAT_VERSION -> {
                    val notes = inputStream.readEntryList()
                    val todos = inputStream.readEntryList()
                    val topics = inputStream.readTopicList()
                    MemoryEntries(notes = notes, todos = todos, topics = topics).withNotesMigrated()
                }
                else -> MemoryEntries()
            }
        }
    } catch (_: IOException) {
        MemoryEntries()
    }
}

internal fun encodeMemoryEntries(entries: MemoryEntries, output: OutputStream) {
    output.write(encodeMemoryEntries(entries))
}

private fun DataOutputStream.writeEntryList(list: List<MemoryEntry>) {
    writeInt(list.size)
    list.forEach { entry ->
        writeUTF(entry.id)
        writeUTF(entry.title.take(MAX_ENTRY_TITLE))
        val body = entry.content.toByteArray(Charsets.UTF_8)
        writeInt(body.size)
        write(body)
        writeBoolean(entry.done)
        writeLong(entry.createdAt)
    }
}

private fun DataInputStream.readEntryList(): List<MemoryEntry> {
    val count = readInt()
    if (count < 0 || count > MAX_ENTRIES * 2) throw IOException("bad count")
    return List(count) {
        MemoryEntry(
            id = readUTF(),
            title = readUTF(),
            content = readBoundedString(MAX_NOTE_CONTENT * 4),
            done = readBoolean(),
            createdAt = readLong(),
        )
    }
}

private fun DataOutputStream.writeTopicList(list: List<MemoryTopic>) {
    writeInt(list.size)
    list.forEach { topic ->
        writeUTF(topic.id)
        writeUTF(topic.title.take(MAX_ENTRY_TITLE))
        writeBoundedString(topic.summary.take(MAX_SUMMARY))
        writeBoundedString(topic.content.take(MAX_NOTE_CONTENT))
        writeUTF(topic.group.take(MAX_GROUP_NAME))
        val tags = topic.tags.map { normalizeTag(it) }.filter { it.isNotEmpty() }.distinct().take(MAX_TAGS)
        writeInt(tags.size)
        tags.forEach { writeUTF(it) }
        writeBoolean(topic.unread)
        writeLong(topic.createdAt)
        writeLong(topic.updatedAt)
    }
}

private fun DataInputStream.readTopicList(): List<MemoryTopic> {
    val count = readInt()
    if (count < 0 || count > MAX_ENTRIES * 2) throw IOException("bad count")
    return List(count) {
        val id = readUTF()
        val title = readUTF()
        val summary = readBoundedString(MAX_SUMMARY * 4)
        val content = readBoundedString(MAX_NOTE_CONTENT * 4)
        val group = readUTF()
        val tagCount = readInt()
        if (tagCount < 0 || tagCount > MAX_TAGS * 2) throw IOException("bad tags")
        val tags = List(tagCount) { normalizeTag(readUTF()) }.filter { it.isNotEmpty() }
        MemoryTopic(
            id = id,
            title = title,
            summary = summary,
            content = content,
            group = group.ifBlank { DEFAULT_TOPIC_GROUP },
            tags = tags,
            unread = readBoolean(),
            createdAt = readLong(),
            updatedAt = readLong(),
        )
    }
}

private fun DataOutputStream.writeBoundedString(value: String) {
    val body = value.toByteArray(Charsets.UTF_8)
    writeInt(body.size)
    write(body)
}

private fun DataInputStream.readBoundedString(maxBytes: Int): String {
    val size = readInt()
    if (size < 0 || size > maxBytes) throw IOException("bad length")
    val body = ByteArray(size)
    readFully(body)
    return String(body, Charsets.UTF_8)
}
