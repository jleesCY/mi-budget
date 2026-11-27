// mi.'s full pages ("sheets"): a big title at the top left with a round close button, connected chips under it when
// the sheet has pages, and the page's Pill at the bottom (swipe it right to go back). Every screen other than the
// start screen is one of these.
package com.jlees.mibudget.ui.common

import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.roundToInt
import androidx.compose.ui.layout.layout
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Closes the sheet a page is shown in (its round ✕), when it's in one. Provided by the navigation. */
val LocalSheetClose = compositionLocalOf<(() -> Unit)?> { null }

/**
 * A sheet with pages (the Library) shows one search Pill for all of them: each page reads what's typed here (and
 * leaves its own Pill out). Null outside such a sheet.
 */
val LocalSheetQuery = compositionLocalOf<String?> { null }

/**
 * A sheet page's add buttons, where adding is the main thing (never on the start screen, where the main bar's + is):
 * at the bottom right, 20 from the edge, just above the sheet's Pill: [main] a FloatingActionButton in primaryContainer,
 * [secondary] a small one in secondaryContainer 16 to its left. They hide while typing.
 */
/** A page's add buttons. [vivid]: the main one wears the accent at full strength (only the Subscriptions page's). */
class SheetFabs(val main: BarButton, val secondary: BarButton? = null, val vivid: Boolean = false)

/** The connected chips under the title, when the page is one of a sheet's pages (the Library's). */
val LocalSheetChips = compositionLocalOf<(@Composable () -> Unit)?> { null }

/** What a sheet's Pill shows and does. */
class PillSpec(
    val text: String,
    val onTap: () -> Unit,
    val leading: PillAction? = null,
    val trailing: List<PillAction> = emptyList(),
    val onLongPress: (() -> Unit)? = null,
    /** Set: the Pill is a search field (see MiPill). */
    val query: String? = null,
    val onQuery: ((String) -> Unit)? = null,
    /** Past searches shown above the field while it's empty. */
    val suggestions: List<String> = emptyList(),
    val onForgetSuggestion: (String) -> Unit = {},
)

/** The Pill a [PillSpec] describes, plus the gestures that depend on where it is. */
@Composable
fun BoxScope.MiPill(
    spec: PillSpec, onSwipeSide: ((Int) -> Unit)? = null, onSwipeUp: (() -> Unit)? = null,
    focusOnShow: Boolean = false,
    morph: SheetMorph? = null,
) = MiPill(
    text = spec.text, onTap = spec.onTap, leading = spec.leading, trailing = spec.trailing, onLongPress = spec.onLongPress,
    query = spec.query, onQuery = spec.onQuery, onSwipeSide = onSwipeSide, onSwipeUp = onSwipeUp,
    focusOnShow = focusOnShow,
    suggestions = spec.suggestions, onForgetSuggestion = spec.onForgetSuggestion, morph = morph,
)

/**
 * A mi. sheet: header (title, [actions], ✕ or ←), chips when it's one of a sheet's pages, [content], and the page's
 * [pill] (swipe it right = Back). [onBack] shows an arrow before the title (a level inside a sheet); otherwise the ✕
 * closes the sheet ([onClose]).
 */
@Composable
fun MiScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    onClose: (() -> Unit)? = LocalSheetClose.current,
    actions: @Composable RowScope.() -> Unit = {},
    pill: PillSpec? = null,
    fabs: SheetFabs? = null,
    bottomBar: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    val back = onBack ?: onClose
    Box(modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = bottomBar,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            topBar = { SheetHeader(title, onBack, if (onBack == null) onClose else null, actions) },
        ) { padding ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.widthIn(max = COMFORTABLE_WIDTH.dp).fillMaxSize()) {
                    // lists end clear of the Pill, and of the add buttons above it (56 + 16), so the last row can scroll
                    // out from under them
                    val fabRoom = if (fabs != null) 72.dp else 0.dp
                    content(
                        if (pill == null && LocalSheetQuery.current == null) PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + fabRoom)
                        else PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + PillSpace + fabRoom)
                    )
                }
            }
        }
        if (fabs != null) FabColumn(fabs)
        // a sheet opened from the main bar: its Pill morphs with it, and swiping it sends the sheet back under the finger
        // (a level inside goes back on a swipe right first)
        val morph = LocalSheetMorph.current
        // in a sheet with one search Pill for all its pages, the sheet draws it
        if (pill != null && LocalSheetQuery.current == null) MiPill(
            pill, morph = morph,
            onSwipeSide = when { onBack != null -> { dir -> if (dir == 1) onBack() }; morph != null -> null; else -> back?.let { b -> { dir -> if (dir == 1) b() } } },
        )
    }
}

/**
 * A scrolling page's [padding] from [MiScaffold]: the header's room above it (nothing scrolls up behind the header and
 * the status bar), and the bottom's room (the navigation bar, the Pill) inside the scroll, so the page scrolls on
 * behind them, as they're see-through, instead of stopping above them.
 */
fun Modifier.scrollingPage(padding: PaddingValues, scroll: androidx.compose.foundation.ScrollState): Modifier =
    padding(top = padding.calculateTopPadding()).consumeWindowInsets(padding).imePadding().verticalScroll(scroll)
        .padding(bottom = padding.calculateBottomPadding())

