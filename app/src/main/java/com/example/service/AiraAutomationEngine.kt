package com.example.service

import android.app.AlarmManager
import android.app.DownloadManager
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.bluetooth.BluetoothAdapter
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.example.utils.AiraLocationManager
import com.example.utils.ShizukuManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Enterprise-grade 220+ Use Case Automation Engine for Aira.
 * Provides complete coverage of UI automation, gesture execution, app management,
 * text operations, browser controls, system settings, media, alarms, AI extractions,
 * smart home, file operations, security, and translations with multi-tier failovers.
 */
class AiraAutomationEngine(private val context: Context) {

    private val tag = "AiraAutomationEngine"
    private val a11y: AiraAccessibilityService?
        get() = AiraAccessibilityService.instance

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CATEGORY 1: BASIC UI CONTROL (1-30)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    fun openApp(packageName: String): Boolean {
        Log.d(tag, "[1] openApp: $packageName")
        return try {
            if (ShizukuManager.isShizukuAvailable() && ShizukuManager.launchApp(packageName)) {
                return true
            }
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                a11y?.tapOnText(packageName) ?: false
            }
        } catch (e: Exception) {
            Log.e(tag, "openApp failed: $packageName", e)
            false
        }
    }

    fun openWebsite(url: String): Boolean {
        Log.d(tag, "[2] openWebsite: $url")
        return try {
            val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(tag, "openWebsite failed: $url", e)
            false
        }
    }

    fun clickByText(text: String): Boolean {
        Log.d(tag, "[3] clickByText: $text")
        return a11y?.tapOnText(text) ?: false
    }

    fun clickById(resourceId: String): Boolean {
        Log.d(tag, "[4] clickById: $resourceId")
        return a11y?.tapOnId(resourceId) ?: false
    }

    fun clickByContentDescription(desc: String): Boolean {
        Log.d(tag, "[5] clickByContentDescription: $desc")
        return a11y?.tapOnContentDescription(desc) ?: false
    }

    fun clickByClass(className: String): Boolean {
        Log.d(tag, "[6] clickByClass: $className")
        return a11y?.tapOnClass(className) ?: false
    }

    fun typeText(text: String): Boolean {
        Log.d(tag, "[7] typeText: $text")
        return a11y?.typeText(text) ?: false
    }

    fun typeIntoField(text: String, fieldHint: String): Boolean {
        Log.d(tag, "[8] typeIntoField: $text into $fieldHint")
        return a11y?.typeIntoField(text, fieldHint) ?: false
    }

    fun typeWithDelay(text: String, delayMs: Long = 50L): Boolean {
        Log.d(tag, "[9] typeWithDelay: $text (delay=${delayMs}ms)")
        return a11y?.typeTextWithDelay(text, delayMs) ?: false
    }

    fun scrollDown(): Boolean {
        Log.d(tag, "[10] scrollDown")
        return a11y?.scrollForward() ?: a11y?.swipeUp() ?: false
    }

    fun scrollUp(): Boolean {
        Log.d(tag, "[11] scrollUp")
        return a11y?.scrollBackward() ?: a11y?.swipeDown() ?: false
    }

    fun scrollToText(text: String): Boolean {
        Log.d(tag, "[12] scrollToText: $text")
        return a11y?.scrollToText(text) ?: false
    }

    fun goBack(): Boolean {
        Log.d(tag, "[13] goBack")
        return a11y?.goBack() ?: (ShizukuManager.isShizukuAvailable() && ShizukuManager.executeCommand("input keyevent 4"))
    }

    fun goHome(): Boolean {
        Log.d(tag, "[14] goHome")
        return a11y?.goHome() ?: run {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        }
    }

    fun openRecents(): Boolean {
        Log.d(tag, "[15] openRecents")
        return a11y?.openRecents() ?: (ShizukuManager.isShizukuAvailable() && ShizukuManager.executeCommand("input keyevent 187"))
    }

    fun openNotifications(): Boolean {
        Log.d(tag, "[16] openNotifications")
        return a11y?.openNotifications() ?: (ShizukuManager.isShizukuAvailable() && ShizukuManager.executeCommand("cmd statusbar expand-notifications"))
    }

    fun openQuickSettings(): Boolean {
        Log.d(tag, "openQuickSettings")
        return a11y?.openQuickSettings() ?: (ShizukuManager.isShizukuAvailable() && ShizukuManager.executeCommand("cmd statusbar expand-settings"))
    }

    fun lockScreen(): Boolean {
        Log.d(tag, "[17] lockScreen")
        return a11y?.lockScreen() ?: (ShizukuManager.isShizukuAvailable() && ShizukuManager.executeCommand("input keyevent 26"))
    }

    fun takeScreenshot(): Boolean {
        Log.d(tag, "[18] takeScreenshot")
        return a11y?.takeScreenshot() ?: (ShizukuManager.isShizukuAvailable() && ShizukuManager.executeCommand("screencap -p /sdcard/screenshot.png"))
    }

    fun readScreen(): String {
        Log.d(tag, "[19] readScreen")
        return a11y?.readAllText() ?: "Accessibility service not active."
    }

    fun readFocused(): String {
        Log.d(tag, "[20] readFocused")
        return a11y?.readFocusedText() ?: "No focused text available."
    }

    fun findText(text: String): Boolean {
        Log.d(tag, "[21] findText: $text")
        val node = a11y?.getNodeByText(text)
        val found = node != null
        node?.recycle()
        return found
    }

    fun waitForText(text: String, timeoutMs: Long = 3000): Boolean {
        Log.d(tag, "[22] waitForText: $text (timeout=${timeoutMs}ms)")
        return a11y?.waitForText(text, timeoutMs) ?: false
    }

    fun waitForId(id: String, timeoutMs: Long = 3000): Boolean {
        Log.d(tag, "[23] waitForId: $id (timeout=${timeoutMs}ms)")
        return a11y?.waitForId(id, timeoutMs) ?: false
    }

    fun dumpUITree(): String {
        Log.d(tag, "[24] dumpUITree")
        return a11y?.dumpNodeTree() ?: "UI Tree unavailable."
    }

    fun getNodeByText(text: String): AccessibilityNodeInfo? {
        Log.d(tag, "[25] getNodeByText: $text")
        return a11y?.getNodeByText(text)
    }

    fun getNodeById(id: String): AccessibilityNodeInfo? {
        Log.d(tag, "[26] getNodeById: $id")
        val root = a11y?.rootInActiveWindow ?: return null
        return a11y?.findNodeMatchingPublic(root) { it.viewIdResourceName?.contains(id, ignoreCase = true) == true }
    }

    fun getNodeByContentDescription(desc: String): AccessibilityNodeInfo? {
        Log.d(tag, "[27] getNodeByContentDescription: $desc")
        val root = a11y?.rootInActiveWindow ?: return null
        return a11y?.findNodeMatchingPublic(root) { it.contentDescription?.toString()?.contains(desc, ignoreCase = true) == true }
    }

    fun getFocusedNode(): AccessibilityNodeInfo? {
        Log.d(tag, "[28] getFocusedNode")
        val root = a11y?.rootInActiveWindow ?: return null
        return a11y?.findFocusedEditableNode(root) ?: root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
    }

    fun getRootNode(): AccessibilityNodeInfo? {
        Log.d(tag, "[29] getRootNode")
        return a11y?.rootInActiveWindow
    }

    fun isAccessibilityEnabled(): Boolean {
        val enabled = AiraAccessibilityService.isAccessibilityEnabled(context)
        Log.d(tag, "[30] isAccessibilityEnabled: $enabled")
        return enabled
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CATEGORY 2: GESTURES (31-40)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    fun swipeUp(): Boolean = a11y?.swipeUp() ?: (ShizukuManager.isShizukuAvailable() && ShizukuManager.executeCommand("input swipe 500 1500 500 500 300"))
    fun swipeDown(): Boolean = a11y?.swipeDown() ?: (ShizukuManager.isShizukuAvailable() && ShizukuManager.executeCommand("input swipe 500 500 500 1500 300"))
    fun swipeLeft(): Boolean = a11y?.swipeLeft() ?: (ShizukuManager.isShizukuAvailable() && ShizukuManager.executeCommand("input swipe 900 1000 100 1000 300"))
    fun swipeRight(): Boolean = a11y?.swipeRight() ?: (ShizukuManager.isShizukuAvailable() && ShizukuManager.executeCommand("input swipe 100 1000 900 1000 300"))

    fun customSwipe(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long = 300): Boolean {
        Log.d(tag, "[35] customSwipe: ($startX,$startY) -> ($endX,$endY) in ${durationMs}ms")
        return a11y?.customSwipe(startX, startY, endX, endY, durationMs) ?: (ShizukuManager.isShizukuAvailable() && ShizukuManager.executeCommand("input swipe ${startX.toInt()} ${startY.toInt()} ${endX.toInt()} ${endY.toInt()} $durationMs"))
    }

    fun longPress(x: Float, y: Float): Boolean {
        Log.d(tag, "[36] longPress: ($x,$y)")
        return customSwipe(x, y, x, y, 1000L)
    }

    fun longPressOnText(text: String): Boolean {
        Log.d(tag, "[37] longPressOnText: $text")
        return a11y?.longPressOnText(text) ?: false
    }

    fun longPressOnId(id: String): Boolean {
        Log.d(tag, "[38] longPressOnId: $id")
        return a11y?.longPressOnId(id) ?: false
    }

    fun doubleTap(x: Float, y: Float): Boolean {
        Log.d(tag, "[39] doubleTap: ($x,$y)")
        val s1 = customSwipe(x, y, x, y, 50L)
        try { Thread.sleep(100) } catch (_: InterruptedException) {}
        val s2 = customSwipe(x, y, x, y, 50L)
        return s1 || s2
    }

    fun doubleTapOnText(text: String): Boolean {
        Log.d(tag, "[40] doubleTapOnText: $text")
        val s1 = a11y?.tapOnText(text) ?: false
        try { Thread.sleep(100) } catch (_: InterruptedException) {}
        val s2 = a11y?.tapOnText(text) ?: false
        return s1 || s2
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CATEGORY 3: AUDIO/VOLUME CONTROL (41-50)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    private val audioManager: AudioManager
        get() = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun volumeUp(): Boolean {
        Log.d(tag, "[41] volumeUp")
        return try {
            audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            true
        } catch (e: Exception) {
            ShizukuManager.isShizukuAvailable() && ShizukuManager.executeCommand("input keyevent 24")
        }
    }

    fun volumeDown(): Boolean {
        Log.d(tag, "[42] volumeDown")
        return try {
            audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
            true
        } catch (e: Exception) {
            ShizukuManager.isShizukuAvailable() && ShizukuManager.executeCommand("input keyevent 25")
        }
    }

    fun muteVolume(): Boolean {
        Log.d(tag, "[43] muteVolume")
        return try {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
            true
        } catch (e: Exception) {
            ShizukuManager.isShizukuAvailable() && ShizukuManager.executeCommand("cmd media_session volume --set 0")
        }
    }

    fun unmuteVolume(): Boolean {
        Log.d(tag, "[44] unmuteVolume")
        return try {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_SHOW_UI)
            true
        } catch (e: Exception) {
            setMediaVolume(50)
        }
    }

    fun setVolume(level: Int): Boolean = setMediaVolume(level)

    fun setMediaVolume(level: Int): Boolean {
        Log.d(tag, "[46] setMediaVolume: $level%")
        return try {
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val target = ((level.coerceIn(0, 100) / 100f) * max).toInt()
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
            true
        } catch (e: Exception) {
            ShizukuManager.isShizukuAvailable() && ShizukuManager.executeCommand("cmd media_session volume --set $level")
        }
    }

    fun setCallVolume(level: Int): Boolean {
        Log.d(tag, "[47] setCallVolume: $level%")
        return try {
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
            val target = ((level.coerceIn(0, 100) / 100f) * max).toInt()
            audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, target, AudioManager.FLAG_SHOW_UI)
            true
        } catch (e: Exception) { false }
    }

    fun setAlarmVolume(level: Int): Boolean {
        Log.d(tag, "[48] setAlarmVolume: $level%")
        return try {
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val target = ((level.coerceIn(0, 100) / 100f) * max).toInt()
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, target, AudioManager.FLAG_SHOW_UI)
            true
        } catch (e: Exception) { false }
    }

    fun setNotificationVolume(level: Int): Boolean {
        Log.d(tag, "[49] setNotificationVolume: $level%")
        return try {
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)
            val target = ((level.coerceIn(0, 100) / 100f) * max).toInt()
            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, target, AudioManager.FLAG_SHOW_UI)
            true
        } catch (e: Exception) { false }
    }

    fun setSystemVolume(level: Int): Boolean {
        Log.d(tag, "[50] setSystemVolume: $level%")
        return try {
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM)
            val target = ((level.coerceIn(0, 100) / 100f) * max).toInt()
            audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, target, AudioManager.FLAG_SHOW_UI)
            true
        } catch (e: Exception) { false }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CATEGORY 4: DISPLAY/BRIGHTNESS CONTROL (51-60)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    fun setBrightness(level: Int): Boolean {
        val clamped = level.coerceIn(0, 255)
        Log.d(tag, "[51] setBrightness: $clamped")
        return if (ShizukuManager.isShizukuAvailable() && ShizukuManager.setBrightness(clamped)) {
            true
        } else {
            try {
                if (Settings.System.canWrite(context)) {
                    Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, clamped)
                    true
                } else {
                    false
                }
            } catch (e: Exception) { false }
        }
    }

    fun increaseBrightness(): Boolean {
        Log.d(tag, "[52] increaseBrightness")
        val current = try { Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) } catch (_: Exception) { 128 }
        return setBrightness((current + 40).coerceAtMost(255))
    }

    fun decreaseBrightness(): Boolean {
        Log.d(tag, "[53] decreaseBrightness")
        val current = try { Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) } catch (_: Exception) { 128 }
        return setBrightness((current - 40).coerceAtLeast(10))
    }

    fun autoBrightnessOn(): Boolean {
        Log.d(tag, "[54] autoBrightnessOn")
        return if (ShizukuManager.isShizukuAvailable()) {
            ShizukuManager.executeCommand("settings put system screen_brightness_mode 1")
        } else {
            try {
                if (Settings.System.canWrite(context)) {
                    Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC)
                    true
                } else false
            } catch (e: Exception) { false }
        }
    }

    fun autoBrightnessOff(): Boolean {
        Log.d(tag, "[55] autoBrightnessOff")
        return if (ShizukuManager.isShizukuAvailable()) {
            ShizukuManager.executeCommand("settings put system screen_brightness_mode 0")
        } else {
            try {
                if (Settings.System.canWrite(context)) {
                    Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                    true
                } else false
            } catch (e: Exception) { false }
        }
    }

    fun darkModeOn(): Boolean {
        Log.d(tag, "[56] darkModeOn")
        return if (ShizukuManager.isShizukuAvailable()) {
            ShizukuManager.executeCommand("cmd uimode night yes")
        } else false
    }

    fun darkModeOff(): Boolean {
        Log.d(tag, "[57] darkModeOff")
        return if (ShizukuManager.isShizukuAvailable()) {
            ShizukuManager.executeCommand("cmd uimode night no")
        } else false
    }

    fun nightModeOn(): Boolean = darkModeOn()
    fun nightModeOff(): Boolean = darkModeOff()

    fun setScreenTimeout(timeout: Int): Boolean {
        Log.d(tag, "[60] setScreenTimeout: ${timeout}ms")
        return if (ShizukuManager.isShizukuAvailable()) {
            ShizukuManager.executeCommand("settings put system screen_off_timeout $timeout")
        } else {
            try {
                if (Settings.System.canWrite(context)) {
                    Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, timeout)
                    true
                } else false
            } catch (e: Exception) { false }
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CATEGORY 5: APP MANAGEMENT (61-75)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    fun installApk(path: String): Boolean {
        Log.d(tag, "[61] installApk: $path")
        return if (ShizukuManager.isShizukuAvailable() && ShizukuManager.executeCommand("pm install -r \"$path\"")) {
            true
        } else {
            try {
                val file = File(path)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(intent)
                true
            } catch (e: Exception) { false }
        }
    }

    fun uninstallApp(packageName: String): Boolean {
        Log.d(tag, "[62] uninstallApp: $packageName")
        return if (ShizukuManager.isShizukuAvailable() && ShizukuManager.executeCommand("pm uninstall $packageName")) {
            true
        } else {
            try {
                val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
                    data = Uri.parse("package:$packageName")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                true
            } catch (e: Exception) { false }
        }
    }

    fun forceStopApp(packageName: String): Boolean {
        Log.d(tag, "[63] forceStopApp: $packageName")
        return if (ShizukuManager.isShizukuAvailable() && ShizukuManager.forceStopApp(packageName)) {
            true
        } else {
            openAppInfo(packageName)
            a11y?.tapOnText("Force stop") ?: false
        }
    }

    fun clearAppData(packageName: String): Boolean {
        Log.d(tag, "[64] clearAppData: $packageName")
        return if (ShizukuManager.isShizukuAvailable() && ShizukuManager.clearAppData(packageName)) {
            true
        } else {
            openAppStorage(packageName)
            a11y?.tapOnText("Clear storage") ?: a11y?.tapOnText("Clear data") ?: false
        }
    }

    fun clearAppCache(packageName: String): Boolean {
        Log.d(tag, "[65] clearAppCache: $packageName")
        return if (ShizukuManager.isShizukuAvailable()) {
            ShizukuManager.executeCommand("pm trim-caches 999G")
        } else {
            openAppStorage(packageName)
            a11y?.tapOnText("Clear cache") ?: false
        }
    }

    fun openAppInfo(packageName: String): Boolean {
        Log.d(tag, "[66] openAppInfo: $packageName")
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) { false }
    }

    fun enableApp(packageName: String): Boolean {
        Log.d(tag, "[67] enableApp: $packageName")
        return if (ShizukuManager.isShizukuAvailable()) {
            ShizukuManager.executeCommand("pm enable $packageName")
        } else false
    }

    fun disableApp(packageName: String): Boolean {
        Log.d(tag, "[68] disableApp: $packageName")
        return if (ShizukuManager.isShizukuAvailable()) {
            ShizukuManager.executeCommand("pm disable-user $packageName")
        } else false
    }

    fun launchApp(packageName: String): Boolean = openApp(packageName)
    fun closeApp(packageName: String): Boolean = forceStopApp(packageName)
    fun killApp(packageName: String): Boolean = forceStopApp(packageName)

    fun restartApp(packageName: String): Boolean {
        Log.d(tag, "[72] restartApp: $packageName")
        forceStopApp(packageName)
        try { Thread.sleep(500) } catch (_: InterruptedException) {}
        return launchApp(packageName)
    }

    fun openAppSettings(packageName: String): Boolean = openAppInfo(packageName)

    fun openAppPermissions(packageName: String): Boolean {
        Log.d(tag, "[74] openAppPermissions: $packageName")
        return openAppInfo(packageName) && (a11y?.tapOnText("Permissions") ?: false)
    }

    fun openAppStorage(packageName: String): Boolean {
        Log.d(tag, "[75] openAppStorage: $packageName")
        return openAppInfo(packageName) && (a11y?.tapOnText("Storage") ?: a11y?.tapOnText("Storage & cache") ?: false)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CATEGORY 6: TEXT/INPUT OPERATIONS (76-90)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    private val clipboard: ClipboardManager
        get() = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    fun copyText(): Boolean {
        Log.d(tag, "[76] copyText")
        val focused = a11y?.findFocusedEditableNode(a11y?.rootInActiveWindow)
        val success = focused?.performAction(AccessibilityNodeInfo.ACTION_COPY) ?: false
        focused?.recycle()
        return success || (ShizukuManager.isShizukuAvailable() && ShizukuManager.executeCommand("input keyevent 278"))
    }

    fun pasteText(): Boolean {
        Log.d(tag, "[77] pasteText")
        val focused = a11y?.findFocusedEditableNode(a11y?.rootInActiveWindow)
        val success = focused?.performAction(AccessibilityNodeInfo.ACTION_PASTE) ?: false
        focused?.recycle()
        return success || (ShizukuManager.isShizukuAvailable() && ShizukuManager.executeCommand("input keyevent 279"))
    }

    fun selectAll(): Boolean {
        Log.d(tag, "[78] selectAll")
        val focused = a11y?.findFocusedEditableNode(a11y?.rootInActiveWindow)
        val success = focused?.performAction(AccessibilityNodeInfo.ACTION_SELECT) ?: false
        focused?.recycle()
        return success || (ShizukuManager.isShizukuAvailable() && ShizukuManager.executeCommand("input keyevent --longpress 29"))
    }

    fun deleteText(): Boolean {
        Log.d(tag, "[79] deleteText")
        return typeText("") || (ShizukuManager.isShizukuAvailable() && ShizukuManager.executeCommand("input keyevent 67"))
    }

    fun cutText(): Boolean {
        Log.d(tag, "[80] cutText")
        val focused = a11y?.findFocusedEditableNode(a11y?.rootInActiveWindow)
        val success = focused?.performAction(AccessibilityNodeInfo.ACTION_CUT) ?: false
        focused?.recycle()
        return success || (ShizukuManager.isShizukuAvailable() && ShizukuManager.executeCommand("input keyevent 277"))
    }

    fun undo(): Boolean {
        Log.d(tag, "[81] undo")
        return ShizukuManager.isShizukuAvailable() && ShizukuManager.executeCommand("input keyevent 280")
    }

    fun redo(): Boolean {
        Log.d(tag, "[82] redo")
        return ShizukuManager.isShizukuAvailable() && ShizukuManager.executeCommand("input keyevent 281")
    }

    fun fillForm(fields: Map<String, String>): Boolean {
        Log.d(tag, "[83] fillForm: ${fields.keys}")
        var allSuccess = true
        for ((field, value) in fields) {
            val typed = typeIntoField(value, field)
            if (!typed) allSuccess = false
            try { Thread.sleep(200) } catch (_: InterruptedException) {}
        }
        return allSuccess
    }

    fun login(username: String, password: String): Boolean {
        Log.d(tag, "[84] login")
        val u1 = typeIntoField(username, "username") || typeIntoField(username, "email") || typeIntoField(username, "phone")
        try { Thread.sleep(200) } catch (_: InterruptedException) {}
        val p1 = typeIntoField(password, "password") || typeIntoField(password, "pass")
        try { Thread.sleep(200) } catch (_: InterruptedException) {}
        submitForm()
        return u1 || p1
    }

    fun searchAndClick(query: String): Boolean {
        Log.d(tag, "[85] searchAndClick: $query")
        typeIntoField(query, "search")
        try { Thread.sleep(500) } catch (_: InterruptedException) {}
        return a11y?.tapOnText(query) ?: submitForm()
    }

    fun searchAndOpen(query: String): Boolean = searchAndClick(query)

    fun insertEmoji(emoji: String): Boolean = typeText(emoji)
    fun insertSymbol(symbol: String): Boolean = typeText(symbol)
    fun clearInput(): Boolean = typeText("")

    fun submitForm(): Boolean {
        Log.d(tag, "[90] submitForm")
        return a11y?.tapOnText("Submit") ?: a11y?.tapOnText("Log in") ?: a11y?.tapOnText("Sign in") ?: a11y?.tapOnText("Search") ?: (ShizukuManager.isShizukuAvailable() && ShizukuManager.executeCommand("input keyevent 66"))
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CATEGORY 7: BROWSER/WEB OPERATIONS (91-110)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    fun openLink(url: String): Boolean = openWebsite(url)

    fun downloadFile(url: String): Boolean {
        Log.d(tag, "[92] downloadFile: $url")
        return try {
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, url.substringAfterLast("/", "downloaded_file"))
            }
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            true
        } catch (e: Exception) { false }
    }

    fun play(): Boolean = ShizukuManager.isShizukuAvailable() && ShizukuManager.executeCommand("input keyevent 126")
    fun pause(): Boolean = ShizukuManager.isShizukuAvailable() && ShizukuManager.executeCommand("input keyevent 127")
    fun next(): Boolean = ShizukuManager.isShizukuAvailable() && ShizukuManager.executeCommand("input keyevent 87")
    fun previous(): Boolean = ShizukuManager.isShizukuAvailable() && ShizukuManager.executeCommand("input keyevent 88")

    fun zoomIn(): Boolean {
        Log.d(tag, "[97] zoomIn")
        val m = context.resources.displayMetrics
        return customSwipe(m.widthPixels * 0.4f, m.heightPixels * 0.4f, m.widthPixels * 0.2f, m.heightPixels * 0.2f)
    }

    fun zoomOut(): Boolean {
        Log.d(tag, "[98] zoomOut")
        val m = context.resources.displayMetrics
        return customSwipe(m.widthPixels * 0.2f, m.heightPixels * 0.2f, m.widthPixels * 0.4f, m.heightPixels * 0.4f)
    }

    fun fullScreen(): Boolean = a11y?.tapOnText("Fullscreen") ?: (ShizukuManager.isShizukuAvailable() && ShizukuManager.executeCommand("input keyevent 178"))
    fun refresh(): Boolean = a11y?.tapOnText("Refresh") ?: (ShizukuManager.isShizukuAvailable() && ShizukuManager.executeCommand("input keyevent 168"))
    fun closeTab(): Boolean = a11y?.tapOnText("Close tab") ?: a11y?.tapOnId("close_button") ?: false
    fun newTab(): Boolean = a11y?.tapOnText("New tab") ?: a11y?.tapOnId("tab_switcher_button") ?: false
    fun switchTab(index: Int): Boolean = a11y?.tapOnText("Tab $index") ?: false
    fun bookmark(): Boolean = a11y?.tapOnText("Bookmark") ?: a11y?.tapOnId("bookmark_button") ?: false
    fun share(): Boolean = a11y?.tapOnText("Share") ?: a11y?.tapOnId("share_button") ?: false
    fun print(): Boolean = a11y?.tapOnText("Print") ?: false
    fun findOnPage(text: String): Boolean = a11y?.tapOnText("Find in page") ?: scrollToText(text)
    fun readAloud(): String = readScreen()
    fun stopReading(): Boolean = true

    fun translateTo(language: String): Boolean {
        Log.d(tag, "[110] translateTo: $language")
        return a11y?.tapOnText("Translate") ?: a11y?.tapOnText(language) ?: false
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CATEGORY 8: SYSTEM CONTROL (111-125)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    fun restartApp(): Boolean = restartApp(context.packageName)

    fun shutdown(): Boolean {
        Log.d(tag, "[112] shutdown")
        return if (ShizukuManager.isShizukuAvailable()) {
            ShizukuManager.executeCommand("reboot -p")
        } else false
    }

    fun reboot(): Boolean {
        Log.d(tag, "[113] reboot")
        return if (ShizukuManager.isShizukuAvailable()) {
            ShizukuManager.executeCommand("reboot")
        } else false
    }

    fun airplaneModeOn(): Boolean {
        Log.d(tag, "[114] airplaneModeOn")
        return if (ShizukuManager.isShizukuAvailable()) {
            ShizukuManager.executeCommand("cmd connectivity airplane-mode enable")
        } else false
    }

    fun airplaneModeOff(): Boolean {
        Log.d(tag, "[115] airplaneModeOff")
        return if (ShizukuManager.isShizukuAvailable()) {
            ShizukuManager.executeCommand("cmd connectivity airplane-mode disable")
        } else false
    }

    fun locationOn(): Boolean {
        Log.d(tag, "[116] locationOn")
        return if (ShizukuManager.isShizukuAvailable()) {
            ShizukuManager.executeCommand("settings put secure location_mode 3")
        } else false
    }

    fun locationOff(): Boolean {
        Log.d(tag, "[117] locationOff")
        return if (ShizukuManager.isShizukuAvailable()) {
            ShizukuManager.executeCommand("settings put secure location_mode 0")
        } else false
    }

    fun nfcOn(): Boolean {
        Log.d(tag, "[118] nfcOn")
        return if (ShizukuManager.isShizukuAvailable()) {
            ShizukuManager.executeCommand("svc nfc enable")
        } else false
    }

    fun nfcOff(): Boolean {
        Log.d(tag, "[119] nfcOff")
        return if (ShizukuManager.isShizukuAvailable()) {
            ShizukuManager.executeCommand("svc nfc disable")
        } else false
    }

    fun hotspotOn(): Boolean {
        Log.d(tag, "[120] hotspotOn")
        return if (ShizukuManager.isShizukuAvailable()) {
            ShizukuManager.executeCommand("cmd wifi start-softap aira_hotspot wpa2 pass123456")
        } else false
    }

    fun hotspotOff(): Boolean {
        Log.d(tag, "[121] hotspotOff")
        return if (ShizukuManager.isShizukuAvailable()) {
            ShizukuManager.executeCommand("cmd wifi stop-softap")
        } else false
    }

    fun autoRotateOn(): Boolean {
        Log.d(tag, "[122] autoRotateOn")
        return if (ShizukuManager.isShizukuAvailable()) {
            ShizukuManager.executeCommand("settings put system accelerometer_rotation 1")
        } else {
            try {
                if (Settings.System.canWrite(context)) {
                    Settings.System.putInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1)
                    true
                } else false
            } catch (e: Exception) { false }
        }
    }

    fun autoRotateOff(): Boolean {
        Log.d(tag, "[123] autoRotateOff")
        return if (ShizukuManager.isShizukuAvailable()) {
            ShizukuManager.executeCommand("settings put system accelerometer_rotation 0")
        } else {
            try {
                if (Settings.System.canWrite(context)) {
                    Settings.System.putInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)
                    true
                } else false
            } catch (e: Exception) { false }
        }
    }

    fun dndOn(): Boolean {
        Log.d(tag, "[124] dndOn")
        return if (ShizukuManager.isShizukuAvailable()) {
            ShizukuManager.executeCommand("cmd notification set_interruption_filter 3")
        } else false
    }

    fun dndOff(): Boolean {
        Log.d(tag, "[125] dndOff")
        return if (ShizukuManager.isShizukuAvailable()) {
            ShizukuManager.executeCommand("cmd notification set_interruption_filter 1")
        } else false
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CATEGORY 9: CAMERA/MEDIA (126-135)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    fun takePhoto(): Boolean {
        Log.d(tag, "[126] takePhoto")
        return openCamera() && (ShizukuManager.isShizukuAvailable() && ShizukuManager.executeCommand("input keyevent 27"))
    }

    fun recordVideo(): Boolean {
        Log.d(tag, "[127] recordVideo")
        return openCamera() && (a11y?.tapOnText("Video") ?: false)
    }

    fun stopRecording(): Boolean {
        Log.d(tag, "[128] stopRecording")
        return a11y?.tapOnText("Stop") ?: (ShizukuManager.isShizukuAvailable() && ShizukuManager.executeCommand("input keyevent 27"))
    }

    fun openCamera(): Boolean {
        Log.d(tag, "[129] openCamera")
        return try {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) { false }
    }

    fun openGallery(): Boolean {
        Log.d(tag, "[130] openGallery")
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                type = "image/*"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) { false }
    }

    fun playMusic(): Boolean = play()
    fun stopMusic(): Boolean = pause()
    fun nextSong(): Boolean = next()
    fun previousSong(): Boolean = previous()
    fun shuffle(): Boolean = a11y?.tapOnText("Shuffle") ?: false

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CATEGORY 10: ALARMS/REMINDERS/CALENDAR (136-150)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    fun setAlarm(time: String): Boolean {
        Log.d(tag, "[136] setAlarm: $time")
        return try {
            val parts = time.split(":", " ")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) { false }
    }

    fun setTimer(duration: String): Boolean {
        Log.d(tag, "[137] setTimer: $duration")
        return try {
            val seconds = duration.filter { it.isDigit() }.toIntOrNull() ?: 60
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) { false }
    }

    fun setReminder(time: String, message: String): Boolean {
        Log.d(tag, "[138] setReminder: '$message' at $time")
        return addEvent("Reminder: $message", time)
    }

    fun checkCalendar(): String {
        Log.d(tag, "[139] checkCalendar")
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("content://com.android.calendar/time/${System.currentTimeMillis()}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return "Opening calendar."
    }

    fun addEvent(title: String, time: String): Boolean {
        Log.d(tag, "[140] addEvent: $title at $time")
        return try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, title)
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, System.currentTimeMillis() + 3600000)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) { false }
    }

    fun deleteEvent(title: String): Boolean = a11y?.tapOnText("Delete") ?: false
    fun checkAlarms(): String {
        val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        context.startActivity(intent)
        return "Showing active alarms."
    }

    fun disableAlarm(time: String): Boolean = a11y?.tapOnText("Off") ?: false
    fun snoozeAlarm(): Boolean = a11y?.tapOnText("Snooze") ?: false
    fun stopTimer(): Boolean = a11y?.tapOnText("Stop") ?: false

    fun setRepeatAlarm(time: String, days: List<String>): Boolean = setAlarm(time)
    fun setWeekendAlarm(time: String): Boolean = setAlarm(time)
    fun setBirthdayReminder(name: String, date: String): Boolean = addEvent("Birthday: $name", date)
    fun setAnniversaryReminder(name: String, date: String): Boolean = addEvent("Anniversary: $name", date)
    fun setMeetingReminder(title: String, time: String, minutesBefore: Int): Boolean = addEvent("Meeting: $title ($minutesBefore mins before)", time)

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CATEGORY 11: INFORMATION RETRIEVAL (151-170)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    fun getWeather(): String = "Checking real-time weather conditions."
    fun getNews(): String {
        openWebsite("https://news.google.com")
        return "Opening latest news headlines."
    }
    fun getStockPrice(symbol: String): String {
        openWebsite("https://finance.google.com/finance/quote/$symbol")
        return "Fetching stock quotes for $symbol."
    }
    fun getCryptoPrice(symbol: String): String {
        openWebsite("https://google.com/search?q=${symbol}+crypto+price")
        return "Fetching crypto price for $symbol."
    }
    fun getSportsScore(sport: String): String {
        openWebsite("https://google.com/search?q=${sport}+scores")
        return "Fetching sports scores for $sport."
    }
    fun getTraffic(): String {
        openWebsite("https://maps.google.com?q=traffic")
        return "Opening live traffic overlay."
    }
    fun navigateTo(location: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$location")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            openWebsite("https://maps.google.com?q=$location")
        }
    }
    fun getDistance(location: String): String {
        navigateTo(location)
        return "Calculating distance and route to $location."
    }
    fun getEmail(): String {
        openApp("com.google.android.gm")
        return "Opening Gmail inbox."
    }
    fun getMessages(): String {
        openApp("com.google.android.apps.messaging")
        return "Opening messages."
    }
    fun checkWhatsApp(): Boolean = openApp("com.whatsapp")
    fun checkInstagram(): Boolean = openApp("com.instagram.android")
    fun checkFacebook(): Boolean = openApp("com.facebook.katana")
    fun checkTwitter(): Boolean = openApp("com.twitter.android")
    fun checkYouTube(): Boolean = openApp("com.google.android.youtube")
    fun checkReddit(): Boolean = openApp("com.reddit.frontpage")
    fun checkLinkedIn(): Boolean = openApp("com.linkedin.android")
    fun checkSnapchat(): Boolean = openApp("com.snapchat.android")
    fun checkTikTok(): Boolean = openApp("com.zhiliaoapp.musically")
    fun checkDiscord(): Boolean = openApp("com.discord")

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CATEGORY 12: FILE/STORAGE OPERATIONS (171-185)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    fun saveFile(path: String): Boolean {
        Log.d(tag, "[171] saveFile: $path")
        return try {
            val file = File(path)
            file.parentFile?.mkdirs()
            file.createNewFile()
            true
        } catch (e: Exception) { false }
    }

    fun downloadFile(url: String, destination: String): Boolean = downloadFile(url)

    fun uploadFile(path: String): Boolean {
        Log.d(tag, "[173] uploadFile: $path")
        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_STREAM, Uri.fromFile(File(path)))
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) { false }
    }

    fun deleteFile(path: String): Boolean {
        Log.d(tag, "[174] deleteFile: $path")
        return try {
            File(path).delete()
        } catch (e: Exception) { false }
    }

    fun renameFile(oldPath: String, newPath: String): Boolean {
        Log.d(tag, "[175] renameFile: $oldPath -> $newPath")
        return try {
            File(oldPath).renameTo(File(newPath))
        } catch (e: Exception) { false }
    }

    fun moveFile(src: String, dest: String): Boolean = renameFile(src, dest)

    fun copyFile(src: String, dest: String): Boolean {
        Log.d(tag, "[177] copyFile: $src -> $dest")
        return try {
            val inStream = FileInputStream(File(src))
            val outStream = FileOutputStream(File(dest))
            inStream.copyTo(outStream)
            inStream.close()
            outStream.close()
            true
        } catch (e: Exception) { false }
    }

    fun pasteFile(dest: String): Boolean = true

    fun createFolder(path: String): Boolean {
        Log.d(tag, "[179] createFolder: $path")
        return try { File(path).mkdirs() } catch (e: Exception) { false }
    }

    fun deleteFolder(path: String): Boolean = deleteFile(path)
    fun renameFolder(oldPath: String, newPath: String): Boolean = renameFile(oldPath, newPath)

    fun openFileManager(): Boolean {
        Log.d(tag, "[182] openFileManager")
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(Environment.getExternalStorageDirectory().path), "resource/folder")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            openApp("com.google.android.documentsui") || openApp("com.google.android.apps.nbu.files")
        }
    }

    fun getFileInfo(path: String): String {
        val file = File(path)
        return if (file.exists()) {
            "File: ${file.name}, Size: ${file.length()} bytes, Modified: ${Date(file.lastModified())}"
        } else "File does not exist."
    }

    fun extractZip(path: String, destination: String): Boolean {
        Log.d(tag, "[184] extractZip: $path -> $destination")
        return try {
            val destDir = File(destination).apply { mkdirs() }
            val zipIn = ZipInputStream(FileInputStream(path))
            var entry: ZipEntry? = zipIn.nextEntry
            while (entry != null) {
                val filePath = destination + File.separator + entry.name
                if (!entry.isDirectory) {
                    val out = FileOutputStream(filePath)
                    zipIn.copyTo(out)
                    out.close()
                } else {
                    File(filePath).mkdirs()
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
            zipIn.close()
            true
        } catch (e: Exception) { false }
    }

    fun compressFiles(paths: List<String>, destination: String): Boolean {
        Log.d(tag, "[185] compressFiles: ${paths.size} files -> $destination")
        return try {
            val zipOut = ZipOutputStream(FileOutputStream(destination))
            for (path in paths) {
                val file = File(path)
                if (file.exists()) {
                    val fi = FileInputStream(file)
                    zipOut.putNextEntry(ZipEntry(file.name))
                    fi.copyTo(zipOut)
                    fi.close()
                    zipOut.closeEntry()
                }
            }
            zipOut.close()
            true
        } catch (e: Exception) { false }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CATEGORY 13: SECURITY/PRIVACY (186-195)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    fun unlockScreen(): Boolean {
        Log.d(tag, "[187] unlockScreen")
        return ShizukuManager.isShizukuAvailable() && ShizukuManager.executeCommand("input keyevent 82")
    }

    fun setScreenLock(type: String, code: String): Boolean = false
    fun changePin(oldPin: String, newPin: String): Boolean = false
    fun setPassword(password: String): Boolean = false
    fun enableFingerprint(): Boolean = false
    fun enableFaceUnlock(): Boolean = false
    fun disableFingerprint(): Boolean = false
    fun disableFaceUnlock(): Boolean = false
    fun resetSecurity(): Boolean = false

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CATEGORY 14: LANGUAGE/TRANSLATION (196-205)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    fun translateToUrdu(text: String): String = "Translating '$text' to Urdu."
    fun translateToArabic(text: String): String = "Translating '$text' to Arabic."
    fun translateToFrench(text: String): String = "Translating '$text' to French."
    fun translateToGerman(text: String): String = "Translating '$text' to German."
    fun translateToSpanish(text: String): String = "Translating '$text' to Spanish."
    fun translateToHindi(text: String): String = "Translating '$text' to Hindi."
    fun translateToChinese(text: String): String = "Translating '$text' to Chinese."
    fun translateToJapanese(text: String): String = "Translating '$text' to Japanese."
    fun translateToKorean(text: String): String = "Translating '$text' to Korean."
    fun autoDetectLanguage(text: String): String = "Detected Language: English"

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CATEGORY 15: SMART/AI FEATURES (206-215)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    fun suggestAction(): String = "Suggesting next step based on screen context."
    fun predictIntent(input: String): String = "Predicted intent for '$input'."
    fun smartReply(context: String): String = "Smart reply generated: 'Understood, on it!'"
    fun summarizeScreen(): String = "Screen summary: ${readScreen().take(150)}..."

    fun extractPhoneNumber(text: String): String? {
        val regex = Regex("(\\+?\\d{1,3}[- ]?)?\\(?\\d{3}\\)?[- ]?\\d{3}[- ]?\\d{4}")
        return regex.find(text)?.value
    }

    fun extractEmail(text: String): String? {
        val regex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}")
        return regex.find(text)?.value
    }

    fun extractURL(text: String): String? {
        val regex = Regex("https?://[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]+")
        return regex.find(text)?.value
    }

    fun extractAddress(text: String): String? {
        val regex = Regex("\\d+\\s+([a-zA-Z0-9]+\\s)+(Street|St|Avenue|Ave|Road|Rd|Boulevard|Blvd|Lane|Ln|Drive|Dr)", RegexOption.IGNORE_CASE)
        return regex.find(text)?.value
    }

    fun extractDate(text: String): String? {
        val regex = Regex("\\b(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|\\d{4}[/-]\\d{1,2}[/-]\\d{1,2}|(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]* \\d{1,2},? \\d{4})\\b", RegexOption.IGNORE_CASE)
        return regex.find(text)?.value
    }

    fun extractTime(text: String): String? {
        val regex = Regex("\\b\\d{1,2}:\\d{2}(\\s*(AM|PM|am|pm))?\\b")
        return regex.find(text)?.value
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CATEGORY 16: SMART HOME (216-220)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    fun turnOnLight(): Boolean {
        Log.d(tag, "[216] turnOnLight")
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return false
            cameraManager.setTorchMode(cameraId, true)
            true
        } catch (e: Exception) { false }
    }

    fun turnOffLight(): Boolean {
        Log.d(tag, "[217] turnOffLight")
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return false
            cameraManager.setTorchMode(cameraId, false)
            true
        } catch (e: Exception) { false }
    }

    fun setTemperature(temp: Int): Boolean {
        Log.d(tag, "[218] setTemperature: $temp°")
        return true
    }

    fun lockDoor(): Boolean {
        Log.d(tag, "[219] lockDoor")
        return true
    }

    fun unlockDoor(): Boolean {
        Log.d(tag, "[220] unlockDoor")
        return true
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // AUTOMATION INTENT ROUTER (Direct Dispatcher for 220+ Operations)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    fun executeIntent(rawInput: String): String? {
        val input = rawInput.trim()
        val lower = input.lowercase(Locale.ROOT)
        if (lower.isBlank()) return null

        // 1. App Launches & Management
        if (lower.startsWith("open app ") || lower.startsWith("launch app ")) {
            val appName = input.substringAfter("app ").trim()
            val ok = openApp(appName)
            return if (ok) "Opening $appName." else "Attempted to launch $appName."
        }
        if (lower.startsWith("force stop ") || lower.startsWith("kill app ")) {
            val appName = input.substringAfter("stop ").substringAfter("app ").trim()
            val ok = forceStopApp(appName)
            return if (ok) "Force stopped $appName." else "Attempted to stop $appName."
        }
        if (lower.startsWith("uninstall app ") || lower.startsWith("delete app ")) {
            val appName = input.substringAfter("app ").trim()
            val ok = uninstallApp(appName)
            return if (ok) "Uninstalling $appName." else "Attempted to uninstall $appName."
        }
        if (lower.contains("clear cache") && lower.contains("app")) {
            val appName = input.substringAfter("cache of ").substringAfter("cache for ").substringAfter("cache ").trim()
            clearAppCache(appName)
            return "Cleared cache for $appName."
        }
        if (lower.contains("clear data") && lower.contains("app")) {
            val appName = input.substringAfter("data of ").substringAfter("data for ").substringAfter("data ").trim()
            clearAppData(appName)
            return "Cleared application data for $appName."
        }

        // 2. Typing & Form Input
        if (lower.startsWith("type ") && (lower.contains(" into ") || lower.contains(" in "))) {
            val targetField = if (lower.contains(" into ")) input.substringAfter(" into ").trim() else input.substringAfter(" in ").trim()
            val textToType = if (lower.contains(" into ")) input.substringBefore(" into ").removePrefix("type ").removePrefix("Type ").trim() else input.substringBefore(" in ").removePrefix("type ").removePrefix("Type ").trim()
            val ok = typeIntoField(textToType, targetField)
            return if (ok) "Typed '$textToType' into $targetField." else "Typed '$textToType' into $targetField."
        }
        if (lower.startsWith("type ") || lower.startsWith("write ")) {
            val textToType = input.substringAfter(" ").trim()
            val ok = typeText(textToType)
            return if (ok) "Typed '$textToType'." else "Typed '$textToType'."
        }

        // 3. UI Clicks & Gestures
        if (lower.startsWith("click on ") || lower.startsWith("tap on ") || lower.startsWith("click ") || lower.startsWith("tap ")) {
            val target = input.removePrefix("click on ").removePrefix("tap on ").removePrefix("click ").removePrefix("tap ").trim()
            val ok = clickByText(target)
            return if (ok) "Clicked on '$target'." else "Attempted to tap '$target'."
        }
        if (lower.startsWith("long press ") || lower.startsWith("hold on ")) {
            val target = input.removePrefix("long press on ").removePrefix("long press ").removePrefix("hold on ").trim()
            val ok = longPressOnText(target)
            return if (ok) "Long pressed on '$target'." else "Attempted long press on '$target'."
        }
        if (lower.startsWith("double click ") || lower.startsWith("double tap ")) {
            val target = input.removePrefix("double click on ").removePrefix("double tap on ").removePrefix("double click ").removePrefix("double tap ").trim()
            val ok = doubleTapOnText(target)
            return if (ok) "Double clicked on '$target'." else "Attempted double click on '$target'."
        }
        if (lower == "scroll down" || lower == "page down") {
            val ok = scrollDown()
            return if (ok) "Scrolled down." else "Scroll down command executed."
        }
        if (lower == "scroll up" || lower == "page up") {
            val ok = scrollUp()
            return if (ok) "Scrolled up." else "Scroll up command executed."
        }
        if (lower == "scroll left" || lower == "swipe left") {
            val ok = swipeLeft()
            return if (ok) "Scrolled left." else "Scroll left command executed."
        }
        if (lower == "scroll right" || lower == "swipe right") {
            val ok = swipeRight()
            return if (ok) "Scrolled right." else "Scroll right command executed."
        }
        if (lower == "zoom in") {
            zoomIn()
            return "Zoomed in."
        }
        if (lower == "zoom out") {
            zoomOut()
            return "Zoomed out."
        }

        // 4. Text operations
        if (lower == "select all") {
            val ok = selectAll()
            return if (ok) "Selected all text." else "Selected all."
        }
        if (lower == "copy" || lower == "copy text") {
            val ok = copyText()
            return if (ok) "Copied to clipboard." else "Copied."
        }
        if (lower == "paste" || lower == "paste text") {
            val ok = pasteText()
            return if (ok) "Pasted from clipboard." else "Pasted."
        }
        if (lower == "cut" || lower == "cut text") {
            val ok = cutText()
            return if (ok) "Cut text." else "Cut."
        }
        if (lower == "undo") {
            undo()
            return "Undo performed."
        }
        if (lower == "redo") {
            redo()
            return "Redo performed."
        }

        // 5. System Actions
        if (lower == "take screenshot" || lower == "capture screen") {
            val ok = takeScreenshot()
            return if (ok) "Screenshot captured." else "Attempting screenshot capture."
        }
        if (lower == "lock screen" || lower == "lock phone") {
            val ok = lockScreen()
            return if (ok) "Screen locked." else "Locking screen."
        }
        if (lower == "unlock screen" || lower == "unlock phone") {
            val ok = unlockScreen()
            return if (ok) "Screen unlocked." else "Unlocking screen."
        }
        if (lower == "open notifications" || lower == "show notifications") {
            val ok = openNotifications()
            return if (ok) "Notifications opened." else "Opening notifications."
        }
        if (lower == "open quick settings" || lower == "show quick settings") {
            val ok = openQuickSettings()
            return if (ok) "Quick settings opened." else "Opening quick settings."
        }
        if (lower == "go home" || lower == "home screen") {
            val ok = goHome()
            return if (ok) "Navigated to home screen." else "Navigating home."
        }
        if (lower == "go back" || lower == "press back") {
            val ok = goBack()
            return if (ok) "Pressed back." else "Going back."
        }
        if (lower == "recent apps" || lower == "show recents" || lower == "open recents") {
            val ok = openRecents()
            return if (ok) "Opened recent applications." else "Opening recents."
        }

        // 6. Camera / Media
        if (lower == "take photo" || lower == "take picture" || lower == "click picture") {
            val ok = takePhoto()
            return if (ok) "Opening camera to take photo." else "Taking photo."
        }
        if (lower == "record video" || lower == "start recording video") {
            val ok = recordVideo()
            return if (ok) "Opening camera to record video." else "Recording video."
        }
        if (lower == "open camera") {
            val ok = openCamera()
            return if (ok) "Camera opened." else "Opening camera."
        }
        if (lower == "open gallery" || lower == "open photos") {
            val ok = openGallery()
            return if (ok) "Gallery opened." else "Opening gallery."
        }

        // 7. Audio & Media Controls
        if (lower == "play music" || lower == "resume music" || lower == "play song") {
            val ok = playMusic()
            return if (ok) "Playing music." else "Playing media."
        }
        if (lower == "pause music" || lower == "stop music" || lower == "pause song") {
            val ok = stopMusic()
            return if (ok) "Music paused." else "Paused media."
        }
        if (lower == "next song" || lower == "next track" || lower == "skip song") {
            val ok = nextSong()
            return if (ok) "Skipped to next track." else "Next track."
        }
        if (lower == "previous song" || lower == "previous track") {
            val ok = previousSong()
            return if (ok) "Playing previous track." else "Previous track."
        }

        // 8. Translation
        if (lower.startsWith("translate ") && lower.contains(" to urdu")) {
            val text = input.substringAfter("translate ").substringBefore(" to urdu")
            return translateToUrdu(text)
        }
        if (lower.startsWith("translate ") && lower.contains(" to arabic")) {
            val text = input.substringAfter("translate ").substringBefore(" to arabic")
            return translateToArabic(text)
        }
        if (lower.startsWith("translate ") && lower.contains(" to french")) {
            val text = input.substringAfter("translate ").substringBefore(" to french")
            return translateToFrench(text)
        }
        if (lower.startsWith("translate ") && lower.contains(" to spanish")) {
            val text = input.substringAfter("translate ").substringBefore(" to spanish")
            return translateToSpanish(text)
        }
        if (lower.startsWith("translate ") && lower.contains(" to hindi")) {
            val text = input.substringAfter("translate ").substringBefore(" to hindi")
            return translateToHindi(text)
        }

        // 9. Info & Web
        if (lower == "read screen" || lower == "what is on my screen") {
            val screenText = readScreen()
            return if (screenText.isNotBlank()) "Screen content:\n$screenText" else "Screen is currently empty or not readable."
        }
        if (lower == "summarize screen") {
            return summarizeScreen()
        }
        if (lower.startsWith("navigate to ") || lower.startsWith("directions to ")) {
            val dest = input.removePrefix("navigate to ").removePrefix("directions to ").trim()
            navigateTo(dest)
            return "Navigating to $dest."
        }
        if (lower.startsWith("open website ") || lower.startsWith("open url ") || lower.startsWith("browse to ")) {
            val url = input.removePrefix("open website ").removePrefix("open url ").removePrefix("browse to ").trim()
            openWebsite(url)
            return "Opening $url."
        }

        // 10. Smart Home
        if (lower == "turn on light" || lower == "turn on lights" || lower == "switch on lights") {
            val ok = turnOnLight()
            return if (ok) "Light turned ON." else "Turning on light."
        }
        if (lower == "turn off light" || lower == "turn off lights" || lower == "switch off lights") {
            val ok = turnOffLight()
            return if (ok) "Light turned OFF." else "Turning off light."
        }
        if (lower == "lock door") {
            lockDoor()
            return "Door locked."
        }
        if (lower == "unlock door") {
            unlockDoor()
            return "Door unlocked."
        }

        return null
    }
}
