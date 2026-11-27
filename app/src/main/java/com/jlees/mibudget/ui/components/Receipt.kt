package com.jlees.mibudget.ui.components

import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import com.jlees.mibudget.ui.common.Sym

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.height
import java.io.File

/** Thumbnail + attach/remove controls for a receipt or screenshot image. */
@Composable
fun ReceiptSection(
    file: File?,
    onAttach: (android.net.Uri) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var viewing by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onAttach(uri)
    }
    // a field-like box under its label, as the other fields: "Attach…" with a + photo symbol, or the picture and ✕
    val cs = MaterialTheme.colorScheme
    // in a group: Settings' row (its symbol or the picture, "Receipt" over what's there, › or ✕)
    if (com.jlees.mibudget.ui.common.LocalInGroup.current) {
        Row(
            modifier.fillMaxWidth().heightIn(min = 64.dp).clip(com.jlees.mibudget.ui.common.fieldShape()).background(cs.surfaceContainerHighest)
                .clickable(onClickLabel = if (file == null) "Attach a picture" else "View the receipt") {
                    if (file == null) picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) else viewing = true
                }
                .padding(start = 16.dp, end = if (file == null) 16.dp else 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (file == null) com.jlees.mibudget.ui.common.RowIcon(Sym.AddPhotoAlternate)
            else AsyncImage(model = file, contentDescription = "Receipt", contentScale = ContentScale.Crop, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)))
            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                Text("Receipt (optional)", color = cs.onSurfaceVariant, fontSize = 13.sp, lineHeight = 18.sp)
                Text(if (file == null) "Attach a receipt or screenshot" else "Receipt attached", color = cs.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            if (file == null) Icon(Sym.ChevronRight, null, Modifier.size(22.dp), tint = cs.onSurfaceVariant)
            else IconButton(onClick = onRemove) { Icon(Sym.Cancel, "Remove receipt", Modifier.size(20.dp), tint = cs.onSurfaceVariant) }
        }
        if (viewing && file != null) ReceiptViewer(file) { viewing = false }
        return
    }
    Column(modifier.fillMaxWidth()) {
        Text("Receipt (optional)", Modifier.padding(bottom = 5.dp), color = cs.onSurfaceVariant, fontSize = 14.sp)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(com.jlees.mibudget.ui.common.fieldShape()).background(cs.surfaceContainerHighest)
                .clickable(onClickLabel = if (file == null) "Attach a picture" else "View the receipt") {
                    if (file == null) picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) else viewing = true
                }
                .padding(start = if (file == null) 16.dp else 8.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (file == null) {
                Icon(Sym.AddPhotoAlternate, null, Modifier.size(24.dp), tint = cs.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Text("Attach a receipt or screenshot", Modifier.weight(1f), color = cs.onSurfaceVariant, fontSize = 16.sp)
                Icon(Sym.ChevronRight, null, Modifier.padding(end = 12.dp).size(20.dp), tint = cs.onSurfaceVariant)
            } else {
                AsyncImage(
                    model = file, contentDescription = "Receipt", contentScale = ContentScale.Crop,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)),
                )
                Spacer(Modifier.width(12.dp))
                Text("Receipt attached", Modifier.weight(1f), color = cs.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                IconButton(onClick = onRemove) { Icon(Sym.Cancel, "Remove receipt", Modifier.size(20.dp), tint = cs.onSurfaceVariant) }
            }
        }
    }
    if (viewing && file != null) ReceiptViewer(file) { viewing = false }
}

/** Read-only receipt: a wide thumbnail that opens full screen. */
@Composable
fun ReceiptPreview(file: File, modifier: Modifier = Modifier) {
    var viewing by remember { mutableStateOf(false) }
    AsyncImage(
        model = file,
        contentDescription = "Receipt — tap to view",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(com.jlees.mibudget.ui.common.fieldShape())
            .clickable { viewing = true },
    )
    if (viewing) ReceiptViewer(file) { viewing = false }
}

@Composable
private fun ReceiptViewer(file: File, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AsyncImage(model = file, contentDescription = "Receipt", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            FilledTonalIconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(12.dp),
            ) { Icon(Sym.Close, "Close") }
        }
    }
}
