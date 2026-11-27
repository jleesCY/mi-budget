// The main bar: the standard bottom bar of every mi. app (mi. Browser's tab switcher bar). A centre Pill (a capsule in
// surfaceContainerHigh, 6dp shadow) holding a big + (56dp, primary) between two 52dp buttons, and, where the app needs
// them, one more 52dp button outside the Pill at each side of the screen. Swiping the Pill opens the edge buttons'
// pages (right: the left one's, from the left; left: the right one's) and the Pill button's page (up, from the bottom),
// following the finger; holding it searches. The Pill itself never moves, and never hides.
package com.jlees.mibudget.ui.common

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** One of the bar's buttons: its symbol, what a screen reader says, and what it does. */
class BarButton(val icon: ImageVector, val label: String, val onClick: () -> Unit)

/**
 * The main bar at the bottom of the start screen (put it in a full-size Box). [plus] is the app's main action, [left]
 * and [right] the buttons beside it in the Pill (the left one's page also opens with a swipe up), [leftEdge] and
 * [rightEdge] the buttons outside it (their pages open with a swipe right and left). [onPageDrag] / [onPageRelease]
 * follow a swipe bringing a page in; [onLongPress] searches. [onPillBounds] reports the Pill's place, for the sheets' Pills to morph from. [onRightBounds]
 * reports the right button's and [onPlusBounds] the +'s, for the item menus to grow from.
 */
@Composable
fun BoxScope.MainBar(
    plus: BarButton,
    left: BarButton,
    right: BarButton,
    leftEdge: BarButton? = null,
    rightEdge: BarButton? = null,
    pages: Set<BarPage> = BarPage.entries.toSet(),
    onPageDrag: (BarPage, Float) -> Unit = { _, _ -> },
    onPageRelease: (BarPage, open: Boolean, speed: Float) -> Unit = { _, _, _ -> },
    onLongPress: (() -> Unit)? = null,
    onPillBounds: (Rect) -> Unit = {},
    onRightBounds: (Rect) -> Unit = {},
    onPlusBounds: (Rect) -> Unit = {},
    /** 0..1: the bar's Pill steps aside while a page's Pill (morphing out of it) takes its place; the edge buttons stay. */
    alpha: () -> Float = { 1f },
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val haptics = LocalHaptics.current
    val density = LocalDensity.current.density
    val widthDp = LocalConfiguration.current.screenWidthDp.toFloat()
    val heightDp = LocalConfiguration.current.screenHeightDp.toFloat()
    var dragPage by remember { mutableStateOf<BarPage?>(null) }
    var armed by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    Box(
        modifier.align(Alignment.BottomCenter).fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            Modifier.widthIn(max = COMFORTABLE_WIDTH.dp).fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            // the edge buttons: straight on the background, at the far sides
            leftEdge?.let { EdgeButton(it, Modifier.align(Alignment.CenterStart)) }
            rightEdge?.let { EdgeButton(it, Modifier.align(Alignment.CenterEnd)) }
            Surface(
                shape = CircleShape, color = cs.surfaceContainerHigh, shadowElevation = 6.dp,
                modifier = Modifier
                    .graphicsLayer { this.alpha = alpha() }
                    .onGloballyPositioned { onPillBounds(it.boundsInWindow()) }
                    // holding the Pill (buttons included) searches. Timed on the Initial pass, before the buttons see the
                    // touch, so their own handling can't cancel it; once it fires, the rest of the touch is swallowed so
                    // nothing clicks or swipes.
                    .then(if (onLongPress == null) Modifier else Modifier.pointerInput(onLongPress) {
                        val slop = viewConfiguration.touchSlop
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                            // lifted or moved before the timeout: not a hold
                            val ended = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                                do { val c = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } }
                                while (c != null && c.pressed && (c.position - down.position).getDistance() <= slop)
                                true
                            }
                            if (ended != null) return@awaitEachGesture
                            haptics?.longPress(); onLongPress()
                            while (true) {
                                val e = awaitPointerEvent(PointerEventPass.Initial)
                                e.changes.forEach { it.consume() }
                                if (e.changes.none { it.pressed }) break
                            }
                        }
                    })
                    .panGesture(
                        enabled = true,
                        onStart = { dragPage = null; armed = false; progress = 0f },
                        onMove = { x, y ->
                            if (dragPage == null) dragPage = MainBarRules.page(x, y)?.takeIf { it in pages }
                            val p = dragPage
                            if (p != null) {
                                progress = MainBarRules.progress(p, x, y, widthDp, heightDp)
                                onPageDrag(p, progress)
                                val now = progress >= MainBarRules.OPEN_AT
                                if (now != armed) { haptics?.tick(); armed = now }
                            }
                        },
                        onRelease = { x, y, vx, vy ->
                            val p = dragPage
                            if (p != null) {
                                // decided from where the finger let go, not the last move seen
                                val at = MainBarRules.progress(p, x, y, widthDp, heightDp)
                                val speed = MainBarRules.speedToward(p, vx * 1000f, vy * 1000f)
                                val extent = if (p == BarPage.UP) heightDp else widthDp
                                onPageRelease(p, MainBarRules.opens(at, speed, density), speed / extent)
                            }
                            dragPage = null; armed = false
                        },
                    ),
            ) {
                Row(Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    PillButton(left, Modifier.padding(horizontal = 4.dp))
                    val press = remember { MutableInteractionSource() }
                    FilledIconButton(
                        onClick = plus.onClick, modifier = Modifier.size(56.dp).pressScale(press).onGloballyPositioned { onPlusBounds(it.boundsInWindow()) }, interactionSource = press,
                        // the accent at full strength, the same in every accent (MiUi.accent)
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = com.jlees.mibudget.ui.theme.LocalMi.current.accent, contentColor = com.jlees.mibudget.ui.theme.LocalMi.current.onAccent),
                    ) { Icon(plus.icon, plus.label, Modifier.size(30.dp)) }
                    PillButton(right, Modifier.padding(horizontal = 4.dp).onGloballyPositioned { onRightBounds(it.boundsInWindow()) })
                }
            }
        }
    }
}

