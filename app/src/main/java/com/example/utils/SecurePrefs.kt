package com.example.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Utility for getting EncryptedSharedPreferences backed by Android Keystore.
 * Automatically migrates existing unencrypted SharedPreferences data to encrypted storage.
 * In case of irreversible Keystore corruption or hardware cryptographic failure,
 * falls back STRICTLY to an in-memory SharedPreferences instance to guarantee
 * that API keys and confidential tokens are NEVER written to disk in plaintext.
 */
object SecurePrefs {

    private const val TAG = "SecurePrefs"
    private val inMemoryPrefsMap = ConcurrentHashMap<String, InMemorySharedPreferences>()

    private val _keystoreCorruptedFlow = MutableStateFlow(false)
    val keystoreCorruptedFlow: StateFlow<Boolean> = _keystoreCorruptedFlow.asStateFlow()

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

            // Migrate legacy unencrypted data if present
            try {
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
            } catch (migrationEx: Throwable) {
                Log.w(TAG, "Legacy preference migration failed or unneeded for $name: ${migrationEx.message}")
            }

            encryptedPrefs
        } catch (e: Throwable) {
            Log.e(TAG, "EncryptedSharedPreferences init failed for $name. Attempting recovery...", e)
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
                Log.e(TAG, "Keystore recovery failed for $name. Falling back to secure volatile InMemorySharedPreferences.", ex)
                _keystoreCorruptedFlow.value = true
                inMemoryPrefsMap.getOrPut(encryptedName) {
                    val inMem = InMemorySharedPreferences(encryptedName)
                    try {
                        val unencryptedPrefs = appContext.getSharedPreferences(name, Context.MODE_PRIVATE)
                        val oldEntries = unencryptedPrefs.all
                        if (oldEntries.isNotEmpty()) {
                            val editor = inMem.edit()
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
                                        if (stringSet != null) editor.putStringSet(key, stringSet.toMutableSet())
                                    }
                                }
                            }
                            editor.apply()
                            unencryptedPrefs.edit().clear().apply()
                        }
                    } catch (mEx: Throwable) {
                        Log.w(TAG, "Fallback migration error: ${mEx.message}")
                    }
                    inMem
                }
            }
        }
    }

    /**
     * In-memory implementation of SharedPreferences for volatile fallback.
     * Prevents any plaintext leaks to disk during Keystore hardware failures.
     */
    class InMemorySharedPreferences(private val name: String) : SharedPreferences {
        private val data = ConcurrentHashMap<String, Any>()
        private val listeners = CopyOnWriteArraySet<SharedPreferences.OnSharedPreferenceChangeListener>()

        override fun getAll(): Map<String, *> = HashMap(data)

        override fun getString(key: String?, defValue: String?): String? {
            return (data[key] as? String) ?: defValue
        }

        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
            @Suppress("UNCHECKED_CAST")
            return (data[key] as? Set<String>)?.toMutableSet() ?: defValues
        }

        override fun getInt(key: String?, defValue: Int): Int {
            return (data[key] as? Int) ?: defValue
        }

        override fun getLong(key: String?, defValue: Long): Long {
            return (data[key] as? Long) ?: defValue
        }

        override fun getFloat(key: String?, defValue: Float): Float {
            return (data[key] as? Float) ?: defValue
        }

        override fun getBoolean(key: String?, defValue: Boolean): Boolean {
            return (data[key] as? Boolean) ?: defValue
        }

        override fun contains(key: String?): Boolean = key != null && data.containsKey(key)

        override fun edit(): SharedPreferences.Editor = EditorImpl()

        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
            if (listener != null) listeners.add(listener)
        }

        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
            if (listener != null) listeners.remove(listener)
        }

        private inner class EditorImpl : SharedPreferences.Editor {
            private val tempMap = HashMap<String, Any?>()
            private var clearRequested = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                if (key != null) {
                    if (value != null) tempMap[key] = value else tempMap[key] = null
                }
                return this
            }

            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
                if (key != null) {
                    if (values != null) tempMap[key] = values.toSet() else tempMap[key] = null
                }
                return this
            }

            override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
                if (key != null) tempMap[key] = value
                return this
            }

            override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
                if (key != null) tempMap[key] = value
                return this
            }

            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
                if (key != null) tempMap[key] = value
                return this
            }

            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
                if (key != null) tempMap[key] = value
                return this
            }

            override fun remove(key: String?): SharedPreferences.Editor {
                if (key != null) tempMap[key] = null
                return this
            }

            override fun clear(): SharedPreferences.Editor {
                clearRequested = true
                return this
            }

            override fun commit(): Boolean {
                apply()
                return true
            }

            override fun apply() {
                val changedKeys = mutableListOf<String>()
                synchronized(this@InMemorySharedPreferences) {
                    if (clearRequested) {
                        changedKeys.addAll(data.keys)
                        data.clear()
                        clearRequested = false
                    }
                    for ((k, v) in tempMap) {
                        if (v == null) {
                            if (data.remove(k) != null) changedKeys.add(k)
                        } else {
                            val prev = data.put(k, v)
                            if (prev != v) changedKeys.add(k)
                        }
                    }
                    tempMap.clear()
                }
                for (listener in listeners) {
                    for (k in changedKeys) {
                        listener.onSharedPreferenceChanged(this@InMemorySharedPreferences, k)
                    }
                }
            }
        }
    }
}
