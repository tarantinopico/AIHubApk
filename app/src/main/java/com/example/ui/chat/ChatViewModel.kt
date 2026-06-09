package com.example.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.*
import com.example.domain.network.ChatChunk
import com.example.domain.network.ChatRequest
import com.example.domain.repository.*
import com.example.data.network.ProviderFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.put
import java.util.UUID
import javax.inject.Inject

data class AttachmentInfo(
    val uriString: String,
    val mimeType: String,
    val base64Data: String
)

data class ChatUiState(
    val conversations: List<Conversation> = emptyList(),
    val filteredConversations: List<Conversation> = emptyList(),
    val activeConversationId: String? = null,
    val messages: List<Message> = emptyList(),
    val providers: List<Provider> = emptyList(),
    val models: List<AiModel> = emptyList(),
    val activeModelId: String? = null,
    val isGenerating: Boolean = false,
    val error: String? = null,
    val isModelSelectorOpen: Boolean = false,
    val inputText: String = "",
    val attachments: List<AttachmentInfo> = emptyList(),
    val isSystemPromptDialogOpen: Boolean = false,
    val currentSystemPrompt: String = "",
    val conversationSearchQuery: String = ""
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val modelRepository: ModelRepository,
    private val providerRepository: ProviderRepository,
    private val providerFactory: ProviderFactory
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var currentChatJob: Job? = null
    private var activeConversationJob: Job? = null

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            launch {
                conversationRepository.getConversations().collect { convs ->
                    _state.update { it.copy(
                        conversations = convs,
                        filteredConversations = if (it.conversationSearchQuery.isBlank()) convs else convs.filter { c -> c.title.contains(it.conversationSearchQuery, ignoreCase = true) }
                    ) }
                }
            }
            launch {
                providerRepository.getProviders().collect { provs ->
                    _state.update { it.copy(providers = provs) }
                }
            }
            launch {
                modelRepository.getAllModels().collect { mods ->
                    val activeId = _state.value.activeModelId ?: mods.firstOrNull()?.id
                    _state.update { it.copy(models = mods, activeModelId = activeId) }
                }
            }
        }
    }

    fun onEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.SelectConversation -> selectConversation(event.id)
            is ChatEvent.NewConversation -> createNewConversation()
            is ChatEvent.SelectModel -> _state.update { it.copy(activeModelId = event.id, isModelSelectorOpen = false) }
            is ChatEvent.ToggleModelSelector -> _state.update { it.copy(isModelSelectorOpen = !it.isModelSelectorOpen) }
            is ChatEvent.SendMessage -> sendMessage()
            is ChatEvent.UpdateInput -> _state.update { it.copy(inputText = event.text) }
            is ChatEvent.DismissError -> _state.update { it.copy(error = null) }
            is ChatEvent.RegenerateMessage -> regenerateMessage(event.messageId)
            is ChatEvent.CancelGeneration -> cancelGeneration()
            is ChatEvent.AddAttachment -> addAttachment(event.uri, event.mimeType, event.base64)
            is ChatEvent.RemoveAttachment -> removeAttachment(event.index)
            is ChatEvent.ToggleSystemPromptDialog -> toggleSystemPromptDialog()
            is ChatEvent.UpdateSystemPrompt -> _state.update { it.copy(currentSystemPrompt = event.prompt) }
            is ChatEvent.SaveSystemPrompt -> saveSystemPrompt()
            is ChatEvent.DeleteConversation -> deleteConversation(event.id)
            is ChatEvent.RenameConversation -> renameConversation(event.id, event.newTitle)
            is ChatEvent.UpdateSearchQuery -> updateSearchQuery(event.query)
        }
    }

    private fun addAttachment(uri: String, mimeType: String, base64: String) {
        val newList = _state.value.attachments.toMutableList()
        newList.add(AttachmentInfo(uri, mimeType, base64))
        _state.update { it.copy(attachments = newList) }
    }

    private fun removeAttachment(index: Int) {
        val newList = _state.value.attachments.toMutableList()
        if (index in newList.indices) newList.removeAt(index)
        _state.update { it.copy(attachments = newList) }
    }

    private fun toggleSystemPromptDialog() {
        _state.update { it.copy(
            isSystemPromptDialogOpen = !it.isSystemPromptDialogOpen,
            currentSystemPrompt = if (!it.isSystemPromptDialogOpen) {
                it.activeConversationId?.let { id ->
                    it.conversations.find { c -> c.id == id }?.systemPrompt ?: ""
                } ?: ""
            } else ""
        ) }
    }

    private fun saveSystemPrompt() {
        val convId = state.value.activeConversationId
        val prompt = state.value.currentSystemPrompt
        if (convId != null) {
            viewModelScope.launch {
                val conv = conversationRepository.getConversationById(convId)
                if (conv != null) {
                    conversationRepository.updateConversation(conv.copy(systemPrompt = prompt.takeIf { it.isNotBlank() }))
                }
            }
        }
        _state.update { it.copy(isSystemPromptDialogOpen = false) }
    }

    private fun deleteConversation(id: String) {
        viewModelScope.launch {
            conversationRepository.deleteConversation(id)
            if (state.value.activeConversationId == id) {
                createNewConversation()
            }
        }
    }

    private fun renameConversation(id: String, newTitle: String) {
        viewModelScope.launch {
            val conv = conversationRepository.getConversationById(id)
            if (conv != null) {
                conversationRepository.updateConversation(conv.copy(title = newTitle))
            }
        }
    }

    private fun updateSearchQuery(query: String) {
        _state.update { it.copy(
            conversationSearchQuery = query,
            filteredConversations = if (query.isBlank()) it.conversations else it.conversations.filter { c -> c.title.contains(query, ignoreCase = true) }
        ) }
    }

    private fun selectConversation(id: String) {
        _state.update { it.copy(activeConversationId = id, isModelSelectorOpen = false, attachments = emptyList()) }
        activeConversationJob?.cancel()
        currentChatJob?.cancel()
        _state.update { it.copy(isGenerating = false) }
        
        activeConversationJob = viewModelScope.launch {
            val conv = conversationRepository.getConversationById(id)
            if (conv != null) {
                _state.update { it.copy(activeModelId = conv.modelId) }
            }
            messageRepository.getMessagesForConversation(id).collect { msgs ->
                _state.update { it.copy(messages = msgs) }
            }
        }
    }

    private fun createNewConversation() {
        activeConversationJob?.cancel()
        currentChatJob?.cancel()
        _state.update { it.copy(activeConversationId = null, messages = emptyList(), isModelSelectorOpen = false, isGenerating = false, attachments = emptyList()) }
    }

    private fun sendMessage() {
        val text = state.value.inputText
        val attachments = state.value.attachments
        if (text.isBlank() && attachments.isEmpty() || state.value.isGenerating) return

        _state.update { it.copy(inputText = "", attachments = emptyList()) }
        
        viewModelScope.launch {
            var convId = state.value.activeConversationId
            val modelId = state.value.activeModelId
            
            if (modelId == null) {
                _state.update { it.copy(error = "Vyberte nejprve AI model.") }
                return@launch
            }

            var conv = convId?.let { conversationRepository.getConversationById(it) }

            if (convId == null || conv == null) {
                convId = UUID.randomUUID().toString()
                val newConv = Conversation(
                    id = convId,
                    title = if (text.isNotBlank()) text.take(30) + if (text.length > 30) "..." else "" else "Nová konverzace",
                    modelId = modelId,
                    systemPrompt = null,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                conversationRepository.insertConversation(newConv)
                conv = newConv
                selectConversation(convId)
            } else {
                conversationRepository.updateConversation(conv.copy(updatedAt = System.currentTimeMillis() + 100))
            }

            val attachmentsJsonStr = if (attachments.isNotEmpty()) {
                val jsonArr = kotlinx.serialization.json.buildJsonArray {
                    attachments.forEach { att ->
                        add(kotlinx.serialization.json.buildJsonObject {
                            put("mimeType", att.mimeType)
                            put("data", att.base64Data)
                        })
                    }
                }
                jsonArr.toString()
            } else null

            val userMsg = Message(
                id = UUID.randomUUID().toString(),
                conversationId = convId,
                role = MessageRole.USER,
                content = text,
                attachmentsJson = attachmentsJsonStr,
                tokensUsed = null,
                createdAt = System.currentTimeMillis(),
                status = MessageStatus.SENT
            )
            messageRepository.insertMessage(userMsg)

            generateResponse(convId, modelId, conv.systemPrompt)
        }
    }

    private fun regenerateMessage(messageId: String) {
        if (state.value.isGenerating) return
        viewModelScope.launch {
            val messages = state.value.messages
            val index = messages.indexOfFirst { it.id == messageId }
            if (index == -1) return@launch
            
            val msgsToRemove = messages.subList(index, messages.size)
            msgsToRemove.forEach { messageRepository.deleteMessage(it.id) }
            
            val convId = state.value.activeConversationId ?: return@launch
            val modelId = state.value.activeModelId ?: return@launch
            val conv = conversationRepository.getConversationById(convId)
            
            generateResponse(convId, modelId, conv?.systemPrompt)
        }
    }

    private fun cancelGeneration() {
        currentChatJob?.cancel()
        _state.update { it.copy(isGenerating = false) }
        
        viewModelScope.launch {
            val aiMsgLocal = state.value.messages.lastOrNull { it.role == MessageRole.ASSISTANT && it.status == MessageStatus.SENDING }
            if (aiMsgLocal != null) {
                messageRepository.updateMessage(aiMsgLocal.copy(status = MessageStatus.SENT))
            }
        }
    }

    private suspend fun generateResponse(conversationId: String, modelId: String, systemPrompt: String?) {
        _state.update { it.copy(isGenerating = true, error = null) }
        
        val aiMsgId = UUID.randomUUID().toString()
        val aiMsg = Message(
            id = aiMsgId,
            conversationId = conversationId,
            role = MessageRole.ASSISTANT,
            content = "",
            attachmentsJson = null,
            tokensUsed = null,
            createdAt = System.currentTimeMillis() + 1,
            status = MessageStatus.SENDING
        )
        messageRepository.insertMessage(aiMsg)

        currentChatJob?.cancel()
        currentChatJob = viewModelScope.launch {
            try {
                val model = modelRepository.getModelById(modelId)
                    ?: throw java.lang.Exception("Model nebyl nalezen.")
                val provider = providerFactory.createActiveProvider(model.providerId)
                
                val history = messageRepository.getMessagesForConversation(conversationId)
                    .first()
                    .filter { it.status == MessageStatus.SENT && it.id != aiMsgId }
                
                val request = ChatRequest(
                    modelId = model.name,
                    messages = history,
                    systemPrompt = systemPrompt
                )

                var accumulatedContent = ""
                provider.streamChat(request).collect { chunk ->
                    when (chunk) {
                        is ChatChunk.Text -> {
                            accumulatedContent += chunk.text
                            messageRepository.updateMessage(aiMsg.copy(content = accumulatedContent))
                        }
                        is ChatChunk.Done -> {
                            messageRepository.updateMessage(aiMsg.copy(content = accumulatedContent, status = MessageStatus.SENT))
                        }
                        is ChatChunk.Error -> {
                            _state.update { it.copy(error = chunk.message) }
                            messageRepository.updateMessage(aiMsg.copy(content = accumulatedContent, status = MessageStatus.ERROR))
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Generování bylo zrušeno
                val currentMsg = messageRepository.getMessageById(aiMsgId)
                if (currentMsg != null) {
                    messageRepository.updateMessage(currentMsg.copy(status = MessageStatus.SENT))
                }
            } catch (e: Exception) {
                val currentMsg = messageRepository.getMessageById(aiMsgId)
                if (currentMsg != null) {
                    messageRepository.updateMessage(currentMsg.copy(status = MessageStatus.ERROR))
                }
                _state.update { it.copy(error = e.localizedMessage ?: "Neznámá chyba při generování") }
            } finally {
                _state.update { it.copy(isGenerating = false) }
            }
        }
    }
}

sealed interface ChatEvent {
    data class SelectConversation(val id: String) : ChatEvent
    data object NewConversation : ChatEvent
    data class SelectModel(val id: String) : ChatEvent
    data object ToggleModelSelector : ChatEvent
    data object SendMessage : ChatEvent
    data class UpdateInput(val text: String) : ChatEvent
    data object DismissError : ChatEvent
    data class RegenerateMessage(val messageId: String) : ChatEvent
    data object CancelGeneration : ChatEvent
    data class AddAttachment(val uri: String, val mimeType: String, val base64: String) : ChatEvent
    data class RemoveAttachment(val index: Int) : ChatEvent
    data object ToggleSystemPromptDialog : ChatEvent
    data class UpdateSystemPrompt(val prompt: String) : ChatEvent
    data object SaveSystemPrompt : ChatEvent
    data class DeleteConversation(val id: String) : ChatEvent
    data class RenameConversation(val id: String, val newTitle: String) : ChatEvent
    data class UpdateSearchQuery(val query: String) : ChatEvent
}
