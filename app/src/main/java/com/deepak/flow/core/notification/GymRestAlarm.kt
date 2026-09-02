package com.deepak.flow.core.notification

import com.deepak.flow.core.gym.GymWorkoutSession
import com.deepak.flow.core.gym.GymWorkoutType
import com.deepak.flow.core.widget.WidgetLaunch

data class GymRestAlarmRequest(
    val workoutId: Long,
    val restEndsAtEpochMilli: Long,
    val exerciseName: String?,
    val destination: String,
) {
    companion object {
        fun from(session: GymWorkoutSession, restEndsAtEpochMilli: Long): GymRestAlarmRequest {
            val exercises = session.exercises
            val exerciseName = if (exercises.isEmpty()) {
                null
            } else {
                exercises[session.currentExerciseIndex.coerceIn(0, exercises.lastIndex)].name
            }
            return GymRestAlarmRequest(
                workoutId = session.id,
                restEndsAtEpochMilli = restEndsAtEpochMilli,
                exerciseName = exerciseName,
                destination = destinationFor(session.type),
            )
        }

        fun destinationFor(type: GymWorkoutType): String =
            if (type == GymWorkoutType.ROUTINE) {
                WidgetLaunch.DEST_GYM_ROUTINE_WORKOUT
            } else {
                WidgetLaunch.DEST_GYM_FREE_WORKOUT
            }
    }
}

interface GymRestAlarmPort {
    fun schedule(request: GymRestAlarmRequest)
    fun cancel()
}

interface GymRestAlerterPort {
    fun signal(request: GymRestAlarmRequest)
    fun suppress(request: GymRestAlarmRequest)
}

/**
 * Gym rest-complete uses one AlarmManager RTC_WAKEUP so the alert survives
 * Doze and background vibration limits. The workout clock never alerts.
 */
class GymRestAlarmCoordinator(
    private val alarms: GymRestAlarmPort,
    private val alerter: GymRestAlerterPort,
) {
    private var pending: GymRestAlarmRequest? = null

    fun onRestStarted(request: GymRestAlarmRequest, nowEpochMilli: Long) {
        flushDueAlert(nowEpochMilli)
        alarms.cancel()
        alarms.schedule(request)
        pending = request
    }

    fun onRestExtended(request: GymRestAlarmRequest) {
        alarms.cancel()
        alarms.schedule(request)
        pending = request
    }

    fun onRestAbandoned() {
        pending?.let(alerter::suppress)
        pending = null
        alarms.cancel()
    }

    private fun flushDueAlert(nowEpochMilli: Long) {
        val current = pending ?: return
        if (GymRestAlarm.shouldAlert(nowEpochMilli, current.restEndsAtEpochMilli)) {
            alerter.signal(current)
        }
        pending = null
    }
}

object GymRestAlarm {
    fun shouldAlert(nowEpochMilli: Long, restEndsAtEpochMilli: Long): Boolean =
        nowEpochMilli >= restEndsAtEpochMilli
}
