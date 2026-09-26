package com.example.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * UNIVERSAL RESPONSIVE UI SYSTEM - ScreenUtils
 * Standardized adaptive breakpoint engine and layout scale manager.
 */
enum class ScreenCategory {
    SMALL,   // width < 360dp
    MEDIUM,  // 360dp <= width < 600dp
    LARGE,   // 600dp <= width < 840dp
    TABLET   // width >= 840dp
}

data class AdaptiveValues(
    val category: ScreenCategory,
    val padding: Dp,
    val fontSize: TextUnit,
    val cornerRadius: Dp,
    val elevation: Dp,
    val gridColumns: Int,
    val buttonHeight: Dp,
    val buttonFontSize: TextUnit,
    val cardPadding: Dp,
    val inputHeight: Dp
)

object ScreenUtils {

    @Composable
    @ReadOnlyComposable
    fun currentScreenCategory(): ScreenCategory {
        val widthDp = LocalConfiguration.current.screenWidthDp
        return when {
            widthDp < 360 -> ScreenCategory.SMALL
            widthDp < 600 -> ScreenCategory.MEDIUM
            widthDp < 840 -> ScreenCategory.LARGE
            else -> ScreenCategory.TABLET
        }
    }

    @Composable
    @ReadOnlyComposable
    fun adaptiveValues(): AdaptiveValues {
        val category = currentScreenCategory()
        return when (category) {
            ScreenCategory.SMALL -> AdaptiveValues(
                category = category,
                padding = 12.dp,
                fontSize = 14.sp,
                cornerRadius = 10.dp,
                elevation = 2.dp,
                gridColumns = 1,
                buttonHeight = 44.dp,
                buttonFontSize = 13.sp,
                cardPadding = 12.dp,
                inputHeight = 48.dp
            )
            ScreenCategory.MEDIUM -> AdaptiveValues(
                category = category,
                padding = 16.dp,
                fontSize = 16.sp,
                cornerRadius = 12.dp,
                elevation = 3.dp,
                gridColumns = 2,
                buttonHeight = 48.dp,
                buttonFontSize = 14.sp,
                cardPadding = 16.dp,
                inputHeight = 52.dp
            )
            ScreenCategory.LARGE -> AdaptiveValues(
                category = category,
                padding = 20.dp,
                fontSize = 18.sp,
                cornerRadius = 14.dp,
                elevation = 4.dp,
                gridColumns = 3,
                buttonHeight = 52.dp,
                buttonFontSize = 15.sp,
                cardPadding = 20.dp,
                inputHeight = 56.dp
            )
            ScreenCategory.TABLET -> AdaptiveValues(
                category = category,
                padding = 24.dp,
                fontSize = 20.sp,
                cornerRadius = 16.dp,
                elevation = 6.dp,
                gridColumns = 4,
                buttonHeight = 56.dp,
                buttonFontSize = 16.sp,
                cardPadding = 24.dp,
                inputHeight = 60.dp
            )
        }
    }
}
