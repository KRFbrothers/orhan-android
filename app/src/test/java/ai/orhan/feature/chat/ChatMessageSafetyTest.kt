package ai.orhan.feature.chat

import ai.orhan.data.chat.ChatAttachment
import ai.orhan.data.chat.ChatRepository
import ai.orhan.design.screens.ChatRole
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.SocketException
import kotlin.concurrent.thread
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Actual VM entrypoints: queued jobs or loopback-only SSE fixtures; no live services. */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatMessageSafetyTest {
    private val dispatcher = StandardTestDispatcher()
    private val models = mutableListOf<ChatViewModel>()
    private val agent = RosterAgent("Writer", "", "Write clearly")
    private val attachment = ChatAttachment("private.png", "image/png", "AAAA", 3)

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }

    @After fun tearDown() {
        models.forEach { it.stop() }
        dispatcher.scheduler.runCurrent()
        Dispatchers.resetMain()
    }

    private fun model(): ChatViewModel = ChatViewModel(
        repository = ChatRepository(baseUrl = "", appToken = "", tokenProvider = { null }),
    ).also { models += it }

    @Suppress("UNCHECKED_CAST")
    private fun attachments(model: ChatViewModel): Map<Long, ChatAttachment> {
        val field = ChatViewModel::class.java.getDeclaredField("attachmentsById")
        field.isAccessible = true
        return (field.get(model) as Map<Long, ChatAttachment>).toMap()
    }

    private suspend fun withStreamingModel(block: suspend (ChatViewModel, List<JSONObject>) -> Unit) {
        val requests = CopyOnWriteArrayList<JSONObject>()
        val server = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))
        val worker = thread(isDaemon = true, name = "chat-sse-fixture") {
            try {
                while (!server.isClosed) server.accept().use { socket ->
                    socket.soTimeout = 5_000
                    val input = socket.getInputStream().buffered()
                    val header = ByteArrayOutputStream()
                    var tail = 0
                    while (tail != 0x0d0a0d0a) {
                        val byte = input.read()
                        check(byte >= 0) { "Incomplete test request headers" }
                        header.write(byte)
                        tail = (tail shl 8) or byte
                    }
                    val length = header.toString("UTF-8").lineSequence()
                        .first { it.startsWith("Content-Length:", ignoreCase = true) }
                        .substringAfter(':').trim().toInt()
                    val body = ByteArray(length)
                    var offset = 0
                    while (offset < length) {
                        val count = input.read(body, offset, length - offset)
                        check(count > 0) { "Incomplete test request body" }
                        offset += count
                    }
                    requests += JSONObject(String(body, Charsets.UTF_8))
                    val response = "data: {\"type\":\"text\",\"delta\":\"reply\"}\n\n" +
                        "data: {\"type\":\"done\"}\n\n"
                    val bytes = response.toByteArray()
                    socket.getOutputStream().use { output ->
                        output.write(("HTTP/1.1 200 OK\r\nContent-Type: text/event-stream\r\n" +
                            "Content-Length: ${bytes.size}\r\nConnection: close\r\n\r\n").toByteArray())
                        output.write(bytes)
                        output.flush()
                    }
                }
            } catch (error: SocketException) {
                if (!server.isClosed) throw error
            }
        }
        val vm = ChatViewModel(repository = ChatRepository(
            baseUrl = "http://127.0.0.1:${server.localPort}", appToken = "",
            tokenProvider = { "local-unit-test" },
        )).also { models += it }
        try { block(vm, requests) } finally { vm.stop(); server.close(); worker.join(2_000) }
    }

    @Test fun actualStreamDeleteAttachmentAndReplyThenPlainTextSendsNoFile() = runTest(dispatcher, timeout = 30.seconds) {
        withStreamingModel { vm, requests ->
            vm.send("inspect", attachment)
            val first = vm.state.first { !it.busy }.messages
            assertEquals(2, first.size)
            assertEquals(listOf(ChatRole.User, ChatRole.Agent), first.map { it.role })
            assertEquals(attachment.base64, requests[0].getJSONArray("messages")
                .getJSONObject(0).getJSONArray("attachments").getJSONObject(0).getString("data"))
            first.forEach { vm.deleteMessage(it.id) }
            assertTrue(attachments(vm).isEmpty())
            vm.send("plain text")
            val second = vm.state.first { !it.busy }.messages
            assertEquals(2, second.size)
            assertEquals(4, (first + second).map { it.id }.toSet().size)
            val sent = requests[1].getJSONArray("messages")
            assertEquals(1, sent.length())
            assertEquals("plain text", sent.getJSONObject(0).getString("text"))
            assertTrue(!sent.getJSONObject(0).has("attachments"))
        }
    }

    @Test fun actualSpecialistStreamKeepsThreeDistinctIdsAndTheUserFile() = runTest(dispatcher, timeout = 30.seconds) {
        withStreamingModel { vm, requests ->
            vm.sendToAgent("@Writer inspect", agent, "inspect", attachment)
            val messages = vm.state.first { !it.busy }.messages
            assertEquals(3, messages.size)
            assertEquals(3, messages.map { it.id }.toSet().size)
            assertEquals(attachment, messages.first().attachment)
            assertEquals(attachment.base64, requests.single().getJSONArray("messages")
                .getJSONObject(0).getJSONArray("attachments").getJSONObject(0).getString("data"))
        }
    }

    @Test fun explicitAgentReservesDistinctUserAndAssignmentIds() {
        val vm = model()
        vm.sendToAgent("@Writer hello", agent, "hello", attachment)
        val messages = vm.state.value.messages
        assertEquals(listOf(ChatRole.User, ChatRole.Agent), messages.map { it.role })
        assertNotEquals(messages[0].id, messages[1].id)
        assertEquals(attachment, messages[0].attachment)
        assertEquals(listOf(attachment), turnsForApi(messages, attachments(vm))[0].attachments)
    }

    @Test fun deleteAttachmentAndAssignmentThenPlainSendCannotResendOldBytes() {
        val vm = model()
        vm.sendToAgent("@Writer inspect", agent, "inspect", attachment)
        vm.stop()
        val removed = vm.state.value.messages.map { it.id }
        removed.forEach(vm::deleteMessage)
        assertTrue(attachments(vm).isEmpty())

        vm.send("plain text")
        val messages = vm.state.value.messages
        assertEquals(1, messages.size)
        assertTrue(messages.none { it.id in removed })
        assertTrue(turnsForApi(messages, attachments(vm)).all { it.attachments.isEmpty() })
    }

    @Test fun deleteKeepsUnrelatedAttachmentAndNormalAttachmentSendWorks() {
        val vm = model()
        vm.send("keep", attachment)
        vm.stop()
        val keptId = vm.state.value.messages.single().id
        val other = attachment.copy(name = "other.png", base64 = "BBBB")
        vm.send("remove", other)
        vm.stop()
        val removedId = vm.state.value.messages.last().id
        vm.deleteMessage(removedId)
        assertEquals(mapOf(keptId to attachment), attachments(vm))
        assertEquals(listOf(attachment), turnsForApi(vm.state.value.messages, attachments(vm)).single().attachments)
    }

    @Test fun pendingReplyReservationIsNotReusedAfterDeletionOrAcrossModels() {
        val vm = model()
        vm.send("first") // Reserves the user and the not-yet-visible streaming reply.
        val firstId = vm.state.value.messages.single().id
        vm.stop()
        vm.deleteMessage(firstId)
        vm.send("second")
        val secondId = vm.state.value.messages.single().id
        assertTrue(secondId > firstId + 1L)

        val other = model()
        other.send("another chat")
        assertTrue(other.state.value.messages.single().id > secondId + 1L)
    }
}
