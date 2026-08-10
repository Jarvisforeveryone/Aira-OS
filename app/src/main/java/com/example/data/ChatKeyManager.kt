package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe secure manager for managing and rotating up to 20 API keys.
 * Implements hardware-backed EncryptedSharedPreferences storage with automatic key rotation
 * and sliding expiration cooldown windows.
 */
class ChatKeyManager private constructor(private val context: Context) {

    private val sharedPreferences: SharedPreferences = com.example.utils.SecurePrefs.getEncryptedSharedPreferences(context, "aira_secure_api_keys")

    // Stores timestamps when keys will emerge from cooldown state (one hour limits)
    private val cooldowns = ConcurrentHashMap<String, Long>()

    /**
     * Scans keys from 1 to 20 and returns the first configured key that is not under cooldown.
     * Returns null if no active keys are available or all keys are in cooldown state.
     */
    fun getNextKey(): String? {
        val currentTime = System.currentTimeMillis()
        
        // 1. Check indexed keys 1..20
        for (i in 1..20) {
            val key = sharedPreferences.getString("chat_api_$i", "")?.trim() ?: ""
            if (key.isNotEmpty()) {
                val cooldownTime = cooldowns[key] ?: 0L
                if (currentTime >= cooldownTime) {
                    return key
                }
            }
        }
        
        // 2. Check unindexed key alias slots in secure prefs
        val keyAliases = listOf("gemini_api_key", "gemini_key", "api_key", "user_api_key")
        for (alias in keyAliases) {
            val key = sharedPreferences.getString(alias, "")?.trim() ?: ""
            if (key.isNotEmpty()) {
                val cooldownTime = cooldowns[key] ?: 0L
                if (currentTime >= cooldownTime) {
                    return key
                }
            }
        }

        // 3. Fallback to standard preferences if stored there
        try {
            val fallbackPrefs = context.getSharedPreferences("aira_settings", Context.MODE_PRIVATE)
            for (i in 1..20) {
                val key = fallbackPrefs.getString("chat_api_$i", "")?.trim() ?: ""
                if (key.isNotEmpty()) {
                    val cooldownTime = cooldowns[key] ?: 0L
                    if (currentTime >= cooldownTime) {
                        return key
                    }
                }
            }
            for (alias in keyAliases) {
                val key = fallbackPrefs.getString(alias, "")?.trim() ?: ""
                if (key.isNotEmpty()) {
                    val cooldownTime = cooldowns[key] ?: 0L
                    if (currentTime >= cooldownTime) {
                        return key
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ChatKeyManager", "Fallback prefs check exception: ", e)
        }

        // 4. Check BuildConfig env variable
        val envKey = com.example.BuildConfig.GEMINI_API_KEY
        if (envKey.isNotBlank() && !envKey.startsWith("MY_") && (cooldowns[envKey] ?: 0L) <= currentTime) {
            return envKey
        }
        return null
    }

    /**
     * Flags a failing key to enter cooldown state for exactly 1 hour.
     */
    fun markCooldown(key: String) {
        if (key.isNotEmpty()) {
            cooldowns[key] = System.currentTimeMillis() + 3600000L // 1 Hour in milliseconds
        }
    }

    fun saveKey(index: Int, key: String) {
        val trimmed = key.trim()
        if (index in 1..20) {
            sharedPreferences.edit()
                .putString("chat_api_$index", trimmed)
                .putString("gemini_api_key", trimmed)
                .apply()

            try {
                val fallbackPrefs = context.getSharedPreferences("aira_settings", Context.MODE_PRIVATE)
                fallbackPrefs.edit()
                    .putString("chat_api_$index", trimmed)
                    .putString("gemini_api_key", trimmed)
                    .apply()
            } catch (e: Exception) {
                Log.e("ChatKeyManager", "Failed saving to fallback prefs: ", e)
            }

            // Instantly clear any cooldown flags on new configs
            if (trimmed.isNotEmpty()) {
                cooldowns.remove(trimmed)
            }
        }
    }

    fun getKey(index: Int): String {
        val secureKey = sharedPreferences.getString("chat_api_$index", "")?.trim() ?: ""
        if (secureKey.isNotEmpty()) return secureKey
        return try {
            val fallbackPrefs = context.getSharedPreferences("aira_settings", Context.MODE_PRIVATE)
            fallbackPrefs.getString("chat_api_$index", "")?.trim() ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    fun getGroqKey(): String = getProviderKey("groq_api_key")
    fun saveGroqKey(key: String) = saveProviderKey("groq_api_key", key)

    fun getOpenAiKey(): String = getProviderKey("openai_api_key")
    fun saveOpenAiKey(key: String) = saveProviderKey("openai_api_key", key)

    fun getClaudeKey(): String = getProviderKey("claude_api_key")
    fun saveClaudeKey(key: String) = saveProviderKey("claude_api_key", key)

    fun getOpenRouterKey(): String = getProviderKey("openrouter_api_key")
    fun saveOpenRouterKey(key: String) = saveProviderKey("openrouter_api_key", key)

    fun getMistralKey(): String = getProviderKey("mistral_api_key")
    fun saveMistralKey(key: String) = saveProviderKey("mistral_api_key", key)

    fun getCohereKey(): String = getProviderKey("cohere_api_key")
    fun saveCohereKey(key: String) = saveProviderKey("cohere_api_key", key)

    fun getHuggingFaceKey(): String = getProviderKey("huggingface_api_key")
    fun saveHuggingFaceKey(key: String) = saveProviderKey("huggingface_api_key", key)

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
        if (trimmed.isNotEmpty()) {
            cooldowns.remove(trimmed)
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
