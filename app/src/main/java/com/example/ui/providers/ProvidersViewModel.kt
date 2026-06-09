package com.example.ui.providers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.AppResult
import com.example.data.network.providers.GeminiProvider
import com.example.data.network.providers.OpenAiCompatibleProvider
import com.example.domain.model.AiModel
import com.example.domain.model.ApiFormat
import com.example.domain.model.ApiKey
import com.example.domain.model.Provider
import com.example.domain.repository.ApiKeyRepository
import com.example.domain.repository.ModelRepository
import com.example.domain.repository.ProviderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ProviderWithDetails(
    val provider: Provider,
    val keys: List<ApiKey>,
    val models: List<AiModel>
)

data class ProvidersUiState(
    val providers: List<ProviderWithDetails> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val expandedProviderId: String? = null,
    val showAddKeyDialogForProviderId: String? = null,
    
    val showAddModelDialogForProviderId: String? = null,
    val testingKeyResult: String? = null,
    val testingKeyLoading: Boolean = false
)

@HiltViewModel
class ProvidersViewModel @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val apiKeyRepository: ApiKeyRepository,
    private val modelRepository: ModelRepository,
    private val httpClient: HttpClient
) : ViewModel() {

    private val _state = MutableStateFlow(ProvidersUiState())
    val state: StateFlow<ProvidersUiState> = _state.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            providerRepository.getProviders().combine(modelRepository.getAllModels()) { providers, models ->
                providers to models
            }.collect { (providers, models) ->
                val detailsList = mutableListOf<ProviderWithDetails>()
                for (p in providers) {
                    val keys = apiKeyRepository.getApiKeysForProvider(p.id).first()
                    val pModels = models.filter { it.providerId == p.id }
                    detailsList.add(ProviderWithDetails(p, keys, pModels))
                }
                _state.update { it.copy(providers = detailsList, isLoading = false) }
            }
        }
    }

    fun toggleProviderExpansion(id: String) {
        _state.update { it.copy(expandedProviderId = if (it.expandedProviderId == id) null else id) }
    }

    fun toggleProviderEnabled(provider: Provider, enabled: Boolean) {
        viewModelScope.launch {
            providerRepository.updateProvider(provider.copy(isEnabled = enabled))
        }
    }

    fun toggleKeyRotation(provider: Provider, enabled: Boolean) {
        viewModelScope.launch {
            providerRepository.updateProvider(provider.copy(keyRotationEnabled = enabled))
        }
    }

    fun showAddKeyDialog(providerId: String) {
        _state.update { it.copy(showAddKeyDialogForProviderId = providerId) }
    }

    fun hideAddKeyDialog() {
        _state.update { it.copy(showAddKeyDialogForProviderId = null) }
    }

    fun addApiKey(providerId: String, label: String, key: String, isPrimary: Boolean) {
        viewModelScope.launch {
            val existingKeys = apiKeyRepository.getApiKeysForProvider(providerId).first()
            if (isPrimary) {
                existingKeys.filter { it.isPrimary }.forEach {
                    apiKeyRepository.updateApiKey(it.copy(isPrimary = false))
                }
            }
            val newKey = ApiKey(
                id = UUID.randomUUID().toString(),
                providerId = providerId,
                keyReference = UUID.randomUUID().toString(),
                label = label,
                isPrimary = isPrimary || existingKeys.isEmpty(),
                rotationOrder = existingKeys.size,
                isDisabledUntil = null
            )
            apiKeyRepository.insertApiKey(newKey, key)
            hideAddKeyDialog()
            forceRefreshData()
        }
    }

    fun deleteApiKey(key: ApiKey) {
        viewModelScope.launch {
            apiKeyRepository.deleteApiKey(key.id)
            forceRefreshData()
        }
    }
    
    fun setPrimaryApiKey(providerId: String, keyId: String) {
        viewModelScope.launch {
            val keys = apiKeyRepository.getApiKeysForProvider(providerId).first()
            keys.forEach {
                if (it.id == keyId) {
                    apiKeyRepository.updateApiKey(it.copy(isPrimary = true))
                } else if (it.isPrimary) {
                    apiKeyRepository.updateApiKey(it.copy(isPrimary = false))
                }
            }
            forceRefreshData()
        }
    }

    fun testApiKey(provider: Provider, apiKey: ApiKey) {
        _state.update { it.copy(testingKeyLoading = true, testingKeyResult = null) }
        viewModelScope.launch {
            try {
                val actualKey = apiKeyRepository.getActualKey(apiKey.keyReference)
                if (actualKey == null) {
                    _state.update { it.copy(testingKeyLoading = false, testingKeyResult = "Klíč nelze načíst.") }
                    return@launch
                }
                
                val delegate = when (provider.apiFormat) {
                    ApiFormat.GEMINI -> GeminiProvider(httpClient, actualKey)
                    ApiFormat.OPENAI_COMPATIBLE -> OpenAiCompatibleProvider(httpClient, provider.baseUrl ?: "https://api.openai.com/v1", actualKey)
                }
                when (val result = delegate.listModels()) {
                    is AppResult.Success -> {
                        _state.update { it.copy(testingKeyLoading = false, testingKeyResult = "✅ Úspěch: Nalezeno ${result.data.size} modelů.") }
                    }
                    is AppResult.Error -> {
                        _state.update { it.copy(testingKeyLoading = false, testingKeyResult = "❌ Chyba: ${result.message}") }
                    }
                    is AppResult.Loading -> {}
                }
            } catch (e: Exception) {
                _state.update { it.copy(testingKeyLoading = false, testingKeyResult = "❌ Výjimka: ${e.message}") }
            }
        }
    }
    
    fun clearTestResult() {
        _state.update { it.copy(testingKeyResult = null) }
    }

    fun loadModelsForProvider(provider: Provider) {
        viewModelScope.launch {
            val actualKeyRaw = try {
                val primaryKey = apiKeyRepository.getPrimaryApiKey(provider.id)
                    ?: apiKeyRepository.getApiKeysForProvider(provider.id).first().firstOrNull()
                primaryKey?.let { apiKeyRepository.getActualKey(it.keyReference) }
            } catch (e: Exception) { null }

            if (actualKeyRaw == null) {
                _state.update { it.copy(error = "Nelze načíst modely: Chybí platný API klíč.") }
                return@launch
            }
            
            _state.update { it.copy(isLoading = true) }
            val delegate = when (provider.apiFormat) {
                ApiFormat.GEMINI -> GeminiProvider(httpClient, actualKeyRaw)
                ApiFormat.OPENAI_COMPATIBLE -> OpenAiCompatibleProvider(httpClient, provider.baseUrl ?: "https://api.openai.com/v1", actualKeyRaw)
            }
            when (val res = delegate.listModels()) {
                is AppResult.Success -> {
                    // Update / insert models
                    val existingModels = modelRepository.getModelsForProvider(provider.id).first()
                    res.data.forEach { remote ->
                        val existing = existingModels.find { it.id == remote.id }
                        if (existing == null) {
                            modelRepository.insertModel(
                                AiModel(
                                    id = remote.id,
                                    providerId = provider.id,
                                    name = remote.id, // For Gemini/OpenAI, id is used as model name
                                    displayName = remote.name,
                                    supportsImages = remote.name.contains("vision", ignoreCase = true) || remote.name.contains("gemini-1.5", ignoreCase = true) || remote.name.contains("gemini-pro-vision", ignoreCase = true),
                                    supportsFiles = false,
                                    contextLength = 8192,
                                    isFavorite = false
                                )
                            )
                        } else {
                            modelRepository.updateModel(existing.copy(name = remote.id, displayName = remote.name))
                        }
                    }
                    forceRefreshData() // refresh the local list
                }
                is AppResult.Error -> {
                    _state.update { it.copy(error = "Chyba při načítání modelů: ${res.message}", isLoading = false) }
                }
                is AppResult.Loading -> {}
            }
        }
    }
    
    fun showAddModelDialog(providerId: String) {
        _state.update { it.copy(showAddModelDialogForProviderId = providerId) }
    }
    
    fun hideAddModelDialog() {
        _state.update { it.copy(showAddModelDialogForProviderId = null) }
    }

    fun addManualModel(providerId: String, idName: String, displayName: String, contextLength: Int, supportsImages: Boolean) {
        viewModelScope.launch {
            val model = AiModel(
                id = idName, // typically provider-specific ID like gpt-4
                providerId = providerId,
                name = idName,
                displayName = displayName,
                supportsImages = supportsImages,
                supportsFiles = false,
                contextLength = contextLength,
                isFavorite = false
            )
            modelRepository.insertModel(model)
            hideAddModelDialog()
            forceRefreshData()
        }
    }
    
    fun toggleModelFavorite(model: AiModel) {
        viewModelScope.launch {
            modelRepository.updateModel(model.copy(isFavorite = !model.isFavorite))
            forceRefreshData()
        }
    }
    
    fun deleteModel(model: AiModel) {
        viewModelScope.launch {
            modelRepository.deleteModel(model.id)
            forceRefreshData()
        }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    private suspend fun forceRefreshData() {
        val providers = providerRepository.getProviders().first()
        val models = modelRepository.getAllModels().first()
        val detailsList = mutableListOf<ProviderWithDetails>()
        for (p in providers) {
            val keys = apiKeyRepository.getApiKeysForProvider(p.id).first()
            val pModels = models.filter { it.providerId == p.id }
            detailsList.add(ProviderWithDetails(p, keys, pModels))
        }
        _state.update { it.copy(providers = detailsList, isLoading = false) }
    }
}
