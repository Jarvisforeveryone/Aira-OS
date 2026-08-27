package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Path
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.*

@Suppress("DEPRECATION")
class AiraAccessibilityService : AccessibilityService() {

    enum class PendingAction {
        NONE, TOGGLE_WIFI, TOGGLE_BLUETOOTH
    }

    private var pendingAction = PendingAction.NONE
    private var targetState: Boolean = false

    // Track last focused editable element for Universal Typing
    private var lastFocusedViewId: String? = null
    private var lastFocusedClassName: String? = null
    private var lastFocusedHint: String? = null
    private var lastFocusedDesc: String? = null
    private var lastFocusedText: String? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Track focus & typing state across all windows
        try {
            val eventType = event.eventType
            if (eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED ||
                eventType == AccessibilityEvent.TYPE_VIEW_CLICKED ||
                eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED ||
                eventType == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED
            ) {
                val source = event.source
                if (source != null && source.isEditable) {
                    lastFocusedViewId = source.viewIdResourceName
                    lastFocusedClassName = source.className?.toString()
                    lastFocusedHint = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) source.hintText?.toString() else null
                    lastFocusedDesc = source.contentDescription?.toString()
                    lastFocusedText = source.text?.toString()
                    source.recycle()
                }
            }
        } catch (e: Exception) {
            // Ignore tracking errors
        }

        val pkg = event.packageName?.toString() ?: ""

        val isSystemUiAction = (pendingAction != PendingAction.NONE && pkg == "com.android.systemui")
        val isClockApp = (pkg.contains("clock") || pkg.contains("alarm"))

        if (!isSystemUiAction && !isClockApp) {
            return
        }

        if (com.example.utils.MemoryManager.isLowMemory(applicationContext)) {
            Log.w("AiraAccessibility", "System low memory state detected. Skipping accessibility node traversal.")
            return
        }

        val rootNode = rootInActiveWindow ?: return

        try {
            if (isSystemUiAction) {
                val targets = when (pendingAction) {
                    PendingAction.TOGGLE_WIFI -> listOf("wi-fi", "wifi", "wlan", "wi fi", "internet")
                    PendingAction.TOGGLE_BLUETOOTH -> listOf("bluetooth", "bt")
                    else -> emptyList()
                }
                if (targets.isNotEmpty()) {
                    val success = findAndClickNodeByTextOrContent(rootNode, targets)
                    if (success) {
                        Log.d("AiraAccessibility", "Action $pendingAction executed successfully!")
                        pendingAction = PendingAction.NONE
                        serviceScope.launch {
                            delay(1200)
                            performGlobalAction(GLOBAL_ACTION_BACK)
                        }
                    }
                }
            }

            if (isClockApp) {
                val alarmTargets = listOf("save", "done", "ok", "create", "confirm", "add")
                val clicked = findAndClickNodeByTextOrContent(rootNode, alarmTargets)
                if (clicked) {
                    Log.d("AiraAccessibility", "Auto-saved Alarm in clock/alarm application package: $pkg")
                }
            }
        } finally {
            rootNode.recycle()
        }
    }

    private fun findAndClickNodeByTextOrContent(rootNode: AccessibilityNodeInfo?, targets: List<String>): Boolean {
        if (rootNode == null) return false

        val text = rootNode.text?.toString()?.lowercase() ?: ""
        val contentDesc = rootNode.contentDescription?.toString()?.lowercase() ?: ""

        for (target in targets) {
            if (text.contains(target) || contentDesc.contains(target)) {
                if (rootNode.isClickable) {
                    return rootNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                } else {
                    var parent = rootNode.parent
                    while (parent != null) {
                        if (parent.isClickable) {
                            val clicked = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            parent.recycle()
                            return clicked
                        }
                        val oldParent = parent
                        parent = parent.parent
                        oldParent.recycle()
                    }
                }
            }
        }

        for (i in 0 until rootNode.childCount) {
            val child = rootNode.getChild(i)
            if (child != null) {
                val found = findAndClickNodeByTextOrContent(child, targets)
                child.recycle()
                if (found) return true
            }
        }
        return false
    }

    override fun onInterrupt() {
        Log.d("AiraAccessibility", "Service Interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        setInstance(this)
        Log.d("AiraAccessibility", "Aira Accessibility Service Connected Successfully")
    }

    // --- PART 1: ACCESSIBILITY SERVICE A-Z CAPABILITIES ---

    // 1. SCREEN READING
    fun readAllText(): String {
        val rootNode = rootInActiveWindow ?: return "Screen content unavailable or inactive."
        val sb = StringBuilder()
        try {
            collectTextRecursive(rootNode, sb)
        } catch (e: Exception) {
            Log.e("AiraAccessibility", "readAllText error", e)
        } finally {
            rootNode.recycle()
        }
        return sb.toString().trim().ifEmpty { "No visible text on screen." }
    }

    internal fun isSensitiveNode(node: AccessibilityNodeInfo): Boolean {
        if (node.isPassword) return true
        val resId = node.viewIdResourceName?.lowercase() ?: ""
        val hint = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) node.hintText?.toString()?.lowercase() ?: "" else ""
        val className = node.className?.toString()?.lowercase() ?: ""
        
        val sensitiveKeywords = listOf("password", "pin", "cvv", "credit_card", "passcode", "secret", "auth_token")
        for (keyword in sensitiveKeywords) {
            if (resId.contains(keyword) || hint.contains(keyword) || className.contains("password")) {
                return true
            }
        }
        return false
    }

    private fun collectTextRecursive(node: AccessibilityNodeInfo, sb: StringBuilder) {
        if (isSensitiveNode(node)) {
            sb.append("[PROTECTED_FIELD]").append("\n")
            return
        }

        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()
        if (!text.isNullOrBlank()) {
            sb.append(text).append("\n")
        } else if (!desc.isNullOrBlank()) {
            sb.append(desc).append("\n")
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                collectTextRecursive(child, sb)
                child.recycle()
            }
        }
    }

    fun readFocusedText(): String {
        val rootNode = rootInActiveWindow ?: return ""
        return try {
            val focused = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                ?: rootNode.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
            if (focused != null) {
                val text = if (isSensitiveNode(focused)) {
                    "[PROTECTED_FIELD]"
                } else {
                    focused.text?.toString() ?: focused.contentDescription?.toString() ?: ""
                }
                focused.recycle()
                text
            } else {
                ""
            }
        } catch (e: Exception) {
            Log.e("AiraAccessibility", "readFocusedText error", e)
            ""
        } finally {
            rootNode.recycle()
        }
    }

    fun getNodeByText(text: String): AccessibilityNodeInfo? {
        val rootNode = rootInActiveWindow ?: return null
        return try {
            findNodeMatching(rootNode) { node ->
                node.text?.toString()?.contains(text, ignoreCase = true) == true ||
                node.contentDescription?.toString()?.contains(text, ignoreCase = true) == true
            }
        } catch (e: Exception) {
            Log.e("AiraAccessibility", "getNodeByText error for '$text'", e)
            null
        } finally {
            rootNode.recycle()
        }
    }

    // 2. TAPPING / CLICKING
    fun tapOnText(text: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        return try {
            val target = findNodeMatching(rootNode) { node ->
                node.text?.toString()?.contains(text, ignoreCase = true) == true ||
                node.contentDescription?.toString()?.contains(text, ignoreCase = true) == true
            }
            val success = performClickOnNodeOrParent(target)
            target?.recycle()
            success
        } catch (e: Exception) {
            Log.e("AiraAccessibility", "tapOnText failed for '$text'", e)
            false
        } finally {
            rootNode.recycle()
        }
    }

    fun tapOnId(resourceId: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        return try {
            val target = findNodeMatching(rootNode) { node ->
                node.viewIdResourceName?.contains(resourceId, ignoreCase = true) == true
            }
            val success = performClickOnNodeOrParent(target)
            target?.recycle()
            success
        } catch (e: Exception) {
            Log.e("AiraAccessibility", "tapOnId failed for '$resourceId'", e)
            false
        } finally {
            rootNode.recycle()
        }
    }

    fun tapOnContentDescription(desc: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        return try {
            val target = findNodeMatching(rootNode) { node ->
                node.contentDescription?.toString()?.contains(desc, ignoreCase = true) == true
            }
            val success = performClickOnNodeOrParent(target)
            target?.recycle()
            success
        } catch (e: Exception) {
            Log.e("AiraAccessibility", "tapOnContentDescription failed for '$desc'", e)
            false
        } finally {
            rootNode.recycle()
        }
    }

    fun tapOnClass(className: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        return try {
            val target = findNodeMatching(rootNode) { node ->
                node.className?.toString()?.contains(className, ignoreCase = true) == true
            }
            val success = performClickOnNodeOrParent(target)
            target?.recycle()
            success
        } catch (e: Exception) {
            Log.e("AiraAccessibility", "tapOnClass failed for '$className'", e)
            false
        } finally {
            rootNode.recycle()
        }
    }

    // 3. INPUT / UNIVERSAL TYPING

    /**
     * 1. Types text into the currently focused editable field using ACTION_SET_TEXT.
     * Returns true if focused editable field was found and typed into, false otherwise.
     */
    fun typeText(text: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        return try {
            val focused = findFocusedEditableNode(rootNode)
            if (focused != null && focused.isEditable) {
                val arguments = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                }
                val success = focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                focused.recycle()
                success
            } else {
                focused?.recycle()
                false
            }
        } catch (e: Exception) {
            Log.e("AiraAccessibility", "typeText failed", e)
            false
        } finally {
            rootNode.recycle()
        }
    }

    /**
     * 2. Finds an editable field by hint, text, contentDescription, or viewId, focuses it, and types text.
     */
    fun typeIntoField(text: String, fieldHint: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        return try {
            var target = findNodeMatching(rootNode) { node ->
                node.isEditable && (
                    node.text?.toString()?.contains(fieldHint, ignoreCase = true) == true ||
                    node.contentDescription?.toString()?.contains(fieldHint, ignoreCase = true) == true ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && node.hintText?.toString()?.contains(fieldHint, ignoreCase = true) == true) ||
                    node.viewIdResourceName?.contains(fieldHint, ignoreCase = true) == true
                )
            }

            // Fallback: Check if label node is sibling or parent of an editable field
            if (target == null) {
                val labelNode = findNodeMatching(rootNode) { node ->
                    node.text?.toString()?.contains(fieldHint, ignoreCase = true) == true ||
                    node.contentDescription?.toString()?.contains(fieldHint, ignoreCase = true) == true
                }
                if (labelNode != null) {
                    val parent = labelNode.parent
                    if (parent != null) {
                        for (i in 0 until parent.childCount) {
                            val sibling = parent.getChild(i)
                            if (sibling != null && sibling.isEditable) {
                                target = sibling
                                break
                            }
                            sibling?.recycle()
                        }
                        parent.recycle()
                    }
                    labelNode.recycle()
                }
            }

            if (target != null) {
                target.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                try { Thread.sleep(150) } catch (_: InterruptedException) {}

                val arguments = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                }
                val success = target.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                target.recycle()
                success
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("AiraAccessibility", "typeIntoField failed for hint '$fieldHint'", e)
            false
        } finally {
            rootNode.recycle()
        }
    }

    /**
     * 3. Remembers the last focused editable field and types into it even if focus changed.
     */
    fun typeIntoLastFocusedField(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        return try {
            val target = when {
                !lastFocusedViewId.isNullOrEmpty() -> findNodeMatching(root) { it.isEditable && it.viewIdResourceName == lastFocusedViewId }
                !lastFocusedHint.isNullOrEmpty() -> findNodeMatching(root) { it.isEditable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && it.hintText?.toString() == lastFocusedHint }
                !lastFocusedDesc.isNullOrEmpty() -> findNodeMatching(root) { it.isEditable && it.contentDescription?.toString() == lastFocusedDesc }
                !lastFocusedClassName.isNullOrEmpty() -> findNodeMatching(root) { it.isEditable && it.className?.toString() == lastFocusedClassName }
                else -> null
            } ?: findFirstEditableNode(root)

            if (target != null && target.isEditable) {
                target.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                try { Thread.sleep(100) } catch (_: InterruptedException) {}

                val args = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                }
                val success = target.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                target.recycle()
                success
            } else {
                target?.recycle()
                false
            }
        } catch (e: Exception) {
            Log.e("AiraAccessibility", "typeIntoLastFocusedField error", e)
            false
        } finally {
            root.recycle()
        }
    }

    /**
     * 4. Types text character by character with delay to simulate human typing.
     */
    fun typeTextWithDelay(text: String, delayMs: Long = 50L): Boolean {
        val root = rootInActiveWindow ?: return false
        return try {
            val target = findFocusedEditableNode(root) ?: findFirstEditableNode(root) ?: return false
            target.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            try { Thread.sleep(100) } catch (_: InterruptedException) {}

            val sb = StringBuilder()
            var success = false
            for (ch in text) {
                sb.append(ch)
                val args = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, sb.toString())
                }
                success = target.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                if (delayMs > 0) {
                    try { Thread.sleep(delayMs) } catch (_: InterruptedException) {}
                }
            }
            target.recycle()
            success
        } catch (e: Exception) {
            Log.e("AiraAccessibility", "typeTextWithDelay error", e)
            false
        } finally {
            root.recycle()
        }
    }

    /**
     * 5. If no field is focused, finds the first editable field on screen, taps it to gain focus, then types.
     */
    fun ensureFocusBeforeTyping(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        return try {
            // First check if an editable field is already focused
            val focused = findFocusedEditableNode(root)
            if (focused != null && focused.isEditable) {
                val args = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                }
                val res = focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                focused.recycle()
                return res
            }
            focused?.recycle()

            // If no focus, find first editable node
            val editableNode = findFirstEditableNode(root)
            if (editableNode != null) {
                editableNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                editableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                try { Thread.sleep(200) } catch (_: InterruptedException) {}

                val args = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                }
                val res = editableNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                editableNode.recycle()
                return res
            }
            false
        } catch (e: Exception) {
            Log.e("AiraAccessibility", "ensureFocusBeforeTyping failed", e)
            false
        } finally {
            root.recycle()
        }
    }

    /** Alias for ensureFocusBeforeTyping */
    fun ensureFocusAndType(text: String): Boolean = ensureFocusBeforeTyping(text)

    /**
     * Helper to find the currently focused editable node.
     */
    fun findFocusedEditableNode(root: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (root == null) return null

        // 1. Direct Input Focus
        val inputFocus = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (inputFocus != null && inputFocus.isEditable) return inputFocus
        inputFocus?.recycle()

        // 2. Accessibility Focus
        val a11yFocus = root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
        if (a11yFocus != null && a11yFocus.isEditable) return a11yFocus
        a11yFocus?.recycle()

        // 3. Recursive search for focused & editable
        return findNodeMatching(root) { it.isEditable && it.isFocused }
    }

    /**
     * Helper to find the first editable node on screen.
     */
    fun findFirstEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable) return AccessibilityNodeInfo.obtain(node)
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findFirstEditableNode(child)
            child.recycle()
            if (result != null) return result
        }
        return null
    }

    /**
     * Universal Typing master dispatcher with full fallback cascade.
     */
    fun universalTypeText(text: String, fieldHint: String? = null, delayMs: Long = 0L): String {
        if (text.isBlank()) return "No text provided to type."

        // 1. Delay simulation if requested
        if (delayMs > 0L) {
            if (typeTextWithDelay(text, delayMs)) {
                return "Typed '$text' into screen field."
            }
        }

        // 2. Specific Field Hint
        if (!fieldHint.isNullOrBlank()) {
            if (typeIntoField(text, fieldHint)) {
                return "Typed '$text' into '$fieldHint' field."
            }
        }

        // 3. Focused Field
        if (typeText(text)) {
            return "Typed '$text' into focused field."
        }

        // 4. Ensure Focus on First Editable Field
        if (ensureFocusBeforeTyping(text)) {
            return "Focused screen input and typed '$text'."
        }

        // 5. Last Focused Field
        if (typeIntoLastFocusedField(text)) {
            return "Typed '$text' into target input field."
        }

        // 6. Shizuku System-Level Input Fallback
        if (com.example.utils.ShizukuManager.isShizukuAvailable()) {
            val escaped = text.replace(" ", "%s").replace("\"", "\\\"")
            val res = com.example.utils.ShizukuManager.executeCommand("input text \"$escaped\"")
            if (res) {
                return "Typed '$text' via Shizuku system input."
            }
        }

        return "Could not find any editable text field on screen to type into."
    }

    // 4. SCROLLING
    fun scrollForward(): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        return try {
            val scrollable = findNodeMatching(rootNode) { node -> node.isScrollable }
            val success = scrollable?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) ?: false
            scrollable?.recycle()
            success
        } catch (e: Exception) {
            Log.e("AiraAccessibility", "scrollForward failed", e)
            false
        } finally {
            rootNode.recycle()
        }
    }

    fun scrollBackward(): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        return try {
            val scrollable = findNodeMatching(rootNode) { node -> node.isScrollable }
            val success = scrollable?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) ?: false
            scrollable?.recycle()
            success
        } catch (e: Exception) {
            Log.e("AiraAccessibility", "scrollBackward failed", e)
            false
        } finally {
            rootNode.recycle()
        }
    }

    fun scrollToText(text: String): Boolean {
        var attempts = 0
        while (attempts < 10) {
            val node = getNodeByText(text)
            if (node != null) {
                node.recycle()
                return true
            }
            if (!scrollForward()) {
                break
            }
            attempts++
            try { Thread.sleep(300) } catch (e: InterruptedException) { break }
        }
        return false
    }

    // 5. GLOBAL ACTIONS
    fun goBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
    fun goHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
    fun openRecents(): Boolean = performGlobalAction(GLOBAL_ACTION_RECENTS)
    fun openNotifications(): Boolean = performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    fun openQuickSettings(): Boolean = performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
    fun lockScreen(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        } else {
            false
        }
    }

    // 6. GESTURES
    fun customSwipe(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long = 300): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        return try {
            val path = Path().apply {
                moveTo(startX, startY)
                lineTo(endX, endY)
            }
            val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            dispatchGesture(gesture, null, null)
        } catch (e: Exception) {
            Log.e("AiraAccessibility", "customSwipe failed", e)
            false
        }
    }

    fun swipeUp(): Boolean {
        val metrics = resources.displayMetrics
        val startX = metrics.widthPixels / 2f
        val startY = metrics.heightPixels * 0.8f
        val endY = metrics.heightPixels * 0.2f
        return customSwipe(startX, startY, startX, endY)
    }

    fun swipeDown(): Boolean {
        val metrics = resources.displayMetrics
        val startX = metrics.widthPixels / 2f
        val startY = metrics.heightPixels * 0.2f
        val endY = metrics.heightPixels * 0.8f
        return customSwipe(startX, startY, startX, endY)
    }

    fun swipeLeft(): Boolean {
        val metrics = resources.displayMetrics
        val startY = metrics.heightPixels / 2f
        val startX = metrics.widthPixels * 0.8f
        val endX = metrics.widthPixels * 0.2f
        return customSwipe(startX, startY, endX, startY)
    }

    fun swipeRight(): Boolean {
        val metrics = resources.displayMetrics
        val startY = metrics.heightPixels / 2f
        val startX = metrics.widthPixels * 0.2f
        val endX = metrics.widthPixels * 0.8f
        return customSwipe(startX, startY, endX, startY)
    }

    // 7. UI TREE DUMP
    fun dumpNodeTree(): String {
        val rootNode = rootInActiveWindow ?: return "Root node unavailable"
        val sb = StringBuilder()
        try {
            dumpNodeRecursive(rootNode, sb, 0)
        } catch (e: Exception) {
            Log.e("AiraAccessibility", "dumpNodeTree failed", e)
        } finally {
            rootNode.recycle()
        }
        return sb.toString()
    }

    private fun dumpNodeRecursive(node: AccessibilityNodeInfo, sb: StringBuilder, indent: Int) {
        val pad = " ".repeat(indent * 2)
        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        val id = node.viewIdResourceName ?: ""
        val cls = node.className?.toString() ?: ""
        val clickable = node.isClickable
        sb.append("$pad[$cls] id=$id text='$text' desc='$desc' clickable=$clickable\n")

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                dumpNodeRecursive(child, sb, indent + 1)
                child.recycle()
            }
        }
    }

    fun findNodeByPath(path: String): AccessibilityNodeInfo? {
        val rootNode = rootInActiveWindow ?: return null
        val indices = path.split("/").mapNotNull { it.toIntOrNull() }
        var currentNode = rootNode
        for (idx in indices) {
            if (idx in 0 until currentNode.childCount) {
                val nextNode = currentNode.getChild(idx)
                if (currentNode != rootNode) {
                    currentNode.recycle()
                }
                if (nextNode == null) return null
                currentNode = nextNode
            } else {
                if (currentNode != rootNode) currentNode.recycle()
                rootNode.recycle()
                return null
            }
        }
        return currentNode
    }

    // 8. WAIT FOR UI
    fun waitForText(text: String, timeoutMs: Long = 3000): Boolean {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val rootNode = rootInActiveWindow
            if (rootNode != null) {
                val foundNode = findNodeMatching(rootNode) { node ->
                    node.text?.toString()?.contains(text, ignoreCase = true) == true ||
                    node.contentDescription?.toString()?.contains(text, ignoreCase = true) == true
                }
                rootNode.recycle()
                if (foundNode != null) {
                    foundNode.recycle()
                    return true
                }
            }
            try { Thread.sleep(200) } catch (e: InterruptedException) { break }
        }
        return false
    }

    fun waitForId(id: String, timeoutMs: Long = 3000): Boolean {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val rootNode = rootInActiveWindow
            if (rootNode != null) {
                val foundNode = findNodeMatching(rootNode) { node ->
                    node.viewIdResourceName?.contains(id, ignoreCase = true) == true
                }
                rootNode.recycle()
                if (foundNode != null) {
                    foundNode.recycle()
                    return true
                }
            }
            try { Thread.sleep(200) } catch (e: InterruptedException) { break }
        }
        return false
    }

    // 9. SCREENSHOT
    fun takeScreenshot(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
        } else {
            false
        }
    }

    fun takeScreenshot(callback: (Bitmap?) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                takeScreenshot(
                    Display.DEFAULT_DISPLAY,
                    applicationContext.mainExecutor,
                    object : TakeScreenshotCallback {
                        override fun onSuccess(screenshot: ScreenshotResult) {
                            val bitmap = Bitmap.wrapHardwareBuffer(
                                screenshot.hardwareBuffer,
                                screenshot.colorSpace
                            )
                            callback(bitmap)
                        }

                        override fun onFailure(errorCode: Int) {
                            Log.e("AiraAccessibility", "takeScreenshot failed with code $errorCode")
                            callback(null)
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("AiraAccessibility", "takeScreenshot failed", e)
                callback(null)
            }
        } else {
            performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
            callback(null)
        }
    }

    fun longPressOnText(text: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        return try {
            val target = findNodeMatching(rootNode) { node ->
                node.text?.toString()?.contains(text, ignoreCase = true) == true ||
                node.contentDescription?.toString()?.contains(text, ignoreCase = true) == true
            }
            val success = target?.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK) ?: false
            target?.recycle()
            success
        } catch (e: Exception) {
            Log.e("AiraAccessibility", "longPressOnText failed for '$text'", e)
            false
        } finally {
            rootNode.recycle()
        }
    }

    fun longPressOnId(resourceId: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        return try {
            val target = findNodeMatching(rootNode) { node ->
                node.viewIdResourceName?.contains(resourceId, ignoreCase = true) == true
            }
            val success = target?.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK) ?: false
            target?.recycle()
            success
        } catch (e: Exception) {
            Log.e("AiraAccessibility", "longPressOnId failed for '$resourceId'", e)
            false
        } finally {
            rootNode.recycle()
        }
    }

    // --- HELPER NODE FUNCTIONS ---
    fun findNodeMatchingPublic(root: AccessibilityNodeInfo?, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        return findNodeMatching(root, predicate)
    }

    private fun findNodeMatching(root: AccessibilityNodeInfo?, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        if (root == null) return null
        if (predicate(root)) {
            return AccessibilityNodeInfo.obtain(root)
        }
        for (i in 0 until root.childCount) {
            val child = root.getChild(i)
            if (child != null) {
                val result = findNodeMatching(child, predicate)
                child.recycle()
                if (result != null) return result
            }
        }
        return null
    }

    private fun performClickOnNodeOrParent(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        if (node.isClickable) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        var parent = node.parent
        while (parent != null) {
            if (parent.isClickable) {
                val success = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                parent.recycle()
                return success
            }
            val oldParent = parent
            parent = parent.parent
            oldParent.recycle()
        }
        return false
    }

    // --- SYSTEM TOGGLE API ---
    fun toggleWifi(enable: Boolean): String {
        if (com.example.utils.ShizukuManager.isShizukuAvailable()) {
            val success = com.example.utils.ShizukuManager.toggleWiFi(enable)
            if (success) {
                return "Successfully toggled Wi-Fi via Shizuku privileged shell (svc wifi ${if (enable) "enable" else "disable"})."
            }
        }

        pendingAction = PendingAction.TOGGLE_WIFI
        targetState = enable
        val opened = performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
        return if (opened) {
            "Direct Wifi toggle restricted. Directing automation sequence via Accessibility Quick Settings override."
        } else {
            "Direct Wifi toggle restricted and Quick Settings panel unavailable."
        }
    }

    fun toggleBluetooth(enable: Boolean): String {
        if (com.example.utils.ShizukuManager.isShizukuAvailable()) {
            val success = com.example.utils.ShizukuManager.toggleBluetooth(enable)
            if (success) {
                return "Successfully toggled Bluetooth via Shizuku privileged shell (svc bluetooth ${if (enable) "enable" else "disable"})."
            }
        }

        pendingAction = PendingAction.TOGGLE_BLUETOOTH
        targetState = enable
        val opened = performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
        return if (opened) {
            "Direct Bluetooth toggle restricted. Directing automation sequence via Accessibility Quick Settings override."
        } else {
            "Direct Bluetooth toggle restricted and Quick Settings panel unavailable."
        }
    }

    fun performBackAction(): Boolean = goBack()
    fun performHomeAction(): Boolean = goHome()
    fun performRecentsAction(): Boolean = openRecents()
    fun performLockScreenAction(): Boolean = lockScreen()
    fun performScreenshotAction(): Boolean = takeScreenshot()
    fun performNotificationsAction(): Boolean = openNotifications()
    fun performQuickSettingsAction(): Boolean = performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
    fun performPowerMenuAction(): Boolean = performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)

    fun clickElementByText(targetText: String): String {
        val success = tapOnText(targetText)
        return if (success) {
            "Successfully clicked element matching '$targetText' on screen."
        } else {
            "Could not find any clickable element matching '$targetText' on current screen."
        }
    }

    companion object {
        @JvmField
        var instance: AiraAccessibilityService? = null

        fun getInstance(): AiraAccessibilityService? = instance

        fun setInstance(service: AiraAccessibilityService?) {
            instance = service
        }

        fun clearInstance() {
            instance = null
        }

        fun isAccessibilityEnabled(context: Context): Boolean {
            val expectedServiceName = "${context.packageName}/${AiraAccessibilityService::class.java.canonicalName}"
            val accessibilityEnabled = try {
                android.provider.Settings.Secure.getInt(
                    context.contentResolver,
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED
                )
            } catch (e: Exception) { 0 }

            if (accessibilityEnabled == 1) {
                val settingValue = android.provider.Settings.Secure.getString(
                    context.contentResolver,
                    android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                ) ?: ""
                return settingValue.contains(expectedServiceName) || instance != null
            }
            return instance != null
        }

        fun openAccessibilitySettings(context: Context) {
            try {
                val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e("AiraAccessibility", "Failed to open accessibility settings", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        clearInstance()
    }
}
