package com.example.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import java.util.Calendar

object AiraNotificationManager {

    const val CHANNEL_REMINDERS_ID = "aira_reminders"
    const val CHANNEL_ALERTS_ID = "aira_alerts"

    private const val NOTIFICATION_ID_BASE_REMINDERS = 1000
    private const val NOTIFICATION_ID_BASE_ALERTS = 2000

    /**
     * Initializes notification channels for Aira.
     * Must be called at app startup or when a notification needs to be created.
     */
    fun initNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nameReminders = "Aira Reminders"
            val descReminders = "Notifications for scheduled user tasks and reminders"
            val importanceReminders = NotificationManager.IMPORTANCE_HIGH
            val channelReminders = NotificationChannel(CHANNEL_REMINDERS_ID, nameReminders, importanceReminders).apply {
                description = descReminders
                enableVibration(true)
                enableLights(true)
                lightColor = androidx.core.content.ContextCompat.getColor(context, R.color.aira_info)
            }

            val nameAlerts = "Aira Breaking Alerts"
            val descAlerts = "Breaking news, intelligence briefings, and critical system alerts"
            val importanceAlerts = NotificationManager.IMPORTANCE_DEFAULT
            val channelAlerts = NotificationChannel(CHANNEL_ALERTS_ID, nameAlerts, importanceAlerts).apply {
                description = descAlerts
                enableVibration(true)
                enableLights(true)
                lightColor = 0xFFFF1744.toInt() // Red Alert
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channelReminders)
            notificationManager?.createNotificationChannel(channelAlerts)
            Log.d("AiraNotificationMgr", "Notification channels initialized successfully")
        }
    }

    /**
     * Schedules an AlarmManager task to broadcast a reminder intent.
     */
    fun scheduleReminderAlarm(context: Context, id: Long, title: String, timeLabel: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.example.ACTION_TRIGGER_REMINDER"
            putExtra("reminder_id", id)
            putExtra("reminder_title", title)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = parseReminderTimeToMillis(timeLabel)
        Log.d("AiraNotificationMgr", "Scheduling alarm for reminder $id ($title) at $timeLabel (millis: $triggerTime)")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: SecurityException) {
            Log.w("AiraNotificationMgr", "Exact alarm scheduling failed, falling back to inexact", e)
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } catch (e: Exception) {
            Log.e("AiraNotificationMgr", "Failed to set alarm", e)
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    /**
     * Cancels an active alarm for a reminder.
     */
    fun cancelReminderAlarm(context: Context, id: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.example.ACTION_TRIGGER_REMINDER"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("AiraNotificationMgr", "Cancelled scheduled alarm for reminder ID: $id")
        }
    }

    /**
     * Instantly shows a local notification for a reminder.
     */
    fun showReminderNotification(context: Context, id: Long, title: String) {
        initNotificationChannels(context)

        // Intent to launch MainActivity when clicking notification
        val clickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id.toInt(),
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Aira Assistant Reminder")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(androidx.core.content.ContextCompat.getColor(context, R.color.aira_primary)) // Primary Blue

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(NOTIFICATION_ID_BASE_REMINDERS + id.toInt(), builder.build())
            Log.d("AiraNotificationMgr", "Reminder notification shown: $title")
        } catch (e: SecurityException) {
            Log.e("AiraNotificationMgr", "Permission missing to show notifications on Android 13+", e)
        }
    }

    /**
     * Instantly shows a local breaking news or intelligence notification.
     */
    fun showNewsAlertNotification(context: Context, title: String, content: String, category: String) {
        initNotificationChannels(context)

        val clickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ALERTS_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("[$category] $title")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(androidx.core.content.ContextCompat.getColor(context, R.color.aira_error)) // Error / Red Alert

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val uniqueId = System.currentTimeMillis().toInt()
            notificationManager.notify(NOTIFICATION_ID_BASE_ALERTS + uniqueId, builder.build())
            Log.d("AiraNotificationMgr", "News alert notification shown: $title")
        } catch (e: SecurityException) {
            Log.e("AiraNotificationMgr", "Permission missing to show notifications", e)
        }
    }

    /**
     * Parses custom time string label formats to milliseconds.
     */
    fun parseReminderTimeToMillis(timeLabel: String): Long {
        val trimmed = timeLabel.trim()
        val calendar = Calendar.getInstance()
        var hour = 9
        var minute = 0
        var isPm = false
        var isAm = false

        try {
            val lower = trimmed.lowercase()
            if (lower.contains("pm")) {
                isPm = true
            } else if (lower.contains("am")) {
                isAm = true
            }

            // Remove non-numeric or non-colon elements
            val cleanTime = trimmed.replace("(?i)am|pm".toRegex(), "").trim()
            val parts = cleanTime.split(":")
            if (parts.isNotEmpty()) {
                hour = parts[0].toIntOrNull() ?: 9
            }
            if (parts.size > 1) {
                minute = parts[1].toIntOrNull() ?: 0
            }

            if (isPm && hour < 12) {
                hour += 12
            } else if (isAm && hour == 12) {
                hour = 0
            }
        } catch (e: Exception) {
            Log.e("AiraNotificationMgr", "Error parsing time label: $timeLabel", e)
        }

        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1) // Schedule for tomorrow if time has already passed
        }

        return calendar.timeInMillis
    }
}
