package com.example.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

import com.example.data.secure.SecureKeyStore
import com.example.domain.repository.ApiKeyRepository
import kotlinx.coroutines.launch
import org.json.JSONObject
import com.example.core.CryptoUtils
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val isDarkMode: Boolean = true,
    val backupMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureKeyStore: SecureKeyStore,
    private val apiKeyRepository: ApiKeyRepository
) : ViewModel() {

    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(SettingsUiState(isDarkMode = prefs.getBoolean("dark_mode", true)))
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    fun toggleDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean("dark_mode", enabled).apply()
        _state.update { it.copy(isDarkMode = enabled) }
    }

    fun clearBackupMessage() {
        _state.update { it.copy(backupMessage = null) }
    }

    fun exportData(password: String): String? {
        return try {
            val keys = secureKeyStore.getAllKeys()
            val json = JSONObject()
            
            val keysObj = JSONObject()
            keys.forEach { (k, v) -> keysObj.put(k, v) }
            json.put("keys", keysObj)
            
            val settingsObj = JSONObject()
            settingsObj.put("dark_mode", prefs.getBoolean("dark_mode", true))
            json.put("settings", settingsObj)

            val plainText = json.toString()
            CryptoUtils.encrypt(plainText, password.toCharArray())
        } catch (e: Exception) {
            _state.update { it.copy(backupMessage = "Chyba při exportu: ${e.message}") }
            null
        }
    }

    fun importData(encryptedData: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val plainText = CryptoUtils.decrypt(encryptedData, password.toCharArray())
                val json = JSONObject(plainText)
                
                val keysObj = json.getJSONObject("keys")
                val importedKeys = mutableMapOf<String, String>()
                val iter = keysObj.keys()
                while (iter.hasNext()) {
                    val key = iter.next()
                    importedKeys[key] = keysObj.getString(key)
                }
                secureKeyStore.importAllKeys(importedKeys)
                
                if (json.has("settings")) {
                    val settingsObj = json.getJSONObject("settings")
                    if (settingsObj.has("dark_mode")) {
                        val darkMode = settingsObj.getBoolean("dark_mode")
                        toggleDarkMode(darkMode)
                    }
                }
                
                _state.update { it.copy(backupMessage = "Záloha úspěšně obnovena.") }
            } catch (e: Exception) {
                _state.update { it.copy(backupMessage = "Chyba při obnově: Zřejmě špatné heslo nebo poškozená záloha.") }
            }
        }
    }
}
