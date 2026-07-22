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
class ChatKeyManager private constructor(context: Context) {

    private val sharedPreferences: SharedPreferences = try {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "aira_secure_api_keys",
            masterKeyAlias,
            context.applicationContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.e("ChatKeyManager", "EncryptedSharedPreferences fail, fallback to cleartext settings", e)
        context.applicationContext.getSharedPreferences("aira_api_keys_fallback", Context.MODE_PRIVATE)
    }

    // Stores timestamps when keys will emerge from cooldown state (one hour limits)
    private val cooldowns = ConcurrentHashMap<String, Long>()

    /**
     * Scans keys from 1 to 20 and returns the first configured key that is not under cooldown.
     * Returns null if no active keys are available or all keys are in cooldown state.
     */
    fun getNextKey(): String? {
        val currentTime = System.currentTimeMillis()
        for (i in 1..20) {
            val key = sharedPreferences.getString("chat_api_$i", "") ?: ""
            if (key.isNotEmpty()) {
                val cooldownTime = cooldowns[key] ?: 0L
                if (currentTime >= cooldownTime) {
                    return key
                }
            }
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
        if (index in 1..20) {
            sharedPreferences.edit().putString("chat_api_$index", key.trim()).apply()
            // Instantly clear any cooldown flags on new configs
            cooldowns.remove(key.trim())
        }
    }

    fun getKey(index: Int): String {
        return sharedPreferences.getString("chat_api_$index", "") ?: ""
    }

    fun getGroqKey(): String {
        return sharedPreferences.getString("groq_api_key", "") ?: ""
    }

    fun saveGroqKey(key: String) {
        sharedPreferences.edit().putString("groq_api_key", key.trim()).apply()
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
    }
}
