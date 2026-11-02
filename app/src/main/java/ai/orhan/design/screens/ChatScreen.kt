package ai.orhan.design.screens

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.app.Activity
import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.content.ActivityNotFoundException
import android.net.Uri
import android.provider.OpenableColumns
import android.speech.RecognizerIntent
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.rememberCoroutineScope
import ai.orhan.data.chat.ChatAttachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import ai.orhan.R
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ai.orhan.design.theme.OrhanTextStyles
import ai.orhan.design.theme.OrhanTheme

/* ============================================================================
 *  Orhan AI — Chat screen (Material 3)
 *
 *  One composer. The draft, multiline field, Enter-to-send and IME inset stay
 *  in this file. Attach offers only the file types the engine accepts.
 *  Long-press on a message opens Copy, Share, and (agent replies) Save.
 * ==========================================================================*/

/** Author of a message. */
enum class ChatRole { Agent, User }

internal fun voiceAttachmentFromBytes(bytes: ByteArray): ChatAttachment? =
    if (bytes.isEmpty() || bytes.size > ChatAttachment.MAX_BYTES) null
    else ChatAttachment("voice-message.m4a", "audio/mp4", java.util.Base64.getEncoder().encodeToString(bytes), bytes.size.toLong())

/** The inline preview card that sits under an agent bubble. */
data class ChatArtifact(
    val name: String,
    val meta: String,
)

/**
 * One message in the thread.
 * [artifact] is non-null on agent replies that produced something inline.
 */
data class ChatMessage(
    val id: Long,
    val role: ChatRole,
    val text: String,
    val time: String,
    val artifact: ChatArtifact? = null,
    /** Epoch millis. Zero for bubbles saved before automatic memory existed. */
    val createdAt: Long = 0L,
    /** Inline payload restored from the local attachment store; never serialized in the thread record. */
    val attachment: ChatAttachment? = null,
)

/**
 * Saver for the whole thread. Fields are joined with U+001F (unit separator),
 * which cannot occur in chat text, so the round-trip is lossless.
 */
private val ChatThreadSaver: Saver<List<ChatMessage>, ArrayList<String>> = Saver(
    save = { list ->
        ArrayList(
            list.map { m ->
                listOf(
                    m.id.toString(),
                    m.role.name,
                    m.text,
                    m.time,
                    m.artifact?.name ?: "",
                    m.artifact?.meta ?: "",
                    m.createdAt.toString(),
                ).joinToString("\u001f")
            },
        )
    },
    restore = { saved ->
        saved.map { row ->
            val p = row.split("\u001f")
            ChatMessage(
                id = p[0].toLong(),
                role = ChatRole.valueOf(p[1]),
                text = p[2],
                time = p[3],
                artifact = if (p.size > 5 && p[4].isNotEmpty()) {
                    ChatArtifact(name = p[4], meta = p[5])
                } else {
                    null
                },
                createdAt = p.getOrNull(6)?.toLongOrNull() ?: 0L,
            )
        }
    },
)

/** New chats start empty. */
private fun initialThread(): List<ChatMessage> = emptyList()

/* ------------------------------ entry point ------------------------------ */

