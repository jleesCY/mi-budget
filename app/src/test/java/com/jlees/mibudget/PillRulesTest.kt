package com.jlees.mibudget

import com.jlees.mibudget.ui.common.PillRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PillRulesTest {
    @Test fun sidewaysNeedsDistanceOrAFlick() {
        assertEquals(1, PillRules.side(30f, 0f))
        assertEquals(-1, PillRules.side(-30f, 0f))
        assertEquals(0, PillRules.side(20f, 0.1f))
        assertEquals(1, PillRules.side(10f, 0.3f))
        assertEquals(0, PillRules.side(10f, -0.3f)) // a flick back the other way doesn't count
        assertEquals(0, PillRules.side(5f, 1f))
    }

    @Test fun upArmsAtTwelvePercentOf220() {
        assertFalse(PillRules.upArmed(-26f))
        assertTrue(PillRules.upArmed(-27f))
        assertTrue(PillRules.up(-5f, -0.25f))
        assertFalse(PillRules.up(5f, -0.25f))
    }
}
