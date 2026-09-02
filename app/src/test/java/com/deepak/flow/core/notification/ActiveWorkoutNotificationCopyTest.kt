package com.deepak.flow.core.notification

import com.deepak.flow.core.gym.GymRestKind
import com.deepak.flow.core.gym.GymSetMeasurements
import com.deepak.flow.core.gym.GymWorkoutExercise
import com.deepak.flow.core.gym.GymWorkoutSession
import com.deepak.flow.core.gym.GymWorkoutSet
import com.deepak.flow.core.gym.GymWorkoutType
import com.deepak.flow.core.gym.TrackingField
import com.deepak.flow.core.gym.WeightUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveWorkoutNotificationCopyTest {

    private val startedAt = 1_000_000L
    private val now = startedAt + 120_000L

    @Test
    fun activeWorkout_showsExerciseAndSetProgress() {
        val session = sessionWithExercise(
            name = "Bench Press",
            plannedSets = 4,
            sets = listOf(
                savedSet(1, weight = 60.0, reps = 8),
                savedSet(2, weight = 60.0, reps = 8),
            ),
        )

        val snapshot = ActiveWorkoutNotificationCopy.fromSession(session, now)

        assertEquals("Flow", snapshot.contentTitle)
        assertEquals("Bench Press", snapshot.contentText)
        assertEquals("Set 2/4 · 60 kg × 8", snapshot.subText)
        assertTrue(snapshot.expandedText.contains("Workout in progress"))
        assertTrue(snapshot.expandedText.contains("Bench Press"))
        assertTrue(snapshot.expandedText.contains("Set 2/4 · 60 kg × 8"))
        assertFalse(snapshot.isResting)
    }

    @Test
    fun restWorkout_showsCountdownAndExerciseName() {
        val session = sessionWithExercise(
            name = "Bench Press",
            restEndsAt = now + 42_000L,
            restKind = GymRestKind.SET,
        )

        val snapshot = ActiveWorkoutNotificationCopy.fromSession(session, now)

        assertEquals("Rest · 00:42", snapshot.contentText)
        assertEquals("Bench Press", snapshot.subText)
        assertTrue(snapshot.isResting)
        assertTrue(snapshot.expandedText.contains("Rest · 00:42"))
    }

    @Test
    fun exerciseRest_showsNextExerciseWhenReliable() {
        val session = sessionWithExercise(
            name = "Bench Press",
            restEndsAt = now + 30_000L,
            restKind = GymRestKind.EXERCISE,
            completedAt = now - 5_000L,
            extraExercises = listOf(
                GymWorkoutExercise(
                    id = 2L,
                    workoutId = 1L,
                    name = "Cable Fly",
                    sortOrder = 1,
                ),
            ),
        )

        val snapshot = ActiveWorkoutNotificationCopy.fromSession(session, now)

        assertTrue(snapshot.expandedText.contains("Next: Cable Fly"))
        assertTrue(snapshot.fingerprint.contains("Cable Fly"))
    }

    @Test
    fun setRest_doesNotShowNextExercise() {
        val session = sessionWithExercise(
            name = "Bench Press",
            restEndsAt = now + 30_000L,
            restKind = GymRestKind.SET,
            extraExercises = listOf(
                GymWorkoutExercise(
                    id = 2L,
                    workoutId = 1L,
                    name = "Cable Fly",
                    sortOrder = 1,
                ),
            ),
        )

        val snapshot = ActiveWorkoutNotificationCopy.fromSession(session, now)

        assertFalse(snapshot.expandedText.contains("Next:"))
    }

    @Test
    fun fingerprint_changesEachRestSecond() {
        val session = sessionWithExercise(
            name = "Bench Press",
            restEndsAt = now + 42_000L,
            restKind = GymRestKind.SET,
        )
        val first = ActiveWorkoutNotificationCopy.fromSession(session, now)
        val second = ActiveWorkoutNotificationCopy.fromSession(session, now + 1_000L)
        assertTrue(first.fingerprint != second.fingerprint)
    }

    @Test
    fun fingerprint_stableForUnchangedActiveWorkout() {
        val session = sessionWithExercise(
            name = "Bench Press",
            plannedSets = 4,
            sets = listOf(savedSet(1, weight = 60.0, reps = 8)),
        )
        val first = ActiveWorkoutNotificationCopy.fromSession(session, now)
        val second = ActiveWorkoutNotificationCopy.fromSession(session, now + 5_000L)
        assertEquals(first.fingerprint, second.fingerprint)
    }

    @Test
    fun missingExerciseData_fallsBackSafely() {
        val session = GymWorkoutSession(
            id = 1L,
            type = GymWorkoutType.FREE,
            startedAtEpochMilli = startedAt,
            exercises = emptyList(),
        )

        val snapshot = ActiveWorkoutNotificationCopy.fromSession(session, now)

        assertEquals("Workout in progress", snapshot.contentText)
        assertNull(snapshot.subText)
        assertTrue(snapshot.expandedText.contains("Workout in progress"))
    }

    @Test
    fun setProgressLine_withoutSavedSetsAndNoPlan_returnsNull() {
        val exercise = GymWorkoutExercise(
            id = 1L,
            workoutId = 1L,
            name = "Bench Press",
            sortOrder = 0,
        )
        assertNull(ActiveWorkoutNotificationCopy.setProgressLine(exercise, WeightUnit.KG))
    }

    @Test
    fun setProgressLine_withPlannedSetsAndNoSavedSets_showsFirstSet() {
        val exercise = GymWorkoutExercise(
            id = 1L,
            workoutId = 1L,
            name = "Bench Press",
            sortOrder = 0,
            plannedSetCount = 4,
            trackingFields = setOf(TrackingField.WEIGHT, TrackingField.REPS),
        )
        assertEquals("Set 1/4", ActiveWorkoutNotificationCopy.setProgressLine(exercise, WeightUnit.KG))
    }

    private fun sessionWithExercise(
        name: String,
        plannedSets: Int = 0,
        sets: List<GymWorkoutSet> = emptyList(),
        restEndsAt: Long? = null,
        restKind: GymRestKind = GymRestKind.NONE,
        completedAt: Long? = null,
        extraExercises: List<GymWorkoutExercise> = emptyList(),
    ): GymWorkoutSession {
        val exercise = GymWorkoutExercise(
            id = 1L,
            workoutId = 1L,
            name = name,
            sortOrder = 0,
            plannedSetCount = plannedSets,
            trackingFields = setOf(TrackingField.WEIGHT, TrackingField.REPS),
            sets = sets,
            completedAtEpochMilli = completedAt,
        )
        return GymWorkoutSession(
            id = 1L,
            type = GymWorkoutType.ROUTINE,
            startedAtEpochMilli = startedAt,
            currentExerciseIndex = 0,
            restEndsAtEpochMilli = restEndsAt,
            restKind = restKind,
            exercises = listOf(exercise) + extraExercises,
        )
    }

    private fun savedSet(
        number: Int,
        weight: Double,
        reps: Int,
    ): GymWorkoutSet = GymWorkoutSet(
        id = number.toLong(),
        workoutExerciseId = 1L,
        setNumber = number,
        measurements = GymSetMeasurements(
            weight = weight,
            weightUnit = WeightUnit.KG,
            reps = reps,
        ),
        saved = true,
    )
}
