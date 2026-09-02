package com.deepak.flow.core.gym

/**
 * Rules for when set measurements (weight, reps, etc.) may be edited during a live workout.
 * Having saved sets on an exercise does not mean the exercise is complete.
 */
object GymSetEditPolicy {
    fun hasSavedSetInfo(exercise: GymWorkoutExercise): Boolean =
        exercise.sets.any { it.saved }

    fun canEditSetMeasurements(
        exercise: GymWorkoutExercise,
        isResting: Boolean,
        isFocused: Boolean,
        setEditorVisible: Boolean,
    ): Boolean {
        if (isResting || !setEditorVisible || !isFocused) return false
        if (!GymWorkoutExercisePolicy.isExerciseEditable(exercise)) return false
        return true
    }

    fun canEditTrackingFields(exercise: GymWorkoutExercise): Boolean =
        GymWorkoutExercisePolicy.isExerciseEditable(exercise) && !hasSavedSetInfo(exercise)
}
