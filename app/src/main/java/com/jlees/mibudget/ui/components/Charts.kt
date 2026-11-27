package com.jlees.mibudget.ui.components

import androidx.compose.ui.text.drawText
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.sp

import com.jlees.mibudget.ui.common.Sym

import com.jlees.mibudget.domain.ChartGeometry

import com.jlees.mibudget.ui.theme.readableOn
import androidx.compose.animation.core.Animatable
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.asin
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.material3.Surface
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.composed
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jlees.mibudget.data.ChartType
import com.jlees.mibudget.domain.Money
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt

/** One wedge / bar. [key] is a category id (null = uncategorized). */
data class ChartSlice(val key: String?, val label: String, val value: Long, val color: Color)

/** The key of the slice that adds up the smallest categories when there are too many for a round chart. */
const val OTHER_SLICE = "__other__"

/** At most this many segments on the donut and pie (the rest add up to "Other"), so each one reads as a segment. */
private const val MAX_ROUND_SLICES = 10

/** The smallest a segment is drawn, in degrees: a donut segment needs room for its two round ends, a gap and a bit of colour. */
private const val DONUT_MIN_DEG = 24f
private const val PIE_MIN_DEG = 12f

private val shortDate = DateTimeFormatter.ofPattern("MMM d")

/**
 * The big, centered overview chart at the top of the Purchases tab. The total is shown by the
 * screen right underneath, so the chart itself focuses on the breakdown.
 * Tapping a slice/bar reports it so the screen can drill into that category.
 */
