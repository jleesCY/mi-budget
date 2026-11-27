// The pieces Settings is built from (after mi. Browser's ui/settings/SettingsKit.kt): sections of rows, every row in the
// one row layout, rows grouped in segmented cards under primary-coloured headings, and a section's bar (← icon Title).
package com.jlees.mibudget.ui.settings

import androidx.compose.runtime.setValue

import com.jlees.mibudget.ui.common.Sym

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jlees.mibudget.ui.common.ConnectedChoice
import com.jlees.mibudget.ui.common.MiSwitch
import com.jlees.mibudget.ui.common.Motion
import com.jlees.mibudget.ui.theme.LocalMi

/** A setting row: [label] and [keywords] are what search matches; [content] draws it. [header]: a heading inside a section. */
class SettingsRow(val label: String, val keywords: String = "", val header: Boolean = false, val content: @Composable () -> Unit)

/** A heading inside a section (Look & Feel › Colors): the rows after it, to the next heading, are its block. */
fun SettingsHeading(label: String) = SettingsRow(label, header = true) {}

/** A section: its own page from the main list ([summary] says what's in it). */
class SettingsSection(val id: String, val title: String, val icon: ImageVector, val summary: String, val rows: List<SettingsRow>)

/** A row with the heading it sits under (null before any) and its place in its block. */
class PlacedRow(val row: SettingsRow, val heading: String?, val index: Int, val count: Int)

/** [rows] split into their headings' blocks; heading rows come through with index 0 / count 0. */
fun placeRows(rows: List<SettingsRow>): List<PlacedRow> {
    val out = ArrayList<PlacedRow>(rows.size)
    var heading: String? = null
    var i = 0
    while (i < rows.size) {
        if (rows[i].header) { heading = rows[i].label; out += PlacedRow(rows[i], heading, 0, 0); i++; continue }
        val end = (i until rows.size).firstOrNull { rows[it].header } ?: rows.size
        for (j in i until end) out += PlacedRow(rows[j], heading, j - i, end - i)
        i = end
    }
    return out
}

/** A setting found by search: its section, the heading it sits under, and its row. */
class SettingsHit(val section: SettingsSection, val heading: String?, val row: SettingsRow)

/**
 * Every row matching [query] (case ignored): its label, keywords or heading, or every row of a section whose title
 * matches. Grouped by section, in the order they're listed. Pure, so it's unit-tested.
 */
fun searchSettings(sections: List<SettingsSection>, query: String): List<Pair<SettingsSection, List<SettingsHit>>> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return emptyList()
    return sections.mapNotNull { g ->
        val placed = placeRows(g.rows).filter { !it.row.header }
        val hits = if (g.title.lowercase().contains(q)) placed
        else placed.filter { it.row.label.lowercase().contains(q) || it.row.keywords.lowercase().contains(q) || it.heading?.lowercase()?.contains(q) == true }
        hits.takeIf { it.isNotEmpty() }?.let { g to it.map { p -> SettingsHit(g, p.heading, p.row) } }
    }
}

/** Row [i] of [count] in a segmented card: a 2dp gap below, big outer corners (the Corners setting), small inner ones. */
@Composable
fun Modifier.segment(i: Int, count: Int): Modifier {
    val big = LocalMi.current.radius.coerceAtLeast(4).dp
    val small = 4.dp
    val shape = RoundedCornerShape(
        topStart = if (i == 0) big else small, topEnd = if (i == 0) big else small,
        bottomStart = if (i == count - 1) big else small, bottomEnd = if (i == count - 1) big else small,
    )
    return fillMaxWidth().padding(bottom = if (i < count - 1) 2.dp else 0.dp).clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHighest)
}

/** A heading over a block of rows (or a section's title over its search results). */
@Composable
fun SectionHeader(icon: ImageVector?, title: String) {
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(start = 8.dp, top = 20.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) Icon(icon, null, Modifier.padding(end = 8.dp).size(18.dp), tint = cs.primary)
        Text(title, color = cs.primary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** A section's own page: back arrow, its icon and title (instead of the search). */
@Composable
fun SectionBar(icon: ImageVector, title: String, onBack: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 2.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Sym.ArrowBack, "Back", tint = cs.onSurface) }
        Icon(icon, null, Modifier.padding(start = 4.dp, end = 10.dp).size(22.dp), tint = cs.primary)
        Text(title, color = cs.onSurface, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * The one row layout every setting uses: its icon in a tonal circle, title, optional explanation, optional trailing
 * control, and optional content underneath (choices, swatches). [onClick] makes the whole row the target.
 */
@Composable
fun Item(
    icon: ImageVector, title: String, subtitle: String? = null, danger: Boolean = false,
    onClick: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null, below: (@Composable () -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) leading()
            else Box(
                Modifier.size(40.dp).clip(CircleShape).background(if (danger) cs.errorContainer else cs.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, Modifier.size(22.dp), tint = if (danger) cs.onErrorContainer else cs.onSecondaryContainer) }
            Column(Modifier.weight(1f).padding(start = 16.dp, end = 8.dp)) {
                com.jlees.mibudget.ui.common.OneLine(title, color = if (danger) cs.error else cs.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                if (subtitle != null) Text(subtitle, Modifier.padding(top = 2.dp), color = cs.onSurfaceVariant, fontSize = 13.sp, lineHeight = 18.sp)
            }
            trailing?.invoke()
        }
        if (below != null) Box(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 14.dp)) { below() }
    }
}

@Composable
fun SwitchItem(icon: ImageVector, title: String, subtitle: String?, value: Boolean, onChange: (Boolean) -> Unit) =
    Item(icon, title, subtitle, onClick = { onChange(!value) }, trailing = { MiSwitch(value, null) })

