package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("ReminderReceiver", "Broadcast received with action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED || 
            action == "android.intent.action.LOCKED_BOOT_COMPLETED" ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            
            // Reschedule all active reminders on system boot or package replacement
            val goAsync = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.d("ReminderReceiver", "Rescheduling active reminders after boot/upgrade...")
                    val db = AppDatabase.getDatabase(context)
                    val remindersList = db.reminderDao().getAllReminders().first()
                    
                    var rescheduledCount = 0
                    remindersList.forEach { reminder ->
                        if (!reminder.isCompleted) {
                            AiraNotificationManager.scheduleReminderAlarm(
                                context,
                                reminder.id,
                                reminder.title,
                                reminder.timeLabel
                            )
                            rescheduledCount++
                        }
                    }
                    Log.d("ReminderReceiver", "Successfully rescheduled $rescheduledCount reminders.")
                } catch (e: Exception) {
                    Log.e("ReminderReceiver", "Failed to reschedule reminders on boot", e)
                } finally {
                    goAsync.finish()
                }
            }
        } else if (action == "com.example.ACTION_TRIGGER_REMINDER") {
            val id = intent.getLongExtra("reminder_id", -1L)
            val title = intent.getStringExtra("reminder_title") ?: "Scheduled Aira Reminder"
            
            Log.d("ReminderReceiver", "Triggering reminder notification: id=$id, title=$title")
            if (id != -1L) {
                AiraNotificationManager.showReminderNotification(context, id, title)
            }
        }
    }
}
