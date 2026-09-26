package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * GLOBAL ICON COLOR SYSTEM
 * Single source of truth for semantic icon color mapping across AIRA.
 * Maps specific application functions (Voice Activation, HUD Elements, System Notifications,
 * Navigation, Accessibility, System Controls) to the unified global theme palette.
 */
object IconColors {

    // ==============================================================================
    // CORE SYSTEM & MATERIAL THEME MAPPINGS
    // ==============================================================================
    val Primary: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary

    val Secondary: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.secondary

    val Success: Color
        @Composable @ReadOnlyComposable
        get() = AiraLightColors.Success

    val Warning: Color
        @Composable @ReadOnlyComposable
        get() = AiraLightColors.Warning

    val Error: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.error

    val Info: Color
        @Composable @ReadOnlyComposable
        get() = AiraLightColors.Accent

    val OnSurface: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onSurface

    val OnSurfaceVariant: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onSurfaceVariant


    // ==============================================================================
    // FUNCTIONAL CATEGORY: VOICE ACTIVATION & AUDIO
    // ==============================================================================
    val VoiceActivation: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary

    val VoiceListening: Color
        @Composable @ReadOnlyComposable
        get() = AiraLightColors.Success

    val VoiceSpeaking: Color
        @Composable @ReadOnlyComposable
        get() = AiraLightColors.Accent

    val VoiceMuted: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.error

    val WakeWordActive: Color
        @Composable @ReadOnlyComposable
        get() = AiraLightColors.Success


    // ==============================================================================
    // FUNCTIONAL CATEGORY: HUD ELEMENTS & OVERLAYS
    // ==============================================================================
    val HudActive: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary

    val HudInactive: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onSurfaceVariant

    val HudStatusOk: Color
        @Composable @ReadOnlyComposable
        get() = AiraLightColors.Success

    val HudStatusWarning: Color
        @Composable @ReadOnlyComposable
        get() = AiraLightColors.Warning

    val HudBorder: Color
        @Composable @ReadOnlyComposable
        get() = AiraLightColors.Border

    val OrbGlow: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary


    // ==============================================================================
    // FUNCTIONAL CATEGORY: SYSTEM NOTIFICATIONS & ALERTS
    // ==============================================================================
    val NotificationInfo: Color
        @Composable @ReadOnlyComposable
        get() = AiraLightColors.Accent

    val NotificationWarning: Color
        @Composable @ReadOnlyComposable
        get() = AiraLightColors.Warning

    val NotificationError: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.error

    val NotificationSuccess: Color
        @Composable @ReadOnlyComposable
        get() = AiraLightColors.Success


    // ==============================================================================
    // FUNCTIONAL CATEGORY: SYSTEM CONTROL & ACCESSIBILITY
    // ==============================================================================
    val AccessibilityActive: Color
        @Composable @ReadOnlyComposable
        get() = AiraLightColors.Success

    val AccessibilityInactive: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onSurfaceVariant

    val DeviceAdminActive: Color
        @Composable @ReadOnlyComposable
        get() = AiraLightColors.Success

    val DeviceAdminInactive: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onSurfaceVariant

    val SystemAction: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary


    // ==============================================================================
    // FUNCTIONAL CATEGORY: NAVIGATION & CONTROLS
    // ==============================================================================
    val TabActive: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary

    val TabInactive: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onSurfaceVariant

    val NavigationBack: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onSurface

    val PrimaryActions: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary

    val SecondaryActions: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.secondary

    val Destructive: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.error

    val InactivePassive: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onSurfaceVariant


    // ==============================================================================
    // FUNCTIONAL CATEGORY: STATUS & FALLBACK INDICATORS
    // ==============================================================================
    val OnlineStatus: Color
        @Composable @ReadOnlyComposable
        get() = AiraLightColors.Success

    val OfflineStatus: Color
        @Composable @ReadOnlyComposable
        get() = AiraLightColors.Warning

    val FallbackActive: Color
        @Composable @ReadOnlyComposable
        get() = AiraLightColors.Accent

    val SystemControl: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary

    val VoiceAssistant: Color
        @Composable @ReadOnlyComposable
        get() = AiraLightColors.Accent

    val Settings: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onSurfaceVariant

    val Security: Color
        @Composable @ReadOnlyComposable
        get() = AiraLightColors.Warning

    val SuccessStatus: Color
        @Composable @ReadOnlyComposable
        get() = AiraLightColors.Success

    val ErrorStatus: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.error
}
