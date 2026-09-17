package com.aira.assistant.core.automation.navigation

import android.util.Log
import com.aira.assistant.core.shizuku.ShizukuManager
import com.aira.assistant.service.AiraAccessibilityService

class NavigationAutomationHandler(
    private val a11yProvider: () -> AiraAccessibilityService?
) {
    private val tag = "NavigationAutomationHandler"
    private val a11y: AiraAccessibilityService? get() = a11yProvider()

    fun goBack(): Boolean {
        Log.d(tag, "goBack")
        if (ShizukuManager.isShizukuAvailable() && ShizukuManager.systemNavigation("back")) return true
        return a11y?.goBack() ?: false
    }

    fun goHome(): Boolean {
        Log.d(tag, "goHome")
        if (ShizukuManager.isShizukuAvailable() && ShizukuManager.systemNavigation("home")) return true
        return a11y?.goHome() ?: false
    }

    fun openRecents(): Boolean {
        Log.d(tag, "openRecents")
        if (ShizukuManager.isShizukuAvailable() && ShizukuManager.systemNavigation("recents")) return true
        return a11y?.openRecents() ?: false
    }

    fun openNotifications(): Boolean {
        Log.d(tag, "openNotifications")
        if (ShizukuManager.isShizukuAvailable() && ShizukuManager.openNotifications()) return true
        return a11y?.openNotifications() ?: false
    }

    fun openQuickSettings(): Boolean {
        Log.d(tag, "openQuickSettings")
        if (ShizukuManager.isShizukuAvailable() && ShizukuManager.openQuickSettings()) return true
        return a11y?.openQuickSettings() ?: false
    }

    fun lockScreen(): Boolean {
        Log.d(tag, "lockScreen")
        if (ShizukuManager.isShizukuAvailable() && ShizukuManager.lockScreen()) return true
        return a11y?.lockScreen() ?: false
    }
}
