package com.deepak.flow.core.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderNotificationCopyTest {

    @Test
    fun emptyNote_hasNoBody() {
        assertNull(reminderNotificationBody(null))
        assertNull(reminderNotificationBody(""))
        assertNull(reminderNotificationBody("   "))
    }

    @Test
    fun placeholderCopy_isNotUsedAsBody() {
        assertNull(reminderNotificationBody("e.g. Time to show up."))
        assertNull(reminderNotificationBody("Time to show up."))
    }

    @Test
    fun realNote_isKept() {
        assertEquals("A little progress today.", reminderNotificationBody("A little progress today."))
    }
}
