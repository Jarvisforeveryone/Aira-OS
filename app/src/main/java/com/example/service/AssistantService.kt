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

    override fun onReady() {
        super.onReady()

        // Memory safety check - prevent OOM
        val runtime = Runtime.getRuntime()
        val freeMemory = runtime.freeMemory() / (1024 * 1024) // in MB
        val maxMemory = runtime.maxMemory() / (1024 * 1024)
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)

        Log.d("AssistantService", "Memory - Free: ${freeMemory}MB, Used: ${usedMemory}MB, Max: ${maxMemory}MB")

        // Minimum 20MB free memory required
        if (freeMemory < 20) {
            Log.e("AssistantService", "INSUFFICIENT FREE MEMORY: ${freeMemory}MB (need 20MB minimum)")
            return // Silently fail - prevent crash
        }

        Log.d("AssistantService", "AssistantService ready - Memory OK")
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


