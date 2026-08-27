package com.deepak.flow.core.repository

import com.deepak.flow.core.database.GymWorkoutDao
import com.deepak.flow.core.database.GymWorkoutEntity
import com.deepak.flow.core.database.GymWorkoutExerciseEntity
import com.deepak.flow.core.database.GymWorkoutSetEntity
import com.deepak.flow.core.gym.GymLimits
import com.deepak.flow.core.gym.GymLogic
import com.deepak.flow.core.gym.GymSetMeasurements
import com.deepak.flow.core.gym.GymWorkoutExercise
import com.deepak.flow.core.gym.GymWorkoutSession
import com.deepak.flow.core.gym.GymWorkoutSet
import com.deepak.flow.core.gym.GymWorkoutStatus
import com.deepak.flow.core.gym.GymWorkoutType
import com.deepak.flow.core.gym.TrackingField
import com.deepak.flow.core.gym.WeightUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class GymWorkoutRepositoryImpl(
    private val dao: GymWorkoutDao,
) : GymWorkoutRepository {

    override fun observeActiveSession(type: GymWorkoutType): Flow<GymWorkoutSession?> =
        dao.observeLatestByTypeAndStatus(type.name, GymWorkoutStatus.ACTIVE.name)
            .flatMapLatestSession()

    override fun observeSession(workoutId: Long): Flow<GymWorkoutSession?> =
        dao.observeWorkout(workoutId).flatMapLatestSession()

    override suspend fun getActiveSession(type: GymWorkoutType): GymWorkoutSession? {
        val entity = dao.getLatestByTypeAndStatus(type.name, GymWorkoutStatus.ACTIVE.name) ?: return null
        return loadSession(entity)
    }

    override suspend fun getSession(workoutId: Long): GymWorkoutSession? {
        val entity = dao.getWorkout(workoutId) ?: return null
        return loadSession(entity)
    }

    override suspend fun startFreeWorkout(weightUnit: WeightUnit): Long {
        return dao.insertWorkout(
            GymWorkoutEntity(
                type = GymWorkoutType.FREE.name,
                status = GymWorkoutStatus.ACTIVE.name,
                startedAtEpochMilli = System.currentTimeMillis(),
                weightUnit = weightUnit.name,
                restDurationSeconds = GymLimits.SET_REST_DEFAULT_SECONDS,
            ),
        )
    }

    override suspend fun ensureActiveFreeWorkout(weightUnit: WeightUnit): Long {
        getActiveSession(GymWorkoutType.FREE)?.let { return it.id }
        return startFreeWorkout(weightUnit)
    }

    override suspend fun setWeightUnit(workoutId: Long, unit: WeightUnit) {
        val workout = dao.getWorkout(workoutId) ?: return
        dao.updateWorkout(workout.copy(weightUnit = unit.name))
    }

    override suspend fun setCurrentExerciseIndex(workoutId: Long, index: Int) {
        val workout = dao.getWorkout(workoutId) ?: return
        dao.updateWorkout(workout.copy(currentExerciseIndex = index.coerceAtLeast(0)))
    }

    override suspend fun addExercise(
        workoutId: Long,
        name: String,
        trackingFields: Set<TrackingField>,
        note: String,
    ): Long {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Name can't be empty." }
        require(trackingFields.isNotEmpty()) { "Pick at least one tracking field." }
        val order = dao.getExercises(workoutId).size
        val exerciseId = dao.insertExercise(
            GymWorkoutExerciseEntity(
                workoutId = workoutId,
                exerciseName = trimmed,
                sortOrder = order,
                note = GymLimits.clampNote(note),
                trackingFields = GymLogic.encodeTrackingFields(trackingFields),
            ),
        )
        val workout = dao.getWorkout(workoutId)
        if (workout != null) {
            dao.updateWorkout(workout.copy(currentExerciseIndex = order))
        }
        return exerciseId
    }

    override suspend fun updateExercise(
        exerciseId: Long,
        name: String,
        trackingFields: Set<TrackingField>,
        note: String,
    ) {
        val existing = dao.getExercise(exerciseId) ?: return
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Name can't be empty." }
        require(trackingFields.isNotEmpty()) { "Pick at least one tracking field." }
        dao.updateExercise(
            existing.copy(
                exerciseName = trimmed,
                trackingFields = GymLogic.encodeTrackingFields(trackingFields),
                note = GymLimits.clampNote(note),
            ),
        )
    }

    override suspend fun deleteExercise(exerciseId: Long) {
        val exercise = dao.getExercise(exerciseId) ?: return
        val sets = dao.getSets(exerciseId)
        sets.forEach { dao.deleteSet(it.id) }
        dao.deleteExercise(exerciseId)
        val remaining = dao.getExercises(exercise.workoutId)
        remaining.forEachIndexed { index, item ->
            if (item.sortOrder != index) {
                dao.updateExercise(item.copy(sortOrder = index))
            }
        }
        val workout = dao.getWorkout(exercise.workoutId) ?: return
        dao.updateWorkout(
            workout.copy(
                currentExerciseIndex = workout.currentExerciseIndex.coerceAtMost(
                    (remaining.size - 1).coerceAtLeast(0),
                ),
            ),
        )
    }

    override suspend fun addSet(
        exerciseId: Long,
        measurements: GymSetMeasurements,
        failure: Boolean,
        saved: Boolean,
    ): Long {
        val sets = dao.getSets(exerciseId)
        return dao.insertSet(
            measurements.toEntity(
                id = 0L,
                workoutExerciseId = exerciseId,
                setNumber = GymLogic.nextSetNumber(sets.map { it.toDomain() }),
                failure = failure,
                saved = saved,
            ),
        )
    }

    override suspend fun updateSet(
        setId: Long,
        measurements: GymSetMeasurements,
        failure: Boolean,
        saved: Boolean,
        setNumber: Int?,
    ) {
        val existing = dao.getSet(setId) ?: return
        val desiredNumber = (setNumber ?: existing.setNumber).coerceAtLeast(1)
        dao.updateSet(
            measurements.toEntity(
                id = existing.id,
                workoutExerciseId = existing.workoutExerciseId,
                setNumber = desiredNumber,
                failure = failure,
                saved = saved,
            ),
        )
        if (desiredNumber != existing.setNumber) {
            renumberSets(existing.workoutExerciseId, preferId = setId, preferNumber = desiredNumber)
        }
    }

    /**
     * Keep set numbers contiguous and unique. When [preferId] is set, that set keeps
     * [preferNumber] and others shift around it.
     */
    private suspend fun renumberSets(
        exerciseId: Long,
        preferId: Long? = null,
        preferNumber: Int? = null,
    ) {
        val remaining = dao.getSets(exerciseId).sortedBy { it.setNumber }.toMutableList()
        if (preferId != null && preferNumber != null) {
            val preferred = remaining.firstOrNull { it.id == preferId } ?: return
            remaining.removeAll { it.id == preferId }
            val insertAt = (preferNumber - 1).coerceIn(0, remaining.size)
            remaining.add(insertAt, preferred)
        }
        remaining.forEachIndexed { index, set ->
            val number = index + 1
            if (set.setNumber != number) {
                dao.updateSet(set.copy(setNumber = number))
            }
        }
    }

    override suspend fun deleteSet(setId: Long) {
        val existing = dao.getSet(setId) ?: return
        dao.deleteSet(setId)
        renumberSets(existing.workoutExerciseId)
    }

    override suspend fun startRest(workoutId: Long, durationSeconds: Int, nowEpochMilli: Long) {
        val workout = dao.getWorkout(workoutId) ?: return
        val seconds = GymLimits.clampSetRestSeconds(durationSeconds)
        dao.updateWorkout(
            workout.copy(
                restDurationSeconds = seconds,
                restEndsAtEpochMilli = nowEpochMilli + seconds * 1000L,
            ),
        )
    }

    override suspend fun extendRest(
        workoutId: Long,
        extraSeconds: Int,
        nowEpochMilli: Long,
    ) {
        if (extraSeconds == 0) return
        val workout = dao.getWorkout(workoutId) ?: return
        val currentEnd = workout.restEndsAtEpochMilli ?: return
        val base = maxOf(currentEnd, nowEpochMilli)
        dao.updateWorkout(
            workout.copy(
                restEndsAtEpochMilli = base + extraSeconds * 1000L,
            ),
        )
    }

    override suspend fun clearRest(workoutId: Long) {
        val workout = dao.getWorkout(workoutId) ?: return
        dao.updateWorkout(workout.copy(restEndsAtEpochMilli = null))
    }

    override suspend fun completeWorkout(workoutId: Long, nowEpochMilli: Long) {
        val workout = dao.getWorkout(workoutId) ?: return
        dao.updateWorkout(
            workout.copy(
                status = GymWorkoutStatus.COMPLETED.name,
                completed = true,
                endedAtEpochMilli = nowEpochMilli,
                restEndsAtEpochMilli = null,
            ),
        )
    }

    override suspend fun discardWorkout(workoutId: Long) {
        dao.deleteWorkoutCascade(workoutId)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun Flow<GymWorkoutEntity?>.flatMapLatestSession(): Flow<GymWorkoutSession?> =
        flatMapLatest { entity ->
            if (entity == null) {
                flowOf(null)
            } else {
                combine(
                    dao.observeWorkout(entity.id),
                    dao.observeExercises(entity.id),
                    dao.observeSetsForWorkout(entity.id),
                ) { workout, exercises, sets ->
                    if (workout == null) return@combine null
                    val setsByExercise = sets.groupBy { it.workoutExerciseId }
                    workout.toDomain(
                        exercises.map { exercise ->
                            exercise.toDomain(
                                setsByExercise[exercise.id].orEmpty().map { it.toDomain() },
                            )
                        },
                    )
                }.distinctUntilChanged()
            }
        }

    private suspend fun loadSession(entity: GymWorkoutEntity): GymWorkoutSession {
        val exercises = dao.getExercises(entity.id)
        val setsByExercise = exercises.associate { exercise ->
            exercise.id to dao.getSets(exercise.id)
        }
        return entity.toDomain(
            exercises.map { exercise ->
                exercise.toDomain(setsByExercise[exercise.id].orEmpty().map { it.toDomain() })
            },
        )
    }
}

private fun GymWorkoutEntity.toDomain(exercises: List<GymWorkoutExercise>) = GymWorkoutSession(
    id = id,
    type = runCatching { GymWorkoutType.valueOf(type) }.getOrDefault(GymWorkoutType.FREE),
    status = runCatching { GymWorkoutStatus.valueOf(status) }.getOrDefault(GymWorkoutStatus.ACTIVE),
    startedAtEpochMilli = startedAtEpochMilli,
    endedAtEpochMilli = endedAtEpochMilli,
    completed = completed,
    weightUnit = runCatching { WeightUnit.valueOf(weightUnit) }.getOrDefault(WeightUnit.KG),
    restEndsAtEpochMilli = restEndsAtEpochMilli,
    restDurationSeconds = restDurationSeconds,
    currentExerciseIndex = currentExerciseIndex,
    exercises = exercises,
)

private fun GymWorkoutExerciseEntity.toDomain(sets: List<GymWorkoutSet>) = GymWorkoutExercise(
    id = id,
    workoutId = workoutId,
    name = exerciseName,
    sortOrder = sortOrder,
    note = note,
    trackingFields = GymLogic.decodeTrackingFields(trackingFields),
    sets = sets,
)

private fun GymWorkoutSetEntity.toDomain() = GymWorkoutSet(
    id = id,
    workoutExerciseId = workoutExerciseId,
    setNumber = setNumber,
    measurements = GymSetMeasurements(
        weight = weight,
        weightUnit = weightUnit?.let { runCatching { WeightUnit.valueOf(it) }.getOrNull() },
        reps = reps,
        durationSeconds = durationSeconds,
        distance = distance,
        speed = speed,
        incline = incline,
        resistance = resistance,
        rounds = rounds,
    ),
    failure = failure,
    saved = saved,
)

private fun GymSetMeasurements.toEntity(
    id: Long,
    workoutExerciseId: Long,
    setNumber: Int,
    failure: Boolean,
    saved: Boolean,
) = GymWorkoutSetEntity(
    id = id,
    workoutExerciseId = workoutExerciseId,
    setNumber = setNumber,
    weight = weight,
    weightUnit = weightUnit?.name,
    reps = reps,
    durationSeconds = durationSeconds,
    distance = distance,
    speed = speed,
    incline = incline,
    resistance = resistance,
    rounds = rounds,
    failure = failure,
    saved = saved,
)

