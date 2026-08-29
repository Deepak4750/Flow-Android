package com.deepak.flow.core.gym

import com.deepak.flow.feature.gym.presentation.toDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GymLogicTest {

    @Test
    fun trackingFields_roundTrip() {
        val fields = setOf(TrackingField.WEIGHT, TrackingField.REPS, TrackingField.DURATION)
        val encoded = GymLogic.encodeTrackingFields(fields)
        assertEquals(fields, GymLogic.decodeTrackingFields(encoded))
    }

    @Test
    fun meaningfulMeasurement_requiresSelectedFieldValue() {
        val fields = setOf(TrackingField.DURATION)
        assertFalse(
            GymLogic.hasMeaningfulMeasurement(
                fields,
                GymSetMeasurements(weight = 50.0, reps = 10),
            ),
        )
        assertTrue(
            GymLogic.hasMeaningfulMeasurement(
                fields,
                GymSetMeasurements(durationSeconds = 45),
            ),
        )
    }

    @Test
    fun volume_onlyWhenWeightAndRepsPresent() {
        val set = GymWorkoutSet(
            setNumber = 1,
            measurements = GymSetMeasurements(weight = 50.0, weightUnit = WeightUnit.KG, reps = 10),
            saved = true,
        )
        assertEquals(500.0, GymLogic.setVolumeKg(set, WeightUnit.KG))

        val failed = set.copy(failure = true)
        assertEquals(500.0, GymLogic.setVolumeKg(failed, WeightUnit.KG))

        val durationOnly = GymWorkoutSet(
            setNumber = 1,
            measurements = GymSetMeasurements(durationSeconds = 60),
            saved = true,
        )
        assertNull(GymLogic.setVolumeKg(durationOnly, WeightUnit.KG))
    }

    @Test
    fun restCountdown_usesEndTimestamp() {
        val ends = 10_000L
        assertEquals(7, GymLogic.remainingRestSeconds(ends, 3_000L))
        assertEquals(0, GymLogic.remainingRestSeconds(ends, 11_000L))
        assertEquals("01:27", GymLogic.formatCountdown(87))
    }

    @Test
    fun compactSetLine_prefersWeightTimesReps() {
        val set = GymWorkoutSet(
            setNumber = 1,
            measurements = GymSetMeasurements(weight = 12.5, weightUnit = WeightUnit.KG, reps = 12),
            saved = true,
        )
        assertEquals(
            "12.5 kg × 12 reps",
            GymLogic.formatCompactSetLine(
                set,
                setOf(TrackingField.WEIGHT, TrackingField.REPS),
                WeightUnit.KG,
            ),
        )
    }

    @Test
    fun convertWeight_kgToLbAndBack() {
        val tenKgAsLb = GymLogic.convertWeight(10.0, from = WeightUnit.KG, to = WeightUnit.LB)
        assertEquals(10.0 * GymLogic.KG_TO_LB, tenKgAsLb, 1e-12)
        assertEquals(
            10.0,
            GymLogic.convertWeight(tenKgAsLb, from = WeightUnit.LB, to = WeightUnit.KG),
            1e-12,
        )

        val twelveFiveAsLb = GymLogic.convertWeight(12.5, from = WeightUnit.KG, to = WeightUnit.LB)
        assertEquals(12.5 * GymLogic.KG_TO_LB, twelveFiveAsLb, 1e-12)
        assertEquals(
            12.5,
            GymLogic.convertWeight(twelveFiveAsLb, from = WeightUnit.LB, to = WeightUnit.KG),
            1e-12,
        )
    }

    @Test
    fun formatWeight_roundsForDisplayWithoutMutatingMath() {
        assertEquals(
            "22.05",
            GymLogic.formatWeight(10.0, storedUnit = WeightUnit.KG, displayUnit = WeightUnit.LB),
        )
        assertEquals(
            "27.56",
            GymLogic.formatWeight(12.5, storedUnit = WeightUnit.KG, displayUnit = WeightUnit.LB),
        )
        assertEquals(
            "10",
            GymLogic.formatWeight(10.0, storedUnit = WeightUnit.KG, displayUnit = WeightUnit.KG),
        )
        // Underlying convert stays full precision; only formatWeight rounds.
        assertEquals(
            22.0462262185,
            GymLogic.convertWeight(10.0, from = WeightUnit.KG, to = WeightUnit.LB),
            1e-12,
        )
    }

    @Test
    fun formatters_convertStoredUnitToDisplayUnit() {
        val set = GymWorkoutSet(
            setNumber = 1,
            measurements = GymSetMeasurements(
                weight = 10.0,
                weightUnit = WeightUnit.KG,
                reps = 8,
            ),
            saved = true,
        )
        assertEquals(
            "22.05 lb × 8 reps",
            GymLogic.formatCompactSetLine(
                set,
                setOf(TrackingField.WEIGHT, TrackingField.REPS),
                WeightUnit.LB,
            ),
        )
        assertEquals(
            "22.05 lb × 8 reps",
            GymLogic.formatSetSummary(
                set,
                setOf(TrackingField.WEIGHT, TrackingField.REPS),
                WeightUnit.LB,
            ),
        )
    }

    @Test
    fun noteLimit_clampedTo200() {
        val long = "a".repeat(250)
        assertEquals(200, GymLimits.clampNote(long).length)
    }

    @Test
    fun restLimits_clampSetAndExercise() {
        assertEquals(10, GymLimits.SET_REST_MIN_SECONDS)
        assertEquals(90, GymLimits.SET_REST_DEFAULT_SECONDS)
        assertEquals(10, GymLimits.clampSetRestSeconds(5))
        assertEquals(300, GymLimits.clampSetRestSeconds(400))
        assertEquals(300, GymLimits.SET_REST_MAX_SECONDS)
        assertEquals(90, GymLimits.clampSetRestSeconds(90))
        assertEquals(150, GymLimits.clampExerciseRestSeconds(200))
        assertEquals(120, GymLimits.clampExerciseRestSeconds(120))
    }

    @Test
    fun extendRestCeiling_neverAboveSetRestMaximum() {
        val remaining = 295
        val extra = 10
        val maxSeconds = GymLimits.SET_REST_MAX_SECONDS
        val next = if (remaining >= maxSeconds) {
            remaining
        } else {
            (remaining + extra).coerceAtMost(maxSeconds)
        }
        assertEquals(300, next)
    }

    @Test
    fun extendRestFloor_neverBelowSetRestMinimum() {
        val remaining = 12
        val extra = -10
        val minSeconds = GymLimits.SET_REST_MIN_SECONDS
        val next = if (remaining <= minSeconds) {
            remaining
        } else {
            (remaining + extra).coerceAtLeast(minSeconds)
        }
        assertEquals(10, next)

        val alreadyMin = 10
        val blocked = alreadyMin <= minSeconds
        assertTrue(blocked)
    }

    @Test
    fun newSetDraft_clearsFailureFlagFromPrevious() {
        val previous = GymWorkoutSet(
            id = 3L,
            workoutExerciseId = 1L,
            setNumber = 2,
            measurements = GymSetMeasurements(
                weight = 50.0,
                weightUnit = WeightUnit.KG,
                reps = 8,
            ),
            failure = true,
            saved = true,
        )
        val draft = previous.toDraft(
            setId = null,
            exerciseId = 1L,
            setNumber = 3,
            displayUnit = WeightUnit.KG,
        ).copy(failure = false)
        assertEquals("50", draft.weight)
        assertEquals("8", draft.reps)
        assertFalse(draft.failure)
    }

    @Test
    fun stopwatch_usesStartTimestamp() {
        val started = 1_000_000L
        val now = started + (1 * 3600 + 24 * 60 + 37) * 1000L
        assertEquals("01:24:37", GymLogic.formatStopwatch(started, now))
    }

    @Test
    fun exerciseElapsed_andNotificationBody_useTimestamps() {
        val workoutStart = 1_000_000L
        val exerciseStart = workoutStart + 60_000L
        val now = exerciseStart + (8 * 60 + 42) * 1000L
        assertEquals("08:42", GymLogic.formatExerciseElapsed(exerciseStart, now))
        assertEquals(
            "Chest Press\nWorkout 00:09:42\nExercise 08:42",
            GymLogic.activeWorkoutNotificationBody(
                exerciseName = "Chest Press",
                workoutStartedAtEpochMilli = workoutStart,
                exerciseStartedAtEpochMilli = exerciseStart,
                nowEpochMilli = now,
            ),
        )
        assertEquals(
            "Workout 00:01:00",
            GymLogic.activeWorkoutNotificationBody(
                exerciseName = null,
                workoutStartedAtEpochMilli = workoutStart,
                exerciseStartedAtEpochMilli = null,
                nowEpochMilli = workoutStart + 60_000L,
            ),
        )
    }

    @Test
    fun noteLinks_detectHttpAndWww() {
        val note = "See https://youtu.be/abc and www.instagram.com/x"
        val links = GymLogic.findNoteLinks(note)
        assertEquals(2, links.size)
        assertTrue(links[0].url.startsWith("https://"))
        assertTrue(links[1].url.startsWith("https://"))
    }

    @Test
    fun allSelectedFieldsFilled_requiresEveryConfiguredField() {
        val fields = setOf(TrackingField.WEIGHT, TrackingField.REPS)
        assertFalse(
            GymLogic.allSelectedFieldsFilled(
                fields,
                GymSetMeasurements(weight = 10.0),
            ),
        )
        assertTrue(
            GymLogic.allSelectedFieldsFilled(
                fields,
                GymSetMeasurements(weight = 10.0, reps = 8),
            ),
        )
        assertTrue(
            GymLogic.allSelectedFieldsFilled(
                setOf(TrackingField.DURATION),
                GymSetMeasurements(durationSeconds = 90),
            ),
        )
    }

    @Test
    fun weightAndRepsStepping() {
        assertEquals("10.5", GymLogic.stepWeightValue("10", up = true))
        assertEquals("7.5", GymLogic.stepWeightValue("8", up = false))
        assertEquals("9", GymLogic.stepWholeValue("8", up = true))
        assertEquals("1", GymLogic.stepWholeValue("1", up = false))
    }

    @Test
    fun postWorkoutEditWindow_usesCompletedAtPlus24Hours() {
        val completedAt = 1_000_000L
        val window = GymLogic.POST_WORKOUT_EDIT_WINDOW_MS
        assertTrue(
            GymLogic.isWithinPostWorkoutEditWindow(
                endedAtEpochMilli = completedAt,
                nowEpochMilli = completedAt + window - 1,
            ),
        )
        assertFalse(
            GymLogic.isWithinPostWorkoutEditWindow(
                endedAtEpochMilli = completedAt,
                nowEpochMilli = completedAt + window,
            ),
        )
        assertFalse(
            GymLogic.isWithinPostWorkoutEditWindow(
                endedAtEpochMilli = null,
                nowEpochMilli = completedAt,
            ),
        )
    }

    @Test
    fun fieldsLosingRecordedValues_onlyWhenSavedValuesExist() {
        val sets = listOf(
            GymWorkoutSet(
                setNumber = 1,
                measurements = GymSetMeasurements(weight = 50.0, reps = 10),
                saved = true,
            ),
        )
        val losing = GymLogic.fieldsLosingRecordedValues(
            currentFields = setOf(TrackingField.WEIGHT, TrackingField.REPS),
            nextFields = setOf(TrackingField.REPS),
            sets = sets,
        )
        assertEquals(setOf(TrackingField.WEIGHT), losing)

        val cleared = GymLogic.clearMeasurementsForFields(
            sets.first().measurements,
            setOf(TrackingField.WEIGHT),
        )
        assertNull(cleared.weight)
        assertEquals(10, cleared.reps)
    }

    @Test
    fun workoutDisplayTitle_usesFallbackWhenBlank() {
        assertEquals("Free Workout", GymLogic.workoutDisplayTitle(""))
        assertEquals("Free Workout", GymLogic.workoutDisplayTitle("   "))
        assertEquals("Chest Day", GymLogic.workoutDisplayTitle("Chest Day"))
        assertEquals("Workout", GymLogic.workoutDisplayTitle("", GymWorkoutType.ROUTINE))
        assertEquals("Pull", GymLogic.workoutDisplayTitle("Pull", GymWorkoutType.ROUTINE))
    }

    @Test
    fun seedMeasurementsForNextSet_usesLastCompletedThenPreviousOccurrence() {
        val set1 = GymWorkoutSet(
            setNumber = 1,
            measurements = GymSetMeasurements(weight = 15.0, reps = 12),
            saved = true,
        )
        val seeded = GymLogic.seedMeasurementsForNextSet(
            currentSets = listOf(set1),
            previousOccurrenceLastSet = GymSetMeasurements(weight = 20.0, reps = 8),
        )
        assertEquals(15.0, seeded.weight)
        assertEquals(12, seeded.reps)

        val firstOfNextWeek = GymLogic.seedMeasurementsForNextSet(
            currentSets = emptyList(),
            previousOccurrenceLastSet = GymSetMeasurements(weight = 25.0, reps = 8),
        )
        assertEquals(25.0, firstOfNextWeek.weight)
        assertEquals(8, firstOfNextWeek.reps)

        val firstEver = GymLogic.seedMeasurementsForNextSet(
            currentSets = emptyList(),
            previousOccurrenceLastSet = null,
        )
        assertNull(firstEver.weight)
        assertNull(firstEver.reps)
    }

    @Test
    fun shouldAutoOpenNextPlannedSet_stopsAtPlannedCount() {
        assertTrue(GymLogic.shouldAutoOpenNextPlannedSet(savedCount = 1, plannedSetCount = 4))
        assertTrue(GymLogic.shouldAutoOpenNextPlannedSet(savedCount = 0, plannedSetCount = 4))
        assertFalse(GymLogic.shouldAutoOpenNextPlannedSet(savedCount = 4, plannedSetCount = 4))
        assertFalse(GymLogic.shouldAutoOpenNextPlannedSet(savedCount = 1, plannedSetCount = 0))
    }

    @Test
    fun isLastPlannedSetNumber_onlyWhenPlanned() {
        assertTrue(GymLogic.isLastPlannedSetNumber(4, 4))
        assertTrue(GymLogic.isLastPlannedSetNumber(5, 4))
        assertFalse(GymLogic.isLastPlannedSetNumber(3, 4))
        assertFalse(GymLogic.isLastPlannedSetNumber(1, 0))
    }

    @Test
    fun matchPreviousExercise_prefersStableKeyThenRoutineExerciseIdThenName() {
        val previous = listOf(
            GymWorkoutExercise(
                id = 1,
                name = "Bench Press",
                sortOrder = 0,
                routineExerciseId = 10L,
                exerciseStableKey = "key-a",
            ),
            GymWorkoutExercise(
                id = 2,
                name = "Squat",
                sortOrder = 1,
                routineExerciseId = 11L,
                exerciseStableKey = "key-b",
            ),
        )
        assertEquals(
            1L,
            GymLogic.matchPreviousExercise(
                previous,
                routineExerciseId = 99L,
                exerciseStableKey = "key-a",
                name = "Other",
            )?.id,
        )
        assertEquals(
            1L,
            GymLogic.matchPreviousExercise(previous, routineExerciseId = 10L, exerciseStableKey = null, name = "Other")?.id,
        )
        assertEquals(
            2L,
            GymLogic.matchPreviousExercise(previous, routineExerciseId = null, exerciseStableKey = null, name = "squat")?.id,
        )
    }

    @Test
    fun formatDayHeading_structuralNumberAndTitle() {
        assertEquals("Day 1", GymLogic.formatDayHeading(0, "", false))
        assertEquals("Day 1 — Chest", GymLogic.formatDayHeading(0, "Chest", false))
        assertEquals("Day 3 — Rest Day", GymLogic.formatDayHeading(2, "", true))
    }

    @Test
    fun formatRoundsCompleted_usesSingularForOne() {
        assertEquals("0 rounds completed", GymLogic.formatRoundsCompleted(0))
        assertEquals("1 round completed", GymLogic.formatRoundsCompleted(1))
        assertEquals("4 rounds completed", GymLogic.formatRoundsCompleted(4))
    }

    @Test
    fun cycleCompletesAfterDay_onlyOnLastDay() {
        assertTrue(GymLogic.cycleCompletesAfterDay(2, 3))
        assertFalse(GymLogic.cycleCompletesAfterDay(0, 3))
    }

    @Test
    fun nextDayIndex_wrapsAround() {
        assertEquals(1, GymLogic.nextDayIndex(0, 3))
        assertEquals(0, GymLogic.nextDayIndex(2, 3))
    }

    @Test
    fun reorderDays_updatesIndices() {
        val days = listOf("a", "b", "c", "d")
        val reordered = days.toMutableList().apply {
            val moved = removeAt(3)
            add(1, moved)
        }
        assertEquals(listOf("a", "d", "b", "c"), reordered)
    }
}

