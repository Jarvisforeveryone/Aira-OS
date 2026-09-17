package com.aira.assistant.presentation.navigation

/**
 * Type-safe navigation screen hierarchy for AIRA OS.
 */
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object SystemControl : Screen("system_control")
    object OfflineDashboard : Screen("offline_dashboard")
    object Settings : Screen("settings")
    object Extras : Screen("extras")
    object Onboarding : Screen("onboarding")
    object ThemeSettings : Screen("theme_settings")
    object Accessibility : Screen("accessibility")
    object WakeWordTrainer : Screen("wake_word_trainer")
    object Chat : Screen("chat")
    object DeviceControl : Screen("device_control")
}

object NavRoutes {
    const val TAB_ASSISTANT = 0
    const val TAB_COMMANDS = 1
    const val TAB_FEEDS = 2
    const val TAB_CONFIG = 3

    const val HOME = "home"
    const val SYSTEM_CONTROL = "system_control"
    const val OFFLINE_DASHBOARD = "offline_dashboard"
    const val SETTINGS = "settings"
    const val EXTRAS = "extras"
    const val ONBOARDING = "onboarding"
    const val THEME_SETTINGS = "theme_settings"
    const val ACCESSIBILITY = "accessibility"
    const val WAKE_WORD_TRAINER = "wake_word_trainer"
    const val CHAT = "chat"
    const val DEVICE_CONTROL = "device_control"
}
