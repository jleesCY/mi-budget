package com.jlees.mibudget.ui.components

import com.jlees.mibudget.ui.common.MiDialog
import com.jlees.mibudget.ui.common.MiBottomPanel

import com.jlees.mibudget.ui.common.Sym

import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jlees.mibudget.domain.CategoryTree
import com.jlees.mibudget.icons.BrandMatcher
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

val MediumDate: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

private val DayThisYear: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d")
private val DayOtherYear: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")

/** A day as a heading — "Today", "Yesterday", "Friday, October 3", "Monday, March 3, 2025". */
fun dayLabel(date: java.time.LocalDate, today: java.time.LocalDate = java.time.LocalDate.now()): String = when {
    date == today -> "Today"
    date == today.minusDays(1) -> "Yesterday"
    else -> date.format(if (date.year == today.year) DayThisYear else DayOtherYear)
}

@Composable
fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Amount",
    isError: Boolean = false,
    large: Boolean = true,
    supportingText: String? = null,
    /** Next when another field follows (the keyboard's ↵ moves on), Done for the last one. */
    imeAction: androidx.compose.ui.text.input.ImeAction = androidx.compose.ui.text.input.ImeAction.Done,
    /** Its symbol, in a FormGroup. */
    icon: ImageVector? = null,
) {
    com.jlees.mibudget.ui.common.LabeledField(
        label = label, value = value, onChange = { v -> onValueChange(v.filter { it.isDigit() || it == '.' || it == ',' }) },
        placeholder = "0.00", prefix = "$", isError = isError, large = large, note = supportingText, modifier = modifier, icon = icon,
        keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction),
    )
}

/** Read-only text field that opens a Material date picker. */
@Composable
fun DateField(
    label: String,
    date: LocalDate?,
    onDateChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    allowClear: Boolean = false,
) {
    var open by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    LaunchedEffect(interaction) {
        interaction.interactions.collect { if (it is PressInteraction.Release) open = true }
    }
    com.jlees.mibudget.ui.common.PickerField(label, date?.format(MediumDate) ?: "None", onClick = { open = true }, modifier = modifier,
        leading = {
            if (com.jlees.mibudget.ui.common.LocalInGroup.current) com.jlees.mibudget.ui.common.RowIcon(Sym.CalendarMonth)
            else Icon(Sym.CalendarMonth, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        })
    if (open) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (date ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onDateChange(LocalDate.ofEpochDay(it / 86_400_000L)) }
                    open = false
                }) { Text("Choose") }
            },
            dismissButton = {
                Row {
                    if (allowClear) TextButton(onClick = { onDateChange(null); open = false }) { Text("Clear") }
                    TextButton(onClick = { open = false }) { Text("Cancel") }
                }
            },
        ) { DatePicker(state = state) }
    }
}

/** Field that shows the chosen category and opens the picker sheet. */
@Composable
fun CategoryField(
    tree: CategoryTree,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Category",
    highlightEmpty: Boolean = false,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val cat = selectedId?.let { tree.byId[it] }
    val path = tree.path(selectedId).dropLast(1)
    com.jlees.mibudget.ui.common.PickerField(
        label, cat?.name ?: "Uncategorized", onClick = { open = true }, modifier = modifier,
        leading = {
            val size = if (com.jlees.mibudget.ui.common.LocalInGroup.current) 40.dp else 32.dp
            if (cat != null) CategoryBadge(cat, size = size) else UncategorizedBadge(size = size)
        },
        detail = if (path.isNotEmpty()) path.joinToString(" › ") { it.name } else null,
        valueColor = if (cat == null && highlightEmpty) MaterialTheme.colorScheme.error else Color.Unspecified,
    )
    if (open) {
        CategoryPickerSheet(tree, selectedId, onDismiss = { open = false }, onSelect = { onSelect(it); open = false })
    }
}

