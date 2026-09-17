package com.aira.assistant

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Process
import android.util.Log
import com.aira.assistant.utils.LifecycleObserverManager
import com.aira.assistant.core.logging.Logger
import com.aira.assistant.core.memory.MemoryManager
import kotlinx.coroutines.CoroutineExceptionHandler

// Main Application class for Aira AI.
class AiraApplication : Application() {

    companion object {
        val globalCoroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
            Log.e("AiraApplication", "Global Coroutine Exception caught: ${throwable.message}", throwable)
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        // Register application context with MemoryManager
        MemoryManager.setupCrashGuard(this)

        val processName = getProcessName(this)
        Logger.i("AiraApplication", "Initializing AiraApplication in process: '$processName' (PID: ${Process.myPid()})")

        // Guard initialization: prevent auto-initializing heavy libraries in the ':assistant' process
        if (processName.endsWith(":assistant")) {
            Logger.i("AiraApplication", "Secondary process ':assistant' detected. Skipping heavy library auto-initialization.")
            return
        }

        // Register activity lifecycle observer
        registerActivityLifecycleCallbacks(LifecycleObserverManager)

        // Initialize main process auto-initializing libraries and components
        initMainProcessComponents()
    }

    private fun initMainProcessComponents() {
        Logger.i("AiraApplication", "Main process components initialized.")
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
