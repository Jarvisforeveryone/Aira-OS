package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * AIRA WORLDWIDE DEVICE ADAPTIVE CONFIGURATION SYSTEM
 * Dynamic math-based calculations for layout scaling across all display sizes and form factors.
 */
data class DeviceConfigValues(
    val padding: Dp = 16.dp,
    val fontSize: TextUnit = 16.sp,
    val cornerRadius: Dp = 12.dp,
    val elevation: Dp = 2.dp
)

object DeviceConfig {
    // Default Fallback values
    val Fallback = DeviceConfigValues(
        padding = 16.dp,
        fontSize = 16.sp,
        cornerRadius = 12.dp,
        elevation = 2.dp
    )

    /**
     * Pure calculation helper with error handling & fallback.
     */
    fun calculate(screenWidthDp: Int, density: Float): DeviceConfigValues {
        return try {
            if (screenWidthDp <= 0 || density <= 0f) {
                Fallback
            } else {
                val paddingDp = (screenWidthDp / 20f).coerceIn(8f, 32f).dp
                val fontSizeSp = (screenWidthDp / 40f).coerceIn(12f, 22f).sp
                val cornerRadiusDp = (screenWidthDp / 80f).coerceIn(4f, 24f).dp
                val elevationDp = (density * 2f).coerceIn(1f, 12f).dp

                DeviceConfigValues(
                    padding = paddingDp,
                    fontSize = fontSizeSp,
                    cornerRadius = cornerRadiusDp,
                    elevation = elevationDp
                )
            }
        } catch (_: Exception) {
            Fallback
        }
    }

    /**
     * Composable reader for current device configuration.
     */
    @Composable
    @ReadOnlyComposable
    fun current(): DeviceConfigValues {
        val configuration = LocalConfiguration.current
        val density = LocalDensity.current.density
        return calculate(configuration.screenWidthDp, density)
    }
}

val LocalDeviceConfig = staticCompositionLocalOf { DeviceConfig.Fallback }

@Composable
fun ProvideDeviceConfig(
    content: @Composable () -> Unit
) {
    val dynamicConfig = DeviceConfig.current()
    CompositionLocalProvider(LocalDeviceConfig provides dynamicConfig) {
        content()
    }
}
