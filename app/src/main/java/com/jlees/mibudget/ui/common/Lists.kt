// mi. lists (purchases, subscriptions, renewals, payment methods, budgets, Recently Deleted): rows follow Look & Feel ›
// List Spacing (height and gap) and List Style (Table: edge to edge with a thin line under each row; Cards: each row its
// own rounded card, 20dp in from the edges). Every list of things uses MiListRow, so they all change together.
package com.jlees.mibudget.ui.common

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jlees.mibudget.data.ListStyle
import com.jlees.mibudget.data.Spacing
import com.jlees.mibudget.ui.theme.LocalMi

/** A list row's height for a spacing (dp), times the Font Size setting. Pure (unit-tested). */
fun rowHeight(padding: Spacing, fontScale: Float): Float = fontScale * when (padding) { Spacing.COMPACT -> 48f; Spacing.NORMAL -> 58f; Spacing.AIRY -> 72f }

/** The gap between cards for a spacing (dp). In a table rows meet at their lines. */
fun rowMargin(padding: Spacing): Float = when (padding) { Spacing.COMPACT -> 8f; Spacing.NORMAL -> 15f; Spacing.AIRY -> 25f }

/**
 * The row's look: its own rounded card (Cards), or flat with a 1px outlineVariant line at 60% under it (Table).
 * [highlight] fills the row instead (a selected row): the card, or the whole table row.
 */
@Composable
fun Modifier.listRow(highlight: Color? = null): Modifier {
    val mi = LocalMi.current
    val cs = MaterialTheme.colorScheme
    if (mi.listStyle == ListStyle.CARDS) return padding(horizontal = 20.dp).clip(MaterialTheme.shapes.large).background(highlight ?: cs.surfaceContainerHighest)
    val line = cs.outlineVariant.copy(alpha = 0.6f)
    // edge to edge, the line too; the row's content comes in a little more, in line with the headers (listInset)
    return (if (highlight != null) background(highlight) else this)
        .drawBehind { drawLine(line, Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), 1f) }
        .padding(horizontal = 5.dp)
}

/** What's revealed behind a swiped row takes the row's shape: the card's (Cards), or the whole row (Table). */
@Composable
fun Modifier.listReveal(): Modifier =
    if (LocalMi.current.listStyle == ListStyle.CARDS) padding(horizontal = 20.dp).clip(MaterialTheme.shapes.large) else this

/** The space between rows: the spacing's gap with Cards, none in a table. */
@Composable
fun listGap(): Dp = LocalMi.current.let { if (it.listStyle == ListStyle.CARDS) rowMargin(it.padding).dp else 0.dp }

/** The space either side of a list: the cards sit 20 in from the edges; a table runs edge to edge. */
@Composable
fun listSides(): Dp = if (LocalMi.current.listStyle == ListStyle.CARDS) 20.dp else 0.dp

/** Headers and notes in a list keep the 20dp margin either way (rows bring their own). */
fun Modifier.listInset(): Modifier = padding(horizontal = 20.dp)

/** The rows' minimum height. */
@Composable
fun listRowHeight(): Dp = LocalMi.current.let { rowHeight(it.padding, it.fontScale).dp }

/**
 * Widens a row [by] on each side, past its container's padding: a list inside a page that pads its content 20dp
 * (the Library's grids) still runs edge to edge as a table, and its cards still sit 20 in.
 */
fun Modifier.bleed(by: Dp = 20.dp): Modifier = layout { measurable, c ->
    val extra = by.roundToPx()
    val w = if (c.hasBoundedWidth) c.maxWidth + 2 * extra else c.maxWidth
    val p = measurable.measure(c.copy(minWidth = w, maxWidth = w))
    layout(c.maxWidth, p.height) { p.place(-extra, 0) }
}

/**
 * One row of a mi. list, as mi. Browser's (its History rows): List Spacing's height (more only if the content needs it),
 * 15 in from the row's sides, the 44dp leading tile, 15, then the title (16sp bold, one line) over the details line
 * (12sp): [lead] (an amount, a date, a state: semibold, primary unless it styles itself), 6, then [details] in
 * onSurfaceVariant. An optional value sits at the end. It follows List Style, the gap under it included. [marks] are
 * small symbols before the details (a receipt, a subscription). [dim] fades a row that's paused or done with.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MiListRow(
    title: String,
    lead: AnnotatedString?,
    details: AnnotatedString,
    leading: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    selected: Boolean = false,
    dim: Boolean = false,
    /** The gap under the row (Cards). Off when something wrapping it (a swipe) keeps the gap outside itself. */
    gap: Boolean = true,
    marks: (@Composable RowScope.() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth().padding(bottom = if (gap) listGap() else 0.dp)
            .listRow(if (selected) cs.secondaryContainer else null)
            .then(if (onClick != null || onLongClick != null) Modifier.combinedClickable(
                onClick = onClick ?: {}, onClickLabel = onClickLabel, onLongClick = onLongClick, onLongClickLabel = onLongClickLabel,
            ) else Modifier)
            .heightIn(min = listRowHeight())
            .graphicsLayer { alpha = if (dim) 0.6f else 1f }
            .padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(Modifier.width(15.dp))
        Column(Modifier.weight(1f)) {
            OneLine(title, color = cs.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                marks?.invoke(this)
                if (lead != null) Text(lead, Modifier.padding(end = 6.dp), color = cs.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(details, Modifier.weight(1f, fill = false), color = cs.onSurfaceVariant, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (trailing != null) { Spacer(Modifier.width(8.dp)); trailing() }
    }
}

/**
 * How many of a long list's rows to hand to its LazyColumn, as mi. Browser loads its History: [chunk] (Settings › Rows
 * Loaded at Once, 25 to begin with) at first, then
 * [chunk] more each time the list is scrolled to within 3 rows of its end; back to [chunk] when [reset] changes (a new
 * search, tab or month). Opening a page then builds only its first rows, and the rest come as they're needed.
 */
@Composable
fun rememberChunkedCount(list: androidx.compose.foundation.lazy.LazyListState, total: Int, reset: Any? = null): Int {
    val chunk = LocalSettings.current.listChunk
    var count by androidx.compose.runtime.remember(reset, chunk) { androidx.compose.runtime.mutableIntStateOf(chunk) }
    val nearEnd by androidx.compose.runtime.remember(list) {
        androidx.compose.runtime.derivedStateOf {
            list.layoutInfo.visibleItemsInfo.lastOrNull()?.index?.let { it >= list.layoutInfo.totalItemsCount - 3 } ?: false
        }
    }
    androidx.compose.runtime.LaunchedEffect(nearEnd, total, count) { if (nearEnd && count < total) count += chunk }
    return count
}