/** The big title (about 30sp, bold), with ← before it or a round tonal ✕ after it, and the chips under it. */
@Composable
fun SheetHeader(title: String, onBack: (() -> Unit)?, onClose: (() -> Unit)?, actions: @Composable RowScope.() -> Unit = {}) {
    val cs = MaterialTheme.colorScheme
    // below the status bar, or with it hidden, below the camera cutout
    Column(Modifier.fillMaxWidth().background(cs.surfaceContainerLow).windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout).only(androidx.compose.foundation.layout.WindowInsetsSides.Top)), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.widthIn(max = COMFORTABLE_WIDTH.dp).fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(start = if (onBack != null) 8.dp else 24.dp, end = 12.dp, top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) IconButton(onClick = onBack) { Icon(Sym.ArrowBack, "Back") }
                Text(
                    title, Modifier.weight(1f).padding(start = if (onBack != null) 4.dp else 0.dp).semantics { heading() },
                    fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = cs.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                actions()
                if (onClose != null) FilledTonalIconButton(onClick = onClose) { Icon(Sym.Close, "Close") }
            }
            LocalSheetChips.current?.let { chips -> Box(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 4.dp)) { chips() } }
        }
    }
}

/** A connected group's button [i] of [last]+1: rounded on the outside ends only. */
@Composable
private fun connectedShapes(i: Int, last: Int) = when (i) {
    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
    last -> ButtonGroupDefaults.connectedTrailingButtonShapes()
    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
}

/** Expressive connected button group, one choice ([options]: value to label). */
@Composable
fun <T> ConnectedChoice(options: List<Pair<T, String>>, value: T, modifier: Modifier = Modifier, onPick: (T) -> Unit) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
        options.forEachIndexed { i, (v, label) ->
            ToggleButton(
                checked = v == value, onCheckedChange = { onPick(v) },
                modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                shapes = connectedShapes(i, options.lastIndex),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            ) { OneLine(label, style = MaterialTheme.typography.labelLarge) }
        }
    }
}

/**
 * Connected icon buttons (one choice) where only the chosen one shows its name and widens, so many pages fit one row
 * (the Library's pages).
 */
@Composable
fun <T> ConnectedIconChoice(
    options: List<Triple<T, String, ImageVector>>, value: T, modifier: Modifier = Modifier,
    /**
     * How chosen each one is (0 … 1), when something that outlives this row animates it: the Library's pages each draw
     * their own header, and while one page crossfades into the next both rows show the same choice widening.
     */
    chosen: ((Int) -> Float)? = null,
    onPick: (T) -> Unit,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
        options.forEachIndexed { i, (v, label, icon) ->
            val on = v == value
            // growing and shrinking at the same rate, so the row's width stays put while the choice changes (no bounce)
            val own by animateFloatAsState(if (on) 1f else 0f, Motion.enter(220), label = "choice")
            val p = (chosen?.invoke(i) ?: own).coerceIn(0f, 1f)
            ToggleButton(
                checked = on, onCheckedChange = { onPick(v) },
                // the chosen one widens to fit its name, as the browser's Library
                modifier = Modifier.weight(1f + 2f * p).semantics { role = Role.RadioButton },
                shapes = connectedShapes(i, options.lastIndex),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            ) {
                Icon(icon, if (on) null else label, Modifier.size(20.dp), tint = LocalContentColor.current)
                // its name grows out beside the icon (and fades in) as it's chosen
                if (p > 0.01f) Text(
                    label,
                    Modifier.clipToBounds().layout { m, c ->
                        val pl = m.measure(c.copy(minWidth = 0))
                        layout((pl.width * p).roundToInt(), pl.height) { pl.place(0, 0) }
                    }.graphicsLayer { alpha = p }.padding(start = 8.dp),
                    maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

/** The M3 switch with mi.'s check in its thumb. */
@Composable
fun MiSwitch(checked: Boolean, onChange: ((Boolean) -> Unit)?) {
    Switch(
        checked = checked, onCheckedChange = onChange,
        thumbContent = if (checked) { { Icon(Sym.Check, null, Modifier.size(16.dp)) } } else null,
    )
}

/** An empty list: a 50dp icon and one Title Case line under it ("No Purchases Yet"). No subtext. */
@Composable
fun EmptyLine(icon: ImageVector, text: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    val cs = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth().padding(top = 50.dp, bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, Modifier.size(50.dp), tint = cs.onSurfaceVariant)
        Text(text, Modifier.padding(top = 10.dp), color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        if (action != null) Box(Modifier.padding(top = 12.dp)) { action() }
    }
}


/** The sheet's add buttons, bottom right just above its Pill (hidden while the keyboard is up). */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.FabColumn(f: SheetFabs) {
    val cs = MaterialTheme.colorScheme
    if (WindowInsets.isImeVisible) return
    Row(
        Modifier.align(Alignment.BottomEnd).windowInsetsPadding(WindowInsets.navigationBars).padding(end = 20.dp, bottom = PillSpace),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        f.secondary?.let { s ->
            androidx.compose.material3.SmallFloatingActionButton(onClick = s.onClick, containerColor = cs.secondaryContainer, contentColor = cs.onSecondaryContainer) {
                Icon(s.icon, s.label, Modifier.size(24.dp))
            }
            androidx.compose.foundation.layout.Spacer(Modifier.size(16.dp))
        }
        // the standard add button in primaryContainer; the Subscriptions page's wears the accent at full strength, as the
        // main bar's + does (MiUi.accent)
        val mi = com.jlees.mibudget.ui.theme.LocalMi.current
        androidx.compose.material3.FloatingActionButton(
            onClick = f.main.onClick,
            containerColor = if (f.vivid) mi.accent else cs.primaryContainer,
            contentColor = if (f.vivid) mi.onAccent else cs.onPrimaryContainer,
        ) {
            Icon(f.main.icon, f.main.label, Modifier.size(28.dp))
        }
    }
}
