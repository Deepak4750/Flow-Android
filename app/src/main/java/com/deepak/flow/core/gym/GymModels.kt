package com.deepak.flow.core.gym

enum class TrackingField(val label: String) {
    WEIGHT("Weight"),
    REPS("Reps"),
    DURATION("Duration"),
    DISTANCE("Distance"),
    SPEED("Speed"),
    INCLINE("Incline"),
    RESISTANCE("Resistance"),
    ROUNDS("Rounds"),
}

enum class WeightUnit(val label: String) {
    KG("KG"),
    LB("LB"),
}

enum class GymWorkoutType {
    FREE,
    ROUTINE,
}

enum class GymWorkoutStatus {
    ACTIVE,
    COMPLETED,
    DISCARDED,
}

object GymLimits {
    const val NOTE_MAX_CHARS = 200
    const val SET_REST_MIN_SECONDS = 10
    const val SET_REST_MAX_SECONDS = 120
    const val SET_REST_DEFAULT_SECONDS = 90
    const val EXERCISE_REST_MIN_SECONDS = 10
    const val EXERCISE_REST_MAX_SECONDS = 150
    const val EXERCISE_REST_DEFAULT_SECONDS = 120

    fun clampNote(raw: String): String = raw.trim().take(NOTE_MAX_CHARS)

    fun clampSetRestSeconds(seconds: Int): Int =
        seconds.coerceIn(SET_REST_MIN_SECONDS, SET_REST_MAX_SECONDS)

    fun clampExerciseRestSeconds(seconds: Int): Int =
        seconds.coerceIn(EXERCISE_REST_MIN_SECONDS, EXERCISE_REST_MAX_SECONDS)
}
