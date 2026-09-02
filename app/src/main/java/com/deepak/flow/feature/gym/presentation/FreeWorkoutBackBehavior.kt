package com.deepak.flow.feature.gym.presentation

/**
 * Maps workout phase to the action FlowShell back should take.
 * Rest must not end on system back; only the explicit Skip control ends rest.
 */
internal enum class FreeWorkoutBackEffect {
    Leave,
    CancelExerciseEditor,
    DismissEndOptions,
    FinishAndLeave,
    OpenEndOptions,
    None,
}

internal fun freeWorkoutBackEffect(phase: FreeWorkoutPhase): FreeWorkoutBackEffect = when (phase) {
    FreeWorkoutPhase.SETUP -> FreeWorkoutBackEffect.Leave
    FreeWorkoutPhase.EDIT_EXERCISE -> FreeWorkoutBackEffect.CancelExerciseEditor
    FreeWorkoutPhase.RESTING -> FreeWorkoutBackEffect.None
    FreeWorkoutPhase.END_OPTIONS -> FreeWorkoutBackEffect.DismissEndOptions
    FreeWorkoutPhase.COMPLETED -> FreeWorkoutBackEffect.FinishAndLeave
    FreeWorkoutPhase.SESSION -> FreeWorkoutBackEffect.OpenEndOptions
}
