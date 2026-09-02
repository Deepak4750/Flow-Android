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

enum class GymRestKind {
    NONE,
    SET,
    EXERCISE,
}

object GymLimits {
    const val NOTE_MAX_CHARS = 200
    const val SET_REST_MIN_SECONDS = 10
    const val SET_REST_MAX_SECONDS = 300
    const val SET_REST_DEFAULT_SECONDS = 90
    const val EXERCISE_REST_MIN_SECONDS = 10
    const val EXERCISE_REST_MAX_SECONDS = 150
    const val EXERCISE_REST_DEFAULT_SECONDS = 120
    const val SET_COUNT_MIN = 1
    const val SET_COUNT_MAX = 20
    const val SET_COUNT_DEFAULT = 3
    const val DAY_COUNT_MIN = 1
    const val DAY_COUNT_MAX = 10
    const val ROUND_FOUR_CHECKPOINT = 4

    fun clampSetCount(count: Int): Int = count.coerceIn(SET_COUNT_MIN, SET_COUNT_MAX)

    fun clampDayCount(count: Int): Int = count.coerceIn(DAY_COUNT_MIN, DAY_COUNT_MAX)

    fun defaultDayName(dayIndex: Int): String = "Day ${dayIndex + 1}"

    fun clampNote(raw: String): String = raw.trim().take(NOTE_MAX_CHARS)

    fun clampSetRestSeconds(seconds: Int): Int =
        seconds.coerceIn(SET_REST_MIN_SECONDS, SET_REST_MAX_SECONDS)

    fun clampExerciseRestSeconds(seconds: Int): Int =
        seconds.coerceIn(EXERCISE_REST_MIN_SECONDS, EXERCISE_REST_MAX_SECONDS)
}

data class GymRoutine(
    val id: Long = 0L,
    val name: String,
    val currentDayIndex: Int = 0,
    val roundsCompleted: Int = 0,
    val roundFourCheckpointDismissed: Boolean = false,
    val starred: Boolean = false,
    val starredAtEpochMilli: Long? = null,
    val updatedAtEpochMilli: Long = 0L,
    val days: List<GymRoutineDay> = emptyList(),
) {
    fun currentDay(): GymRoutineDay? =
        days.getOrNull(currentDayIndex.coerceIn(0, days.lastIndex.coerceAtLeast(0)))

    fun nextWorkoutDayAfter(fromDayIndex: Int): GymRoutineDay? {
        if (days.isEmpty()) return null
        var index = fromDayIndex
        repeat(days.size) {
            index = (index + 1).mod(days.size)
            val day = days[index]
            if (!day.isRestDay && day.exercises.isNotEmpty()) return day
        }
        return null
    }
}

data class GymRoutineDay(
    val id: Long = 0L,
    val routineId: Long = 0L,
    val dayIndex: Int,
    /** User-editable title only. Day number comes from [dayIndex]. */
    val name: String,
    val isRestDay: Boolean = false,
    val exercises: List<GymRoutineExercise> = emptyList(),
    /** Stable in-memory key for builder UI (not persisted). */
    val localKey: String = "",
) {
    fun stableLocalKey(): String = when {
        localKey.isNotBlank() -> localKey
        id > 0L -> "day-$id"
        else -> ""
    }
}

data class GymRoutineExercise(
    val id: Long = 0L,
    val dayId: Long = 0L,
    val stableKey: String = "",
    val exerciseId: String = "",
    val name: String,
    val trackingFields: Set<TrackingField> = setOf(TrackingField.WEIGHT, TrackingField.REPS),
    val sortOrder: Int = 0,
    val setCount: Int = GymLimits.SET_COUNT_DEFAULT,
    val note: String = "",
)

