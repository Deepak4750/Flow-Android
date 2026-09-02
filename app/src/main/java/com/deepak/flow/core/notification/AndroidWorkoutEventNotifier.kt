package com.deepak.flow.core.notification

import android.content.Context
import com.deepak.flow.core.gym.GymRestKind
import com.deepak.flow.core.gym.GymWorkoutType

class AndroidWorkoutEventNotifier(
    private val appContext: Context,
) : WorkoutEventNotifierPort {
    override fun onWorkoutStarted(
        workoutId: Long,
        workoutTitle: String,
        workoutType: GymWorkoutType,
        startedAtEpochMilli: Long,
    ) {
        val snapshot = WorkoutEventNotificationCopy.started(
            workoutTitle = workoutTitle,
            workoutType = workoutType,
            startedAtEpochMilli = startedAtEpochMilli,
        )
        NotificationChannelManager.postWorkoutEventNotification(appContext, snapshot)
    }

    override fun onExerciseCompleted(exerciseName: String, workoutType: GymWorkoutType) {
        val snapshot = WorkoutEventNotificationCopy.exerciseCompleted(
            exerciseName = exerciseName,
            workoutType = workoutType,
        )
        NotificationChannelManager.postWorkoutEventNotification(appContext, snapshot)
    }

    override fun onRestStarted(
        exerciseName: String,
        restKind: GymRestKind,
        workoutType: GymWorkoutType,
    ) {
        val snapshot = WorkoutEventNotificationCopy.restStarted(
            exerciseName = exerciseName,
            restKind = restKind,
            workoutType = workoutType,
        )
        NotificationChannelManager.postWorkoutEventNotification(appContext, snapshot)
    }

    override fun onWorkoutCompleted(
        workoutTitle: String,
        workoutType: GymWorkoutType,
        durationSeconds: Int,
    ) {
        val snapshot = WorkoutEventNotificationCopy.workoutCompleted(
            workoutTitle = workoutTitle,
            workoutType = workoutType,
            durationSeconds = durationSeconds,
        )
        NotificationChannelManager.postWorkoutEventNotification(appContext, snapshot)
    }
}
