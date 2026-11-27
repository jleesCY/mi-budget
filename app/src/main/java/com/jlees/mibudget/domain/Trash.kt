package com.jlees.mibudget.domain

/** Recently Deleted's rules. Pure, unit-tested. */
object Trash {
    private const val DAY = 86_400_000L

    /** Whole days left before something deleted at [deletedAt] is gone for good (never below 0). */
    fun daysLeft(deletedAt: Long, now: Long, keepDays: Long): Long = (keepDays - (now - deletedAt) / DAY).coerceAtLeast(0)

    /** Things deleted before this moment are purged. */
    fun purgeBefore(now: Long, keepDays: Long): Long = now - keepDays * DAY
}
