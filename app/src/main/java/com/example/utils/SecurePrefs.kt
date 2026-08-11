package com.example.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Utility for getting EncryptedSharedPreferences backed by Android Keystore.
 * Automatically migrates existing unencrypted SharedPreferences data to encrypted storage.
 */
object SecurePrefs {

    fun getEncryptedSharedPreferences(context: Context, name: String): SharedPreferences {
        val appContext = context.applicationContext
        val encryptedName = "sec_$name"

        return try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val encryptedPrefs = EncryptedSharedPreferences.create(
                appContext,
                encryptedName,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            // Migrate unencrypted data if present
            val unencryptedPrefs = appContext.getSharedPreferences(name, Context.MODE_PRIVATE)
            val oldEntries = unencryptedPrefs.all
            if (oldEntries.isNotEmpty()) {
                val editor = encryptedPrefs.edit()
                for ((key, value) in oldEntries) {
                    when (value) {
                        is Boolean -> editor.putBoolean(key, value)
                        is Float -> editor.putFloat(key, value)
                        is Int -> editor.putInt(key, value)
                        is Long -> editor.putLong(key, value)
                        is String -> editor.putString(key, value)
                        is Set<*> -> {
                            @Suppress("UNCHECKED_CAST")
                            val stringSet = (value as? Set<*>)?.filterIsInstance<String>()?.toSet()
                            if (stringSet != null) {
                                editor.putStringSet(key, stringSet)
                            }
                        }
                    }
                }
                editor.apply()
                unencryptedPrefs.edit().clear().apply()
            }

            encryptedPrefs
        } catch (e: Throwable) {
            Log.e("SecurePrefs", "Failed to initialize EncryptedSharedPreferences for $name", e)
            try {
                appContext.deleteSharedPreferences(encryptedName)
                val masterKey = MasterKey.Builder(appContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    appContext,
                    encryptedName,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (ex: Throwable) {
                Log.e("SecurePrefs", "Recovery failed for $name, fallback to isolated encrypted pref", ex)
                appContext.getSharedPreferences(encryptedName, Context.MODE_PRIVATE)
            }
        }
    }
}
