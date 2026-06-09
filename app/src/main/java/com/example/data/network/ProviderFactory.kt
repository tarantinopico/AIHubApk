package com.example.data.network

import com.example.core.AppResult
import com.example.data.network.providers.GeminiProvider
import com.example.data.network.providers.OpenAiCompatibleProvider
import com.example.domain.model.ApiFormat
import com.example.domain.network.ChatChunk
import com.example.domain.network.ChatProvider
import com.example.domain.network.ChatRequest
import com.example.domain.network.RemoteModelInfo
import com.example.domain.repository.ProviderRepository
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderFactory @Inject constructor(
    private val client: HttpClient,
    private val providerRepository: ProviderRepository,
    private val apiKeyManager: ApiKeyManager
) {
    suspend fun createActiveProvider(providerId: String): ChatProvider {
        val provider = providerRepository.getProviderById(providerId) 
            ?: throw IllegalArgumentException("Provider nebyl nalezen.")

        return object : ChatProvider {
            override fun streamChat(request: ChatRequest): Flow<ChatChunk> = flow {
                var attempts = 0
                val maxAttempts = 3
                var success = false
                
                while (attempts < maxAttempts && !success) {
                    try {
                        val activeKey = apiKeyManager.getBestKey(providerId, provider.keyRotationEnabled)
                        
                        val delegate = when (provider.apiFormat) {
                            ApiFormat.GEMINI -> GeminiProvider(client, activeKey)
                            ApiFormat.OPENAI_COMPATIBLE -> OpenAiCompatibleProvider(client, provider.baseUrl ?: "https://api.openai.com/v1", activeKey)
                        }
                        
                        delegate.streamChat(request).collect { chunk ->
                            emit(chunk)
                        }
                        success = true

                    } catch (e: Exception) {
                        val msg = e.message ?: ""
                        if (msg.contains("HTTP 429") || msg.contains("rate limit", ignoreCase = true) || msg.contains("Too Many Requests", ignoreCase = true)) {
                            try {
                                val keyRef = apiKeyManager.getBestKey(providerId, provider.keyRotationEnabled)
                                apiKeyManager.markKeyAsRateLimited(providerId, keyRef)
                            } catch (e2: Exception) {}
                            attempts++
                            if (attempts >= maxAttempts) {
                                emit(ChatChunk.Error("Vyčerpán limit požadavků pro všechny dostupné klíče. Zkuste to za okamžik.", e))
                            }
                        } else if (msg.contains("HTTP 401") || msg.contains("HTTP 403") || msg.contains("Unauthorized", ignoreCase = true)) {
                            try {
                                val keyRef = apiKeyManager.getBestKey(providerId, provider.keyRotationEnabled)
                                apiKeyManager.markKeyAsInvalid(providerId, keyRef)
                            } catch (e2: Exception) {}
                            attempts++
                            if (attempts >= maxAttempts) {
                                emit(ChatChunk.Error("Odmítnut přístup. Zkontrolujte prosím svůj API klíč.", e))
                            }
                        } else {
                            emit(ChatChunk.Error("Došlo k chybě připojení k providerovi: $msg", e))
                            success = true 
                        }
                    }
                }
            }

            override suspend fun listModels(): AppResult<List<RemoteModelInfo>> {
                return try {
                    val activeKey = apiKeyManager.getBestKey(providerId, provider.keyRotationEnabled)
                    val delegate = when (provider.apiFormat) {
                        ApiFormat.GEMINI -> GeminiProvider(client, activeKey)
                        ApiFormat.OPENAI_COMPATIBLE -> OpenAiCompatibleProvider(client, provider.baseUrl ?: "https://api.openai.com/v1", activeKey)
                    }
                    delegate.listModels()
                } catch (e: Exception) {
                    AppResult.Error(e.message ?: "Chyba načítání modelů", e)
                }
            }
        }
    }
}