/** A row that opens something: the whole row is the target, with › at its end. */
@Composable
fun LinkItem(icon: ImageVector, title: String, subtitle: String? = null, onClick: () -> Unit) =
    Item(icon, title, subtitle, onClick = onClick, trailing = {
        Icon(Sym.KeyboardArrowRight, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    })

/** A multi-way choice: the row, with a connected button group under it. */
@Composable
fun <T> ChoiceItem(icon: ImageVector, title: String, subtitle: String?, options: List<Pair<T, String>>, value: T, onPick: (T) -> Unit) =
    Item(icon, title, subtitle, below = { ConnectedChoice(options, value, onPick = onPick) })

/** A row of round swatches, 48dp; the chosen one shrinks a little into a rounded square. [icon]: drawn in a swatch (Adaptive). */
@Composable
fun SwatchRow(swatches: List<Triple<Color, String, ImageVector?>>, selected: Int, onPick: (Int) -> Unit) {
    LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(swatches.size) { i ->
            val (color, name, icon) = swatches[i]
            val on = i == selected
            val scale by animateFloatAsState(if (on) 0.86f else 1f, Motion.fastSpatial(), label = "swatch")
            Box(
                Modifier.size(48.dp).graphicsLayer { scaleX = scale; scaleY = scale }
                    .clip(if (on) RoundedCornerShape(30) else CircleShape).background(color)
                    .clickable { onPick(i) }
                    .semantics { contentDescription = name; role = Role.RadioButton; this.selected = on },
                contentAlignment = Alignment.Center,
            ) {
                // Adaptive shows its wallpaper symbol (onTertiaryContainer); a chosen colour, a white check
                if (icon != null) Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                else if (on) Icon(Sym.Check, null, Modifier.size(22.dp), tint = Color.White)
            }
        }
    }
}


/**
 * A stepped slider (0.1 steps within [min]..[max]) under the row, the value as a percentage at its end. It follows the
 * finger itself and saves ([onChange]) when the finger lets go: saving mid-drag can change what's under the finger
 * (Font Size changes the screen's density, which restarts every touch handler), and the drag would be lost to the
 * page's scrolling.
 */
@Composable
fun SliderItem(icon: ImageVector, title: String, subtitle: String?, value: Float, min: Float, max: Float, onChange: (Float) -> Unit) {
    var dragging by androidx.compose.runtime.remember(value) { androidx.compose.runtime.mutableStateOf<Float?>(null) }
    val shown = dragging ?: value
    Item(icon, title, subtitle,
        trailing = { Text("${Math.round(shown * 100)}%", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) },
        below = {
            androidx.compose.material3.Slider(
                value = shown, onValueChange = { dragging = Math.round(it * 10f) / 10f },
                onValueChangeFinished = { dragging?.let { v -> if (v != value) onChange(v) } },
                valueRange = min..max, steps = Math.round((max - min) / 0.1f) - 1,
            )
        })
}

/**
 * A slider through whole steps ([range]) under the row, as Font Size's, with the step's [label] at the row's end. Like
 * [SliderItem] it follows the finger itself and saves ([onChange], then [onDone]) when the finger lets go.
 */
@Composable
fun StepSliderItem(
    icon: ImageVector, title: String, subtitle: String?, value: Int, range: IntRange,
    label: (Int) -> String, onChange: (Int) -> Unit, onDone: () -> Unit = {},
) {
    var dragging by androidx.compose.runtime.remember(value) { androidx.compose.runtime.mutableStateOf<Int?>(null) }
    val shown = dragging ?: value
    Item(icon, title, subtitle,
        trailing = { Text(label(shown), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) },
        below = {
            androidx.compose.material3.Slider(
                value = shown.toFloat(), onValueChange = { dragging = Math.round(it) },
                onValueChangeFinished = { dragging?.let { v -> if (v != value) { onChange(v); onDone() } } },
                valueRange = range.first.toFloat()..range.last.toFloat(), steps = range.last - range.first - 1,
            )
        })
}

/** A row that opens its options under itself, with its current [value] and a turning chevron. */
@Composable
fun ExpandItem(icon: ImageVector, title: String, subtitle: String?, value: String?, options: @Composable (close: () -> Unit) -> Unit) {
    var open by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().animateContentSize()) {
        Item(icon, title, subtitle, onClick = { open = !open }, trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (value != null) Text(value, Modifier.padding(end = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                com.jlees.mibudget.ui.common.Chevron(open, 22f, MaterialTheme.colorScheme.onSurfaceVariant)
            }
        })
        if (open) Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) { options { open = false } }
    }
}

/** A credit or info line under a row, lined up with the titles: what (14sp semibold), who (13sp). */
@Composable
fun CreditLine(what: String, who: String) {
    Column(Modifier.fillMaxWidth().padding(start = 72.dp, end = 20.dp, top = 6.dp, bottom = 6.dp)) {
        Text(what, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(who, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

/**
 * A bar (a section's ← icon Title) that the list scrolls under: the bar sits on the sheet's colour with a 20dp fade
 * below it, so rows slide behind it and fade out instead of being cut off. [content] gets the top padding that keeps
 * its first row clear of the bar at rest.
 */
@Composable
fun TopBarOver(bar: @Composable () -> Unit, content: @Composable (top: androidx.compose.ui.unit.Dp) -> Unit) {
    val bg = MaterialTheme.colorScheme.surfaceContainerLow
    val density = androidx.compose.ui.platform.LocalDensity.current
    var barPx by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    Box(Modifier.fillMaxWidth()) {
        content(with(density) { barPx.toDp() } + 20.dp)
        Column(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().background(bg).onSizeChanged { barPx = it.height }) { bar() }
            Box(Modifier.fillMaxWidth().height(20.dp).background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(bg, bg.copy(alpha = 0f)))))
        }
    }
}