/** The bar Pill's three buttons as they look, for a sheet's Pill to fade into as it shrinks back into the bar. */
@Composable
fun MainBarPillLook(plus: ImageVector, left: ImageVector, right: ImageVector) {
    val cs = MaterialTheme.colorScheme
    Row(Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.padding(horizontal = 4.dp).size(52.dp), contentAlignment = Alignment.Center) { Icon(left, null, Modifier.size(24.dp), tint = cs.onSurface) }
        val mi = com.jlees.mibudget.ui.theme.LocalMi.current
        Surface(Modifier.size(56.dp), shape = CircleShape, color = mi.accent) { Box(contentAlignment = Alignment.Center) { Icon(plus, null, Modifier.size(30.dp), tint = mi.onAccent) } }
        Box(Modifier.padding(horizontal = 4.dp).size(52.dp), contentAlignment = Alignment.Center) { Icon(right, null, Modifier.size(24.dp), tint = cs.onSurface) }
    }
}

@Composable
private fun PillButton(b: BarButton, modifier: Modifier) {
    val press = remember { MutableInteractionSource() }
    IconButton(onClick = b.onClick, modifier = modifier.size(52.dp).pressScale(press), interactionSource = press) {
        Icon(b.icon, b.label, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun EdgeButton(b: BarButton, modifier: Modifier) {
    val press = remember { MutableInteractionSource() }
    IconButton(onClick = b.onClick, modifier = modifier.size(52.dp).pressScale(press), interactionSource = press) {
        Icon(b.icon, b.label, Modifier.size(26.dp), tint = MaterialTheme.colorScheme.onSurface)
    }
}

