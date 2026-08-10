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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ThemeRepository

val AiraShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
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
    appTheme: String = "light",
    content: @Composable () -> Unit
) {
    val darkTheme = (appTheme == "dark")

    // Resolve standard resource colors
    val primaryColorRes = colorResource(id = if (darkTheme) R.color.aira_primary_dark else R.color.aira_primary)
    val onPrimaryColorRes = colorResource(id = R.color.aira_surface_light)
    val primaryContainerColorRes = colorResource(id = if (darkTheme) R.color.aira_surface_variant_dark else R.color.aira_surface_variant_light)
    val onPrimaryContainerColorRes = colorResource(id = if (darkTheme) R.color.aira_text_primary_dark else R.color.aira_text_primary_light)
    val secondaryColorRes = colorResource(id = if (darkTheme) R.color.aira_text_secondary_dark else R.color.aira_text_secondary_light)
    val onSecondaryColorRes = colorResource(id = if (darkTheme) R.color.aira_surface_light else R.color.aira_surface_light)
    val secondaryContainerColorRes = colorResource(id = if (darkTheme) R.color.aira_surface_variant_dark else R.color.aira_surface_variant_light)
    val onSecondaryContainerColorRes = secondaryColorRes
    val tertiaryColorRes = secondaryColorRes
    val onTertiaryColorRes = onSecondaryColorRes
    val backgroundColorRes = colorResource(id = if (darkTheme) R.color.aira_bg_dark else R.color.aira_bg_light)
    val onBackgroundColorRes = colorResource(id = if (darkTheme) R.color.aira_text_primary_dark else R.color.aira_text_primary_light)
    val surfaceColorRes = colorResource(id = if (darkTheme) R.color.aira_surface_dark else R.color.aira_surface_light)
    val onSurfaceColorRes = onBackgroundColorRes
    val surfaceVariantColorRes = colorResource(id = if (darkTheme) R.color.aira_surface_variant_dark else R.color.aira_surface_variant_light)
    val onSurfaceVariantColorRes = secondaryColorRes
    val outlineColorRes = colorResource(id = if (darkTheme) R.color.aira_border_dark else R.color.aira_border_light)
    val outlineVariantColorRes = outlineColorRes
    val errorColorRes = colorResource(id = R.color.aira_error)
    val onErrorColorRes = colorResource(id = R.color.aira_surface_light)
    val errorContainerColorRes = colorResource(id = if (darkTheme) R.color.aira_surface_variant_dark else R.color.aira_surface_variant_light)
    val onErrorContainerColorRes = colorResource(id = R.color.aira_error)
    val inverseSurfaceColorRes = colorResource(id = if (darkTheme) R.color.aira_surface_light else R.color.aira_surface_dark)
    val inverseOnSurfaceColorRes = colorResource(id = if (darkTheme) R.color.aira_bg_light else R.color.aira_bg_dark)
    val inversePrimaryColorRes = colorResource(id = if (darkTheme) R.color.aira_primary else R.color.aira_primary_dark)

    val themeResId = ThemeRepository.themes.getOrNull(themeIndex)?.colorResId
    val themeColor = themeResId?.let { colorResource(id = it) }

    // Resolve the primary color from the selected design system theme
    val primaryColor = themeColor ?: primaryColorRes

    // M3 Surface Container definitions (Dark vs Light)
    val surfaceLowest = if (darkTheme) Color(0xFF0F1115) else Color(0xFFFFFFFF)
    val surfaceLow = if (darkTheme) Color(0xFF1A1D24) else Color(0xFFF4F6F9)
    val surfaceContainer = if (darkTheme) Color(0xFF21252F) else Color(0xFFEDF0F5)
    val surfaceHigh = if (darkTheme) Color(0xFF2B303C) else Color(0xFFE5E9EF)
    val surfaceHighest = if (darkTheme) Color(0xFF363C4A) else Color(0xFFDCDFE6)

    val baseScheme = if (darkTheme) {
        darkColorScheme(
            primary = primaryColor,
            onPrimary = onPrimaryColorRes,
            primaryContainer = primaryContainerColorRes,
            onPrimaryContainer = onPrimaryContainerColorRes,
            secondary = secondaryColorRes,
            onSecondary = onSecondaryColorRes,
            secondaryContainer = secondaryContainerColorRes,
            onSecondaryContainer = onSecondaryContainerColorRes,
            tertiary = tertiaryColorRes,
            onTertiary = onTertiaryColorRes,
            background = backgroundColorRes,
            onBackground = onBackgroundColorRes,
            surface = surfaceColorRes,
            onSurface = onSurfaceColorRes,
            surfaceVariant = surfaceVariantColorRes,
            onSurfaceVariant = onSurfaceVariantColorRes,
            surfaceContainerLowest = surfaceLowest,
            surfaceContainerLow = surfaceLow,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceHigh,
            surfaceContainerHighest = surfaceHighest,
            outline = outlineColorRes,
            outlineVariant = outlineVariantColorRes,
            error = errorColorRes,
            onError = onErrorColorRes,
            errorContainer = errorContainerColorRes,
            onErrorContainer = onErrorContainerColorRes,
            inverseSurface = inverseSurfaceColorRes,
            inverseOnSurface = inverseOnSurfaceColorRes,
            inversePrimary = inversePrimaryColorRes
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            onPrimary = onPrimaryColorRes,
            primaryContainer = primaryContainerColorRes,
            onPrimaryContainer = onPrimaryContainerColorRes,
            secondary = secondaryColorRes,
            onSecondary = onSecondaryColorRes,
            secondaryContainer = secondaryContainerColorRes,
            onSecondaryContainer = onSecondaryContainerColorRes,
            tertiary = tertiaryColorRes,
            onTertiary = onTertiaryColorRes,
            background = backgroundColorRes,
            onBackground = onBackgroundColorRes,
            surface = surfaceColorRes,
            onSurface = onSurfaceColorRes,
            surfaceVariant = surfaceVariantColorRes,
            onSurfaceVariant = onSurfaceVariantColorRes,
            surfaceContainerLowest = surfaceLowest,
            surfaceContainerLow = surfaceLow,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceHigh,
            surfaceContainerHighest = surfaceHighest,
            outline = outlineColorRes,
            outlineVariant = outlineVariantColorRes,
            error = errorColorRes,
            onError = onErrorColorRes,
            errorContainer = errorContainerColorRes,
            onErrorContainer = onErrorContainerColorRes,
            inverseSurface = inverseSurfaceColorRes,
            inverseOnSurface = inverseOnSurfaceColorRes,
            inversePrimary = inversePrimaryColorRes
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
    val surfaceContainerLowest = animateColorAsState(targetColorScheme.surfaceContainerLowest, animationSpec, label = "surfaceLowest").value
    val surfaceContainerLow = animateColorAsState(targetColorScheme.surfaceContainerLow, animationSpec, label = "surfaceLow").value
    val surfaceContainer = animateColorAsState(targetColorScheme.surfaceContainer, animationSpec, label = "surfaceContainer").value
    val surfaceContainerHigh = animateColorAsState(targetColorScheme.surfaceContainerHigh, animationSpec, label = "surfaceHigh").value
    val surfaceContainerHighest = animateColorAsState(targetColorScheme.surfaceContainerHighest, animationSpec, label = "surfaceHighest").value
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
        surfaceContainerLowest = surfaceContainerLowest,
        surfaceContainerLow = surfaceContainerLow,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
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
