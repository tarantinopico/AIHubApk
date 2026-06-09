package com.example.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.components.AppTextField
import com.example.ui.components.gradientBackground
import com.example.ui.theme.*

@Composable
fun DrawerContent(
    state: ChatUiState,
    onNavigateToSettings: () -> Unit,
    onNavigateToProviders: () -> Unit,
    onNewChat: () -> Unit,
    onSelectConversation: (String) -> Unit,
    onSearchChange: (String) -> Unit,
    onDelete: (String) -> Unit,
    onRename: (String, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(LocalAppSpacing.current.large)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = LocalAppSpacing.current.medium)) {
            Box(modifier = Modifier
                .size(32.dp)
                .gradientBackground(LocalAppShapes.current.small))
            Spacer(Modifier.width(LocalAppSpacing.current.medium))
            Text(text = "AI Hub", style = AppTypography.titleLarge)
        }

        AppTextField(
            value = state.conversationSearchQuery,
            onValueChange = onSearchChange,
            placeholder = "Hledat konverzace...",
            modifier = Modifier.fillMaxWidth().padding(bottom = LocalAppSpacing.current.medium)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(LocalAppShapes.current.medium)
                .background(SurfaceElevated)
                .clickable { onNewChat() }
                .padding(LocalAppSpacing.current.large),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Add, contentDescription = "Nový", tint = TextPrimary)
            Spacer(Modifier.width(LocalAppSpacing.current.medium))
            Text("Nový chat", style = AppTypography.labelLarge)
        }

        Spacer(Modifier.height(LocalAppSpacing.current.medium))
        Text("Konverzace", style = AppTypography.labelMedium, color = TextMuted, modifier = Modifier.padding(vertical = LocalAppSpacing.current.small))
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(state.filteredConversations, key = { it.id }) { conv ->
                var showOptions by remember { mutableStateOf(false) }
                var showRenameDialog by remember { mutableStateOf(false) }
                val isActive = conv.id == state.activeConversationId
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(LocalAppShapes.current.small)
                        .then(if (isActive) Modifier.background(SurfaceElevated.copy(alpha = 0.5f)) else Modifier)
                        .clickable { onSelectConversation(conv.id) }
                        .padding(LocalAppSpacing.current.medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = if (isActive) TextPrimary else TextMuted, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(LocalAppSpacing.current.medium))
                    Text(
                        text = conv.title, 
                        style = AppTypography.bodyMedium, 
                        color = if (isActive) TextPrimary else TextSecondary,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Box {
                        IconButton(onClick = { showOptions = true }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Možnosti", tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(
                            expanded = showOptions,
                            onDismissRequest = { showOptions = false },
                            containerColor = SurfaceElevated
                        ) {
                            DropdownMenuItem(
                                text = { Text("Přejmenovat") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = { showOptions = false; showRenameDialog = true }
                            )
                            DropdownMenuItem(
                                text = { Text("Smazat", color = Error) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Error) },
                                onClick = { showOptions = false; onDelete(conv.id) }
                            )
                        }
                    }
                }
                
                if (showRenameDialog) {
                    var newTitle by remember { mutableStateOf(conv.title) }
                    AlertDialog(
                        onDismissRequest = { showRenameDialog = false },
                        title = { Text("Přejmenovat") },
                        text = {
                            AppTextField(
                                value = newTitle,
                                onValueChange = { newTitle = it },
                                placeholder = "Nový název"
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { onRename(conv.id, newTitle); showRenameDialog = false }) {
                                Text("Uložit", color = MaterialTheme.colorScheme.primary)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showRenameDialog = false }) {
                                Text("Zrušit", color = TextSecondary)
                            }
                        },
                        containerColor = SurfaceElevated
                    )
                }
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
        
        Spacer(Modifier.height(LocalAppSpacing.current.small))
        DrawerItem(icon = Icons.Default.VpnKey, text = "Provideři & Klíče", onClick = onNavigateToProviders)
        DrawerItem(icon = Icons.Default.Settings, text = "Nastavení", onClick = onNavigateToSettings)
    }
}

@Composable
fun DrawerItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LocalAppShapes.current.small)
            .clickable { onClick() }
            .padding(LocalAppSpacing.current.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextSecondary)
        Spacer(Modifier.width(LocalAppSpacing.current.medium))
        Text(text, style = AppTypography.bodyMedium, color = TextSecondary)
    }
}
