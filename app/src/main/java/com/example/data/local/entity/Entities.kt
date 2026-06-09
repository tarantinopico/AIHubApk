package com.example.data.local.entity

import androidx.room.*
import com.example.domain.model.ApiFormat
import com.example.domain.model.MessageRole
import com.example.domain.model.MessageStatus
import com.example.domain.model.ProviderType

@Entity(tableName = "providers")
data class ProviderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: ProviderType,
    val baseUrl: String?,
    val apiFormat: ApiFormat,
    val isEnabled: Boolean,
    val keyRotationEnabled: Boolean,
    val createdAt: Long
)

@Entity(
    tableName = "api_keys",
    foreignKeys = [ForeignKey(entity = ProviderEntity::class, parentColumns = ["id"], childColumns = ["providerId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("providerId")]
)
data class ApiKeyEntity(
    @PrimaryKey val id: String,
    val providerId: String,
    val keyReference: String,
    val label: String,
    val isPrimary: Boolean,
    val rotationOrder: Int,
    val isDisabledUntil: Long?
)

@Entity(
    tableName = "models",
    foreignKeys = [ForeignKey(entity = ProviderEntity::class, parentColumns = ["id"], childColumns = ["providerId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("providerId")]
)
data class ModelEntity(
    @PrimaryKey val id: String,
    val providerId: String,
    val name: String,
    val displayName: String,
    val supportsImages: Boolean,
    val supportsFiles: Boolean,
    val contextLength: Int,
    val isFavorite: Boolean,
    val inputCostPer1k: Double = 0.0,
    val outputCostPer1k: Double = 0.0
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val modelId: String,
    val systemPrompt: String?,
    val draft: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "messages",
    foreignKeys = [ForeignKey(entity = ConversationEntity::class, parentColumns = ["id"], childColumns = ["conversationId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("conversationId")]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: MessageRole,
    val content: String,
    val attachmentsJson: String?,
    val tokensUsed: Int?,
    val createdAt: Long,
    val status: MessageStatus
)
