// Pages opened from the main bar (Library, Settings, Calendar) come in over the start screen as sheets that follow the
// finger, and their search Pill grows out of the main bar's Pill as they come ("One Pill, always morphing"), shrinking
// back into it as they go. Back, the ✕ or swiping the sheet's Pill toward the side it came from sends them away.
package com.jlees.mibudget.ui.common

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Which page the main bar has open (or is bringing in), and how far it's out: [slide] 0 shown … 1 off screen. Held
 * above the start screen so a page stays open while something is opened from it and comes back.
 */
@Stable
class BarSheets(private val scope: CoroutineScope) {
    var page by mutableStateOf<BarPage?>(null)
        private set
    val slide = Animatable(1f)
    /** The main bar's Pill, in window coordinates: where a sheet's Pill grows from and shrinks back into. */
    var barPill by mutableStateOf<Rect?>(null)
    /**
     * Where a sheet's Pill rests (window px; the same for every page). Remembered between opens, so a sheet's Pill can
     * start morphing on its very first frame instead of laying out full width for a frame and jumping (a flicker).
     * Until it's known, the bar's own Pill stays on screen.
     */
    var sheetPill by mutableStateOf<Rect?>(null)
    /** True while a page opened from a sheet shows its split Pill grown out of the sheet's (the sheet's Pill hides). */
    var coveredBy: () -> Boolean = { false }
    /** What the open sheet's search field shows (its query, or its placeholder: false), for a split Pill shrinking back into it. */
    var sheetPillText = "Search" to false
    /** The Library's page has been built once since the launch (see LibrarySheet: the first open builds it once open). */
    var libraryBuilt = false
    /** The Library's page (kept while it's closed). */
    var libraryPage by mutableStateOf(0)

    /** A swipe on the main bar is bringing [p] in, [progress] of the way. */
    fun drag(p: BarPage, progress: Float) {
        page = p
        scope.launch { slide.snapTo(1f - progress) }
    }

    /** The swipe let go: open (or not), carrying on at the finger's speed ([speed]: slide fractions a second toward open). */
    fun settle(open: Boolean, speed: Float) = scope.launch {
        if (open) slide.animateTo(0f, Motion.switcher, initialVelocity = -speed)
        else { slide.animateTo(1f, Motion.switcher, initialVelocity = -speed); page = null }
    }

    /** A back swipe on the sheet's Pill let go short: back in place, from the finger's [speed] (fractions a second, outward). */
    fun springBack(speed: Float) = scope.launch { slide.animateTo(0f, Motion.switcher, initialVelocity = speed) }

    /** A button opened [p]: it slides in on the switcher spring. */
    fun open(p: BarPage) {
        if (page == p && slide.value < 0.5f) return
        page = p
        scope.launch { slide.snapTo(1f); slide.animateTo(0f, Motion.switcher) }
    }

    /** Back or ✕: out in exit(200); flung: on the switcher spring at the finger's [speed] (fractions a second, outward). */
    fun close(speed: Float? = null) = scope.launch {
        if (speed != null) slide.animateTo(1f, Motion.switcher, initialVelocity = speed) else slide.animateTo(1f, Motion.exit(200))
        page = null
    }

    /** Shown at once (coming back to the start screen with a page open). */
    fun snapOpen(p: BarPage) { page = p; scope.launch { slide.snapTo(0f) } }
}

/**
 * What a page's Pill needs to morph with its sheet: how far the sheet is out, which side it came from, the bar's Pill
 * to grow from, the sheet's current offset (cancelled, so the Pill holds still while the sheet slides), and the swipe
 * that sends the sheet back, following the finger.
 */
class SheetMorph(
    val page: BarPage,
    val slide: () -> Float,
    val morphFrom: () -> Rect?,
    /** Where the sheet's Pill rests, as last measured (any page): read before its own first layout. */
    val restCache: () -> Rect?,
    val onRest: (Rect) -> Unit,
    /** The page opened from this sheet has its own Pill, grown out of this one: this one hides meanwhile. */
    val covered: () -> Boolean = { false },
    val offset: () -> Offset,
    /** How far (px) a back swipe has to go to send the sheet all the way: its width, or its height for one from the bottom. */
    val extent: () -> Float,
    val drag: (Float) -> Unit,
    val release: (finish: Boolean, speed: Float) -> Unit,
    /** The bar Pill's buttons, faded in as the sheet's Pill becomes it. */
    val morphContent: @Composable () -> Unit,
)

