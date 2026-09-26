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
    }

    override fun onReady() {
        super.onReady()
        // FIX 3.1: onReady MUST ONLY call super.onReady() and do zero heavy work to avoid system binding timeout crashes
        // Check RAM guard safely without blocking system thread
        if (com.example.utils.MemoryManager.isLowMemory(applicationContext)) {
            Log.w("AssistantService", "Low RAM detected (<20MB available). AssistantService running in ultra-lean mode.")
        }
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


