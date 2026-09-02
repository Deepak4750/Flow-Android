package com.deepak.flow.core.notification

interface WorkoutEventNotifierPort {
    fun onWorkoutStarted(
        workoutId: Long,
        workoutTitle: String,
        workoutType: com.deepak.flow.core.gym.GymWorkoutType,
        startedAtEpochMilli: Long,
    )

    fun onExerciseCompleted(
        exerciseName: String,
        workoutType: com.deepak.flow.core.gym.GymWorkoutType,
    )

    fun onRestStarted(
        exerciseName: String,
        restKind: com.deepak.flow.core.gym.GymRestKind,
        workoutType: com.deepak.flow.core.gym.GymWorkoutType,
    )

    fun onWorkoutCompleted(
        workoutTitle: String,
        workoutType: com.deepak.flow.core.gym.GymWorkoutType,
        durationSeconds: Int,
    )
}

object NoOpWorkoutEventNotifier : WorkoutEventNotifierPort {
    override fun onWorkoutStarted(
        workoutId: Long,
        workoutTitle: String,
        workoutType: com.deepak.flow.core.gym.GymWorkoutType,
        startedAtEpochMilli: Long,
    ) = Unit

    override fun onExerciseCompleted(
        exerciseName: String,
        workoutType: com.deepak.flow.core.gym.GymWorkoutType,
    ) = Unit

    override fun onRestStarted(
        exerciseName: String,
        restKind: com.deepak.flow.core.gym.GymRestKind,
        workoutType: com.deepak.flow.core.gym.GymWorkoutType,
    ) = Unit

    override fun onWorkoutCompleted(
        workoutTitle: String,
        workoutType: com.deepak.flow.core.gym.GymWorkoutType,
        durationSeconds: Int,
    ) = Unit
}