/**
 * Chat screen.
 *
 * The composer is live: typing and tapping Send appends a [ChatRole.User]
 * message and scrolls to it. Blank input is ignored, so Send never adds an
 * empty bubble.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ChatScreen(
    agentName: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    messagesOverride: List<ChatMessage>? = null,
    loading: Boolean = false,
    busy: Boolean = false,
    /** Optional live status (e.g. "Thinking…") shown beside the typing indicator. */
    activityLabel: String? = null,
    error: String? = null,
    systemNotice: String? = null,
    onSendMessage: ((String) -> Unit)? = null,
    /** Sends text plus one inline file to the engine. When null, Attach explains files aren't available here. */
    onSendAttachment: ((text: String, attachment: ChatAttachment) -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    onStopGeneration: (() -> Unit)? = null,
    sendWithEnter: Boolean = true,
    showTypingIndicator: Boolean = true,
    showActivityCards: Boolean = true,
    previewBeforeSending: Boolean = false,
    onSaveArtifact: ((title: String, content: String, chatId: String, messageId: Long, author: String) -> Unit)? = null,
    /** Saves a user message as a to-do. The caller reports the store result. Nothing is run here. */
    onSaveAction: ((messageId: Long, text: String) -> Unit)? = null,
    /** Prefills the composer once (e.g. from a workflow). Never auto-sent; a restored draft wins. */
    initialComposerText: String = "",
    /** Scrolls to this message when opening a memory source. Zero means no source. */
    focusMessageId: Long = 0L,
    /** False until the saved thread has been applied, so a missing source is not reported early. */
    threadRestored: Boolean = true,
    /** Opens Settings → Chat preferences from the overflow menu; hidden when null. */
    onOpenChatPreferences: (() -> Unit)? = null,
) {
    var localMessages by rememberSaveable(stateSaver = ChatThreadSaver) {
        mutableStateOf(initialThread())
    }
    val messages = messagesOverride ?: localMessages
    var input by rememberSaveable { mutableStateOf(initialComposerText) }
    val listState = rememberLazyListState()
    var didFocusSource by rememberSaveable { mutableStateOf(false) }

    var pendingPreview by rememberSaveable { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var attachment by remember { mutableStateOf<ChatAttachment?>(null) }
    var voiceRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var voiceFile by remember { mutableStateOf<java.io.File?>(null) }
    var recordingVoice by remember { mutableStateOf(false) }
    var attachSheet by rememberSaveable { mutableStateOf(false) }
    var actionMessageId by rememberSaveable { mutableStateOf<Long?>(null) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            when (val r = withContext(Dispatchers.IO) { readAttachment(context, uri) }) {
                is AttachResult.Ok -> attachment = r.file
                is AttachResult.Fail -> Toast.makeText(context, r.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
    val speech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val spoken = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (res.resultCode == Activity.RESULT_OK && !spoken.isNullOrBlank()) {
            input = if (input.isBlank()) spoken else input.trimEnd() + " " + spoken
        }
    }

    fun startVoiceRecording() {
        if (voiceRecorder != null) return
        val file = runCatching { java.io.File.createTempFile("orhan-voice-", ".m4a", context.cacheDir) }.getOrNull()
        if (file == null) {
            Toast.makeText(context, "Couldn't start voice recording", Toast.LENGTH_SHORT).show()
            return
        }
        val recorder = runCatching {
            val created = if (android.os.Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()
            created.setAudioSource(MediaRecorder.AudioSource.MIC)
            created.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            created.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            created.setAudioEncodingBitRate(64_000)
            created.setAudioSamplingRate(44_100)
            created.setOutputFile(file.absolutePath)
            created.prepare()
            created.start()
            created
        }.getOrElse {
            file.delete()
            Toast.makeText(context, "Couldn't start voice recording", Toast.LENGTH_SHORT).show()
            return
        }
        voiceFile = file
        voiceRecorder = recorder
        recordingVoice = true
    }

    fun stopVoiceRecording() {
        val recorder = voiceRecorder ?: return
        val file = voiceFile
        recordingVoice = false
        voiceRecorder = null
        voiceFile = null
        val valid = runCatching { recorder.stop(); true }.getOrDefault(false)
        runCatching { recorder.release() }
        if (!valid || file == null) {
            file?.delete()
            Toast.makeText(context, "Voice recording was too short or couldn't be saved", Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    require(file.length() in 1..ChatAttachment.MAX_BYTES)
                    val bytes = file.readBytes()
                    voiceAttachmentFromBytes(bytes)
                }.getOrNull()?.takeIf { it.sizeBytes > 0 }
            }
            file.delete()
            if (result == null) Toast.makeText(context, "Recording is empty or exceeds the 4 MB attachment limit", Toast.LENGTH_LONG).show()
            else attachment = result
        }
    }

    val voicePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startVoiceRecording()
        else Toast.makeText(context, "Microphone permission is needed to record audio", Toast.LENGTH_SHORT).show()
    }
    val latestVoiceRecorder by rememberUpdatedState(voiceRecorder)
    DisposableEffect(voiceRecorder, voiceFile) {
        val activeRecorder = voiceRecorder
        val activeFile = voiceFile
        onDispose {
            if (activeRecorder != null && latestVoiceRecorder === activeRecorder) {
                runCatching { activeRecorder.stop() }
                runCatching { activeRecorder.release() }
                activeFile?.delete()
            }
        }
    }

    fun onAttach() {
        if (onSendAttachment == null) {
            Toast.makeText(context, "Attaching files isn't available in this chat yet", Toast.LENGTH_SHORT).show()
        } else {
            attachSheet = true
        }
    }

    fun pickFiles(types: Array<String>) {
        attachSheet = false
        runCatching { filePicker.launch(types) }
            .onFailure { Toast.makeText(context, "No file picker found on this device", Toast.LENGTH_SHORT).show() }
    }

    fun onMic() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your message")
        try {
            speech.launch(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "Voice typing isn't available on this device", Toast.LENGTH_SHORT).show()
        }
    }

    fun deliver(text: String) {
        val file = attachment
        if (file != null && onSendAttachment != null) {
            onSendAttachment(text, file)
            attachment = null
        } else if (onSendMessage != null) {
            onSendMessage(text)
        } else {
            localMessages = localMessages + ChatMessage(
                id = (localMessages.lastOrNull()?.id ?: 0L) + 1L,
                role = ChatRole.User,
                text = text,
                time = "now",
            )
        }
        input = ""
    }

    fun send() {
        val text = input.trim()
        if (text.isEmpty() && attachment == null) return
        if (previewBeforeSending) pendingPreview = text else deliver(text)
    }

    pendingPreview?.let { draft ->
        AlertDialog(
            onDismissRequest = { pendingPreview = null },
            title = { Text("Send this message?") },
            text = {
                val file = attachment
                val label = file?.let { "\uD83D\uDCCE ${it.name} (${formatSize(it.sizeBytes)})" }
                Text(listOfNotNull(label, draft.takeIf { it.isNotBlank() }).joinToString("\n\n"))
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingPreview = null
                    deliver(draft)
                }) { Text("Send") }
            },
            dismissButton = {
                TextButton(onClick = { pendingPreview = null }) { Text("Cancel") }
            },
        )
    }

    // LazyColumn index 0 is the "Today" divider, so messages occupy
    // 1..messages.size and the newest bubble sits at index messages.size.
    // A memory source scrolls once to that message. Later sends still follow the end.
    LaunchedEffect(messages.size, threadRestored, focusMessageId) {
        if (focusMessageId != 0L && !didFocusSource) {
            if (!threadRestored) return@LaunchedEffect
            val index = messages.indexOfFirst { it.id == focusMessageId }
            didFocusSource = true
            if (index >= 0) {
                listState.scrollToItem(index + 1)
            } else {
                Toast.makeText(context, "That message is no longer in this chat.", Toast.LENGTH_LONG).show()
            }
            return@LaunchedEffect
        }
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        ChatTopBar(
            agentName = agentName,
            onBack = onBack,
            onShareChat = { shareTranscript(context, agentName, messages) },
            onOpenChatPreferences = onOpenChatPreferences,
        )
        if (systemNotice != null) {
            Text(
                text = systemNotice,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "day") {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Today",
                        style = OrhanTextStyles.meta,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items(items = messages, key = { it.id }) { message ->
                MessageBubble(
                    message = message,
                    agentName = agentName,
                    selected = message.id == actionMessageId ||
                        (actionMessageId == null && focusMessageId != 0L && message.id == focusMessageId),
                    sourceMessage = focusMessageId != 0L && message.id == focusMessageId,
                    showActivityCards = showActivityCards,
                    onLongPress = if (message.text.isNotBlank()) {
                        { actionMessageId = message.id }
                    } else {
                        null
                    },
                )
            }

            if (error != null) {
                item(key = "error") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = error,
                            style = OrhanTextStyles.meta,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (onRetry != null && !loading) {
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = onRetry) { Text("Try again") }
                        }
                    }
                }
            }

            if (showTypingIndicator && (loading || messagesOverride == null)) {
                item(key = "typing") {
                    TypingIndicator(agentName, activityLabel)
                }
            }
        }

        Composer(
            value = input,
            onValueChange = { input = it },
            onSend = { send() },
            onAttach = { onAttach() },
            onMic = { onMic() },
            recordingVoice = recordingVoice,
            onRecordStart = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startVoiceRecording()
                else voicePermission.launch(Manifest.permission.RECORD_AUDIO)
            },
            onRecordStop = ::stopVoiceRecording,
            attachment = attachment,
            onRemoveAttachment = { attachment = null },
            busy = busy || loading,
            onStop = { onStopGeneration?.invoke() },
            sendWithEnter = sendWithEnter,
        )
    }

    if (attachSheet) {
        AttachSheet(
            onDismiss = { attachSheet = false },
            onPhotos = { pickFiles(arrayOf("image/*")) },
            onFiles = { pickFiles(ChatAttachment.PICKER_TYPES) },
        )
    }
    val actionMessage = actionMessageId?.let { id -> messages.firstOrNull { it.id == id } }
    if (actionMessage != null && actionMessage.text.isNotBlank()) {
        MessageActionsSheet(
            canSave = actionMessage.role == ChatRole.Agent && actionMessage.id != 0L && onSaveArtifact != null,
            canSaveAction = actionMessage.role == ChatRole.User && onSaveAction != null && actionMessage.id != 0L,
            onDismiss = { actionMessageId = null },
            onCopy = {
                copyText(context, actionMessage.text)
                actionMessageId = null
            },
            onShare = {
                shareText(context, actionMessage.text)
                actionMessageId = null
            },
            onSave = {
                if (actionMessage.id != 0L) onSaveArtifact?.invoke(
                    agentName.ifBlank { actionMessage.text.take(42) },
                    actionMessage.text,
                    agentName,
                    actionMessage.id,
                    agentName.ifBlank { "Orhan" },
                )
                actionMessageId = null
            },
            onSaveAction = {
                onSaveAction?.invoke(actionMessage.id, actionMessage.text)
                actionMessageId = null
            },
        )
    }
}

