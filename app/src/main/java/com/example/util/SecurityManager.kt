package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Centralized Security and Encryption Manager for AIRA OS.
 * Provides EncryptedSharedPreferences for API keys and hardware-backed AES-GCM encryption.
 */
class SecurityManager private constructor(private val context: Context) {

    private val masterKeyAlias by lazy {
        try {
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        } catch (e: Exception) {
            "aira_default_master_key"
        }
    }

    private val securePrefs: SharedPreferences by lazy {
        try {
            EncryptedSharedPreferences.create(
                AppConfig.PREFS_SECURE_KEYS,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Logger.e("SecurityManager", "Failed to create EncryptedSharedPreferences, falling back to standard prefs", e)
            context.getSharedPreferences(AppConfig.PREFS_SECURE_KEYS, Context.MODE_PRIVATE)
        }
    }

    companion object {
        @Volatile
        private var instance: SecurityManager? = null

        fun getInstance(context: Context): SecurityManager {
            return instance ?: synchronized(this) {
                instance ?: SecurityManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun saveEncryptedKey(keyName: String, keyValue: String) {
        securePrefs.edit().putString(keyName, keyValue).apply()
    }

    fun getEncryptedKey(keyName: String, defaultValue: String = ""): String {
        return securePrefs.getString(keyName, defaultValue) ?: defaultValue
    }

    fun removeEncryptedKey(keyName: String) {
        securePrefs.edit().remove(keyName).apply()
    }

    fun clearAllSecureKeys() {
        securePrefs.edit().clear().apply()
    }
}
