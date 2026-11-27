// The split action Pill: on one purchase's or subscription's page (its summary, and its editor) the bottom Pill holds
// that page's actions (Edit | Refund | Delete, or Delete | Save), as one Pill split into connected segments. It's the
// same Pill as everywhere ("One Pill, always morphing"): opening the page from a list, the list page's Pill (the main
// bar's, or a sheet's search Pill) turns into this one, its place, size, colour and contents travelling with the move;
// going back, it turns back. Swiping it right goes back, as every page's Pill does.
//
// It's drawn above all screens (ActionPillHost, in AppNav), so the move doesn't slide with either page. Screens declare
// theirs with ActionPill(spec); whoever opens such a page says where its Pill grows from (ActionPills.origin).
package com.jlees.mibudget.ui.common

import androidx.compose.ui.geometry.lerp
import androidx.activity.BackEventCompat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.jlees.mibudget.ui.theme.LocalMi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** One segment of the split Pill. [main]: the page's main action (primary); [danger]: destructive (errorContainer, its symbol only). */
class PillSegment(
    val label: String, val icon: ImageVector, val main: Boolean = false, val danger: Boolean = false,
    val enabled: Boolean = true, val onClick: () -> Unit,
)

/**
 * A page's split Pill: its segments, and where swiping it right goes (back). [backPops]: going back leaves the page
 * (it slides away with the finger, the Pill shrinking back into the one it came from), rather than staying on it
 * (out of editing: the Pill just follows the finger and springs back).
 */
class ActionPillSpec(val segments: List<PillSegment>, val onBack: () -> Unit, val backPops: Boolean = true)

/** Which Pill a page's split Pill grows out of (that one hides while this stands in for it). */
enum class PillOrigin { NONE, BAR, SHEET }

@Stable
class ActionPills(private val scope: CoroutineScope) {
    var spec by mutableStateOf<ActionPillSpec?>(null)
        private set
    /** Where the next page's Pill grows from (window px), set by whoever opens it; null: it's just there. */
    var origin by mutableStateOf<Rect?>(null)
        private set
    var originKind by mutableStateOf(PillOrigin.NONE)
        private set
    /**
     * Where the split Pill rests (window px), remembered between pages, so it can start morphing on its first frame.
     * Until it's known (the very first page), the Pill it grows from stays on screen: never a frame without a Pill.
     */
    internal var rest by mutableStateOf<Rect?>(null)
    /** The host has composed the split Pill: only from then does the Pill it grew from hide (never a frame with neither). */
    internal var drawn by mutableStateOf(false)
    /** 0: the split Pill at rest … 1: back at [origin], as the Pill it grew out of. */
    val m = Animatable(0f)
    private var owner: Any? = null
    /** What the sheet's search field it grew from shows (and whether that's a typed query rather than the placeholder). */
    internal var originText = "Search" to false
        private set
    /** A page is opening from a Pill and hasn't said what its own holds yet: until it does, the origin Pill's look stands in. */
    internal var pending by mutableStateOf(false)
        private set

    /** A page is about to open from a Pill at [rect] ([kind]) (or from somewhere without one: null). */
    fun openFrom(rect: Rect?, kind: PillOrigin, text: Pair<String, Boolean> = "Search" to false) {
        originText = text; origin = rect; originKind = if (rect == null) PillOrigin.NONE else kind
        pending = rect != null && owner == null
        if (pending) scope.launch { m.snapTo(1f) }
    }

    /** The Pill the opener showed is hidden while this one stands in for it. */
    fun covers(kind: PillOrigin) = drawn && originKind == kind

    internal fun show(key: Any, s: ActionPillSpec) {
        if (owner === key) { spec = s; return }
        owner = key; spec = s; pending = false
        scope.launch { if (origin != null) { m.snapTo(1f); m.animateTo(0f, MORPH_IN) } else m.snapTo(0f) }
    }

    internal fun update(key: Any, s: ActionPillSpec) { if (owner === key) spec = s }

    /** A finger is driving the back animation: page transitions go linearly, so the page stays under the finger. */
    var seeking = false
        internal set

