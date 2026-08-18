package com.deepak.flow.core.notification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderNotificationActionTest {

    @Test
    fun complete_cancelsAndDoesNotRestore() {
        val plan = planForReminderNotificationAction(ReminderNotificationIntents.ACTION_COMPLETE)
        assertNotNull(plan)
        assertTrue(plan!!.markCompleted)
        assertTrue(plan.cancelNotification)
        assertTrue(plan.cancelPendingSnooze)
        assertFalse(plan.scheduleSnooze)
    }

    @Test
    fun dismiss_cancelsAndDoesNotRestore() {
        val plan = planForReminderNotificationAction(ReminderNotificationIntents.ACTION_DISMISS)
        assertNotNull(plan)
        assertFalse(plan!!.markCompleted)
        assertTrue(plan.cancelNotification)
        assertTrue(plan.cancelPendingSnooze)
        assertFalse(plan.scheduleSnooze)
    }

    @Test
    fun snooze_cancelsCurrentAndSchedulesLater() {
        val plan = planForReminderNotificationAction(ReminderNotificationIntents.ACTION_SNOOZE)
        assertNotNull(plan)
        assertTrue(plan!!.scheduleSnooze)
        assertTrue(plan.cancelNotification)
        assertFalse(plan.cancelPendingSnooze)
        assertFalse(plan.markCompleted)
    }

    @Test
    fun unknownActions_doNotCancel() {
        assertNull(planForReminderNotificationAction(null))
        assertNull(planForReminderNotificationAction("android.intent.action.DELETE"))
        assertNull(planForReminderNotificationAction(ReminderNotificationIntents.ACTION_RESTORE))
    }
}
