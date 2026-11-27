package com.jlees.mibudget

import com.jlees.mibudget.domain.ChartGeometry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartGeometryTest {
    private fun List<Float>.total() = sum()

    @Test fun proportionalWhenEverythingIsBigEnough() {
        val s = ChartGeometry.sweeps(listOf(300, 100), 20f)
        assertEquals(270f, s[0], 0.01f)
        assertEquals(90f, s[1], 0.01f)
    }

    @Test fun tinySlicesGetTheMinimumAndTheRestShrinks() {
        val s = ChartGeometry.sweeps(listOf(100_000, 50_000, 30, 1), 24f)
        assertTrue(s.all { it >= 24f - 0.01f })
        assertEquals(360f, s.total(), 0.01f)
        assertTrue(s[0] > s[1])
    }

    @Test fun tooManyForTheMinimumAreEqual() {
        val s = ChartGeometry.sweeps(List(20) { 1L + it }, 24f)
        assertTrue(s.all { kotlin.math.abs(it - 18f) < 0.01f })
    }

    @Test fun singleValueIsTheWholeCircle() {
        assertEquals(listOf(360f), ChartGeometry.sweeps(listOf(5), 24f))
    }

    @Test fun keepsRoomForOther() {
        assertEquals(8, ChartGeometry.keptCount(8, 10))
        assertEquals(9, ChartGeometry.keptCount(14, 10))
    }

    @Test fun gridStepsAreRound() {
        assertEquals(listOf(0L, 5000L, 10000L, 15000L), ChartGeometry.gridSteps(12_340))
        assertEquals(listOf(0L, 100L, 200L, 300L), ChartGeometry.gridSteps(250))
        assertEquals(listOf(0L), ChartGeometry.gridSteps(0))
    }

    @Test fun extremesSkipEmptyDays() {
        val days = listOf("a" to 0L, "b" to 500L, "c" to 120L, "d" to 900L)
        val (low, high) = ChartGeometry.extremes(days)!!
        assertEquals("c" to 120L, low)
        assertEquals("d" to 900L, high)
        assertEquals(null, ChartGeometry.extremes(listOf("a" to 0L)))
    }
}
