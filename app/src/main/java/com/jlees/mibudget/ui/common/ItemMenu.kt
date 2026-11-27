// The item menu: everything about what's open (for the start screen, this month), in a panel that grows from its ⋮.
// Top to bottom: a header row, up to four large action tiles, a "Tools" heading over round buttons, then plain rows (destructive ones last, in the error colour).
package com.jlees.mibudget.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.jlees.mibudget.ui.theme.LocalMi

/** A large action tile (filled when [on]). */
class MenuTile(val icon: ImageVector, val label: String, val on: Boolean = false, val onClick: () -> Unit)
/** A round tool button with its label under it, and an optional count. */
class MenuTool(val icon: ImageVector, val label: String, val badge: String? = null, val onClick: () -> Unit)
/** A plain row at the bottom; [danger] in the error colour, last. */
class MenuRow(val icon: ImageVector, val label: String, val danger: Boolean = false, val onClick: () -> Unit)

/**
 * The item menu: a panel (surfaceContainer, the Corners radius up to 28, a 10dp shadow, 16 padding) filling the screen
 * between the status bar (+16) and the bottom bar (+88), 12 from the sides, at most 720 wide. It grows from [origin]
 * (its ⋮, window coordinates): scale 0.85→1 on noBounce with a 120 fade, and closes to 0.9 in 120 with a 100 fade. A
 * tap outside closes it; its content scrolls inside it. Every action closes it first.
 */
