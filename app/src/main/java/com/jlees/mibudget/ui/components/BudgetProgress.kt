package com.jlees.mibudget.ui.components

import com.jlees.mibudget.ui.theme.AppColors
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jlees.mibudget.domain.BudgetStatus
import com.jlees.mibudget.domain.Money

/**
 * Bar colour for budget health: on track → category colour, ahead of pace → amber, over → error.
 * For bars and dots only — use [budgetTextColor] for words and numbers.
 */
@Composable
fun budgetColor(status: BudgetStatus): Color = when {
    status.over -> MaterialTheme.colorScheme.error
    status.aheadOfPace -> AppColors.warningGraphic
    else -> Color(status.category.color)
}

/** Readable text colour for budget health (category colours can be too light to read). */
@Composable
fun budgetTextColor(status: BudgetStatus): Color = when {
    status.over -> MaterialTheme.colorScheme.error
    status.aheadOfPace -> AppColors.warning
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

/** Budget health in words, so it doesn't rely on colour alone. */
fun budgetStatusWords(status: BudgetStatus): String = when {
    status.over -> "${Money.format(-status.remaining)} over"
    status.aheadOfPace -> "${Money.format(status.remaining)} left · ahead of pace"
    else -> "${Money.format(status.remaining)} left"
}

/**
 * mi.'s progress bar: flat, 4dp tall with 2dp corners, [color] filling [fraction] of a surfaceVariant track. Drawn as
 * it is, with no animation: a budget's progress is a reading, not something happening.
 */
@Composable
fun ProgressBar(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    val f = fraction.coerceIn(0f, 1f)
    Box(modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
        if (f > 0f) Box(Modifier.fillMaxWidth(f).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(color))
    }
}

/** A budget's progress bar, with "spent of budget" and how it's going under it. */
@Composable
fun BudgetProgress(status: BudgetStatus, modifier: Modifier = Modifier, showLabels: Boolean = true) {
    val color = budgetColor(status)
    Column(modifier) {
        ProgressBar(status.fraction, color)
        if (showLabels) {
            Spacer(Modifier.height(6.dp))
            Row {
                Text(
                    "${Money.format(status.spentCents)} of ${Money.format(status.budgetCents)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    budgetStatusWords(status),
                    style = MaterialTheme.typography.bodySmall,
                    color = budgetTextColor(status),
                    maxLines = 1,
                )
            }
        }
    }
}
