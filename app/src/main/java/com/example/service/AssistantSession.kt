package com.example.service

import android.content.Context
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.utils.CommandParser
import com.example.utils.MemoryManager

class AssistantSession(context: Context) : VoiceInteractionSession(context) {

    private var fallbackTts: TextToSpeech? = null
    private var isFallbackTtsReady = false

    private val ttsManager: PiperTtsManager? by lazy {
        if (MemoryManager.isDeviceCapable(context.applicationContext)) {
            PiperTtsManager.activeInstance ?: PiperTtsManager(context.applicationContext)
        } else {
            null
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (!MemoryManager.isDeviceCapable(context.applicationContext)) {
            try {
                fallbackTts = TextToSpeech(context.applicationContext) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        isFallbackTtsReady = true
                    }
                }
            } catch (e: Exception) {
                Log.e("AssistantSession", "Error initializing fallback TTS", e)
            }
        }
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        val command = args?.getString("query")
            ?: args?.getString("command")
            ?: args?.getString("voice_command")

        if (!command.isNullOrBlank()) {
            handleVoiceCommand(command)
        }
    }

    fun handleVoiceCommand(input: String) {
        try {
            val response = ShizukuVoiceExecutionService.executeVoiceCommand(context.applicationContext, input)
            speakResponse(response.responseMessage)
        } catch (e: Exception) {
            Log.e("AssistantSession", "Error processing assistant voice command: $input", e)
            speakResponse("Sorry, I could not process that command.")
        }
    }

    private fun speakResponse(message: String) {
        val manager = ttsManager
        if (manager != null) {
            manager.speak(message)
        } else if (isFallbackTtsReady && fallbackTts != null) {
            fallbackTts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "AssistantSessionTts")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            fallbackTts?.stop()
            fallbackTts?.shutdown()
            fallbackTts = null
        } catch (e: Exception) {
            Log.e("AssistantSession", "Error shutting down session TTS", e)
        }
    }
}

