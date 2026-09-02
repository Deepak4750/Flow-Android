package com.deepak.flow.core.gym

/**
 * Detects whether an in-progress set editor draft contains user-entered workout data.
 */
object GymSetDraftPolicy {
    fun hasUserEnteredData(
        trackingFields: Set<TrackingField>,
        weight: String = "",
        reps: String = "",
        durationMinutes: String = "",
        durationSeconds: String = "",
        distance: String = "",
        speed: String = "",
        incline: String = "",
        resistance: String = "",
        rounds: String = "",
    ): Boolean {
        if (trackingFields.isEmpty()) return false
        return trackingFields.any { field ->
            when (field) {
                TrackingField.WEIGHT -> weight.isNotBlank()
                TrackingField.REPS -> reps.isNotBlank()
                TrackingField.DURATION ->
                    durationMinutes.isNotBlank() || durationSeconds.isNotBlank()
                TrackingField.DISTANCE -> distance.isNotBlank()
                TrackingField.SPEED -> speed.isNotBlank()
                TrackingField.INCLINE -> incline.isNotBlank()
                TrackingField.RESISTANCE -> resistance.isNotBlank()
                TrackingField.ROUNDS -> rounds.isNotBlank()
            }
        }
    }
}
