package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.service.voice.VoiceInteractionService
import android.util.Log
import androidx.core.app.NotificationCompat

class AssistantService : VoiceInteractionService() {

    override fun onReady() {
        super.onReady()
        Log.d("AssistantService", "AssistantService onReady called")
        startForegroundNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundNotification()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startForegroundNotification() {
        val channelId = "aira_voice_assistant_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "AIRA Voice Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background microphone processing for AIRA assistant"
            }
            notificationManager?.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("AIRA Voice Service")
            .setContentText("Listening for wake-word and hands-free voice commands...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        val notificationId = 1001

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    notificationId,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(notificationId, notification)
            }
            Log.d("AssistantService", "Foreground service started with MICROPHONE type")
        } catch (e: Exception) {
            Log.e("AssistantService", "Error starting foreground service", e)
        }
    }
}

