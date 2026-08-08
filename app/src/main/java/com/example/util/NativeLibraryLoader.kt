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
