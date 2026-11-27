package com.jlees.mibudget.domain

import com.jlees.mibudget.data.db.PurchaseEntity
import java.time.LocalDate

/** Refunding a purchase: it stays in history, and only what wasn't refunded counts as spent. */
object Refunds {
    /**
     * [p] with [cents] of it refunded on [on]. The refund is kept between nothing and the whole amount; 0 takes the
     * refund off. Money that came in (a negative amount) has nothing to refund.
     */
    fun apply(p: PurchaseEntity, cents: Long, on: LocalDate): PurchaseEntity {
        val refunded = if (p.amountCents <= 0) 0L else cents.coerceIn(0L, p.amountCents)
        return p.copy(refundedCents = refunded, refundedOn = if (refunded == 0L) null else on)
    }
}
