package com.aira.assistant.core.automation

import android.content.Context
import com.aira.assistant.core.automation.apps.AppAutomationHandler
import com.aira.assistant.core.automation.audio.AudioAutomationHandler
import com.aira.assistant.core.automation.connectivity.ConnectivityHandler
import com.aira.assistant.core.automation.display.DisplayAutomationHandler
import com.aira.assistant.core.automation.navigation.NavigationAutomationHandler
import com.aira.assistant.core.automation.ui.UiAutomationHandler
import com.aira.assistant.core.shizuku.ShizukuManager
import com.aira.assistant.domain.automation.AutomationEngine
import com.aira.assistant.service.AiraAccessibilityService

/**
 * Facade dispatcher providing a unified public API across all automation domains.
 */
class AutomationDispatcher(
    private val context: Context,
    private val a11yProvider: () -> AiraAccessibilityService? = { AiraAccessibilityService.instance }
) {
    val connectivity = ConnectivityHandler(context, a11yProvider)
    val audio = AudioAutomationHandler(context)
    val display = DisplayAutomationHandler(context, a11yProvider)
    val apps = AppAutomationHandler(context, a11yProvider)
    val navigation = NavigationAutomationHandler(a11yProvider)
    val ui = UiAutomationHandler(context, a11yProvider)

    val engine = AutomationEngine(context)

    fun isAccessibilityEnabled(): Boolean = a11yProvider() != null || AiraAccessibilityService.instance != null
    fun isShizukuAvailable(): Boolean = ShizukuManager.isShizukuAvailable()
    fun isShizukuPermissionGranted(): Boolean = ShizukuManager.isPermissionGranted()

    companion object {
        @Volatile
        private var instance: AutomationDispatcher? = null

        fun getInstance(
            context: Context,
            a11yProvider: () -> AiraAccessibilityService? = { AiraAccessibilityService.instance }
        ): AutomationDispatcher {
            return instance ?: synchronized(this) {
                instance ?: AutomationDispatcher(context.applicationContext, a11yProvider).also { instance = it }
            }
        }
    }
}
