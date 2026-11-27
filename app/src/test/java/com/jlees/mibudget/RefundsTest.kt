package com.jlees.mibudget

import com.jlees.mibudget.data.db.PurchaseEntity
import com.jlees.mibudget.data.db.fullyRefunded
import com.jlees.mibudget.data.db.spentCents
import com.jlees.mibudget.domain.Refunds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RefundsTest {
    private val day = LocalDate.of(2026, 11, 2)
    private val p = PurchaseEntity(merchant = "Target", amountCents = 4200, date = day)

    @Test fun fullRefundCountsNothing() {
        val r = Refunds.apply(p, p.amountCents, day)
        assertEquals(0, r.spentCents)
        assertTrue(r.fullyRefunded)
        assertEquals(day, r.refundedOn)
    }

    @Test fun partialRefundCountsTheRest() {
        val r = Refunds.apply(p, 1200, day)
        assertEquals(3000, r.spentCents)
        assertFalse(r.fullyRefunded)
    }

    @Test fun refundIsClampedAndCanBeTakenOff() {
        assertEquals(4200, Refunds.apply(p, 99_999, day).refundedCents)
        val off = Refunds.apply(Refunds.apply(p, 1000, day), 0, day)
        assertEquals(4200, off.spentCents)
        assertNull(off.refundedOn)
    }

    @Test fun moneyInHasNothingToRefund() {
        val income = p.copy(amountCents = -500)
        assertEquals(0, Refunds.apply(income, 500, day).refundedCents)
    }
}