    /** A back swipe let go short of going back: the Pill settles where it was. */
    internal fun settle() { if (owner != null) scope.launch { m.animateTo(0f, Motion.switcher) } }
    internal fun drag(p: Float) { if (origin != null) scope.launch { m.snapTo(p) } }

    /** The page is going back (or away): the Pill turns back into the one it came from, then the opener's shows again. */
    fun leave() {
        if (pending && owner == null) { pending = false; origin = null; originKind = PillOrigin.NONE; return }
        if (spec == null || owner == null) return
        owner = null
        scope.launch {
            if (origin != null) m.animateTo(1f, MORPH_OUT)
            if (owner == null) { spec = null; origin = null; originKind = PillOrigin.NONE }
        }
    }

    internal fun release(key: Any) { if (owner === key) leave() }
}

val LocalActionPills = staticCompositionLocalOf<ActionPills?> { null }

/** This page's bottom Pill is the split action Pill [spec] (kept up to date as the page changes, e.g. into editing). */
@Composable
fun ActionPill(spec: ActionPillSpec) {
    val host = LocalActionPills.current ?: return
    val key = remember { Any() }
    DisposableEffect(key) {
        host.show(key, spec)
        onDispose { host.release(key) }
    }
    SideEffect { host.update(key, spec) }
}

/**
 * Draws the split Pill above every screen: at rest at the bottom (10 in from the sides and above the navigation bar,
 * riding on the keyboard while typing, up to 600 wide), or [ActionPills.m] of the way back to the Pill it grew from.
 * While it travels it's fully round, its colour and shadow go from the Pill's to the bar's, and its segments fade out
 * over the first half (keeping their width, centred) as the origin Pill's contents fade in over the second.
 */
@Composable
fun BoxScope.ActionPillHost(h: ActionPills, barContent: @Composable () -> Unit) {
    val spec = h.spec
    // set as this frame is applied, so the opener's Pill hides in the very frame this one first draws
    val drawing = (spec != null || h.pending) && h.rest != null
    SideEffect { h.drawn = drawing }
    if (spec == null && !h.pending) return
    val cs = MaterialTheme.colorScheme
    val mi = LocalMi.current
    val density = LocalDensity.current
    val haptics = LocalHaptics.current
    val back = androidx.activity.compose.LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val screenW = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.toFloat()
    var progress by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    // where it rests, measured and remembered for the next page. The swipe is read here, on this slot that stays
    // still, not on the Pill drawn inside it, which moves as it morphs (its own movement would feed back into the drag)
    Box(
        // it stays at the bottom while typing (it doesn't ride the keyboard)
        Modifier.align(Alignment.BottomCenter).fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 10.dp, end = 10.dp, bottom = 10.dp).widthIn(max = PILL_MAX_WIDTH.dp).fillMaxWidth().height(70.dp)
            .onGloballyPositioned { val b = it.boundsInWindow(); if (h.rest != b) h.rest = b }
            // swiping it right goes back with the finger, as a sheet's Pill does: the page slides away under it (the
            // system's back animation, driven by the finger) as the Pill shrinks back toward the one it came from.
            // Past a quarter of the screen (or flung) letting go goes back; short of that everything settles.
            // (Read h.spec here, not spec: these lambdas live as long as the gesture does.)
            .panGesture(
                enabled = spec != null,
                onStart = {
                    progress = 0f
                    h.seeking = true
                    back?.dispatchOnBackStarted(BackEventCompat(0f, 0f, 0f, BackEventCompat.EDGE_LEFT))
                },
                onMove = { x, _ ->
                    val p = (x / screenW).coerceIn(0f, 1f)
                    if ((p > 0.25f) != (progress > 0.25f)) haptics?.tick() // past here, letting go goes back
                    progress = p
                    back?.dispatchOnBackProgressed(BackEventCompat(x * density.density, 0f, p, BackEventCompat.EDGE_LEFT))
                    // leaving the page: the Pill shrinks back toward the one it came from as the page slides
                    if (h.spec?.backPops == true) h.drag(p)
                },
                onRelease = { _, _, vx, _ ->
                    val go = progress > 0.25f || (vx > 0.5f && progress > 0.02f)
                    progress = 0f
                    if (back == null) { if (go) h.spec?.onBack?.invoke() }
                    else if (go) back.onBackPressed() else { back.dispatchOnBackCancelled(); h.settle() }
                    h.seeking = false
                },
            ),
    ) {
        val r = h.rest ?: return@Box
        val m = h.m.value.coerceIn(0f, 1f)
        val from = h.origin
        val at = if (from != null && m > 0f) lerp(r, from, m) else r
        val toBar = h.originKind == PillOrigin.BAR
        val bar = if (toBar) cs.surfaceContainerHigh else with(mi) { cs.pillBar }
        fun rel(x: Rect) = Rect(x.left - r.left, x.top - r.top, x.right - r.left, x.bottom - r.top)
        // the last part of the way back: the Pill it grew from takes over (its shadow, then what it shows)
        val back = ((m - 0.5f) * 2f).coerceIn(0f, 1f)
        with(density) {
            // that Pill's shadow coming back; and, until the page says what its Pill holds, that Pill itself
            if (back > 0f || spec == null) Box(
                Modifier.placedAt(rel(at)).shadow(lerp(0f, if (toBar) 6f else 10f, back).dp, CircleShape)
                    .then(if (spec == null) Modifier.clip(CircleShape).background(bar) else Modifier),
            )
            if (spec != null) MorphSegments(spec.segments, r.width, r.height, from?.let(::rel), m, bar)
            if (back > 0f) Box(Modifier.placedAt(rel(at)).graphicsLayer { alpha = back }, contentAlignment = Alignment.Center) {
                Box(Modifier.wrapContentSize(unbounded = true)) {
                    if (toBar) barContent() else Box(Modifier.requiredWidth(at.width.toDp() - 20.dp)) { SearchFieldLook(h.originText) }
                }
            }
        }
    }
}

