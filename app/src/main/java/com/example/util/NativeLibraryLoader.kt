package com.example.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast

object NativeLibraryLoader {
    private const val TAG = "NativeLibraryLoader"
    private var isOnnxruntimeLoaded = false
    private var isPiperLoaded = false

    @Synchronized
    fun loadLibraries(context: Context? = null): Boolean {
        if (isOnnxruntimeLoaded && isPiperLoaded) {
            return true
        }

        // Memory & process safety guard: block JNI loading on 2GB devices or :assistant process
        if (context != null) {
            if (!com.example.utils.MemoryManager.isOfflineSupported(context)) {
                Log.w(TAG, "Offline mode not supported on 2GB devices (<3GB RAM). Native library loading blocked.")
                return false
            }
        } else if (!com.example.utils.MemoryManager.isOfflineSupported()) {
            Log.w(TAG, "Offline mode not supported on 2GB devices (<3GB RAM). Native library loading blocked.")
            return false
        }

        try {
            val pid = android.os.Process.myPid()
            val processName = if (context != null) {
                val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
                am?.runningAppProcesses?.find { it.pid == pid }?.processName ?: ""
            } else ""

            if (processName.endsWith(":assistant")) {
                Log.w(TAG, "Native library loading blocked in ':assistant' process.")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking process name in NativeLibraryLoader", e)
        }

        var onnxError: Throwable? = null
        var piperError: Throwable? = null

        // Load onnxruntime first as libpiper depends on it
        if (!isOnnxruntimeLoaded) {
            try {
                Log.d(TAG, "Attempting to load native library: onnxruntime")
                System.loadLibrary("onnxruntime")
                isOnnxruntimeLoaded = true
                Log.i(TAG, "Native library 'onnxruntime' loaded successfully.")
            } catch (e: UnsatisfiedLinkError) {
                onnxError = e
                Log.w(TAG, "Native library 'onnxruntime' not available on this device ABI: ${e.message}")
            } catch (e: Throwable) {
                onnxError = e
                Log.w(TAG, "Error loading 'onnxruntime': ${e.message}")
            }
        }

        if (!isPiperLoaded) {
            try {
                Log.d(TAG, "Attempting to load native library: piper")
                System.loadLibrary("piper")
                isPiperLoaded = true
                Log.i(TAG, "Native library 'piper' loaded successfully.")
            } catch (e: UnsatisfiedLinkError) {
                piperError = e
                Log.w(TAG, "Native library 'piper' not available on this device ABI: ${e.message}")
            } catch (e: Throwable) {
                piperError = e
                Log.w(TAG, "Error loading 'piper': ${e.message}")
            }
        }

        val success = isOnnxruntimeLoaded && isPiperLoaded
        if (!success) {
            Log.i(TAG, "Native Piper libraries unavailable. App will automatically use Google/System TTS engine.")
        }

        return success
    }

    fun isLoaded(): Boolean {
        return isOnnxruntimeLoaded && isPiperLoaded
    }

    fun areLibrariesLoaded(): Boolean = isLoaded()
}
