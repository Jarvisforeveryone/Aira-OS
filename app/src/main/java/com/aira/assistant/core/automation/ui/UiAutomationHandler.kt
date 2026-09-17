package com.aira.assistant.core.automation.ui

import android.content.Context
import android.util.Log
import com.aira.assistant.core.shizuku.ShizukuManager
import com.aira.assistant.service.AiraAccessibilityService

class UiAutomationHandler(
    private val context: Context,
    private val a11yProvider: () -> AiraAccessibilityService?
) {
    private val tag = "UiAutomationHandler"
    private val a11y: AiraAccessibilityService? get() = a11yProvider()

    fun tapText(text: String): Boolean {
        Log.d(tag, "tapText: $text")
        return a11y?.tapOnText(text) ?: false
    }

    fun tapId(resourceId: String): Boolean {
        Log.d(tag, "tapId: $resourceId")
        return a11y?.tapOnId(resourceId) ?: false
    }

    fun tapCoordinates(x: Float, y: Float): Boolean {
        Log.d(tag, "tapCoordinates: ($x, $y)")
        if (ShizukuManager.isShizukuAvailable() && ShizukuManager.executeCommand("input tap ${x.toInt()} ${y.toInt()}")) return true
        return a11y?.customSwipe(x, y, x, y, 50L) ?: false
    }

    fun typeText(text: String): Boolean {
        Log.d(tag, "typeText: $text")
        if (ShizukuManager.isShizukuAvailable()) {
            val sanitized = ShizukuManager.sanitizeShellArg(text)
            if (ShizukuManager.executeCommand("input text $sanitized")) return true
        }
        return a11y?.typeText(text) ?: false
    }

    fun swipeUp(): Boolean = a11y?.swipeUp() ?: false
    fun swipeDown(): Boolean = a11y?.swipeDown() ?: false
    fun swipeLeft(): Boolean = a11y?.swipeLeft() ?: false
    fun swipeRight(): Boolean = a11y?.swipeRight() ?: false

    fun customSwipe(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long = 300L): Boolean {
        return a11y?.customSwipe(startX, startY, endX, endY, durationMs) ?: false
    }
}
