package com.example.ui.providers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.ApiFormat
import com.example.domain.model.ApiKey
import com.example.domain.model.Provider
import com.example.domain.model.ProviderType
import com.example.domain.repository.ApiKeyRepository
import com.example.domain.repository.ProviderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class AddProviderUiState(
    val name: String = "",
    val baseUrl: String = "",
    val apiFormat: ApiFormat = ApiFormat.OPENAI_COMPATIBLE,
    val apiKey: String = "",
    val isLoading: Boolean = false,
    val isComplete: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AddProviderViewModel @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val apiKeyRepository: ApiKeyRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AddProviderUiState())
    val state: StateFlow<AddProviderUiState> = _state.asStateFlow()

    fun updateName(name: String) = _state.update { it.copy(name = name) }
    fun updateBaseUrl(url: String) = _state.update { it.copy(baseUrl = url) }
    fun updateApiFormat(format: ApiFormat) = _state.update { it.copy(apiFormat = format) }
    fun updateApiKey(key: String) = _state.update { it.copy(apiKey = key) }

    fun saveProvider() {
        val s = state.value
        if (s.name.isBlank()) {
            _state.update { it.copy(error = "Zadejte název providera.") }
            return
        }
        if (s.apiFormat == ApiFormat.OPENAI_COMPATIBLE && s.baseUrl.isBlank()) {
            _state.update { it.copy(error = "Zadejte Base URL.") }
            return
        }
        if (s.apiKey.isBlank()) {
            _state.update { it.copy(error = "Zadejte API klíč.") }
            return
        }

        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val providerId = UUID.randomUUID().toString()
                val provider = Provider(
                    id = providerId,
                    name = s.name,
                    type = ProviderType.CUSTOM,
                    baseUrl = s.baseUrl.ifBlank { null },
                    apiFormat = s.apiFormat,
                    isEnabled = true,
                    keyRotationEnabled = false,
                    createdAt = System.currentTimeMillis()
                )
                
                val rootKey = ApiKey(
                    id = UUID.randomUUID().toString(),
                    providerId = providerId,
                    keyReference = UUID.randomUUID().toString(),
                    label = "Základní Klíč",
                    isPrimary = true,
                    rotationOrder = 0,
                    isDisabledUntil = null
                )

                providerRepository.insertProvider(provider)
                apiKeyRepository.insertApiKey(rootKey, s.apiKey)
                
                _state.update { it.copy(isLoading = false, isComplete = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
    
    fun dismissError() = _state.update { it.copy(error = null) }
}
