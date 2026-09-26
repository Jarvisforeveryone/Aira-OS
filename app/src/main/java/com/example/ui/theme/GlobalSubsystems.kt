package com.example.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

// Global Navigation system alias
typealias NavRoutes = com.example.ui.navigation.NavRoutes

// ==============================================================================
// GLOBAL SUBSYSTEMS ARCHITECTURE
// Single Source of Truth for Design Tokens, Constants, Logic & Configurations
// ==============================================================================

/**
 * SUBSYSTEM 1: COLORS
 */
object AiraColors {
    val BluePrimary = Color(0xFF2563EB)
    val BlueSecondary = Color(0xFF1D4ED8)
    val BackgroundLight = Color(0xFFFBFAFC)
    val BackgroundDark = Color(0xFF0F172A)
    val SurfaceLight = Color(0xFFFFFFFF)
    val SurfaceDark = Color(0xFF1E293B)
    val TextPrimaryLight = Color(0xFF0F172A)
    val TextSecondaryLight = Color(0xFF6B7280)
    val TextPrimaryDark = Color(0xFFFFFFFF)
    val TextSecondaryDark = Color(0xFF9CA3AF)
    val Error = Color(0xFFDC2626)
    val Warning = Color(0xFFF59E0B)
    val Success = Color(0xFF16A34A)
    val Border = Color(0xFFE2E8F0)
}

/**
 * SUBSYSTEM 2: TYPOGRAPHY
 */
object TypographySubsystem {
    val FontMain = Inter
    
    val SizeDisplay: TextUnit = 28.sp
    val SizeTitleLarge: TextUnit = 24.sp
    val SizeTitleMedium: TextUnit = 20.sp
    val SizeSubheading: TextUnit = 18.sp
    val SizeBodyLarge: TextUnit = 16.sp
    val SizeBodyMedium: TextUnit = 14.sp
    val SizeCaption: TextUnit = 12.sp
    val SizeTiny: TextUnit = 10.sp

    val WeightBold = FontWeight.Bold
    val WeightSemiBold = FontWeight.SemiBold
    val WeightMedium = FontWeight.Medium
    val WeightRegular = FontWeight.Normal
}

/**
 * SUBSYSTEM 3: SPACING
 */
object Spacing {
    val ExtraLarge: Dp = 24.dp
    val Large: Dp = 20.dp
    val Medium: Dp = 16.dp
    val Small: Dp = 12.dp
    val ExtraSmall: Dp = 8.dp
    val Tiny: Dp = 4.dp
}

/**
 * SUBSYSTEM 5: STRINGS
 */
object AppStrings {
    val AppName = R.string.app_name
    const val DefaultGreeting = "Good morning, I'm Aira"
    const val DefaultSubtitle = "How can I help you today?"
    const val VoiceLabel = "Voice: Amy"
    const val AppBrand = "Aira"
}

/**
 * SUBSYSTEM 6: DIMENSIONS
 */
object Dimensions {
    val IconSizeSmall: Dp = 16.dp
    val IconSizeMedium: Dp = 20.dp
    val IconSizeLarge: Dp = 24.dp
    val IconSizeExtraLarge: Dp = 32.dp
    
    val OrbSize: Dp = 200.dp
    val TouchTargetMin: Dp = 48.dp
    val BottomBarHeight: Dp = 64.dp
    val CardElevation: Dp = 2.dp
}

/**
 * SUBSYSTEM 7: THEME CONFIGURATION
 */
object AiraThemeConfig {
    const val THEME_SYSTEM = 0
    const val THEME_LIGHT = 1
    const val THEME_DARK = 2
}

/**
 * SUBSYSTEM 9: API
 */
object ApiConstants {
    const val DEFAULT_TIMEOUT_MS = 15000L
    const val CACHE_MAX_SIZE_BYTES = 50 * 1024 * 1024L // 50MB
}

/**
 * SUBSYSTEM 10: PERMISSIONS
 */
