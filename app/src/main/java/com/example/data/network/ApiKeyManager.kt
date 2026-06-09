package com.example.data.network

import com.example.data.secure.SecureKeyStore
import com.example.domain.repository.ApiKeyRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiKeyManager @Inject constructor(
    private val apiKeyRepository: ApiKeyRepository,
    private val secureKeyStore: SecureKeyStore
) {
    suspend fun getBestKey(providerId: String, rotationEnabled: Boolean): String {
        val keys = apiKeyRepository.getApiKeysForProvider(providerId).first()
        if (keys.isEmpty()) throw IllegalStateException("Nebyl nalezen žádný API klíč pro tohoto providera. Prosím přidejte klíč v nastavení.")
        
        val currentTime = System.currentTimeMillis()
        val availableKeys = keys.filter { it.isDisabledUntil == null || it.isDisabledUntil!! < currentTime }
        
        if (availableKeys.isEmpty()) {
            throw IllegalStateException("Všechny API klíče jsou dočasně blokovány z důvodu vyčerpání limitu (Rate Limit). Zkuste to za chvíli.")
        }
        
        val selectedKey = if (rotationEnabled) {
            availableKeys.minByOrNull { it.rotationOrder } ?: availableKeys.first()
        } else {
            availableKeys.find { it.isPrimary } ?: availableKeys.first()
        }
        
        return secureKeyStore.getKey(selectedKey.keyReference) 
            ?: throw IllegalStateException("Klíč nelze dešifrovat z úložiště.")
    }

    suspend fun markKeyAsRateLimited(providerId: String, rawKey: String, lockDurationMs: Long = 60_000) {
        val keys = apiKeyRepository.getApiKeysForProvider(providerId).first()
        val keyRef = keys.find { secureKeyStore.getKey(it.keyReference) == rawKey }
        if (keyRef != null) {
            apiKeyRepository.updateApiKey(keyRef.copy(isDisabledUntil = System.currentTimeMillis() + lockDurationMs))
        }
    }
    
    suspend fun markKeyAsInvalid(providerId: String, rawKey: String) {
        val keys = apiKeyRepository.getApiKeysForProvider(providerId).first()
        val keyRef = keys.find { secureKeyStore.getKey(it.keyReference) == rawKey }
        if (keyRef != null) {
            apiKeyRepository.updateApiKey(keyRef.copy(isDisabledUntil = System.currentTimeMillis() + 86400_000L)) // 24 hodin
        }
    }
}
