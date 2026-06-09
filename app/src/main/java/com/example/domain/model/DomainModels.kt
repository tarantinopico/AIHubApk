package com.example.domain.model

enum class ProviderType { GOOGLE, GROQ, NVIDIA, CUSTOM }
enum class ApiFormat { GEMINI, OPENAI_COMPATIBLE }
enum class MessageRole { USER, ASSISTANT, SYSTEM }
enum class MessageStatus { SENDING, SENT, ERROR }

data class Provider(
    val id: String,
    val name: String,
    val type: ProviderType,
    val baseUrl: String?,
    val apiFormat: ApiFormat,
    val isEnabled: Boolean,
    val keyRotationEnabled: Boolean,
    val createdAt: Long
)

data class ApiKey(
    val id: String,
    val providerId: String,
    val keyReference: String,
    val label: String,
    val isPrimary: Boolean,
    val rotationOrder: Int,
    val isDisabledUntil: Long?
)

data class AiModel(
    val id: String,
    val providerId: String,
    val name: String,
    val displayName: String,
    val supportsImages: Boolean,
    val supportsFiles: Boolean,
    val contextLength: Int,
    val isFavorite: Boolean
)

data class Conversation(
    val id: String,
    val title: String,
    val modelId: String,
    val systemPrompt: String?,
    val createdAt: Long,
    val updatedAt: Long
)

data class Message(
    val id: String,
    val conversationId: String,
    val role: MessageRole,
    val content: String,
    val attachmentsJson: String?,
    val tokensUsed: Int?,
    val createdAt: Long,
    val status: MessageStatus
)
