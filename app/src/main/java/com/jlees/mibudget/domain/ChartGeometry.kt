package com.jlees.mibudget.domain

/** Pure geometry for the round charts (donut, pie), so tiny categories still draw as proper segments. Unit-tested. */
object ChartGeometry {
    /**
     * Each value's angle on a full circle (360°), in proportion, except that none is smaller than [minDeg]: what the
     * small ones gain comes out of the bigger ones, in proportion. When even that doesn't fit, all get equal angles.
     */
    fun sweeps(values: List<Long>, minDeg: Float): List<Float> {
        val full = 360f
        if (values.isEmpty()) return emptyList()
        if (minDeg * values.size >= full) return List(values.size) { full / values.size }
        val fixed = BooleanArray(values.size)
        while (true) {
            val free = full - minDeg * fixed.count { it }
            val rest = values.indices.filter { !fixed[it] }.sumOf { values[it] }.toDouble()
            // Every unfixed value at least minDeg? Then the proportional split of what's left holds.
            val newlyFixed = values.indices.filter { !fixed[it] && (rest <= 0.0 || values[it] / rest * free < minDeg) }
            if (newlyFixed.isEmpty()) {
                return values.indices.map { if (fixed[it]) minDeg else (values[it] / rest * free).toFloat() }
            }
            newlyFixed.forEach { fixed[it] = true }
        }
    }

    /**
     * Keeps the [max]-1 biggest of [values] (already biggest first) and adds up the rest into one, so a round chart
     * never has more segments than fit. Returns how many are kept as they are; the rest become one "Other".
     */
    fun keptCount(size: Int, max: Int): Int = if (size <= max) size else max - 1

    /**
     * Value labels for a line chart's grid: 0 and up to 3 round steps (1, 2 or 5 × a power of ten, in cents)
     * reaching at least [max].
     */
    fun gridSteps(max: Long): List<Long> {
        val count = 3
        if (max <= 0) return listOf(0)
        val raw = max.toDouble() / count
        val mag = Math.pow(10.0, Math.floor(Math.log10(raw)))
        val step = listOf(1.0, 2.0, 5.0, 10.0).map { it * mag }.first { it >= raw }.toLong().coerceAtLeast(1)
        return (0..count).map { it * step }.takeWhile { it <= max + step - 1 }
    }

    /** The day with the least and the most spent (days with nothing spent don't count as lowest). */
    fun <T> extremes(daily: List<Pair<T, Long>>): Pair<Pair<T, Long>, Pair<T, Long>>? {
        val spent = daily.filter { it.second > 0 }
        if (spent.isEmpty()) return null
        return spent.minBy { it.second } to spent.maxBy { it.second }
    }
}
