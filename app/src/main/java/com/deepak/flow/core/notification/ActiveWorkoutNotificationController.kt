package com.deepak.flow.core.notification

import android.content.Context
import com.deepak.flow.core.gym.GymWorkoutSession
import com.deepak.flow.core.gym.GymWorkoutStatus
import com.deepak.flow.core.repository.GymWorkoutRepository
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
        var lastFingerprint: String? = null
        while (currentCoroutineContext().isActive) {
            val fresh = repository.getAnyActiveSession()
            if (fresh == null || fresh.status != GymWorkoutStatus.ACTIVE) {
                NotificationChannelManager.cancelActiveWorkoutNotification(appContext)
                return
            }
            val now = System.currentTimeMillis()
            val snapshot = ActiveWorkoutNotificationCopy.fromSession(fresh, now)
            if (snapshot.fingerprint != lastFingerprint) {
                NotificationChannelManager.postActiveWorkoutNotification(
                    context = appContext,
                    snapshot = snapshot,
                )
                lastFingerprint = snapshot.fingerprint
            }
            delay(if (snapshot.isResting) 1_000L else 3_000L)
        }
    }

    /** Clears an active rest timer from a notification action without touching the rest-complete alert path. */
    suspend fun skipRestIfActive(workoutId: Long) {
        val session = repository.getAnyActiveSession() ?: return
        if (session.id != workoutId) return
        if (session.restEndsAtEpochMilli == null) return
        repository.cancelScheduledRestAlert()
        repository.clearRest(session.id)
        restoreIfActive()
    }

    private fun post(session: GymWorkoutSession, nowEpochMilli: Long) {
        val snapshot = ActiveWorkoutNotificationCopy.fromSession(session, nowEpochMilli)
        NotificationChannelManager.postActiveWorkoutNotification(
            context = appContext,
            snapshot = snapshot,
        )
    }
}
