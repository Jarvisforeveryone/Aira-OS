package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ThemeRepository

val AiraShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

val AiraTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
fun AiraTheme(
    themeIndex: Int = 0,
    customColorHex: String = "#2563EB",
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Resolve the primary color from the selected theme, with fallback to customColorHex parsing
    val primaryColor = try {
        val themeColor = ThemeRepository.themes.getOrNull(themeIndex)?.color
        themeColor ?: if (customColorHex.startsWith("#") && (customColorHex.length == 7 || customColorHex.length == 9)) {
            Color(android.graphics.Color.parseColor(customColorHex))
        } else {
            PrimaryBlue
        }
    } catch (e: Exception) {
        PrimaryBlue
    }

    // Determine the base schemes based on the target dark/light mode and primary color choice
    val baseScheme = if (darkTheme) {
        darkColorScheme(
            primary = primaryColor,
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFF1E40AF), // Dark blue container
            onPrimaryContainer = Color(0xFFDBEAFE),
            secondary = AiraDarkColors.TextSecondary,
            onSecondary = AiraDarkColors.Surface,
            secondaryContainer = Color(0xFF1F2937),
            onSecondaryContainer = Color(0xFF94A3B8),
            tertiary = AiraDarkColors.TextSecondary,
            onTertiary = AiraDarkColors.Surface,
            background = Color(0xFF0B1120),
            onBackground = Color(0xFFFFFFFF),
            surface = Color(0xFF111827),
            onSurface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFF1E293B),
            onSurfaceVariant = AiraDarkColors.TextSecondary,
            outline = AiraDarkColors.Border,
            outlineVariant = AiraDarkColors.Border,
            error = AiraDarkColors.Error,
            onError = AiraDarkColors.TextPrimary,
            errorContainer = AiraDarkColors.Error.copy(alpha = 0.16f),
            onErrorContainer = AiraDarkColors.Error,
            // COLOR SYSTEM v1
            inverseSurface = AiraLightColors.Surface,
            inverseOnSurface = AiraDarkColors.Background,
            inversePrimary = AiraDarkColors.Primary
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFDBEAFE), // Light blue container
            onPrimaryContainer = Color(0xFF1E40AF),
            secondary = AiraLightColors.TextSecondary,
            onSecondary = AiraLightColors.Surface,
            secondaryContainer = Color(0xFFF1F5F9),
            onSecondaryContainer = Color(0xFF475569),
            tertiary = AiraLightColors.TextSecondary,
            onTertiary = AiraLightColors.Surface,
            background = Color(0xFFFBFAFC),
            onBackground = Color(0xFF0F172A),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF0F172A),
            surfaceVariant = Color(0xFFE2E8F0),
            onSurfaceVariant = AiraLightColors.TextSecondary,
            outline = AiraLightColors.Border,
            outlineVariant = AiraLightColors.Border,
            error = AiraLightColors.Error,
            onError = AiraLightColors.Surface,
            errorContainer = AiraLightColors.Error.copy(alpha = 0.12f),
            onErrorContainer = AiraLightColors.Error,
            inverseSurface = AiraDarkColors.Background,
            inverseOnSurface = AiraLightColors.Background,
            inversePrimary = AiraDarkColors.Primary
        )
    }

    // Smoothly animate between color schemes to support natural transitions
    val animatedScheme = animateColorScheme(baseScheme)

    // Selection Colors override: Light = 0.18f alpha selection, Dark = 0.22f alpha selection
    val customTextSelectionColors = TextSelectionColors(
        handleColor = animatedScheme.primary,
        backgroundColor = animatedScheme.primary.copy(alpha = if (darkTheme) 0.22f else 0.18f)
    )

    CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
        MaterialTheme(
            colorScheme = animatedScheme,
            shapes = AiraShapes,
            typography = AiraTypography,
            content = content
        )
    }
}

@Composable
fun animateColorScheme(targetColorScheme: ColorScheme): ColorScheme {
    val animationSpec = tween<Color>(durationMillis = 250)

    val background = animateColorAsState(targetColorScheme.background, animationSpec, label = "background").value
    val surface = animateColorAsState(targetColorScheme.surface, animationSpec, label = "surface").value
    val surfaceVariant = animateColorAsState(targetColorScheme.surfaceVariant, animationSpec, label = "surfaceVariant").value
    val primary = animateColorAsState(targetColorScheme.primary, animationSpec, label = "primary").value
    val secondary = animateColorAsState(targetColorScheme.secondary, animationSpec, label = "secondary").value
    val tertiary = animateColorAsState(targetColorScheme.tertiary, animationSpec, label = "tertiary").value
    val outline = animateColorAsState(targetColorScheme.outline, animationSpec, label = "outline").value
    val outlineVariant = animateColorAsState(targetColorScheme.outlineVariant, animationSpec, label = "outlineVariant").value
    val onBackground = animateColorAsState(targetColorScheme.onBackground, animationSpec, label = "onBackground").value
    val onSurface = animateColorAsState(targetColorScheme.onSurface, animationSpec, label = "onSurface").value
    val onPrimary = animateColorAsState(targetColorScheme.onPrimary, animationSpec, label = "onPrimary").value
    val onSecondary = animateColorAsState(targetColorScheme.onSecondary, animationSpec, label = "onSecondary").value
    val onSurfaceVariant = animateColorAsState(targetColorScheme.onSurfaceVariant, animationSpec, label = "onSurfaceVariant").value
    val inverseSurface = animateColorAsState(targetColorScheme.inverseSurface, animationSpec, label = "inverseSurface").value
    val inverseOnSurface = animateColorAsState(targetColorScheme.inverseOnSurface, animationSpec, label = "inverseOnSurface").value
    val inversePrimary = animateColorAsState(targetColorScheme.inversePrimary, animationSpec, label = "inversePrimary").value
    val scrim = animateColorAsState(targetColorScheme.scrim, animationSpec, label = "scrim").value
    val error = animateColorAsState(targetColorScheme.error, animationSpec, label = "error").value
    val onError = animateColorAsState(targetColorScheme.onError, animationSpec, label = "onError").value

    return targetColorScheme.copy(
        primary = primary,
        onPrimary = onPrimary,
        secondary = secondary,
        onSecondary = onSecondary,
        tertiary = tertiary,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        outline = outline,
        outlineVariant = outlineVariant,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
        inversePrimary = inversePrimary,
        scrim = scrim,
        error = error,
        onError = onError
    )
}
