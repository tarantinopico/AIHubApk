package com.example.data.repository

import com.example.data.local.dao.*
import com.example.data.secure.SecureKeyStore
import com.example.domain.model.*
import com.example.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ProviderRepositoryImpl @Inject constructor(
    private val providerDao: ProviderDao
) : ProviderRepository {
    override fun getProviders() = providerDao.getProviders().map { entities -> entities.map { it.toDomain() } }
    override suspend fun getProviderById(id: String) = providerDao.getProviderById(id)?.toDomain()
    override suspend fun insertProvider(provider: Provider) = providerDao.insertProvider(provider.toEntity())
    override suspend fun updateProvider(provider: Provider) = providerDao.updateProvider(provider.toEntity())
    override suspend fun deleteProvider(id: String) = providerDao.deleteProvider(id)
}

class ApiKeyRepositoryImpl @Inject constructor(
    private val apiKeyDao: ApiKeyDao,
    private val secureKeyStore: SecureKeyStore
) : ApiKeyRepository {
    override fun getApiKeysForProvider(providerId: String) = apiKeyDao.getApiKeysForProvider(providerId).map { entities -> entities.map { it.toDomain() } }
    override suspend fun getPrimaryApiKey(providerId: String) = apiKeyDao.getPrimaryApiKey(providerId)?.toDomain()
    override suspend fun insertApiKey(apiKey: ApiKey, actualKey: String) {
        secureKeyStore.saveKey(apiKey.keyReference, actualKey)
        apiKeyDao.insertApiKey(apiKey.toEntity())
    }
    override suspend fun updateApiKey(apiKey: ApiKey) {
        apiKeyDao.updateApiKey(apiKey.toEntity())
    }
    override suspend fun deleteApiKey(id: String) {
        val apiKey = apiKeyDao.getApiKeyById(id)
        if (apiKey != null) {
            secureKeyStore.deleteKey(apiKey.keyReference)
            apiKeyDao.deleteApiKey(id)
        }
    }
    override suspend fun getActualKey(keyReference: String): String? {
        return secureKeyStore.getKey(keyReference)
    }
}

class ModelRepositoryImpl @Inject constructor(
    private val modelDao: ModelDao
) : ModelRepository {
    override fun getModelsForProvider(providerId: String) = modelDao.getModelsForProvider(providerId).map { entities -> entities.map { it.toDomain() } }
    override fun getAllModels() = modelDao.getAllModels().map { entities -> entities.map { it.toDomain() } }
    override suspend fun getModelById(id: String) = modelDao.getModelById(id)?.toDomain()
    override suspend fun insertModel(model: AiModel) = modelDao.insertModel(model.toEntity())
    override suspend fun updateModel(model: AiModel) = modelDao.updateModel(model.toEntity())
    override suspend fun deleteModel(id: String) = modelDao.deleteModel(id)
}

class ConversationRepositoryImpl @Inject constructor(
    private val conversationDao: ConversationDao
) : ConversationRepository {
    override fun getConversations() = conversationDao.getConversations().map { entities -> entities.map { it.toDomain() } }
    override suspend fun getConversationById(id: String) = conversationDao.getConversationById(id)?.toDomain()
    override suspend fun insertConversation(conversation: Conversation) = conversationDao.insertConversation(conversation.toEntity())
    override suspend fun updateConversation(conversation: Conversation) = conversationDao.updateConversation(conversation.toEntity())
    override suspend fun deleteConversation(id: String) = conversationDao.deleteConversation(id)
}

class MessageRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao
) : MessageRepository {
    override fun getMessagesForConversation(conversationId: String) = messageDao.getMessagesForConversation(conversationId).map { entities -> entities.map { it.toDomain() } }
    override suspend fun getMessageById(id: String) = messageDao.getMessageById(id)?.toDomain()
    override suspend fun insertMessage(message: Message) = messageDao.insertMessage(message.toEntity())
    override suspend fun updateMessage(message: Message) = messageDao.updateMessage(message.toEntity())
    override suspend fun deleteMessage(id: String) = messageDao.deleteMessage(id)
}
