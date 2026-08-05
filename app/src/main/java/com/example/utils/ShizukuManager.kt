package com.example.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import android.util.Log
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

object ShizukuManager {

    private const val TAG = "ShizukuManager"
    const val SHIZUKU_PERMISSION_REQUEST_CODE = 7001

    fun isShizukuInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun isShizukuRunning(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Throwable) {
            false
        }
    }

    fun isPermissionGranted(): Boolean {
        return try {
            if (!isShizukuRunning()) return false
            if (Shizuku.isPreV11()) {
                false
            } else {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error checking Shizuku permission", e)
            false
        }
    }

    fun requestPermission(listener: (granted: Boolean) -> Unit) {
        if (!isShizukuRunning()) {
            listener(false)
            return
        }

        try {
            if (isPermissionGranted()) {
                listener(true)
                return
            }

            val permissionListener = object : Shizuku.OnRequestPermissionResultListener {
                override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                    if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE) {
                        Shizuku.removeRequestPermissionResultListener(this)
                        val granted = grantResult == PackageManager.PERMISSION_GRANTED
                        listener(granted)
                    }
                }
            }

            Shizuku.addRequestPermissionResultListener(permissionListener)
            Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
        } catch (e: Throwable) {
            Log.e(TAG, "Error requesting Shizuku permission", e)
            listener(false)
        }
    }

    fun executeShellCommand(cmd: String): String {
        if (!isShizukuRunning() || !isPermissionGranted()) {
            return "Error: Shizuku not running or permission denied."
        }

        return try {
            val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            newProcessMethod.isAccessible = true
            val processObj = newProcessMethod.invoke(null, arrayOf("sh", "-c", cmd), null, null)
            val process = processObj as java.lang.Process

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))

            val output = reader.readText()
            val error = errorReader.readText()
            process.waitFor()

            if (error.isNotBlank()) {
                "Output: $output\nError: $error"
            } else {
                output.ifBlank { "Executed successfully." }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Shell execution failed: $cmd", e)
            "Error executing command: ${e.localizedMessage}"
        }
    }

    fun toggleWiFi(enable: Boolean): String {
        val stateStr = if (enable) "enable" else "disable"
        val result = executeShellCommand("svc wifi $stateStr || cmd wifi set-wifi-enabled $enable")
        return "Shizuku: Wi-Fi set to $stateStr. $result"
    }

    fun toggleBluetooth(enable: Boolean): String {
        val stateStr = if (enable) "enable" else "disable"
        val result = executeShellCommand("svc bluetooth $stateStr || cmd bluetooth_manager $stateStr")
        return "Shizuku: Bluetooth set to $stateStr. $result"
    }

    fun setBrightness(percent: Int): String {
        val clampedPct = percent.coerceIn(0, 100)
        val value = (clampedPct * 255) / 100
        val result = executeShellCommand("settings put system screen_brightness $value")
        return "Shizuku: Screen brightness set to $clampedPct%. $result"
    }

    fun setVolume(streamType: Int = 3, percent: Int): String {
        val clampedPct = percent.coerceIn(0, 100)
        val maxVol = 15
        val volValue = (clampedPct * maxVol) / 100
        val result = executeShellCommand("media volume --stream $streamType --set $volValue")
        return "Shizuku: Volume set to $clampedPct%. $result"
    }

    fun toggleFlashlight(enable: Boolean): String {
        val valStr = if (enable) "1" else "0"
        val result = executeShellCommand("cmd statusbar set-flashlight $valStr || cmd camera set-flashlight $valStr")
        return "Shizuku: Flashlight set to ${if (enable) "ON" else "OFF"}. $result"
    }

    fun lockScreen(): String {
        val result = executeShellCommand("input keyevent 26")
        return "Shizuku: Lock screen executed. $result"
    }

    fun takeScreenshot(): String {
        val result = executeShellCommand("screencap -p /sdcard/Pictures/screenshot_aira.png || input keyevent 221")
        return "Shizuku: Screenshot captured. $result"
    }

    fun openNotifications(): String {
        val result = executeShellCommand("cmd statusbar expand-notifications")
        return "Shizuku: Notifications panel expanded. $result"
    }

    fun openQuickSettings(): String {
        val result = executeShellCommand("cmd statusbar expand-settings")
        return "Shizuku: Quick Settings shade expanded. $result"
    }

    fun openPowerMenu(): String {
        val result = executeShellCommand("input keyevent --longpress 26")
        return "Shizuku: Power menu displayed. $result"
    }

    fun systemNavigation(target: String): String {
        val keycode = when (target.lowercase()) {
            "home" -> 3
            "back" -> 4
            "recents", "recent" -> 187
            else -> 3
        }
        val result = executeShellCommand("input keyevent $keycode")
        return "Shizuku: System navigation '$target' executed. $result"
    }

    fun setRingerMode(mode: Int): String {
        // mode: 0 = SILENT, 1 = VIBRATE, 2 = NORMAL
        val result = executeShellCommand("cmd audio set-ringer-mode $mode")
        return "Shizuku: Ringer mode set to $mode. $result"
    }

    fun launchApp(packageName: String): String {
        val result = executeShellCommand("monkey -p $packageName -c android.intent.category.LAUNCHER 1")
        return "Shizuku: Launching $packageName. $result"
    }

    fun uninstallApp(packageName: String): String {
        val result = executeShellCommand("pm uninstall $packageName")
        return "Shizuku: Uninstalling $packageName. $result"
    }
}
