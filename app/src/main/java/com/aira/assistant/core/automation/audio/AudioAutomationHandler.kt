package com.aira.assistant.core.automation.audio

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.util.Log
import com.aira.assistant.core.shizuku.ShizukuManager

class AudioAutomationHandler(
    private val context: Context
) {
    private val tag = "AudioAutomationHandler"
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    fun setVolume(level: Int): Boolean {
        Log.d(tag, "setVolume: $level")
        if (ShizukuManager.isShizukuAvailable() && ShizukuManager.setVolume(3, level)) return true
        return try {
            val maxVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
            val target = (level.coerceIn(0, 100) * maxVol) / 100
            audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
            true
        } catch (e: Exception) {
            Log.e(tag, "setVolume failed", e)
            false
        }
    }

    fun volumeUp(): Boolean {
        Log.d(tag, "volumeUp")
        if (ShizukuManager.isShizukuAvailable() && ShizukuManager.executeCommand("input keyevent 24")) return true
        audioManager?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
        return true
    }

    fun volumeDown(): Boolean {
        Log.d(tag, "volumeDown")
        if (ShizukuManager.isShizukuAvailable() && ShizukuManager.executeCommand("input keyevent 25")) return true
        audioManager?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
        return true
    }

    fun muteVolume(): Boolean {
        Log.d(tag, "muteVolume")
        if (ShizukuManager.isShizukuAvailable() && ShizukuManager.executeCommand("cmd media_session volume --set 0")) return true
        audioManager?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
        return true
    }

    fun unmuteVolume(): Boolean {
        Log.d(tag, "unmuteVolume")
        if (ShizukuManager.isShizukuAvailable() && ShizukuManager.setVolume(3, 50)) return true
        audioManager?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_SHOW_UI)
        return true
    }

    fun setDnd(enable: Boolean): Boolean {
        Log.d(tag, "setDnd: $enable")
        if (ShizukuManager.isShizukuAvailable()) {
            val filter = if (enable) 1 else 0 // 1: PRIORITY / ALARMS, 0: ALL
            return ShizukuManager.executeCommand("cmd notification set_zen_mode $filter")
        }
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager?.isNotificationPolicyAccessGranted == true) {
                val targetMode = if (enable) NotificationManager.INTERRUPTION_FILTER_PRIORITY else NotificationManager.INTERRUPTION_FILTER_ALL
                notificationManager.setInterruptionFilter(targetMode)
                true
            } else false
        } catch (e: Exception) {
            Log.e(tag, "setDnd failed", e)
            false
        }
    }
}
