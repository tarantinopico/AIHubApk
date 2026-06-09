package com.example.ui.chat.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.domain.model.Message
import com.example.domain.model.MessageRole
import com.example.domain.model.MessageStatus
import com.example.ui.components.TypingIndicator
import com.example.ui.components.gradientBackground
import com.example.ui.components.MarkdownRenderer
import com.example.ui.components.TtsManager
import com.example.ui.theme.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun EmptyChatState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .gradientBackground(RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Ahoj Tome, do čeho se dnes pustíme?",
            style = AppTypography.titleLarge,
            color = TextPrimary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Vyber si model ze seznamu a napiš první zprávu.",
            style = AppTypography.bodyMedium,
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun MessageList(
    messages: List<Message>,
    isGenerating: Boolean,
    onRegenerate: (String) -> Unit,
    onEdit: (Message) -> Unit
) {
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val ttsManager = remember { TtsManager(context) }

    DisposableEffect(Unit) {
        onDispose { ttsManager.shutdown() }
    }

    LaunchedEffect(messages.size, messages.lastOrNull()?.content?.length) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(LocalAppSpacing.current.large),
        verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.large)
    ) {
        items(messages, key = { it.id }) { msg ->
            MessageBubble(
                msg = msg, 
                onRegenerate = { onRegenerate(msg.id) },
                onEdit = { onEdit(msg) },
                onSpeak = { ttsManager.speak(msg.content) },
                modifier = Modifier.animateItem(
                    fadeInSpec = tween(150),
                    fadeOutSpec = tween(150),
                    placementSpec = tween(150)
                )
            )
        }
    }
}

@Composable
fun MessageBubble(
    msg: Message,
    modifier: Modifier = Modifier,
    onRegenerate: () -> Unit,
    onEdit: () -> Unit,
    onSpeak: () -> Unit
) {
    val isUser = msg.role == MessageRole.USER
    val clipboardManager = LocalClipboardManager.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    
    val imageUris = try {
        if (!msg.attachmentsJson.isNullOrBlank()) {
            val arr = Json.parseToJsonElement(msg.attachmentsJson!!).takeIf { it is JsonArray } as? JsonArray
            arr?.mapNotNull { 
                val obj = it.jsonObject
                val mime = obj["mimeType"]?.jsonPrimitive?.content ?: ""
                val data = obj["data"]?.jsonPrimitive?.content ?: ""
                if (mime.startsWith("image/") && data.isNotEmpty()) {
                    "data:$mime;base64,$data"
                } else null
            } ?: emptyList()
        } else emptyList()
    } catch (e: Exception) {
        emptyList()
    }

    val estimatedTokens = msg.tokensUsed ?: (msg.content.length / 4)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (isUser) 20.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 20.dp
                    )
                )
                .then(
                    if (isUser) Modifier.gradientBackground(RoundedCornerShape(0.dp))
                    else Modifier.background(SurfaceElevated)
                )
                .widthIn(max = 320.dp)
                .padding(LocalAppSpacing.current.large)
        ) {
            Column {
                if (imageUris.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                        items(imageUris) { uri ->
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp))
                            )
                        }
                    }
                }

                if (msg.status == MessageStatus.SENDING && msg.content.isEmpty()) {
                    TypingIndicator()
                } else {
                    if (!isUser) {
                        MarkdownRenderer(
                            text = msg.content,
                            textColor = TextPrimary,
                            isUser = false
                        )
                    } else {
                        Text(
                            text = msg.content,
                            style = AppTypography.bodyLarge,
                            color = Color.White
                        )
                    }
                }
            }
        }
        
        Row(
            modifier = Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isUser && msg.status != MessageStatus.SENDING && msg.content.isNotEmpty()) {
                IconButton(onClick = { 
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    clipboardManager.setText(AnnotatedString(msg.content)) 
                }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Kopírovat", tint = TextMuted, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = { 
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    onRegenerate() 
                }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Regenerovat", tint = TextMuted, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = { 
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    onSpeak() 
                }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Přečíst nahlas", tint = TextMuted, modifier = Modifier.size(16.dp))
                }
                Text("~ $estimatedTokens tokenů", style = AppTypography.labelSmall, color = TextMuted)
            } else if (isUser && msg.status != MessageStatus.SENDING) {
                IconButton(onClick = { 
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    onEdit() 
                }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Upravit", tint = TextMuted, modifier = Modifier.size(16.dp))
                }
                Text("~ $estimatedTokens tokenů", style = AppTypography.labelSmall, color = TextMuted)
            }
        }
        if (msg.status == MessageStatus.ERROR) {
            Text("Chyba při odesílání/příjmu", color = Error, style = AppTypography.labelMedium, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
