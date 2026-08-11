package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

enum class OrbState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING
}

@Composable
fun AiraVoiceOrb(
    isListening: Boolean,
    isProcessing: Boolean,
    isSpeaking: Boolean,
    audioAmp: Float = 0f,
    reduceAnimations: Boolean = false,
    derivedStatus: String = "",
    onOrbClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    // Determine current Orb state
    val orbState = when {
        isProcessing -> OrbState.PROCESSING
        isListening -> OrbState.LISTENING
        isSpeaking -> OrbState.SPEAKING
        else -> OrbState.IDLE
    }

    // Light mode: #4A90E2 to #2563EB
    // Dark mode:  #3B82F6 to #1D4ED8
    val startBlue = if (isDark) Color(0xFF3B82F6) else Color(0xFF4A90E2)
    val endBlue = if (isDark) Color(0xFF1D4ED8) else Color(0xFF2563EB)
    val glowColor = if (isDark) Color(0xFF3B82F6) else Color(0xFF4A90E2)

    val coreGradient = Brush.linearGradient(
        colors = listOf(startBlue, endBlue),
        start = Offset(0f, 0f),
        end = Offset(200f, 200f)
    )

    // Infinite transition for pulse and rotation
    val infiniteTransition = rememberInfiniteTransition(label = "OrbAnimations")

    // 1. Slow breathing pulse for IDLE (2.5s cycle)
    val idlePulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (reduceAnimations) 5000 else 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "IdlePulseScale"
    )

    // 2. Fast pulse for LISTENING
    val listeningPulseScale by infiniteTransition.animateFloat(
        initialValue = 1.02f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (reduceAnimations) 3000 else 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ListeningPulseScale"
    )

    // 3. Rotation angle for PROCESSING state
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (reduceAnimations) 5000 else 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ProcessingRotation"
    )

    // Audio amplitude reactive scaling during LISTENING/SPEAKING
    val animatedAmpScale by animateFloatAsState(
        targetValue = when (orbState) {
            OrbState.LISTENING -> (audioAmp * 0.25f + 1.0f).coerceIn(1.0f, 1.25f)
            OrbState.SPEAKING -> (audioAmp * 0.18f + 1.0f).coerceIn(1.0f, 1.18f)
            else -> 1.0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "AudioAmpScale"
    )

    // Overall scale factor
    val currentScale = when (orbState) {
        OrbState.IDLE -> idlePulseScale
        OrbState.LISTENING -> listeningPulseScale * animatedAmpScale
        OrbState.PROCESSING -> 1.03f
        OrbState.SPEAKING -> animatedAmpScale
    }

    Box(
        modifier = modifier
            .size(300.dp) // Total container fits 150% glow ring (270dp) + safety padding
            .testTag("aira_voice_orb")
            .semantics {
                liveRegion = LiveRegionMode.Polite
                role = Role.Button
                contentDescription = when (orbState) {
                    OrbState.LISTENING -> "AIRA Voice Orb, listening to your voice. Tap to stop."
                    OrbState.PROCESSING -> "AIRA Voice Orb, processing request..."
                    OrbState.SPEAKING -> "AIRA Voice Orb, speaking response."
                    OrbState.IDLE -> "AIRA Voice Orb, ready. Tap to speak."
                }
                stateDescription = derivedStatus
                onClick(label = if (isListening) "Stop voice input" else "Start voice input") {
                    onOrbClick()
                    true
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onOrbClick()
            },
        contentAlignment = Alignment.Center
    ) {
        // 5. GLOW EFFECT: 3 Concentric Glow Rings Behind the Orb
        // Outer Glow Ring (150% = 270dp, 10% opacity)
        Box(
            modifier = Modifier
                .size(270.dp)
                .scale(currentScale)
                .background(glowColor.copy(alpha = if (orbState == OrbState.LISTENING) 0.18f else 0.10f), CircleShape)
        )

        // Middle Glow Ring (130% = 234dp, 20% opacity)
        Box(
            modifier = Modifier
                .size(234.dp)
                .scale(currentScale)
                .background(glowColor.copy(alpha = if (orbState == OrbState.LISTENING) 0.28f else 0.20f), CircleShape)
        )

        // Inner Glow Ring (110% = 198dp, 30% opacity)
        Box(
            modifier = Modifier
                .size(198.dp)
                .scale(currentScale)
                .background(glowColor.copy(alpha = if (orbState == OrbState.LISTENING) 0.42f else 0.30f), CircleShape)
        )

        // 4. CORE: Perfect circle, 180dp diameter with solid blue gradient fill
        Box(
            modifier = Modifier
                .size(180.dp)
                .scale(currentScale)
                .shadow(
                    elevation = if (orbState == OrbState.LISTENING) 24.dp else 12.dp,
                    shape = CircleShape,
                    ambientColor = glowColor,
                    spotColor = glowColor
                )
                .clip(CircleShape)
                .background(coreGradient),
            contentAlignment = Alignment.Center
        ) {
            // PROCESSING / THINKING Inner Rotation Effect
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                if (orbState == OrbState.PROCESSING) {
                    rotate(rotationAngle) {
                        val strokePx = 4.dp.toPx()
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.9f),
                                    Color.White.copy(alpha = 0.3f),
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.9f)
                                )
                            ),
                            startAngle = 0f,
                            sweepAngle = 270f,
                            useCenter = false,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round),
                            size = Size(size.width - strokePx * 2, size.height - strokePx * 2),
                            topLeft = Offset(strokePx, strokePx)
                        )
                    }
                }

                // JARVIS-style Specular Glass Highlight Arc across top of orb
                val highlightPath = Path().apply {
                    moveTo(size.width * 0.2f, size.height * 0.25f)
                    quadraticTo(
                        size.width * 0.5f, size.height * 0.08f,
                        size.width * 0.8f, size.height * 0.25f
                    )
                }
                drawPath(
                    path = highlightPath,
                    color = Color.White.copy(alpha = 0.25f),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Center Icon: White microphone with sound waves
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (orbState == OrbState.SPEAKING || orbState == OrbState.LISTENING) {
                    Icon(
                        imageVector = Icons.Filled.GraphicEq,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier
                            .size(28.dp)
                            .padding(end = 4.dp)
                    )
                }

                Icon(
                    imageVector = Icons.Outlined.Mic,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(52.dp)
                )

                if (orbState == OrbState.SPEAKING || orbState == OrbState.LISTENING) {
                    Icon(
                        imageVector = Icons.Filled.GraphicEq,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier
                            .size(28.dp)
                            .padding(start = 4.dp)
                    )
                }
            }
        }
    }
}
