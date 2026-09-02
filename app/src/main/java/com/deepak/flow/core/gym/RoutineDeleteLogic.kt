package com.deepak.flow.core.gym

sealed interface RoutineDeleteDecision {
    data class Confirm(val routineId: Long, val name: String) : RoutineDeleteDecision
    data object Blocked : RoutineDeleteDecision
    data object Ignore : RoutineDeleteDecision
}

object RoutineDeleteLogic {
    const val BLOCKED_MESSAGE =
        "Finish or discard the active workout before deleting this routine."

    fun request(
        routineId: Long,
        routineName: String?,
        inActiveWorkout: Boolean,
        routineMissing: Boolean,
    ): RoutineDeleteDecision {
        if (routineMissing) return RoutineDeleteDecision.Ignore
        if (inActiveWorkout) return RoutineDeleteDecision.Blocked
        val name = routineName?.trim().orEmpty().ifEmpty { "Routine" }
        return RoutineDeleteDecision.Confirm(routineId = routineId, name = name)
    }

    fun confirm(
        routineId: Long?,
        inActiveWorkout: Boolean,
    ): RoutineDeleteDecision {
        if (routineId == null) return RoutineDeleteDecision.Ignore
        if (inActiveWorkout) return RoutineDeleteDecision.Blocked
        return RoutineDeleteDecision.Confirm(routineId = routineId, name = "")
    }
}
