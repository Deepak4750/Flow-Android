package com.deepak.flow.core.model

object SnoozeSettings {
    const val DEFAULT_ENABLED = false
    const val DEFAULT_INTERVAL_MINUTES = 10
    const val MIN_INTERVAL_MINUTES = 5
    const val MAX_INTERVAL_MINUTES = 60
    const val STEP_MINUTES = 5

    fun coerceIntervalMinutes(minutes: Int): Int =
        minutes.coerceIn(MIN_INTERVAL_MINUTES, MAX_INTERVAL_MINUTES)
}
