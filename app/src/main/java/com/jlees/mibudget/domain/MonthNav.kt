package com.jlees.mibudget.domain

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** Swiping the Pill sideways steps the start screen a month back or forward. Pure, unit-tested. */
object MonthNav {
    /** The whole month a date filter shows, or null when it isn't one month (a week, a year, all time…). */
    fun monthOf(f: PurchaseFilter, today: LocalDate): YearMonth? = when (f.datePreset) {
        DatePreset.THIS_MONTH -> YearMonth.from(today)
        DatePreset.LAST_MONTH -> YearMonth.from(today).minusMonths(1)
        DatePreset.CUSTOM -> f.customRange?.let { r ->
            val m = YearMonth.from(r.start)
            m.takeIf { r.start == m.atDay(1) && r.end == m.atEndOfMonth() }
        }
        else -> null
    }

    /**
     * [f] moved [dir] months (-1 back, +1 forward). Not one month yet: back goes to last month, forward to this one.
     * Null when it would go past this month (nothing to show in the future).
     */
    fun step(f: PurchaseFilter, today: LocalDate, dir: Int): PurchaseFilter? {
        val now = YearMonth.from(today)
        val target = monthOf(f, today)?.plusMonths(dir.toLong()) ?: if (dir < 0) now.minusMonths(1) else now
        if (target.isAfter(now)) return null
        return when (target) {
            now -> f.copy(datePreset = DatePreset.THIS_MONTH, customRange = null)
            now.minusMonths(1) -> f.copy(datePreset = DatePreset.LAST_MONTH, customRange = null)
            else -> f.copy(datePreset = DatePreset.CUSTOM, customRange = DateRange(target.atDay(1), target.atEndOfMonth()))
        }
    }

    /** The start screen's title: the month's name ("November", "March 2025" in another year), else the range's name. */
    fun title(f: PurchaseFilter, today: LocalDate, locale: Locale = Locale.getDefault()): String {
        val m = monthOf(f, today) ?: return when (f.datePreset) {
            DatePreset.CUSTOM -> "Custom Range"
            else -> f.datePreset.label.split(' ').joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
        }
        val name = m.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)
        return if (m.year == today.year) name else "$name ${m.year}"
    }
}
