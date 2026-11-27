// The pieces a mi. form is built from, beside LabeledField / PickerField (Dialogs.kt): headings, choice chips, switch
// rows, the scan banner, and the button bar pinned at the bottom. The purchase and subscription editors use them, so
// every form in the app lines up: fields 15 apart, 20 in from the edges.
package com.jlees.mibudget.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jlees.mibudget.ui.theme.LocalMi

/** Between a form's fields. */
val FormGap: Dp = 15.dp

/**
 * A form box's corners (fields, switches, detail rows): the Corners setting, as everything else that's rounded; inside
 * a [FormGroup], the small inner corners of a segment (the group rounds its outer ones).
 */
@Composable
fun fieldShape(): androidx.compose.ui.graphics.Shape =
    if (LocalInGroup.current) RoundedCornerShape(4.dp) else RoundedCornerShape(LocalMi.current.radius.dp)

/** What chips and fields are filled with: surfaceContainerHighest, or a step darker on a row of that colour ([FormRow]). */
val LocalFieldColor = androidx.compose.runtime.staticCompositionLocalOf<Color?> { null }

/** True inside a [FormGroup]: fields are its rows, laid out as Settings' rows (label inside, over the value). */
val LocalInGroup = androidx.compose.runtime.staticCompositionLocalOf { false }

/**
 * A section's rows as one segmented block, exactly as Settings' sections: each row surfaceContainerHighest, 2dp apart,
 * the block's outer corners at the Corners radius (at least 4) and 4dp between rows. The fields inside take Settings'
 * row layout: a 40dp tonal circle, the label (13sp) over the value (16sp semibold), at least 64 tall.
 */
@Composable
fun FormGroup(modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val r = maxOf(LocalMi.current.radius, 4).dp
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(r)), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        androidx.compose.runtime.CompositionLocalProvider(LocalInGroup provides true) { content() }
    }
}

/** A row's symbol in Settings' 40dp secondaryContainer circle. */
@Composable
fun RowIcon(icon: ImageVector) {
    val cs = MaterialTheme.colorScheme
    Box(Modifier.size(40.dp).clip(CircleShape).background(cs.secondaryContainer), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(22.dp), tint = cs.onSecondaryContainer)
    }
}

/**
 * A row of a [FormGroup] holding a choice (chips, a connected group): Settings' row (its symbol, title, an optional
 * line) with the choice under it, 16 in from the sides and 14 above the row's bottom, as Settings' rows with choices.
 */
@Composable
fun FormRow(icon: ImageVector, title: String, subtitle: String? = null, content: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().clip(fieldShape()).background(cs.surfaceContainerHighest)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            RowIcon(icon)
            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                Text(title, color = cs.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                if (subtitle != null) Text(subtitle, Modifier.padding(top = 2.dp), color = cs.onSurfaceVariant, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
        // what's under the row is on the row's colour: its chips and fields a step darker, as Settings' choices
        Box(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 14.dp)) {
            androidx.compose.runtime.CompositionLocalProvider(LocalInGroup provides false, LocalFieldColor provides cs.surfaceContainer) { content() }
        }
    }
}

/**
 * A section's title, the same on every editor and summary page: 19sp regular in the text's own colour (not the
 * accent), larger than everything under it so it stands out without being bold, with room above.
 */
@Composable
fun FormHeading(text: String, modifier: Modifier = Modifier) {
    Text(
        text, modifier.padding(start = 8.dp, top = 14.dp).semantics { heading() },
        color = MaterialTheme.colorScheme.onSurface, fontSize = 19.sp, fontWeight = FontWeight.Normal,
    )
}

/** A line under a field or group (13sp): what something adds up to, or what it means. */
@Composable
fun FormNote(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, lineHeight = 18.sp)
}

