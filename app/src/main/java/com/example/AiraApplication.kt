package com.example

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Process
import android.util.Log
import com.example.utils.MemoryManager

/**
 * Main Application class for Aira AI.
 * Handles process name checks to prevent auto-initializing heavy libraries
 * in secondary processes such as the ':assistant' VoiceInteractionService process.
 */
class AiraApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Register application context with MemoryManager
        MemoryManager.setupCrashGuard(this)

        val processName = getProcessName(this)
        Log.i("AiraApplication", "Initializing AiraApplication in process: '$processName' (PID: ${Process.myPid()})")

        // Guard initialization: prevent auto-initializing heavy libraries in the ':assistant' process
        if (processName.endsWith(":assistant")) {
            Log.i("AiraApplication", "Secondary process ':assistant' detected. Skipping heavy library auto-initialization.")
            return
        }

        // Initialize main process auto-initializing libraries and components
        initMainProcessComponents()
    }

    private fun initMainProcessComponents() {
        Log.i("AiraApplication", "Main process components initialized.")
    }

    private fun getProcessName(context: Context): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Application.getProcessName()
        } else {
            try {
                val pid = Process.myPid()
                val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                am?.runningAppProcesses?.find { it.pid == pid }?.processName ?: ""
            } catch (e: Exception) {
                ""
            }
        }
    }
}
