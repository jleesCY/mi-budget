package com.jlees.mibudget.ui.common

import kotlin.math.abs

/** Where a page opened from the main bar comes in from: its left or right edge button's side, or up from the Pill. */
enum class BarPage { LEFT, RIGHT, UP }

/**
 * Swipes on the main bar's Pill open the pages its buttons open (mi. Browser's tab switcher bar): a swipe right brings
 * in the left edge button's page from the left, a swipe left the right one's from the right, and a swipe up the Pill
 * button's page from the bottom. The page follows the finger (a screen width sideways, a screen height up), ticks at 20%, and
 * opens past it or on a 700px/s flick toward it. Distances in dp, speeds in dp/s. Pure, unit-tested.
 */
object MainBarRules {
    const val OPEN_AT = 0.2f
    /** 700px/s, in dp/s for a screen of [density]. */
    fun flick(density: Float) = 700f / density

    /** Which page a drag is bringing in (by its main direction); null for a drag down. */
    fun page(dx: Float, dy: Float): BarPage? = when {
        abs(dx) >= abs(dy) -> if (dx > 0) BarPage.LEFT else if (dx < 0) BarPage.RIGHT else null
        dy < 0 -> BarPage.UP
        else -> null
    }

    /** How far (0..1) [page] has come in for a drag of ([dx], [dy]) on a screen [widthDp] × [heightDp]: 1:1 with the finger. */
    fun progress(page: BarPage, dx: Float, dy: Float, widthDp: Float, heightDp: Float): Float = when (page) {
        BarPage.LEFT -> dx / widthDp
        BarPage.RIGHT -> -dx / widthDp
        BarPage.UP -> -dy / heightDp
    }.coerceIn(0f, 1f)

    /** The speed (dp/s) toward [page] for a release velocity ([vx], [vy]) in dp/s. */
    fun speedToward(page: BarPage, vx: Float, vy: Float): Float = when (page) { BarPage.LEFT -> vx; BarPage.RIGHT -> -vx; BarPage.UP -> -vy }

    /** Let go: does the page open? Past [OPEN_AT], or a flick toward it — decided from where the finger let go. */
    fun opens(progress: Float, speedToward: Float, density: Float): Boolean = progress >= OPEN_AT || speedToward >= flick(density)
}
