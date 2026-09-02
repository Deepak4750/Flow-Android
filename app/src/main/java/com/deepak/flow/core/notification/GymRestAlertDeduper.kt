package com.deepak.flow.core.notification

import java.util.concurrent.ConcurrentHashMap

/**
 * One rest-complete alert per rest period (workoutId + restEndsAt).
 * Skip suppresses the period so a racing alarm cannot vibrate.
 */
internal object GymRestAlertDeduper {
    private val claimed = ConcurrentHashMap<String, Boolean>()

    fun tryClaim(workoutId: Long, restEndsAtEpochMilli: Long): Boolean {
        val previous = claimed.putIfAbsent(key(workoutId, restEndsAtEpochMilli), true)
        return previous == null
    }

    fun suppress(workoutId: Long, restEndsAtEpochMilli: Long) {
        claimed[key(workoutId, restEndsAtEpochMilli)] = true
    }

    fun resetForTests() {
        claimed.clear()
    }

    private fun key(workoutId: Long, restEndsAtEpochMilli: Long): String =
        "$workoutId:$restEndsAtEpochMilli"
}
