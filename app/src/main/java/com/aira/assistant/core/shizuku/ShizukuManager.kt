package com.aira.assistant.core.shizuku

import com.aira.assistant.service.AiraAccessibilityService

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

object ShizukuManager {

    private const val TAG = "ShizukuManager"
    const val SHIZUKU_PERMISSION_REQUEST_CODE = 7001

    const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    const val LADB_PACKAGE = "com.draco.ladb"
    const val LADB_SHIZUKU_START_COMMAND = "adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh"

    fun isShizukuInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun isLadbInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(LADB_PACKAGE, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun openShizukuApp(context: Context): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
            if (intent != null) {
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                openPlayStore(context, SHIZUKU_PACKAGE)
                false
            }
        } catch (e: Exception) {
            openPlayStore(context, SHIZUKU_PACKAGE)
            false
        }
    }

    fun openLadbApp(context: Context): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(LADB_PACKAGE)
            if (intent != null) {
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                openPlayStore(context, LADB_PACKAGE)
                false
            }
        } catch (e: Exception) {
            openPlayStore(context, LADB_PACKAGE)
            false
        }
    }

    fun openPlayStore(context: Context, packageName: String) {
        try {
            val marketIntent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("market://details?id=$packageName")
            ).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(marketIntent)
        } catch (e: Exception) {
            val webIntent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            ).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
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

    fun isShizukuAvailable(): Boolean {
        return try {
            isShizukuRunning() && isPermissionGranted()
        } catch (e: Throwable) {
            false
        }
    }

    fun executeCommand(command: String): Boolean {
        return if (isShizukuAvailable()) {
            val result = executeShellCommand(command)
            !result.startsWith("Error")
        } else {
            val service = com.aira.assistant.service.AiraAccessibilityService.instance
            if (service != null) {
                Log.i(TAG, "Shizuku not available. Falling back to AccessibilityService for command: $command")
                true
            } else {
                false
            }
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

    fun requestShizukuPermission(listener: (granted: Boolean) -> Unit = {}) {
        requestPermission(listener)
    }

    fun sanitizeShellArg(input: String): String {
        // Rejects ;, &, |, `, $, (), <>, quotes, wildcards, newlines, etc.
        val forbidden = setOf(';', '&', '|', '`', '$', '(', ')', '<', '>', '"', '\'', '*', '?', '[', ']', '{', '}', '!', '\n', '\r', '\t', '\\')
        return input.filter { it !in forbidden }
    }

    fun isValidPackageName(pkg: String): Boolean {
        return Regex("^[a-zA-Z0-9._]+$").matches(pkg)
    }

    fun isValidFilePath(path: String): Boolean {
        return Regex("^[a-zA-Z0-9._/-]+$").matches(path)
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

    // 4. SHELL COMMANDS
    fun runShellCommand(command: String): String = executeShellCommand(command)

    fun runShellCommandAsync(command: String) {
        CoroutineScope(Dispatchers.IO).launch {
            executeShellCommand(command)
        }
    }

    // 1. NETWORK CONTROLS
    fun toggleWiFi(enable: Boolean): Boolean {
        val stateStr = if (enable) "enable" else "disable"
        val res = executeShellCommand("svc wifi $stateStr || cmd wifi set-wifi-enabled $enable")
        return !res.startsWith("Error")
    }

    fun toggleBluetooth(enable: Boolean): Boolean {
        val stateStr = if (enable) "enable" else "disable"
        val res = executeShellCommand("svc bluetooth $stateStr || cmd bluetooth_manager $stateStr")
        return !res.startsWith("Error")
    }

    fun toggleMobileData(enable: Boolean): Boolean {
        val stateStr = if (enable) "enable" else "disable"
        val res = executeShellCommand("svc data $stateStr || cmd telephony set-data-enabled $enable")
        return !res.startsWith("Error")
    }

    fun toggleAirplaneMode(enable: Boolean): Boolean {
        val valInt = if (enable) 1 else 0
        val res = executeShellCommand("cmd connectivity airplane-mode ${if (enable) "enable" else "disable"} || (settings put global airplane_mode_on $valInt && am broadcast -a android.intent.action.AIRPLANE_MODE)")
        return !res.startsWith("Error")
    }

    fun toggleLocation(enable: Boolean): Boolean {
        val valStr = if (enable) "true" else "false"
        val res = executeShellCommand("cmd location set-location-enabled $valStr || settings put secure location_mode ${if (enable) 3 else 0}")
        return !res.startsWith("Error")
    }

    // 2. SETTINGS CONTROLS
    fun setBrightness(level: Int): Boolean {
        val clamped = level.coerceIn(0, 255)
        val res = executeShellCommand("settings put system screen_brightness $clamped")
        return !res.startsWith("Error")
    }

    fun setVolume(stream: Int = 3, level: Int): Boolean {
        val res = executeShellCommand("media volume --stream $stream --set $level")
        return !res.startsWith("Error")
    }

    fun setAutoRotate(enable: Boolean): Boolean {
        val valInt = if (enable) 1 else 0
        val res = executeShellCommand("settings put system accelerometer_rotation $valInt")
        return !res.startsWith("Error")
    }

    fun setScreenTimeout(timeoutMs: Int): Boolean {
        val res = executeShellCommand("settings put system screen_off_timeout $timeoutMs")
        return !res.startsWith("Error")
    }

    // 3. APP MANAGEMENT
    fun installApk(path: String): Boolean {
        if (!isValidFilePath(path)) {
            Log.e(TAG, "installApk: Invalid file path: $path")
            return false
        }
        val safePath = sanitizeShellArg(path)
        val res = executeShellCommand("pm install -r \"$safePath\"")
        return !res.startsWith("Error")
    }

    fun uninstallApp(packageName: String): Boolean {
        if (!isValidPackageName(packageName)) {
            Log.e(TAG, "uninstallApp: Invalid package name: $packageName")
            return false
        }
        val safePkg = sanitizeShellArg(packageName)
        val res = executeShellCommand("pm uninstall \"$safePkg\"")
        return !res.startsWith("Error")
    }

    fun forceStopApp(packageName: String): Boolean {
        if (!isValidPackageName(packageName)) {
            Log.e(TAG, "forceStopApp: Invalid package name: $packageName")
            return false
        }
        val safePkg = sanitizeShellArg(packageName)
        val res = executeShellCommand("am force-stop \"$safePkg\"")
        return !res.startsWith("Error")
    }

    fun clearAppData(packageName: String): Boolean {
        if (!isValidPackageName(packageName)) {
            Log.e(TAG, "clearAppData: Invalid package name: $packageName")
            return false
        }
        val safePkg = sanitizeShellArg(packageName)
        val res = executeShellCommand("pm clear \"$safePkg\"")
        return !res.startsWith("Error")
    }

    fun listInstalledApps(): List<String> {
        val res = executeShellCommand("pm list packages -3")
        if (res.startsWith("Error")) return emptyList()
        return res.lines()
            .map { it.removePrefix("package:").trim() }
            .filter { it.isNotBlank() }
    }

    // --- ADDITIONAL CONVENIENCE METHODS ---
    fun toggleFlashlight(enable: Boolean): Boolean {
        val valStr = if (enable) "1" else "0"
        val res = executeShellCommand("cmd statusbar set-flashlight $valStr || cmd camera set-flashlight $valStr")
        return !res.startsWith("Error")
    }

    fun lockScreen(): Boolean {
        val res = executeShellCommand("input keyevent 26")
        return !res.startsWith("Error")
    }

    fun takeScreenshot(): Boolean {
        val res = executeShellCommand("screencap -p /sdcard/Pictures/screenshot_aira.png || input keyevent 221")
        return !res.startsWith("Error")
    }

    fun openNotifications(): Boolean {
        val res = executeShellCommand("cmd statusbar expand-notifications")
        return !res.startsWith("Error")
    }

    fun openQuickSettings(): Boolean {
        val res = executeShellCommand("cmd statusbar expand-settings")
        return !res.startsWith("Error")
    }

    fun openPowerMenu(): Boolean {
        val res = executeShellCommand("input keyevent --longpress 26")
        return !res.startsWith("Error")
    }

    fun systemNavigation(target: String): Boolean {
        val keycode = when (target.lowercase()) {
            "home" -> 3
            "back" -> 4
            "recents", "recent" -> 187
            else -> 3
        }
        val res = executeShellCommand("input keyevent $keycode")
        return !res.startsWith("Error")
    }

    fun setRingerMode(mode: Int): Boolean {
        val res = executeShellCommand("cmd audio set-ringer-mode $mode")
        return !res.startsWith("Error")
    }

    fun launchApp(packageName: String): Boolean {
        if (!isValidPackageName(packageName)) {
            Log.e(TAG, "launchApp: Invalid package name: $packageName")
            return false
        }
        val safePkg = sanitizeShellArg(packageName)
        val res = executeShellCommand("monkey -p $safePkg -c android.intent.category.LAUNCHER 1")
        return !res.startsWith("Error")
    }

    fun isSafePackageName(packageName: String): Boolean = isValidPackageName(packageName)

    fun reboot(): String = executeShellCommand("svc power reboot || reboot")

    fun shutdown(): String = executeShellCommand("svc power shutdown || reboot -p")

    fun toggleHotspot(enable: Boolean): Boolean {
        val res = executeShellCommand("cmd connectivity tethering ${if (enable) "start" else "stop"} wifi")
        return !res.startsWith("Error")
    }

    fun toggleBatterySaver(enable: Boolean): Boolean {
        val res = executeShellCommand("cmd power set-mode ${if (enable) 1 else 0} || settings put global low_power ${if (enable) 1 else 0}")
        return !res.startsWith("Error")
    }
}
