package com.example.service

import android.content.Context
import android.util.Log

enum class TtsEngineType {
    PIPER_ONNX_JNI,
    SYSTEM_GOOGLE_TTS,
    AUTO_FALLBACK
}

/**
 * UNIFIED TTS ENGINE FACTORY
 * Provides a single point of control for switching between Piper ONNX JNI,
 * System Google TTS, and Auto-Fallback modes across the application.
 */
object TtsEngineFactory {
    private const val TAG = "TtsEngineFactory"
    private const val PREF_KEY_TTS_ENGINE = "preferred_tts_engine"
    private const val PREFS_NAME = "aira_prefs"

    @Volatile
    private var instance: PiperTtsManager? = null

    /**
     * Gets or creates the singleton PiperTtsManager instance.
     */
    fun getPiperTtsManager(context: Context): PiperTtsManager {
        return instance ?: synchronized(this) {
            instance ?: (PiperTtsManager.activeInstance ?: PiperTtsManager(context.applicationContext)).also {
                instance = it
            }
        }
    }

    /**
     * Speaks text using the specified or preferred TTS engine type.
     */
    fun speak(
        context: Context,
        text: String,
        engineType: TtsEngineType = getEnginePreference(context)
    ) {
        val manager = getPiperTtsManager(context)
        Log.i(TAG, "Speaking via TtsEngineFactory with engine mode: $engineType")
        when (engineType) {
            TtsEngineType.PIPER_ONNX_JNI -> manager.speakText(text)
            TtsEngineType.SYSTEM_GOOGLE_TTS -> manager.speakText(text)
            TtsEngineType.AUTO_FALLBACK -> manager.speakText(text)
        }
    }

    /**
     * Stops current speech output.
     */
    fun stop(context: Context) {
        try {
            getPiperTtsManager(context).stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS output", e)
        }
    }

    /**
     * Releases TTS native resources.
     */
    fun release(context: Context) {
        try {
            getPiperTtsManager(context).release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing TTS resources", e)
        }
    }

    /**
     * Shuts down the TTS engines completely.
     */
    fun shutdown(context: Context) {
        try {
            getPiperTtsManager(context).shutdown()
            instance = null
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TTS engines", e)
        }
    }

    /**
     * Gets all supported TTS engines.
     */
    fun getAvailableEngines(): List<TtsEngineType> {
        return TtsEngineType.values().toList()
    }

    /**
     * Sets the user's preferred TTS engine.
     */
    fun setEnginePreference(context: Context, engineType: TtsEngineType) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(PREF_KEY_TTS_ENGINE, engineType.name).apply()
        Log.i(TAG, "TTS preferred engine set to: ${engineType.name}")
    }

    /**
     * Gets the user's preferred TTS engine.
     */
    fun getEnginePreference(context: Context): TtsEngineType {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val engineName = prefs.getString(PREF_KEY_TTS_ENGINE, TtsEngineType.AUTO_FALLBACK.name)
        return try {
            TtsEngineType.valueOf(engineName ?: TtsEngineType.AUTO_FALLBACK.name)
        } catch (e: Exception) {
            TtsEngineType.AUTO_FALLBACK
        }
    }
}
