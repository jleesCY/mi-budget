package com.jlees.mibudget.ui.common

/** How a swiped list row reveals its action (mi. Browser's numbers). [progress] is 0..1 of the row's width. Pure. */
object SwipeRules {
    /**
     * The action arms at 30% of the row's width (a little further than the browser's 25%: these rows sit in a long
     * list that's scrolled all the time, and a stray swipe shouldn't refund or delete anything).
     */
    const val ARM_AT = 0.3f

    /** A flick acts short of that: at least this fast (dp a second) the way it's going, and already 12% of the way. */
    const val FLICK_SPEED = 1000f
    const val FLICK_MIN = 0.12f

    /** A swipe only takes the gesture once it's this many times more sideways than up or down; anything else scrolls. */
    const val SIDEWAYS = 2f

    /** Letting go at [progress] (signed, of the row's width) at [speed] (dp a second, signed): acts? */
    fun acts(progress: Float, speed: Float): Boolean =
        kotlin.math.abs(progress) >= ARM_AT || (kotlin.math.abs(progress) >= FLICK_MIN && speed * progress > 0 && kotlin.math.abs(speed) >= FLICK_SPEED)

    /** The revealed colour fades in from 40% as you swipe, fully there once armed. */
    fun colorAlpha(progress: Float): Float = (0.4f + 0.6f * (progress / ARM_AT)).coerceIn(0.4f, 1f)

    /** Its icon grows from 40% to full size by 35% of the way, and to 125% just past the point where it acts. */
    fun iconScale(progress: Float): Float = when {
        progress >= ARM_AT + 0.02f -> 1.25f
        else -> (0.4f + 0.6f * (progress / 0.35f)).coerceIn(0.4f, 1f)
    }
}
