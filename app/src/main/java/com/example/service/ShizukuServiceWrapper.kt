package com.example.service

import android.content.Context
import android.util.Log
import com.example.utils.ShizukuManager

/**
 * Service Wrapper for Shizuku API execution.
 * Allows AIRA to perform privileged system operations (Wi-Fi, Bluetooth, Screen Lock, Volume, Brightness)
 * safely without requiring the app to bind as the default system VoiceInteractionService (AssistantService).
 * This prevents background memory pressure and crash loops on 2GB RAM devices like Android Go.
 */
class ShizukuServiceWrapper(private val context: Context) {

    companion object {
        private const val TAG = "ShizukuServiceWrapper"

        @Volatile
        private var instance: ShizukuServiceWrapper? = null

        fun getInstance(context: Context): ShizukuServiceWrapper {
            return instance ?: synchronized(this) {
                instance ?: ShizukuServiceWrapper(context.applicationContext).also { instance = it }
            }
        }
    }

    fun isServiceAvailable(): Boolean {
        return ShizukuManager.isShizukuRunning() && ShizukuManager.isPermissionGranted()
    }

    fun requestPermission(onResult: (Boolean) -> Unit) {
        ShizukuManager.requestPermission(onResult)
    }

    fun executeTask(command: String): ServiceTaskResult {
        if (!isServiceAvailable()) {
            Log.w(TAG, "Shizuku service unavailable or permission missing.")
            return ServiceTaskResult.Error("Shizuku service unavailable or permission missing.")
        }

        return try {
            val output = ShizukuManager.executeShellCommand(command)
            if (output.startsWith("Error")) {
                ServiceTaskResult.Error(output)
            } else {
                ServiceTaskResult.Success(output)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to execute Shizuku task safely", e)
            ServiceTaskResult.Error(e.localizedMessage ?: "Execution error")
        }
    }

    fun toggleWiFi(enable: Boolean): ServiceTaskResult = executeTask("svc wifi ${if (enable) "enable" else "disable"} || cmd wifi set-wifi-enabled $enable")

    fun toggleBluetooth(enable: Boolean): ServiceTaskResult = executeTask("svc bluetooth ${if (enable) "enable" else "disable"} || cmd bluetooth_manager ${if (enable) "enable" else "disable"}")

    fun toggleMobileData(enable: Boolean): ServiceTaskResult = executeTask("svc data ${if (enable) "enable" else "disable"}")

    fun toggleHotspot(enable: Boolean): ServiceTaskResult = executeTask("cmd connectivity tethering ${if (enable) "start" else "stop"} wifi || cmd tethering ${if (enable) "start" else "stop"} wifi")

    fun toggleAirplaneMode(enable: Boolean): ServiceTaskResult = executeTask("settings put global airplane_mode_on ${if (enable) 1 else 0} && am broadcast -a android.intent.action.AIRPLANE_MODE --ez state ${if (enable) true else false}")

    fun toggleLocation(enable: Boolean): ServiceTaskResult = executeTask("settings put secure location_mode ${if (enable) 3 else 0}")

    fun toggleBatterySaver(enable: Boolean): ServiceTaskResult = executeTask("cmd power set-mode ${if (enable) 1 else 0} || settings put global low_power ${if (enable) 1 else 0}")

    fun toggleDoNotDisturb(enable: Boolean): ServiceTaskResult = executeTask("settings put global zen_mode ${if (enable) 1 else 0}")

    fun setScreenTimeout(seconds: Int): ServiceTaskResult {
        val ms = seconds * 1000
        return executeTask("settings put system screen_off_timeout $ms")
    }

    fun toggleScreenRotation(autoRotate: Boolean): ServiceTaskResult = executeTask("settings put system accelerometer_rotation ${if (autoRotate) 1 else 0}")

    fun setBrightness(percent: Int): ServiceTaskResult {
        val clampedPct = percent.coerceIn(0, 100)
        val value = (clampedPct * 255) / 100
        return executeTask("settings put system screen_brightness $value")
    }

    fun setVolume(streamType: Int = 3, percent: Int): ServiceTaskResult {
        val clampedPct = percent.coerceIn(0, 100)
        val volValue = (clampedPct * 15) / 100
        return executeTask("media volume --stream $streamType --set $volValue")
    }

    fun toggleFlashlight(enable: Boolean): ServiceTaskResult {
        val valStr = if (enable) "1" else "0"
        return executeTask("cmd statusbar set-flashlight $valStr || cmd camera set-flashlight $valStr")
    }

    fun lockScreen(): ServiceTaskResult = executeTask("input keyevent 26")

    fun takeScreenshot(): ServiceTaskResult = executeTask("screencap -p /sdcard/Pictures/screenshot_aira.png || input keyevent 221")

    fun openNotifications(): ServiceTaskResult = executeTask("cmd statusbar expand-notifications")

    fun openQuickSettings(): ServiceTaskResult = executeTask("cmd statusbar expand-settings")

    fun openPowerMenu(): ServiceTaskResult = executeTask("input keyevent --longpress 26")

    fun systemNavigation(target: String): ServiceTaskResult {
        val keycode = when (target.lowercase()) {
            "home" -> 3
            "back" -> 4
            "recents", "recent" -> 187
            else -> 3
        }
        return executeTask("input keyevent $keycode")
    }

    fun launchApp(packageName: String): ServiceTaskResult = executeTask("monkey -p $packageName -c android.intent.category.LAUNCHER 1")

    fun forceStopApp(packageName: String): ServiceTaskResult = executeTask("am force-stop $packageName")

    fun clearAppCache(packageName: String): ServiceTaskResult = executeTask("pm trim-caches 100M")

    fun uninstallApp(packageName: String): ServiceTaskResult = executeTask("pm uninstall $packageName")

    fun grantPermission(packageName: String, permission: String): ServiceTaskResult = executeTask("pm grant $packageName $permission")

    fun revokePermission(packageName: String, permission: String): ServiceTaskResult = executeTask("pm revoke $packageName $permission")

    fun reboot(): ServiceTaskResult = executeTask("svc power reboot || reboot")

    fun shutdown(): ServiceTaskResult = executeTask("svc power shutdown || reboot -p")

    fun inputText(text: String): ServiceTaskResult {
        val sanitized = text.replace(" ", "%s").replace("\"", "\\\"")
        return executeTask("input text \"$sanitized\"")
    }

    fun simulateTap(x: Int, y: Int): ServiceTaskResult = executeTask("input tap $x $y")
}

sealed class ServiceTaskResult {
    data class Success(val output: String) : ServiceTaskResult()
    data class Error(val message: String) : ServiceTaskResult()
}
