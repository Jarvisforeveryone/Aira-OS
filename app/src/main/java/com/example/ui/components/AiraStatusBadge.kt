package com.example.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.Dimens

enum class BadgeStatus {
    SUCCESS,
    WARNING,
    ERROR,
    INFO,
    PRIMARY
}

/**
 * Standardized status badge pill for AIRA dashboards and cards.
 */
@Composable
fun AiraStatusBadge(
    text: String,
    modifier: Modifier = Modifier,
    status: BadgeStatus = BadgeStatus.PRIMARY,
    customColor: Color? = null,
    customTextColor: Color? = null
) {
    val backgroundColor = customColor ?: when (status) {
        BadgeStatus.SUCCESS -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        BadgeStatus.WARNING -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f)
        BadgeStatus.ERROR -> MaterialTheme.colorScheme.error.copy(alpha = 0.18f)
        BadgeStatus.INFO -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
        BadgeStatus.PRIMARY -> MaterialTheme.colorScheme.primaryContainer
    }

    val textColor = customTextColor ?: when (status) {
        BadgeStatus.SUCCESS -> MaterialTheme.colorScheme.primary
        BadgeStatus.WARNING -> MaterialTheme.colorScheme.tertiary
        BadgeStatus.ERROR -> MaterialTheme.colorScheme.error
        BadgeStatus.INFO -> MaterialTheme.colorScheme.secondary
        BadgeStatus.PRIMARY -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    Surface(
        modifier = modifier,
        color = backgroundColor,
        shape = RoundedCornerShape(Dimens.CornerRadiusSmall)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = Dimens.GapSmall, vertical = Dimens.GapTiny)
        )
    }
}
