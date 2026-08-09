package com.example.service

import android.content.Intent
import android.service.voice.VoiceInteractionService
import android.util.Log

/**
 * System VoiceInteractionService bound by Android when AIRA is selected as the
 * Default Digital Assistant App in Android Settings.
 *
 * CRITICAL STABILITY FIX:
 * VoiceInteractionService is a system-managed binder service bound directly by Android's
 * VoiceInteractionManagerService in system_server.
 * It MUST NOT call startForeground() inside onReady() or onStartCommand().
 * On Android 12/13/14+, calling startForeground with microphone type from background
 * (which happens when the user is inside System Settings picking default assistant)
 * throws ForegroundServiceStartNotAllowedException / SecurityException.
 * Re-binding by system_server then caused an infinite CPU crash loop that froze the UI,
 * turned off the screen, and caused device watchdog reboots.
 */
class AssistantService : VoiceInteractionService() {

    override fun onCreate() {
        super.onCreate()
        Log.d("AssistantService", "AIRA AssistantService onCreate called.")
        com.example.utils.MemoryDebugger.startMonitoring(this, intervalMs = 100L, durationMs = 10000L)
    }

    override fun onReady() {
        super.onReady()

        // Memory safety check - prevent OOM
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory() / (1024 * 1024)
        val totalMemory = runtime.totalMemory() / (1024 * 1024)
        val usedMemory = totalMemory - (runtime.freeMemory() / (1024 * 1024))
        val availableHeap = maxMemory - usedMemory

        Log.d("AssistantService", "Heap - Max: ${maxMemory}MB, Used: ${usedMemory}MB, Available: ${availableHeap}MB")

        if (availableHeap < 30) {
            Log.e("AssistantService", "INSUFFICIENT HEAP SPACE: ${availableHeap}MB available (need 30MB)")
            return // Silently fail - prevent crash
        }

        Log.d("AssistantService", "AssistantService ready - Heap OK")
    }

    override fun onShutdown() {
        super.onShutdown()
        Log.d("AssistantService", "AIRA AssistantService onShutdown called.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("AssistantService", "AIRA AssistantService onStartCommand called.")
        return START_STICKY
    }
}