/**
 * One choice among several, as capsules: the chosen one in secondaryContainer with a check, the others in the fields'
 * colour. They wrap onto more lines when there are many. ([options]: value to label.)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChipChoice(options: List<Pair<T, String>>, selected: T?, onPick: (T) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
        options.forEach { (v, text) -> MiChip(text, v == selected) { onPick(v) } }
    }
}

/** A capsule choice: 40 tall (48 to touch), 14sp semibold. */
@Composable
fun MiChip(text: String, selected: Boolean, leading: ImageVector? = null, role: Role = Role.RadioButton, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val bg by animateColorAsState(if (selected) cs.secondaryContainer else LocalFieldColor.current ?: cs.surfaceContainerHighest, Motion.enter(200), label = "chip")
    val ink = if (selected) cs.onSecondaryContainer else cs.onSurface
    Row(
        Modifier.minimumInteractiveComponentSize().height(40.dp).clip(CircleShape).background(bg)
            .clickable(role = role, onClick = onClick).then(if (role == Role.RadioButton) Modifier.semantics { this.selected = selected } else Modifier)
            .padding(start = if (selected || leading != null) 12.dp else 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val icon = if (selected) Sym.Check else leading
        if (icon != null) { Icon(icon, null, Modifier.size(18.dp), tint = ink); Spacer(Modifier.width(6.dp)) }
        Text(text, color = ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** A switch in a form, in the same box as the fields: title (16sp semibold), an optional line under it, MiSwitch. */
@Composable
fun SwitchField(
    title: String, checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier,
    subtitle: String? = null, icon: ImageVector? = null,
) {
    val cs = MaterialTheme.colorScheme
    val grouped = LocalInGroup.current
    Row(
        modifier.fillMaxWidth().heightIn(min = if (grouped) 64.dp else 56.dp).clip(fieldShape()).background(cs.surfaceContainerHighest)
            .toggleable(checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 16.dp, vertical = if (grouped) 12.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { RowIcon(icon); Spacer(Modifier.width(16.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, color = cs.onSurface, fontSize = 16.sp, fontWeight = if (grouped) FontWeight.Medium else FontWeight.SemiBold)
            if (subtitle != null) Text(subtitle, Modifier.padding(top = 2.dp), color = cs.onSurfaceVariant, fontSize = 13.sp, lineHeight = 18.sp)
        }
        Spacer(Modifier.width(12.dp))
        MiSwitch(checked, null)
    }
}

/**
 * A card at the top of a form for something to know first (filled in from a scan), as Settings' setup card:
 * primaryContainer, the Corners radius (at least 12), a 28dp symbol, the title over one line, a tonal button at the end.
 */
@Composable
fun FormBanner(icon: ImageVector, title: String, text: String, modifier: Modifier = Modifier, action: String? = null, onAction: () -> Unit = {}) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(maxOf(LocalMi.current.radius, 12).dp)).background(cs.primaryContainer).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(28.dp), tint = cs.onPrimaryContainer)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = cs.onPrimaryContainer, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(text, Modifier.padding(top = 2.dp), color = cs.onPrimaryContainer, fontSize = 13.sp, lineHeight = 18.sp)
        }
        if (action != null) { Spacer(Modifier.width(8.dp)); FilledTonalButton(onClick = onAction) { Text(action) } }
    }
}

/**
 * The form's main action, pinned at the bottom (riding on the keyboard while typing): a full-width 52-tall Button on
 * the page's colour, 20 in from the sides, 12 above and below.
 */
@Composable
fun FormActionBar(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow)
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Button(onClick = onClick, enabled = enabled, modifier = Modifier.widthIn(max = PILL_MAX_WIDTH.dp).fillMaxWidth().height(52.dp)) {
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * The item's picture at the start of its name field: [avatar] (56dp) with a small primary edit badge at its corner;
 * tapping it changes the picture.
 */
@Composable
fun IconTile(onClick: () -> Unit, avatar: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(Modifier.size(56.dp)) {
        Box(Modifier.matchParentSize().clip(CircleShape).clickable(onClickLabel = "Change picture", onClick = onClick)) { avatar() }
        // the badge sits on the circle's edge, outside its clip (inside it, its corner was cut off)
        Box(
            Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 4.dp).size(24.dp).clip(CircleShape).background(cs.surfaceContainerLow).padding(2.dp)
                .clip(CircleShape).background(cs.primary),
            contentAlignment = Alignment.Center,
        ) { Icon(Sym.Edit, null, Modifier.size(12.dp), tint = cs.onPrimary) }
    }
}
