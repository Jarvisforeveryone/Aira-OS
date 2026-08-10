package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class ActiveListeningService : Service() {

    companion object {
        const val CHANNEL_ID = "aira_active_listening_channel"
        const val NOTIFICATION_ID = 4001
        const val ACTION_START = "com.example.ACTION_START_ACTIVE_LISTENING"
        const val ACTION_STOP = "com.example.ACTION_STOP_ACTIVE_LISTENING"
        const val EXTRA_WAKE_WORD = "extra_wake_word"

        fun startService(context: Context, wakeWord: String = "Hey Aira") {
            val intent = Intent(context, ActiveListeningService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_WAKE_WORD, wakeWord)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e("ActiveListeningService", "Failed to launch service", e)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, ActiveListeningService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e("ActiveListeningService", "Failed to stop service", e)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        if (action == ACTION_STOP) {
            Log.d("ActiveListeningService", "Stopping Active Listening Foreground Service")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val wakeWord = intent?.getStringExtra(EXTRA_WAKE_WORD) ?: "Hey Aira"
        createNotificationChannel()
        val notification = createNotification(wakeWord)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            Log.d("ActiveListeningService", "Active Listening Foreground Service started successfully with wake word: $wakeWord")
        } catch (e: Exception) {
            Log.e("ActiveListeningService", "Failed to start foreground with microphone type, falling back to standard foreground", e)
            try {
                startForeground(NOTIFICATION_ID, notification)
            } catch (e2: Exception) {
                Log.e("ActiveListeningService", "Critical failure launching foreground service", e2)
            }
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Active Listening Mode"
            val descriptionText = "Persistent background voice wake monitoring service"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(wakeWord: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpenApp = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ActiveListeningService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AIRA Active Listening Mode")
            .setContentText("Background voice wake active • Say \"$wakeWord\"")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pendingOpenApp)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop Active Listening",
                pendingStop
            )
            .setSubText("Hands-Free Background Service")
            .build()
    }
}
