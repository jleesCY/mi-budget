// mi.'s notices replace Android toasts and snackbars: one pill at a time, above the Pill, growing out of its middle.
package com.jlees.mibudget.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * A notice: [message], and an [action] ("Undo") with what it does. [ok]: good news that finished at once (it gets a ✓);
 * never for errors, things in progress, or deleting (neutral: it gets Undo instead). [onGone]: the notice left without
 * its action (the deletion stands).
 */
class Notice(val message: String, val action: String? = null, val ok: Boolean = false, val ms: Long = 2500, val onAction: () -> Unit = {}, val onGone: () -> Unit = {})

object Notices {
    var current by mutableStateOf<Notice?>(null)
        private set

    /** Shows [message]. [done]: finished, at once, and good news ("Exported"): it gets a ✓. */
    fun show(message: String, done: Boolean = false) = post(Notice(message, ok = done, ms = 2500))

    /** Something just happened that can be put back: "Deleted Target · Undo". [onGone] runs if it isn't undone. */
    fun undo(message: String, onGone: () -> Unit = {}, undo: () -> Unit) = post(Notice(message, "Undo", ms = 4000, onAction = undo, onGone = onGone))

    fun post(n: Notice) { current?.onGone?.invoke(); current = n }

    internal fun clear(n: Notice, acted: Boolean) {
        if (current !== n) return
        current = null
        if (acted) n.onAction() else n.onGone()
    }
}

/** Where notices show: put it once over the app, just above the Pill. */
@Composable
fun NoticeHost(modifier: Modifier = Modifier) {
    val notice = Notices.current
    LaunchedEffect(notice) { if (notice != null) { delay(notice.ms); Notices.clear(notice, acted = false) } }
    // what it says stays while it shrinks away
    var shown by remember { mutableStateOf(notice) }
    if (notice != null) shown = notice
    val cs = MaterialTheme.colorScheme
    Box(modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            notice != null, Modifier.clip(CircleShape),
            enter = expandHorizontally(Motion.enter(280), expandFrom = Alignment.CenterHorizontally) + fadeIn(Motion.enter(150)),
            exit = shrinkHorizontally(Motion.exit(160), shrinkTowards = Alignment.CenterHorizontally) + fadeOut(Motion.exit(160)),
        ) {
            Surface(Modifier.widthIn(max = 520.dp), shape = CircleShape, color = cs.inverseSurface) {
                val n = shown
                Row(Modifier.heightIn(min = 48.dp).padding(start = 20.dp, end = if (n?.action == null) 20.dp else 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (n?.ok == true) {
                        val pop = remember(n) { Animatable(0f) }
                        LaunchedEffect(n) { pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 500f)) }
                        Icon(Sym.Check, null, Modifier.padding(end = 8.dp).size(18.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value }, tint = cs.inversePrimary)
                    }
                    Text(n?.message.orEmpty(), Modifier.weight(1f, fill = false), color = cs.inverseOnSurface, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    n?.action?.let { a -> TextButton(onClick = { Notices.clear(n, acted = true) }) { Text(a, color = cs.inversePrimary, fontWeight = FontWeight.SemiBold) } }
                }
            }
        }
    }
}
