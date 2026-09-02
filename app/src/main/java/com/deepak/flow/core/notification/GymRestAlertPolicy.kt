package com.deepak.flow.core.notification

/**
 * Chooses how a gym rest-complete alert is delivered.
 *
 * Direct vibration is reserved for when Flow is resumed in the foreground with the screen on.
 * All other cases rely on the gym-events notification channel (including its vibration pattern).
 */
enum class GymRestAlertDelivery {
    DirectVibration,
    Notification,
}

object GymRestAlertPolicy {
    fun delivery(
        isAppResumed: Boolean,
        isScreenInteractive: Boolean,
    ): GymRestAlertDelivery {
        return if (isAppResumed && isScreenInteractive) {
            GymRestAlertDelivery.DirectVibration
        } else {
            GymRestAlertDelivery.Notification
        }
    }
}
