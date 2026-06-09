package com.example.ui.chat.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.chat.ChatUiState
import com.example.ui.theme.*

@Composable
fun ChatTopBar(
    state: ChatUiState,
    onOpenDrawer: () -> Unit,
    onNewChat: () -> Unit,
    onToggleModelSelector: () -> Unit,
    onToggleSystemPrompt: () -> Unit
) {
    val activeModel = state.models.find { it.id == state.activeModelId }
    val modelName = activeModel?.displayName ?: "Vyberte model"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = LocalAppSpacing.current.small, vertical = LocalAppSpacing.current.small)
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onOpenDrawer) {
            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = TextPrimary)
        }
        
        Spacer(Modifier.weight(1f))
        
        Row(
            modifier = Modifier
                .clickable { onToggleModelSelector() }
                .padding(horizontal = LocalAppSpacing.current.medium, vertical = LocalAppSpacing.current.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = modelName, style = AppTypography.titleMedium)
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Změnit", tint = TextSecondary)
        }
        
        Spacer(Modifier.weight(1f))
        
        IconButton(onClick = onToggleSystemPrompt) {
            Icon(Icons.Default.Info, contentDescription = "System Prompt", tint = TextSecondary)
        }
        
        IconButton(onClick = onNewChat) {
            Icon(Icons.Default.Add, contentDescription = "Nový chat", tint = TextPrimary)
        }
    }
}
