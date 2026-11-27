package com.jlees.mibudget

import com.jlees.mibudget.domain.DatePreset
import com.jlees.mibudget.domain.DateRange
import com.jlees.mibudget.domain.MonthNav
import com.jlees.mibudget.domain.PurchaseFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class MonthNavTest {
    private val today = LocalDate.of(2026, 11, 23)
    private val f = PurchaseFilter()

    @Test fun backThroughTheMonths() {
        val oct = MonthNav.step(f, today, -1)!!
        assertEquals(DatePreset.LAST_MONTH, oct.datePreset)
        val sep = MonthNav.step(oct, today, -1)!!
        assertEquals(DateRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)), sep.customRange)
        assertEquals("September", MonthNav.title(sep, today, Locale.US))
        assertEquals(DatePreset.LAST_MONTH, MonthNav.step(sep, today, 1)!!.datePreset)
    }

    @Test fun neverIntoTheFuture() {
        assertNull(MonthNav.step(f, today, 1))
    }

    @Test fun otherRangesStartFromThisMonth() {
        val year = f.copy(datePreset = DatePreset.THIS_YEAR)
        assertEquals(DatePreset.THIS_MONTH, MonthNav.step(year, today, 1)!!.datePreset)
        assertEquals("This Year", MonthNav.title(year, today, Locale.US))
    }

    @Test fun otherYearsShowTheYear() {
        var x = f
        repeat(12) { x = MonthNav.step(x, today, -1)!! }
        assertEquals("November 2025", MonthNav.title(x, today, Locale.US))
    }
}
