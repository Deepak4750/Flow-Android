package com.deepak.flow.core.repository

import com.deepak.flow.core.gym.GymRoutine
import com.deepak.flow.core.gym.GymSetMeasurements
import com.deepak.flow.core.gym.GymWorkoutSession
import com.deepak.flow.core.gym.GymWorkoutSet
import com.deepak.flow.core.gym.GymWorkoutType
import com.deepak.flow.core.gym.TrackingField
import com.deepak.flow.core.gym.WeightUnit
import kotlinx.coroutines.flow.Flow

/**
 * Reusable workout-session API. Free Workout and Routine workouts both use this.
 */
interface GymWorkoutRepository {
    fun observeActiveSession(type: GymWorkoutType): Flow<GymWorkoutSession?>

    fun observeAnyActiveSession(): Flow<GymWorkoutSession?>

    fun observeSession(workoutId: Long): Flow<GymWorkoutSession?>

    /**
     * Completed workouts whose [GymWorkoutSession.endedAtEpochMilli] falls in
     * [[fromInclusive], [toExclusive]). Newest first.
     */
    fun observeCompletedSessionsBetween(
        fromInclusive: Long,
        toExclusive: Long,
    ): Flow<List<GymWorkoutSession>>

    suspend fun getActiveSession(type: GymWorkoutType): GymWorkoutSession?

    suspend fun getAnyActiveSession(): GymWorkoutSession?

    suspend fun getSession(workoutId: Long): GymWorkoutSession?

    suspend fun startFreeWorkout(weightUnit: WeightUnit = WeightUnit.KG): Long

    suspend fun ensureActiveFreeWorkout(weightUnit: WeightUnit = WeightUnit.KG): Long

    suspend fun ensureActiveRoutineWorkout(weightUnit: WeightUnit = WeightUnit.KG): Long?

    suspend fun setWeightUnit(workoutId: Long, unit: WeightUnit)

    suspend fun setCurrentExerciseIndex(workoutId: Long, index: Int)

    suspend fun addExercise(
        workoutId: Long,
        name: String,
        trackingFields: Set<TrackingField>,
        note: String = "",
        plannedSetCount: Int = 0,
        routineExerciseId: Long? = null,
        exerciseStableKey: String? = null,
    ): Long

    suspend fun updateExercise(
        exerciseId: Long,
        name: String,
        trackingFields: Set<TrackingField>,
        note: String,
    )

    suspend fun setExerciseSkipped(exerciseId: Long, skipped: Boolean)

    suspend fun deleteExercise(exerciseId: Long)

    suspend fun addSet(
        exerciseId: Long,
        measurements: GymSetMeasurements,
        failure: Boolean,
        saved: Boolean,
    ): Long

    suspend fun updateSet(
        setId: Long,
        measurements: GymSetMeasurements,
        failure: Boolean,
        saved: Boolean,
        setNumber: Int? = null,
    )

    suspend fun deleteSet(setId: Long)

    /**
     * Re-inserts a deleted set at [setNumber], renumbering siblings around it.
     * Used for short-lived Undo after removing a set mid-workout.
     */
    suspend fun restoreSet(
        exerciseId: Long,
        measurements: GymSetMeasurements,
        failure: Boolean,
        setNumber: Int,
    ): Long

    suspend fun startRest(
        workoutId: Long,
        durationSeconds: Int,
        kind: com.deepak.flow.core.gym.GymRestKind = com.deepak.flow.core.gym.GymRestKind.SET,
        nowEpochMilli: Long = System.currentTimeMillis(),
    )

    /**
     * Extends the active rest end time without changing the saved default duration.
     */
    suspend fun extendRest(
        workoutId: Long,
        extraSeconds: Int,
        nowEpochMilli: Long = System.currentTimeMillis(),
    )

    suspend fun clearRest(workoutId: Long)

    suspend fun completeWorkout(workoutId: Long, nowEpochMilli: Long = System.currentTimeMillis())

    suspend fun discardWorkout(workoutId: Long)

    suspend fun setWorkoutStarred(workoutId: Long, starred: Boolean)

    suspend fun setWorkoutTitle(workoutId: Long, title: String)

    fun displayWorkoutTitle(title: String): String =
        title.trim().ifEmpty { "Free Workout" }

    /**
     * Re-inserts a deleted exercise with its sets at [sortOrder].
     * Used for short-lived Undo after removing an exercise mid-workout.
     */
    suspend fun restoreExercise(
        workoutId: Long,
        sortOrder: Int,
        name: String,
        trackingFields: Set<TrackingField>,
        note: String,
        sets: List<GymWorkoutSet>,
        plannedSetCount: Int = 0,
        skipped: Boolean = false,
        routineExerciseId: Long? = null,
        exerciseStableKey: String? = null,
    ): Long

    fun observeRoutines(): Flow<List<GymRoutine>>

    suspend fun setActiveRoutine(routineId: Long)

    suspend fun skipRoutineDay(routineId: Long, nowEpochMilli: Long = System.currentTimeMillis())

    suspend fun confirmRestDay(routineId: Long, nowEpochMilli: Long = System.currentTimeMillis())

    suspend fun dismissRoundFourCheckpoint(routineId: Long)

    suspend fun resetAllRoundsCompleted()

    suspend fun getPrimaryRoutine(): GymRoutine?

    suspend fun getRoutine(routineId: Long): GymRoutine?

    suspend fun saveRoutine(routine: GymRoutine): Long

    suspend fun deleteRoutine(routineId: Long)

    suspend fun setRoutineStarred(routineId: Long, starred: Boolean)

    /**
     * Last completed set of each exercise from the previous occurrence of this
     * routine day, keyed by this session's exercise id.
     */
    suspend fun previousOccurrenceSeeds(session: GymWorkoutSession): Map<Long, GymSetMeasurements>

    fun observePrimaryRoutine(): Flow<GymRoutine?>
}
