package com.deepak.flow.core.gym

data class GymSetMeasurements(
    val weight: Double? = null,
    val weightUnit: WeightUnit? = null,
    val reps: Int? = null,
    val durationSeconds: Int? = null,
    val distance: Double? = null,
    val speed: Double? = null,
    val incline: Double? = null,
    val resistance: Double? = null,
    val rounds: Int? = null,
)

data class GymWorkoutSet(
    val id: Long = 0L,
    val workoutExerciseId: Long = 0L,
    val setNumber: Int,
    val measurements: GymSetMeasurements = GymSetMeasurements(),
    val failure: Boolean = false,
    val saved: Boolean = false,
)

data class GymWorkoutExercise(
    val id: Long = 0L,
    val workoutId: Long = 0L,
    val name: String,
    val sortOrder: Int,
    val note: String = "",
    val trackingFields: Set<TrackingField> = emptySet(),
    val sets: List<GymWorkoutSet> = emptyList(),
    /** Planned sets from the routine. 0 means unbounded (Free Workout). */
    val plannedSetCount: Int = 0,
    val skipped: Boolean = false,
    val routineExerciseId: Long? = null,
    val exerciseStableKey: String? = null,
)

/**
 * Reusable workout session. Free and future Routine workouts share this shape.
 */
data class GymWorkoutSession(
    val id: Long = 0L,
    val type: GymWorkoutType = GymWorkoutType.FREE,
    val status: GymWorkoutStatus = GymWorkoutStatus.ACTIVE,
    val startedAtEpochMilli: Long,
    val endedAtEpochMilli: Long? = null,
    val completed: Boolean = false,
    val weightUnit: WeightUnit = WeightUnit.KG,
    val restEndsAtEpochMilli: Long? = null,
    val restDurationSeconds: Int = GymLimits.SET_REST_DEFAULT_SECONDS,
    val currentExerciseIndex: Int = 0,
    /** When the current exercise became current. Null until the first exercise exists. */
    val currentExerciseStartedAtEpochMilli: Long? = null,
    val starred: Boolean = false,
    val title: String = "",
    val exercises: List<GymWorkoutExercise> = emptyList(),
    val routineId: Long? = null,
    val dayIndex: Int? = null,
    val restKind: GymRestKind = GymRestKind.NONE,
)

data class GymWorkoutSummary(
    val durationSeconds: Long,
    val exerciseCount: Int,
    val setCount: Int,
    val volumeKg: Double?,
)

