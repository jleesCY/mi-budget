// One-line text that doesn't fit can pan (Look & Feel › Pan Overflowed Text): a slow marquee that waits 1.5s, moves at
// 30dp/s and pauses 2s, instead of ending in "…". Each place that may pan provides LocalPan from its own switch.
package com.jlees.mibudget.ui.common

import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.basicMarquee
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.jlees.mibudget.data.AppSettings
import com.jlees.mibudget.data.PanArea

/** The settings, for pieces that read one directly (provided by MainActivity). */
val LocalSettings = staticCompositionLocalOf { AppSettings() }

/** Whether one-line text here pans when it doesn't fit (see [PanIn]). */
val LocalPan = compositionLocalOf { false }

/** [content] pans its overflowing one-line text if Pan Overflowed Text is on for [area]. */
@Composable
fun PanIn(area: PanArea, content: @Composable () -> Unit) {
    val s = LocalSettings.current
    CompositionLocalProvider(LocalPan provides (s.panLongText && area in s.panAreas), content = content)
}

/** One line of text: pans where that's on ([LocalPan]) and animations aren't removed, else ends in "…". */
@Composable
fun OneLine(
    text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified, fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null, style: TextStyle = LocalTextStyle.current,
) {
    val context = LocalContext.current
    // Android's "Remove animations": our own loops stop
    val motion = remember { android.provider.Settings.Global.getFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f }
    val panning = LocalPan.current && motion
    Text(
        text,
        if (panning) modifier.basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 1500, repeatDelayMillis = 2000,
            spacing = MarqueeSpacing.fractionOfContainer(1f / 4f), velocity = 30.dp) else modifier,
        color = color, fontSize = fontSize, fontWeight = fontWeight, style = style,
        maxLines = 1, softWrap = !panning, overflow = if (panning) TextOverflow.Clip else TextOverflow.Ellipsis,
    )
}
