package com.jlees.mibudget

import com.jlees.mibudget.ui.common.BarPage
import com.jlees.mibudget.ui.common.MainBarRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MainBarRulesTest {
    @Test fun aPagesButtonSwipeAndSideAgree() {
        assertEquals(BarPage.LEFT, MainBarRules.page(40f, 5f))   // swipe right: the left button's page, from the left
        assertEquals(BarPage.RIGHT, MainBarRules.page(-40f, 5f)) // swipe left: the right button's page
        assertEquals(BarPage.UP, MainBarRules.page(3f, -40f))   // swipe up: the Pill button's page
        assertNull(MainBarRules.page(0f, 40f))                  // down opens nothing
    }

    @Test fun progressFollowsTheFinger() {
        assertEquals(0.5f, MainBarRules.progress(BarPage.LEFT, 200f, 0f, 400f, 800f))
        assertEquals(0.25f, MainBarRules.progress(BarPage.RIGHT, -100f, 0f, 400f, 800f))
        // up moves the page with the finger across the screen's height, not faster
        assertEquals(0.25f, MainBarRules.progress(BarPage.UP, 0f, -200f, 400f, 800f))
        assertEquals(1f, MainBarRules.progress(BarPage.UP, 0f, -900f, 400f, 800f))
        assertEquals(0f, MainBarRules.progress(BarPage.LEFT, -50f, 0f, 400f, 800f))
    }

    @Test fun opensPastTwentyPercentOrOnAFlick() {
        assertTrue(MainBarRules.opens(0.21f, 0f, 2.625f))
        assertFalse(MainBarRules.opens(0.1f, 100f, 2.625f))
        assertTrue(MainBarRules.opens(0.05f, 300f, 2.625f)) // 300dp/s ≈ 790px/s
        assertEquals(-500f, MainBarRules.speedToward(BarPage.RIGHT, 500f, 0f))
    }
}
