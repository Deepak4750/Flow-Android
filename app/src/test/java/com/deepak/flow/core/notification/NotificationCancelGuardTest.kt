package com.deepak.flow.core.notification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NotificationCancelGuardTest {

    @Before
    fun reset() {
        NotificationCancelGuard.resetForTests()
    }

    @Test
    fun plainSwipe_isNotConsumedSoRestoreRuns() {
        assertFalse(NotificationCancelGuard.consume(7L))
    }

    @Test
    fun completeOrDismiss_armsTheGuardSoRestoreIsSkipped() {
        NotificationCancelGuard.arm(7L)
        assertTrue(NotificationCancelGuard.consume(7L))
    }

    @Test
    fun armingAppliesOnceAndOnlyToThatReminder() {
        NotificationCancelGuard.arm(7L)
        assertFalse(NotificationCancelGuard.consume(8L))
        assertTrue(NotificationCancelGuard.consume(7L))
        assertFalse(NotificationCancelGuard.consume(7L))
    }
}
