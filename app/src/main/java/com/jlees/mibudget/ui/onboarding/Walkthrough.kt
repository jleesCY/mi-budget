// The gesture walkthrough, as mi. Notes' and mi. Browser's: a hands-on tour over a light copy of mi. Budget itself,
// everything where it really is: the start screen (this month, its purchases, the main bar at the bottom), and the
// Library, Settings and the Calendar sliding in from their sides with their search Pills, each Pill growing out of the
// bar's. It teaches only the gestures: the buttons explain themselves. Each step asks for the real gesture and the copy
// responds as the app does; a caption card says what to try, ticking it off as you do, and everything but what to try
// dims. Nothing real is touched: no purchases or settings change, except the flag that it was seen. Shows once to
// everyone (AppSettings.walkthroughDone), and again from Settings › About › Gesture Walkthrough.
package com.jlees.mibudget.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import com.jlees.mibudget.ui.common.COMFORTABLE_WIDTH
import com.jlees.mibudget.ui.common.LocalHaptics
import com.jlees.mibudget.ui.common.LocalSettings
import com.jlees.mibudget.ui.common.MainBarPillLook
import com.jlees.mibudget.ui.common.MiListRow
import com.jlees.mibudget.ui.common.Motion
import com.jlees.mibudget.ui.common.Sym
import com.jlees.mibudget.ui.common.listReveal
import com.jlees.mibudget.ui.common.panGesture
import com.jlees.mibudget.ui.common.popScale
import com.jlees.mibudget.ui.settings.Item
import com.jlees.mibudget.ui.settings.segment
import com.jlees.mibudget.ui.theme.LocalMi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/** One thing a step asks you to try: a line in the caption's checklist. [leaves]: going back out of the step's page. */
private enum class Task(val label: String, val leaves: Boolean = false) {
    LEAVE_LIBRARY("Swipe the search Pill left to go back", leaves = true),
    LEAVE_SETTINGS("Swipe the search Pill right to go back", leaves = true),
    LEAVE_CALENDAR("Swipe the search Pill down to go back", leaves = true),
    REFUND("Swipe a purchase right to refund it"),
    DELETE("Swipe a purchase left to delete it"),
}

/** Where a page comes in from: the side of the button that opens it (the Calendar rises from the bar). */
private enum class Side { LEFT, RIGHT, UP }

/** The pages the main bar's Pill opens. */
private enum class Page(val title: String, val side: Side) { LIBRARY("Subscriptions", Side.LEFT), SETTINGS("Settings", Side.RIGHT), CALENDAR("Calendar", Side.UP) }

/** The tour. [inside]: the caption once the step's page is open. [chips]: what the step needs, ticked off as it's done. */
private enum class Step(val title: String, val text: String, val inside: String = "", val chips: List<Task> = emptyList(), val page: Page? = null) {
    LIBRARY("Your Library", "Swipe the Pill right for your Library.", "Subscriptions, budgets and tools live here.",
        listOf(Task.LEAVE_LIBRARY), Page.LIBRARY),
    SETTINGS("Settings", "Swipe the Pill left for Settings.", "Every setting is here. Inside a section, swipe its Pill right to go up a level.",
        listOf(Task.LEAVE_SETTINGS), Page.SETTINGS),
    CALENDAR("Calendar", "Swipe the Pill up for the calendar.", "Each day is shaded by what you spent.",
        listOf(Task.LEAVE_CALENDAR), Page.CALENDAR),
    SEARCH("Search", "Hold the Pill to search your purchases."),
    MONTHS("Another Month", "Swipe this month sideways to see the one before."),
    ROWS("Refund or Delete", "Swipe a purchase right to refund it, or left to delete it.", chips = listOf(Task.REFUND, Task.DELETE)),
    DONE("You're All Set", "That's every gesture. Enjoy mi. Budget."),
}

/** Which way the fingertip hint moves. */
private enum class Move { HOLD, UP, DOWN, LEFT, RIGHT }

