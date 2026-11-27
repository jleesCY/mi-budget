package com.jlees.mibudget.ui.common

import kotlin.math.abs

/**
 * When a drag on the Pill acts: mi. Browser's numbers. Distances in dp, speeds in dp/ms. Pure,
 * so they're unit-tested.
 */
object PillRules {
    /** Sideways (a month back or forward): 28dp, or 8dp with a 0.25dp/ms flick the same way. +1 right, -1 left, 0 none. */
    fun side(dx: Float, vx: Float): Int = when {
        abs(dx) >= 28f -> if (dx > 0) 1 else -1
        abs(dx) >= 8f && abs(vx) >= 0.25f && dx * vx > 0 -> if (dx > 0) 1 else -1
        else -> 0
    }

    /** Up (the Library) arms at 12% of 220dp. */
    fun upArmed(dy: Float): Boolean = -dy >= 220f * 0.12f

    /** Up acts when armed, or on a 0.2dp/ms upward flick. */
    fun up(dy: Float, vy: Float): Boolean = upArmed(dy) || (dy < 0 && -vy >= 0.2f)
}
