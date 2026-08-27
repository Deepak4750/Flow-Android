package com.deepak.flow.core.gym

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
            measurements = GymSetMeasurements(weight = 12.5, reps = 12),
            saved = true,
        )
        assertEquals(
            "12.5 × 12",
            GymLogic.formatCompactSetLine(
                set,
                setOf(TrackingField.WEIGHT, TrackingField.REPS),
                WeightUnit.KG,
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
        assertEquals(10, GymLimits.clampSetRestSeconds(5))
        assertEquals(120, GymLimits.clampSetRestSeconds(200))
        assertEquals(90, GymLimits.clampSetRestSeconds(90))
        assertEquals(150, GymLimits.clampExerciseRestSeconds(200))
        assertEquals(120, GymLimits.clampExerciseRestSeconds(120))
    }

    @Test
    fun stopwatch_usesStartTimestamp() {
        val started = 1_000_000L
        val now = started + (1 * 3600 + 24 * 60 + 37) * 1000L
        assertEquals("01:24:37", GymLogic.formatStopwatch(started, now))
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
    fun summarize_countsSavedWorkOnly() {
        val session = GymWorkoutSession(
            startedAtEpochMilli = 0L,
            endedAtEpochMilli = 90_000L,
            exercises = listOf(
                GymWorkoutExercise(
                    name = "Bench",
                    sortOrder = 0,
                    trackingFields = setOf(TrackingField.WEIGHT, TrackingField.REPS),
                    sets = listOf(
                        GymWorkoutSet(
                            setNumber = 1,
                            measurements = GymSetMeasurements(
                                weight = 40.0,
                                weightUnit = WeightUnit.KG,
                                reps = 8,
                            ),
                            saved = true,
                        ),
                        GymWorkoutSet(
                            setNumber = 2,
                            measurements = GymSetMeasurements(weight = 40.0, reps = 8),
                            saved = false,
                        ),
                    ),
                ),
            ),
        )
        val summary = GymLogic.summarize(session)
        assertEquals(90L, summary.durationSeconds)
        assertEquals(1, summary.exerciseCount)
        assertEquals(1, summary.setCount)
        assertEquals(320.0, summary.volumeKg)
    }
}

