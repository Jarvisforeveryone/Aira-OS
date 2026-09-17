package com.aira.assistant.domain.automation

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.provider.Settings
import android.util.Log
import com.aira.assistant.service.AiraAccessibilityService
import com.aira.assistant.core.shizuku.ShizukuManager

/**
 * Handles device control: wifi, bluetooth, volume, brightness, rotation, flash, system settings.
 */
class DeviceControlHandler(
    private val context: Context,
    private val a11yProvider: () -> AiraAccessibilityService?
) {
    private val tag = "DeviceControlHandler"
    private val a11y: AiraAccessibilityService? get() = a11yProvider()
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    fun setWifi(enable: Boolean): Boolean {
        Log.d(tag, "setWifi: $enable")
        if (ShizukuManager.isShizukuAvailable() && ShizukuManager.toggleWiFi(enable)) return true
        val res = a11y?.toggleWifi(enable)
        return res != null && !res.startsWith("Failed") && !res.startsWith("Error")
    }

    fun setBluetooth(enable: Boolean): Boolean {
        Log.d(tag, "setBluetooth: $enable")
        if (ShizukuManager.isShizukuAvailable() && ShizukuManager.toggleBluetooth(enable)) return true
        val res = a11y?.toggleBluetooth(enable)
        return res != null && !res.startsWith("Failed") && !res.startsWith("Error")
    }

    fun setBrightness(percent: Int): Boolean {
        Log.d(tag, "setBrightness: $percent")
        val clamped = percent.coerceIn(0, 100)
        val value255 = (clamped * 255) / 100
        if (ShizukuManager.isShizukuAvailable() && ShizukuManager.setBrightness(value255)) return true
        return try {
            if (Settings.System.canWrite(context)) {
                Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, value255)
                true
            } else {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                false
            }
        } catch (e: Exception) {
            Log.e(tag, "setBrightness failed", e)
            false
        }
    }

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

    fun setFlashlight(enable: Boolean): Boolean {
        Log.d(tag, "setFlashlight: $enable")
        if (ShizukuManager.isShizukuAvailable() && ShizukuManager.toggleFlashlight(enable)) return true
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? android.hardware.camera2.CameraManager
            val cameraId = cameraManager?.cameraIdList?.firstOrNull()
            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, enable)
                true
            } else false
        } catch (e: Exception) {
            Log.e(tag, "setFlashlight failed", e)
            false
        }
    }

    fun setAutoRotation(enable: Boolean): Boolean {
        Log.d(tag, "setAutoRotation: $enable")
        if (ShizukuManager.isShizukuAvailable() && ShizukuManager.setAutoRotate(enable)) return true
        return try {
            if (Settings.System.canWrite(context)) {
                Settings.System.putInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, if (enable) 1 else 0)
                true
            } else false
        } catch (e: Exception) {
            Log.e(tag, "setAutoRotation failed", e)
            false
        }
    }

    fun setAirplaneMode(enable: Boolean): Boolean {
        Log.d(tag, "setAirplaneMode: $enable")
        if (ShizukuManager.isShizukuAvailable()) {
            return ShizukuManager.toggleAirplaneMode(enable)
        }
        val intent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return true
    }

    fun setGps(enable: Boolean): Boolean {
        Log.d(tag, "setGps: $enable")
        if (ShizukuManager.isShizukuAvailable()) {
            return ShizukuManager.toggleLocation(enable)
        }
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return true
    }

    fun setMobileData(enable: Boolean): Boolean {
        Log.d(tag, "setMobileData: $enable")
        if (ShizukuManager.isShizukuAvailable()) {
            return ShizukuManager.toggleMobileData(enable)
        }
        val intent = Intent(Settings.ACTION_DATA_ROAMING_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return true
    }

    fun reboot(): Boolean {
        Log.d(tag, "reboot")
        if (ShizukuManager.isShizukuAvailable() && ShizukuManager.executeCommand("reboot")) return true
        return false
    }

    fun shutdown(): Boolean {
        Log.d(tag, "shutdown")
        if (ShizukuManager.isShizukuAvailable() && ShizukuManager.executeCommand("reboot -p")) return true
        return false
    }

    fun lockScreen(): Boolean {
        Log.d(tag, "lockScreen")
        if (ShizukuManager.isShizukuAvailable() && ShizukuManager.lockScreen()) return true
        return a11y?.lockScreen() ?: false
    }

    fun unlockScreen(): Boolean {
        Log.d(tag, "unlockScreen")
        if (ShizukuManager.isShizukuAvailable() && ShizukuManager.executeCommand("input keyevent 82")) return true
        return a11y?.openQuickSettings() ?: false
    }

    fun getBatteryLevel(): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        return bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
    }
}