@Composable
fun ItemMenu(
    open: Boolean, origin: Rect?, onDismiss: () -> Unit,
    icon: ImageVector, title: String, subtitle: String?,
    tiles: List<MenuTile> = emptyList(),
    tools: List<MenuTool> = emptyList(),
    rows: List<MenuRow> = emptyList(),
) {
    val shown = remember { MutableTransitionState(false) }
    shown.targetState = open
    if (!shown.currentState && !shown.targetState) return
    val cs = MaterialTheme.colorScheme
    val radius = minOf(LocalMi.current.radius, 28).dp
    var panel by remember { mutableStateOf<Rect?>(null) }
    val pivot = if (origin != null && panel != null && panel!!.width > 0f) TransformOrigin(
        ((origin.center.x - panel!!.left) / panel!!.width).coerceIn(0f, 1f), ((origin.center.y - panel!!.top) / panel!!.height).coerceIn(0f, 1f),
    ) else TransformOrigin(1f, 1f)
    fun act(f: () -> Unit) { onDismiss(); f() }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val window = (androidx.compose.ui.platform.LocalView.current.parent as? DialogWindowProvider)?.window
        LaunchedEffect(window) { window?.setDimAmount(0f); window?.setWindowAnimations(0) }
        // above the bottom bar, near the ⋮ it grows from
        Box(Modifier.fillMaxSize().clickable(MutableInteractionSource(), null, onClick = onDismiss), contentAlignment = Alignment.BottomCenter) {
            AnimatedVisibility(
                shown,
                Modifier.windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 88.dp),
                enter = scaleIn(Motion.noBounce(), initialScale = 0.85f, transformOrigin = pivot) + fadeIn(tween(120)),
                exit = scaleOut(Motion.exit(120), targetScale = 0.9f, transformOrigin = pivot) + fadeOut(Motion.exit(100)),
            ) {
                Surface(
                    Modifier.widthIn(max = COMFORTABLE_WIDTH.dp).fillMaxWidth().onGloballyPositioned { panel = it.boundsInWindow() }
                        .clickable(MutableInteractionSource(), null) {},
                    shape = RoundedCornerShape(radius), color = cs.surfaceContainer, shadowElevation = 10.dp,
                ) {
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                        // header row
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(44.dp).clip(CircleShape).background(cs.surfaceContainerHighest), contentAlignment = Alignment.Center) {
                                Icon(icon, null, Modifier.size(22.dp), tint = cs.onSurface)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                OneLine(title, color = cs.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                if (subtitle != null) Text(subtitle, color = cs.onSurfaceVariant, fontSize = 13.sp, maxLines = 1)
                            }
                        }
                        // the main actions, up to four
                        if (tiles.isNotEmpty()) Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            tiles.take(4).forEach { t ->
                                val corner by animateDpAsState(if (t.on) 28.dp else 18.dp, Motion.spatial(), label = "tile")
                                val press = remember { MutableInteractionSource() }
                                Column(
                                    Modifier.weight(1f).height(72.dp).pressScale(press).clip(RoundedCornerShape(corner))
                                        .background(if (t.on) cs.primaryContainer else cs.surfaceContainerHighest)
                                        .clickable(press, androidx.compose.material3.ripple()) { act(t.onClick) },
                                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                                ) {
                                    val ink = if (t.on) cs.onPrimaryContainer else cs.onSurface
                                    val pop = popScale(t.on)
                                    Icon(t.icon, null, Modifier.size(22.dp).graphicsLayer { scaleX = pop; scaleY = pop }, tint = ink)
                                    Text(t.label, Modifier.padding(top = 4.dp), color = ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, textAlign = TextAlign.Center)
                                }
                            }
                        }
                        if (tools.isNotEmpty()) {
                            MenuHeading("Tools")
                            tools.chunked(4).forEach { row ->
                                Row(Modifier.fillMaxWidth()) {
                                    row.forEach { t ->
                                        Column(
                                            Modifier.weight(1f).heightIn(min = 84.dp).clip(RoundedCornerShape(16.dp)).clickable { act(t.onClick) }.padding(vertical = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            Box {
                                                Box(Modifier.size(50.dp).clip(CircleShape).background(cs.secondaryContainer), contentAlignment = Alignment.Center) {
                                                    Icon(t.icon, null, Modifier.size(24.dp), tint = cs.onSecondaryContainer)
                                                }
                                                if (t.badge != null) Text(
                                                    t.badge.take(4), Modifier.align(Alignment.TopEnd).clip(CircleShape).background(cs.primary).padding(horizontal = 5.dp, vertical = 1.dp),
                                                    color = cs.onPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                                )
                                            }
                                            Text(t.label, Modifier.padding(top = 4.dp), color = cs.onSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, textAlign = TextAlign.Center)
                                        }
                                    }
                                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                        if (rows.isNotEmpty()) {
                            HorizontalDivider(Modifier.padding(vertical = 8.dp), 1.dp, cs.outlineVariant)
                            rows.sortedBy { it.danger }.forEach { r ->
                                val ink = if (r.danger) cs.error else cs.onSurface
                                Row(
                                    Modifier.fillMaxWidth().heightIn(min = 44.dp).clip(RoundedCornerShape(14.dp)).clickable { act(r.onClick) }.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(r.icon, null, Modifier.size(22.dp), tint = ink)
                                    Spacer(Modifier.width(16.dp))
                                    Text(r.label, color = ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A heading inside the menu: 13sp bold primary ("This Month", "Tools"). */
@Composable
private fun MenuHeading(text: String) =
    Text(text, Modifier.padding(start = 6.dp, top = 14.dp, bottom = 8.dp), color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)

/** What a + asks first: scan a receipt or type it in (or pick up the unfinished one). The item menu, from [origin]. */
@Composable
fun AddPrompt(
    open: Boolean, origin: Rect?, onDismiss: () -> Unit, title: String,
    onScan: () -> Unit, onManual: () -> Unit, onResume: (() -> Unit)? = null,
) = ItemMenu(
    open = open, origin = origin, onDismiss = onDismiss, icon = Sym.Add, title = title, subtitle = "Scan a receipt, or type it in",
    tiles = listOfNotNull(
        MenuTile(Sym.PhotoCamera, "Scan Receipt", onClick = onScan),
        MenuTile(Sym.Edit, "Add Manually", onClick = onManual),
        onResume?.let { MenuTile(Sym.History, "Resume", onClick = it) },
    ),
)
