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
                Log.e(TAG, "UnsatisfiedLinkError: Failed to load 'onnxruntime'. Possible missing dependency or incorrect architecture.", e)
            } catch (e: Throwable) {
                onnxError = e
                Log.e(TAG, "Unexpected error loading 'onnxruntime'.", e)
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
                Log.e(TAG, "UnsatisfiedLinkError: Failed to load 'piper'. Ensure all required C++ symbols and 'onnxruntime' are linked.", e)
            } catch (e: Throwable) {
                piperError = e
                Log.e(TAG, "Unexpected error loading 'piper'.", e)
            }
        }

        val success = isOnnxruntimeLoaded && isPiperLoaded
        if (!success && context != null) {
            val failureReason = when {
                onnxError != null && piperError != null -> "Both onnxruntime and piper failed to load."
                onnxError != null -> "Failed to load onnxruntime."
                piperError != null -> "Failed to load piper."
                else -> "Unknown error loading native libraries."
            }
            
            try {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        context.applicationContext,
                        "Piper Engine Error: $failureReason. Check logs for details.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to display toast notification", e)
            }
        }

        return success
    }

    fun isLoaded(): Boolean {
        return isOnnxruntimeLoaded && isPiperLoaded
    }
}