@Composable
fun SpendingChart(
    type: ChartType,
    slices: List<ChartSlice>,
    daily: List<Pair<LocalDate, Long>>,
    caption: String,
    onSliceClick: (ChartSlice) -> Unit,
    modifier: Modifier = Modifier,
) {
    val positive = remember(slices) { slices.filter { it.value > 0 } }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(positive, type) {
        progress.snapTo(0f)
        // Drawn in on mi.'s enter spec. It replays on every month swipe, so it stays short (300ms, not 700).
        progress.animateTo(1f, com.jlees.mibudget.ui.common.Motion.enter(300))
    }
    // Touch & hold a slice / ring to preview its category; keep holding and slide around to preview
    // others. Letting go ends the preview (a quick tap still opens the category).
    var preview by remember(positive, type) { mutableStateOf<ChartSlice?>(null) }
    val haptics = com.jlees.mibudget.ui.common.LocalHaptics.current
    val picker = SlicePreview(
        onStart = { s -> preview = s; haptics?.longPress() },
        onChange = { s -> preview = s; haptics?.tick() },
        onEnd = { preview = null },
    )
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        when (type) {
            ChartType.DONUT, ChartType.PIE -> {
                val donut = type == ChartType.DONUT
                // Biggest first; past MAX_ROUND_SLICES the smallest add up to one "Other" (not a category: not tappable).
                val otherColor = MaterialTheme.colorScheme.outline
                val round = remember(positive, otherColor) {
                    val sorted = positive.sortedByDescending { it.value }
                    val kept = ChartGeometry.keptCount(sorted.size, MAX_ROUND_SLICES)
                    if (kept == sorted.size) sorted
                    else sorted.take(kept) + ChartSlice(OTHER_SLICE, "Other", sorted.drop(kept).sumOf { it.value }, otherColor)
                }
                val sweeps = remember(round, donut) { ChartGeometry.sweeps(round.map { it.value }, if (donut) DONUT_MIN_DEG else PIE_MIN_DEG) }
                val onRoundClick: (ChartSlice) -> Unit = { if (it.key != OTHER_SLICE) onSliceClick(it) }
                Box(Modifier.fillMaxWidth(0.72f).widthIn(max = 280.dp).aspectRatio(1f), contentAlignment = Alignment.Center) {
                    PieCanvas(round, sweeps, donut, progress = { progress.value }, onSliceClick = onRoundClick, selected = preview, picker = picker)
                    val p = preview
                    when {
                        donut && p != null -> PreviewText(p, round, Modifier.fillMaxWidth(0.56f))
                        donut -> CenterCaption(round, caption)
                        p != null -> PreviewCard(p, round)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Legend(round, Modifier.fillMaxWidth(), vertical = false, onSliceClick = onRoundClick, selected = preview)
            }
            ChartType.RADIAL -> {
                val rings = positive.sortedByDescending { it.value }.take(5)
                // Each ring is its category's share of all spending, not just of the five shown.
                val all = positive.sumOf { it.value }
                Box(Modifier.fillMaxWidth(0.72f).widthIn(max = 280.dp).aspectRatio(1f), contentAlignment = Alignment.Center) {
                    RingsCanvas(rings, all, { progress.value }, onSliceClick = onSliceClick, selected = preview, picker = picker)
                    preview?.let { PreviewCard(it, positive) }
                }
                Spacer(Modifier.height(16.dp))
                Legend(rings, Modifier.fillMaxWidth(), vertical = false, onSliceClick = onSliceClick, selected = preview)
            }
            ChartType.BARS -> Bars(positive.take(7), { progress.value }, onSliceClick)
            ChartType.TREND -> Column(Modifier.fillMaxWidth()) {
                TrendCanvas(daily, { progress.value }, Modifier.fillMaxWidth().height(200.dp))
                // beside the chart: the day with the least and the most spent
                ChartGeometry.extremes(daily)?.let { (low, high) ->
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                        Text("Lowest ${Money.format(low.second)} · ${low.first.format(shortDate)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Text("Highest ${Money.format(high.second)} · ${high.first.format(shortDate)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/** Picks the chart style. The choice is saved app-wide, so every chart follows it and it survives restarts. */
@Composable
fun ChartTypeButton(current: ChartType, onType: (ChartType) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        FilledTonalIconButton(onClick = { open = true }) { Icon(chartIcon(current), "Chart style: ${current.label}") }
        com.jlees.mibudget.ui.common.MiMenu(expanded = open, onDismissRequest = { open = false }) {
            ChartType.entries.forEach { t ->
                DropdownMenuItem(
                    text = { Text(t.label) },
                    leadingIcon = { Icon(chartIcon(t), null) },
                    trailingIcon = { if (t == current) Icon(Sym.Check, null) },
                    onClick = { open = false; onType(t) },
                )
            }
        }
    }
}

fun chartIcon(t: ChartType): ImageVector = when (t) {
    ChartType.DONUT -> Sym.DonutLarge
    ChartType.PIE -> Sym.PieChart
    ChartType.BARS -> Sym.BarChart
    ChartType.TREND -> Sym.ShowChart
    ChartType.RADIAL -> Sym.DataUsage
}

@Composable
private fun CenterCaption(slices: List<ChartSlice>, caption: String) {
    // Constrained to the donut hole so long captions ellipsize instead of spilling onto the ring.
    Column(Modifier.fillMaxWidth(0.56f), horizontalAlignment = Alignment.CenterHorizontally) {
        val top = slices.firstOrNull()
        val total = slices.sumOf { it.value }
        if (top != null && total > 0) {
            Text("${top.value * 100 / total}%", style = MaterialTheme.typography.displaySmall, color = top.color, maxLines = 1)
            Text(top.label, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            Text("biggest share", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        } else {
            Text(caption, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
    }
}

/** Material 3 emphasized-decelerate easing. */

/** Callbacks for touch-and-hold previews on the round charts. */
private class SlicePreview(
    val onStart: (ChartSlice) -> Unit,
    val onChange: (ChartSlice) -> Unit,
    val onEnd: () -> Unit,
)


/**
 * Tap = [onTap] the slice under the finger. Touch & hold = preview it, then keep holding and drag
 * to preview whichever slice is under the finger ([pick] with start = false may be more forgiving,
 * e.g. by angle only). While previewing, the gesture is consumed so the list doesn't scroll; a drag
 * that starts before the hold kicks in is left alone, so the screen still scrolls normally.
 */
private fun Modifier.slicePicker(
    key: Any?,
    pick: (pos: Offset, size: IntSize, start: Boolean) -> ChartSlice?,
    onTap: (ChartSlice) -> Unit,
    picker: SlicePreview,
): Modifier = composed {
    val currentPick by rememberUpdatedState(pick)
    val currentTap by rememberUpdatedState(onTap)
    val currentPicker by rememberUpdatedState(picker)
    pointerInput(key) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            // 0 = still holding, 1 = lifted (tap), 2 = moved away / cancelled.
            var outcome = 0
            var last = down.position
            val held = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                while (outcome == 0) {
                    val c = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                    when {
                        c == null -> outcome = 2
                        !c.pressed -> { last = c.position; outcome = if (c.isConsumed) 2 else 1 }
                        (c.position - down.position).getDistance() > viewConfiguration.touchSlop -> outcome = 2
                    }
                }
            } == null
            if (outcome == 1) {
                currentPick(last, size, true)?.let(currentTap)
                return@awaitEachGesture
            }
            if (!held) return@awaitEachGesture
            var current = currentPick(down.position, size, true) ?: return@awaitEachGesture
            currentPicker.onStart(current)
            try {
                while (true) {
                    val c = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                    c.consume()
                    if (!c.pressed) break
                    val s = currentPick(c.position, size, false)
                    if (s != null && s != current) {
                        current = s
                        currentPicker.onChange(s)
                    }
                }
            } finally {
                currentPicker.onEnd()
            }
        }
    }
}

/** Degrees clockwise from 12 o'clock to [pos], 0–360. */
private fun angleAt(pos: Offset, size: IntSize): Float {
    val dx = pos.x - size.width / 2f
    val dy = pos.y - size.height / 2f
    var angle = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat() + 90f
    if (angle < 0) angle += 360f
    return angle
}

/** The slice at [angle] degrees from 12 o'clock, by the angles the slices are drawn at ([sweeps]). */
private fun sliceAtAngle(angle: Float, slices: List<ChartSlice>, sweeps: List<Float>): ChartSlice? {
    if (slices.isEmpty()) return null
    var acc = 0f
    slices.forEachIndexed { i, s ->
        acc += sweeps[i]
        if (angle <= acc) return s
    }
    return slices.lastOrNull()
}

/** The category being previewed: its share, name and amount (inside the donut's hole). */
@Composable
private fun PreviewText(s: ChartSlice, slices: List<ChartSlice>, modifier: Modifier = Modifier) {
    val total = slices.sumOf { it.value }.coerceAtLeast(1)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "${s.value * 100 / total}%",
            style = MaterialTheme.typography.displaySmall,
            color = s.color.readableOn(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface),
            maxLines = 1,
        )
        Text(s.label, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        Text(Money.format(s.value), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

/** The same preview as a floating card, for the pie and rings (no hole to put it in). */
@Composable
private fun PreviewCard(s: ChartSlice, slices: List<ChartSlice>) {
    val total = slices.sumOf { it.value }.coerceAtLeast(1)
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 6.dp,
        modifier = Modifier.widthIn(max = 200.dp),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(s.color))
                Spacer(Modifier.width(6.dp))
                Text(s.label, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(Money.format(s.value), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text("${s.value * 100 / total}% of spending", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

/** Which slice (if any) sits at [pos], by angle from 12 o'clock. */
private fun sliceAt(pos: Offset, size: IntSize, slices: List<ChartSlice>, sweeps: List<Float>, hole: Float): ChartSlice? {
    val c = Offset(size.width / 2f, size.height / 2f)
    val r = min(size.width, size.height) / 2f
    val dist = (pos - c).getDistance()
    if (dist > r || dist < r * hole) return null
    return sliceAtAngle(angleAt(pos, size), slices, sweeps)
}

private fun pointOn(center: Offset, radius: Float, degrees: Float): Offset {
    val rad = Math.toRadians(degrees.toDouble())
    return Offset(center.x + radius * cos(rad).toFloat(), center.y + radius * sin(rad).toFloat())
}

/**
 * Donut / pie in the Material 3 Expressive style:
 *  • donut: a thick ring of separate, fully rounded segments with clear gaps (like Material's
 *    segmented progress indicators); shares too small for a segment become dots so they're never lost;
 *  • pie: wedges pulled slightly apart, each with softly rounded corners.
 */
@Composable
private fun PieCanvas(
    slices: List<ChartSlice>,
    /** Each slice's angle (proportional, but never too small to draw; see ChartGeometry.sweeps). */
    sweeps: List<Float>,
    donut: Boolean,
    progress: () -> Float,
    onSliceClick: (ChartSlice) -> Unit,
    selected: ChartSlice?,
    picker: SlicePreview,
) {
    val total = slices.sumOf { it.value }.toFloat()
    val empty = MaterialTheme.colorScheme.surfaceContainerHighest
    val density = LocalDensity.current
    val gapPx = with(density) { 4.dp.toPx() }
    val cornerPx = with(density) { 10.dp.toPx() }
    val popPx = with(density) { 6.dp.toPx() }
    // While previewing, the other slices fade back so the picked one stands out.
    fun colorOf(s: ChartSlice) = if (selected != null && s != selected) s.color.copy(alpha = 0.3f) else s.color
    Canvas(
        Modifier
            .fillMaxSize()
            .slicePicker(
                key = slices to donut,
                pick = { pos, size, start ->
                    if (start) sliceAt(pos, size, slices, sweeps, hole = if (donut) 0.6f else 0f)
                    else {
                        // Sliding around: only the angle matters, wherever the finger is — except
                        // right at the middle, where the angle jumps around.
                        val c = Offset(size.width / 2f, size.height / 2f)
                        if ((pos - c).getDistance() < min(size.width, size.height) * 0.08f) null
                        else sliceAtAngle(angleAt(pos, size), slices, sweeps)
                    }
                },
                onTap = onSliceClick,
                picker = picker,
            )
    ) {
        val d = min(size.width, size.height)
        val center = Offset(size.width / 2f, size.height / 2f)
        val p = progress()
        if (donut) {
            val stroke = d * 0.13f
            val radius = (d - stroke) / 2f
            val arcTopLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2, radius * 2)
            // No track behind the segments — only an empty ring when there's nothing to show.
            if (total <= 0f) {
                drawArc(empty, 0f, 360f, false, arcTopLeft, arcSize, style = Stroke(stroke))
                return@Canvas
            }
            // Each rounded cap pokes stroke/2 past the arc's end: leave room for both caps plus a gap.
            val capDeg = Math.toDegrees((stroke / 2f / radius).toDouble()).toFloat()
            val gapDeg = Math.toDegrees((gapPx / radius).toDouble()).toFloat()
            val trim = if (slices.size > 1) capDeg * 2 + gapDeg else 0f
            // Every segment is at least DONUT_MIN_DEG wide (room for its round ends, the gap and some colour), so small
            // categories are short segments like the rest rather than blobs. While it grows in, a segment appears once
            // there's room for it.
            var start = -90f
            slices.forEachIndexed { i, s ->
                val sweep = sweeps[i] * p
                val visible = sweep - trim
                if (trim == 0f) drawArc(colorOf(s), start, sweep, false, arcTopLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                else if (visible > 0f) drawArc(colorOf(s), start + trim / 2, visible.coerceAtLeast(0.1f), false, arcTopLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                start += sweep
            }
        } else {
            val radius = d / 2f
            if (total <= 0f) {
                drawCircle(empty, radius, center)
                return@Canvas
            }
            var start = -90f
            slices.forEachIndexed { i, s ->
                val sweep = sweeps[i] * p
                if (sweep > 0f) {
                    val wedge = if (slices.size == 1 || sweep >= 359.9f) {
                        Path().apply { addOval(Rect(center, radius)) }
                    } else roundedWedge(center, radius, start, sweep, gapPx, cornerPx)
                    if (wedge != null) {
                        if (s == selected && slices.size > 1) {
                            // The previewed wedge pops out a little.
                            val out = pointOn(Offset.Zero, popPx, start + sweep / 2f)
                            translate(out.x, out.y) { drawPath(wedge, colorOf(s)) }
                        } else drawPath(wedge, colorOf(s))
                    }
                }
                start += sweep
            }
        }
    }
}

/**
 * One pie wedge with a constant-width gap to its neighbours and every corner rounded (the tip at
 * the center and both outer corners), Material 3 Expressive style. Returns null when the wedge is
 * too thin to draw.
 */
private fun roundedWedge(center: Offset, radius: Float, startDeg: Float, sweepDeg: Float, gap: Float, corner: Float): Path? {
    val half = gap / 2f
    // Trim the outer arc so the gap is the same width all the way out.
    val trim = Math.toDegrees(asin((half / radius).coerceAtMost(1f).toDouble())).toFloat()
    val a0 = startDeg + trim
    val a1 = startDeg + sweepDeg - trim
    if (a1 <= a0) return null
    // Tip: where the two edges (each moved half a gap inward) meet. Wide wedges keep the center.
    val tipDistance = if (sweepDeg < 180f) (half / sin(Math.toRadians(sweepDeg / 2.0)).toFloat()).coerceAtMost(radius * 0.5f) else 0f
    val tip = pointOn(center, tipDistance, startDeg + sweepDeg / 2f)
    val p1 = pointOn(center, radius, a0)
    val p2 = pointOn(center, radius, a1)
    val edge = (p1 - tip).getDistance()
    if (edge <= 0f) return null
    val d1 = (p1 - tip) / edge
    val d2 = (p2 - tip) / (p2 - tip).getDistance()
    val arcLength = Math.toRadians((a1 - a0).toDouble()).toFloat() * radius
    val r = minOf(corner, edge * 0.35f, arcLength * 0.35f)
    val rTip = minOf(corner * 0.6f, edge * 0.3f)
    val rDeg = Math.toDegrees((r / radius).toDouble()).toFloat()
    val tipOut = tip + d1 * rTip
    val tipIn = tip + d2 * rTip
    val bounds = Rect(center, radius)
    return Path().apply {
        moveTo(tipOut.x, tipOut.y)
        val beforeP1 = p1 - d1 * r
        lineTo(beforeP1.x, beforeP1.y)
        val afterP1 = pointOn(center, radius, a0 + rDeg)
        quadraticTo(p1.x, p1.y, afterP1.x, afterP1.y)
        arcTo(bounds, a0 + rDeg, (a1 - a0) - rDeg * 2, forceMoveTo = false)
        val afterP2 = p2 - d2 * r
        quadraticTo(p2.x, p2.y, afterP2.x, afterP2.y)
        lineTo(tipIn.x, tipIn.y)
        quadraticTo(tip.x, tip.y, tipOut.x, tipOut.y)
        close()
    }
}

/**
 * Concentric rings, one per top category, styled like Material 3 Expressive circular progress
 * indicators: a rounded indicator showing the category's share, a gap, then a tonal track in
 * the ring's own color.
 */
@Composable
private fun RingsCanvas(
    slices: List<ChartSlice>,
    /** All spending; each ring shows its slice's share of it. */
    total: Long,
    progress: () -> Float,
    onSliceClick: (ChartSlice) -> Unit,
    selected: ChartSlice?,
    picker: SlicePreview,
) {
    val sum = total.toFloat()
    val gapPx = with(LocalDensity.current) { 4.dp.toPx() }
    Canvas(
        Modifier
            .fillMaxSize()
            .slicePicker(
                key = slices,
                pick = { pos, size, start ->
                    // Each ring is a band of the radius (same layout as the drawing below).
                    val d = min(size.width, size.height).toFloat()
                    val n = slices.size.coerceAtLeast(1)
                    val band = d / 2f * 0.8f / n
                    val dist = (pos - Offset(size.width / 2f, size.height / 2f)).getDistance()
                    val i = ((d / 2f - dist) / band).toInt()
                    when {
                        dist <= d / 2f && i in slices.indices -> slices[i]
                        // Sliding past the outer or inner ring keeps the nearest one.
                        !start && dist > d / 2f -> slices.firstOrNull()
                        !start && i >= slices.size -> slices.lastOrNull()
                        else -> null
                    }
                },
                onTap = onSliceClick,
                picker = picker,
            )
    ) {
        val d = min(size.width, size.height)
        val center = Offset(size.width / 2f, size.height / 2f)
        val p = progress()
        val n = slices.size.coerceAtLeast(1)
        // Rings fill the outer ~80% of the radius with space between them; the middle stays open.
        val band = d / 2f * 0.8f / n
        val stroke = band * 0.7f
        slices.forEachIndexed { i, s ->
            val radius = d / 2f - stroke / 2f - i * band
            val tl = Offset(center.x - radius, center.y - radius)
            val sz = Size(radius * 2, radius * 2)
            val sweep = if (sum > 0f) 360f * (s.value / sum) * p else 0f
            val capDeg = Math.toDegrees((stroke / 2f / radius).toDouble()).toFloat()
            val gapDeg = Math.toDegrees((gapPx / radius).toDouble()).toFloat()
            // Visible extents (caps included): indicator from 12 o'clock to the share's angle, then a
            // gap, then the track around to just before 12 o'clock again.
            // While previewing, the other rings fade back so the picked one stands out.
            val dim = selected != null && s != selected
            val color = if (dim) s.color.copy(alpha = 0.3f) else s.color
            // A share too small for an arc still shows as a round dot at 12 o'clock (one round end's worth each side),
            // and the track starts after what's actually drawn, so the two never overlap.
            val shown = if (sweep > 0f) maxOf(sweep, capDeg * 2) else 0f
            if (shown < 360f - 0.5f) {
                val trackStart = -90f + shown + gapDeg + capDeg
                val trackSweep = 360f - shown - gapDeg * 2 - capDeg * 2
                if (trackSweep > 0f) drawArc(s.color.copy(alpha = if (dim) 0.1f else 0.24f), trackStart, trackSweep, false, tl, sz, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            if (shown > 0f) {
                drawArc(color, -90f + capDeg, (shown - capDeg * 2).coerceAtLeast(0.1f), false, tl, sz, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
    }
}

@Composable
private fun Bars(slices: List<ChartSlice>, progress: () -> Float, onSliceClick: (ChartSlice) -> Unit) {
    val max = slices.maxOfOrNull { it.value }?.toFloat() ?: 1f
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        slices.forEach { s ->
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onSliceClick(s) }) {
                Row {
                    Text(s.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(Money.format(s.value), style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth().height(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                    Box(
                        Modifier
                            .fillMaxWidth((s.value / max * progress()).coerceIn(0.02f, 1f))
                            .height(12.dp)
                            .clip(CircleShape)
                            .background(s.color)
                    )
                }
            }
        }
        if (slices.isEmpty()) Text("No spending in this period", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Cumulative spending across the period. */
/**
 * The month's running total as a line: primary, 2dp, round caps, with a gradient of the line colour under it (25% to
 * 0), a dot per day (smaller past 60), the latest circled; dashed grid lines with their values at the left (44 wide)
 * and the first and last dates under a solid baseline. Touch: a line at the finger, a dot on the line and a tooltip
 * with that day's total, above the point (below it when there's no room).
 */
@Composable
private fun TrendCanvas(daily: List<Pair<LocalDate, Long>>, progress: () -> Float, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    val line = cs.primary
    val grid = cs.outlineVariant
    val label = cs.onSurfaceVariant
    val cumulative = remember(daily) { var acc = 0L; daily.map { acc += it.second; acc } }
    val steps = remember(cumulative) { ChartGeometry.gridSteps(maxOf(cumulative.maxOrNull() ?: 0L, 0L)) }
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = label)
    var touch by remember(daily) { mutableStateOf<Int?>(null) }
    Canvas(
        modifier.pointerInput(daily) {
            fun index(x: Float): Int? {
                if (cumulative.size < 2) return null
                val left = 44.dp.toPx()
                return (((x - left) / (size.width - left)) * (cumulative.size - 1)).toInt().coerceIn(0, cumulative.size - 1)
            }
            awaitPointerEventScope {
                while (true) {
                    val e = awaitPointerEvent()
                    val c = e.changes.firstOrNull() ?: continue
                    touch = if (c.pressed) index(c.position.x) else null
                }
            }
        }
    ) {
        val left = 44.dp.toPx()
        val top = 6.dp.toPx()
        val bottomKeep = 16.dp.toPx()
        val w = size.width - left
        val h = size.height - top - bottomKeep
        val maxV = (steps.lastOrNull() ?: 1L).coerceAtLeast(1L).toFloat()
        fun yOf(v: Float) = top + h - (v.coerceAtLeast(0f) / maxV) * h
        val dash = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx()))
        // grid: dashed lines with their values; a solid baseline
        steps.forEach { v ->
            val y = yOf(v.toFloat())
            if (v == 0L) drawLine(grid, Offset(left, y), Offset(size.width, y), 1.dp.toPx())
            else drawLine(grid, Offset(left, y), Offset(size.width, y), 1.dp.toPx(), pathEffect = dash)
            val t = measurer.measure(Money.formatCompact(v), labelStyle)
            drawText(t, topLeft = Offset(0f, y - t.size.height / 2f))
        }
        if (cumulative.size < 2) return@Canvas
        // the first and last dates under the baseline
        val first = measurer.measure(daily.first().first.format(shortDate), labelStyle)
        val last = measurer.measure(daily.last().first.format(shortDate), labelStyle)
        drawText(first, topLeft = Offset(left, top + h + 3.dp.toPx()))
        drawText(last, topLeft = Offset(size.width - last.size.width, top + h + 3.dp.toPx()))
        val p = progress()
        val base = yOf(0f)
        val points = cumulative.mapIndexed { i, v -> Offset(left + w * i / (cumulative.size - 1), base + (yOf(v.toFloat()) - base) * p) }
        val path = Path().apply { moveTo(points.first().x, points.first().y); points.drop(1).forEach { lineTo(it.x, it.y) } }
        val fill = Path().apply { addPath(path); lineTo(points.last().x, base); lineTo(points.first().x, base); close() }
        drawPath(fill, Brush.verticalGradient(listOf(line.copy(alpha = 0.25f), line.copy(alpha = 0f)), startY = top, endY = base))
        drawPath(path, line, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
        val dot = (if (points.size > 60) 1.5f else 3f).dp.toPx() / 2f * 2f
        points.forEach { drawCircle(line, dot / 2f, it) }
        drawCircle(line, 5.dp.toPx(), points.last(), style = Stroke(1.5.dp.toPx()))
        // touch: a line at the finger, a dot on the line, and a tooltip
        touch?.let { i ->
            val pt = points[i]
            drawLine(label.copy(alpha = 0.5f), Offset(pt.x, top), Offset(pt.x, base), 1.dp.toPx())
            drawCircle(line, 5.dp.toPx(), pt)
            val t = measurer.measure("${daily[i].first.format(shortDate)} · ${Money.format(cumulative[i])}", labelStyle.copy(color = cs.inverseOnSurface))
            val pad = 6.dp.toPx()
            val bw = t.size.width + pad * 2
            val bh = t.size.height + pad * 2
            val above = pt.y - 8.dp.toPx() - bh >= 0f
            val bx = (pt.x - bw / 2).coerceIn(left, size.width - bw)
            val by = if (above) pt.y - 8.dp.toPx() - bh else pt.y + 8.dp.toPx()
            drawRoundRect(cs.inverseSurface, Offset(bx, by), androidx.compose.ui.geometry.Size(bw, bh), androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()))
            drawText(t, topLeft = Offset(bx + pad, by + pad))
        }
    }
}

/** Legend as small tonal pills (color dot, name, share), tappable to drill in. */
@Composable
fun Legend(
    slices: List<ChartSlice>,
    modifier: Modifier = Modifier,
    vertical: Boolean,
    onSliceClick: (ChartSlice) -> Unit,
    /** The category being previewed on the chart (its chip is highlighted). */
    selected: ChartSlice? = null,
) {
    val total = slices.sumOf { it.value }.coerceAtLeast(1)
    val items: @Composable () -> Unit = {
        slices.take(if (vertical) 6 else 12).forEach { s ->
            Row(
                Modifier
                    .padding(vertical = 3.dp)
                    .clip(CircleShape)
                    .background(if (s == selected) s.color.copy(alpha = 0.28f) else MaterialTheme.colorScheme.surfaceContainerHigh)
                    // Compose widens small touch targets to 48 dp for taps; TalkBack reads it as a button.
                    .clickable(onClickLabel = "Show ${s.label}", role = androidx.compose.ui.semantics.Role.Button) { onSliceClick(s) }
                    .padding(start = 8.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(s.color))
                Spacer(Modifier.width(6.dp))
                Text(s.label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = if (vertical) Modifier.weight(1f, fill = false) else Modifier.widthIn(max = 140.dp))
                Spacer(Modifier.width(6.dp))
                Text("${s.value * 100 / total}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (slices.size > 6 && vertical) Text("+${slices.size - 6} more", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
    }
    if (vertical) Column(modifier) { items() }
    else FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) { items() }
}
