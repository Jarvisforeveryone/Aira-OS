package com.aira.assistant.domain.automation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.aira.assistant.service.AiraAccessibilityService
import com.aira.assistant.core.shizuku.ShizukuManager
import java.io.File

/**
 * Handles application lifecycle, launch, termination, permissions, and app management.
 */
class AppManagementHandler(
    private val context: Context,
    private val a11yProvider: () -> AiraAccessibilityService?
) {
    private val tag = "AppManagementHandler"
    private val a11y: AiraAccessibilityService? get() = a11yProvider()

    fun openApp(packageName: String): Boolean {
        Log.d(tag, "openApp: $packageName")
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

    fun launchApp(packageName: String): Boolean = openApp(packageName)

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

    fun killApp(packageName: String): Boolean = forceStopApp(packageName)

    fun clearAppData(packageName: String): Boolean {
        Log.d(tag, "clearAppData: $packageName")
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

    fun installApk(apkPath: String): Boolean {
        Log.d(tag, "installApk: $apkPath")
        val file = File(apkPath)
        if (!file.exists()) return false
        if (ShizukuManager.isShizukuAvailable()) {
            return ShizukuManager.installApk(apkPath)
        }
        return try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(tag, "installApk failed", e)
            false
        }
    }

    fun grantPermission(packageName: String, permission: String): Boolean {
        Log.d(tag, "grantPermission: $permission for $packageName")
        if (ShizukuManager.isShizukuAvailable()) {
            return ShizukuManager.executeCommand("pm grant $packageName $permission")
        }
        return false
    }

    fun revokePermission(packageName: String, permission: String): Boolean {
        Log.d(tag, "revokePermission: $permission for $packageName")
        if (ShizukuManager.isShizukuAvailable()) {
            return ShizukuManager.executeCommand("pm revoke $packageName $permission")
        }
        return false
    }

    fun openAppSettings(packageName: String): Boolean {
        Log.d(tag, "openAppSettings: $packageName")
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return true
    }

    fun listInstalledApps(): List<String> {
        if (ShizukuManager.isShizukuAvailable()) {
            val apps = ShizukuManager.listInstalledApps()
            if (apps.isNotEmpty()) return apps
        }
        return context.packageManager.getInstalledApplications(0).map { it.packageName }
    }
}
