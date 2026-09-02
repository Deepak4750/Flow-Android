package com.deepak.flow.core.notification

import com.deepak.flow.core.gym.GymRestKind
import com.deepak.flow.core.gym.GymWorkoutType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutEventNotificationCopyTest {

    @Test
    fun workoutStarted_includesTitleAndDuration() {
        val startedAt = 1_000_000L
        val snapshot = WorkoutEventNotificationCopy.started(
            workoutTitle = "Push Day",
            workoutType = GymWorkoutType.ROUTINE,
            startedAtEpochMilli = startedAt,
            nowEpochMilli = startedAt + 90_000L,
        )

        assertEquals(WorkoutEventKind.STARTED, snapshot.kind)
        assertEquals("Workout started", snapshot.text)
        assertTrue(snapshot.expandedText.contains("Push Day"))
        assertTrue(snapshot.expandedText.contains("Duration: 00:01:30"))
        assertTrue(snapshot.useChronometer)
    }

    @Test
    fun restStarted_usesSetOrExerciseHeadline() {
        val setRest = WorkoutEventNotificationCopy.restStarted(
            exerciseName = "Bench Press",
            restKind = GymRestKind.SET,
            workoutType = GymWorkoutType.FREE,
        )
        val exerciseRest = WorkoutEventNotificationCopy.restStarted(
            exerciseName = "Bench Press",
            restKind = GymRestKind.EXERCISE,
            workoutType = GymWorkoutType.FREE,
        )

        assertEquals("Set rest started", setRest.text)
        assertEquals("Exercise rest started", exerciseRest.text)
    }

    @Test
    fun workoutCompleted_includesDuration() {
        val snapshot = WorkoutEventNotificationCopy.workoutCompleted(
            workoutTitle = "Leg Day",
            workoutType = GymWorkoutType.ROUTINE,
            durationSeconds = 3_661,
        )
        assertEquals("Workout completed", snapshot.text)
        assertTrue(snapshot.expandedText.contains("Leg Day"))
        assertTrue(snapshot.expandedText.contains("Duration: 01:01:01"))
    }
}