/* -------------------------------- top bar -------------------------------- */

@Composable
private fun ChatTopBar(
    agentName: String,
    onBack: () -> Unit,
    onShareChat: (() -> Unit)? = null,
    onOpenChatPreferences: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .padding(start = 2.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }

        AgentMark(name = agentName, size = 40.dp)

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = agentName,
            style = OrhanTextStyles.rowTitle,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        var menuOpen by remember { mutableStateOf(false) }
        Box {
            IconButton(onClick = { menuOpen = !menuOpen }) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                if (onShareChat != null) {
                    DropdownMenuItem(
                        text = { Text("Share chat") },
                        onClick = { menuOpen = false; onShareChat() },
                    )
                }
                if (onOpenChatPreferences != null) {
                    DropdownMenuItem(
                        text = { Text("Chat preferences") },
                        onClick = { menuOpen = false; onOpenChatPreferences() },
                    )
                }
            }
        }
    }
}

/** Shares the visible transcript as plain text via the Android share sheet. */
private fun shareTranscript(context: Context, agentName: String, messages: List<ChatMessage>) {
    val lines = messages.filter { it.text.isNotBlank() }.map { m ->
        val who = if (m.role == ChatRole.User) "You" else agentName
        "$who: ${m.text.trim()}"
    }
    if (lines.isEmpty()) {
        Toast.makeText(context, "Nothing to share", Toast.LENGTH_SHORT).show()
        return
    }
    val body = "Orhan \u00b7 $agentName\n\n" + lines.joinToString("\n\n")
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Orhan \u00b7 $agentName")
        putExtra(Intent.EXTRA_TEXT, body)
    }
    val chooser = Intent.createChooser(send, "Share chat")
    if (context !is android.app.Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
}

