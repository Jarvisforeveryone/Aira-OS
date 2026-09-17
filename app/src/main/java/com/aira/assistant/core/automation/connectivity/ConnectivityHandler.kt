package com.aira.assistant.core.automation.connectivity

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import com.aira.assistant.core.shizuku.ShizukuManager
import com.aira.assistant.service.AiraAccessibilityService

class ConnectivityHandler(
    private val context: Context,
    private val a11yProvider: () -> AiraAccessibilityService?
) {
    private val tag = "ConnectivityHandler"
    private val a11y: AiraAccessibilityService? get() = a11yProvider()

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

    fun setHotspot(enable: Boolean): Boolean {
        Log.d(tag, "setHotspot: $enable")
        if (ShizukuManager.isShizukuAvailable()) {
            val cmd = if (enable) "cmd wifi start-softap aira_hotspot wpa2 pass123456" else "cmd wifi stop-softap"
            return ShizukuManager.executeCommand(cmd)
        }
        return false
    }
}
