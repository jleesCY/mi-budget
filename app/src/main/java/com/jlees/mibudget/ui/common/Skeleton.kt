// Skeletons: the shape of a page drawn while its data loads (or while the page is still sliding in), so a page is never
// blank and then suddenly full. Grey blocks in surfaceContainerHighest, laid out as the real rows and cards, breathing
// gently together (one shared pulse per skeleton, read only while drawing, so it costs no recomposition).
package com.jlees.mibudget.ui.common

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jlees.mibudget.ui.theme.LocalMi

private val LocalPulse = staticCompositionLocalOf<State<Float>> { mutableFloatStateOf(1f) }

/** A skeleton's blocks, breathing together (0.55 … 1 over 900ms each way). */
@Composable
fun Skeleton(content: @Composable () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "skeleton").animateFloat(
        0.55f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "pulse",
    )
    CompositionLocalProvider(LocalPulse provides pulse, content = content)
}

/** One grey block of a skeleton. */
@Composable
fun Bone(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(6.dp)) {
    val pulse = LocalPulse.current
    val cs = MaterialTheme.colorScheme
    Box(modifier.graphicsLayer { alpha = pulse.value }.clip(shape).background(cs.surfaceContainerHighest))
}

/** A list row's skeleton, as MiListRow (List Style and Spacing): the 44dp tile, the title over the details line. */
@Composable
fun BoneRow(titleWidth: Dp = 140.dp, detailsWidth: Dp = 200.dp) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = listGap()).padding(horizontal = listSides()).heightIn(min = listRowHeight()).padding(horizontal = 20.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Bone(Modifier.size(44.dp), CircleShape)
        Spacer(Modifier.width(15.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Bone(Modifier.width(titleWidth).height(14.dp))
            Bone(Modifier.width(detailsWidth).height(10.dp))
        }
    }
}

/** [count] list rows' skeletons, their widths varied a little so it reads as a list. */
@Composable
fun BoneRows(count: Int = 6) {
    Skeleton {
        Column {
            repeat(count) { i ->
                BoneRow(titleWidth = listOf(140, 110, 170, 125)[i % 4].dp, detailsWidth = listOf(200, 160, 220, 180)[i % 4].dp)
            }
        }
    }
}

/** A heading's skeleton (a section title over a list or a block). */
@Composable
fun BoneHeading() {
    Bone(Modifier.padding(start = 28.dp, top = 18.dp, bottom = 12.dp).width(110.dp).height(16.dp))
}

/** The Subscriptions page's skeleton: its totals card, the coming-up cards, the tabs and the list. */
@Composable
fun SubscriptionsSkeleton() {
    Skeleton {
        Column {
            Bone(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(132.dp), MaterialTheme.shapes.extraLarge)
            BoneHeading()
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(3) { Bone(Modifier.width(160.dp).height(150.dp), MaterialTheme.shapes.extraLarge) }
            }
            Row(Modifier.padding(horizontal = 16.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(84, 84, 104, 56).forEach { Bone(Modifier.width(it.dp).height(36.dp), CircleShape) }
            }
            repeat(5) { i -> BoneRow(titleWidth = listOf(120, 150, 100, 140, 130)[i].dp, detailsWidth = listOf(190, 160, 210, 170, 200)[i].dp) }
        }
    }
}

/** The start screen's skeleton under its month: the total, the chart, its tags, and the first rows. */
@Composable
fun HomeSkeleton() {
    Skeleton {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            Bone(Modifier.padding(top = 8.dp).width(200.dp).height(44.dp), RoundedCornerShape(10.dp))
            Bone(Modifier.padding(top = 12.dp).width(170.dp).height(14.dp))
            Bone(Modifier.padding(top = 10.dp).width(240.dp).height(12.dp))
            // the donut
            Box(Modifier.padding(vertical = 28.dp).size(220.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Bone(Modifier.size(220.dp), CircleShape)
                Box(Modifier.size(150.dp).clip(CircleShape).background(with(LocalMi.current) { MaterialTheme.colorScheme.homeBackground }))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(110, 100).forEach { Bone(Modifier.width(it.dp).height(32.dp), CircleShape) } }
            Row(Modifier.padding(top = 8.dp, bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(120, 90).forEach { Bone(Modifier.width(it.dp).height(32.dp), CircleShape) } }
            repeat(4) { i -> BoneRow(titleWidth = listOf(130, 100, 150, 120)[i].dp) }
        }
    }
}

/** One item's page (a purchase or a subscription) while it loads: its picture, name and amount, and a block of rows. */
@Composable
fun DetailSkeleton() {
    Skeleton {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            Bone(Modifier.padding(top = 8.dp).size(72.dp), CircleShape)
            Bone(Modifier.padding(top = 14.dp).width(120.dp).height(16.dp))
            Bone(Modifier.padding(top = 10.dp).width(150.dp).height(30.dp), RoundedCornerShape(8.dp))
            Bone(Modifier.padding(top = 10.dp, bottom = 20.dp).width(180.dp).height(12.dp))
            val r = maxOf(LocalMi.current.radius, 4).dp
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(r)), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(3) { Bone(Modifier.fillMaxWidth().height(64.dp), RoundedCornerShape(4.dp)) }
            }
        }
    }
}