@Composable
fun CategoryPickerSheet(
    tree: CategoryTree,
    selectedId: String?,
    onDismiss: () -> Unit,
    onSelect: (String?) -> Unit,
    title: String = "Choose Category",
    allowNone: Boolean = true,
    noneLabel: String = "Uncategorized",
    /** Optional second line under the "none" choice (e.g. what choosing it means). */
    noneSupporting: String? = null,
    excludeSubtreeOf: String? = null,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val excluded = remember(excludeSubtreeOf, tree) { excludeSubtreeOf?.let { tree.subtreeIds(it) } ?: emptySet() }
    // starts folded to the top level, opened only down to what's chosen; searching looks through everything
    var open by rememberSaveable { mutableStateOf(tree.path(selectedId).dropLast(1).map { it.id }.toSet()) }
    val rows = remember(tree, query, excluded, open) {
        val q = query.trim().lowercase()
        tree.flattened(if (q.isEmpty()) open else null).filter { (c, _) -> c.id !in excluded && (q.isEmpty() || c.name.lowercase().contains(q)) }
    }
    MiBottomPanel(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        com.jlees.mibudget.ui.common.PanelTitle(title)
        com.jlees.mibudget.ui.common.ClearableField(
            value = query, onChange = { query = it }, placeholder = "Search categories", leading = { Icon(Sym.Search, null) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), singleLine = true,
        )
        LazyColumn(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (allowNone && query.isBlank()) item {
                com.jlees.mibudget.ui.common.ChoiceRow(noneLabel, selectedId == null, { onSelect(null) }, supporting = noneSupporting) { UncategorizedBadge(size = 36.dp) }
            }
            items(rows, key = { it.first.id }) { (c, depth) ->
                com.jlees.mibudget.ui.common.ChoiceRow(
                    c.name, selectedId == c.id, { onSelect(c.id) },
                    // searching flattens the tree, so a match says where it lives instead of being indented
                    supporting = if (query.isNotBlank() && c.parentId != null) tree.pathLabel(c.parentId) else null,
                    indent = if (query.isBlank()) (depth * 24).dp else 0.dp,
                    trailing = if (query.isBlank() && tree.children(c.id).any { it.id !in excluded }) { {
                        val isOpen = c.id in open
                        IconButton(
                            onClick = { open = if (isOpen) open - tree.subtreeIds(c.id) else open + c.id },
                            modifier = Modifier.semantics { contentDescription = if (isOpen) "Hide ${c.name}'s categories" else "Show ${c.name}'s categories" },
                        ) { com.jlees.mibudget.ui.common.Chevron(isOpen, 24f, if (selectedId == c.id) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant) }
                    } } else null,
                ) { CategoryBadge(c, size = 36.dp) }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun IconCircle(icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier.size(36.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp)) }
}

@Composable
fun ColorPicker(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        CategoryColors.forEach { c ->
            val argb = c.toInt()
            ColorSwatch(Color(c), selected = argb == selected, dotSize = 36.dp) { onSelect(argb) }
        }
    }
}

/**
 * One colour choice: a dot inside a 48 dp touch target, read out by TalkBack as its colour name
 * ("Blue, selected") and as one of a set of choices.
 */
@Composable
fun ColorSwatch(
    color: Color, selected: Boolean, dotSize: androidx.compose.ui.unit.Dp = 36.dp,
    /** What a screen reader says (else a plain name for the colour). */
    label: String? = null,
    /** Shown in the dot when not selected (the Adaptive accent's wallpaper). */
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit,
) {
    val name = label ?: colorName(color)
    Box(
        Modifier
            .size(maxOf(48.dp, dotSize + 8.dp))
            .clip(CircleShape)
            .selectable(selected = selected, role = androidx.compose.ui.semantics.Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(dotSize)
                .background(color, CircleShape)
                .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (selected || icon != null) Icon(
                if (selected) Sym.Check else icon!!, null, modifier = Modifier.size(18.dp),
                tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
            )
        }
    }
}

/** A plain-words name for a colour, for screen readers ("Dark blue", "Light green", "Grey"). */
fun colorName(color: Color): String {
    val r = color.red; val g = color.green; val b = color.blue
    val max = maxOf(r, g, b); val min = minOf(r, g, b)
    val l = (max + min) / 2f
    val d = max - min
    if (d < 0.08f) return when { l < 0.2f -> "Black"; l > 0.85f -> "White"; else -> "Grey" }
    val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
    var h = when (max) {
        r -> ((g - b) / d) % 6f
        g -> (b - r) / d + 2f
        else -> (r - g) / d + 4f
    } * 60f
    if (h < 0) h += 360f
    val base = when {
        h < 15 || h >= 345 -> "Red"
        h < 40 -> if (l < 0.4f || s < 0.5f) "Brown" else "Orange"
        h < 65 -> if (l < 0.35f) "Olive" else "Yellow"
        h < 160 -> "Green"
        h < 190 -> "Teal"
        h < 250 -> "Blue"
        h < 290 -> "Purple"
        else -> "Pink"
    }
    return when {
        base == "Brown" || base == "Olive" -> base
        l < 0.3f -> "Dark ${base.lowercase()}"
        l > 0.7f -> "Light ${base.lowercase()}"
        else -> base
    }
}

@Composable
fun IconPicker(selected: String, color: Int, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CategoryIcons.all.forEach { (key, icon) ->
            val isSel = key == selected
            Box(
                Modifier
                    .size(44.dp)
                    .background(
                        if (isSel) Color(color).copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                        CircleShape,
                    )
                    .clickable { onSelect(key) },
                contentAlignment = Alignment.Center,
            ) { Icon(icon, key, tint = if (isSel) Color(color) else MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}







/** Shown instead of an editor when its item no longer exists (deleted elsewhere, or a stale notification). */
@Composable
fun ItemNotFound(text: String, onBack: () -> Unit) {
    com.jlees.mibudget.ui.common.MiScaffold(title = "", onBack = onBack) { padding ->
        com.jlees.mibudget.ui.common.EmptyLine(Sym.SearchOff, text, Modifier.padding(padding),
            action = { androidx.compose.material3.FilledTonalButton(onClick = onBack) { Text("Go Back") } })
    }
}
