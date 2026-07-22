package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CssPulsingEffect(
    isListening: Boolean,
    pulseColor: Color,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = CircleShape,
    pulseCount: Int = 3
) {
    if (!isListening) return

    val infiniteTransition = rememberInfiniteTransition(label = "css_pulse_transition")

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        for (i in 0 until pulseCount) {
            val duration = 1800
            val staggerDelay = i * (duration / pulseCount)
            
            val progress by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = duration
                        0f at 0 with LinearEasing
                        0f at staggerDelay with EaseOutQuint
                        1f at (staggerDelay + 1100).coerceAtMost(duration) with EaseOutQuad
                        1f at duration with LinearEasing
                    },
                    repeatMode = RepeatMode.Restart
                ),
                label = "css_pulse_progress_$i"
            )

            // CSS pulse effect: scales from base (1.0) up to 1.5
            val scale = if (progress <= 0.01f) 1.0f else 1.0f + (progress * 0.5f)
            
            // CSS Box shadow opacity decay curve (starts at 0.70f and fades cleanly to 0f at limit)
            val opacity = if (progress <= 0.01f || progress >= 0.99f) {
                0f
            } else {
                (0.70f * (1f - progress)).coerceIn(0f, 1f)
            }

            if (opacity > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(scale)
                        .alpha(opacity)
                        .border(1.5.dp, pulseColor, shape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(pulseColor.copy(alpha = 0.15f), Color.Transparent)
                            ),
                            shape = shape
                        )
                )
            }
        }
    }
}
