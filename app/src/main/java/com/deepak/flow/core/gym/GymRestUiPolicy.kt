package com.deepak.flow.core.gym

/**
 * Presentation rules for inline rest UI. Alarm/scheduling logic lives in the repository.
 */
object GymRestUiPolicy {
    fun isRestActive(
        restEndsAtEpochMilli: Long?,
        nowEpochMilli: Long,
        restUiSuppressed: Boolean = false,
    ): Boolean {
        if (restUiSuppressed) return false
        if (restEndsAtEpochMilli == null) return false
        return GymLogic.remainingRestSeconds(restEndsAtEpochMilli, nowEpochMilli) > 0
    }

    fun shouldShowSetEditor(
        exercise: GymWorkoutExercise,
        isResting: Boolean,
        isFocused: Boolean,
        setEditorVisible: Boolean,
    ): Boolean = GymSetEditPolicy.canEditSetMeasurements(
        exercise = exercise,
        isResting = isResting,
        isFocused = isFocused,
        setEditorVisible = setEditorVisible,
    )

    fun shouldShowAwaitingActions(
        isResting: Boolean,
        isFocused: Boolean,
        isEditable: Boolean,
        awaitingNextAction: Boolean,
    ): Boolean = !isResting && isFocused && isEditable && awaitingNextAction

    fun shouldOpenEditorAfterExerciseSelect(isResting: Boolean): Boolean = !isResting

    fun exerciseProgressLabel(
        exercise: GymWorkoutExercise,
        isFocused: Boolean,
        isEditable: Boolean,
        isActivelyEditing: Boolean,
    ): String? = GymWorkoutFocusPolicy.exerciseStatusLabel(
        exercise = exercise,
        isFocused = isFocused,
        isEditable = isEditable,
        isActivelyEditing = isActivelyEditing,
    )
}
