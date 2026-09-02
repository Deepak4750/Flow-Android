package com.deepak.flow.core.gym

/**
 * Pure transition rules after rest completes. Alarm scheduling stays in the repository;
 * this only decides which exercise/editor state the live workout should enter.
 */
object GymRestCompletionPolicy {
    fun nextExerciseIndexAfterExerciseRest(
        exercises: List<GymWorkoutExercise>,
        fromIndex: Int,
    ): Int? = GymWorkoutExercisePolicy.nextUnfinishedExerciseIndex(exercises, fromIndex)

    fun shouldOpenSetEditorAfterSetRest(exercise: GymWorkoutExercise?): Boolean =
        exercise != null && GymWorkoutExercisePolicy.isExerciseEditable(exercise)
}