/** Lays this out exactly at [r] (px, in its parent's coordinates), whatever the parent's constraints. */
// (its size is reported within the parent's, so a segment at rest, inside its slot, is touchable all over)
private fun Modifier.placedAt(r: Rect): Modifier = offset { IntOffset(r.left.roundToInt(), r.top.roundToInt()) }.layout { measurable, c ->
    val p = measurable.measure(Constraints.fixed(r.width.roundToInt().coerceAtLeast(0), r.height.roundToInt().coerceAtLeast(0)))
    layout(c.constrainWidth(p.width), c.constrainHeight(p.height)) { p.place(0, 0) }
}

/**
 * The Pill's segments: connected buttons that are the whole Pill (nothing behind them), 3 apart, the outer ends as
 * round as the Pill and the inner corners small, as mi.'s connected button groups. The destructive one (always first:
 * the far left) in errorContainer showing only its symbol, the main one (always last: the far right) in primary, any
 * others between in secondaryContainer.
 *
 * They morph out of the Pill they grew from: at [m] 1 every segment is that Pill (its place, its round shape, its
 * colour [bar]), all on top of each other; toward 0 each one travels to its own place, its corners and colour
 * becoming its own, so the Pill visibly splits into its buttons (and going back, they merge into it again). What
 * they say fades over the first half, staying centred. When the set changes (into editing and back) they also morph
 * in place, without springs: one that goes shrinks away, one that comes grows in, and the main one's label crossfades.
 */
