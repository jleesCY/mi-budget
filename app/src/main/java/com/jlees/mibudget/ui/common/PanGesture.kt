// A drag (from mi. Browser) read in the coordinates of a slot that stays still, for controls that move with the finger (the Pill, sheets, handles).
package com.jlees.mibudget.ui.common

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlin.math.abs

/**
 * PanResponder-like gesture: claims after the touch slop of movement in any direction (Initial pass so it wins over
 * the text field), reports cumulative dx/dy in dp and release velocity in dp/ms.
 */
fun Modifier.panGesture(
    enabled: Boolean,
    onStart: () -> Unit = {},
    onMove: (Float, Float) -> Unit,
    onRelease: (Float, Float, Float, Float) -> Unit,
    onTap: (() -> Unit)? = null,
): Modifier = if (!enabled) this else pointerInput(enabled) {
    val dens = density
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val tracker = VelocityTracker()
        // Positions are local: put this on something that doesn't move with the drag (the Pill's slot, not the Pill),
        // or its own movement feeds back into the gesture (it lags at half speed and jitters).
        var total = Offset.Zero
        tracker.addPosition(down.uptimeMillis, total)
        var claimed = false
        var released = false
        try {
            while (true) {
                val e = awaitPointerEvent(PointerEventPass.Initial)
                val c = e.changes.firstOrNull { it.id == down.id } ?: break
                total += c.positionChangeIgnoreConsumed()
                tracker.addPosition(c.uptimeMillis, total)
                if (!c.pressed) {
                    if (claimed) {
                        c.consume()
                        val v = tracker.calculateVelocity()
                        released = true
                        onRelease(total.x / dens, total.y / dens, v.x / dens / 1000f, v.y / dens / 1000f)
                    } else if (onTap != null) onTap()
                    break
                }
                val slop = viewConfiguration.touchSlop
                if (!claimed && (abs(total.x) > slop || abs(total.y) > slop)) { claimed = true; onStart() }
                if (claimed) { c.consume(); onMove(total.x / dens, total.y / dens) }
            }
        } finally {
            // Interrupted (pointer lost, node removed/restarted, multi-touch): always settle so nothing is left half-way.
            if (claimed && !released) onRelease(total.x / dens, total.y / dens, 0f, 0f)
        }
    }
}
