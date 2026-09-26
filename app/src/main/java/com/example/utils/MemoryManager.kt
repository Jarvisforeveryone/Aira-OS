package com.example.utils

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import com.example.util.Logger
import java.lang.ref.WeakReference

enum class NativeModelType {
    LLAMA_CPP,
    PIPER_TTS,
    VOSK_STT
}

/**
 * UNIFIED MEMORY MANAGER FOR JNI NATIVE MEMORY
 * Dynamically manages RAM allocations, prevents crashes on low-RAM devices (< 3GB),
 * enforces lazy model loading on demand, and ensures immediate native memory release.
 */
object MemoryManager {
    private const val TAG = "MemoryManager"
    private const val MIN_RAM_BYTES_FOR_LLAMA = 3L * 1024L * 1024L * 1024L // 3 GB RAM Threshold

    private const val PREFS_SAFETY = "aira_safety_guard"
    private const val KEY_CRASH_COUNT = "crash_count"
    private const val KEY_SAFE_MODE = "safe_mode"

    private val loadedModels = mutableSetOf<NativeModelType>()
    private var appContext: Context? = null
    private var currentActivityRef: WeakReference<Activity>? = null

    /**
     * Sets up Uncaught Exception Handler to auto-detect crashes and enable Safe Mode on next startup.
     */
    fun setupCrashGuard(context: Context) {
        appContext = context.applicationContext
        if (context is Activity) {
            currentActivityRef = WeakReference(context)
        }
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Logger.e(TAG, "Uncaught exception caught by Safety Guard! Enabling Safe Mode for next boot.", throwable)
                val prefs = context.applicationContext.getSharedPreferences(PREFS_SAFETY, Context.MODE_PRIVATE)
                val count = prefs.getInt(KEY_CRASH_COUNT, 0) + 1
                prefs.edit()
                    .putInt(KEY_CRASH_COUNT, count)
                    .putBoolean(KEY_SAFE_MODE, true)
                    .apply()
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to record crash in preferences", e)
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    /**
     * Unregisters activity references to prevent memory leaks.
     */
    fun unregister(activity: Activity? = null) {
        if (activity == null || currentActivityRef?.get() == activity) {
            currentActivityRef?.clear()
            currentActivityRef = null
            Logger.d(TAG, "MemoryManager activity reference cleared safely.")
        }
    }

    /**
     * Checks if the app is running in Auto-Safe Mode due to past crashes or low RAM (< 3GB).
     */
    fun isSafeMode(context: Context): Boolean {
        val safeContext = context.applicationContext
        val prefs = safeContext.getSharedPreferences(PREFS_SAFETY, Context.MODE_PRIVATE)
        val forcedSafeMode = prefs.getBoolean(KEY_SAFE_MODE, false)
        return forcedSafeMode || !isDeviceCapable(safeContext)
    }

    /**
     * Returns user-facing warning text if device RAM is low or safe mode is active.
     */
    fun getLowRamWarningText(context: Context): String? {
        val safeContext = context.applicationContext
        val ramMb = getTotalRamMb(safeContext)
        return if (ramMb < 3072) {
            "2GB device detected ($ramMb MB). Cloud mode enabled for safety. Offline features disabled to prevent crashes."
        } else if (isSafeMode(safeContext)) {
            "Safe Mode active due to a previous unexpected event. Heavy background native models have been safely paused."
        } else {
            null
        }
    }

    /**
     * Resets safe mode after successful boot without crashes.
     */
    fun clearSafeMode(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS_SAFETY, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SAFE_MODE, false)
            .putInt(KEY_CRASH_COUNT, 0)
            .apply()
    }