@Composable
private fun MorphSegments(segments: List<PillSegment>, w: Float, h: Float, origin: Rect?, m: Float, bar: Color) {
    val cs = MaterialTheme.colorScheme
    val mi = LocalMi.current
    val d = LocalDensity.current
    val outer = with(d) { (if (mi.radius >= 20) 35.dp else (mi.radius * 2).dp).toPx() }
    val inner = with(d) { 6.dp.toPx() }
    val gap = with(d) { 3.dp.toPx() }
    val dangerW = with(d) { 72.dp.toPx() }
    val danger = segments.firstOrNull { it.danger }
    val main = segments.lastOrNull { it.main }
    val middle = segments.filter { it !== danger && it !== main }
    // the ones between, by label, kept while one that's gone shrinks away
    val shown = remember { mutableStateMapOf<String, PillSegment>() }
    middle.forEach { shown[it.label] = it }
    // one that's there when the Pill first shows starts whole (it's part of the morph out of the origin Pill); one that
    // comes later (Refund, back from editing) grows in from nothing, and one that goes shrinks to nothing
    var first by remember { androidx.compose.runtime.mutableStateOf(true) }
    androidx.compose.runtime.SideEffect { first = false }
    val weights = shown.keys.toList().associateWith { label ->
        key(label) {
            val here = middle.any { it.label == label }
            val a = remember { Animatable(if (first) 1f else 0f) }
            androidx.compose.runtime.LaunchedEffect(here) {
                a.animateTo(if (here) 1f else 0f, tween(280, easing = androidx.compose.animation.core.FastOutSlowInEasing))
                if (!here) shown.remove(label)
            }
            a.value
        }
    }
    class Item(val s: PillSegment, val bg: Color, val ink: Color, val fixed: Float?, val weight: Float, val iconOnly: Boolean = false)
    val items = buildList {
        if (danger != null) add(Item(danger, cs.errorContainer, cs.onErrorContainer, dangerW, 1f, iconOnly = true))
        shown.forEach { (label, s) -> val wt = weights[label] ?: 0f; if (wt > 0.01f) add(Item(s, cs.secondaryContainer, cs.onSecondaryContainer, null, wt)) }
        if (main != null) add(Item(main, cs.primary, cs.onPrimary, null, 1f))
    }
    if (items.isEmpty()) return
    // their places at rest: the fixed ones their width, the rest sharing what's left (a gap fades with the one going)
    val gaps = items.zipWithNext { a, b -> gap * minOf(a.weight, b.weight) }
    val flex = (w - items.sumOf { (it.fixed ?: 0f).toDouble() }.toFloat() - gaps.sum()).coerceAtLeast(0f)
    val totalWeight = items.filter { it.fixed == null }.sumOf { it.weight.toDouble() }.toFloat().coerceAtLeast(0.001f)
    var x = 0f
    val textAlpha = (1f - m * 2f).coerceIn(0f, 1f)
    items.forEachIndexed { i, it ->
        val width = it.fixed ?: (flex * it.weight / totalWeight)
        val rest = Rect(x, 0f, x + width, h)
        x += width + (gaps.getOrNull(i) ?: 0f)
        val at = if (origin != null && m > 0f) lerp(rest, origin, m) else rest
        // its corners: its own at rest, fully round (as the Pill it came from) at the other end
        val round = at.height / 2f
        val start = lerp(if (i == 0) outer else inner, round, m)
        val end = lerp(if (i == items.lastIndex) outer else inner, round, m)
        androidx.compose.runtime.key(it.s.label, it.fixed) {
            Segment(
                it.s, RoundedCornerShape(topStart = start, bottomStart = start, topEnd = end, bottomEnd = end),
                Modifier.placedAt(at), androidx.compose.ui.graphics.lerp(it.bg, bar, m), it.ink, textAlpha, clickable = m == 0f, iconOnly = it.iconOnly,
                contentWidth = with(d) { width.toDp() },
            )
        }
    }
}

