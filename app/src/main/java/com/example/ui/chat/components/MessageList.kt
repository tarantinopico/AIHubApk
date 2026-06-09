package com.example.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.domain.model.Message
import com.example.domain.model.MessageRole
import com.example.domain.model.MessageStatus
import com.example.ui.components.TypingIndicator
import com.example.ui.components.gradientBackground
import com.example.ui.theme.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun EmptyChatState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .gradientBackground(LocalAppShapes.current.extraLarge)
        )
        Spacer(modifier = Modifier.height(LocalAppSpacing.current.huge))
        Text(
            text = "Ahoj, do čeho se dnes pustíme?",
            style = AppTypography.titleLarge,
            color = TextPrimary
        )
    }
}

@Composable
fun MessageList(
    messages: List<Message>,
    isGenerating: Boolean,
    onRegenerate: (String) -> Unit
) {
    val listState = rememberLazyListState()

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
        items(messages) { msg ->
            MessageBubble(msg = msg, onRegenerate = { onRegenerate(msg.id) })
        }
    }
}

@Composable
fun MessageBubble(msg: Message, onRegenerate: () -> Unit) {
    val isUser = msg.role == MessageRole.USER
    val clipboardManager = LocalClipboardManager.current
    
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

    Column(
        modifier = Modifier.fillMaxWidth(),
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
                .widthIn(max = 300.dp)
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
                    Text(
                        text = msg.content,
                        style = AppTypography.bodyLarge,
                        color = if (isUser) Color.White else TextPrimary
                    )
                }
            }
        }
        
        if (!isUser && msg.status != MessageStatus.SENDING && msg.content.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = { clipboardManager.setText(AnnotatedString(msg.content)) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Kopírovat", tint = TextMuted, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onRegenerate, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Regenerovat", tint = TextMuted, modifier = Modifier.size(16.dp))
                }
            }
        }
        if (msg.status == MessageStatus.ERROR) {
            Text("Chyba při odesílání/příjmu", color = Error, style = AppTypography.labelMedium, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
