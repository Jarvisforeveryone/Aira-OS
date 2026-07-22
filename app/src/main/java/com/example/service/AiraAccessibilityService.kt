package com.example.service

import android.accessibilityservice.AccessibilityService
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.*

class AiraAccessibilityService : AccessibilityService() {

    enum class PendingAction {
        NONE, TOGGLE_WIFI, TOGGLE_BLUETOOTH
    }

    private var pendingAction = PendingAction.NONE
    private var targetState: Boolean = false

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val eventType = event.eventType
        val pkg = event.packageName?.toString() ?: ""
        
        Log.d("AiraAccessibility", "Accessibility Event: $eventType Package: $pkg")

        val rootNode = rootInActiveWindow ?: return

        try {
            // 1. QUICK SETTINGS AUTOMATION FOR WIFI & BLUETOOTH
            if (pendingAction != PendingAction.NONE && pkg == "com.android.systemui") {
                val targets = when (pendingAction) {
                    PendingAction.TOGGLE_WIFI -> listOf("wi-fi", "wifi", "wlan", "wi fi", "internet")
                    PendingAction.TOGGLE_BLUETOOTH -> listOf("bluetooth", "bt")
                    else -> emptyList()
                }
                if (targets.isNotEmpty()) {
                    val success = findAndClickNodeByTextOrContent(rootNode, targets)
                    if (success) {
                        Log.d("AiraAccessibility", "Action $pendingAction executed successfully! Auto closing quick settings in 1.2s.")
                        pendingAction = PendingAction.NONE
                        serviceScope.launch {
                            delay(1200)
                            performGlobalAction(GLOBAL_ACTION_BACK)
                        }
                    }
                }
            }

            // 2. AUTO-SAVE SYSTEM ALARMS (Triggered from clock application)
            if (pkg.contains("clock") || pkg.contains("alarm")) {
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
                    val clicked = rootNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    Log.d("AiraAccessibility", "Clicked target '$target' directly: $clicked")
                    return true
                } else {
                    var parent = rootNode.parent
                    while (parent != null) {
                        if (parent.isClickable) {
                            val clicked = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            Log.d("AiraAccessibility", "Clicked parent of '$target': $clicked")
                            parent.recycle()
                            return true
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
                if (found) {
                    return true
                }
            }
        }
        return false
    }

    override fun onInterrupt() {
        Log.d("AiraAccessibility", "Service Interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("AiraAccessibility", "Aira Accessibility Service Connected Successfully")
    }

    // --- System Control API Hook Ups ---
    fun toggleWifi(enable: Boolean): String {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (wifiManager != null) {
            try {
                @Suppress("DEPRECATION")
                if (wifiManager.isWifiEnabled == enable) {
                    return "Wi-Fi is already ${if (enable) "enabled" else "disabled"}."
                }
                @Suppress("DEPRECATION")
                val success = wifiManager.setWifiEnabled(enable)
                if (success) {
                    return "Successfully toggled Wi-Fi programmatically via WifiManager."
                }
            } catch (e: Exception) {
                Log.d("AiraAccessibility", "Programmatic Wifi toggle failed: ${e.message}")
            }
        }

        // Drop back to automated Accessibility implementation via Quick Settings
        pendingAction = PendingAction.TOGGLE_WIFI
        targetState = enable
        val opened = performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
        return if (opened) {
            "Direct Wifi toggle restricted. Directing automation sequence via Accessibility Quick Settings override."
        } else {
            "Direct Wifi toggle restricted and Quick Settings panel unavailable."
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    fun toggleBluetooth(enable: Boolean): String {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter != null) {
            try {
                val isCurrentlyEnabled = adapter.isEnabled
                if (isCurrentlyEnabled == enable) {
                    return "Bluetooth is already ${if (enable) "enabled" else "disabled"}."
                }
                @Suppress("DEPRECATION")
                val success = if (enable) adapter.enable() else adapter.disable()
                if (success) {
                    return "Successfully toggled Bluetooth programmatically."
                }
            } catch (e: Exception) {
                Log.d("AiraAccessibility", "Programmatic Bluetooth toggle failed: ${e.message}")
            }
        }

        // Drop back to automated Accessibility implementation via Quick Settings
        pendingAction = PendingAction.TOGGLE_BLUETOOTH
        targetState = enable
        val opened = performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
        return if (opened) {
            "Direct Bluetooth toggle restricted. Directing automation sequence via Accessibility Quick Settings override."
        } else {
            "Direct Bluetooth toggle restricted and Quick Settings panel unavailable."
        }
    }

    companion object {
        var instance: AiraAccessibilityService? = null
            private set
    }

    init {
        instance = this
    }

    fun performBackAction(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun performHomeAction(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }

    fun performRecentsAction(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_RECENTS)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        if (instance == this) {
            instance = null
        }
    }
}
