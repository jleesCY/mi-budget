// The Pill: mi.'s one control at the bottom of the screen. Tap it for the page's main action; swipe it sideways (where
// there's a previous and next) or up (the Library); hold it to search. It moves with the finger, ticks where letting
// go would act (both ways), keeps the fling's speed and springs back. Anatomy (from mi. Browser): a 70dp bar in
// pillBar with a 10dp shadow, corners at twice the Corners setting; inside it, 10 in, a 49dp field in pillField with
// corners at 1.5× the setting; the text 16sp W600; controls at the field's end (32dp circles, 48dp to touch). Typing,
// it rides on the keyboard edge to edge.
package com.jlees.mibudget.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jlees.mibudget.ui.theme.LocalMi
import kotlinx.coroutines.launch
import kotlin.math.abs

/** A control at the end of the Pill's field. [accent]: the main one, a filled primary circle. */
class PillAction(val icon: ImageVector, val label: String, val accent: Boolean = false, val onClick: () -> Unit)

/**
 * The Pill, at the bottom of a page (put it in a full-size Box). [text] is what its field shows when nothing's typed:
 * where you are, or what a tap does ("Add a Purchase"). [onSwipeSide]: +1 for a swipe right (previous), -1 left. With [onQuery] set it's a search field: [text] is its placeholder, a search symbol leads,
 * a clear button pops in while there's text, and [suggestions] (past searches) grow above the field while it's empty.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun BoxScope.MiPill(
    text: String,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    leading: PillAction? = null,
    trailing: List<PillAction> = emptyList(),
    onSwipeSide: ((Int) -> Unit)? = null,
    onSwipeUp: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    query: String? = null,
    onQuery: ((String) -> Unit)? = null,
    focusOnShow: Boolean = false,
    suggestions: List<String> = emptyList(),
    onForgetSuggestion: (String) -> Unit = {},
    /** In a sheet opened from the main bar: morph with it (SheetHost). */
    morph: SheetMorph? = null,
) {
    val cs = MaterialTheme.colorScheme
    val mi = LocalMi.current
    val haptics = LocalHaptics.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current.density
    val dx = remember { Animatable(0f) }
    val dy = remember { Animatable(0f) }
    var widthDp by remember { mutableFloatStateOf(360f) }
    // past the point where letting go acts: 0 none, ±1 sideways, 2 up
    var armed by remember { mutableFloatStateOf(0f) }
    var focused by remember { mutableStateOf(false) }
    val docked = focused && WindowInsets.isImeVisible
    // the Pill fully round from Corners 20 up; the field at 1.5× the setting
    val barRadius = if (mi.radius >= 20) 35.dp else (mi.radius * 2).dp
    val barShape = RoundedCornerShape(barRadius)
    val fieldShape = RoundedCornerShape(minOf(mi.radius * 1.5f, 24.5f).dp)
    val actions = listOfNotNull(leading) + trailing
    val showSuggestions = onQuery != null && focused && query.isNullOrEmpty() && suggestions.isNotEmpty()
    // morphing with its sheet: m = 0 this Pill … 1 the main bar's Pill, from how far the sheet is out
    val from = morph?.morphFrom?.invoke()
    val m = if (morph != null && from != null) morph.slide().coerceIn(0f, 1f) else 0f
    var measuredRest by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    // where this Pill rests: measured, or (on its first frame, before it's been laid out) where a sheet's Pill rested
    // last time, so the morph starts in the right place rather than full width for a frame
    val rest = measuredRest ?: morph?.restCache?.invoke()
    // nothing known yet (the very first open): stay invisible for that frame; the bar's own Pill is still showing
    val hidden = morph != null && from != null && rest == null
    // a back swipe on a sheet's Pill (toward the side the sheet came from) slides the sheet away with the finger
    var back by remember { mutableFloatStateOf(0f) }
    val sheetBack = morph != null && onSwipeSide == null

    Box(
        modifier.align(Alignment.BottomCenter).fillMaxWidth()
            // while typing it rides 10 above the keyboard, keeping its shape (it never melts into the keyboard)
            .windowInsetsPadding(if (docked) WindowInsets.navigationBars.union(WindowInsets.ime) else WindowInsets.navigationBars).padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
            // while morphing, the Pill holds still as its sheet slides (it moves only toward the bar's Pill)
            .graphicsLayer {
                if (morph != null && from != null) { val o = morph.offset(); translationX = -o.x; translationY = -o.y }
                if (hidden || morph?.covered?.invoke() == true) alpha = 0f
            }
            .then(if (!sheetBack || focused) Modifier else Modifier.panGesture(
                enabled = true,
                onStart = { back = 0f },
                onMove = { x, y ->
                    val toward = when (morph!!.page) { BarPage.LEFT -> -x; BarPage.RIGHT -> x; BarPage.UP -> y }
                    val p = (toward * density / morph.extent()).coerceIn(0f, 1f)
                    if ((p > 0.25f) != (back > 0.25f)) haptics?.tick() // past here, letting go goes back
                    back = p; morph.drag(p)
                },
                onRelease = { _, _, vx, vy ->
                    val toward = when (morph!!.page) { BarPage.LEFT -> -vx; BarPage.RIGHT -> vx; BarPage.UP -> vy } // dp/ms
                    val speed = toward * 1000f * density / morph.extent() // sheet fractions a second, outward
                    morph.release(back > 0.25f || (toward > 0.5f && back > 0.02f), speed)
                    back = 0f
                },
            ))
            .panGesture(
                enabled = !sheetBack && !docked && (onSwipeSide != null || onSwipeUp != null),
                onStart = { armed = 0f },
                onMove = { x, y ->
                    val side = onSwipeSide != null && abs(x) >= abs(y)
                    // moves 1:1 with the finger the way it can act; a little (with resistance) the ways it can't
                    scope.launch {
                        dx.snapTo(if (side) x else x / 6f)
                        dy.snapTo((if (!side && y < 0 && onSwipeUp != null) y else y / 6f).coerceAtLeast(-240f))
                    }
                    val now = when {
                        side && PillRules.side(x, 0f) != 0 -> if (x > 0) 1f else -1f
                        !side && y < 0 && onSwipeUp != null && PillRules.upArmed(y) -> 2f
                        else -> 0f
                    }
                    if (now != armed) { haptics?.tick(); armed = now }
                },
                onRelease = { x, y, vx, vy ->
                    val sideways = abs(x) >= abs(y)
                    val act = when {
                        sideways && onSwipeSide != null -> PillRules.side(x, vx)
                        y < 0 && onSwipeUp != null && PillRules.up(y, vy) -> 2
                        else -> 0
                    }
                    armed = 0f
                    // whatever finishes keeps the fling's speed
                    scope.launch { dx.animateTo(0f, Motion.springBack(), initialVelocity = vx * 1000f) }
                    scope.launch { dy.animateTo(0f, Motion.springBack(), initialVelocity = vy * 1000f) }
                    when (act) { 2 -> onSwipeUp?.invoke(); 1, -1 -> onSwipeSide?.invoke(act) }
                },
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        // the field's width at rest: while morphing, the field keeps it (centred, clipped by the shrinking bar) so its
        // icon and text stay put and only fade, instead of sliding about as the bar narrows
        val fieldWidth = rest?.let { (it.width / density - 20f).coerceAtLeast(1f).dp }
        Box(
            Modifier.widthIn(max = PILL_MAX_WIDTH.dp).fillMaxWidth()
                .onSizeChanged { widthDp = it.width / density }
                // the box always reports the resting place: morphToward keeps its outer size, and the sheet's slide is cancelled
                .onGloballyPositioned { b -> if (!docked && !showSuggestions) { val r = b.boundsInWindow(); if (measuredRest != r) measuredRest = r; morph?.onRest?.invoke(r) } }
                .morphToward(if (rest != null) from else null, rest, m)
                .graphicsLayer {
                    translationX = dx.value * density
                    translationY = dy.value * density
                }
                // fully round while morphing; its colour and shadow travel to the bar Pill's
                .shadow(androidx.compose.ui.util.lerp(10f, 6f, m).dp, if (m > 0f) CircleShape else barShape)
                .clip(if (m > 0f) CircleShape else barShape)
                .background(androidx.compose.ui.graphics.lerp(with(mi) { cs.pillBar }, cs.surfaceContainerHigh, m)),
        ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
            // past searches grow inside the bar, above the field (newest nearest the field)
            AnimatedVisibility(showSuggestions, enter = fadeIn(Motion.enter(200)), exit = fadeOut(Motion.exit(100))) {
                Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                    suggestions.reversed().forEach { q ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onQuery?.invoke(q) }.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Sym.History, null, Modifier.size(20.dp), tint = cs.onSurfaceVariant)
                            Spacer(Modifier.width(14.dp))
                            Text(q, Modifier.weight(1f), color = cs.onSurface, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            IconButton(onClick = { onForgetSuggestion(q) }) { Icon(Sym.Close, "Remove $q", Modifier.size(16.dp), tint = cs.onSurfaceVariant) }
                        }
                    }
                    HorizontalDivider(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), 1.dp, cs.outlineVariant.copy(alpha = 0.5f))
                }
            }
            Box(Modifier.fillMaxWidth().height(70.dp), contentAlignment = Alignment.Center) {
                Row(
                    Modifier.then(if (m > 0f && fieldWidth != null) Modifier.requiredWidth(fieldWidth) else Modifier.fillMaxWidth()).height(49.dp).graphicsLayer { alpha = (1f - m * 2f).coerceIn(0f, 1f) }.clip(fieldShape).background(with(mi) { cs.pillField })
                        .semantics {
                            contentDescription = text
                            if (onQuery == null) onClick { onTap(); true }
                            if (onLongPress != null) onLongClick("Search") { onLongPress(); true }
                        }
                        .then(if (onQuery != null) Modifier else Modifier.pointerInput(onLongPress) {
                            detectTapGestures(onTap = { onTap() }, onLongPress = onLongPress?.let { lp -> { haptics?.longPress(); lp() } })
                        })
                        .padding(start = 16.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (onQuery != null) { Icon(Sym.Search, null, Modifier.size(24.dp), tint = cs.onSurfaceVariant); Spacer(Modifier.width(12.dp)) }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        val style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = MaterialTheme.typography.bodyLarge.fontFamily)
                        if (onQuery != null) {
                            val focus = remember { FocusRequester() }
                            LaunchedEffect(Unit) { if (focusOnShow) runCatching { focus.requestFocus() } }
                            if (query.isNullOrEmpty()) Text(text, style = style, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            BasicTextField(
                                value = query.orEmpty(), onValueChange = onQuery, singleLine = true,
                                textStyle = style.copy(color = cs.onSurface), cursorBrush = SolidColor(cs.primary),
                                modifier = Modifier.fillMaxWidth().focusRequester(focus).onFocusChanged { focused = it.isFocused },
                            )
                        } else Text(text, style = style, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    // clear: pops in while there's text
                    AnimatedVisibility(!query.isNullOrEmpty(),
                        enter = fadeIn(Motion.enter(150)) + scaleIn(Motion.enter(200), initialScale = 0.6f),
                        exit = fadeOut(Motion.exit(100)) + scaleOut(Motion.exit(100), targetScale = 0.6f)) {
                        IconButton(onClick = { onQuery?.invoke("") }) { Icon(Sym.Cancel, "Clear", Modifier.size(20.dp), tint = cs.onSurfaceVariant) }
                    }
                    if (actions.isNotEmpty()) Spacer(Modifier.width(5.dp))
                    actions.forEach { PillButton(it) }
                    Spacer(Modifier.width(4.dp))
                }
            }
        }
        // the bar Pill's buttons fade in over the second half of the morph: laid out at their own size and centred on
        // the whole bar (not squeezed inside its padding), so they sit exactly where the bar's own buttons do
        if (m > 0.5f && morph != null) Box(
            Modifier.matchParentSize().graphicsLayer { alpha = (m - 0.5f) * 2f },
            contentAlignment = Alignment.Center,
        ) { Box(Modifier.wrapContentSize(unbounded = true)) { morph.morphContent() } }
        }
    }
}

/** A control at the field's end: a 32dp circle in a 48dp touch area (the main one filled with the accent). */
@Composable
private fun PillButton(a: PillAction) {
    val cs = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    Box(
        Modifier.size(48.dp).clip(CircleShape).clickable(source, ripple(bounded = false, radius = 24.dp), onClickLabel = a.label, onClick = a.onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.pressScale(source).size(32.dp).clip(CircleShape).then(if (a.accent) Modifier.background(cs.primary) else Modifier), contentAlignment = Alignment.Center) {
            Icon(a.icon, a.label, Modifier.size(if (a.accent) 20.dp else 22.dp), tint = if (a.accent) cs.onPrimary else cs.onSurface)
        }
    }
}

/** The room a page's list leaves at its end for the Pill (and a notice above it). */
val PillSpace = 112.dp

