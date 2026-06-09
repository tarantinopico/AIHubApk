package com.example.domain.network

import com.example.core.AppResult
import com.example.domain.model.Message
import kotlinx.coroutines.flow.Flow

data class ChatRequest(
    val modelId: String,
    val messages: List<Message>,
    val systemPrompt: String?,
    val temperature: Float = 0.7f,
    val maxTokens: Int? = null
)

sealed interface ChatChunk {
    data class Text(val text: String) : ChatChunk
    data object Done : ChatChunk
    data class Error(val message: String, val throwable: Throwable? = null) : ChatChunk
}

data class RemoteModelInfo(
    val id: String,
    val name: String
)

interface ChatProvider {
    fun streamChat(request: ChatRequest): Flow<ChatChunk>
    suspend fun listModels(): AppResult<List<RemoteModelInfo>>
}
