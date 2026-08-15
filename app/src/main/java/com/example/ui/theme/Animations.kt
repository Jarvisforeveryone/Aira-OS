package com.example.ui.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * AIRA CENTRALIZED ANIMATION & MOTION SYSTEM
 * Production-ready motion specs, durations, enter/exit transitions, and micro-interactions
 * designed for fluid, high-tech cinematic AI assistant responses.
 */
object AiraMotion {
    // Duration Constants
    const val DurationFast = 150
    const val DurationNormal = 300
    const val DurationSlow = 500
    const val DurationCinematic = 800

    // Easing Curves
    val EasingEmphasized = FastOutSlowInEasing
    val EasingDecelerate = LinearOutSlowInEasing

    // Spring Specs
    val SpringBouncy: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    val SpringSubtle: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val SpringCinematic: AnimationSpec<Float> = spring(
        dampingRatio = 0.68f,
        stiffness = 250f
    )

    val SpringHolographic: AnimationSpec<Float> = spring(
        dampingRatio = 0.55f,
        stiffness = 180f
    )

    // Standard Animation Specs
    fun <T> tweenFast(): AnimationSpec<T> = tween(DurationFast, easing = EasingEmphasized)
    fun <T> tweenNormal(): AnimationSpec<T> = tween(DurationNormal, easing = EasingEmphasized)
    fun <T> tweenSlow(): AnimationSpec<T> = tween(DurationSlow, easing = EasingEmphasized)
    fun <T> tweenCinematic(): AnimationSpec<T> = tween(DurationCinematic, easing = EasingEmphasized)

    // Fade Transitions
    val FadeIn: EnterTransition = fadeIn(animationSpec = tween(DurationNormal, easing = EasingEmphasized))
    val FadeOut: ExitTransition = fadeOut(animationSpec = tween(DurationFast, easing = EasingEmphasized))

    // Slide Transitions
    val SlideInHorizontally: EnterTransition = slideInHorizontally(
        initialOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(DurationNormal, easing = EasingEmphasized)
    )
    val SlideInVertically: EnterTransition = slideInVertically(
        initialOffsetY = { fullHeight -> fullHeight },
        animationSpec = tween(DurationNormal, easing = EasingEmphasized)
    )
    val SlideIn: EnterTransition = slideIn(
        initialOffset = { size -> IntOffset(0, size.height / 3) },
        animationSpec = tween(DurationNormal, easing = EasingEmphasized)
    )

    // Scale Transitions
    val ScaleIn: EnterTransition = scaleIn(initialScale = 0.92f, animationSpec = tween(DurationNormal, easing = EasingEmphasized))

    // Scale & Fade Transitions
    val ScaleFadeIn: EnterTransition = fadeIn(tween(DurationNormal)) + scaleIn(initialScale = 0.92f, animationSpec = tween(DurationNormal, easing = EasingEmphasized))
    val ScaleFadeOut: ExitTransition = fadeOut(tween(DurationFast)) + scaleOut(targetScale = 0.95f, animationSpec = tween(DurationFast, easing = EasingEmphasized))
}

/**
 * MICRO-INTERACTION MODIFIER: Press Scale Effect with Tactile Haptic Feedback
 * Provides tactile button press animation and haptic feedback with futuristic spring response.
 */
@Composable
fun Modifier.bounceClick(
    onClick: () -> Unit,
    scaleDownFactor: Float = 0.96f
): Modifier {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDownFactor else 1.0f,
        animationSpec = AiraMotion.SpringCinematic,
        label = "bounceClickScale"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
        )
}

/**
 * AMBIENT LEVITATION MODIFIER
 * Implements subtle weightless floating/drift motion for high-tech cinematic UI elements.
 */
