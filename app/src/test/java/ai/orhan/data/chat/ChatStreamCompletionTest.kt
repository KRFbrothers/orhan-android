package ai.orhan.data.chat

import ai.orhan.feature.chat.ChatViewModel
import ai.orhan.feature.chat.RosterAgent
import ai.orhan.design.screens.ChatRole
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatStreamCompletionTest {
    private val dispatcher = StandardTestDispatcher()
    private val models = mutableListOf<ChatViewModel>()
    private val text = "event: text\ndata: {\"type\":\"text\",\"delta\":\"partial\"}\n\n"
    private val done = "event: done\ndata: {\"type\":\"done\"}\n\n"

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() {
        models.forEach { it.stop() }
        dispatcher.scheduler.runCurrent()
        Dispatchers.resetMain()
    }

    private fun model(fixture: StreamFixture) = ChatViewModel(repository = fixture.repository()).also { models += it }

    private suspend fun read(fixture: StreamFixture, deltas: StringBuilder = StringBuilder()) =
        fixture.repository().stream(listOf(ChatTurn("user", "hello")), onDelta = { deltas.append(it) })

    @Test fun completeTextAndDoneSucceedsWithCommentsMultilineDataAndMetadata() = runTest(dispatcher, timeout = 30.seconds) {
        val body = ": heartbeat\n\nevent: sources\ndata: {\"type\":\"sources\",\"sources\":[]}\n\n" +
            "event: progress\ndata: {\"type\":\"progress\",\"message\":\"Working\"}\n\n" +
            "event: text\ndata: {\"type\":\"text\",\ndata: \"delta\":\"hello\"}\n\n" + done
        StreamFixture(Reply(body)).use { fixture ->
            assertEquals("hello", read(fixture).getOrThrow())
            assertEquals(1, fixture.requests.size)
        }
    }

    @Test fun eventNameSuppliesTypeWhenJsonOmitsIt() = runTest(dispatcher, timeout = 30.seconds) {
        StreamFixture(Reply("event: text\ndata: {\"delta\":\"hello\"}\n\nevent: done\ndata: {}\n\n")).use {
            assertEquals("hello", read(it).getOrThrow())
        }
    }

    @Test fun textThenCleanEofFailsWithoutLosingDeliveredDelta() = runTest(dispatcher, timeout = 30.seconds) {
        StreamFixture(Reply(text)).use {
            val deltas = StringBuilder()
            val result = read(it, deltas)
            assertTrue(result.isFailure)
            assertEquals("partial", deltas.toString())
            assertTrue(result.exceptionOrNull()!!.message!!.contains("interrupted"))
            assertFalse(result.exceptionOrNull() is ChatRepository.FallbackEligibleStreamException)
        }
    }

    @Test fun eofBeforeTextFailsWithoutFallbackEligibility() = runTest(dispatcher, timeout = 30.seconds) {
        StreamFixture(Reply(": heartbeat\n\n")).use {
            val result = read(it)
            assertTrue(result.isFailure)
            assertFalse(result.exceptionOrNull() is ChatRepository.FallbackEligibleStreamException)
        }
    }

    @Test fun malformedFrameCannotBeSkippedEvenWhenDoneFollows() = runTest(dispatcher, timeout = 30.seconds) {
        StreamFixture(Reply(text + "data: {\"type\":\n\n" + done)).use {
            val deltas = StringBuilder()
            assertTrue(read(it, deltas).isFailure)
            assertEquals("partial", deltas.toString())
        }
    }

    @Test fun truncatedTextFrameThenEofFails() = runTest(dispatcher, timeout = 30.seconds) {
        StreamFixture(Reply(text + "data: {\"type\":\"text\",\"delta\":\"cut")).use {
            val deltas = StringBuilder()
            assertTrue(read(it, deltas).isFailure)
            assertEquals("partial", deltas.toString())
        }
    }

    @Test fun doneWithoutFinalBlankLineIsNotDispatched() = runTest(dispatcher, timeout = 30.seconds) {
        StreamFixture(Reply(text + "data: {\"type\":\"done\"}\n")).use {
            assertTrue(read(it).isFailure)
        }
    }

    @Test fun trailingGarbageCannotTurnDoneIntoSuccess() = runTest(dispatcher, timeout = 30.seconds) {
        StreamFixture(Reply(text + "data: {\"type\":\"done\"} garbage\n\n")).use {
            assertTrue(read(it).isFailure)
        }
    }

    @Test fun explicitServerErrorAfterTextRetainsTextAndError() = runTest(dispatcher, timeout = 30.seconds) {
        StreamFixture(Reply(text + "event: error\ndata: {\"type\":\"error\",\"message\":\"Service stopped\"}\n\n")).use {
            val deltas = StringBuilder()
            val result = read(it, deltas)
            assertEquals("partial", deltas.toString())
            assertEquals("Service stopped", result.exceptionOrNull()!!.message)
        }
    }

    @Test fun rawDoneSentinelIsNotTheEnginesJsonCompletionProtocol() = runTest(dispatcher, timeout = 30.seconds) {
        StreamFixture(Reply(text + "data: [DONE]\n\n")).use { assertTrue(read(it).isFailure) }
    }

    @Test fun explicitDoneStillRequiresNonemptyText() = runTest(dispatcher, timeout = 30.seconds) {
        StreamFixture(Reply(done)).use { assertTrue(read(it).isFailure) }
    }

    @Test fun serviceTruncationFlagDoesNotClaimACompleteAnswer() = runTest(dispatcher, timeout = 30.seconds) {
        StreamFixture(Reply(text + "data: {\"type\":\"done\",\"truncated\":true}\n\n")).use {
            val result = read(it)
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()!!.message!!.contains("output limit"))
        }
    }

    @Test fun repositoryCancellationDoesNotReturnSuccessOrReplay() = runTest(dispatcher, timeout = 30.seconds) {
        StreamFixture(Reply(text, holdOpen = true)).use { fixture ->
            val deltaReceived = CompletableDeferred<Unit>()
            var returned = false
            val job = launch {
                fixture.repository().stream(listOf(ChatTurn("user", "hello")), onDelta = { deltaReceived.complete(Unit) })
                returned = true
            }
            deltaReceived.await()
            job.cancelAndJoin()
            assertTrue(job.isCancelled)
            assertFalse(returned)
            assertEquals(1, fixture.requests.size)
        }
    }

    @Test fun translationInterruptionReturnsFailureNotPartialSuccess() = runTest(dispatcher, timeout = 30.seconds) {
        StreamFixture(Reply(text)).use { fixture ->
            val result = model(fixture).translateMessage("hello", "Hindi")
            assertTrue(result.isFailure)
            assertNull(result.getOrNull())
            assertEquals(1, fixture.requests.size)
            assertEquals("auto", fixture.requests.single().getString("mode"))
        }
    }

    @Test fun completedTranslationStillSucceeds() = runTest(dispatcher, timeout = 30.seconds) {
        StreamFixture(Reply(text + done)).use { assertEquals("partial", model(it).translateMessage("hello", "Hindi").getOrThrow()) }
    }

    @Test fun viewModelKeepsPartialAndErrorUntilExplicitManualRetry() = runTest(dispatcher, timeout = 30.seconds) {
        StreamFixture(Reply(text), Reply(text.replace("partial", "complete") + done)).use { fixture ->
            val vm = model(fixture)
            vm.send("hello")
            val interrupted = vm.state.first { !it.busy }
            assertEquals("partial", interrupted.messages.last().text)
            assertFalse(interrupted.loading)
            assertTrue(interrupted.error!!.contains("interrupted"))
            assertEquals(1, fixture.requests.size)
            vm.retry()
            val retried = vm.state.first { !it.busy }
            assertNull(retried.error)
            assertEquals(2, retried.messages.size)
            assertEquals("complete", retried.messages.last().text)
            assertEquals(2, fixture.requests.size)
            assertTrue(fixture.requests.all { it.getBoolean("stream") })
        }
    }

    @Test fun viewModelEarlyEofDoesNotStartNonstreamFallback() = runTest(dispatcher, timeout = 30.seconds) {
        StreamFixture(Reply("")).use { fixture ->
            val vm = model(fixture)
            vm.send("hello")
            val state = vm.state.first { !it.busy }
            assertFalse(state.loading)
            assertNotNull(state.error)
            assertEquals(1, state.messages.size)
            assertEquals(1, fixture.requests.size)
        }
    }

    @Test fun specialistInterruptionKeepsLabeledPartialAndReportsError() = runTest(dispatcher, timeout = 30.seconds) {
        StreamFixture(Reply(text)).use { fixture ->
            val vm = model(fixture)
            vm.sendToAgent("@Writer hello", RosterAgent("Writer", "", "Write"), "hello")
            val state = vm.state.first { !it.busy }
            assertEquals(3, state.messages.size)
            assertTrue(state.messages.last().text.endsWith("partial"))
            assertTrue(state.error!!.startsWith("Writer:"))
            assertFalse(state.loading)
            assertEquals(1, fixture.requests.size)
        }
    }

    @Test fun legitimateUnsupportedSseStatusStillFallsBackOnce() = runTest(dispatcher, timeout = 30.seconds) {
        for (status in listOf(406, 415)) {
            StreamFixture(Reply("{}", status, "application/json"), Reply("{\"text\":\"fallback\"}", contentType = "application/json")).use { fixture ->
                val vm = model(fixture)
                vm.send("hello")
                val state = vm.state.first { !it.busy }
                assertNull(state.error)
                assertEquals("fallback", state.messages.last().text)
                assertEquals(listOf(true, false), fixture.requests.map { it.getBoolean("stream") })
            }
        }
    }

    @Test fun nonSseResponseStillUsesExistingFallbackPath() = runTest(dispatcher, timeout = 30.seconds) {
        StreamFixture(Reply("{}", contentType = "application/json"), Reply("{\"text\":\"fallback\"}", contentType = "application/json")).use { fixture ->
            val vm = model(fixture)
            vm.send("hello")
            assertNull(vm.state.first { !it.busy }.error)
            assertEquals(listOf(true, false), fixture.requests.map { it.getBoolean("stream") })
        }
    }

    @Test fun stoppingViewModelKeepsPartialWithoutInterruptionError() = runTest(dispatcher, timeout = 30.seconds) {
        StreamFixture(Reply(text, holdOpen = true)).use { fixture ->
            val vm = model(fixture)
            vm.send("hello")
            vm.state.first { it.messages.any { message -> message.role == ChatRole.Agent } }
            vm.stop()
            dispatcher.scheduler.runCurrent()
            assertFalse(vm.state.value.busy)
            assertFalse(vm.state.value.loading)
            assertNull(vm.state.value.error)
            assertEquals("partial", vm.state.value.messages.last().text)
            assertEquals(1, fixture.requests.size)
        }
    }
}

