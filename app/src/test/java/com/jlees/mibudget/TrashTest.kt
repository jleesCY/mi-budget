package com.jlees.mibudget

import com.jlees.mibudget.domain.Trash
import org.junit.Assert.assertEquals
import org.junit.Test

class TrashTest {
    private val day = 86_400_000L

    @Test fun daysLeftFollowTheSetting() {
        assertEquals(30, Trash.daysLeft(deletedAt = 0, now = 0, keepDays = 30))
        assertEquals(23, Trash.daysLeft(deletedAt = 0, now = 7 * day, keepDays = 30))
        assertEquals(0, Trash.daysLeft(deletedAt = 0, now = 40 * day, keepDays = 30))
        assertEquals(6, Trash.daysLeft(deletedAt = 0, now = day, keepDays = 7))
    }

    @Test fun purgesWhatsOlderThanTheKeepPeriod() {
        assertEquals(10 * day, Trash.purgeBefore(now = 40 * day, keepDays = 30))
    }
}
