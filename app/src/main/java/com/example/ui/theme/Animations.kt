package com.example.ui.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/**
 * AIRA CENTRALIZED ANIMATION & MOTION SYSTEM
 * Production-ready motion specs, durations, enter/exit transitions, and micro-interactions.
 */
object AiraMotion {
    // Duration Constants
    const val DurationFast = 150
    const val DurationNormal = 300
    const val DurationSlow = 500

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

    // Standard Animation Specs
    fun <T> tweenFast(): AnimationSpec<T> = tween(DurationFast, easing = EasingEmphasized)
    fun <T> tweenNormal(): AnimationSpec<T> = tween(DurationNormal, easing = EasingEmphasized)
    fun <T> tweenSlow(): AnimationSpec<T> = tween(DurationSlow, easing = EasingEmphasized)

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
 * Provides tactile button press animation and haptic feedback.
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
        animationSpec = AiraMotion.SpringBouncy,
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
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
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
