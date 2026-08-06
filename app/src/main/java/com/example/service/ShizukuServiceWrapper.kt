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

    fun launchApp(packageName: String): ServiceTaskResult = executeTask("monkey -p $packageName -c android.intent.category.LAUNCHER 1")
}

sealed class ServiceTaskResult {
    data class Success(val output: String) : ServiceTaskResult()
    data class Error(val message: String) : ServiceTaskResult()
}
