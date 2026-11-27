// Settings › Scanner: one scanner, with its status and details.
package com.jlees.mibudget.ui.settings

import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jlees.mibudget.scan.NanoStatus
import com.jlees.mibudget.scan.ScanEngine
import com.jlees.mibudget.ui.common.Sym
import com.jlees.mibudget.ui.common.appear
import com.jlees.mibudget.ui.common.disappear

@Composable
internal fun EngineItem(
    row: EngineRow,
    selected: Boolean,
    nanoMessage: String?,
    onSelect: () -> Unit,
) {
    val e = row.engine
    val status: String = when (e) {
        ScanEngine.STANDARD -> "Built in · ${e.speed}"
        ScanEngine.GEMINI_NANO -> when (row.nano) {
            NanoStatus.READY -> "Available on this phone · ${e.speed}"
            NanoStatus.NEEDS_DOWNLOAD -> nanoMessage?.takeIf { it.startsWith("failed") || it.startsWith("Couldn't") }
                ?.let { "Setup didn't finish ($it) · select it to try again" }
                ?: "Supported · Android sets it up when you select it"
            NanoStatus.DOWNLOADING -> "Android is setting it up… " + (nanoMessage?.takeIf { it != "done" } ?: "")
            NanoStatus.UNKNOWN -> "Couldn't check with Android right now · try again in a moment"
            else -> "Not available on this phone"
        }
    }
    var showInfo by rememberSaveable(e.name) { mutableStateOf(false) }
    Item(
        Sym.DocumentScanner, e.title, status,
        onClick = if (row.ready) onSelect else null,
        // as every Settings row: a 40dp circle; the chosen scanner's is filled with the accent's container and a check
        // (mi.'s "this one is on"), one that can't be used yet is faded
        leading = {
            val cs = MaterialTheme.colorScheme
            Box(
                Modifier.size(40.dp).clip(CircleShape)
                    .background(if (selected) cs.primaryContainer else cs.secondaryContainer)
                    .semantics { this.selected = selected; role = Role.RadioButton },
                contentAlignment = Alignment.Center,
            ) {
                // each scanner its own symbol, always fully shown (the line under the title says why one can't be used)
                Icon(if (selected) Sym.Check else when (e) {
                    ScanEngine.STANDARD -> Sym.DocumentScanner
                    ScanEngine.GEMINI_NANO -> Sym.AutoAwesome
                }, null, Modifier.size(22.dp), tint = if (selected) cs.onPrimaryContainer else cs.onSecondaryContainer)
            }
        },
        trailing = {
            // the (i) reveals the full description
            IconButton(onClick = { showInfo = !showInfo }) {
                Icon(if (showInfo) Sym.Info else Sym.InfoOutlined, if (showInfo) "Hide Details" else "Details",
                    tint = if (showInfo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        below = if (!showInfo) null else { {
            Column(Modifier.padding(start = 56.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                AnimatedVisibility(visible = showInfo, enter = appear(), exit = disappear()) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(e.summary, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, lineHeight = 18.sp)
                        val facts = buildList {
                            add("Speed: ${e.speed}")
                            add(if (e == ScanEngine.STANDARD) "Reads: text, from 3 cleaned-up copies of the photo" else "Reads: the original photo")
                        }
                        facts.forEach { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, lineHeight = 18.sp) }
                    }
                }
            }
        } },
    )
}
