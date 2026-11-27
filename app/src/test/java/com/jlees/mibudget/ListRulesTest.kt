package com.jlees.mibudget

import com.jlees.mibudget.data.Spacing
import com.jlees.mibudget.ui.common.SwipeRules
import com.jlees.mibudget.ui.common.rowHeight
import com.jlees.mibudget.ui.common.rowMargin
import org.junit.Assert.assertEquals
import org.junit.Test

class ListRulesTest {
    @Test fun spacingSetsRowHeightAndGap() {
        assertEquals(48f, rowHeight(Spacing.COMPACT, 1f))
        assertEquals(58f, rowHeight(Spacing.NORMAL, 1f))
        assertEquals(72f, rowHeight(Spacing.AIRY, 1f))
        assertEquals(69.6f, rowHeight(Spacing.NORMAL, 1.2f), 0.01f) // grows with Font Size
        assertEquals(listOf(8f, 15f, 25f), Spacing.entries.map(::rowMargin))
    }

    @Test fun swipeReveal() {
        assertEquals(0.4f, SwipeRules.colorAlpha(0f))
        assertEquals(1f, SwipeRules.colorAlpha(0.3f))
        assertEquals(0.4f, SwipeRules.iconScale(0f))
        assertEquals(1f, SwipeRules.iconScale(0.24f).coerceAtMost(1f), 0.2f)
        assertEquals(1.25f, SwipeRules.iconScale(0.33f))
    }

    @Test fun aSwipeActsOnlyFarEnoughOrFlickedHard() {
        assertEquals(true, SwipeRules.acts(0.3f, 0f))
        assertEquals(true, SwipeRules.acts(-0.31f, 0f))
        assertEquals(false, SwipeRules.acts(0.25f, 0f)) // the browser's distance isn't enough here
        assertEquals(true, SwipeRules.acts(0.15f, 1200f))
        assertEquals(false, SwipeRules.acts(0.05f, 3000f)) // a flick that hardly moved the row
        assertEquals(false, SwipeRules.acts(0.15f, 500f)) // too slow
        assertEquals(false, SwipeRules.acts(0.15f, -1500f)) // flung back the other way
    }
}
