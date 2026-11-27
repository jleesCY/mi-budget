// mi.'s pop-ups (after mi. Browser's ModalFrame / DialogCard): a full-screen frame with its own fading black scrim and
// a card that scales in, the dialog card itself, its text actions, fields, and the bottom panel.
package com.jlees.mibudget.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.jlees.mibudget.ui.theme.LocalMi

/**
 * A full-screen frame for a dialog: Android's own dim and animation are off, so mi.'s are the only ones: black at 50%
 * fading in over 200 (out over 120), the card scaling in from 0.9 (250) and out to 0.95 (120). It stays clear of the
 * system bars, cutouts and the keyboard. A tap on the scrim closes it.
 */
@Composable
fun ModalFrame(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        LaunchedEffect(window) { window?.setDimAmount(0f); window?.setWindowAnimations(0) }
        val shown = remember { MutableTransitionState(false).apply { targetState = true } }
        Box(Modifier.fillMaxSize()) {
            AnimatedVisibility(shown, enter = fadeIn(Motion.enter(200)), exit = fadeOut(Motion.exit(120))) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f))
                    .clickable(MutableInteractionSource(), null) { onDismiss() })
            }
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                AnimatedVisibility(
                    shown,
                    enter = scaleIn(Motion.enter(250), initialScale = 0.9f) + fadeIn(Motion.enter(200)),
                    exit = scaleOut(Motion.exit(120), targetScale = 0.95f) + fadeOut(Motion.exit(120)),
                ) { content() }
            }
        }
    }
}

/**
 * The dialog card: surfaceContainerHigh, 28 corners, tonal elevation 6, 280–560 wide and 24 from the screen's sides.
 * The title (24sp regular, Title Case) and buttons stay put; the body (14sp onSurfaceVariant) scrolls when space runs out.
 */
@Composable
fun DialogCard(
    title: String?,
    modifier: Modifier = Modifier,
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier.widthIn(min = 280.dp, max = 560.dp).padding(horizontal = 24.dp),
        shape = MaterialTheme.shapes.extraLarge, color = cs.surfaceContainerHigh, tonalElevation = 6.dp,
    ) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = if (actions != null) 12.dp else 24.dp)) {
            if (title != null) Text(title, Modifier.padding(bottom = 16.dp), color = cs.onSurface, fontSize = 24.sp, fontWeight = FontWeight.Normal)
            CompositionLocalProvider(LocalContentColor provides cs.onSurfaceVariant) {
                ProvideTextStyle(TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontFamily = MaterialTheme.typography.bodyMedium.fontFamily)) {
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), content = content)
                }
            }
            if (actions != null) Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), content = actions)
        }
    }
}

/** A dialog's text button, named for what it does ("Delete", "Got It"); [destructive] in the error colour. */
@Composable
fun DialogAction(text: String, destructive: Boolean = false, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    TextButton(onClick) {
        Text(text, color = if (destructive) cs.error else cs.primary,
            style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, maxLines = 2)
    }
}

/**
 * The same slots as Material's AlertDialog, drawn as a mi. dialog (ModalFrame + DialogCard): Cancel ([dismissButton])
 * then the action ([confirmButton]), at the end. [icon] isn't drawn: mi. dialogs have none.
 */
@Composable
fun MiDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
) {
    ModalFrame(onDismissRequest) {
        DialogCard(null, modifier, actions = { dismissButton?.invoke(); confirmButton() }) {
            // the title as DialogCard draws one (24sp regular onSurface, 16 below), over the body
            if (title != null) Box(Modifier.padding(bottom = 16.dp)) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                    ProvideTextStyle(TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Normal, fontFamily = MaterialTheme.typography.headlineSmall.fontFamily)) { title() }
                }
            }
            text?.invoke()
        }
    }
}

/**
 * Before something Undo can't bring back: says exactly what will be lost, with Cancel and an action named for what it
 * does. Everything else happens at once, with Undo.
 */
@Composable
fun ConfirmDialog(title: String, text: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit, destructive: Boolean = false) {
    ModalFrame(onDismiss) {
        DialogCard(title, actions = {
            DialogAction("Cancel", onClick = onDismiss)
            DialogAction(confirmLabel, destructive = destructive) { onConfirm(); onDismiss() }
        }) { Text(text) }
    }
}

/**
 * mi.'s text field (outside the Pill): 56 tall, surfaceContainerHighest, 16dp corners, 16 padding sideways, 16sp
 * W600 text (placeholder regular onSurfaceVariant), cursor primary, and a 48dp clear button popping in while there's text.
 */
