package com.deepak.flow.core.repository

import com.deepak.flow.core.gym.GymExerciseSearchHit
import com.deepak.flow.core.gym.GymExerciseSelection
import com.deepak.flow.core.gym.GymLibraryExercise
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
        canonicalExerciseId: String = "",
    ): Long

    suspend fun updateExercise(
        exerciseId: Long,
        name: String,
        trackingFields: Set<TrackingField>,
        note: String,
        canonicalExerciseId: String = "",
    )

    suspend fun setExerciseSkipped(exerciseId: Long, skipped: Boolean)

    /** Persists skipped placeholders for every unfinished planned set slot. */
    suspend fun skipRemainingPlannedSets(exerciseId: Long)

    suspend fun clearSkippedSets(exerciseId: Long)

    suspend fun setExerciseCompleted(exerciseId: Long, completedAtEpochMilli: Long)

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

    /**
     * Cancels a pending rest-complete alarm without alerting.
     * Used by Skip. Natural rest completion must not call this.
     */
    fun cancelScheduledRestAlert()

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
        completedAtEpochMilli: Long? = null,
        routineExerciseId: Long? = null,
        exerciseStableKey: String? = null,
        canonicalExerciseId: String = "",
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

    suspend fun isRoutineInActiveWorkout(routineId: Long): Boolean

    suspend fun setRoutineStarred(routineId: Long, starred: Boolean)

    /**
     * Last completed set of each exercise from the previous occurrence of this
     * routine day, keyed by this session's exercise id.
     */
    suspend fun previousOccurrenceSeeds(session: GymWorkoutSession): Map<Long, GymSetMeasurements>

    /**
     * Last completed set for each exercise in [session], keyed by workout exercise row id.
     * Uses canonical [exerciseId] across all completed workouts.
     */
    suspend fun previousPerformanceSeeds(session: GymWorkoutSession): Map<Long, GymSetMeasurements>

    suspend fun resolveExerciseSelection(
        displayName: String,
        canonicalExerciseId: String = "",
    ): GymExerciseSelection

    suspend fun searchExercises(
        query: String,
        muscleFilter: com.deepak.flow.core.gym.GymMuscleGroup? = null,
        equipmentFilter: com.deepak.flow.core.gym.GymEquipment? = null,
        limit: Int = com.deepak.flow.core.gym.GymExerciseNameCatalog.MAX_SUGGESTIONS,
    ): List<GymExerciseSearchHit>

    suspend fun browseExercises(
        query: String = "",
        muscleFilter: com.deepak.flow.core.gym.GymMuscleGroup? = null,
        equipmentFilter: com.deepak.flow.core.gym.GymEquipment? = null,
        limit: Int = com.deepak.flow.core.gym.GymExerciseNameCatalog.PICKER_LIMIT,
    ): List<GymExerciseSearchHit>

    suspend fun getExerciseMetadata(exerciseId: String): com.deepak.flow.core.gym.GymExerciseMetadata?

    suspend fun saveBuiltinExerciseOverride(
        exerciseId: String,
        displayName: String? = null,
        primaryMuscle: com.deepak.flow.core.gym.GymMuscleGroup? = null,
        secondaryMuscles: List<com.deepak.flow.core.gym.GymMuscleGroup> = emptyList(),
        equipment: com.deepak.flow.core.gym.GymEquipment? = null,
    )

    suspend fun clearBuiltinExerciseOverride(exerciseId: String)

    suspend fun saveCustomExerciseMetadata(
        exerciseId: String,
        displayName: String? = null,
        primaryMuscle: com.deepak.flow.core.gym.GymMuscleGroup? = null,
        secondaryMuscles: List<com.deepak.flow.core.gym.GymMuscleGroup> = emptyList(),
        equipment: com.deepak.flow.core.gym.GymEquipment? = null,
    )

    suspend fun createCustomExercise(
        displayName: String,
        primaryMuscle: com.deepak.flow.core.gym.GymMuscleGroup? = null,
        secondaryMuscles: List<com.deepak.flow.core.gym.GymMuscleGroup> = emptyList(),
        equipment: com.deepak.flow.core.gym.GymEquipment? = null,
    ): GymExerciseSelection

    suspend fun deleteCustomExercise(exerciseId: String)

    suspend fun listLibraryExercises(
        query: String = "",
        muscleFilter: com.deepak.flow.core.gym.GymMuscleGroup? = null,
        equipmentFilter: com.deepak.flow.core.gym.GymEquipment? = null,
        sourceFilter: com.deepak.flow.core.gym.GymLibrarySourceFilter =
            com.deepak.flow.core.gym.GymLibrarySourceFilter.ALL,
    ): List<GymLibraryExercise>

    suspend fun getLibraryExercise(exerciseId: String): GymLibraryExercise?

    suspend fun getExerciseNameSuggestions(): List<String>

    fun observePrimaryRoutine(): Flow<GymRoutine?>
}