val LocalSheetMorph = staticCompositionLocalOf<SheetMorph?> { null }

/**
 * The main bar's pages, kept across the activity being recreated (Look & Feel › Rotate Screen turning the screen, a
 * phone setting changing): an open page comes back open, the Library on the page it was on.
 */
@Composable
fun rememberBarSheets(scope: CoroutineScope): BarSheets = androidx.compose.runtime.saveable.rememberSaveable(
    saver = androidx.compose.runtime.saveable.Saver(
        save = { listOf(it.page?.name ?: "", it.libraryPage) },
        restore = { saved ->
            BarSheets(scope).apply {
                libraryPage = saved[1] as Int
                (saved[0] as String).takeIf { n -> n.isNotEmpty() }?.let { n -> snapOpen(BarPage.valueOf(n)) }
            }
        },
    ),
) { BarSheets(scope) }

/**
 * The open page, over the start screen: what's behind dims with scrim at 32% as it comes in, and it slides from its
 * side ([BarPage]). Back closes it one level at a time (levels inside handle Back first).
 */
@Composable
fun BarSheetHost(sheets: BarSheets, morphContent: @Composable () -> Unit, content: @Composable (BarPage) -> Unit) {
    val page = sheets.page ?: return
    var size by remember { mutableStateOf(IntSize.Zero) }
    BackHandler { sheets.close() }
    val morph = remember(page) {
        SheetMorph(
            page = page, slide = { sheets.slide.value }, morphFrom = { sheets.barPill },
            restCache = { sheets.sheetPill }, onRest = { if (sheets.sheetPill != it) sheets.sheetPill = it },
            covered = { sheets.coveredBy() },
            offset = {
                val s = sheets.slide.value
                when (page) { BarPage.LEFT -> Offset(-s * size.width, 0f); BarPage.RIGHT -> Offset(s * size.width, 0f); BarPage.UP -> Offset(0f, s * size.height) }
            },
            extent = { if (page == BarPage.UP) size.height.toFloat() else size.width.toFloat() },
            drag = { p -> sheets.drag(page, 1f - p) },
            release = { finish, speed -> if (finish) sheets.close(speed) else sheets.springBack(speed) },
            morphContent = morphContent,
        )
    }
    val scrim = MaterialTheme.colorScheme.scrim
    Box(Modifier.fillMaxSize().zIndex(50f).onSizeChanged { size = it }) {
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = (1f - sheets.slide.value).coerceIn(0f, 1f) }.background(scrim.copy(alpha = 0.32f))
            .clickable(MutableInteractionSource(), null) { sheets.close() })
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                val s = sheets.slide.value
                when (page) { BarPage.LEFT -> translationX = -s * size.width; BarPage.RIGHT -> translationX = s * size.width; BarPage.UP -> translationY = s * size.height }
                shadowElevation = 8.dp.toPx()
            }.background(MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            CompositionLocalProvider(LocalSheetMorph provides morph, LocalSheetClose provides { sheets.close(); Unit }) { content(page) }
        }
    }
}

/**
 * Sizes and places a Pill [m] of the way from its own place ([rest], window px) to [target]'s: the same size and spot as
 * the target at 1. Without a target (or before its own place is known) it lays out as usual.
 */
fun Modifier.morphToward(target: Rect?, rest: Rect?, m: Float) = layout { measurable, c ->
    if (target == null || rest == null || m <= 0f) {
        val p = measurable.measure(c)
        return@layout layout(p.width, p.height) { p.place(0, 0) }
    }
    val w = lerp(rest.width, target.width, m).roundToInt().coerceAtLeast(1)
    val h = lerp(rest.height, target.height, m).roundToInt().coerceAtLeast(1)
    val p = measurable.measure(Constraints.fixed(w, h))
    layout(rest.width.roundToInt(), rest.height.roundToInt()) {
        p.place(((target.left - rest.left) * m).roundToInt(), ((target.top - rest.top) * m).roundToInt())
    }
}
