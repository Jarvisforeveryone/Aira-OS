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
import androidx.compose.ui.unit.IntOffset

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
 * MICRO-INTERACTION MODIFIER: Press Scale Effect
 * Provides tactile button press animation.
 */
@Composable
fun Modifier.bounceClick(
    onClick: () -> Unit,
    scaleDownFactor: Float = 0.96f
): Modifier {
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
            onClick = onClick
        )
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