object Permissions {
    const val RECORD_AUDIO = android.Manifest.permission.RECORD_AUDIO
    const val CALL_PHONE = android.Manifest.permission.CALL_PHONE
    const val CAMERA = android.Manifest.permission.CAMERA
}

/**
 * SUBSYSTEM 11: BUSINESS LOGIC
 */
object BusinessLogic {
    const val MAX_RECENT_CONVERSATIONS = 10
    const val WAKE_WORD_SENSITIVITY = 0.5f
}

/**
 * SUBSYSTEM 12: DURATIONS
 */
object Durations {
    const val ShortMs = 150
    const val MediumMs = 300
    const val LongMs = 500
    const val RippleMs = 200
}

/**
 * SUBSYSTEM 13: RADIUS
 */
object Radius {
    val Small: Dp = 8.dp
    val Medium: Dp = 12.dp
    val Large: Dp = 16.dp
    val ExtraLarge: Dp = 20.dp
    val FullPill: Dp = 999.dp
}

/**
 * SUBSYSTEM 14: ELEVATIONS
 */
object Elevations {
    val None: Dp = 0.dp
    val Low: Dp = 2.dp
    val Medium: Dp = 4.dp
    val High: Dp = 8.dp
}

/**
 * SUBSYSTEM 15: OPACITIES
 */
object Opacities {
    const val Disabled = 0.38f
    const val Divider = 0.12f
    const val SubtleChipBg = 0.15f
    const val ActivePillBg = 0.12f
    const val Full = 1.0f
}

/**
 * SUBSYSTEM 16: ANIMATIONS
 */
object Animations {
    val EasingFastOutSlowIn: Easing = FastOutSlowInEasing
    val EasingLinearOutSlowIn: Easing = LinearOutSlowInEasing
    val DefaultFadeTween = tween<Float>(durationMillis = Durations.MediumMs, easing = FastOutSlowInEasing)
}

/**
 * SUBSYSTEM 17: Z-INDEX
 */
object ZIndex {
    const val Base = 0f
    const val CardOverlay = 1f
    const val FloatingButton = 5f
    const val ModalDialog = 10f
}

/**
 * SUBSYSTEM 18: BREAKPOINTS
 */
object Breakpoints {
    val CompactWidthMax: Dp = 600.dp
    val MediumWidthMax: Dp = 840.dp
}

/**
 * SUBSYSTEM 19: LOGGING
 */
object Logging {
    const val TAG_MAIN = "AiraMainActivity"
    const val TAG_VIEWMODEL = "AiraViewModel"
    const val TAG_TTS = "PiperTtsManager"
}

/**
 * SUBSYSTEM 20: MESSAGES
 */
object Messages {
    const val OfflineActive = "Offline Mode Active"
    const val DeletedSuccessfully = "Deleted successfully"
    const val ErrorGeneric = "An unexpected error occurred"
}

/**
 * SUBSYSTEM 21: VALIDATION
 */
object Validation {
    const val MIN_WAKE_WORD_LENGTH = 2
    const val MAX_INPUT_LENGTH = 500
}

/**
 * SUBSYSTEM 22: DEFAULTS
 */
object Defaults {
    const val DEFAULT_TTS_PITCH = 1.0f
    const val DEFAULT_TTS_RATE = 1.0f
}

/**
 * SUBSYSTEM 23: CONFIG
 */
object Config {
    const val IS_OFFLINE_BRAIN_PREFERRED = true
    const val ENABLE_ANALYTICS_LOGGING = false
}

/**
 * SUBSYSTEM 24: PATHS
 */
object Paths {
    const val VOSK_MODEL_DIR = "vosk-model-small-en-us-0.15"
    const val PIPER_TTS_DIR = "piper_models"
}

/**
 * SUBSYSTEM 25: ERROR HANDLING
 */
object ErrorHandler {
    fun formatErrorMessage(raw: String?): String {
        return raw?.ifBlank { Messages.ErrorGeneric } ?: Messages.ErrorGeneric
    }
}
