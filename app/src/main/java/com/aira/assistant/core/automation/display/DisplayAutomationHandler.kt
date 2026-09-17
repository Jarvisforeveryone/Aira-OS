package com.aira.assistant.core.automation.display

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import com.aira.assistant.core.shizuku.ShizukuManager
import com.aira.assistant.service.AiraAccessibilityService

class DisplayAutomationHandler(
    private val context: Context,
    private val a11yProvider: () -> AiraAccessibilityService?
) {
    private val tag = "DisplayAutomationHandler"
    private val a11y: AiraAccessibilityService? get() = a11yProvider()

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

    fun setFlashlight(enable: Boolean): Boolean {
        Log.d(tag, "setFlashlight: $enable")
        if (ShizukuManager.isShizukuAvailable() && ShizukuManager.toggleFlashlight(enable)) return true
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
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

    fun takeScreenshot(): Boolean {
        Log.d(tag, "takeScreenshot")
        if (ShizukuManager.isShizukuAvailable() && ShizukuManager.takeScreenshot()) return true
        return a11y?.takeScreenshot() ?: false
    }
}
