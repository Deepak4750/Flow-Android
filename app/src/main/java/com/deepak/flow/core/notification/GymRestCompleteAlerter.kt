package com.deepak.flow.core.notification

import android.content.Context
import com.deepak.flow.core.gym.vibrateRestComplete

object GymRestCompleteAlerter {
    fun signal(context: Context, request: GymRestAlarmRequest) {
        if (!GymRestAlertDeduper.tryClaim(request.workoutId, request.restEndsAtEpochMilli)) {
            return
        }
        when (
            GymRestAlertPolicy.delivery(
                isAppResumed = FlowAppVisibility.isResumed(context),
                isScreenInteractive = FlowAppVisibility.isScreenInteractive(context),
            )
        ) {
            GymRestAlertDelivery.DirectVibration -> {
                vibrateRestComplete(context)
                GymRestCompleteSound.playForeground(context)
            }
            GymRestAlertDelivery.Notification ->
                NotificationChannelManager.postRestCompleteNotification(
                    context = context,
                    exerciseName = request.exerciseName,
                    destination = request.destination,
                )
        }
    }
}

class AndroidGymRestAlerter(
    private val context: Context,
) : GymRestAlerterPort {
    override fun signal(request: GymRestAlarmRequest) {
        GymRestCompleteAlerter.signal(context, request)
    }

    override fun suppress(request: GymRestAlarmRequest) {
        GymRestAlertDeduper.suppress(request.workoutId, request.restEndsAtEpochMilli)
    }
}
