package ai.orhan.design.screens

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
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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

/** Thread as it appears in the shipped mockup. */
private fun initialThread(): List<ChatMessage> = listOf(
    ChatMessage(
        id = 1L,
        role = ChatRole.User,
        text = "Draft a landing page for our AI meetup in Palo Alto \u2014 dark hero, teal accent, one CTA.",
        time = "9:38 AM",
    ),
    ChatMessage(
        id = 2L,
        role = ChatRole.Agent,
        text = "On it. I\u2019ll use the Orhan green (#087c45) so it stays on-brand, keep one primary CTA, and make it mobile-first.",
        time = "9:38 AM",
        artifact = ChatArtifact(name = "meetup-landing \u00b7 v1", meta = "Responsive \u00b7 12 sections \u00b7 Lato"),
    ),
    ChatMessage(
        id = 3L,
        role = ChatRole.User,
        text = "Looks right. Make the hero headline bigger and add the speaker line-up.",
        time = "9:40 AM",
    ),
    ChatMessage(
        id = 4L,
        role = ChatRole.Agent,
        text = "Done \u2014 headline is now 56/60 with the mint accent, and the line-up is a 3-up card row that stacks on small screens. Draft is ready to publish.",
        time = "9:41 AM",
    ),
)

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
    error: String? = null,
    onSendMessage: ((String) -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    sendWithEnter: Boolean = true,
    showTypingIndicator: Boolean = true,
    showActivityCards: Boolean = true,
    previewBeforeSending: Boolean = false,
) {
    val colors = OrhanTheme.colors
    val shapes = OrhanTheme.shapes

    var localMessages by rememberSaveable(stateSaver = ChatThreadSaver) {
        mutableStateOf(initialThread())
    }
    val messages = messagesOverride ?: localMessages
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    var pendingPreview by rememberSaveable { mutableStateOf<String?>(null) }

    fun deliver(text: String) {
        if (onSendMessage != null) {
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
        if (text.isEmpty()) return
        if (previewBeforeSending) pendingPreview = text else deliver(text)
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
        ChatTopBar(agentName = agentName, onBack = onBack)

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
                MessageBubble(message = message, showActivityCards = showActivityCards)
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
            agentName = agentName,
            sendWithEnter = sendWithEnter,
        )
    }
}

/* -------------------------------- top bar -------------------------------- */

@Composable
private fun ChatTopBar(agentName: String, onBack: () -> Unit) {
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

        IconButton(onClick = { }) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "More options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/* ------------------------------- messages -------------------------------- */

@Composable
private fun MessageBubble(message: ChatMessage, showActivityCards: Boolean = true) {
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
                Text(
                    text = message.text,
                    style = OrhanTextStyles.bubble,
                    color = if (isUser) colors.userBubbleInk else colors.agentBubbleInk,
                )
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
    sendWithEnter: Boolean = true,
) {
    val colors = OrhanTheme.colors
    val shapes = OrhanTheme.shapes
    val canSend = value.isNotBlank()

    Surface(color = colors.navBackground) {
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.hairline),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                IconButton(onClick = { }) {
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
                                KeyboardActions(onSend = { onSend() })
                            } else {
                                KeyboardActions.Default
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                if (canSend) {
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
                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = "Hold to record voice",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/* -------------------------------- previews ------------------------------- */

@Preview(name = "Chat \u2014 light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ChatLightPreview() {
    OrhanTheme(darkTheme = false) {
        ChatScreen(agentName = "Webpage Developer")
    }
}

@Preview(name = "Chat \u2014 dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ChatDarkPreview() {
    OrhanTheme(darkTheme = true) {
        ChatScreen(agentName = "Webpage Developer")
    }
}

@Preview(name = "Chat \u2014 360dp", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun ChatNarrowPreview() {
    OrhanTheme(darkTheme = true) {
        ChatScreen(agentName = "Webpage Developer")
    }
}