@Composable
fun Walkthrough(onDone: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val mi = LocalMi.current
    val settings = LocalSettings.current
    val haptics = LocalHaptics.current
    val scope = rememberCoroutineScope()
    val px = LocalDensity.current.density
    val screenW = LocalConfiguration.current.screenWidthDp.toFloat()

    // gestures that are off in Settings › Gestures aren't taught
    val steps = remember(settings.longPressSearch, settings.swipeMonths) {
        Step.entries.filter { (it != Step.SEARCH || settings.longPressSearch) && (it != Step.MONTHS || settings.swipeMonths) }
    }
    var step by remember { mutableStateOf(steps.first()) }
    var stepDone by remember { mutableStateOf(false) }
    val lit = remember { mutableStateListOf<Task>() }
    fun advance() { val i = steps.indexOf(step); if (i < steps.lastIndex) { step = steps[i + 1]; stepDone = false; lit.clear() } }
    fun complete() {
        if (stepDone) return
        stepDone = true; haptics?.tick()
        val at = step
        scope.launch { delay(900); if (step == at) advance() }
    }
    fun light(t: Task) {
        if (t !in step.chips || t in lit) return
        lit += t; haptics?.tick()
        if (step.chips.all { it in lit }) complete()
    }
    BackHandler { onDone() }

    // ---- the copy's state
    val pageP = remember { Animatable(0f) }  // the step's page: 0 away … 1 in place
    val search = remember { Animatable(0f) } // the bar's Pill widening into the search field
    var earlier by remember { mutableStateOf(false) } // swiped to last month
    var pageOpened by remember { mutableStateOf(false) }
    val rects = remember { mutableStateMapOf<String, Rect>() }
    LaunchedEffect(step) { pageP.snapTo(0f); pageOpened = false }

    Box(
        Modifier.fillMaxSize().zIndex(1000f).background(with(mi) { cs.homeBackground })
            .clickable(MutableInteractionSource(), null) {}, // the real screen stays out of reach until the tour ends
    ) {
        // ---- the start screen, as the app lays it out: this month, then its purchases
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
            Column(
                Modifier.fillMaxWidth().onGloballyPositioned { rects["month"] = it.boundsInRoot() }
                    .panGesture(enabled = step == Step.MONTHS, onMove = { _, _ -> }, onRelease = { x, y, _, _ ->
                        if (abs(x) > abs(y) && abs(x) > 28f) { earlier = !earlier; haptics?.tick(); complete() }
                    })
                    .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 12.dp),
            ) {
                Text(if (earlier) "October" else "November", color = cs.onSurface, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
                Text(if (earlier) "$1,512.04" else "$1,248.30", Modifier.padding(top = 12.dp).align(Alignment.CenterHorizontally),
                    color = cs.onSurface, fontSize = 48.sp, fontWeight = FontWeight.SemiBold)
                Text("spent ${if (earlier) "in October" else "this month"} · ${if (earlier) 41 else 4} purchases", Modifier.align(Alignment.CenterHorizontally),
                    color = cs.onSurfaceVariant, fontSize = 14.sp)
            }
            Text("Today", Modifier.padding(horizontal = 20.dp, vertical = 10.dp), color = cs.onSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            GuidePurchases(onRow = { rects["row"] = it }, onSwiped = { right -> light(if (right) Task.REFUND else Task.DELETE) }, swipeable = step == Step.ROWS)
        }

        // ---- the main bar, where it always is: Library · ( calendar  +  ⋮ ) · Settings
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            val s = search.value
            val away = pageP.value
            GuideEdge(Sym.LibraryBooks, Modifier.align(Alignment.CenterStart).graphicsLayer { alpha = (1f - s) })
            GuideEdge(Sym.Settings, Modifier.align(Alignment.CenterEnd).graphicsLayer { alpha = (1f - s) })
            Box(
                Modifier.onGloballyPositioned { rects["bar"] = it.boundsInRoot() }
                    // while a page's Pill (morphing out of it) stands in for it, the bar's own Pill hides
                    .graphicsLayer { alpha = if (away > 0.001f) 0f else 1f }
                    .pointerInput(step) {
                        detectTapGestures(onLongPress = {
                            if (step == Step.SEARCH) {
                                haptics?.longPress()
                                scope.launch { search.animateTo(1f, Motion.noBounce()); complete(); delay(800); search.animateTo(0f, Motion.exit(200)) }
                            }
                        })
                    }
                    .panGesture(
                        enabled = true,
                        onMove = { x, y -> step.page?.let { p -> val pull = pullOf(p.side, x, y, screenW); if (pull > 0f) scope.launch { pageP.snapTo(pull.coerceIn(0f, 1f)) } } },
                        onRelease = { x, y, vx, vy ->
                            val p = step.page ?: return@panGesture
                            val pull = pullOf(p.side, x, y, screenW)
                            val toward = when (p.side) { Side.LEFT -> vx; Side.RIGHT -> -vx; Side.UP -> -vy } * px * 1000f // px/s
                            val go = pull > 0.2f || (toward > 700f && pull > 0.03f)
                            if (go) pageOpened = true
                            scope.launch { pageP.animateTo(if (go) 1f else 0f, Motion.switcher) }
                        },
                    )
                    .shadow(6.dp, CircleShape).background(cs.surfaceContainerHigh, CircleShape)
                    .then(if (s > 0f) Modifier.fillMaxWidth(lerp(0.5f, 1f, s)) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                // its buttons explain themselves: the tour is for gestures
                if (s < 0.5f) Box(Modifier.graphicsLayer { alpha = 1f - s * 2f }) { MainBarPillLook(Sym.Add, Sym.CalendarMonth, Sym.MoreVert) }
                else Box(Modifier.fillMaxWidth().height(68.dp).padding(horizontal = 10.dp).graphicsLayer { alpha = (s - 0.5f) * 2f }, contentAlignment = Alignment.Center) {
                    GuideField("Search purchases")
                }
            }
        }

        // ---- the step's page, coming in from its side with the finger
        val page = step.page
        if (page != null && pageP.value > 0f) GuidePage(
            page, pageP.value, onPill = { rects["pagePill"] = it },
            pillSwipe = Modifier.panGesture(
                enabled = true,
                onMove = { x, y -> val back = pullOf(page.side, -x, -y, screenW); scope.launch { pageP.snapTo((1f - back).coerceIn(0f, 1f)) } },
                onRelease = { x, y, vx, vy ->
                    val back = pullOf(page.side, -x, -y, screenW)
                    val speed = when (page.side) { Side.LEFT -> -vx; Side.RIGHT -> vx; Side.UP -> vy } * px * 1000f
                    val goes = back > 0.25f || speed > 500f
                    scope.launch { pageP.animateTo(if (goes) 0f else 1f, Motion.switcher) }
                    if (goes) step.chips.firstOrNull { it.leaves }?.let(::light)
                },
            ),
        )

        // ---- one Pill, always: as a page comes in (or goes), its search Pill grows out of the bar's and back
        if (page != null && pageP.value > 0f && pageP.value < 0.999f) GuideMorph(from = rects["pagePill"], to = rects["bar"], m = 1f - pageP.value)

        // ---- dim everything but what to try
        val pageOpen = page != null && pageP.value > 0f
        val spot = when {
            stepDone || step == Step.DONE -> null
            step == Step.ROWS -> rects["row"]
            step == Step.MONTHS -> rects["month"]
            pageOpen -> null
            else -> rects["bar"]
        }
        val dim by animateFloatAsState(if (spot == null) 0f else 0.5f, label = "dim")
        Canvas(Modifier.fillMaxSize().zIndex(8f).graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
            drawRect(Color.Black.copy(alpha = dim))
            spot?.let { r ->
                val pad = 8.dp.toPx()
                drawRoundRect(Color.Transparent, Offset(r.left - pad, r.top - pad), Size(r.width + pad * 2, r.height + pad * 2),
                    CornerRadius(minOf(r.height / 2 + pad, 28.dp.toPx())), blendMode = BlendMode.Clear)
            }
        }

        // ---- the fingertip, acting out what to do next
        if (!stepDone && step != Step.DONE) {
            val hint: Pair<Rect?, Move> = when (step) {
                Step.SEARCH -> rects["bar"] to Move.HOLD
                Step.MONTHS -> rects["month"] to Move.RIGHT
                Step.ROWS -> rects["row"] to if (Task.REFUND in lit) Move.LEFT else Move.RIGHT
                else -> when {
                    page == null -> null to Move.HOLD
                    !pageOpen -> rects["bar"] to when (page.side) { Side.LEFT -> Move.RIGHT; Side.RIGHT -> Move.LEFT; Side.UP -> Move.UP }
                    else -> rects["pagePill"] to when (page.side) { Side.LEFT -> Move.LEFT; Side.RIGHT -> Move.RIGHT; Side.UP -> Move.DOWN }
                }
            }
            hint.first?.let { FingerHint(hint.second, it) }
        }

        // ---- the caption: at the top, out of the Pill's way; while a page is open, or the step is about the screen's
        // middle, it moves down to sit above the Pill
        val low = (pageOpen && pageP.value > 0.5f) || step == Step.ROWS || step == Step.MONTHS || step == Step.DONE
        val bias by animateFloatAsState(if (low) 1f else -1f, spring(0.85f, 300f), label = "caption")
        Box(Modifier.fillMaxSize().zIndex(9f).windowInsetsPadding(WindowInsets.systemBars).padding(top = 8.dp, bottom = 96.dp)) {
            Column(
                Modifier.align(BiasAlignment(0f, bias)).padding(horizontal = 16.dp).widthIn(max = COMFORTABLE_WIDTH.dp).fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp)).background(cs.surfaceContainerHigh).padding(20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        steps.forEach { s -> Box(Modifier.weight(1f).height(4.dp).clip(CircleShape).background(if (steps.indexOf(s) <= steps.indexOf(step)) cs.primary else cs.outlineVariant)) }
                    }
                    if (step != Step.DONE) TextButton(onClick = onDone, Modifier.padding(start = 8.dp)) { Text("Skip", color = cs.primary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
                }
                Text(step.title, Modifier.padding(top = 8.dp), color = cs.onSurface, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text(if (pageP.value >= 1f && step.inside.isNotEmpty()) step.inside else step.text, Modifier.padding(top = 4.dp),
                    color = cs.onSurfaceVariant, fontSize = 15.sp, lineHeight = 22.sp)
                // a page's checklist shows once it's open
                val shown = if (page == null) step.chips else if (!pageOpened) emptyList() else step.chips
                if (shown.isNotEmpty()) Column(Modifier.padding(top = 12.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    shown.forEach { Check(it.label, it in lit) }
                }
                if (step == Step.DONE) Box(Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
                    Button(onClick = onDone) { Text("Start Budgeting", fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
                } else Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    AnimatedVisibility(stepDone, enter = scaleIn(spring(0.5f, 500f), initialScale = 0.5f) + fadeIn(Motion.enter(150)), exit = fadeOut(Motion.exit(100))) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Sym.Check, null, Modifier.padding(end = 6.dp).size(20.dp), tint = cs.primary)
                            Text("Nice!", color = cs.primary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    // skipping a step leaves the copy where that step would have
                    TextButton(onClick = { scope.launch { pageP.snapTo(0f); advance() } }) {
                        Text("Next", color = cs.onSurfaceVariant, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/** How far (0…1) a drag of [x], [y] dp has pulled a page in from [side]: a screen width sideways, 220dp up. */
private fun pullOf(side: Side, x: Float, y: Float, screenW: Float) = when (side) { Side.LEFT -> x / screenW; Side.RIGHT -> -x / screenW; Side.UP -> -y / 220f }

/** An edge button of the main bar, as it looks: a 26dp symbol on the background in a 52dp spot. */
@Composable
private fun GuideEdge(icon: ImageVector, modifier: Modifier) {
    Box(modifier.size(52.dp), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(26.dp), tint = MaterialTheme.colorScheme.onSurface) }
}

/** The copy's purchases, as the start screen's table rows; the first one can be swiped like the real ones. */
@Composable
private fun GuidePurchases(onRow: (Rect) -> Unit, onSwiped: (right: Boolean) -> Unit, swipeable: Boolean) {
    val cs = MaterialTheme.colorScheme
    val rows = listOf(
        Triple("Corner Market", "$42.18", "Groceries" to Sym.ShoppingCart),
        Triple("Blue Bottle", "$6.50", "Coffee & Snacks" to Sym.LocalCafe),
        Triple("Noodle Bar", "$18.75", "Restaurants" to Sym.Restaurant),
    )
    rows.forEachIndexed { i, (name, amount, cat) ->
        val row: @Composable () -> Unit = {
            MiListRow(
                title = name, lead = AnnotatedString(amount), details = AnnotatedString(cat.first), gap = false,
                leading = { Box(Modifier.size(44.dp).clip(CircleShape).background(cs.surfaceContainerHigh), contentAlignment = Alignment.Center) { Icon(cat.second, null, Modifier.size(22.dp), tint = cs.onSurfaceVariant) } },
                modifier = Modifier.background(with(LocalMi.current) { cs.homeBackground }),
            )
        }
        if (i == 0) {
            // the app's swipe row: right refunds, left deletes (here they only spring back)
            val state = rememberSwipeToDismissBoxState(positionalThreshold = { it * com.jlees.mibudget.ui.common.SwipeRules.ARM_AT })
            LaunchedEffect(state.currentValue) {
                if (state.currentValue != SwipeToDismissBoxValue.Settled) { onSwiped(state.currentValue == SwipeToDismissBoxValue.StartToEnd); state.reset() }
            }
            SwipeToDismissBox(
                state = state, gesturesEnabled = swipeable, modifier = Modifier.onGloballyPositioned { onRow(it.boundsInRoot()) },
                backgroundContent = {
                    val dir = state.dismissDirection
                    if (dir == SwipeToDismissBoxValue.Settled) return@SwipeToDismissBox
                    val refund = dir == SwipeToDismissBoxValue.StartToEnd
                    Row(
                        Modifier.fillMaxSize().listReveal().background(if (refund) cs.primaryContainer else cs.errorContainer).padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = if (refund) Arrangement.Start else Arrangement.End,
                    ) { Icon(if (refund) Sym.Undo else Sym.Delete, null, tint = if (refund) cs.onPrimaryContainer else cs.onErrorContainer) }
                },
            ) { row() }
        } else row()
    }
}

/** A copy of the Library, Settings or the Calendar, [p] of the way in from its side, with its search Pill at the bottom. */
@Composable
private fun GuidePage(page: Page, p: Float, onPill: (Rect) -> Unit, pillSwipe: Modifier) {
    val cs = MaterialTheme.colorScheme
    val mi = LocalMi.current
    Box(
        Modifier.fillMaxSize().zIndex(6f)
            .graphicsLayer {
                when (page.side) {
                    Side.LEFT -> translationX = -size.width * (1f - p)
                    Side.RIGHT -> translationX = size.width * (1f - p)
                    Side.UP -> translationY = size.height * (1f - p)
                }
                shadowElevation = 8.dp.toPx()
            }
            .background(cs.surfaceContainerLow).clickable(MutableInteractionSource(), null) {}.windowInsetsPadding(WindowInsets.systemBars),
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(page.title, Modifier.weight(1f), color = cs.onSurface, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                Box(Modifier.size(48.dp).clip(CircleShape).background(cs.secondaryContainer), contentAlignment = Alignment.Center) { Icon(Sym.Close, null, tint = cs.onSecondaryContainer) }
            }
            when (page) {
                Page.LIBRARY -> Column(Modifier.padding(top = 16.dp, end = 12.dp)) {
                    listOf(Triple("Music Service", "$10.99/mo", "renews in 3 days"), Triple("Cloud Storage", "$2.99/mo", "renews in 9 days"), Triple("Streaming", "$15.49/mo", "renews in 12 days"))
                        .forEach { (name, price, due) ->
                            MiListRow(
                                title = name, lead = AnnotatedString(due), details = AnnotatedString(""), gap = false,
                                leading = { Box(Modifier.size(44.dp).clip(CircleShape).background(cs.surfaceContainerHigh), contentAlignment = Alignment.Center) { Icon(Sym.Autorenew, null, Modifier.size(22.dp), tint = cs.onSurfaceVariant) } },
                                trailing = { Text(price, color = cs.onSurface, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold) },
                            )
                        }
                }
                Page.SETTINGS -> Column(Modifier.padding(top = 16.dp, end = 12.dp)) {
                    // Settings' own rows, in their rounded block
                    listOf(Triple(Sym.Savings, "Budgeting", "How budgets and weeks work"), Triple(Sym.Palette, "Look & Feel", "Colors, UI and haptics"), Triple(Sym.Vibration, "Gestures", "How gestures work throughout mi. Budget"))
                        .forEachIndexed { i, (icon, title, sub) ->
                            Box(Modifier.segment(i, 3)) { Item(icon, title, sub, trailing = { Icon(Sym.ChevronRight, null, Modifier.size(22.dp), tint = cs.onSurfaceVariant) }) }
                        }
                }
                Page.CALENDAR -> Column(Modifier.padding(top = 16.dp, end = 12.dp)) {
                    Text("November", color = cs.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                    // days shaded by spending, as the real calendar's
                    val shade = listOf(0, 2, 0, 1, 3, 0, 1, 2, 0, 0, 1, 3, 2, 0, 1, 0, 2, 1, 0, 3, 1, 0, 0, 2, 1, 0, 1, 2)
                    shade.chunked(7).forEachIndexed { w, week ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                            week.forEachIndexed { d, level ->
                                Box(
                                    Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(12.dp))
                                        .background(if (level == 0) Color.Transparent else cs.primary.copy(alpha = 0.15f + 0.2f * level)),
                                    contentAlignment = Alignment.TopStart,
                                ) { Text("${w * 7 + d + 1}", Modifier.padding(6.dp), color = cs.onSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                            }
                        }
                    }
                }
            }
        }
        // its search Pill, where the real one sits
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 10.dp).height(70.dp)
                .onGloballyPositioned { onPill(it.boundsInRoot()) }.then(pillSwipe)
                .graphicsLayer { alpha = if (p < 0.999f) 0f else 1f } // the morphing Pill stands in while it moves
                .shadow(10.dp, RoundedCornerShape(35.dp)).clip(RoundedCornerShape(35.dp)).background(with(mi) { cs.pillBar }).padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center,
        ) { GuideField("Search") }
    }
}

/**
 * The Pill between the bar's and a page's, [m] of the way from the page's ([from], root px) to the bar's ([to]): its
 * place, size, colour and shadow travel, its search field fading out over the first half (keeping its width, centred)
 * and the bar's buttons fading in over the second (at their own size, centred), as the app's sheets do.
 */
@Composable
private fun GuideMorph(from: Rect?, to: Rect?, m: Float) {
    if (from == null || to == null) return
    val cs = MaterialTheme.colorScheme
    val mi = LocalMi.current
    val density = LocalDensity.current
    val r = androidx.compose.ui.geometry.lerp(from, to, m)
    with(density) {
        Box(
            Modifier.zIndex(7f).offset { IntOffset(r.left.toInt(), r.top.toInt()) }.size(r.width.toDp(), r.height.toDp())
                .shadow(lerp(10f, 6f, m).dp, CircleShape).clip(CircleShape)
                .background(androidx.compose.ui.graphics.lerp(with(mi) { cs.pillBar }, cs.surfaceContainerHigh, m)),
            contentAlignment = Alignment.Center,
        ) {
            if (m < 0.5f) Box(Modifier.requiredWidth((from.width - 20 * density.density).toDp()).graphicsLayer { alpha = 1f - m * 2f }) { GuideField("Search") }
            else Box(Modifier.wrapContentSize(unbounded = true).graphicsLayer { alpha = (m - 0.5f) * 2f }) { MainBarPillLook(Sym.Add, Sym.CalendarMonth, Sym.MoreVert) }
        }
    }
}

/** The copies' search field: the Pill's lighter field with the search symbol and [text]. */
@Composable
private fun GuideField(text: String, modifier: Modifier = Modifier.fillMaxWidth()) {
    val cs = MaterialTheme.colorScheme
    val mi = LocalMi.current
    Row(
        modifier.height(49.dp).background(with(mi) { cs.pillField }, RoundedCornerShape(minOf(mi.radius * 1.5f, 24.5f).dp)).padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Sym.Search, null, Modifier.padding(end = 12.dp).size(24.dp), tint = cs.onSurfaceVariant)
        Text(text, color = cs.onSurfaceVariant, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/**
 * A translucent fingertip acting out [move] over [at], on a loop: a 30dp white circle at 60% with a 2dp primary ring
 * (1400ms a phase, linear). It holds still when Android's "Remove animations" is on.
 */
@Composable
private fun FingerHint(move: Move, at: Rect) {
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current
    val moving = remember { android.provider.Settings.Global.getFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f }
    val t by if (moving) rememberInfiniteTransition(label = "hint").animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "t")
    else remember { mutableStateOf(0.5f) }
    val density = LocalDensity.current
    val (x, y) = when (move) { Move.HOLD -> 0f to 0f; Move.UP -> 0f to -110f * t; Move.DOWN -> 0f to 110f * t; Move.RIGHT -> 90f * t to 0f; Move.LEFT -> -90f * t to 0f }
    val size = 30.dp
    Box(
        Modifier.zIndex(10f).graphicsLayer {
            val half = size.toPx() / 2
            translationX = at.center.x - half + x * density.density
            translationY = at.center.y - half + y * density.density
            val s = if (move == Move.HOLD) 1f - 0.2f * (if (t < 0.7f) t / 0.7f else 1f) else 1f
            scaleX = s; scaleY = s
            alpha = if (move == Move.HOLD) 0.7f else 0.8f * sin(PI * t).toFloat()
        }.size(size).clip(CircleShape).background(Color.White.copy(alpha = 0.6f)).border(2.dp, cs.primary, CircleShape),
    )
}

/** One line of a step's checklist: an empty circle, then a check that pops once it's done. */
@Composable
private fun Check(label: String, on: Boolean) {
    val cs = MaterialTheme.colorScheme
    val circle by animateColorAsState(if (on) cs.primary else Color.Transparent, label = "check")
    val pop = popScale(on)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(22.dp).graphicsLayer { scaleX = pop; scaleY = pop }.clip(CircleShape).background(circle).border(2.dp, if (on) cs.primary else cs.outline, CircleShape),
            contentAlignment = Alignment.Center,
        ) { if (on) Icon(Sym.Check, null, Modifier.size(16.dp), tint = cs.onPrimary) }
        Text(label, Modifier.padding(start = 10.dp), color = if (on) cs.onSurface else cs.onSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
