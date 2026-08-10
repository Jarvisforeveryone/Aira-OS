package com.example.presentation.navigation

sealed class ScreenRoute(val route: String) {
    object Home : ScreenRoute("home")
    object SystemControl : ScreenRoute("system_control")
    object OfflineDashboard : ScreenRoute("offline_dashboard")
    object Settings : ScreenRoute("settings")
    object Extras : ScreenRoute("extras")
    object Onboarding : ScreenRoute("onboarding")
    object ThemeSettings : ScreenRoute("theme_settings")
    object Accessibility : ScreenRoute("accessibility")
    object WakeWordTrainer : ScreenRoute("wake_word_trainer")
}

object NavDestinations {
    const val HOME = "home"
    const val SYSTEM_CONTROL = "system_control"
    const val OFFLINE_DASHBOARD = "offline_dashboard"
    const val SETTINGS = "settings"
    const val EXTRAS = "extras"
    const val ONBOARDING = "onboarding"
    const val THEME_SETTINGS = "theme_settings"
    const val ACCESSIBILITY = "accessibility"
    const val WAKE_WORD_TRAINER = "wake_word_trainer"
}