@Composable
private fun Segment(
    s: PillSegment, shape: Shape, modifier: Modifier, bg: Color, ink: Color, textAlpha: Float, clickable: Boolean,
    iconOnly: Boolean, contentWidth: androidx.compose.ui.unit.Dp,
) {
    // not ready yet (Add Purchase before there's an amount): solid grey with dimmer text, never see-through, easing
    // into its colours as it becomes ready
    val on by animateFloatAsState(if (s.enabled) 1f else 0f, tween(180), label = "enabled")
    val cs = MaterialTheme.colorScheme
    val off = cs.surfaceContainerHighest
    val fill = androidx.compose.ui.graphics.lerp(off, bg, on)
    val text = androidx.compose.ui.graphics.lerp(androidx.compose.ui.graphics.lerp(off, cs.onSurface, 0.45f), ink, on)
    Box(
        modifier.clip(shape).background(fill)
            .clickable(enabled = s.enabled && clickable, role = Role.Button, onClickLabel = s.label, onClick = s.onClick),
        contentAlignment = Alignment.Center,
    ) {
        // what it says keeps its size and stays centred as the segment changes shape, and only fades; it crossfades
        // when it changes (Edit → Save Changes), without moving
        if (textAlpha > 0f) Box(Modifier.requiredWidth(contentWidth).graphicsLayer { alpha = textAlpha }, contentAlignment = Alignment.Center) {
            AnimatedContent(
                s.label to s.icon,
                transitionSpec = { fadeIn(Motion.enter(180)) togetherWith fadeOut(Motion.exit(90)) },
                label = "segment label",
            ) { (label, icon) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, if (iconOnly) label else null, Modifier.size(22.dp), tint = text)
                    if (!iconOnly) {
                        Spacer(Modifier.width(8.dp))
                        Text(label, color = text, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
    }
}

/** A sheet's search field as it looks (for the split Pill to fade into as it shrinks back into a sheet's Pill). */
@Composable
private fun SearchFieldLook(text: Pair<String, Boolean>) {
    val cs = MaterialTheme.colorScheme
    val mi = LocalMi.current
    Row(
        Modifier.fillMaxWidth().height(49.dp).background(with(mi) { cs.pillField }, RoundedCornerShape(minOf(mi.radius * 1.5f, 24.5f).dp)).padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Sym.Search, null, Modifier.padding(end = 12.dp).size(24.dp), tint = cs.onSurfaceVariant)
        Text(text.first, color = if (text.second) cs.onSurface else cs.onSurfaceVariant, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/**
 * One item's summary and its editor, as two pages: opening the editor slides it in over the summary from the right
 * (as a page opening); Back, whether the system gesture or the split Pill's swipe, slides it away with the finger,
 * showing the summary under it, and saving slides it out the same way. A new item ([canLeave] false) has only the
 * editor: Back leaves the whole page.
 */
@Composable
fun EditorPages(editing: Boolean, canLeave: Boolean, onLeave: () -> Unit, summary: @Composable () -> Unit, editor: @Composable () -> Unit) {
    // 0: the editor in place … 1: slid off to the right
    val p = remember { Animatable(if (editing) 0f else 1f) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val leave by androidx.compose.runtime.rememberUpdatedState(onLeave)
    androidx.compose.runtime.LaunchedEffect(editing) {
        if (!editing) p.animateTo(1f, Motion.exit(200))
        else if (canLeave && p.value > 0f) p.animateTo(0f, Motion.switcher)
    }
    androidx.activity.compose.PredictiveBackHandler(enabled = editing && canLeave) { events ->
        try {
            events.collect { p.snapTo(it.progress) }
            leave() // the rest of the way out follows (editing turns false)
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            scope.launch { p.animateTo(0f, Motion.switcher) }
            throw e
        }
    }
    val v = p.value
    Box(Modifier.fillMaxSize()) {
        // under the editor, dimmed a little until it's uncovered (as a page under one sliding away)
        if (!editing || v > 0f) Box(Modifier.fillMaxSize().graphicsLayer { alpha = lerp(0.6f, 1f, v) }) { summary() }
        if (editing || v < 1f) Box(Modifier.fillMaxSize().graphicsLayer { translationX = v * size.width }) { editor() }
    }
}

// The morph between a Pill and its split Pill takes long enough to see it split (or merge), easing at both ends.
private val MORPH_IN = androidx.compose.animation.core.tween<Float>(400, easing = androidx.compose.animation.core.FastOutSlowInEasing)
private val MORPH_OUT = androidx.compose.animation.core.tween<Float>(320, easing = androidx.compose.animation.core.FastOutSlowInEasing)
