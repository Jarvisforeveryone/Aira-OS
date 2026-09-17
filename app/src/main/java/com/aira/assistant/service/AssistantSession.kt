package com.aira.assistant.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.util.Log

class AssistantSession(context: Context) : VoiceInteractionSession(context) {

    override fun onCreate() {
        super.onCreate()
        // DO NOT initialize TextToSpeech in :assistant process.
        // Instead, broadcast the response text to the main process for speech.
        // This keeps :assistant lean (< 30MB) and prevents OOM.
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
        try {
            // Send to main process for TTS handling
            val intent = Intent("com.aira.assistant.ACTION_SPEAK_TEXT").apply {
                setPackage(context.packageName)
                putExtra("text", message)
            }
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e("AssistantSession", "Failed to broadcast TTS request", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // No TTS to clean up
    }
}

