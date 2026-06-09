package com.example.data.repository

import com.example.data.local.entity.*
import com.example.domain.model.*

fun ProviderEntity.toDomain() = Provider(id, name, type, baseUrl, apiFormat, isEnabled, keyRotationEnabled, createdAt)
fun Provider.toEntity() = ProviderEntity(id, name, type, baseUrl, apiFormat, isEnabled, keyRotationEnabled, createdAt)

fun ApiKeyEntity.toDomain() = ApiKey(id, providerId, keyReference, label, isPrimary, rotationOrder, isDisabledUntil)
fun ApiKey.toEntity() = ApiKeyEntity(id, providerId, keyReference, label, isPrimary, rotationOrder, isDisabledUntil)

fun ModelEntity.toDomain() = AiModel(id, providerId, name, displayName, supportsImages, supportsFiles, contextLength, isFavorite, inputCostPer1k, outputCostPer1k)
fun AiModel.toEntity() = ModelEntity(id, providerId, name, displayName, supportsImages, supportsFiles, contextLength, isFavorite, inputCostPer1k, outputCostPer1k)

fun ConversationEntity.toDomain() = Conversation(id, title, modelId, systemPrompt, draft, createdAt, updatedAt)
fun Conversation.toEntity() = ConversationEntity(id, title, modelId, systemPrompt, draft, createdAt, updatedAt)

fun MessageEntity.toDomain() = Message(id, conversationId, role, content, attachmentsJson, tokensUsed, createdAt, status)
fun Message.toEntity() = MessageEntity(id, conversationId, role, content, attachmentsJson, tokensUsed, createdAt, status)
