package com.tencent.piperncnn

import android.content.res.AssetManager
import android.util.Log
import com.example.util.NativeLibraryLoader

class PiperNcnn {
    external fun loadModel(mgr: AssetManager, modelPath: String, configPath: String): Boolean
    external fun synthesize(text: String, speechRate: Float): FloatArray?

    companion object {
        init {
            try {
                Log.d("PiperNcnn", "Initializing JNI libraries via NativeLibraryLoader...")
                NativeLibraryLoader.loadLibraries()
            } catch (e: Throwable) {
                Log.e("PiperNcnn", "Failed to load native libraries in static initializer", e)
            }
        }
    }
}