    /**
     * Checks if the device has sufficient total RAM (>= 3GB) to run heavy JNI models like local Llama 3.2.
     * On devices with < 3GB RAM (e.g. 2GB RAM devices), Llama 3.2 is disabled to prevent native OOM crashes.
     */
    fun isLlamaSupported(context: Context): Boolean {
        return try {
            val actManager = context.applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            if (actManager == null) return false
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)
            val totalRam = memInfo.totalMem
            val isLowRam = actManager.isLowRamDevice
            Logger.d(TAG, "Device total RAM: ${totalRam / (1024 * 1024)} MB, isLowRamDevice: $isLowRam")
            !isLowRam && totalRam >= MIN_RAM_BYTES_FOR_LLAMA
        } catch (e: Exception) {
            Logger.e(TAG, "Error checking system RAM, defaulting to false for safety", e)
            false
        }
    }

    /**
     * Checks if the device has sufficient RAM (>= 3GB) to run Piper ONNX TTS natively without OOM.
     */
    fun isPiperSupported(context: Context): Boolean {
        return isLlamaSupported(context.applicationContext)
    }

    /**
     * Checks if the device has sufficient RAM (>= 3GB) to run Vosk STT natively without OOM.
     */
    fun isVoskSupported(context: Context): Boolean {
        return isLlamaSupported(context.applicationContext)
    }

    /**
     * Checks if the device has sufficient total RAM (>= 3GB) to run heavy JNI models.
     */
    fun isDeviceCapable(context: Context): Boolean = isLlamaSupported(context.applicationContext)

    /**
     * Checks if Offline Mode (Llama, Piper, Vosk) is supported on this device.
     * Offline mode is disabled ONLY on 2GB devices (< 3GB RAM).
     */
    fun isOfflineSupported(context: Context): Boolean = isLlamaSupported(context.applicationContext)

    fun isOfflineSupported(): Boolean {
        val ctx = appContext
        return if (ctx != null) {
            isOfflineSupported(ctx)
        } else {
            false
        }
    }

    /**
     * Returns total RAM in megabytes.
     */
    fun getTotalRamMb(context: Context): Long {
        return try {
            val actManager = context.applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return 2048L
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)
            memInfo.totalMem / (1024 * 1024)
        } catch (e: Exception) {
            2048L
        }
    }

    /**
     * Checks if current available memory is critically low.
     */
    fun isLowMemory(context: Context): Boolean {
        return try {
            val actManager = context.applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)
            memInfo.lowMemory
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Loads a native model on demand if system memory permits.
     * Skips heavy models on low-RAM (< 4GB) devices to prevent OOM crashes.
     */
    @Synchronized
    fun loadModelOnDemand(context: Context, type: NativeModelType, onLoadAction: () -> Unit): Boolean {
        val safeContext = context.applicationContext
        if (type == NativeModelType.LLAMA_CPP && !isLlamaSupported(safeContext)) {
            Logger.w(TAG, "Device RAM < 3GB. Skipping heavy Llama 3.2 local model to prevent native memory crash.")
            return false
        }
        if (type == NativeModelType.PIPER_TTS && !isPiperSupported(safeContext)) {
            Logger.w(TAG, "Device RAM < 3GB. Skipping native Piper TTS engine to prevent native memory crash.")
            return false
        }
        if (type == NativeModelType.VOSK_STT && !isVoskSupported(safeContext)) {
            Logger.w(TAG, "Device RAM < 3GB. Skipping native Vosk STT engine to prevent native memory crash.")
            return false
        }

        return try {
            Logger.i(TAG, "Loading native model on demand: $type")
            onLoadAction()
            loadedModels.add(type)
            Logger.i(TAG, "Successfully loaded $type. Currently active models: $loadedModels")
            true
        } catch (e: Throwable) {
            Logger.e(TAG, "Failed to load native model $type on demand", e)
            false
        }
    }

    /**
     * Releases a native model and deallocates its memory immediately.
     */
    @Synchronized
    fun releaseModel(type: NativeModelType, onReleaseAction: () -> Unit) {
        try {
            Logger.i(TAG, "Releasing native model memory: $type")
            onReleaseAction()
            loadedModels.remove(type)
            Logger.i(TAG, "Successfully released $type. Currently active models: $loadedModels")
        } catch (e: Throwable) {
            Logger.e(TAG, "Error while releasing native model $type", e)
        }
    }

    /**
     * Returns whether a specific native model is currently registered as loaded in memory.
     */
    @Synchronized
    fun isModelLoaded(type: NativeModelType): Boolean {
        return loadedModels.contains(type)
    }

    /**
     * Returns whether any native model is currently registered as loaded in memory.
     */
    @Synchronized
    fun isAnyModelLoaded(): Boolean {
        return loadedModels.isNotEmpty()
    }
}
