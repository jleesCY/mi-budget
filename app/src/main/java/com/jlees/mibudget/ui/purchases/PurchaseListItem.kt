package com.jlees.mibudget.ui.purchases

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.jlees.mibudget.data.db.CategoryEntity
import com.jlees.mibudget.data.db.PaymentMethodEntity
import com.jlees.mibudget.data.db.PurchaseEntity
import com.jlees.mibudget.data.db.PurchaseSource
import com.jlees.mibudget.data.db.fullyRefunded
import com.jlees.mibudget.data.db.isRefunded
import com.jlees.mibudget.data.db.spentCents
import com.jlees.mibudget.domain.Money
import com.jlees.mibudget.ui.common.Sym
import com.jlees.mibudget.ui.common.MiListRow
import com.jlees.mibudget.ui.components.ItemIcon
import com.jlees.mibudget.ui.theme.AppColors

/**
 * One purchase in a list, the same everywhere it appears (the start screen, a category, the calendar): a mi. list row.
 * Its 44dp leading tile (the merchant's logo or the category), then the merchant (16sp bold) over a details line (12sp):
 * the amount first, semibold in primary ("+" in the refund colour for a scanned return), then the category and payment method.
 * A refunded purchase stays in history with its amount crossed out and what still counts (or "Refunded") after it.
 */
@Composable
fun PurchaseListItem(
    p: PurchaseEntity,
    category: CategoryEntity?,
    method: PaymentMethodEntity?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    /** False inside a swipe row, which keeps the gap outside itself. */
    gap: Boolean = true,
) {
    val cs = MaterialTheme.colorScheme
    val refundInk = AppColors.refund
    MiListRow(
        title = p.merchant,
        // the amount first ("+" in the refund colour for a scanned return; crossed out with what still counts when refunded)
        lead = buildAnnotatedString {
            val amount = if (p.amountCents < 0) "+" + Money.format(-p.amountCents) else Money.format(p.amountCents)
            withStyle(SpanStyle(color = if (p.amountCents < 0) refundInk else cs.primary,
                textDecoration = if (p.isRefunded) TextDecoration.LineThrough else null)) { append(amount) }
            if (p.isRefunded) withStyle(SpanStyle(color = refundInk)) {
                append("  " + if (p.fullyRefunded) "Refunded" else Money.format(p.spentCents))
            }
        },
        details = buildAnnotatedString {
            withStyle(SpanStyle(color = if (category == null) cs.error else cs.onSurfaceVariant)) { append(category?.name ?: "Uncategorized") }
            if (method != null) append(" · " + method.name)
        },
        leading = {
            if (selected) Box(Modifier.size(44.dp).clip(CircleShape).background(cs.primary), contentAlignment = Alignment.Center) {
                Icon(Sym.Check, null, tint = cs.onPrimary)
            } else ItemIcon(p.brandKey, p.merchant, category)
        },
        modifier = modifier,
        onClick = onClick,
        onLongClick = onLongClick,
        onLongClickLabel = if (onLongClick != null) (if (selected) "Unselect" else "Select") else null,
        selected = selected,
        gap = gap,
        // payments of a subscription, and purchases with a receipt picture, carry a small mark
        marks = {
            if (p.source == PurchaseSource.SUBSCRIPTION || p.subscriptionId != null) {
                Icon(Sym.Autorenew, "Subscription", Modifier.size(12.dp), tint = cs.onSurfaceVariant); Spacer(Modifier.width(4.dp))
            }
            if (p.receiptFile != null) {
                Icon(Sym.Receipt, "Has receipt", Modifier.size(12.dp), tint = cs.onSurfaceVariant); Spacer(Modifier.width(4.dp))
            }
        },
    )
}
