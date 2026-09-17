package com.aira.assistant.domain.automation

import android.content.Context
import android.util.Log
import com.aira.assistant.service.AiraAccessibilityService
import com.aira.assistant.core.shizuku.ShizukuManager

/**
 * Handles UI gestures: tap, swipe, scroll, click, navigation, inspection.
 */
class UiGestureHandler(
    private val context: Context,
    private val a11yProvider: () -> AiraAccessibilityService?
) {
    private val tag = "UiGestureHandler"
    private val a11y: AiraAccessibilityService? get() = a11yProvider()

    fun clickByText(text: String): Boolean {
        Log.d(tag, "clickByText: $text")
        return a11y?.tapOnText(text) ?: false
    }

    fun clickById(resourceId: String): Boolean {
        Log.d(tag, "clickById: $resourceId")
        return a11y?.tapOnId(resourceId) ?: false
    }

    fun clickByContentDescription(desc: String): Boolean {
        Log.d(tag, "clickByContentDescription: $desc")
        return a11y?.tapOnContentDescription(desc) ?: false
    }

    fun clickByClass(className: String): Boolean {
        Log.d(tag, "clickByClass: $className")
        return a11y?.tapOnClass(className) ?: false
    }

    fun scrollDown(): Boolean {
        Log.d(tag, "scrollDown")
        return a11y?.scrollForward() ?: a11y?.swipeUp() ?: false
    }

    fun scrollUp(): Boolean {
        Log.d(tag, "scrollUp")
        return a11y?.scrollBackward() ?: a11y?.swipeDown() ?: false
    }

    fun scrollToText(text: String): Boolean {
        Log.d(tag, "scrollToText: $text")
        return a11y?.let { service ->
            kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.Default) {
                service.scrollToText(text)
            }
        } ?: false
    }

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

    fun takeScreenshot(): Boolean {
        Log.d(tag, "takeScreenshot")
        if (ShizukuManager.isShizukuAvailable() && ShizukuManager.takeScreenshot()) return true
        return a11y?.takeScreenshot() ?: false
    }

    fun readScreen(): String {
        Log.d(tag, "readScreen")
        return a11y?.readAllText() ?: ""
    }

    fun readFocusedText(): String {
        Log.d(tag, "readFocusedText")
        return a11y?.readFocusedText() ?: ""
    }

    fun findText(text: String): Boolean {
        Log.d(tag, "findText: $text")
        return a11y?.getNodeByText(text) != null
    }

    fun waitForText(text: String, timeoutMs: Long = 5000L): Boolean {
        Log.d(tag, "waitForText: $text (timeout=${timeoutMs}ms)")
        return a11y?.let { service ->
            kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.Default) {
                service.waitForText(text, timeoutMs)
            }
        } ?: false
    }

    fun waitForId(resourceId: String, timeoutMs: Long = 5000L): Boolean {
        Log.d(tag, "waitForId: $resourceId (timeout=${timeoutMs}ms)")
        return a11y?.let { service ->
            kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.Default) {
                service.waitForId(resourceId, timeoutMs)
            }
        } ?: false
    }

    fun dumpUITree(): String {
        Log.d(tag, "dumpUITree")
        return a11y?.dumpNodeTree() ?: "UI Tree unavailable (Accessibility disabled)"
    }

    fun swipeUp(): Boolean {
        Log.d(tag, "swipeUp")
        return a11y?.swipeUp() ?: false
    }

    fun swipeDown(): Boolean {
        Log.d(tag, "swipeDown")
        return a11y?.swipeDown() ?: false
    }

    fun swipeLeft(): Boolean {
        Log.d(tag, "swipeLeft")
        return a11y?.swipeLeft() ?: false
    }

    fun swipeRight(): Boolean {
        Log.d(tag, "swipeRight")
        return a11y?.swipeRight() ?: false
    }

    fun customSwipe(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long = 300L): Boolean {
        Log.d(tag, "customSwipe from ($startX,$startY) to ($endX,$endY)")
        return a11y?.customSwipe(startX, startY, endX, endY, durationMs) ?: false
    }

    fun zoomIn(): Boolean {
        Log.d(tag, "zoomIn")
        val w = context.resources.displayMetrics.widthPixels.toFloat()
        val h = context.resources.displayMetrics.heightPixels.toFloat()
        return customSwipe(w * 0.4f, h * 0.4f, w * 0.1f, h * 0.1f, 300L)
    }

    fun zoomOut(): Boolean {
        Log.d(tag, "zoomOut")
        val w = context.resources.displayMetrics.widthPixels.toFloat()
        val h = context.resources.displayMetrics.heightPixels.toFloat()
        return customSwipe(w * 0.1f, h * 0.1f, w * 0.4f, h * 0.4f, 300L)
    }

    fun doubleTap(text: String): Boolean {
        Log.d(tag, "doubleTap: $text")
        val first = a11y?.tapOnText(text) ?: false
        Thread.sleep(100)
        val second = a11y?.tapOnText(text) ?: false
        return first && second
    }

    fun longPress(text: String): Boolean {
        Log.d(tag, "longPress: $text")
        return a11y?.longPressOnText(text) ?: false
    }

    fun longPressById(resourceId: String): Boolean {
        Log.d(tag, "longPressById: $resourceId")
        return a11y?.longPressOnId(resourceId) ?: false
    }

    fun tapCoordinates(x: Float, y: Float): Boolean {
        Log.d(tag, "tapCoordinates: ($x, $y)")
        if (ShizukuManager.isShizukuAvailable() && ShizukuManager.executeCommand("input tap ${x.toInt()} ${y.toInt()}")) return true
        return customSwipe(x, y, x, y, 50L)
    }
}
