package com.deepak.flow.core.model

object SnoozeSettings {
    const val DEFAULT_ENABLED = false
    const val DEFAULT_INTERVAL_MINUTES = 10
    const val MIN_INTERVAL_MINUTES = 2
    const val MAX_INTERVAL_MINUTES = 60
    const val STEP_MINUTES = 5

    fun coerceIntervalMinutes(minutes: Int): Int {
        val allowed = allowedIntervals()
        return allowed.minBy { kotlin.math.abs(it - minutes) }
    }

    fun nextInterval(minutes: Int): Int {
        val current = coerceIntervalMinutes(minutes)
        return allowedIntervals().firstOrNull { it > current } ?: current
    }

    fun previousInterval(minutes: Int): Int {
        val current = coerceIntervalMinutes(minutes)
        return allowedIntervals().lastOrNull { it < current } ?: current
    }

    private fun allowedIntervals(): List<Int> =
        listOf(MIN_INTERVAL_MINUTES) + (STEP_MINUTES..MAX_INTERVAL_MINUTES step STEP_MINUTES)
}
