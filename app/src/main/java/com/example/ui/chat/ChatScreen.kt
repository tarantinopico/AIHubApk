package com.example.ui.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.domain.model.AiModel
import com.example.domain.model.Message
import com.example.domain.model.MessageRole
import com.example.domain.model.MessageStatus
import com.example.domain.model.Provider
import com.example.ui.components.*
import com.example.ui.chat.components.*
import com.example.ui.navigation.Destination
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToProviders: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Surface,
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
            ) {
                DrawerContent(
                    state = state,
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToProviders = onNavigateToProviders,
                    onNewChat = {
                        viewModel.onEvent(ChatEvent.NewConversation)
                        coroutineScope.launch { drawerState.close() }
                    },
                    onSelectConversation = {
                        viewModel.onEvent(ChatEvent.SelectConversation(it))
                        coroutineScope.launch { drawerState.close() }
                    },
                    onSearchChange = { viewModel.onEvent(ChatEvent.UpdateSearchQuery(it)) },
                    onDelete = { viewModel.onEvent(ChatEvent.DeleteConversation(it)) },
                    onRename = { id, title -> viewModel.onEvent(ChatEvent.RenameConversation(id, title)) }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                ChatTopBar(
                    state = state,
                    onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                    onNewChat = { viewModel.onEvent(ChatEvent.NewConversation) },
                    onToggleModelSelector = { viewModel.onEvent(ChatEvent.ToggleModelSelector) },
                    onToggleSystemPrompt = { viewModel.onEvent(ChatEvent.ToggleSystemPromptDialog) },
                    onShowStats = { viewModel.onEvent(ChatEvent.ToggleStatsDialog) }
                )
            },
            snackbarHost = {
                if (state.error != null) {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .padding(LocalAppSpacing.current.large)
                        .clip(LocalAppShapes.current.large)
                        .background(Error.copy(alpha = 0.2f))
                        .padding(LocalAppSpacing.current.large)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = state.error!!, color = Error, modifier = Modifier.weight(1f), style = AppTypography.bodyMedium)
                            IconButton(onClick = { viewModel.onEvent(ChatEvent.DismissError) }) {
                                Icon(Icons.Default.Close, contentDescription = "Zavřít", tint = Error)
                            }
                        }
                    }
                }
            },
            containerColor = Background,
            bottomBar = {
                val activeModel = state.models.find { it.id == state.activeModelId }
                ChatInputBar(
                    inputText = state.inputText,
                    attachments = state.attachments,
                    onInputChanged = { text -> viewModel.onEvent(ChatEvent.UpdateInput(text)) },
                    onSend = { viewModel.onEvent(ChatEvent.SendMessage) },
                    onCancel = { viewModel.onEvent(ChatEvent.CancelGeneration) },
                    onAddAttachment = { uri, mime, b64 -> viewModel.onEvent(ChatEvent.AddAttachment(uri, mime, b64)) },
                    onRemoveAttachment = { idx -> viewModel.onEvent(ChatEvent.RemoveAttachment(idx)) },
                    isGenerating = state.isGenerating,
                    supportsImages = activeModel?.supportsImages ?: false
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (state.activeConversationId == null && state.messages.isEmpty()) {
                    EmptyChatState()
                } else {
                    MessageList(
                        messages = state.messages,
                        isGenerating = state.isGenerating,
                        onRegenerate = { id -> viewModel.onEvent(ChatEvent.RegenerateMessage(id)) },
                        onEdit = { msg -> viewModel.onEvent(ChatEvent.EditMessage(msg)) }
                    )
                }

                // Temporary dimmed overlay
                AnimatedVisibility(
                    visible = state.isModelSelectorOpen,
                    enter = fadeIn(tween(150)),
                    exit = fadeOut(tween(150))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable { viewModel.onEvent(ChatEvent.ToggleModelSelector) }
                    )
                }

                // Model Selector Panel
                AnimatedVisibility(
                    visible = state.isModelSelectorOpen,
                    enter = expandVertically(tween(150), expandFrom = Alignment.Top) + fadeIn(tween(150)),
                    exit = shrinkVertically(tween(150), shrinkTowards = Alignment.Top) + fadeOut(tween(150)),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    ModelSelector(
                        providers = state.providers,
                        models = state.models,
                        activeModelId = state.activeModelId,
                        onSelectModel = { id -> viewModel.onEvent(ChatEvent.SelectModel(id)) },
                        onAddProvider = {
                            viewModel.onEvent(ChatEvent.ToggleModelSelector)
                            onNavigateToProviders()
                        }
                    )
                }

                if (state.isSystemPromptDialogOpen) {
                    AlertDialog(
                        onDismissRequest = { viewModel.onEvent(ChatEvent.ToggleSystemPromptDialog) },
                        title = { Text("System Prompt" , style = AppTypography.titleLarge, color = TextPrimary) },
                        text = {
                            OutlinedTextField(
                                value = state.currentSystemPrompt,
                                onValueChange = { viewModel.onEvent(ChatEvent.UpdateSystemPrompt(it)) },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                                placeholder = { Text("Zadejte instrukce (např. 'Jste expert na kotlin...')" ) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { viewModel.onEvent(ChatEvent.SaveSystemPrompt) }) {
                                Text("Uložit", color = MaterialTheme.colorScheme.primary)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { viewModel.onEvent(ChatEvent.ToggleSystemPromptDialog) }) {
                                Text("Zrušit", color = TextSecondary)
                            }
                        },
                        containerColor = SurfaceElevated
                    )
                }

                if (state.isStatsDialogOpen) {
                    val activeModel = state.models.find { it.id == state.activeModelId }
                    val inputTokens = state.messages.filter { it.role == MessageRole.USER }.sumOf { it.tokensUsed ?: (it.content.length / 4) }
                    val outputTokens = state.messages.filter { it.role == MessageRole.ASSISTANT }.sumOf { it.tokensUsed ?: (it.content.length / 4) }
                    val totalTokens = inputTokens + outputTokens
                    val cost = ((inputTokens / 1000.0) * (activeModel?.inputCostPer1k ?: 0.0)) + ((outputTokens / 1000.0) * (activeModel?.outputCostPer1k ?: 0.0))
                    
                    AlertDialog(
                        onDismissRequest = { viewModel.onEvent(ChatEvent.ToggleStatsDialog) },
                        title = { Text("Statistiky konverzace", color = TextPrimary) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Vstupní tokeny: $inputTokens", style = AppTypography.bodyLarge, color = TextPrimary)
                                Text("Výstupní tokeny: $outputTokens", style = AppTypography.bodyLarge, color = TextPrimary)
                                HorizontalDivider(color = Surface)
                                Text("Celkem tokenů: $totalTokens", style = AppTypography.titleMedium, color = TextPrimary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Orientační cena: $${java.lang.String.format(java.util.Locale.US, "%.5f", cost)}", style = AppTypography.bodyMedium, color = TextSecondary)
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { viewModel.onEvent(ChatEvent.ToggleStatsDialog) }) {
                                Text("Zavřít", color = GradientMiddle)
                            }
                        },
                        containerColor = SurfaceElevated
                    )
                }
            }
        }
    }
}
