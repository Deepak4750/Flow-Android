package com.deepak.flow.core.gym

/**
 * Live-workout exercise resolution: incomplete, completed, or explicitly skipped.
 */
object GymWorkoutExercisePolicy {
    fun isExerciseResolved(exercise: GymWorkoutExercise): Boolean =
        exercise.skipped || exercise.completedAtEpochMilli != null

    fun isExerciseSkipped(exercise: GymWorkoutExercise): Boolean = exercise.skipped

    /** Completed exercises are locked. Skipped and unfinished exercises stay selectable. */
    fun isExerciseSelectable(exercise: GymWorkoutExercise): Boolean =
        exercise.completedAtEpochMilli == null

    fun isExerciseEditable(exercise: GymWorkoutExercise): Boolean =
        !exercise.skipped && exercise.completedAtEpochMilli == null

    fun firstIncompleteExercise(exercises: List<GymWorkoutExercise>): GymWorkoutExercise? =
        exercises.firstOrNull { isExerciseEditable(it) }

    /**
     * Next editable exercise after [fromIndex], scanning forward then wrapping to the start.
     * Returns null when every exercise is completed or skipped.
     */
    fun nextUnfinishedExerciseIndex(
        exercises: List<GymWorkoutExercise>,
        fromIndex: Int,
    ): Int? {
        if (exercises.isEmpty()) return null
        val start = fromIndex.coerceIn(0, exercises.lastIndex)
        for (index in (start + 1) until exercises.size) {
            if (isExerciseEditable(exercises[index])) return index
        }
        for (index in 0 until start) {
            if (isExerciseEditable(exercises[index])) return index
        }
        return if (isExerciseEditable(exercises[start])) start else null
    }

    fun unresolvedExercises(exercises: List<GymWorkoutExercise>): List<GymWorkoutExercise> =
        exercises.filterNot { isExerciseResolved(it) }

    fun workoutCompletionBlockReason(
        session: GymWorkoutSession?,
        hasUnsavedComposerDraft: Boolean,
    ): String? {
        if (session == null) return null
        if (hasUnsavedComposerDraft) {
            return WORKOUT_COMPLETION_BLOCK_MESSAGE
        }
        if (unresolvedExercises(session.exercises).isNotEmpty()) {
            return WORKOUT_COMPLETION_BLOCK_MESSAGE
        }
        return null
    }

    fun canCompleteWorkout(
        session: GymWorkoutSession?,
        hasUnsavedComposerDraft: Boolean,
    ): Boolean = workoutCompletionBlockReason(session, hasUnsavedComposerDraft) == null

    const val WORKOUT_COMPLETION_BLOCK_MESSAGE =
        "Finish the remaining exercises or skip them before ending your workout."
}
