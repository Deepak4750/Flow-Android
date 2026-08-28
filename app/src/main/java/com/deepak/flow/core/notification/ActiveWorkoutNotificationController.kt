package com.deepak.flow.core.notification

import android.content.Context
import com.deepak.flow.core.gym.GymWorkoutSession
import com.deepak.flow.core.gym.GymWorkoutStatus
import com.deepak.flow.core.gym.GymWorkoutType
import com.deepak.flow.core.repository.GymWorkoutRepository
import com.deepak.flow.core.widget.WidgetLaunch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Keeps a single ongoing Free Workout notification in sync with the active session.
 * Elapsed times are always derived from persisted timestamps, not a UI clock.
 */
class ActiveWorkoutNotificationController(
    private val appContext: Context,
    private val repository: GymWorkoutRepository,
    private val scope: CoroutineScope,
) {
    private var observeJob: Job? = null

    fun start() {
        if (observeJob != null) return
        observeJob = scope.launch {
            repository.observeAnyActiveSession()
                .distinctUntilChanged()
                .collectLatest { session ->
                    if (session == null || session.status != GymWorkoutStatus.ACTIVE) {
                        NotificationChannelManager.cancelActiveWorkoutNotification(appContext)
                        return@collectLatest
                    }
                    tickWhileActive(session)
                }
        }
    }

    /** Re-post immediately if an active Free Workout still exists (e.g. after swipe). */
    suspend fun restoreIfActive() {
        val session = repository.getAnyActiveSession()
        if (session == null || session.status != GymWorkoutStatus.ACTIVE) {
            NotificationChannelManager.cancelActiveWorkoutNotification(appContext)
            return
        }
        post(session, System.currentTimeMillis())
    }

    private suspend fun tickWhileActive(initial: GymWorkoutSession) {
        var latest = initial
        while (currentCoroutineContext().isActive) {
            val fresh = repository.getAnyActiveSession()
            if (fresh == null || fresh.status != GymWorkoutStatus.ACTIVE) {
                NotificationChannelManager.cancelActiveWorkoutNotification(appContext)
                return
            }
            latest = fresh
            post(latest, System.currentTimeMillis())
            delay(1_000L)
        }
    }

    private fun post(session: GymWorkoutSession, nowEpochMilli: Long) {
        val exercises = session.exercises
        val current = if (exercises.isEmpty()) {
            null
        } else {
            exercises[session.currentExerciseIndex.coerceIn(0, exercises.lastIndex)]
        }
        NotificationChannelManager.postActiveWorkoutNotification(
            context = appContext,
            exerciseName = current?.name,
            workoutStartedAtEpochMilli = session.startedAtEpochMilli,
            exerciseStartedAtEpochMilli = session.currentExerciseStartedAtEpochMilli,
            nowEpochMilli = nowEpochMilli,
            destination = if (session.type == GymWorkoutType.ROUTINE) {
                WidgetLaunch.DEST_GYM_ROUTINE_WORKOUT
            } else {
                WidgetLaunch.DEST_GYM_FREE_WORKOUT
            },
        )
    }
}