@Composable
fun ClearableField(
    value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier,
    keyboard: KeyboardOptions = KeyboardOptions.Default, singleLine: Boolean = true, minLines: Int = 1,
    leading: (@Composable () -> Unit)? = null, isError: Boolean = false,
    prefix: String? = null, suffix: String? = null,
    /** Bigger text, for the one amount a screen is about. */
    large: Boolean = false,
    /** Settings' row layout (in a [FormGroup]): this label inside the box over the text, and a line under it. */
    label: String? = null, note: String? = null, noteColor: Color = Color.Unspecified,
) {
    val cs = MaterialTheme.colorScheme
    val size = if (large) 22.sp else 16.sp
    val row = label != null
    Row(
        modifier.fillMaxWidth().then(if (row) Modifier.heightIn(min = 64.dp) else if (singleLine) Modifier.height(56.dp) else Modifier.heightIn(min = 56.dp))
            .clip(fieldShape()).background(LocalFieldColor.current ?: cs.surfaceContainerHighest)
            .then(if (isError) Modifier.background(cs.errorContainer.copy(alpha = 0.35f)) else Modifier)
            .padding(start = 16.dp, end = 4.dp, top = if (row) 12.dp else 0.dp, bottom = if (row) 12.dp else 0.dp),
        verticalAlignment = if (singleLine || row) Alignment.CenterVertically else Alignment.Top,
    ) {
        if (leading != null) { leading(); Box(Modifier.width(if (row) 16.dp else 12.dp)) }
        Column(Modifier.weight(1f)) {
            if (label != null) Text(label, color = cs.onSurfaceVariant, fontSize = 13.sp, lineHeight = 18.sp, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (prefix != null) Text(prefix, Modifier.padding(end = 4.dp), color = cs.onSurfaceVariant, fontSize = size, fontWeight = FontWeight.SemiBold)
                Box(Modifier.weight(1f).padding(vertical = if (singleLine || row) 0.dp else 16.dp), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text(placeholder, color = cs.onSurfaceVariant, fontSize = size, maxLines = if (singleLine) 1 else minLines)
                    BasicTextField(
                        value, onChange, Modifier.fillMaxWidth(), singleLine = singleLine, minLines = minLines, keyboardOptions = keyboard,
                        textStyle = TextStyle(color = cs.onSurface, fontSize = size, fontWeight = FontWeight.SemiBold, fontFamily = MaterialTheme.typography.bodyLarge.fontFamily),
                        cursorBrush = SolidColor(cs.primary),
                    )
                }
            }
            if (note != null) Text(note, Modifier.padding(top = 2.dp), color = if (noteColor == Color.Unspecified) cs.onSurfaceVariant else noteColor, fontSize = 13.sp, lineHeight = 18.sp, maxLines = 2)
        }
        if (suffix != null) Text(suffix, Modifier.padding(start = 4.dp, end = 8.dp), color = cs.onSurfaceVariant, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        AnimatedVisibility(value.isNotEmpty(),
            enter = fadeIn(Motion.enter(150)) + scaleIn(Motion.enter(200), initialScale = 0.6f),
            exit = fadeOut(Motion.exit(100)) + scaleOut(Motion.exit(100), targetScale = 0.6f)) {
            IconButton(onClick = { onChange("") }) { Icon(Sym.Cancel, "Clear", Modifier.size(20.dp), tint = cs.onSurfaceVariant) }
        }
    }
}


/** A field with its label above it (14sp regular onSurfaceVariant, 5 above the field). */
@Composable
fun LabeledField(
    label: String, value: String, onChange: (String) -> Unit, placeholder: String = "", modifier: Modifier = Modifier,
    keyboard: KeyboardOptions = KeyboardOptions.Default, singleLine: Boolean = true, minLines: Int = 1, isError: Boolean = false,
    prefix: String? = null, suffix: String? = null, large: Boolean = false,
    /** A line under the field (13sp), e.g. what the amount is made of. */
    note: String? = null,
    /** In a [FormGroup]: the row's symbol (a 40dp tonal circle), or something of its own there instead ([leading]). */
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null, leading: (@Composable () -> Unit)? = null,
) {
    if (LocalInGroup.current) {
        ClearableField(
            value, onChange, placeholder, modifier, keyboard = keyboard, singleLine = singleLine, minLines = minLines, isError = isError,
            prefix = prefix, suffix = suffix, large = large, label = label, note = note,
            noteColor = if (isError) MaterialTheme.colorScheme.error else Color.Unspecified,
            leading = leading ?: icon?.let { ic -> @Composable { RowIcon(ic) } },
        )
        return
    }
    Column(modifier.fillMaxWidth()) {
        Text(label, Modifier.padding(bottom = 5.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        ClearableField(value, onChange, placeholder, keyboard = keyboard, singleLine = singleLine, minLines = minLines, isError = isError, prefix = prefix, suffix = suffix, large = large)
        if (note != null) Text(note, Modifier.padding(start = 16.dp, top = 4.dp), color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, maxLines = 1)
    }
}

/**
 * A picker that looks like a field (a date, a category, a payment method): its label above, then the same box, with
 * the value (16sp semibold; a path joined with " › "), an optional [leading] image, and a chevron. It opens a picker.
 */
@Composable
fun PickerField(
    label: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null, detail: String? = null, valueColor: Color = Color.Unspecified,
) {
    val cs = MaterialTheme.colorScheme
    // in a group: Settings' row (its picture, the label over the value, ›), as the summary's detail rows
    if (LocalInGroup.current) {
        Row(
            modifier.fillMaxWidth().heightIn(min = 64.dp).clip(fieldShape()).background(cs.surfaceContainerHighest)
                .clickable(onClickLabel = "Choose $label", onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) { Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { leading() }; Box(Modifier.width(16.dp)) }
            Column(Modifier.weight(1f)) {
                Text(label, color = cs.onSurfaceVariant, fontSize = 13.sp, lineHeight = 18.sp, maxLines = 1)
                Text(value, color = if (valueColor == Color.Unspecified) cs.onSurface else valueColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                if (detail != null) Text(detail, color = cs.onSurfaceVariant, fontSize = 12.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            Icon(Sym.ChevronRight, null, Modifier.size(22.dp), tint = cs.onSurfaceVariant)
        }
        return
    }
    Column(modifier.fillMaxWidth()) {
        Text(label, Modifier.padding(bottom = 5.dp), color = cs.onSurfaceVariant, fontSize = 14.sp)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(fieldShape()).background(cs.surfaceContainerHighest)
                .clickable(onClickLabel = "Choose $label", onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) { leading(); Box(Modifier.width(12.dp)) }
            Column(Modifier.weight(1f)) {
                Text(value, color = if (valueColor == Color.Unspecified) cs.onSurface else valueColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                if (detail != null) Text(detail, color = cs.onSurfaceVariant, fontSize = 12.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            Icon(Sym.ChevronRight, null, Modifier.size(20.dp), tint = cs.onSurfaceVariant)
        }
    }
}

/**
 * A bottom panel (one thing's details, a picker, filters): edge to edge across the bottom, at most 720 wide,
 * surfaceContainerLow, top corners at the Corners radius, a 36×4 handle, black at 50% behind it (a tap there closes it).
 */
@Composable
fun MiBottomPanel(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val radius = LocalMi.current.radius.dp
    ModalBottomSheet(
        onDismissRequest = onDismissRequest, modifier = modifier.widthIn(max = COMFORTABLE_WIDTH.dp), sheetState = sheetState,
        shape = RoundedCornerShape(topStart = radius, topEnd = radius),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        dragHandle = {
            Box(Modifier.fillMaxWidth().height(28.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(36.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.outlineVariant))
            }
        },
        content = content,
    )
}

/** A bottom panel's title: 24sp regular, as a dialog's, 24 in. */
@Composable
fun PanelTitle(text: String) =
    Text(text, color = MaterialTheme.colorScheme.onSurface, fontSize = 24.sp, modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp))

/**
 * One choice in a bottom panel's list: a 14dp-cornered row, 12 in from the panel's sides, at least 56 tall, straight on
 * the panel (no colour of its own) with its [leading] image, 16, the title (16sp semibold) over an optional line (13sp).
 * The chosen one fills with primaryContainer and shows a check. [indent] nests a row under its parent.
 */
@Composable
fun ChoiceRow(
    title: String, selected: Boolean, onClick: () -> Unit,
    supporting: String? = null, indent: androidx.compose.ui.unit.Dp = 0.dp,
    /** After the text (and the check): an expander's button, say. */
    trailing: (@Composable () -> Unit)? = null,
    leading: @Composable () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val text = if (selected) cs.onPrimaryContainer else cs.onSurface
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp).clip(RoundedCornerShape(14.dp))
            .background(if (selected) cs.primaryContainer else Color.Transparent)
            .selectable(selected, role = androidx.compose.ui.semantics.Role.RadioButton, onClick = onClick)
            .heightIn(min = 56.dp).padding(start = 12.dp + indent, end = if (trailing != null) 0.dp else 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        androidx.compose.foundation.layout.Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            if (supporting != null) Text(
                supporting, color = if (selected) text.copy(alpha = 0.8f) else cs.onSurfaceVariant, fontSize = 13.sp, lineHeight = 18.sp,
                maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (selected) Icon(Sym.Check, null, Modifier.padding(start = 8.dp).size(22.dp), tint = text)
        trailing?.invoke()
    }
}

/**
 * A menu (⋮, a choice that drops down): surfaceContainer, the Corners radius up to 28, a 10dp shadow. Its rows are
 * at least 48 tall.
 */
@Composable
fun MiMenu(expanded: Boolean, onDismissRequest: () -> Unit, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    androidx.compose.material3.DropdownMenu(
        expanded = expanded, onDismissRequest = onDismissRequest, modifier = modifier,
        shape = RoundedCornerShape(minOf(LocalMi.current.radius, 28).dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 0.dp, shadowElevation = 10.dp,
        content = content,
    )
}
