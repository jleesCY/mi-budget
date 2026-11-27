package com.jlees.mibudget.ui.components

import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/*
 * Read-only views of a purchase or subscription. Tapping an item opens one of these first, so
 * nothing can be changed by accident; the Edit button in the top bar switches to the editor.
 */

/**
 * The top of one thing's page (as mi.'s detail pages): its picture, the name (17sp bold, 2 lines), the main number
 * (30sp extrabold), a caption (13sp semibold) and an optional status badge (its colour at 16% behind 13sp bold text).
 */
@Composable
fun PreviewHeader(
    avatar: @Composable () -> Unit,
    title: String,
    amount: String,
    amountColor: Color = MaterialTheme.colorScheme.onSurface,
    caption: String? = null,
    chip: String? = null,
    chipColor: Color = MaterialTheme.colorScheme.primary,
) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        avatar()
        Spacer(Modifier.height(12.dp))
        Text(title, color = cs.onSurface, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Text(amount, color = amountColor, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        if (caption != null) Text(caption, color = cs.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        if (chip != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                chip, Modifier.clip(CircleShape).background(chipColor.copy(alpha = 0.16f)).padding(horizontal = 10.dp, vertical = 4.dp),
                color = chipColor, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** One labelled value, as a Settings row: a 40dp secondaryContainer circle (or [leading]), the label (13sp) over the value (16sp semibold). */
@Composable
fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    leading: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clip(com.jlees.mibudget.ui.common.fieldShape())
            .background(cs.surfaceContainerHighest)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(cs.secondaryContainer), contentAlignment = Alignment.Center) {
            if (leading != null) leading() else Icon(icon, null, Modifier.size(22.dp), tint = cs.onSecondaryContainer)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = cs.onSurfaceVariant, fontSize = 13.sp, lineHeight = 18.sp)
            Text(value, color = cs.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        if (onClick != null) Icon(com.jlees.mibudget.ui.common.Sym.ChevronRight, null, Modifier.size(22.dp), tint = cs.onSurfaceVariant)
    }
}
