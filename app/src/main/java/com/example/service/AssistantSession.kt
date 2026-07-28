package com.example.service

import android.content.Context
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.util.Log
import com.example.utils.CommandParser

class AssistantSession(context: Context) : VoiceInteractionSession(context) {

    private val ttsManager: PiperTtsManager by lazy {
        PiperTtsManager.activeInstance ?: PiperTtsManager(context.applicationContext)
    }

    override fun onCreate() {
        super.onCreate()
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
            val parsedCommand = CommandParser.parse(input)
            val responseText = if (parsedCommand != null) {
                CommandParser.execute(context.applicationContext, parsedCommand, null)
            } else {
                "Command not recognized: $input"
            }
            ttsManager.speak(responseText)
        } catch (e: Exception) {
            Log.e("AssistantSession", "Error processing assistant voice command: $input", e)
            ttsManager.speak("Sorry, I could not process that command.")
        }
    }
}
