package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.network.api.ApiProvider
import com.example.utils.SecurePrefs
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

data class KeyStatusInfo(
    val key: String,
    val isSuccess: Boolean = true,
    val lastTested: Long = System.currentTimeMillis(),
    val message: String = "Not tested"
)

class MultiKeyManager private constructor(private val context: Context) {

    private val sharedPreferences: SharedPreferences = SecurePrefs.getEncryptedSharedPreferences(context, "aira_multi_api_keys")

    init {
        // Clean up any legacy unencrypted fallback storage for security hardening
        try {
            context.deleteSharedPreferences("aira_multi_api_keys_fallback")
        } catch (e: Throwable) {
            Log.w("MultiKeyManager", "Legacy fallback preference cleanup: ${e.message}")
        }
    }

    private val cooldowns = ConcurrentHashMap<String, Long>()
    private val statusMap = ConcurrentHashMap<String, KeyStatusInfo>()
    private val providerPointers = ConcurrentHashMap<String, Int>()

    fun getKeys(provider: String): List<String> {
        val provKey = provider.uppercase().trim()
        val jsonStr = sharedPreferences.getString("keys_$provKey", null) ?: "[]"
        val list = mutableListOf<String>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val k = array.optString(i, "").trim()
                if (k.isNotBlank() && !list.contains(k)) {
                    list.add(k)
                }
            }
        } catch (e: Exception) {
            Log.e("MultiKeyManager", "Error parsing keys for $provKey", e)
        }
        return list
    }

    fun addKey(provider: String, key: String): Boolean {
        val provKey = provider.uppercase().trim()
        val trimmedKey = key.trim()
        if (trimmedKey.isBlank()) return false

        val currentList = getKeys(provKey).toMutableList()
        if (!currentList.contains(trimmedKey)) {
            currentList.add(trimmedKey)
            saveKeysList(provKey, currentList)
            // Clear cooldown if re-adding
            cooldowns.remove(trimmedKey)
            Log.d("MultiKeyManager", "Added key to $provKey. Total keys: ${currentList.size}")
            return true
        }
        return false
    }

    fun removeKey(provider: String, index: Int): Boolean {
        val provKey = provider.uppercase().trim()
        val currentList = getKeys(provKey).toMutableList()
        if (index in currentList.indices) {
            val removedKey = currentList.removeAt(index)
            saveKeysList(provKey, currentList)
            cooldowns.remove(removedKey)
            statusMap.remove(removedKey)
            Log.d("MultiKeyManager", "Removed key at index $index from $provKey. Remaining: ${currentList.size}")
            return true
        }
        return false
    }

    fun removeKeyByValue(provider: String, key: String): Boolean {
        val provKey = provider.uppercase().trim()
        val currentList = getKeys(provKey).toMutableList()
        if (currentList.remove(key.trim())) {
            saveKeysList(provKey, currentList)
            cooldowns.remove(key.trim())
            statusMap.remove(key.trim())
            return true
        }
        return false
    }

    fun getNextKey(provider: String): String? {
        val provKey = provider.uppercase().trim()
        val keys = getKeys(provKey)
        if (keys.isEmpty()) return null

        val currentTime = System.currentTimeMillis()
        val validKeys = keys.filter { key ->
            val cooldownTime = cooldowns[key] ?: 0L
            currentTime >= cooldownTime
        }

        if (validKeys.isEmpty()) {
            Log.w("MultiKeyManager", "All ${keys.size} keys for $provKey are currently in cooldown.")
            return null
        }

        // Round-robin selection among valid keys
        val currentPointer = providerPointers[provKey] ?: 0
        val selectedIndex = currentPointer % validKeys.size
        providerPointers[provKey] = (selectedIndex + 1) % validKeys.size
        return validKeys[selectedIndex]
    }

    fun markCooldown(provider: String, key: String, durationMs: Long = 3600000L) {
        val trimmed = key.trim()
        if (trimmed.isNotBlank()) {
            cooldowns[trimmed] = System.currentTimeMillis() + durationMs
            Log.w("MultiKeyManager", "Key for $provider marked cooldown for ${durationMs / 1000}s")
        }
    }

    fun setKeyStatus(provider: String, key: String, isSuccess: Boolean, message: String) {
        val trimmed = key.trim()
        if (trimmed.isNotBlank()) {
            statusMap[trimmed] = KeyStatusInfo(
                key = trimmed,
                isSuccess = isSuccess,
                lastTested = System.currentTimeMillis(),
                message = message
            )
            // Persist status summary
            try {
                val statusObj = JSONObject()
                statusObj.put("isSuccess", isSuccess)
                statusObj.put("message", message)
                statusObj.put("lastTested", System.currentTimeMillis())
                sharedPreferences.edit().putString("status_${provider}_${trimmed.takeLast(8)}", statusObj.toString()).apply()
            } catch (e: Exception) {
                Log.e("MultiKeyManager", "Error saving status", e)
            }
        }
    }

    fun getKeyStatus(provider: String, key: String): KeyStatusInfo {
        val trimmed = key.trim()
        val inMem = statusMap[trimmed]
        if (inMem != null) return inMem

        val persisted = sharedPreferences.getString("status_${provider}_${trimmed.takeLast(8)}", null)
        if (persisted != null) {
            try {
                val obj = JSONObject(persisted)
                val status = KeyStatusInfo(
                    key = trimmed,
                    isSuccess = obj.optBoolean("isSuccess", true),
                    lastTested = obj.optLong("lastTested", 0L),
                    message = obj.optString("message", "Tested")
                )
                statusMap[trimmed] = status
                return status
            } catch (_: Exception) {}
        }
        return KeyStatusInfo(key = trimmed, isSuccess = true, message = "Not tested yet")
    }

    fun getKeyCount(provider: String): Int = getKeys(provider).size

    private fun saveKeysList(provider: String, keys: List<String>) {
        val array = JSONArray()
        keys.forEach { array.put(it) }
        val jsonStr = array.toString()
        sharedPreferences.edit().putString("keys_$provider", jsonStr).apply()
    }

    companion object {
        @Volatile
        private var INSTANCE: MultiKeyManager? = null

        fun getInstance(context: Context): MultiKeyManager {
            return INSTANCE ?: synchronized(this) {
                val instance = MultiKeyManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
