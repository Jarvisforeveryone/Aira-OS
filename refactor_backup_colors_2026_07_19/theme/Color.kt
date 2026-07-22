package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ColorScheme

// ==================================================
// MASTER PALETTE - LIGHT MODE
// ==================================================
val PrimaryBlue = Color(0xFF2563EB)
val PrimaryPressed = Color(0xFF1E40AF)
val PrimaryVariant = Color(0xFF1D4ED8)
val InfoBlue = Color(0xFF2563EB)

val BackgroundColor = Color(0xFFF8FAFC)
val SurfaceColor = Color(0xFFFFFFFF)
val SecondarySurface = Color(0xFFF1F5F9)
val SurfaceVariantColor = Color(0xFFE2E8F0)
val DividerColor = Color(0xFFE2E8F0)
val BorderColor = Color(0xFFE2E8F0)
val OutlineColor = Color(0xFFE2E8F0)

val PrimaryTextColor = Color(0xFF0F172A)
val SecondaryTextColor = Color(0xFF475569)
val TertiaryTextColor = Color(0xFF64748B)
val DisabledTextColor = Color(0xFFCBD5E1)

val SuccessColor = Color(0xFF16A34A)
val WarningColor = Color(0xFFF59E0B)
val ErrorColor = Color(0xFFDC2626)

// ==================================================
// MASTER PALETTE - DARK MODE
// ==================================================
val DarkBackgroundColor = Color(0xFF090D16)
val DarkSurfaceColor = Color(0xFF111827)
val DarkSurfaceVariantColor = Color(0xFF1E293B)
val DarkDividerColor = Color(0xFF334155)
val DarkBorderColor = Color(0xFF334155)
val DarkPrimaryTextColor = Color(0xFFF8FAFC)
val DarkSecondaryTextColor = Color(0xFFCBD5E1)
val DarkTertiaryTextColor = Color(0xFF94A3B8)
val DarkDisabledTextColor = Color(0xFF4B5563)

// ==================================================
// CENTRALIZED THEME EXTENSIONS
// ==================================================
val ColorScheme.success: Color get() = SuccessColor
val ColorScheme.warning: Color get() = WarningColor

object AiraLightColors {
    val Primary = Color(0xFF2563EB)
    val PrimaryVariant = Color(0xFF1D4EDB)
    val Pressed = Color(0xFF1E40AF)
    val Background = Color(0xFFFBFAFC)
    val Surface = Color(0xFFFFFFFF)
    val SecondarySurface = Color(0xFFF1F5F9)
    val TextPrimary = Color(0xFF0F172A)
    val TextSecondary = Color(0xFF475569)
    val Border = Color(0xFFE2E8F0)
    val Success = Color(0xFF16A34A)
    val Warning = Color(0xFFF59E0B)
    val Error = Color(0xFFDC2626)
    val Accent = Color(0xFF0EA5E9)
}

object AiraDarkColors {
    val Primary = Color(0xFF2563EB)
    val Background = Color(0xFF0B1120)
    val Surface = Color(0xFF111827)
    val SecondarySurface = Color(0xFF1F2937)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF94A3B8)
    val Border = Color(0xFF1E293B)
    val Success = Color(0xFF16A34A)
    val Warning = Color(0xFFF59E0B)
    val Error = Color(0xFFDC2626)
    val Accent = Color(0xFF38BDF8)
}

object AiraAccent {
    val LightHighlight = Color(0xFF0EA5E9)
    val DarkHighlight = Color(0xFF38BDF8)
    val PrimaryAccent = Color(0xFF2563EB)
}


