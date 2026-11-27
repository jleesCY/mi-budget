// mi.'s motion, haptics and small physical touches (copied from mi. Browser's ui/common until a shared mi-design
// library exists): springs and tweens with fixed roles, a press that sinks and springs back, a pop when something turns
// on, a shake when nothing matches. The more often something is used, the less it animates.
package com.jlees.mibudget.ui.common

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp

/** The widest pages and panels get (dp): on tablets, foldables and in landscape they stay centred at this width. */
const val COMFORTABLE_WIDTH = 720
const val PILL_MAX_WIDTH = 600

object Motion {
    /** Big full-screen moves that follow a finger (a sheet sliding out at the finger's speed): fast, no long tail. */
    val switcher = spring<Float>(dampingRatio = 0.9f, stiffness = 900f, visibilityThreshold = 0.001f)

    fun <T> fastSpatial(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.6f, stiffness = 800f) // swaps, a sheet opening
    fun <T> spatial(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.8f, stiffness = 380f)     // settling, resets
    fun <T> noBounce(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 600f)      // growing without overshoot
    fun <T> playful(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.5f, stiffness = 300f)     // toys
    fun <T> springBack(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.45f, stiffness = 500f) // a dragged control returning

    /** Something arriving: fast at first, settling gently (Material's emphasized decelerate). */
    fun <T> enter(ms: Int = 250): FiniteAnimationSpec<T> = tween(ms, easing = ENTER)
    /** Something leaving: gathers speed, and goes quicker than it came (emphasized accelerate). */
    fun <T> exit(ms: Int = 150): FiniteAnimationSpec<T> = tween(ms, easing = EXIT)
    val ENTER = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val EXIT = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
}

/** Something showing up in place (a panel, a line of details): fades in and grows. */
fun appear(): androidx.compose.animation.EnterTransition = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically()

/** The opposite of [appear]: fades out and shrinks. */
fun disappear(): androidx.compose.animation.ExitTransition = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()

/** Pressed, it sinks to 94% within 100ms; let go, it springs back. Put it first in the chain, with the click's [source]. */
fun Modifier.pressScale(source: InteractionSource): Modifier = composed {
    val pressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.94f else 1f, if (pressed) tween(100) else spring(dampingRatio = 0.45f, stiffness = 700f), label = "press")
    graphicsLayer { scaleX = s; scaleY = s }
}

/** Something just turned [on]: it pops to 125% and settles. Not on first showing, nor turning off. */
@Composable
fun popScale(on: Boolean): Float {
    val scale = remember { Animatable(1f) }
    var seen by remember { mutableStateOf(false) }
    LaunchedEffect(on) {
        if (seen && on) { scale.animateTo(1.25f, tween(100)); scale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 500f)) }
        seen = true
    }
    return scale.value
}

/** An expander's arrow: points down closed, turns (200ms) to point up [open]. */
@Composable
fun Chevron(open: Boolean, size: Float, color: androidx.compose.ui.graphics.Color) {
    val turn by animateFloatAsState(if (open) 180f else 0f, tween(200), label = "chevron")
    androidx.compose.material3.Icon(Sym.KeyboardArrowDown, null, Modifier.size(size.dp).graphicsLayer { rotationZ = turn }, tint = color)
}

/** A sideways shake (dp offset) each time [wrong] turns true: three swings over 300ms, a "no" nod. */
@Composable
fun shakeOffset(wrong: Boolean): Float {
    val x = remember { Animatable(0f) }
    LaunchedEffect(wrong) {
        if (wrong) x.animateTo(0f, keyframes { durationMillis = 300; -8f at 50; 8f at 100; -6f at 150; 6f at 200; -3f at 250 })
    }
    return x.value
}

/**
 * Haptic feedback at a gesture's decision points (a swipe passing the point where it acts, a long press opening
 * something). Off with Settings › Look & Feel › Gesture Haptics; Android's own touch-feedback setting applies too.
 */
class Haptics(private val view: View, private val enabled: () -> Boolean) {
    fun tick() = perform(if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.SEGMENT_TICK else HapticFeedbackConstants.CLOCK_TICK)
    fun longPress() = perform(HapticFeedbackConstants.LONG_PRESS)
    private fun perform(effect: Int) { if (enabled()) view.performHapticFeedback(effect) }
}

/** Provided by MainActivity; null in previews. */
val LocalHaptics = staticCompositionLocalOf<Haptics?> { null }
