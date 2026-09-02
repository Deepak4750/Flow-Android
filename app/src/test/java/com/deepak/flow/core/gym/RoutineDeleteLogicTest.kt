package com.deepak.flow.core.gym

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineDeleteLogicTest {

    @Test
    fun requestShowsConfirmForIdleRoutine() {
        val decision = RoutineDeleteLogic.request(
            routineId = 7L,
            routineName = " Push ",
            inActiveWorkout = false,
            routineMissing = false,
        )
        assertEquals(RoutineDeleteDecision.Confirm(7L, "Push"), decision)
    }

    @Test
    fun requestBlocksWhenRoutineIsInActiveWorkout() {
        val decision = RoutineDeleteLogic.request(
            routineId = 7L,
            routineName = "Push",
            inActiveWorkout = true,
            routineMissing = false,
        )
        assertEquals(RoutineDeleteDecision.Blocked, decision)
        assertTrue(RoutineDeleteLogic.BLOCKED_MESSAGE.isNotBlank())
    }

    @Test
    fun requestIgnoresMissingRoutine() {
        val decision = RoutineDeleteLogic.request(
            routineId = 7L,
            routineName = null,
            inActiveWorkout = false,
            routineMissing = true,
        )
        assertEquals(RoutineDeleteDecision.Ignore, decision)
    }

    @Test
    fun emptyNameFallsBackToRoutine() {
        val decision = RoutineDeleteLogic.request(
            routineId = 3L,
            routineName = "  ",
            inActiveWorkout = false,
            routineMissing = false,
        )
        assertEquals(RoutineDeleteDecision.Confirm(3L, "Routine"), decision)
    }

    @Test
    fun confirmFromEditUsesSameBlockPath() {
        assertEquals(
            RoutineDeleteDecision.Blocked,
            RoutineDeleteLogic.confirm(routineId = 4L, inActiveWorkout = true),
        )
        assertEquals(
            RoutineDeleteDecision.Confirm(4L, ""),
            RoutineDeleteLogic.confirm(routineId = 4L, inActiveWorkout = false),
        )
        assertEquals(
            RoutineDeleteDecision.Ignore,
            RoutineDeleteLogic.confirm(routineId = null, inActiveWorkout = false),
        )
    }
}
