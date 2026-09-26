package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object Dimens {
    // Spacing & Padding (Standard increments)
    val SpaceZero = 0.dp
    val SpaceExtraSmall = 4.dp
    val SpaceSmall = 8.dp
    val SpaceMedium = 12.dp
    val SpaceDefault = 16.dp   // This is the primary padding for most cards
    val SpaceLarge = 20.dp
    val SpaceExtraLarge = 24.dp
    val SpaceHuge = 32.dp

    // Heights (Card & Component)
    val CardHeightSmall = 56.dp
    val CardHeightMedium = 64.dp
    val CardHeightDefault = 72.dp   // Standard toggle cards
    val CardHeightLarge = 100.dp    // Weather / Info cards
    val CardHeightExtraLarge = 140.dp

    // Corner Radius
    val RadiusSmall = 8.dp
    val RadiusDefault = 12.dp
    val RadiusLarge = 16.dp

    // Icon Sizes
    val IconSmall = 20.dp
    val IconDefault = 28.dp
    val IconLarge = 36.dp

    // Accessibility Touch Targets & Aliases
    val MinTouchTarget = 48.dp
    val ItemMinHeight = 56.dp

    // Screen Padding
    val ScreenHorizontalPadding = SpaceDefault
    val ScreenVerticalPadding = SpaceDefault
    val ScreenPadding = SpaceDefault

    // Cards
    val CardPadding = SpaceDefault
    val InsideCardPadding: Dp get() = CardPadding
    val CardGap = SpaceDefault

    // Gaps & Spacing
    val GapMicro = 2.dp
    val GapTiny = SpaceExtraSmall
    val GapSmall = SpaceSmall
    val GapMedium = SpaceMedium
    val GapLarge = SpaceDefault
    val GapExtraLarge = SpaceExtraLarge
    val GapHuge = SpaceHuge

    // Icon Standards
    val IconMedium = 20.dp
    val IconStandard = 24.dp

    // Lists
    val ListVerticalSpacing = SpaceMedium
    val ListHorizontalSpacing = SpaceDefault

    // Buttons
    val ButtonHorizontalPadding = SpaceLarge
    val ButtonVerticalPadding = SpaceMedium
    val ButtonGap = SpaceMedium

    // Input Fields
    val InputLabelGap = SpaceSmall
    val InputMultiFieldGap = SpaceDefault

    // Chips
    val ChipHorizontalSpacing = SpaceSmall
    val ChipVerticalSpacing = SpaceSmall

    // Sections
    val SectionTitleGap = SpaceDefault
    val SectionMajorGap = SpaceExtraLarge
    val ElementSpacing = SpaceSmall
    val SectionSpacing = SpaceExtraLarge

    // Dialogs & Bottom Sheets
    val DialogPadding = SpaceExtraLarge
    val DialogTitleBodyGap = SpaceMedium
    val DialogBodyButtonGap = SpaceExtraLarge

    // Standard M3 Corner Radii
    val CornerRadiusExtraSmall = 4.dp
    val CornerRadiusSmall = RadiusSmall
    val CornerRadiusMedium = RadiusDefault
    val CornerRadiusLarge = RadiusLarge
    val CornerRadiusExtraLarge = 28.dp

    // Elevations
    val ElevationLow = 2.dp
    val ElevationMedium = 6.dp
    val ElevationHigh = 12.dp

    // Design System Aliases & Standards
    val CardCornerRadius = 22.dp
    val RadiusMedium = 14.dp
    val CardElevation = ElevationLow
    val BorderThin = 1.dp
    val BorderMedium = 1.5.dp
    val BorderThick = 2.dp
    val PaddingScreenOuter = SpaceDefault
    val PaddingCardInner = SpaceLarge
    val ButtonHeightSmall = 36.dp
    val CardHeightExtraSmall = 40.dp

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



