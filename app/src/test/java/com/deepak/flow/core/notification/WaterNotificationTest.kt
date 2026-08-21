package com.deepak.flow.core.notification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WaterNotificationTest {

    @Test
    fun swipeRestoreKeepsUntilOneLitreLoggedFromNotification() {
        // Swipe-to-dismiss still re-posts until the session reaches 1 L.
        // Add actions (250 / 500) cancel the notification immediately in the receiver.
        assertFalse(waterNotificationIsFilled(0))
        assertFalse(waterNotificationIsFilled(250))
        assertFalse(waterNotificationIsFilled(500))
        assertFalse(waterNotificationIsFilled(750))
        assertTrue(waterNotificationIsFilled(1000))
        assertTrue(waterNotificationIsFilled(1250))
    }

    @Test
    fun notificationActionsAreTwoAddsPlusDismissSlot() {
        assertTrue(WaterNotificationAddAmountsMl.contentEquals(intArrayOf(250, 500)))
    }

    @Test
    fun drinkReminderBodiesRotateCalmly() {
        assertTrue(WaterNotificationCopy.Bodies.size >= 6)
        assertTrue(WaterNotificationCopy.Bodies.all { it.isNotBlank() })
    }
}
