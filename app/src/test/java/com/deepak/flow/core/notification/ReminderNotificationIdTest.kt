package com.deepak.flow.core.notification

import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderNotificationIdTest {

    @Test
    fun reminderIdsMapToTheSameNotificationId() {
        assertEquals(7, reminderNotificationId(7L))
    }

    @Test
    fun zeroAndNegativeIdsAreRejectedAsForegroundIds() {
        assertEquals(1, reminderNotificationId(0L))
        assertEquals(1, reminderNotificationId(-4L))
    }
}
