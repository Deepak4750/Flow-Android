package com.deepak.flow.core.gym

/**
 * Library-level rules for custom exercise lifecycle. Workout/routine rows keep their
 * canonical exerciseId snapshots; deleting a custom exercise only removes library metadata.
 */
object GymCustomExercisePolicy {
    fun canDeleteFromLibrary(exerciseId: String): Boolean =
        GymExerciseIdentity.isCustomId(exerciseId)

    fun canEditInLibrary(exercise: GymLibraryExercise): Boolean = exercise.isCustom
}
