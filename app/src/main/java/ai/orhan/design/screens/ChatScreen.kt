package ai.orhan.design.screens

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.app.Activity
import android.content.ActivityNotFoundException
import android.net.Uri
import android.provider.OpenableColumns
import android.speech.RecognizerIntent
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ai.orhan.design.theme.OrhanTextStyles
import ai.orhan.design.theme.OrhanTheme

/* ============================================================================
 *  Orhan AI \u2014 Chat screen (Material 3, 390dp base)
 *
 *  Same token set as HomeScreen.kt. The two bubble shapes come straight from
 *  Shape.kt:
 *      shapes.agentBubble  = 20 / 20 / 20 / 6  (tail bottom-start)
 *      shapes.userBubble   = 20 / 20 / 6 / 20  (tail bottom-end)
 *
 *  State lives in [ChatScreen] behind rememberSaveable, so the thread, the
 *  draft text and the scroll position survive rotation and process death.
 *  [ChatMessage] is not Parcelable, so [ChatThreadSaver] serialises the list.
 *
 *  Icons used: ArrowBack, Send, AttachFile, Mic, MoreVert and Description are
 *  all part of the core icon set — no extra dependency needed here.
 * ==========================================================================*/

/** Author of a message. */
enum class ChatRole { Agent, User }

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
fun ChatScreen(
    agentName: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    messagesOverride: List<ChatMessage>? = null,
    loading: Boolean = false,
    busy: Boolean = false,
    error: String? = null,
    onSendMessage: ((String) -> Unit)? = null,
    /** Sends text plus one inline file to the engine. When null, Attach explains files aren't available here. */
    onSendAttachment: ((text: String, attachment: ChatAttachment) -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    onStopGeneration: (() -> Unit)? = null,
    sendWithEnter: Boolean = true,
    showTypingIndicator: Boolean = true,
    showActivityCards: Boolean = true,
    previewBeforeSending: Boolean = false,
    onSaveArtifact: ((title: String, content: String) -> Unit)? = null,
    /** Prefills the composer once (e.g. from a workflow). Never auto-sent; a restored draft wins. */
    initialComposerText: String = "",
    /** Opens Settings → Chat preferences from the overflow menu; hidden when null. */
    onOpenChatPreferences: (() -> Unit)? = null,
) {
    val colors = OrhanTheme.colors
    val shapes = OrhanTheme.shapes

    var localMessages by rememberSaveable(stateSaver = ChatThreadSaver) {
        mutableStateOf(initialThread())
    }
    val messages = messagesOverride ?: localMessages
    var input by rememberSaveable { mutableStateOf(initialComposerText) }
    val listState = rememberLazyListState()

    var pendingPreview by rememberSaveable { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var attachment by remember { mutableStateOf<ChatAttachment?>(null) }

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

    fun onAttach() {
        if (onSendAttachment == null) {
            Toast.makeText(context, "Attaching files isn't available in this chat yet", Toast.LENGTH_SHORT).show()
        } else {
            runCatching { filePicker.launch(ChatAttachment.PICKER_TYPES) }
                .onFailure { Toast.makeText(context, "No file picker found on this device", Toast.LENGTH_SHORT).show() }
        }
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
        if (previewBeforeSending && attachment == null) pendingPreview = text else deliver(text)
    }

    pendingPreview?.let { draft ->
        AlertDialog(
            onDismissRequest = { pendingPreview = null },
            title = { Text("Send this message?") },
            text = { Text(draft) },
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
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .imePadding(),
    ) {
        val context = LocalContext.current
        ChatTopBar(
            agentName = agentName,
            onBack = onBack,
            onShareChat = { shareTranscript(context, agentName, messages) },
            onOpenChatPreferences = onOpenChatPreferences,
        )

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
                        style = OrhanTextStyles.kicker,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(shapes.pill)
                            .background(colors.field)
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
            }

            items(items = messages, key = { it.id }) { message ->
                MessageBubble(
                    message = message,
                    showActivityCards = showActivityCards,
                    onSaveArtifact = onSaveArtifact?.let { save ->
                        { text: String ->
                            save(agentName.ifBlank { text.take(42) }, text)
                        }
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
                    TypingIndicator()
                }
            }
        }

        Composer(
            value = input,
            onValueChange = { input = it },
            onSend = { send() },
            onAttach = { onAttach() },
            onMic = { onMic() },
            attachment = attachment,
            onRemoveAttachment = { attachment = null },
            busy = busy || loading,
            onStop = { onStopGeneration?.invoke() },
            agentName = agentName,
            sendWithEnter = sendWithEnter,
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
    val colors = OrhanTheme.colors
    val shapes = OrhanTheme.shapes

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

        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(shapes.avatarHeader)
                .background(colors.brandSoft),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initialsOf(agentName),
                style = OrhanTextStyles.rowTitle,
                color = colors.onBrandSoft,
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = agentName,
                style = OrhanTextStyles.rowTitle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(colors.logoMint),
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Agent \u00b7 always on",
                    style = OrhanTextStyles.meta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

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

@Composable
private fun MessageBubble(
    message: ChatMessage,
    showActivityCards: Boolean = true,
    onSaveArtifact: ((String) -> Unit)? = null,
) {
    val colors = OrhanTheme.colors
    val shapes = OrhanTheme.shapes
    val isUser = message.role == ChatRole.User
    val bubbleShape = if (isUser) shapes.userBubble else shapes.agentBubble

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(shapes.avatarMessage)
                    .background(colors.brandSoft),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "T",
                    style = OrhanTextStyles.meta,
                    color = colors.onBrandSoft,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(bubbleShape)
                    .background(if (isUser) colors.userBubble else colors.agentBubble)
                    .border(
                        width = if (isUser) 0.dp else 1.dp,
                        color = if (isUser) Color.Transparent else colors.hairline,
                        shape = bubbleShape,
                    )
                    .padding(horizontal = 14.dp, vertical = 11.dp),
            ) {
                SelectionContainer {
                    Text(
                        text = message.text,
                        style = OrhanTextStyles.bubble,
                        color = if (isUser) colors.userBubbleInk else colors.agentBubbleInk,
                    )
                }
            }

            if (showActivityCards) message.artifact?.let { artifact ->
                Spacer(modifier = Modifier.height(6.dp))
                ArtifactCard(artifact = artifact)
            }

            Spacer(modifier = Modifier.height(3.dp))
            if (!isUser && message.text.isNotBlank()) {
                val context = LocalContext.current
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = message.time,
                        style = OrhanTextStyles.meta,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    MessageAction(Icons.Rounded.ContentCopy, "Copy message") { copyText(context, message.text) }
                    MessageAction(Icons.Rounded.Share, "Share message") { shareText(context, message.text) }
                    if (onSaveArtifact != null) {
                        MessageAction(Icons.Rounded.Description, "Save as artifact") {
                            onSaveArtifact(message.text)
                            Toast.makeText(context, "Saved to Artifacts", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
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
private fun MessageAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
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
            .clickable { }
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
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Open preview",
                    style = OrhanTextStyles.meta,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
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
private fun TypingIndicator() {
    val colors = OrhanTheme.colors
    val shapes = OrhanTheme.shapes

    Row(verticalAlignment = Alignment.Bottom) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(shapes.avatarMessage)
                .background(colors.brandSoft),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "T",
                style = OrhanTextStyles.meta,
                color = colors.onBrandSoft,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Row(
            modifier = Modifier
                .clip(shapes.agentBubble)
                .background(colors.agentBubble)
                .border(1.dp, colors.hairline, shapes.agentBubble)
                .padding(horizontal = 14.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Dot(alpha = 1f)
            Dot(alpha = 0.62f)
            Dot(alpha = 0.34f)
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
 * Composer bar: 1dp top hairline, attach + field + mic, and a gradient Send
 * button that only appears once there is text \u2014 the mockup's behaviour.
 * Every control is at least 48dp tall at any font scale.
 */
@Composable
private fun Composer(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    agentName: String,
    onAttach: () -> Unit = {},
    onMic: () -> Unit = {},
    attachment: ChatAttachment? = null,
    onRemoveAttachment: () -> Unit = {},
    busy: Boolean = false,
    onStop: () -> Unit = {},
    sendWithEnter: Boolean = true,
) {
    val colors = OrhanTheme.colors
    val shapes = OrhanTheme.shapes
    val canSend = value.isNotBlank() || attachment != null

    Surface(color = colors.navBackground) {
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.hairline),
            )

            if (attachment != null) {
                Row(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 8.dp, top = 8.dp)
                        .clip(shapes.pill)
                        .background(colors.field)
                        .border(1.dp, colors.fieldEdge, shapes.pill)
                        .padding(start = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AttachFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${attachment.name} \u00B7 ${formatSize(attachment.sizeBytes)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 240.dp),
                    )
                    IconButton(onClick = onRemoveAttachment) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Remove attachment",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                IconButton(onClick = onAttach) {
                    Icon(
                        imageVector = Icons.Rounded.AttachFile,
                        contentDescription = "Attach file",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(shapes.composer)
                        .background(colors.field)
                        .border(1.dp, colors.fieldEdge, shapes.composer)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            Text(
                                text = "Message $agentName\u2026",
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
                }

                Spacer(modifier = Modifier.width(6.dp))

                if (busy) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(shapes.pill)
                            .background(colors.primaryGradient)
                            .clickable(onClick = onStop),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Stop,
                            contentDescription = "Stop generating",
                            tint = colors.onGradient,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                } else if (canSend) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(shapes.pill)
                            .background(colors.primaryGradient)
                            .clickable(onClick = onSend),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Send message",
                            tint = colors.onGradient,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                } else {
                    IconButton(onClick = onMic) {
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = "Voice typing",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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
