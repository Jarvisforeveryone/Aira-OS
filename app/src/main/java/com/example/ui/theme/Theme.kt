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

    // Resolve standard resource colors from the Aira palette system
    val aira_primary = colorResource(id = if (darkTheme) R.color.aira_primary_dark else R.color.aira_primary)
    val aira_on_primary = colorResource(id = R.color.white)
    val aira_primary_container = colorResource(id = if (darkTheme) R.color.aira_surface_variant_dark else R.color.aira_surface_variant_light)
    val aira_on_primary_container = colorResource(id = if (darkTheme) R.color.aira_text_primary_dark else R.color.aira_text_primary_light)
    val aira_secondary = colorResource(id = if (darkTheme) R.color.aira_secondary_dark else R.color.aira_secondary)
    val aira_on_secondary = colorResource(id = R.color.white)
    val aira_secondary_container = colorResource(id = if (darkTheme) R.color.aira_surface_variant_dark else R.color.aira_surface_variant_light)
    val aira_on_secondary_container = aira_secondary
    val aira_tertiary = aira_secondary
    val aira_on_tertiary = aira_on_secondary
    val aira_tertiary_container = aira_secondary_container
    val aira_on_tertiary_container = aira_on_secondary_container
    val aira_bg_light = colorResource(id = R.color.aira_bg_light)
    val aira_bg_dark = colorResource(id = R.color.aira_bg_dark)
    val aira_text_primary_light = colorResource(id = R.color.aira_text_primary_light)
    val aira_text_primary_dark = colorResource(id = R.color.aira_text_primary_dark)
    val aira_surface_light = colorResource(id = R.color.aira_surface_light)
    val aira_surface_dark = colorResource(id = R.color.aira_surface_dark)
    val aira_surface_variant_light = colorResource(id = R.color.aira_surface_variant_light)
    val aira_surface_variant_dark = colorResource(id = R.color.aira_surface_variant_dark)
    val aira_text_secondary_light = colorResource(id = R.color.aira_text_secondary_light)
    val aira_text_secondary_dark = colorResource(id = R.color.aira_text_secondary_dark)
    val aira_border_light = colorResource(id = R.color.aira_border_light)
    val aira_border_dark = colorResource(id = R.color.aira_border_dark)
    val aira_error = colorResource(id = R.color.aira_error)
    val aira_on_error = colorResource(id = R.color.white)
    val aira_error_container = if (darkTheme) aira_surface_variant_dark else aira_surface_variant_light
    val aira_on_error_container = aira_error

    val themeResId = ThemeRepository.themes.getOrNull(themeIndex)?.colorResId
    val themeColor = themeResId?.let { colorResource(id = it) }

    // Resolve the primary color from the selected design system theme
    val effectivePrimary = themeColor ?: aira_primary

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = effectivePrimary,
            onPrimary = aira_on_primary,
            primaryContainer = aira_primary_container,
            onPrimaryContainer = aira_on_primary_container,
            secondary = aira_secondary,
            onSecondary = aira_on_secondary,
            secondaryContainer = aira_secondary_container,
            onSecondaryContainer = aira_on_secondary_container,
            tertiary = aira_tertiary,
            onTertiary = aira_on_tertiary,
            tertiaryContainer = aira_tertiary_container,
            onTertiaryContainer = aira_on_tertiary_container,
            background = aira_bg_dark,
            onBackground = aira_text_primary_dark,
            surface = aira_surface_dark,
            onSurface = aira_text_primary_dark,
            surfaceVariant = aira_surface_variant_dark,
            onSurfaceVariant = aira_text_secondary_dark,
            surfaceContainerLowest = aira_bg_dark,
            surfaceContainerLow = aira_surface_dark,
            surfaceContainer = aira_surface_variant_dark,
            surfaceContainerHigh = aira_border_dark,
            surfaceContainerHighest = aira_border_dark,
            outline = aira_border_dark,
            outlineVariant = aira_border_dark,
            error = aira_error,
            onError = aira_on_error,
            errorContainer = aira_error_container,
            onErrorContainer = aira_on_error_container,
            inverseSurface = aira_surface_light,
            inverseOnSurface = aira_bg_light,
            inversePrimary = effectivePrimary
        )
    } else {
        lightColorScheme(
            primary = effectivePrimary,
            onPrimary = aira_on_primary,
            primaryContainer = aira_primary_container,
            onPrimaryContainer = aira_on_primary_container,
            secondary = aira_secondary,
            onSecondary = aira_on_secondary,
            secondaryContainer = aira_secondary_container,
            onSecondaryContainer = aira_on_secondary_container,
            tertiary = aira_tertiary,
            onTertiary = aira_on_tertiary,
            tertiaryContainer = aira_tertiary_container,
            onTertiaryContainer = aira_on_tertiary_container,
            background = aira_bg_light,
            onBackground = aira_text_primary_light,
            surface = aira_surface_light,
            onSurface = aira_text_primary_light,
            surfaceVariant = aira_surface_variant_light,
            onSurfaceVariant = aira_text_secondary_light,
            surfaceContainerLowest = aira_surface_light,
            surfaceContainerLow = aira_bg_light,
            surfaceContainer = aira_surface_variant_light,
            surfaceContainerHigh = aira_surface_variant_light,
            surfaceContainerHighest = aira_border_light,
            outline = aira_border_light,
            outlineVariant = aira_border_light,
            error = aira_error,
            onError = aira_on_error,
            errorContainer = aira_error_container,
            onErrorContainer = aira_on_error_container,
            inverseSurface = aira_surface_dark,
            inverseOnSurface = aira_bg_dark,
            inversePrimary = effectivePrimary
        )
    }

    // Smoothly animate between color schemes to support natural transitions
    val animatedScheme = animateColorScheme(colorScheme)

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
