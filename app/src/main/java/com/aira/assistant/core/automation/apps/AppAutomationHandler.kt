package com.aira.assistant.core.automation.apps

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import com.aira.assistant.core.shizuku.ShizukuManager
import com.aira.assistant.service.AiraAccessibilityService

class AppAutomationHandler(
    private val context: Context,
    private val a11yProvider: () -> AiraAccessibilityService?
) {
    private val tag = "AppAutomationHandler"
    private val a11y: AiraAccessibilityService? get() = a11yProvider()

    fun launchApp(packageName: String): Boolean {
        Log.d(tag, "launchApp: $packageName")
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
            Log.e(tag, "launchApp failed: $packageName", e)
            false
        }
    }

    fun forceStopApp(packageName: String): Boolean {
        Log.d(tag, "forceStopApp: $packageName")
        if (ShizukuManager.isShizukuAvailable() && ShizukuManager.forceStopApp(packageName)) {
            return true
        }
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return true
    }

    fun clearCache(packageName: String): Boolean {
        Log.d(tag, "clearCache: $packageName")
        if (ShizukuManager.isShizukuAvailable() && ShizukuManager.clearAppData(packageName)) {
            return true
        }
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return true
    }

    fun uninstallApp(packageName: String): Boolean {
        Log.d(tag, "uninstallApp: $packageName")
        if (ShizukuManager.isShizukuAvailable() && ShizukuManager.uninstallApp(packageName)) {
            return true
        }
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:$packageName")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return true
    }
}
