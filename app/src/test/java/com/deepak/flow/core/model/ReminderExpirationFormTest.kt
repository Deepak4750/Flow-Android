package com.deepak.flow.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ReminderExpirationFormTest {

    private val startDate = LocalDate.of(2026, 6, 1)

    @Test
    fun switchingToEndDateClearsOccurrenceLimit() {
        val updated = ReminderExpirationForm(
            mode = ReminderExpirationMode.OCCURRENCE_LIMIT,
            endDate = null,
            occurrenceLimit = 12,
        ).withMode(ReminderExpirationMode.END_DATE, startDate)

        assertEquals(ReminderExpirationMode.END_DATE, updated.mode)
        assertEquals(startDate.plusMonths(1), updated.endDate)
        assertEquals(ReminderExpirationForm.DEFAULT_OCCURRENCE_LIMIT, updated.occurrenceLimit)
    }

    @Test
    fun switchingToOccurrenceLimitClearsEndDate() {
        val updated = ReminderExpirationForm(
            mode = ReminderExpirationMode.END_DATE,
            endDate = LocalDate.of(2026, 8, 30),
            occurrenceLimit = ReminderExpirationForm.DEFAULT_OCCURRENCE_LIMIT,
        ).withMode(ReminderExpirationMode.OCCURRENCE_LIMIT, startDate)

        assertEquals(ReminderExpirationMode.OCCURRENCE_LIMIT, updated.mode)
        assertNull(updated.endDate)
        assertEquals(ReminderExpirationForm.DEFAULT_OCCURRENCE_LIMIT, updated.occurrenceLimit)
    }

    @Test
    fun noneClearsBothExpirationFields() {
        val updated = ReminderExpirationForm(
            mode = ReminderExpirationMode.END_DATE,
            endDate = LocalDate.of(2026, 8, 30),
            occurrenceLimit = 5,
        ).withMode(ReminderExpirationMode.NONE, startDate)

        assertEquals(ReminderExpirationMode.NONE, updated.mode)
        assertNull(updated.endDate)
        assertEquals(ReminderExpirationForm.DEFAULT_OCCURRENCE_LIMIT, updated.occurrenceLimit)

        val saved = updated.normalizedForSave().toReminderFields()
        assertEquals(ReminderExpirationMode.NONE, saved.first)
        assertNull(saved.second)
        assertNull(saved.third)
    }
}
