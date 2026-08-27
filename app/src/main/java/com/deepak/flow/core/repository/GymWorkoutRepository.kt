package com.deepak.flow.core.repository

import com.deepak.flow.core.gym.GymSetMeasurements
import com.deepak.flow.core.gym.GymWorkoutSession
import com.deepak.flow.core.gym.GymWorkoutType
import com.deepak.flow.core.gym.TrackingField
import com.deepak.flow.core.gym.WeightUnit
import kotlinx.coroutines.flow.Flow

/**
 * Reusable workout-session API. Free Workout and future Routine workouts both use this.
 */
interface GymWorkoutRepository {
    fun observeActiveSession(type: GymWorkoutType): Flow<GymWorkoutSession?>

    fun observeSession(workoutId: Long): Flow<GymWorkoutSession?>

    suspend fun getActiveSession(type: GymWorkoutType): GymWorkoutSession?

    suspend fun getSession(workoutId: Long): GymWorkoutSession?

    suspend fun startFreeWorkout(weightUnit: WeightUnit = WeightUnit.KG): Long

    suspend fun ensureActiveFreeWorkout(weightUnit: WeightUnit = WeightUnit.KG): Long

    suspend fun setWeightUnit(workoutId: Long, unit: WeightUnit)

    suspend fun setCurrentExerciseIndex(workoutId: Long, index: Int)

    suspend fun addExercise(
        workoutId: Long,
        name: String,
        trackingFields: Set<TrackingField>,
        note: String = "",
    ): Long

    suspend fun updateExercise(
        exerciseId: Long,
        name: String,
        trackingFields: Set<TrackingField>,
        note: String,
    )

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

    suspend fun startRest(workoutId: Long, durationSeconds: Int, nowEpochMilli: Long = System.currentTimeMillis())

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
}