/** Loopback-only HTTP fixture: no external endpoints, tokens or paid requests. */
private data class Reply(val body: String, val status: Int = 200, val contentType: String = "text/event-stream", val holdOpen: Boolean = false)

private class StreamFixture(vararg replies: Reply) : java.io.Closeable {
    val requests = CopyOnWriteArrayList<JSONObject>()
    private val server = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))
    private val sockets = CopyOnWriteArrayList<Socket>()
    private val released = CountDownLatch(1)
    private val failure = AtomicReference<Throwable?>()
    private val worker = thread(isDaemon = true, name = "sse-completion-fixture") {
        try {
            while (!server.isClosed) server.accept().use { socket ->
                sockets += socket
                try {
                    socket.soTimeout = 5_000
                    val input = socket.getInputStream().buffered()
                    val header = ByteArrayOutputStream()
                    var tail = 0
                    while (tail != 0x0d0a0d0a) {
                        val byte = input.read()
                        check(byte >= 0 && header.size() < 65_536) { "Invalid test HTTP headers" }
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
                        check(count > 0) { "Incomplete test HTTP body" }
                        offset += count
                    }
                    val index = requests.size
                    requests += JSONObject(String(body, Charsets.UTF_8))
                    val reply = replies.getOrElse(index) { Reply("Unexpected replay", 500, "text/plain") }
                    val bytes = reply.body.toByteArray(Charsets.UTF_8)
                    val output = socket.getOutputStream()
                    val lengthHeader = if (reply.holdOpen) "" else "Content-Length: ${bytes.size}\r\n"
                    output.write(("HTTP/1.1 ${reply.status} Test\r\nContent-Type: ${reply.contentType}\r\n" +
                        lengthHeader + "Connection: close\r\n\r\n").toByteArray())
                    output.write(bytes)
                    output.flush()
                    if (reply.holdOpen) try {
                        while (!released.await(25, TimeUnit.MILLISECONDS)) {
                            output.write(": heartbeat\n\n".toByteArray())
                            output.flush()
                        }
                    } catch (_: IOException) { /* Expected when the client cancels. */ }
                } finally { sockets -= socket }
            }
        } catch (error: Throwable) { if (!server.isClosed) failure.set(error) }
    }

    fun repository() = ChatRepository(
        baseUrl = "http://127.0.0.1:${server.localPort}", appToken = "", tokenProvider = { "local-unit-test" },
    )

    override fun close() {
        released.countDown()
        server.close()
        sockets.forEach { it.close() }
        worker.join(2_000)
        failure.get()?.let { throw AssertionError("Loopback fixture failed", it) }
    }
}
