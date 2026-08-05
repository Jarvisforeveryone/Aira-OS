package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Dimens
import com.example.ui.theme.IconColors
import com.example.ui.theme.bounceClick
import com.example.utils.ScreenUtils

enum class AiraButtonVariant {
    PRIMARY,
    OUTLINED,
    TEXT
}

/**
 * AIRA UNIFIED BUTTON COMPONENT
 * Single source of truth for action buttons across AIRA.
 * Standardizes typography, colors, padding, loading states, and bounce micro-interactions.
 * Automatically adapts size, elevation, and font size using ScreenUtils responsive system.
 */
@Composable
fun AiraButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AiraButtonVariant = AiraButtonVariant.PRIMARY,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    shape: RoundedCornerShape? = null,
    height: Dp? = null,
    fullWidth: Boolean = false
) {
    val adaptive = ScreenUtils.adaptiveValues()
    val effectiveHeight = height ?: adaptive.buttonHeight
    val effectiveShape = shape ?: RoundedCornerShape(adaptive.cornerRadius)

    val buttonModifier = modifier
        .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
        .height(effectiveHeight)
        .bounceClick(onClick = if (enabled && !isLoading) onClick else { {} })

    val contentPadding = PaddingValues(horizontal = adaptive.padding, vertical = 8.dp)

    when (variant) {
        AiraButtonVariant.PRIMARY -> {
            Button(
                onClick = onClick,
                enabled = enabled && !isLoading,
                modifier = buttonModifier,
                shape = effectiveShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = containerColor,
                    contentColor = contentColor,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                ),
                contentPadding = contentPadding
            ) {
                AiraButtonContent(
                    text = text,
                    icon = icon,
                    isLoading = isLoading,
                    contentColor = contentColor,
                    fontSize = adaptive.buttonFontSize
                )
            }
        }
        AiraButtonVariant.OUTLINED -> {
            OutlinedButton(
                onClick = onClick,
                enabled = enabled && !isLoading,
                modifier = buttonModifier,
                shape = effectiveShape,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = containerColor
                ),
                contentPadding = contentPadding
            ) {
                AiraButtonContent(
                    text = text,
                    icon = icon,
                    isLoading = isLoading,
                    contentColor = containerColor,
                    fontSize = adaptive.buttonFontSize
                )
            }
        }
        AiraButtonVariant.TEXT -> {
            TextButton(
                onClick = onClick,
                enabled = enabled && !isLoading,
                modifier = buttonModifier,
                shape = effectiveShape,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = containerColor
                ),
                contentPadding = contentPadding
            ) {
                AiraButtonContent(
                    text = text,
                    icon = icon,
                    isLoading = isLoading,
                    contentColor = containerColor,
                    fontSize = adaptive.buttonFontSize
                )
            }
        }
    }
}

@Composable
private fun AiraButtonContent(
    text: String,
    icon: ImageVector?,
    isLoading: Boolean,
    contentColor: Color,
    fontSize: androidx.compose.ui.unit.TextUnit
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = contentColor,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold
        )
    }
}
