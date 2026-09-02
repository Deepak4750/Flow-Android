package com.deepak.flow.core.gym

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GymWorkoutExercisePolicyTest {

    private fun exercise(
        id: Long,
        name: String,
        skipped: Boolean = false,
        completedAtEpochMilli: Long? = null,
        sortOrder: Int = id.toInt(),
    ) = GymWorkoutExercise(
        id = id,
        workoutId = 1L,
        name = name,
        sortOrder = sortOrder,
        skipped = skipped,
        completedAtEpochMilli = completedAtEpochMilli,
    )

    private fun session(vararg exercises: GymWorkoutExercise) = GymWorkoutSession(
        id = 1L,
        startedAtEpochMilli = 0L,
        exercises = exercises.toList(),
    )

    @Test
    fun incompleteExercise_isEditable() {
        val open = exercise(1L, "Bench Press")
        assertTrue(GymWorkoutExercisePolicy.isExerciseEditable(open))
        assertFalse(GymWorkoutExercisePolicy.isExerciseResolved(open))
    }

    @Test
    fun completedExercise_isNotEditable() {
        val done = exercise(1L, "Bench Press", completedAtEpochMilli = 100L)
        assertFalse(GymWorkoutExercisePolicy.isExerciseEditable(done))
        assertTrue(GymWorkoutExercisePolicy.isExerciseResolved(done))
    }

    @Test
    fun skippedExercise_isSelectableButNotEditable() {
        val skipped = exercise(1L, "Bench Press", skipped = true)
        assertTrue(GymWorkoutExercisePolicy.isExerciseSelectable(skipped))
        assertFalse(GymWorkoutExercisePolicy.isExerciseEditable(skipped))
        assertTrue(GymWorkoutExercisePolicy.isExerciseResolved(skipped))
    }

    @Test
    fun skippedExercise_isResolvedButNotCompletedData() {
        val skipped = exercise(1L, "Bench Press", skipped = true)
        assertFalse(GymWorkoutExercisePolicy.isExerciseEditable(skipped))
        assertTrue(GymWorkoutExercisePolicy.isExerciseResolved(skipped))
        assertTrue(skipped.skipped)
        assertNull(skipped.completedAtEpochMilli)
    }

    @Test
    fun untouchedExercise_doesNotCountAsResolved() {
        val untouched = exercise(2L, "Cable Fly")
        assertFalse(GymWorkoutExercisePolicy.isExerciseResolved(untouched))
    }

    @Test
    fun finishBlockedWhenAnyExerciseUnresolved() {
        val blocked = session(
            exercise(1L, "Bench Press", completedAtEpochMilli = 1L),
            exercise(2L, "Cable Fly", skipped = true),
            exercise(3L, "Squat", completedAtEpochMilli = 2L),
            exercise(4L, "Lat Pulldown"),
        )
        assertEquals(
            GymWorkoutExercisePolicy.WORKOUT_COMPLETION_BLOCK_MESSAGE,
            GymWorkoutExercisePolicy.workoutCompletionBlockReason(blocked, hasUnsavedComposerDraft = false),
        )
    }

    @Test
    fun finishAllowedWhenAllCompletedOrSkipped() {
        val allowed = session(
            exercise(1L, "Bench Press", completedAtEpochMilli = 1L),
            exercise(2L, "Cable Fly", skipped = true),
            exercise(3L, "Squat", completedAtEpochMilli = 2L),
            exercise(4L, "Lat Pulldown", completedAtEpochMilli = 3L),
        )
        assertNull(
            GymWorkoutExercisePolicy.workoutCompletionBlockReason(allowed, hasUnsavedComposerDraft = false),
        )
        assertTrue(GymWorkoutExercisePolicy.canCompleteWorkout(allowed, hasUnsavedComposerDraft = false))
    }

    @Test
    fun completingOneExercise_doesNotResolveOthers() {
        val exercises = listOf(
            exercise(1L, "Bench Press"),
            exercise(2L, "Cable Fly"),
            exercise(3L, "Squat", completedAtEpochMilli = 99L),
            exercise(4L, "Lat Pulldown"),
        )
        assertEquals(listOf(1L, 2L, 4L), GymWorkoutExercisePolicy.unresolvedExercises(exercises).map { it.id })
    }

    @Test
    fun firstIncompleteExercise_ignoresListOrderProgress() {
        val exercises = listOf(
            exercise(1L, "Bench Press", completedAtEpochMilli = 1L),
            exercise(2L, "Cable Fly"),
            exercise(3L, "Squat", completedAtEpochMilli = 2L),
            exercise(4L, "Lat Pulldown", sortOrder = 4),
        )
        assertEquals(2L, GymWorkoutExercisePolicy.firstIncompleteExercise(exercises)?.id)
    }

    @Test
    fun composerDraftBlocksFinishForFreeWorkout() {
        val allowed = session(exercise(1L, "Bench Press", completedAtEpochMilli = 1L))
        assertEquals(
            GymWorkoutExercisePolicy.WORKOUT_COMPLETION_BLOCK_MESSAGE,
            GymWorkoutExercisePolicy.workoutCompletionBlockReason(allowed, hasUnsavedComposerDraft = true),
        )
    }

    @Test
    fun nextUnfinishedExerciseIndex_advancesForwardThenWraps() {
        val exercises = listOf(
            exercise(1L, "Bench Press", completedAtEpochMilli = 1L),
            exercise(2L, "Cable Fly", completedAtEpochMilli = 2L),
            exercise(3L, "Squat"),
            exercise(4L, "Lat Pulldown"),
        )
        assertEquals(2, GymWorkoutExercisePolicy.nextUnfinishedExerciseIndex(exercises, fromIndex = 1))
        val wrapped = listOf(
            exercise(1L, "Bench Press"),
            exercise(2L, "Cable Fly", completedAtEpochMilli = 2L),
            exercise(3L, "Squat", completedAtEpochMilli = 3L),
        )
        assertEquals(0, GymWorkoutExercisePolicy.nextUnfinishedExerciseIndex(wrapped, fromIndex = 2))
    }
}
