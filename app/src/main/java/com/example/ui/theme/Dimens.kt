package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object Dimens {
    // Accessibility Touch Targets
    val MinTouchTarget = 48.dp
    val ItemMinHeight = 56.dp

    // Screen Padding (Standard 16dp / 24dp)
    val ScreenHorizontalPadding = 16.dp
    val ScreenVerticalPadding = 16.dp
    val ScreenPadding = 16.dp

    // Cards (Standard 16dp inside padding)
    val CardPadding = 16.dp
    val InsideCardPadding: Dp get() = CardPadding
    val CardGap = 16.dp

    // Gaps & Spacing (Strict 8dp grid)
    val GapMicro = 2.dp
    val GapTiny = 4.dp
    val GapSmall = 8.dp
    val GapMedium = 12.dp
    val GapLarge = 16.dp
    val GapExtraLarge = 24.dp
    val GapHuge = 32.dp

    // Icon Sizes
    val IconSmall = 16.dp
    val IconMedium = 20.dp
    val IconStandard = 24.dp
    val IconLarge = 32.dp

    // Lists
    val ListVerticalSpacing = 12.dp
    val ListHorizontalSpacing = 16.dp

    // Buttons
    val ButtonHorizontalPadding = 20.dp
    val ButtonVerticalPadding = 12.dp
    val ButtonGap = 12.dp

    // Input Fields
    val InputLabelGap = 8.dp
    val InputMultiFieldGap = 16.dp

    // Chips
    val ChipHorizontalSpacing = 8.dp
    val ChipVerticalSpacing = 8.dp

    // Sections
    val SectionTitleGap = 16.dp
    val SectionMajorGap = 24.dp
    val ElementSpacing = 8.dp
    val SectionSpacing = 24.dp

    // Dialogs & Bottom Sheets
    val DialogPadding = 24.dp
    val DialogTitleBodyGap = 12.dp
    val DialogBodyButtonGap = 24.dp

    // Standard M3 Corner Radii
    val CornerRadiusExtraSmall = 4.dp
    val CornerRadiusSmall = 8.dp
    val CornerRadiusMedium = 12.dp
    val CornerRadiusLarge = 16.dp
    val CornerRadiusExtraLarge = 28.dp

    // Elevations
    val ElevationLow = 2.dp
    val ElevationMedium = 6.dp
    val ElevationHigh = 12.dp

    /**
     * Dynamic responsive screen padding based on screenWidth / 20.
     */
    val responsiveScreenPadding: Dp
        @Composable
        @ReadOnlyComposable
        get() {
            val widthDp = LocalConfiguration.current.screenWidthDp
            return (widthDp / 20f).coerceIn(12f, 32f).dp
        }
}


