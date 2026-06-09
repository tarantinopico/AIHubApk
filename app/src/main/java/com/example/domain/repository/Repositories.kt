package com.example.domain.repository

import com.example.domain.model.*
import kotlinx.coroutines.flow.Flow

interface ProviderRepository {
    fun getProviders(): Flow<List<Provider>>
    suspend fun getProviderById(id: String): Provider?
    suspend fun insertProvider(provider: Provider)
    suspend fun updateProvider(provider: Provider)
    suspend fun deleteProvider(id: String)
}

interface ApiKeyRepository {
    fun getApiKeysForProvider(providerId: String): Flow<List<ApiKey>>
    suspend fun getPrimaryApiKey(providerId: String): ApiKey?
    suspend fun insertApiKey(apiKey: ApiKey, actualKey: String)
    suspend fun updateApiKey(apiKey: ApiKey)
    suspend fun deleteApiKey(id: String)
    suspend fun getActualKey(keyReference: String): String?
}

interface ModelRepository {
    fun getModelsForProvider(providerId: String): Flow<List<AiModel>>
    fun getAllModels(): Flow<List<AiModel>>
    suspend fun getModelById(id: String): AiModel?
    suspend fun insertModel(model: AiModel)
    suspend fun updateModel(model: AiModel)
    suspend fun deleteModel(id: String)
}

interface ConversationRepository {
    fun getConversations(): Flow<List<Conversation>>
    suspend fun getConversationById(id: String): Conversation?
    suspend fun insertConversation(conversation: Conversation)
    suspend fun updateConversation(conversation: Conversation)
    suspend fun deleteConversation(id: String)
}

interface MessageRepository {
    fun getMessagesForConversation(conversationId: String): Flow<List<Message>>
    suspend fun getMessageById(id: String): Message?
    suspend fun insertMessage(message: Message)
    suspend fun updateMessage(message: Message)
    suspend fun deleteMessage(id: String)
}
