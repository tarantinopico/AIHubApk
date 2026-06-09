package com.example.data.local

import com.example.domain.model.AiModel
import com.example.domain.model.ApiFormat
import com.example.domain.model.Provider
import com.example.domain.model.ProviderType
import com.example.domain.repository.ModelRepository
import com.example.domain.repository.ProviderRepository
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

class DatabaseSeeder @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val modelRepository: ModelRepository,
) {
    suspend fun seedDatabaseIfNeeded() {
        try {
            if (providerRepository.getProviders().first().isEmpty()) {
                val googleId = UUID.randomUUID().toString()
                val groqId = UUID.randomUUID().toString()
                val nvidiaId = UUID.randomUUID().toString()

                val googleProvider = Provider(
                    id = googleId,
                    name = "Google Gemini",
                    type = ProviderType.GOOGLE,
                    baseUrl = null,
                    apiFormat = ApiFormat.GEMINI,
                    isEnabled = true,
                    keyRotationEnabled = false,
                    createdAt = System.currentTimeMillis()
                )
                val groqProvider = Provider(
                    id = groqId,
                    name = "Groq",
                    type = ProviderType.GROQ,
                    baseUrl = "https://api.groq.com/openai/v1",
                    apiFormat = ApiFormat.OPENAI_COMPATIBLE,
                    isEnabled = true,
                    keyRotationEnabled = false,
                    createdAt = System.currentTimeMillis()
                )
                val nvidiaProvider = Provider(
                    id = nvidiaId,
                    name = "NVIDIA NIM",
                    type = ProviderType.NVIDIA,
                    baseUrl = "https://integrate.api.nvidia.com/v1",
                    apiFormat = ApiFormat.OPENAI_COMPATIBLE,
                    isEnabled = true,
                    keyRotationEnabled = false,
                    createdAt = System.currentTimeMillis()
                )

                providerRepository.insertProvider(googleProvider)
                providerRepository.insertProvider(groqProvider)
                providerRepository.insertProvider(nvidiaProvider)

                val models = listOf(
                    AiModel(UUID.randomUUID().toString(), googleId, "gemini-2.5-pro", "Gemini 2.5 Pro", true, true, 2097152, true),
                    AiModel(UUID.randomUUID().toString(), googleId, "gemini-2.5-flash", "Gemini 2.5 Flash", true, true, 1048576, false),
                    AiModel(UUID.randomUUID().toString(), googleId, "gemini-2.0-flash", "Gemini 2.0 Flash", true, true, 1048576, false),

                    AiModel(UUID.randomUUID().toString(), groqId, "llama-3.3-70b-versatile", "Llama 3.3 70B Versatile", false, false, 131072, true),
                    AiModel(UUID.randomUUID().toString(), groqId, "llama-3.1-8b-instant", "Llama 3.1 8B Instant", false, false, 131072, false),
                    AiModel(UUID.randomUUID().toString(), groqId, "mixtral-8x7b-32768", "Mixtral 8x7B", false, false, 32768, false),

                    AiModel(UUID.randomUUID().toString(), nvidiaId, "nvidia/llama-3.1-nemotron-70b-instruct", "Llama 3.1 Nemotron 70B", false, false, 131072, true),
                    AiModel(UUID.randomUUID().toString(), nvidiaId, "meta/llama-3.1-405b-instruct", "Llama 3.1 405B", false, false, 131072, false)
                )

                models.forEach { modelRepository.insertModel(it) }
            }
        } catch (e: Exception) {
            android.util.Log.e("DatabaseSeeder", "Chyba při seedování DB: ${e.message}", e)
        }
    }
}
