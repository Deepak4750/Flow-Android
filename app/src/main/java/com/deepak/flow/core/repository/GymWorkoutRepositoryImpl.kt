package com.deepak.flow.core.repository

import com.deepak.flow.core.database.GymCustomExerciseDao
import com.deepak.flow.core.database.GymCustomExerciseEntity
import com.deepak.flow.core.database.GymExerciseOverrideDao
import com.deepak.flow.core.database.GymExerciseOverrideEntity
import com.deepak.flow.core.database.GymRoutineDao
import com.deepak.flow.core.database.GymRoutineDayEntity
import com.deepak.flow.core.database.GymRoutineEntity
import com.deepak.flow.core.database.GymRoutineExerciseEntity
import com.deepak.flow.core.database.GymWorkoutDao
import com.deepak.flow.core.database.GymWorkoutEntity
import com.deepak.flow.core.database.GymWorkoutExerciseEntity
import com.deepak.flow.core.database.GymWorkoutSetEntity
import com.deepak.flow.core.database.UserProfileDao
import com.deepak.flow.core.gym.GymLibraryExercise
import com.deepak.flow.core.gym.GymBuiltinExerciseCatalog
import com.deepak.flow.core.gym.GymBuiltinExerciseOverridePolicy
import com.deepak.flow.core.gym.GymCustomExerciseRecord
import com.deepak.flow.core.gym.GymEquipment
import com.deepak.flow.core.gym.GymExerciseIdentity
import com.deepak.flow.core.gym.GymExerciseMetadata
import com.deepak.flow.core.gym.GymExerciseMetadataCodec
import com.deepak.flow.core.gym.GymExerciseMetadataResolver
import com.deepak.flow.core.gym.GymExerciseNameCatalog
import com.deepak.flow.core.gym.GymExerciseNormalizer
import com.deepak.flow.core.gym.GymExerciseOverrideRecord
import com.deepak.flow.core.gym.GymExerciseSearchHit
import com.deepak.flow.core.gym.GymExerciseSelection
import com.deepak.flow.core.gym.GymExerciseLibrary
import com.deepak.flow.core.gym.GymLibrarySourceFilter
import com.deepak.flow.core.gym.GymMuscleGroup
import com.deepak.flow.core.gym.toEntity
import com.deepak.flow.core.gym.toRecord
import com.deepak.flow.core.gym.GymLimits
import com.deepak.flow.core.gym.GymLogic
import com.deepak.flow.core.gym.GymRestKind
import com.deepak.flow.core.gym.GymRoutine
import com.deepak.flow.core.gym.GymRoutineDay
import com.deepak.flow.core.gym.GymRoutineExercise
import com.deepak.flow.core.gym.GymSetMeasurements
import com.deepak.flow.core.gym.GymWorkoutExercise
import com.deepak.flow.core.gym.GymWorkoutSession
import com.deepak.flow.core.gym.GymWorkoutSet
import com.deepak.flow.core.gym.GymWorkoutStatus
import com.deepak.flow.core.gym.GymWorkoutType
import com.deepak.flow.core.gym.TrackingField
import com.deepak.flow.core.gym.WeightUnit
import com.deepak.flow.core.notification.GymRestAlarmCoordinator
import com.deepak.flow.core.notification.NoOpWorkoutEventNotifier
import com.deepak.flow.core.notification.WorkoutEventNotifierPort
import com.deepak.flow.core.notification.GymRestAlarmPort
import com.deepak.flow.core.notification.GymRestAlarmRequest
import com.deepak.flow.core.notification.GymRestAlerterPort
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class GymWorkoutRepositoryImpl(
    private val dao: GymWorkoutDao,
    private val routineDao: GymRoutineDao,
    private val customExerciseDao: GymCustomExerciseDao,
    private val exerciseOverrideDao: GymExerciseOverrideDao,
    private val profileDao: UserProfileDao,
    private val workoutEvents: WorkoutEventNotifierPort = NoOpWorkoutEventNotifier,
    private val gymRestAlarms: GymRestAlarmCoordinator = GymRestAlarmCoordinator(
        alarms = object : GymRestAlarmPort {
            override fun schedule(request: GymRestAlarmRequest) = Unit
            override fun cancel() = Unit
        },
        alerter = object : GymRestAlerterPort {
            override fun signal(request: GymRestAlarmRequest) = Unit
            override fun suppress(request: GymRestAlarmRequest) = Unit
        },
    ),
) : GymWorkoutRepository {

    override fun observeActiveSession(type: GymWorkoutType): Flow<GymWorkoutSession?> =
        dao.observeLatestByTypeAndStatus(type.name, GymWorkoutStatus.ACTIVE.name)
            .flatMapLatestSession()

    override fun observeAnyActiveSession(): Flow<GymWorkoutSession?> =
        dao.observeLatestByStatus(GymWorkoutStatus.ACTIVE.name).flatMapLatestSession()

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

    override suspend fun getAnyActiveSession(): GymWorkoutSession? {
        val entity = dao.getLatestByStatus(GymWorkoutStatus.ACTIVE.name) ?: return null
        return loadSession(entity)
    }

    override suspend fun getSession(workoutId: Long): GymWorkoutSession? {
        val entity = dao.getWorkout(workoutId) ?: return null
        return loadSession(entity)
    }

    override suspend fun startFreeWorkout(weightUnit: WeightUnit): Long {
        val startedAt = System.currentTimeMillis()
        val id = dao.insertWorkout(
            GymWorkoutEntity(
                type = GymWorkoutType.FREE.name,
                status = GymWorkoutStatus.ACTIVE.name,
                startedAtEpochMilli = startedAt,
                weightUnit = weightUnit.name,
                restDurationSeconds = GymLimits.SET_REST_DEFAULT_SECONDS,
            ),
        )
        workoutEvents.onWorkoutStarted(
            workoutId = id,
            workoutTitle = "",
            workoutType = GymWorkoutType.FREE,
            startedAtEpochMilli = startedAt,
        )
        return id
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

    override suspend fun ensureActiveRoutineWorkout(weightUnit: WeightUnit): Long? {
        getActiveSession(GymWorkoutType.ROUTINE)?.let { existing ->
            if (existing.weightUnit != weightUnit) {
                setWeightUnit(existing.id, weightUnit)
            }
            backfillExerciseStartedAtIfNeeded(existing)
            return existing.id
        }
        val routine = loadPrimaryRoutine() ?: return null
        val day = routine.currentDay() ?: return null
        if (day.isRestDay || day.exercises.isEmpty()) return null
        val startedAt = System.currentTimeMillis()
        val workoutId = dao.insertWorkout(
            GymWorkoutEntity(
                type = GymWorkoutType.ROUTINE.name,
                status = GymWorkoutStatus.ACTIVE.name,
                startedAtEpochMilli = startedAt,
                weightUnit = weightUnit.name,
                restDurationSeconds = GymLimits.SET_REST_DEFAULT_SECONDS,
                title = GymLogic.formatDayHeading(day.dayIndex, day.name, day.isRestDay),
                routineId = routine.id,
                dayIndex = day.dayIndex,
            ),
        )
        day.exercises.forEach { template ->
            addExercise(
                workoutId = workoutId,
                name = template.name,
                trackingFields = template.trackingFields,
                note = template.note,
                plannedSetCount = template.setCount,
                routineExerciseId = template.id,
                exerciseStableKey = template.stableKey,
                canonicalExerciseId = template.exerciseId,
            )
        }
        setCurrentExerciseIndex(workoutId, 0)
        workoutEvents.onWorkoutStarted(
            workoutId = workoutId,
            workoutTitle = GymLogic.formatDayHeading(day.dayIndex, day.name, day.isRestDay),
            workoutType = GymWorkoutType.ROUTINE,
            startedAtEpochMilli = startedAt,
        )
        return workoutId
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
        plannedSetCount: Int,
        routineExerciseId: Long?,
        exerciseStableKey: String?,
        canonicalExerciseId: String,
    ): Long {
        val selection = resolveExerciseSelection(name, canonicalExerciseId)
        persistCustomExerciseIfNeeded(selection)
        require(trackingFields.isNotEmpty()) { "Pick at least one tracking field." }
        val order = dao.getExercises(workoutId).size
        val exerciseId = dao.insertExercise(
            GymWorkoutExerciseEntity(
                workoutId = workoutId,
                exerciseId = selection.exerciseId,
                exerciseName = selection.displayName,
                sortOrder = order,
                note = GymLimits.clampNote(note),
                trackingFields = GymLogic.encodeTrackingFields(trackingFields),
                plannedSetCount = if (plannedSetCount > 0) GymLimits.clampSetCount(plannedSetCount) else 0,
                skipped = false,
                completedAtEpochMilli = null,
                routineExerciseId = routineExerciseId,
                exerciseStableKey = exerciseStableKey,
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
        canonicalExerciseId: String,
    ) {
        val existing = dao.getExercise(exerciseId) ?: return
        val selection = resolveExerciseSelection(name, canonicalExerciseId)
        persistCustomExerciseIfNeeded(selection)
        require(trackingFields.isNotEmpty()) { "Pick at least one tracking field." }
        val clampedNote = GymLimits.clampNote(note)
        dao.updateExercise(
            existing.copy(
                exerciseId = selection.exerciseId,
                exerciseName = selection.displayName,
                trackingFields = GymLogic.encodeTrackingFields(trackingFields),
                note = clampedNote,
            ),
        )
        val templateId = existing.routineExerciseId ?: return
        val template = routineDao.getExercise(templateId) ?: return
        routineDao.updateExercise(
            template.copy(
                exerciseId = selection.exerciseId,
                name = selection.displayName,
                trackingFields = GymLogic.encodeTrackingFields(trackingFields),
                note = clampedNote,
            ),
        )
    }

    override suspend fun setExerciseSkipped(exerciseId: Long, skipped: Boolean) {
        val existing = dao.getExercise(exerciseId) ?: return
        dao.updateExercise(existing.copy(skipped = skipped))
    }

    override suspend fun skipRemainingPlannedSets(exerciseId: Long) {
        val existing = dao.getExercise(exerciseId) ?: return
        if (existing.plannedSetCount <= 0) return
        val savedSets = dao.getSets(exerciseId)
        for (setNumber in 1..existing.plannedSetCount) {
            val alreadyRecorded = savedSets.any { it.setNumber == setNumber && it.saved }
            if (alreadyRecorded) continue
            dao.insertSet(
                GymWorkoutSetEntity(
                    workoutExerciseId = exerciseId,
                    setNumber = setNumber,
                    saved = true,
                    skipped = true,
                ),
            )
        }
    }

    override suspend fun clearSkippedSets(exerciseId: Long) {
        val skippedSets = dao.getSets(exerciseId).filter { it.skipped }
        skippedSets.forEach { dao.deleteSet(it.id) }
    }

    override suspend fun setExerciseCompleted(exerciseId: Long, completedAtEpochMilli: Long) {
        val existing = dao.getExercise(exerciseId) ?: return
        if (existing.skipped || existing.completedAtEpochMilli != null) return
        dao.updateExercise(existing.copy(completedAtEpochMilli = completedAtEpochMilli))
        val workout = dao.getWorkout(existing.workoutId)
        val workoutType = workout?.type?.let { runCatching { GymWorkoutType.valueOf(it) }.getOrNull() }
            ?: GymWorkoutType.FREE
        workoutEvents.onExerciseCompleted(
            exerciseName = existing.exerciseName,
            workoutType = workoutType,
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

    override suspend fun startRest(
        workoutId: Long,
        durationSeconds: Int,
        kind: GymRestKind,
        nowEpochMilli: Long,
    ) {
        val workout = dao.getWorkout(workoutId) ?: return
        val seconds = when (kind) {
            GymRestKind.EXERCISE -> GymLimits.clampExerciseRestSeconds(durationSeconds)
            else -> GymLimits.clampSetRestSeconds(durationSeconds)
        }
        val restEndsAt = nowEpochMilli + seconds * 1000L
        dao.updateWorkout(
            workout.copy(
                restDurationSeconds = seconds,
                restEndsAtEpochMilli = restEndsAt,
                restKind = kind.name,
            ),
        )
        scheduleRestAlarm(workoutId, restEndsAt, nowEpochMilli)
        val workoutType = runCatching { GymWorkoutType.valueOf(workout.type) }
            .getOrDefault(GymWorkoutType.FREE)
        val exerciseName = dao.getExercises(workoutId)
            .getOrNull(workout.currentExerciseIndex.coerceAtLeast(0))
            ?.exerciseName
            .orEmpty()
        workoutEvents.onRestStarted(
            exerciseName = exerciseName,
            restKind = kind,
            workoutType = workoutType,
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
        val kind = runCatching { GymRestKind.valueOf(workout.restKind) }
            .getOrDefault(GymRestKind.SET)
        val minSeconds = when (kind) {
            GymRestKind.EXERCISE -> GymLimits.EXERCISE_REST_MIN_SECONDS
            else -> GymLimits.SET_REST_MIN_SECONDS
        }
        val maxSeconds = when (kind) {
            GymRestKind.EXERCISE -> GymLimits.EXERCISE_REST_MAX_SECONDS
            else -> GymLimits.SET_REST_MAX_SECONDS
        }
        if (extraSeconds < 0) {
            if (remainingSeconds <= minSeconds) return
            val nextSeconds = (remainingSeconds + extraSeconds).coerceAtLeast(minSeconds)
            val restEndsAt = nowEpochMilli + nextSeconds * 1000L
            dao.updateWorkout(
                workout.copy(
                    restEndsAtEpochMilli = restEndsAt,
                ),
            )
            rescheduleRestAlarm(workoutId, restEndsAt)
            return
        }
        if (remainingSeconds >= maxSeconds) return
        val nextSeconds = (remainingSeconds + extraSeconds).coerceAtMost(maxSeconds)
        val restEndsAt = nowEpochMilli + nextSeconds * 1000L
        dao.updateWorkout(
            workout.copy(
                restEndsAtEpochMilli = restEndsAt,
            ),
        )
        rescheduleRestAlarm(workoutId, restEndsAt)
    }

    override suspend fun clearRest(workoutId: Long) {
        val workout = dao.getWorkout(workoutId) ?: return
        dao.updateWorkout(
            workout.copy(
                restEndsAtEpochMilli = null,
                restKind = GymRestKind.NONE.name,
            ),
        )
    }

    override fun cancelScheduledRestAlert() {
        gymRestAlarms.onRestAbandoned()
    }

    override suspend fun completeWorkout(workoutId: Long, nowEpochMilli: Long) {
        val workout = dao.getWorkout(workoutId) ?: return
        if (workout.restEndsAtEpochMilli != null) {
            gymRestAlarms.onRestAbandoned()
        }
        dao.updateWorkout(
            workout.copy(
                status = GymWorkoutStatus.COMPLETED.name,
                completed = true,
                endedAtEpochMilli = nowEpochMilli,
                restEndsAtEpochMilli = null,
                restKind = GymRestKind.NONE.name,
            ),
        )
        val workoutType = runCatching { GymWorkoutType.valueOf(workout.type) }
            .getOrDefault(GymWorkoutType.FREE)
        val durationSeconds = GymLogic.elapsedSeconds(workout.startedAtEpochMilli, nowEpochMilli).toInt()
        workoutEvents.onWorkoutCompleted(
            workoutTitle = workout.title.orEmpty(),
            workoutType = workoutType,
            durationSeconds = durationSeconds,
        )
        val routineId = workout.routineId ?: return
        advanceRoutineDay(routineId, nowEpochMilli)
    }

    override suspend fun discardWorkout(workoutId: Long) {
        val workout = dao.getWorkout(workoutId)
        if (workout?.restEndsAtEpochMilli != null) {
            gymRestAlarms.onRestAbandoned()
        }
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
        plannedSetCount: Int,
        skipped: Boolean,
        completedAtEpochMilli: Long?,
        routineExerciseId: Long?,
        exerciseStableKey: String?,
        canonicalExerciseId: String,
    ): Long {
        val selection = resolveExerciseSelection(name, canonicalExerciseId)
        persistCustomExerciseIfNeeded(selection)
        require(trackingFields.isNotEmpty()) { "Pick at least one tracking field." }
        val existing = dao.getExercises(workoutId)
        existing.filter { it.sortOrder >= sortOrder }.forEach { exercise ->
            dao.updateExercise(exercise.copy(sortOrder = exercise.sortOrder + 1))
        }
        val exerciseId = dao.insertExercise(
            GymWorkoutExerciseEntity(
                workoutId = workoutId,
                exerciseId = selection.exerciseId,
                exerciseName = selection.displayName,
                sortOrder = sortOrder,
                note = GymLimits.clampNote(note),
                trackingFields = GymLogic.encodeTrackingFields(trackingFields),
                plannedSetCount = plannedSetCount,
                skipped = skipped,
                completedAtEpochMilli = completedAtEpochMilli,
                routineExerciseId = routineExerciseId,
                exerciseStableKey = exerciseStableKey,
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
                    skipped = set.skipped,
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

    override fun observeRoutines(): Flow<List<GymRoutine>> =
        routineDao.observeRoutines().mapLatest { entities ->
            entities.mapNotNull { loadRoutine(it.id) }
        }

    override suspend fun skipRoutineDay(routineId: Long, nowEpochMilli: Long) {
        advanceRoutineDay(routineId, nowEpochMilli)
    }

    override suspend fun confirmRestDay(routineId: Long, nowEpochMilli: Long) {
        advanceRoutineDay(routineId, nowEpochMilli)
    }

    override suspend fun setActiveRoutine(routineId: Long) {
        val profile = profileDao.getProfile() ?: return
        profileDao.upsert(profile.copy(activeGymRoutineId = routineId))
    }

    override suspend fun dismissRoundFourCheckpoint(routineId: Long) {
        val routine = routineDao.getRoutine(routineId) ?: return
        routineDao.updateRoutine(
            routine.copy(
                roundFourCheckpointDismissed = true,
                updatedAtEpochMilli = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun resetAllRoundsCompleted() {
        routineDao.resetAllRoundsCompleted()
    }

    override fun observePrimaryRoutine(): Flow<GymRoutine?> =
        profileDao.observeProfile().flatMapLatest { profile ->
            val activeId = profile?.activeGymRoutineId
            if (activeId != null) {
                observeRoutineWithDays(activeId)
            } else {
                routineDao.observePrimaryRoutine().flatMapLatest { entity ->
                    if (entity == null) {
                        flowOf(null)
                    } else {
                        observeRoutineWithDays(entity.id)
                    }
                }
            }
        }

    private fun observeRoutineWithDays(routineId: Long): Flow<GymRoutine?> =
        combine(
            routineDao.observeRoutine(routineId),
            routineDao.observeDays(routineId),
        ) { entity, _ -> entity }
            .mapLatest { entity ->
                if (entity == null) null else loadRoutine(entity.id)
            }

    override suspend fun getPrimaryRoutine(): GymRoutine? = loadPrimaryRoutine()

    override suspend fun getRoutine(routineId: Long): GymRoutine? = loadRoutine(routineId)

    override suspend fun saveRoutine(routine: GymRoutine): Long {
        val now = System.currentTimeMillis()
        val name = routine.name.trim().ifEmpty { "Routine" }
        val days = routine.days.take(GymLimits.DAY_COUNT_MAX)
        require(days.isNotEmpty()) { "Add at least one day." }
        require(days.any { !it.isRestDay && it.exercises.isNotEmpty() }) {
            "Add at least one exercise."
        }
        val existingId = routine.id.takeIf { it > 0L }
        val routineId = if (existingId == null) {
            routineDao.insertRoutine(
                GymRoutineEntity(
                    name = name,
                    currentDayIndex = routine.currentDayIndex.coerceIn(0, days.lastIndex),
                    roundsCompleted = routine.roundsCompleted,
                    roundFourCheckpointDismissed = routine.roundFourCheckpointDismissed,
                    createdAtEpochMilli = now,
                    updatedAtEpochMilli = now,
                ),
            )
        } else {
            val existing = routineDao.getRoutine(existingId) ?: return existingId
            routineDao.updateRoutine(
                existing.copy(
                    name = name,
                    currentDayIndex = routine.currentDayIndex.coerceIn(0, days.lastIndex),
                    updatedAtEpochMilli = now,
                ),
            )
            existingId
        }
        replaceRoutineDays(routineId, days)
        val profile = profileDao.getProfile()
        if (profile != null && profile.activeGymRoutineId == null) {
            profileDao.upsert(profile.copy(activeGymRoutineId = routineId))
        }
        return routineId
    }

    override suspend fun deleteRoutine(routineId: Long) {
        routineDao.deleteRoutineCascade(routineId)
        val profile = profileDao.getProfile()
        if (profile?.activeGymRoutineId == routineId) {
            profileDao.upsert(profile.copy(activeGymRoutineId = null))
        }
    }

    override suspend fun isRoutineInActiveWorkout(routineId: Long): Boolean {
        val active = dao.getLatestByTypeAndStatus(
            GymWorkoutType.ROUTINE.name,
            GymWorkoutStatus.ACTIVE.name,
        ) ?: return false
        return active.routineId == routineId
    }

    override suspend fun setRoutineStarred(routineId: Long, starred: Boolean) {
        val routine = routineDao.getRoutine(routineId) ?: return
        val now = System.currentTimeMillis()
        routineDao.updateRoutine(
            routine.copy(
                starred = starred,
                starredAtEpochMilli = if (starred) now else null,
                updatedAtEpochMilli = now,
            ),
        )
    }

    override suspend fun getExerciseNameSuggestions(): List<String> =
        GymExerciseNameCatalog.mergeNames(
            routineNames = routineDao.getDistinctExerciseNames(),
            workoutNames = dao.getDistinctExerciseNames(),
        )

    override suspend fun searchExercises(
        query: String,
        muscleFilter: GymMuscleGroup?,
        equipmentFilter: GymEquipment?,
        limit: Int,
    ): List<GymExerciseSearchHit> {
        val customRecords = loadCustomExerciseRecords()
        val overrides = loadExerciseOverrideRecords()
        return GymExerciseNameCatalog.searchExercises(
            query = query,
            customExercises = customRecords,
            historicalNames = getExerciseNameSuggestions(),
            overridesById = overrides,
            muscleFilter = muscleFilter,
            equipmentFilter = equipmentFilter,
            limit = limit,
        )
    }

    override suspend fun browseExercises(
        query: String,
        muscleFilter: GymMuscleGroup?,
        equipmentFilter: GymEquipment?,
        limit: Int,
    ): List<GymExerciseSearchHit> {
        val customRecords = loadCustomExerciseRecords()
        val overrides = loadExerciseOverrideRecords()
        return GymExerciseNameCatalog.browseExercises(
            query = query,
            customExercises = customRecords,
            overridesById = overrides,
            muscleFilter = muscleFilter,
            equipmentFilter = equipmentFilter,
            limit = limit,
        )
    }

    override suspend fun getExerciseMetadata(exerciseId: String): GymExerciseMetadata? {
        val trimmed = exerciseId.trim()
        if (trimmed.isEmpty()) return null
        if (GymExerciseIdentity.isBuiltinId(trimmed)) {
            val builtin = GymBuiltinExerciseCatalog.byId(trimmed) ?: return null
            val override = exerciseOverrideDao.getByExerciseId(trimmed)
            return GymExerciseMetadataResolver.resolveBuiltin(builtin, override)
        }
        if (GymExerciseIdentity.isCustomId(trimmed)) {
            val custom = customExerciseDao.getById(trimmed) ?: return null
            return GymExerciseMetadataResolver.resolveCustom(custom)
        }
        return null
    }

    override suspend fun saveBuiltinExerciseOverride(
        exerciseId: String,
        displayName: String?,
        primaryMuscle: GymMuscleGroup?,
        secondaryMuscles: List<GymMuscleGroup>,
        equipment: GymEquipment?,
    ) {
        require(GymExerciseIdentity.isBuiltinId(exerciseId)) { "Override target must be a built-in exercise ID." }
        val builtin = GymBuiltinExerciseCatalog.byId(exerciseId)
            ?: error("Unknown built-in exercise ID.")
        val entity = GymBuiltinExerciseOverridePolicy.buildOverrideEntity(
            builtin = builtin,
            displayName = displayName,
            primaryMuscle = primaryMuscle,
            secondaryMuscles = secondaryMuscles,
            equipment = equipment,
            nowEpochMilli = System.currentTimeMillis(),
        )
        if (entity == null) {
            exerciseOverrideDao.delete(exerciseId)
        } else {
            exerciseOverrideDao.upsert(entity)
        }
    }

    override suspend fun clearBuiltinExerciseOverride(exerciseId: String) {
        require(GymExerciseIdentity.isBuiltinId(exerciseId)) { "Override target must be a built-in exercise ID." }
        exerciseOverrideDao.delete(exerciseId)
    }

    override suspend fun saveCustomExerciseMetadata(
        exerciseId: String,
        displayName: String?,
        primaryMuscle: GymMuscleGroup?,
        secondaryMuscles: List<GymMuscleGroup>,
        equipment: GymEquipment?,
    ) {
        val existing = customExerciseDao.getById(exerciseId) ?: return
        val resolvedName = displayName?.trim()?.takeIf { it.isNotEmpty() } ?: existing.displayName
        customExerciseDao.insert(
            existing.copy(
                displayName = resolvedName,
                normalizedKey = GymExerciseNormalizer.normalizeKey(resolvedName),
                primaryMuscle = primaryMuscle?.name,
                secondaryMuscles = GymExerciseMetadataCodec.encodeMuscles(secondaryMuscles),
                equipment = GymExerciseMetadataCodec.encodeEquipment(equipment),
            ),
        )
    }

    override suspend fun deleteCustomExercise(exerciseId: String) {
        require(GymExerciseIdentity.isCustomId(exerciseId)) {
            "Only custom exercises can be deleted from the library."
        }
        customExerciseDao.deleteById(exerciseId)
    }

    override suspend fun createCustomExercise(
        displayName: String,
        primaryMuscle: GymMuscleGroup?,
        secondaryMuscles: List<GymMuscleGroup>,
        equipment: GymEquipment?,
    ): GymExerciseSelection {
        val trimmed = displayName.trim()
        require(trimmed.isNotEmpty()) { "Name can't be empty." }
        GymBuiltinExerciseCatalog.resolveExact(trimmed)?.let { builtin ->
            return GymExerciseSelection(
                exerciseId = builtin.id,
                displayName = builtin.canonicalName,
                isCustom = false,
            )
        }
        val normalizedKey = GymExerciseNormalizer.normalizeKey(trimmed)
        customExerciseDao.getByNormalizedKey(normalizedKey)?.let { existing ->
            return GymExerciseSelection(
                exerciseId = existing.id,
                displayName = existing.displayName,
                isCustom = true,
            )
        }
        val customId = GymExerciseIdentity.newCustomId()
        val record = GymCustomExerciseRecord(
            id = customId,
            displayName = trimmed,
            normalizedKey = normalizedKey,
            createdAtEpochMilli = System.currentTimeMillis(),
            primaryMuscle = primaryMuscle,
            secondaryMuscles = secondaryMuscles,
            equipment = equipment,
        )
        customExerciseDao.insert(record.toEntity())
        return GymExerciseSelection(
            exerciseId = record.id,
            displayName = record.displayName,
            isCustom = true,
        )
    }

    override suspend fun listLibraryExercises(
        query: String,
        muscleFilter: GymMuscleGroup?,
        equipmentFilter: GymEquipment?,
        sourceFilter: GymLibrarySourceFilter,
    ): List<GymLibraryExercise> = GymExerciseLibrary.listExercises(
        query = query,
        customExercises = loadCustomExerciseRecords(),
        overridesById = loadExerciseOverrideRecords(),
        muscleFilter = muscleFilter,
        equipmentFilter = equipmentFilter,
        sourceFilter = sourceFilter,
    )

    override suspend fun getLibraryExercise(exerciseId: String): GymLibraryExercise? =
        GymExerciseLibrary.getExercise(
            exerciseId = exerciseId,
            customExercises = loadCustomExerciseRecords(),
            overridesById = loadExerciseOverrideRecords(),
        )

    override suspend fun resolveExerciseSelection(
        displayName: String,
        canonicalExerciseId: String,
    ): GymExerciseSelection {
        val customByKey = loadCustomExerciseRecords().associateBy { it.normalizedKey }
        return GymExerciseIdentity.resolveFromSelection(
            exerciseId = canonicalExerciseId,
            displayName = displayName,
            existingCustomByKey = customByKey,
        )
    }

    override suspend fun previousOccurrenceSeeds(
        session: GymWorkoutSession,
    ): Map<Long, GymSetMeasurements> {
        val routineId = session.routineId ?: return emptyMap()
        val dayIndex = session.dayIndex ?: return emptyMap()
        val previous = dao.getLatestCompletedRoutineDay(
            type = GymWorkoutType.ROUTINE.name,
            status = GymWorkoutStatus.COMPLETED.name,
            routineId = routineId,
            dayIndex = dayIndex,
        ) ?: return emptyMap()
        if (previous.id == session.id) return emptyMap()
        val previousSession = loadSession(previous)
        return session.exercises.mapNotNull { exercise ->
            val match = GymLogic.matchPreviousExercise(
                previousExercises = previousSession.exercises,
                exerciseId = exercise.exerciseId,
                routineExerciseId = exercise.routineExerciseId,
                exerciseStableKey = exercise.exerciseStableKey,
                name = exercise.name,
            )
            val last = GymLogic.lastSavedSet(match?.sets.orEmpty()) ?: return@mapNotNull null
            exercise.id to last.measurements
        }.toMap()
    }

    override suspend fun previousPerformanceSeeds(
        session: GymWorkoutSession,
    ): Map<Long, GymSetMeasurements> {
        val occurrenceSeeds = if (session.type == GymWorkoutType.ROUTINE) {
            previousOccurrenceSeeds(session)
        } else {
            emptyMap()
        }
        return session.exercises.mapNotNull { exercise ->
            val canonicalId = exercise.exerciseId.trim()
            val seed = if (canonicalId.isNotEmpty()) {
                lastSavedSetForCanonicalExercise(canonicalId, excludeWorkoutId = session.id)
            } else {
                null
            }
            val measurements = occurrenceSeeds[exercise.id]
                ?: seed
                ?: return@mapNotNull null
            exercise.id to measurements
        }.toMap()
    }

    private suspend fun lastSavedSetForCanonicalExercise(
        canonicalExerciseId: String,
        excludeWorkoutId: Long,
    ): GymSetMeasurements? {
        val entity = dao.getLatestCompletedExerciseByCanonicalId(
            status = GymWorkoutStatus.COMPLETED.name,
            exerciseId = canonicalExerciseId,
            excludeWorkoutId = excludeWorkoutId,
        ) ?: return null
        val last = GymLogic.lastSavedSet(
            dao.getSets(entity.id).map { it.toDomain() },
        ) ?: return null
        return last.measurements
    }

    private suspend fun loadCustomExerciseRecords(): List<GymCustomExerciseRecord> =
        customExerciseDao.getAll().map { it.toRecord() }

    private suspend fun loadExerciseOverrideRecords(): Map<String, GymExerciseOverrideRecord> =
        exerciseOverrideDao.getAll().associate { entity -> entity.exerciseId to entity.toRecord() }

    private suspend fun persistCustomExerciseIfNeeded(selection: GymExerciseSelection) {
        if (!selection.isCustom) return
        val normalizedKey = GymExerciseNormalizer.normalizeKey(selection.displayName)
        val existing = customExerciseDao.getByNormalizedKey(normalizedKey)
        if (existing != null) return
        customExerciseDao.insert(
            GymCustomExerciseEntity(
                id = selection.exerciseId,
                displayName = selection.displayName,
                normalizedKey = normalizedKey,
                createdAtEpochMilli = System.currentTimeMillis(),
            ),
        )
    }

    private suspend fun loadPrimaryRoutine(): GymRoutine? {
        val activeId = profileDao.getProfile()?.activeGymRoutineId
        val entity = if (activeId != null) {
            routineDao.getRoutine(activeId)
        } else {
            routineDao.getPrimaryRoutine()
        } ?: return null
        return loadRoutine(entity.id)
    }

    private suspend fun advanceRoutineDay(routineId: Long, nowEpochMilli: Long) {
        val routine = routineDao.getRoutine(routineId) ?: return
        val days = routineDao.getDays(routineId)
        if (days.isEmpty()) return
        val roundCompleted = GymLogic.cycleCompletesAfterDay(routine.currentDayIndex, days.size)
        val nextIndex = GymLogic.nextDayIndex(routine.currentDayIndex, days.size)
        val roundsCompleted = if (roundCompleted) {
            routine.roundsCompleted + 1
        } else {
            routine.roundsCompleted
        }
        routineDao.updateRoutine(
            routine.copy(
                currentDayIndex = nextIndex,
                roundsCompleted = roundsCompleted,
                updatedAtEpochMilli = nowEpochMilli,
            ),
        )
    }

    private suspend fun loadRoutine(routineId: Long): GymRoutine? {
        val entity = routineDao.getRoutine(routineId) ?: return null
        val days = routineDao.getDays(routineId).map { day ->
            day.toDomain(routineDao.getExercises(day.id).map { it.toDomain() })
        }
        return entity.toDomain(days)
    }

    private suspend fun replaceRoutineDays(routineId: Long, days: List<GymRoutineDay>) {
        val existingDays = routineDao.getDays(routineId)
        existingDays.forEach { day ->
            routineDao.deleteExercisesForDay(day.id)
            routineDao.deleteDay(day.id)
        }
        days.forEachIndexed { index, day ->
            val dayId = routineDao.insertDay(
                GymRoutineDayEntity(
                    routineId = routineId,
                    dayIndex = index,
                    name = day.name.trim(),
                    isRestDay = day.isRestDay,
                ),
            )
            if (!day.isRestDay) {
                day.exercises.forEachIndexed { exerciseIndex, exercise ->
                    val fields = exercise.trackingFields.ifEmpty {
                        setOf(TrackingField.WEIGHT, TrackingField.REPS)
                    }
                    require(exercise.name.trim().isNotEmpty()) { "Name can't be empty." }
                    val stableKey = exercise.stableKey.ifBlank { UUID.randomUUID().toString() }
                    val selection = resolveExerciseSelection(
                        displayName = exercise.name,
                        canonicalExerciseId = exercise.exerciseId,
                    )
                    persistCustomExerciseIfNeeded(selection)
                    routineDao.insertExercise(
                        GymRoutineExerciseEntity(
                            dayId = dayId,
                            stableKey = stableKey,
                            exerciseId = selection.exerciseId,
                            name = selection.displayName,
                            trackingFields = GymLogic.encodeTrackingFields(fields),
                            sortOrder = exerciseIndex,
                            setCount = GymLimits.clampSetCount(exercise.setCount),
                            note = GymLimits.clampNote(exercise.note),
                        ),
                    )
                }
            }
        }
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

    private suspend fun scheduleRestAlarm(
        workoutId: Long,
        restEndsAtEpochMilli: Long,
        nowEpochMilli: Long,
    ) {
        val request = restAlarmRequest(workoutId, restEndsAtEpochMilli) ?: return
        gymRestAlarms.onRestStarted(request, nowEpochMilli)
    }

    private suspend fun rescheduleRestAlarm(workoutId: Long, restEndsAtEpochMilli: Long) {
        val request = restAlarmRequest(workoutId, restEndsAtEpochMilli) ?: return
        gymRestAlarms.onRestExtended(request)
    }

    private suspend fun restAlarmRequest(
        workoutId: Long,
        restEndsAtEpochMilli: Long,
    ): GymRestAlarmRequest? {
        val entity = dao.getWorkout(workoutId) ?: return null
        return GymRestAlarmRequest.from(loadSession(entity), restEndsAtEpochMilli)
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
    routineId = routineId,
    dayIndex = dayIndex,
    restKind = runCatching { GymRestKind.valueOf(restKind) }.getOrDefault(GymRestKind.NONE),
)

private fun GymWorkoutExerciseEntity.toDomain(sets: List<GymWorkoutSet>) = GymWorkoutExercise(
    id = id,
    workoutId = workoutId,
    exerciseId = exerciseId,
    name = exerciseName,
    sortOrder = sortOrder,
    note = note,
    trackingFields = GymLogic.decodeTrackingFields(trackingFields),
    sets = sets,
    plannedSetCount = plannedSetCount,
    skipped = skipped,
    completedAtEpochMilli = completedAtEpochMilli,
    routineExerciseId = routineExerciseId,
    exerciseStableKey = exerciseStableKey,
)

private fun GymRoutineEntity.toDomain(days: List<GymRoutineDay>) = GymRoutine(
    id = id,
    name = name,
    currentDayIndex = currentDayIndex,
    roundsCompleted = roundsCompleted,
    roundFourCheckpointDismissed = roundFourCheckpointDismissed,
    starred = starred,
    starredAtEpochMilli = starredAtEpochMilli,
    updatedAtEpochMilli = updatedAtEpochMilli,
    days = days,
)

private fun GymRoutineDayEntity.toDomain(exercises: List<GymRoutineExercise>) = GymRoutineDay(
    id = id,
    routineId = routineId,
    dayIndex = dayIndex,
    name = name,
    isRestDay = isRestDay,
    exercises = exercises,
    localKey = if (id > 0L) "day-$id" else "",
)

private fun GymRoutineExerciseEntity.toDomain() = GymRoutineExercise(
    id = id,
    dayId = dayId,
    stableKey = stableKey,
    exerciseId = exerciseId,
    name = name,
    trackingFields = GymLogic.decodeTrackingFields(trackingFields),
    sortOrder = sortOrder,
    setCount = setCount,
    note = note,
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
    skipped = skipped,
)

private fun GymSetMeasurements.toEntity(
    id: Long,
    workoutExerciseId: Long,
    setNumber: Int,
    failure: Boolean,
    saved: Boolean,
    skipped: Boolean = false,
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
    skipped = skipped,
)

