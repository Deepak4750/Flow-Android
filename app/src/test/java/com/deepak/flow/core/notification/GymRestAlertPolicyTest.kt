package com.deepak.flow.core.notification

import org.junit.Assert.assertEquals
import org.junit.Test

class GymRestAlertPolicyTest {

    @Test
    fun `foreground with screen on uses direct vibration`() {
        assertEquals(
            GymRestAlertDelivery.DirectVibration,
            GymRestAlertPolicy.delivery(isAppResumed = true, isScreenInteractive = true),
        )
    }

    @Test
    fun `background uses notification`() {
        assertEquals(
            GymRestAlertDelivery.Notification,
            GymRestAlertPolicy.delivery(isAppResumed = false, isScreenInteractive = true),
        )
    }

    @Test
    fun `screen off uses notification even when resumed`() {
        assertEquals(
            GymRestAlertDelivery.Notification,
            GymRestAlertPolicy.delivery(isAppResumed = true, isScreenInteractive = false),
        )
    }

    @Test
    fun `background and screen off uses notification`() {
        assertEquals(
            GymRestAlertDelivery.Notification,
            GymRestAlertPolicy.delivery(isAppResumed = false, isScreenInteractive = false),
        )
    }
}