/* ------------------------------- messages -------------------------------- */

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: ChatMessage,
    agentName: String,
    selected: Boolean,
    showActivityCards: Boolean = true,
    onLongPress: (() -> Unit)? = null,
    sourceMessage: Boolean = false,
) {
    val colors = OrhanTheme.colors
    val isUser = message.role == ChatRole.User
    val bubbleShape = RoundedCornerShape(20.dp)
    val fill = if (isUser || selected) colors.brandSoft else MaterialTheme.colorScheme.surface
    val borderColor = when {
        selected -> MaterialTheme.colorScheme.primary
        isUser -> Color.Transparent
        else -> colors.hairline
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (sourceMessage) {
                    Modifier.semantics { contentDescription = "Source message" }
                } else {
                    Modifier
                },
            ),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        if (!isUser) {
            AgentMark(name = agentName, size = 32.dp)
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 300.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        ) {
            if (!isUser) {
                Text(
                    text = agentName,
                    style = OrhanTextStyles.meta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            val bubbleModifier = Modifier
                .clip(bubbleShape)
                .background(fill)
                .border(width = if (isUser && !selected) 0.dp else 1.dp, color = borderColor, shape = bubbleShape)
                .then(
                    if (onLongPress != null) {
                        Modifier.combinedClickable(onClick = {}, onLongClick = onLongPress)
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = 14.dp, vertical = 11.dp)
            Box(modifier = bubbleModifier) {
                SelectionContainer {
                    Text(
                        text = message.text,
                        style = OrhanTextStyles.bubble,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            message.attachment?.let { payload ->
                Spacer(modifier = Modifier.height(6.dp))
                AttachmentMessagePreview(payload)
            }

            if (showActivityCards) message.artifact?.let { artifact ->
                Spacer(modifier = Modifier.height(6.dp))
                ArtifactCard(artifact = artifact)
            }

            if (message.time.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = message.time,
                    style = OrhanTextStyles.meta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AttachmentMessagePreview(attachment: ChatAttachment) {
    if (attachment.mimeType.startsWith("audio/")) {
        AudioAttachmentPlayer(attachment)
    } else if (attachment.mimeType.startsWith("image/")) {
        val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, attachment.base64) {
            value = withContext(Dispatchers.Default) {
                runCatching {
                    val bytes = Base64.decode(attachment.base64, Base64.DEFAULT)
                    val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                    if (bounds.outWidth !in 1..12000 || bounds.outHeight !in 1..12000) return@runCatching null
                    val sample = maxOf(1, maxOf(bounds.outWidth, bounds.outHeight) / 512)
                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, android.graphics.BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
                }.getOrNull()
            }
        }
        if (bitmap != null) Image(
            bitmap!!,
            contentDescription = attachment.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier.widthIn(max = 260.dp).heightIn(max = 260.dp).clip(RoundedCornerShape(12.dp)),
        ) else Text("Image unavailable · ${attachment.name}", style = OrhanTextStyles.meta, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        Text("${attachment.mimeType} · ${attachment.name}", style = OrhanTextStyles.meta, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AudioAttachmentPlayer(attachment: ChatAttachment) {
    val context = LocalContext.current
    val loaded by produceState<Pair<java.io.File?, Boolean>>(null to false, attachment.base64, attachment.name) {
        val file = withContext(Dispatchers.IO) {
            runCatching {
                val suffix = attachment.name.substringAfterLast('.', "audio").filter { it.isLetterOrDigit() }.take(8).ifBlank { "audio" }
                java.io.File.createTempFile("orhan-audio-", ".$suffix", context.cacheDir).also { file ->
                    file.writeBytes(Base64.decode(attachment.base64, Base64.DEFAULT))
                }
            }.getOrNull()
        }
        value = file to true
    }
    val audioFile = loaded.first
    val fileReady = loaded.second
    val player = remember(audioFile) { mutableStateOf<MediaPlayer?>(null) }
    val playing = remember(audioFile) { mutableStateOf(false) }
    val loading = remember(audioFile) { mutableStateOf(false) }
    val error = remember(audioFile) { mutableStateOf(false) }
    DisposableEffect(audioFile) {
        onDispose {
            player.value?.release()
            player.value = null
            audioFile?.delete()
        }
    }
    androidx.compose.material3.Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.material3.TextButton(
            enabled = fileReady && audioFile != null && !loading.value,
            onClick = {
                if (playing.value) {
                    player.value?.pause()
                    playing.value = false
                } else {
                    val existing = player.value
                    if (existing != null) {
                        runCatching { existing.start(); playing.value = true }.onFailure { error.value = true }
                    } else {
                        val file = audioFile ?: return@TextButton
                        loading.value = true
                        runCatching {
                            MediaPlayer().also { mp ->
                                player.value = mp
                                mp.setDataSource(file.absolutePath)
                                mp.setOnPreparedListener { ready ->
                                    loading.value = false
                                    ready.start()
                                    playing.value = true
                                }
                                mp.setOnCompletionListener { playing.value = false; player.value?.release(); player.value = null }
                                mp.setOnErrorListener { failed, _, _ ->
                                    loading.value = false
                                    playing.value = false
                                    error.value = true
                                    failed.release()
                                    player.value = null
                                    true
                                }
                                mp.prepareAsync()
                            }
                        }.onFailure { loading.value = false; error.value = true; player.value?.release(); player.value = null }
                    }
                }
            },
        ) { Text(when { !fileReady -> "Preparing audio…"; loading.value -> "Loading audio…"; playing.value -> "Pause audio"; else -> "Play audio" }) }
        Text(attachment.name, style = OrhanTextStyles.meta, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (error.value || (fileReady && audioFile == null)) {
        Text("Audio unavailable", style = OrhanTextStyles.meta, color = MaterialTheme.colorScheme.error)
    }
}

private fun copyText(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    cm.setPrimaryClip(ClipData.newPlainText("Orhan message", text))
    // Android 13+ shows its own clipboard confirmation.
    if (android.os.Build.VERSION.SDK_INT < 33) Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
}

private fun shareText(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, "Share from Orhan").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/** Inline preview of what the agent produced \u2014 14dp radius, 42x54 thumb. */
@Composable
private fun ArtifactCard(artifact: ChatArtifact) {
    val colors = OrhanTheme.colors
    val shapes = OrhanTheme.shapes

    Row(
        modifier = Modifier
            .widthIn(max = 280.dp)
            .clip(shapes.artifactCard)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, colors.hairline, shapes.artifactCard)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(width = 42.dp, height = 54.dp)
                .clip(shapes.preview)
                .background(colors.primaryGradient),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                // Three skeleton rules stand in for the page thumbnail.
                SkeletonRule(widthFraction = 0.9f)
                SkeletonRule(widthFraction = 0.65f)
                Spacer(modifier = Modifier.height(2.dp))
                SkeletonRule(widthFraction = 0.75f)
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artifact.name,
                style = OrhanTextStyles.rowTitle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = artifact.meta,
                style = OrhanTextStyles.meta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SkeletonRule(widthFraction: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(4.dp)
            .clip(CircleShape)
            .background(OrhanTheme.colors.onGradient.copy(alpha = 0.55f)),
    )
}

/* ---------------------------- typing indicator --------------------------- */

/** Three mint dots in an agent bubble \u2014 the agent is still writing. */
@Composable
private fun TypingIndicator(agentName: String, activityLabel: String? = null) {
    val colors = OrhanTheme.colors
    val bubble = RoundedCornerShape(20.dp)

    Row(verticalAlignment = Alignment.Top) {
        AgentMark(name = agentName, size = 32.dp)
        Spacer(modifier = Modifier.width(8.dp))
        Row(
            modifier = Modifier
                .clip(bubble)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, colors.hairline, bubble)
                .padding(horizontal = 14.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Dot(alpha = 1f)
            Dot(alpha = 0.62f)
            Dot(alpha = 0.34f)
            if (!activityLabel.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = activityLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun Dot(alpha: Float) {
    Box(
        modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(OrhanTheme.colors.logoMint.copy(alpha = alpha)),
    )
}

/* -------------------------------- composer ------------------------------- */

/**
 * One rounded composer. Plus opens Photos or Files. Tap the empty round
 * control for voice typing; press and hold to record a reviewable audio
 * attachment. Stop replaces it while a reply is in progress.
 */
@Composable
private fun Composer(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit = {},
    onMic: () -> Unit = {},
    recordingVoice: Boolean = false,
    onRecordStart: () -> Unit = {},
    onRecordStop: () -> Unit = {},
    attachment: ChatAttachment? = null,
    onRemoveAttachment: () -> Unit = {},
    busy: Boolean = false,
    onStop: () -> Unit = {},
    sendWithEnter: Boolean = true,
) {
    val colors = OrhanTheme.colors
    val cardShape = RoundedCornerShape(24.dp)
    val canSend = value.isNotBlank() || attachment != null
    val currentRecording by rememberUpdatedState(recordingVoice)
    val latestOnMic by rememberUpdatedState(onMic)
    val latestOnRecordStart by rememberUpdatedState(onRecordStart)
    val latestOnRecordStop by rememberUpdatedState(onRecordStop)
    val latestOnSend by rememberUpdatedState(onSend)
    val latestOnStop by rememberUpdatedState(onStop)

    // IME when the keyboard is open, otherwise the navigation bar. Applied once.
    Column(
        modifier = Modifier
            .windowInsetsPadding(
                WindowInsets.ime
                    .union(WindowInsets.navigationBars)
                    .only(WindowInsetsSides.Bottom),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        if (recordingVoice) {
            Text("Recording voice message · release to attach", style = OrhanTextStyles.meta, color = MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, colors.fieldEdge, cardShape),
        ) {
            if (attachment != null) {
                Row(
                    modifier = Modifier
                        .padding(start = 12.dp, end = 4.dp, top = 8.dp)
                        .clip(CircleShape)
                        .background(colors.brandSoft)
                        .padding(start = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AttachFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${attachment.name} \u00B7 ${formatSize(attachment.sizeBytes)}",
                        style = OrhanTextStyles.meta,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 220.dp),
                    )
                    IconButton(onClick = onRemoveAttachment, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Remove attachment",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp)
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = "Type a message...",
                        style = OrhanTextStyles.bubble,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = OrhanTextStyles.bubble.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(
                        imeAction = if (sendWithEnter) ImeAction.Send else ImeAction.Default,
                    ),
                    keyboardActions = if (sendWithEnter) {
                        KeyboardActions(onSend = { if (!busy) onSend() })
                    } else {
                        KeyboardActions.Default
                    },
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 2.dp, end = 6.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onAttach) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Attach",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                val actionDescription = when {
                    busy -> "Stop generating"
                    canSend -> "Send message"
                    else -> "Voice typing"
                }
                val actionClick = when {
                    busy -> onStop
                    canSend -> onSend
                    else -> onMic
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (recordingVoice) MaterialTheme.colorScheme.error else colors.primaryGradient)
                        .then(if (!busy && !canSend) Modifier.pointerInput(Unit) {
                            var longPressed = false
                            detectTapGestures(
                                onPress = {
                                    longPressed = false
                                    tryAwaitRelease()
                                    if (longPressed) latestOnRecordStop()
                                },
                                onLongPress = {
                                    longPressed = true
                                    if (!currentRecording) latestOnRecordStart()
                                },
                                onTap = { if (currentRecording) latestOnRecordStop() else latestOnMic() },
                            )
                        } else Modifier.clickable(onClick = { if (busy) latestOnStop() else latestOnSend() }),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = when {
                            busy -> Icons.Rounded.Stop
                            canSend -> Icons.AutoMirrored.Rounded.Send
                            recordingVoice -> Icons.Rounded.Stop
                            else -> Icons.Rounded.Mic
                        },
                        contentDescription = if (recordingVoice) "Release to attach voice recording" else if (canSend || busy) actionDescription else "Voice typing; hold to record audio",
                        tint = colors.onGradient,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/* ------------------------------ attachments ------------------------------ */

private sealed interface AttachResult {
    data class Ok(val file: ChatAttachment) : AttachResult
    data class Fail(val message: String) : AttachResult
}

/** Reads a picked document into a base64 inline part, enforcing the engine's type and 4 MB limits. */
private fun readAttachment(context: Context, uri: Uri): AttachResult {
    val resolver = context.contentResolver
    val mime = resolver.getType(uri) ?: "application/octet-stream"
    if (!ChatAttachment.isSupported(mime)) return AttachResult.Fail("Only images, PDFs, text, audio or video files can be attached")
    var name = "file"
    var size = -1L
    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
        if (c.moveToFirst()) {
            c.getString(0)?.let { name = it }
            if (!c.isNull(1)) size = c.getLong(1)
        }
    }
    if (size > ChatAttachment.MAX_BYTES) return AttachResult.Fail("File is too large (max 4 MB)")
    val bytes = runCatching {
        resolver.openInputStream(uri)?.use { input ->
            val out = java.io.ByteArrayOutputStream()
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                out.write(buf, 0, n)
                if (out.size() > ChatAttachment.MAX_BYTES) return AttachResult.Fail("File is too large (max 4 MB)")
            }
            out.toByteArray()
        }
    }.getOrNull() ?: return AttachResult.Fail("Couldn't read that file")
    if (bytes.isEmpty()) return AttachResult.Fail("That file is empty")
    return AttachResult.Ok(ChatAttachment(name, mime, Base64.encodeToString(bytes, Base64.NO_WRAP), bytes.size.toLong()))
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1024 -> "${bytes / 1024} KB"
    else -> "$bytes B"
}

@Composable
private fun AgentMark(name: String, size: androidx.compose.ui.unit.Dp) {
    val colors = OrhanTheme.colors
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.brandSoft),
        contentAlignment = Alignment.Center,
    ) {
        if (name.equals("Orhan", ignoreCase = true)) {
            Image(
                painter = painterResource(R.drawable.orhan_mark),
                contentDescription = null,
                modifier = Modifier.size(size * 0.7f),
                contentScale = ContentScale.Fit,
            )
        } else {
            Text(
                text = initialsOf(name).ifBlank { "?" },
                style = OrhanTextStyles.meta,
                color = colors.onBrandSoft,
                maxLines = 1,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttachSheet(
    onDismiss: () -> Unit,
    onPhotos: () -> Unit,
    onFiles: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = OrhanTheme.shapes.sheet,
    ) {
        Column(modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 28.dp)) {
            Text(
                text = "Attach",
                style = OrhanTextStyles.rowTitle,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            SheetAction(Icons.Rounded.Image, "Photos", "Images on this device", onPhotos)
            SheetAction(
                Icons.Rounded.Description,
                "Files",
                "Images, PDFs, text, audio or video",
                onFiles,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageActionsSheet(
    canSave: Boolean,
    canSaveAction: Boolean,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onSaveAction: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = OrhanTheme.shapes.sheet,
    ) {
        Column(modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 28.dp)) {
            Text(
                text = "Message",
                style = OrhanTextStyles.rowTitle,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            SheetAction(Icons.Rounded.ContentCopy, "Copy", null, onCopy)
            SheetAction(Icons.Rounded.Share, "Share", null, onShare)
            if (canSave) {
                SheetAction(Icons.Rounded.Description, "Save as artifact", null, onSave)
            }
            if (canSaveAction) {
                SheetAction(Icons.Rounded.Add, "Save as action", "Saves the to-do. Nothing is run.", onSaveAction)
            }
        }
    }
}

@Composable
private fun SheetAction(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(title, style = OrhanTextStyles.rowTitle, color = MaterialTheme.colorScheme.onSurface)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = OrhanTextStyles.meta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/* -------------------------------- previews ------------------------------- */

@Preview(name = "Chat \u2014 light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ChatLightPreview() {
    OrhanTheme(darkTheme = false) {
        ChatScreen(agentName = "Orhan")
    }
}

@Preview(name = "Chat \u2014 dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ChatDarkPreview() {
    OrhanTheme(darkTheme = true) {
        ChatScreen(agentName = "Orhan")
    }
}

@Preview(name = "Chat \u2014 360dp", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun ChatNarrowPreview() {
    OrhanTheme(darkTheme = true) {
        ChatScreen(agentName = "Orhan")
    }
}