@Composable
fun Modifier.ambientLevitation(
    enabled: Boolean = true,
    floatDistanceDp: Float = 6f,
    durationMs: Int = 3200
): Modifier {
    if (!enabled) return this

    val infiniteTransition = rememberInfiniteTransition(label = "ambientLevitationTransition")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -floatDistanceDp,
        targetValue = floatDistanceDp,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "levitationOffsetY"
    )

    val scaleOscillation by infiniteTransition.animateFloat(
        initialValue = 0.995f,
        targetValue = 1.005f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs + 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "levitationScale"
    )

    return this.graphicsLayer {
        translationY = offsetY
        scaleX = scaleOscillation
        scaleY = scaleOscillation
    }
}

/**
 * HOLOGRAPHIC LIGHT SWEEP MODIFIER
 * Draws a subtle high-tech light shimmer sweep across the UI component to give a cinematic HUD interface feel.
 */
@Composable
fun Modifier.holographicLightSweep(
    enabled: Boolean = true,
    sweepColor: Color = Color(0xFF00AFFF).copy(alpha = 0.25f),
    periodMs: Int = 4500
): Modifier {
    if (!enabled) return this

    val infiniteTransition = rememberInfiniteTransition(label = "holoSweepTransition")
    val sweepProgress by infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(periodMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepProgress"
    )

    return this.drawWithContent {
        drawContent()
        if (sweepProgress in -0.2f..1.2f) {
            val width = size.width
            val height = size.height
            val xOffset = width * sweepProgress
            val brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    sweepColor,
                    Color.Transparent
                ),
                start = Offset(xOffset - width * 0.25f, 0f),
                end = Offset(xOffset + width * 0.25f, height)
            )
            drawRect(brush = brush)
        }
    }
}

/**
 * HIGH-TECH GLOW PULSE MODIFIER
 * Animates a glowing holographic border pulse on interactive/active UI elements.
 */
@Composable
fun Modifier.highTechGlowPulse(
    active: Boolean = true,
    glowColor: Color = Color(0xFF00AFFF),
    maxAlpha: Float = 0.6f
): Modifier {
    if (!active) return this

    val infiniteTransition = rememberInfiniteTransition(label = "glowPulseTransition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = maxAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    return this.drawWithContent {
        drawContent()
        val borderBrush = Brush.sweepGradient(
            colors = listOf(
                glowColor.copy(alpha = pulseAlpha),
                glowColor.copy(alpha = pulseAlpha * 0.3f),
                glowColor.copy(alpha = pulseAlpha),
                glowColor.copy(alpha = pulseAlpha * 0.3f),
                glowColor.copy(alpha = pulseAlpha)
            )
        )
        drawRect(
            brush = borderBrush,
            alpha = pulseAlpha
        )
    }
}

/**
 * Haptic Click Modifier
 * Wraps clickable with automatic tactile haptic feedback.
 */
@Composable
fun Modifier.hapticClick(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val haptic = LocalHapticFeedback.current
    return this.clickable(enabled = enabled) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onClick()
    }
}

/**
 * STAGGERED ENTRY MODIFIER
 * Applies a smooth spring-based staggered slide-and-fade animation for cards when loading.
 */
@Composable
fun Modifier.staggeredEntry(
    index: Int = 0,
    baseDelayMs: Long = 60L,
    initialOffsetY: Float = 40f
): Modifier {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * baseDelayMs)
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "staggeredAlpha_$index"
    )

    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else initialOffsetY,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "staggeredOffset_$index"
    )

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.94f,
        animationSpec = AiraMotion.SpringCinematic,
        label = "staggeredScale_$index"
    )

    return this.graphicsLayer {
        this.alpha = alpha
        this.translationY = offsetY
        this.scaleX = scale
        this.scaleY = scale
    }
}

/**
 * REUSABLE ANIMATED CONTAINER
 */
@Composable
fun AiraAnimatedContainer(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = AiraMotion.ScaleFadeIn,
    exit: ExitTransition = AiraMotion.ScaleFadeOut,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = enter,
        exit = exit,
        content = content
    )
}

