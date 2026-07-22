package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.material3.ColorScheme
import com.example.R

// ==================================================
// MASTER PALETTE - RESOLVED FROM SINGLE SOURCE OF TRUTH (colors.xml / colors-night.xml)
// ==================================================
val PrimaryBlue: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_primary)
val PrimaryPressed: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_pressed)
val PrimaryVariant: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_primary_variant)
val InfoBlue: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_info)

val BackgroundColor: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_bg_light)
val SurfaceColor: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_surface_light)
val SecondarySurface: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_surface_variant_light)
val SurfaceVariantColor: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_surface_variant_light)
val DividerColor: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_border_light)
val BorderColor: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_border_light)
val OutlineColor: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_border_light)

val PrimaryTextColor: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_text_primary_light)
val SecondaryTextColor: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_text_secondary_light)
val TertiaryTextColor: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_text_secondary_light)
val DisabledTextColor: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_disabled_light)

val SuccessColor: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_success)
val WarningColor: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_warning)
val ErrorColor: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_error)

val DarkBackgroundColor: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_bg_dark)
val DarkSurfaceColor: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_surface_dark)
val DarkSurfaceVariantColor: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_surface_variant_dark)
val DarkDividerColor: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_border_dark)
val DarkBorderColor: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_border_dark)
val DarkPrimaryTextColor: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_text_primary_dark)
val DarkSecondaryTextColor: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_text_secondary_dark)
val DarkTertiaryTextColor: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_text_secondary_dark)
val DarkDisabledTextColor: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_disabled_dark)

val ColorScheme.success: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_success)
val ColorScheme.warning: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_warning)

object AiraLightColors {
    val Primary: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_primary)
    val PrimaryVariant: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_primary_variant)
    val Pressed: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_pressed)
    val Background: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_bg_light)
    val Surface: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_surface_light)
    val SecondarySurface: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_surface_variant_light)
    val TextPrimary: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_text_primary_light)
    val TextSecondary: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_text_secondary_light)
    val Border: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_border_light)
    val Success: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_success)
    val Warning: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_warning)
    val Error: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_error)
    val Accent: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_info)
}

object AiraDarkColors {
    val Primary: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_primary_dark)
    val Background: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_bg_dark)
    val Surface: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_surface_dark)
    val SecondarySurface: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_surface_variant_dark)
    val TextPrimary: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_text_primary_dark)
    val TextSecondary: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_text_secondary_dark)
    val Border: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_border_dark)
    val Success: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_success)
    val Warning: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_warning)
    val Error: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_error)
    val Accent: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_info)
}

object AiraAccent {
    val LightHighlight: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_info)
    val DarkHighlight: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_info)
    val PrimaryAccent: Color @Composable @ReadOnlyComposable get() = colorResource(R.color.aira_primary)
}
