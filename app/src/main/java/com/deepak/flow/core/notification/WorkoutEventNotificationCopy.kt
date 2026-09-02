package com.deepak.flow.core.notification

import com.deepak.flow.core.gym.GymLogic
import com.deepak.flow.core.gym.GymRestKind
import com.deepak.flow.core.gym.GymWorkoutType
import com.deepak.flow.core.widget.WidgetLaunch

enum class WorkoutEventKind {
    STARTED,
    EXERCISE_COMPLETED,
    REST_STARTED,
    WORKOUT_COMPLETED,
}

data class WorkoutEventSnapshot(
    val kind: WorkoutEventKind,
    val notificationId: Int,
    val title: String,
    val text: String,
    val expandedText: String,
    val destination: String,
    val startedAtEpochMilli: Long? = null,
    val useChronometer: Boolean = false,
)

object WorkoutEventNotificationCopy {
    const val APP_LABEL = "Flow"

    fun started(
        workoutTitle: String,
        workoutType: GymWorkoutType,
        startedAtEpochMilli: Long,
        nowEpochMilli: Long = System.currentTimeMillis(),
    ): WorkoutEventSnapshot {
        val name = workoutTitle.trim().ifBlank { defaultWorkoutLabel(workoutType) }
        val duration = GymLogic.formatStopwatch(startedAtEpochMilli, nowEpochMilli)
        val expanded = buildString {
            append("Workout started")
            append('\n')
            append(name)
            append('\n')
            append("Duration: $duration")
        }
        return WorkoutEventSnapshot(
            kind = WorkoutEventKind.STARTED,
            notificationId = WORKOUT_STARTED_NOTIFICATION_ID,
            title = APP_LABEL,
            text = "Workout started",
            expandedText = expanded,
            destination = destinationFor(workoutType),
            startedAtEpochMilli = startedAtEpochMilli,
            useChronometer = true,
        )
    }

    fun exerciseCompleted(
        exerciseName: String,
        workoutType: GymWorkoutType,
    ): WorkoutEventSnapshot {
        val name = exerciseName.trim().ifBlank { "Exercise" }
        return WorkoutEventSnapshot(
            kind = WorkoutEventKind.EXERCISE_COMPLETED,
            notificationId = EXERCISE_COMPLETED_NOTIFICATION_ID,
            title = APP_LABEL,
            text = "Exercise completed",
            expandedText = "Exercise completed\n$name",
            destination = destinationFor(workoutType),
        )
    }

    fun restStarted(
        exerciseName: String,
        restKind: GymRestKind,
        workoutType: GymWorkoutType,
    ): WorkoutEventSnapshot {
        val name = exerciseName.trim().ifBlank { "Exercise" }
        val headline = when (restKind) {
            GymRestKind.EXERCISE -> "Exercise rest started"
            else -> "Set rest started"
        }
        return WorkoutEventSnapshot(
            kind = WorkoutEventKind.REST_STARTED,
            notificationId = REST_STARTED_NOTIFICATION_ID,
            title = APP_LABEL,
            text = headline,
            expandedText = "$headline\n$name",
            destination = destinationFor(workoutType),
        )
    }

    fun workoutCompleted(
        workoutTitle: String,
        workoutType: GymWorkoutType,
        durationSeconds: Int,
    ): WorkoutEventSnapshot {
        val name = workoutTitle.trim().ifBlank { defaultWorkoutLabel(workoutType) }
        val duration = GymLogic.formatElapsedHms(durationSeconds.toLong())
        return WorkoutEventSnapshot(
            kind = WorkoutEventKind.WORKOUT_COMPLETED,
            notificationId = WORKOUT_COMPLETED_NOTIFICATION_ID,
            title = APP_LABEL,
            text = "Workout completed",
            expandedText = "Workout completed\n$name\nDuration: $duration",
            destination = destinationFor(workoutType),
        )
    }

    private fun defaultWorkoutLabel(type: GymWorkoutType): String = when (type) {
        GymWorkoutType.ROUTINE -> "Routine workout"
        else -> "Free workout"
    }

    private fun destinationFor(type: GymWorkoutType): String = when (type) {
        GymWorkoutType.ROUTINE -> WidgetLaunch.DEST_GYM_ROUTINE_WORKOUT
        else -> WidgetLaunch.DEST_GYM_FREE_WORKOUT
    }
}

internal const val WORKOUT_STARTED_NOTIFICATION_ID = 0x6C6F7730
internal const val EXERCISE_COMPLETED_NOTIFICATION_ID = 0x6C6F7731
internal const val REST_STARTED_NOTIFICATION_ID = 0x6C6F7732
internal const val WORKOUT_COMPLETED_NOTIFICATION_ID = 0x6C6F7733
