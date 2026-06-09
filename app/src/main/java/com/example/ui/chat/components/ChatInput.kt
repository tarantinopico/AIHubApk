package com.example.ui.chat.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.chat.AttachmentInfo
import com.example.ui.components.AppTextField
import com.example.ui.components.gradientBackground
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Base64

@Composable
fun ChatInputBar(
    inputText: String,
    attachments: List<AttachmentInfo>,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit,
    onAddAttachment: (uri: String, mimeType: String, base64: String) -> Unit,
    onRemoveAttachment: (Int) -> Unit,
    isGenerating: Boolean,
    supportsImages: Boolean
) {
    val context = LocalContext.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch(Dispatchers.IO) {
                for (uri in uris) {
                    val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (bytes != null) {
                        val base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)
                        onAddAttachment(uri.toString(), mimeType, base64Data)
                    }
                }
            }
        }
    }

    Surface(
        color = Background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            if (attachments.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = LocalAppSpacing.current.large, vertical = LocalAppSpacing.current.small),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(attachments) { index, attachment ->
                        Box(modifier = Modifier.size(64.dp)) {
                            AsyncImage(
                                model = attachment.uriString,
                                contentDescription = "Příloha",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(LocalAppShapes.current.medium)
                                    .background(SurfaceElevated)
                            )
                            IconButton(
                                onClick = { onRemoveAttachment(index) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(20.dp)
                                    .padding(2.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Odebrat", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
            Row(
                modifier = Modifier
                    .padding(horizontal = LocalAppSpacing.current.large, vertical = LocalAppSpacing.current.medium)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.small)
            ) {
                if (supportsImages) {
                    IconButton(onClick = { photoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        Icon(Icons.Default.Attachment, contentDescription = "Příloha", tint = TextSecondary)
                    }
                }
                
                AppTextField(
                    value = inputText,
                    onValueChange = onInputChanged,
                    placeholder = "Zeptejte se na cokoliv...",
                    singleLine = false,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(max = 120.dp)
                )
                
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .then(if (isGenerating) Modifier.background(SurfaceElevated) else Modifier.gradientBackground(CircleShape))
                        .clickable(onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            if (isGenerating) onCancel() else onSend()
                        }),
                    contentAlignment = Alignment.Center
                ) {
                    if (isGenerating) {
                        Icon(Icons.Default.Stop, contentDescription = "Zastavit", tint = Color.White)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Odeslat", tint = Color.White)
                    }
                }
            }
        }
    }
}
