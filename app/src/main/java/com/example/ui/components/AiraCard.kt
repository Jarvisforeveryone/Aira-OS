package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AiraMotion
import com.example.ui.theme.Dimens
import com.example.ui.theme.ambientLevitation
import com.example.ui.theme.bounceClick
import com.example.ui.theme.highTechGlowPulse
import com.example.ui.theme.holographicLightSweep
import com.example.utils.ScreenUtils

/**
 * AIRA UNIFIED CARD COMPONENT (LOOP 1)
 * Standardizes card structures, headers, margins, borders, and micro-interactions across all screens.
 * Automatically adjusts padding, elevation, and corner radius based on ScreenUtils responsive layout scale.
 * Supports high-tech cinematic motion effects like holographic light sweep and ambient levitation.
 */
@Composable
fun AiraCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    title: String? = null,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    headerTrailing: (@Composable RowScope.() -> Unit)? = null,
    cornerShape: RoundedCornerShape? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
    contentPadding: Dp? = null,
    verticalSpacing: Dp = Dimens.ListVerticalSpacing,
    showHoloSweep: Boolean = false,
    enableFloating: Boolean = false,
    highlightGlow: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val adaptive = ScreenUtils.adaptiveValues()
    val effectivePadding = contentPadding ?: adaptive.cardPadding
    val effectiveShape = cornerShape ?: RoundedCornerShape(adaptive.cornerRadius)
    val effectiveElevation = adaptive.elevation

    var baseModifier = modifier
        .fillMaxWidth()
        .ambientLevitation(enabled = enableFloating, floatDistanceDp = 4f, durationMs = 3600)
        .holographicLightSweep(enabled = showHoloSweep)
        .highTechGlowPulse(active = highlightGlow)

    val cardModifier = if (onClick != null) {
        baseModifier.bounceClick(onClick = onClick)
    } else {
        baseModifier
    }

    Card(
        modifier = cardModifier,
        shape = effectiveShape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(if (highlightGlow) 1.5.dp else 1.dp, if (highlightGlow) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = effectiveElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(effectivePadding),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing)
        ) {
            if (title != null || icon != null || headerTrailing != null) {
                AiraCardHeader(
                    title = title,
                    subtitle = subtitle,
                    icon = icon,
                    iconTint = iconTint,
                    headerTrailing = headerTrailing
                )
            }
            content()
        }
    }
}

@Composable
fun AiraCardHeader(
    title: String?,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    headerTrailing: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column {
                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.SansSerif,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.SansSerif,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (headerTrailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                headerTrailing()
            }
        }
    }
}

/**
 * Status Badge helper for card headers.
 */
@Composable
fun AiraBadge(
    text: String,
    badgeColor: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    Surface(
        color = badgeColor,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}
