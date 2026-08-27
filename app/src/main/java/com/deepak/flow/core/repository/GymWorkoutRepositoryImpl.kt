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

    override fun observeCompletedSessionsBetween(
        fromInclusive: Long,
        toExclusive: Long,
    ): Flow<List<GymWorkoutSession>> =
        dao.observeCompletedBetween(
            status = GymWorkoutStatus.COMPLETED.name,
            fromInclusive = fromInclusive,
            toExclusive = toExclusive,
        ).flatMapLatest { entities ->
            if (entities.isEmpty()) {
                flowOf(emptyList())
            } else {
                val sessionFlows = entities.map { entity ->
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
                    }
                }
                combine(sessionFlows) { sessions ->
                    sessions.filterNotNull()
                }
            }
        }

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
        getActiveSession(GymWorkoutType.FREE)?.let { existing ->
            if (existing.weightUnit != weightUnit) {
                setWeightUnit(existing.id, weightUnit)
            }
            backfillExerciseStartedAtIfNeeded(existing)
            return existing.id
        }
        return startFreeWorkout(weightUnit)
    }

    /**
     * One-time repair for active sessions that predate [currentExerciseStartedAtEpochMilli].
     * Uses session start (not set count) so elapsed stays anchored to a real timestamp.
     */
    private suspend fun backfillExerciseStartedAtIfNeeded(session: GymWorkoutSession) {
        if (session.exercises.isEmpty()) return
        if (session.currentExerciseStartedAtEpochMilli != null) return
        val workout = dao.getWorkout(session.id) ?: return
        dao.updateWorkout(
            workout.copy(currentExerciseStartedAtEpochMilli = session.startedAtEpochMilli),
        )
    }

    override suspend fun setWeightUnit(workoutId: Long, unit: WeightUnit) {
        val workout = dao.getWorkout(workoutId) ?: return
        dao.updateWorkout(workout.copy(weightUnit = unit.name))
    }

    override suspend fun setCurrentExerciseIndex(workoutId: Long, index: Int) {
        val workout = dao.getWorkout(workoutId) ?: return
        val newIndex = index.coerceAtLeast(0)
        val now = System.currentTimeMillis()
        val exerciseStartedAt = if (newIndex != workout.currentExerciseIndex) {
            now
        } else {
            workout.currentExerciseStartedAtEpochMilli ?: now
        }
        dao.updateWorkout(
            workout.copy(
                currentExerciseIndex = newIndex,
                currentExerciseStartedAtEpochMilli = exerciseStartedAt,
            ),
        )
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
            dao.updateWorkout(
                workout.copy(
                    currentExerciseIndex = order,
                    currentExerciseStartedAtEpochMilli = System.currentTimeMillis(),
                ),
            )
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
        val workoutBefore = dao.getWorkout(exercise.workoutId) ?: return
        val exercisesBefore = dao.getExercises(exercise.workoutId)
        val previousCurrentId = exercisesBefore
            .getOrNull(workoutBefore.currentExerciseIndex.coerceAtLeast(0))
            ?.id
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
        val newIndex = workout.currentExerciseIndex.coerceAtMost(
            (remaining.size - 1).coerceAtLeast(0),
        )
        val newCurrentId = remaining.getOrNull(newIndex)?.id
        val exerciseStartedAt = when {
            newCurrentId == null -> null
            newCurrentId != previousCurrentId -> System.currentTimeMillis()
            else -> workout.currentExerciseStartedAtEpochMilli
                ?: System.currentTimeMillis()
        }
        dao.updateWorkout(
            workout.copy(
                currentExerciseIndex = newIndex,
                currentExerciseStartedAtEpochMilli = exerciseStartedAt,
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

    override suspend fun restoreSet(
        exerciseId: Long,
        measurements: GymSetMeasurements,
        failure: Boolean,
        setNumber: Int,
    ): Long {
        val id = dao.insertSet(
            measurements.toEntity(
                id = 0L,
                workoutExerciseId = exerciseId,
                setNumber = setNumber.coerceAtLeast(1),
                failure = failure,
                saved = true,
            ),
        )
        renumberSets(exerciseId, preferId = id, preferNumber = setNumber.coerceAtLeast(1))
        return id
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
        val remainingSeconds = GymLogic.remainingRestSeconds(currentEnd, nowEpochMilli)
        if (extraSeconds < 0) {
            val minSeconds = GymLimits.SET_REST_MIN_SECONDS
            if (remainingSeconds <= minSeconds) return
            val nextSeconds = (remainingSeconds + extraSeconds).coerceAtLeast(minSeconds)
            dao.updateWorkout(
                workout.copy(
                    restEndsAtEpochMilli = nowEpochMilli + nextSeconds * 1000L,
                ),
            )
            return
        }
        val maxSeconds = GymLimits.SET_REST_MAX_SECONDS
        if (remainingSeconds >= maxSeconds) return
        val nextSeconds = (remainingSeconds + extraSeconds).coerceAtMost(maxSeconds)
        dao.updateWorkout(
            workout.copy(
                restEndsAtEpochMilli = nowEpochMilli + nextSeconds * 1000L,
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

    override suspend fun setWorkoutStarred(workoutId: Long, starred: Boolean) {
        val workout = dao.getWorkout(workoutId) ?: return
        dao.updateWorkout(workout.copy(starred = starred))
    }

    override suspend fun setWorkoutTitle(workoutId: Long, title: String) {
        val workout = dao.getWorkout(workoutId) ?: return
        dao.updateWorkout(workout.copy(title = title.trim()))
    }

    override suspend fun restoreExercise(
        workoutId: Long,
        sortOrder: Int,
        name: String,
        trackingFields: Set<TrackingField>,
        note: String,
        sets: List<GymWorkoutSet>,
    ): Long {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Name can't be empty." }
        require(trackingFields.isNotEmpty()) { "Pick at least one tracking field." }
        val existing = dao.getExercises(workoutId)
        existing.filter { it.sortOrder >= sortOrder }.forEach { exercise ->
            dao.updateExercise(exercise.copy(sortOrder = exercise.sortOrder + 1))
        }
        val exerciseId = dao.insertExercise(
            GymWorkoutExerciseEntity(
                workoutId = workoutId,
                exerciseName = trimmed,
                sortOrder = sortOrder,
                note = GymLimits.clampNote(note),
                trackingFields = GymLogic.encodeTrackingFields(trackingFields),
            ),
        )
        sets.sortedBy { it.setNumber }.forEach { set ->
            dao.insertSet(
                set.measurements.toEntity(
                    id = 0L,
                    workoutExerciseId = exerciseId,
                    setNumber = set.setNumber,
                    failure = set.failure,
                    saved = set.saved,
                ),
            )
        }
        val remaining = dao.getExercises(workoutId)
        remaining.forEachIndexed { index, item ->
            if (item.sortOrder != index) {
                dao.updateExercise(item.copy(sortOrder = index))
            }
        }
        val workout = dao.getWorkout(workoutId) ?: return exerciseId
        if (sortOrder <= workout.currentExerciseIndex) {
            val maxIndex = remaining.lastIndex.coerceAtLeast(0)
            val nextIndex = (workout.currentExerciseIndex + 1).coerceIn(0, maxIndex)
            dao.updateWorkout(
                workout.copy(currentExerciseIndex = nextIndex),
            )
        }
        return exerciseId
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
    currentExerciseStartedAtEpochMilli = currentExerciseStartedAtEpochMilli,
    starred = starred,
    title = title,
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

