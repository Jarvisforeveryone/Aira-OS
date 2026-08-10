package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.example.ui.theme.Dimens
import com.example.utils.ScreenUtils

/**
 * AdaptiveGrid layout helper that renders items into 1, 2, 3, or 4 columns based on ScreenUtils.
 */
@Composable
fun AdaptiveGrid(
    modifier: Modifier = Modifier,
    spacing: Dp = Dimens.CardGap,
    items: List<@Composable () -> Unit>
) {
    val adaptive = ScreenUtils.adaptiveValues()
    val columns = adaptive.gridColumns.coerceIn(1, 4)

    if (columns == 1 || items.size <= 1) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            items.forEach { item ->
                item()
            }
        }
    } else {
        val rows = (items.size + columns - 1) / columns
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            for (rowIndex in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    for (colIndex in 0 until columns) {
                        val itemIndex = rowIndex * columns + colIndex
                        if (itemIndex < items.size) {
                            Box(modifier = Modifier.weight(1f)) {
                                items[itemIndex]()
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
