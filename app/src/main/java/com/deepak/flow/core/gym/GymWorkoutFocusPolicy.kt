package com.deepak.flow.core.gym

/**
 * Focus labels and exercise-switch affordances for out-of-order routine workouts.
 */
object GymWorkoutFocusPolicy {
    fun hasActiveEditSession(
        composingExercise: Boolean,
        setEditorVisible: Boolean,
        awaitingNextAction: Boolean,
    ): Boolean = composingExercise || setEditorVisible || awaitingNextAction

    fun exerciseStatusLabel(
        exercise: GymWorkoutExercise,
        isFocused: Boolean,
        isEditable: Boolean,
        isActivelyEditing: Boolean,
    ): String? = when {
        exercise.skipped -> if (isFocused) "Skipped" else null
        exercise.completedAtEpochMilli != null -> null
        isFocused && isEditable -> "Current"
        else -> null
    }

    fun shouldOfferSwitchExercise(
        exercise: GymWorkoutExercise,
        isFocused: Boolean,
        hasFocusedExerciseInSession: Boolean,
        isResting: Boolean,
    ): Boolean =
        GymWorkoutSwitchPolicy.shouldShowSwitchAction(
            isFocused = isFocused,
            isSelectable = GymWorkoutExercisePolicy.isExerciseSelectable(exercise),
            hasFocusedExerciseInSession = hasFocusedExerciseInSession,
            isResting = isResting,
        )
}
