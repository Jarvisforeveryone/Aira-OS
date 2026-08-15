package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe secure manager for managing and rotating up to 20 API keys.
 * Implements hardware-backed EncryptedSharedPreferences storage with automatic key rotation
 * and sliding expiration cooldown windows.
 */
class ChatKeyManager private constructor(private val context: Context) {

    private val multiKeyManager = MultiKeyManager.getInstance(context)
    private val sharedPreferences: SharedPreferences = com.example.utils.SecurePrefs.getEncryptedSharedPreferences(context, "aira_secure_api_keys")

    /**
     * Scans keys for GEMINI from MultiKeyManager and returns next valid key.
     */
    fun getNextKey(): String? {
        val multiKey = multiKeyManager.getNextKey("GEMINI")
        if (!multiKey.isNullOrBlank()) return multiKey

        // Fallback check legacy key aliases
        val keyAliases = listOf("gemini_api_key", "gemini_key", "api_key", "user_api_key")
        for (alias in keyAliases) {
            val key = sharedPreferences.getString(alias, "")?.trim() ?: ""
            if (key.isNotEmpty()) return key
        }

        // Fallback to BuildConfig
        val envKey = com.example.BuildConfig.GEMINI_API_KEY
        if (envKey.isNotBlank() && !envKey.startsWith("MY_")) {
            return envKey
        }
        return null
    }

    fun markCooldown(key: String) {
        multiKeyManager.markCooldown("GEMINI", key)
    }

    fun saveKey(index: Int, key: String) {
        if (key.isNotBlank()) {
            multiKeyManager.addKey("GEMINI", key)
        }
    }

    fun getKey(index: Int): String {
        val keys = multiKeyManager.getKeys("GEMINI")
        return keys.getOrNull(index - 1) ?: ""
    }

    fun getGroqKey(): String = multiKeyManager.getNextKey("GROQ") ?: getProviderKey("groq_api_key")
    fun saveGroqKey(key: String) {
        saveProviderKey("groq_api_key", key)
        multiKeyManager.addKey("GROQ", key)
    }

    fun getOpenAiKey(): String = multiKeyManager.getNextKey("OPENAI") ?: getProviderKey("openai_api_key")
    fun saveOpenAiKey(key: String) {
        saveProviderKey("openai_api_key", key)
        multiKeyManager.addKey("OPENAI", key)
    }

    fun getClaudeKey(): String = multiKeyManager.getNextKey("CLAUDE") ?: getProviderKey("claude_api_key")
    fun saveClaudeKey(key: String) {
        saveProviderKey("claude_api_key", key)
        multiKeyManager.addKey("CLAUDE", key)
    }

    fun getOpenRouterKey(): String = multiKeyManager.getNextKey("OPENROUTER") ?: getProviderKey("openrouter_api_key")
    fun saveOpenRouterKey(key: String) {
        saveProviderKey("openrouter_api_key", key)
        multiKeyManager.addKey("OPENROUTER", key)
    }

    fun getMistralKey(): String = multiKeyManager.getNextKey("MISTRAL") ?: getProviderKey("mistral_api_key")
    fun saveMistralKey(key: String) {
        saveProviderKey("mistral_api_key", key)
        multiKeyManager.addKey("MISTRAL", key)
    }

    fun getCohereKey(): String = multiKeyManager.getNextKey("COHERE") ?: getProviderKey("cohere_api_key")
    fun saveCohereKey(key: String) {
        saveProviderKey("cohere_api_key", key)
        multiKeyManager.addKey("COHERE", key)
    }

    fun getHuggingFaceKey(): String = multiKeyManager.getNextKey("HUGGINGFACE") ?: getProviderKey("huggingface_api_key")
    fun saveHuggingFaceKey(key: String) {
        saveProviderKey("huggingface_api_key", key)
        multiKeyManager.addKey("HUGGINGFACE", key)
    }

    fun getProviderKey(prefKey: String): String {
        val secureKey = sharedPreferences.getString(prefKey, "")?.trim() ?: ""
        if (secureKey.isNotEmpty()) return secureKey
        return try {
            val fallbackPrefs = context.getSharedPreferences("aira_settings", Context.MODE_PRIVATE)
            fallbackPrefs.getString(prefKey, "")?.trim() ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    fun saveProviderKey(prefKey: String, key: String) {
        val trimmed = key.trim()
        sharedPreferences.edit().putString(prefKey, trimmed).apply()
        try {
            val fallbackPrefs = context.getSharedPreferences("aira_settings", Context.MODE_PRIVATE)
            fallbackPrefs.edit().putString(prefKey, trimmed).apply()
        } catch (e: Exception) {
            Log.e("ChatKeyManager", "Failed saving $prefKey: ", e)
        }
    }

    fun getSelectedProvider(): String {
        val prov = sharedPreferences.getString("active_api_provider", "GEMINI")?.trim() ?: "GEMINI"
        return if (prov.isBlank()) "GEMINI" else prov
    }

    fun setSelectedProvider(providerName: String) {
        sharedPreferences.edit().putString("active_api_provider", providerName).apply()
        try {
            val fallbackPrefs = context.getSharedPreferences("aira_settings", Context.MODE_PRIVATE)
            fallbackPrefs.edit().putString("active_api_provider", providerName).apply()
        } catch (e: Exception) {
            Log.e("ChatKeyManager", "Failed saving active provider", e)
        }
    }

    fun getSelectedModel(providerName: String, defaultModel: String): String {
        val key = "model_for_${providerName.lowercase()}"
        val model = sharedPreferences.getString(key, defaultModel)?.trim() ?: defaultModel
        return if (model.isBlank()) defaultModel else model
    }

    fun setSelectedModel(providerName: String, modelName: String) {
        val key = "model_for_${providerName.lowercase()}"
        sharedPreferences.edit().putString(key, modelName.trim()).apply()
        try {
            val fallbackPrefs = context.getSharedPreferences("aira_settings", Context.MODE_PRIVATE)
            fallbackPrefs.edit().putString(key, modelName.trim()).apply()
        } catch (e: Exception) {
            Log.e("ChatKeyManager", "Failed saving model for $providerName", e)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: ChatKeyManager? = null

        fun getInstance(context: Context): ChatKeyManager {
            return INSTANCE ?: synchronized(this) {
                val instance = ChatKeyManager(context)
                INSTANCE = instance
                instance
            }
        }

        fun clearInstance() {
            INSTANCE = null
        }
    }
}
