package com.deepak.flow.core.gym

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GymWorkoutSwitchPolicyTest {

    private val weightReps = setOf(TrackingField.WEIGHT, TrackingField.REPS)

    private fun exercise(
        id: Long,
        plannedSetCount: Int = 0,
        skipped: Boolean = false,
        completedAtEpochMilli: Long? = null,
        sets: List<GymWorkoutSet> = emptyList(),
        trackingFields: Set<TrackingField> = weightReps,
    ) = GymWorkoutExercise(
        id = id,
        workoutId = 1L,
        name = "Exercise $id",
        sortOrder = id.toInt(),
        plannedSetCount = plannedSetCount,
        skipped = skipped,
        completedAtEpochMilli = completedAtEpochMilli,
        sets = sets,
        trackingFields = trackingFields,
    )

    private fun savedSet(number: Int, skipped: Boolean = false) = GymWorkoutSet(
        id = number.toLong(),
        workoutExerciseId = 1L,
        setNumber = number,
        saved = true,
        skipped = skipped,
    )

    private fun emptyDraft(setNumber: Int = 1) = GymSetDraftSnapshot(setNumber = setNumber)

    private fun canSwitch(
        active: GymWorkoutExercise?,
        target: GymWorkoutExercise,
        draft: GymSetDraftSnapshot = emptyDraft(),
        setEditorVisible: Boolean = false,
        awaitingNextAction: Boolean = false,
        composingExercise: Boolean = false,
        isResting: Boolean = false,
        trackingFields: Set<TrackingField> = weightReps,
    ) = GymWorkoutSwitchPolicy.canSwitchToTarget(
        activeExercise = active,
        targetExercise = target,
        trackingFields = trackingFields,
        setDraft = draft,
        setEditorVisible = setEditorVisible,
        awaitingNextAction = awaitingNextAction,
        composingExercise = composingExercise,
        isResting = isResting,
    )

    @Test
    fun autoOpenedEmptySet_enablesSwitch() {
        val active = exercise(id = 1L, plannedSetCount = 3)
        assertTrue(
            canSwitch(
                active = active,
                target = exercise(2L),
                draft = emptyDraft(setNumber = 1),
                setEditorVisible = true,
            ),
        )
    }

    @Test
    fun weightEnteredOnly_disablesSwitch() {
        val active = exercise(id = 1L, plannedSetCount = 3)
        assertFalse(
            canSwitch(
                active = active,
                target = exercise(2L),
                draft = emptyDraft(setNumber = 1).copy(weight = "10"),
                setEditorVisible = true,
            ),
        )
    }

    @Test
    fun repsEnteredOnly_disablesSwitch() {
        val active = exercise(id = 1L, plannedSetCount = 3)
        assertFalse(
            canSwitch(
                active = active,
                target = exercise(2L),
                draft = emptyDraft(setNumber = 1).copy(reps = "10"),
                setEditorVisible = true,
            ),
        )
    }

    @Test
    fun weightAndRepsEntered_disablesSwitch() {
        val active = exercise(id = 1L, plannedSetCount = 3)
        assertFalse(
            canSwitch(
                active = active,
                target = exercise(2L),
                draft = emptyDraft(setNumber = 1).copy(weight = "10", reps = "10"),
                setEditorVisible = true,
            ),
        )
    }

    @Test
    fun zeroWeightCountsAsEnteredData() {
        val active = exercise(id = 1L, plannedSetCount = 3)
        assertFalse(
            canSwitch(
                active = active,
                target = exercise(2L),
                draft = emptyDraft(setNumber = 1).copy(weight = "0"),
                setEditorVisible = true,
            ),
        )
    }

    @Test
    fun clearingDraftReEnablesSwitch() {
        val active = exercise(id = 1L, plannedSetCount = 3)
        assertTrue(
            canSwitch(
                active = active,
                target = exercise(2L),
                draft = emptyDraft(setNumber = 1),
                setEditorVisible = true,
            ),
        )
    }

    @Test
    fun completedSetsWithUnfinishedRequiredSet_disablesSwitch() {
        val active = exercise(
            id = 1L,
            plannedSetCount = 3,
            sets = listOf(savedSet(1), savedSet(2)),
        )
        assertFalse(
            canSwitch(
                active = active,
                target = exercise(2L),
                draft = emptyDraft(setNumber = 3),
                setEditorVisible = true,
            ),
        )
    }

    @Test
    fun allPlannedSetsRecorded_enablesSwitch() {
        val active = exercise(
            id = 1L,
            plannedSetCount = 3,
            sets = listOf(savedSet(1), savedSet(2), savedSet(3)),
        )
        assertTrue(
            canSwitch(
                active = active,
                target = exercise(2L),
                awaitingNextAction = true,
            ),
        )
    }

    @Test
    fun remainingSetSkipped_enablesSwitch() {
        val active = exercise(
            id = 1L,
            plannedSetCount = 3,
            sets = listOf(savedSet(1), savedSet(2), savedSet(3, skipped = true)),
        )
        assertTrue(
            canSwitch(
                active = active,
                target = exercise(2L),
            ),
        )
    }

    @Test
    fun skippedExercise_allowsSwitching() {
        val active = exercise(id = 1L, skipped = true)
        assertFalse(
            GymWorkoutSwitchPolicy.hasUnfinishedRequiredWork(
                exercise = active,
                trackingFields = weightReps,
                setDraft = emptyDraft(),
                setEditorVisible = true,
                awaitingNextAction = false,
                composingExercise = false,
                isResting = false,
            ),
        )
    }

    @Test
    fun switchDisabledDuringSetRest() {
        val active = exercise(
            id = 1L,
            plannedSetCount = 3,
            sets = listOf(savedSet(1), savedSet(2), savedSet(3)),
        )
        assertFalse(
            canSwitch(
                active = active,
                target = exercise(2L),
                isResting = true,
            ),
        )
    }

    @Test
    fun switchDisabledDuringExerciseRest() {
        val active = exercise(
            id = 1L,
            plannedSetCount = 2,
            sets = listOf(savedSet(1), savedSet(2)),
        )
        assertFalse(
            canSwitch(
                active = active,
                target = exercise(3L),
                isResting = true,
            ),
        )
    }

    @Test
    fun arbitraryTargetAllowedWhenSwitchPermitted() {
        val active = exercise(
            id = 1L,
            plannedSetCount = 2,
            sets = listOf(savedSet(1), savedSet(2)),
        )
        assertTrue(canSwitch(active = active, target = exercise(4L)))
        assertTrue(canSwitch(active = active, target = exercise(5L)))
    }

    @Test
    fun switchActionHiddenDuringRest() {
        assertFalse(
            GymWorkoutSwitchPolicy.shouldShowSwitchAction(
                isFocused = false,
                isSelectable = true,
                hasFocusedExerciseInSession = true,
                isResting = true,
            ),
        )
    }

    @Test
    fun labelsAreSwitchAndCurrent() {
        val open = exercise(1L)
        assertEquals(
            "Current",
            GymWorkoutFocusPolicy.exerciseStatusLabel(
                exercise = open,
                isFocused = true,
                isEditable = true,
                isActivelyEditing = true,
            ),
        )
    }
}
