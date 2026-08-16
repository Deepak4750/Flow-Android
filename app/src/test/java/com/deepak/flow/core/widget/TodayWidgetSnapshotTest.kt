package com.deepak.flow.core.widget

import com.deepak.flow.core.model.Category
import com.deepak.flow.core.model.Reminder
import com.deepak.flow.core.model.Schedule
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class TodayWidgetSnapshotTest {

    private val zone = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 8, 16)
    private val formatter = DateTimeFormatter.ofPattern("H:mm")

    private fun reminder(
        id: Long,
        title: String,
        time: LocalTime,
        enabled: Boolean = true,
    ) = Reminder(
        id = id,
        title = title,
        category = Category.PERSONAL,
        schedule = Schedule.Daily,
        reminderTimes = listOf(time),
        startDate = today.minusDays(7),
        enabled = enabled,
    )

    @Test
    fun incompleteItems_comeFirst() {
        val snapshot = buildTodayWidgetSnapshot(
            reminders = listOf(
                reminder(1, "Done", LocalTime.of(7, 0)),
                reminder(2, "Later", LocalTime.of(19, 0)),
            ),
            completedIds = setOf(1L),
            today = today,
            zoneId = zone,
            timeFormatter = formatter,
        )
        assertEquals(listOf("Later", "Done"), snapshot.items.map { it.title })
        assertEquals(1, snapshot.progress.completedTasks)
        assertEquals(2, snapshot.progress.totalTasks)
    }

    @Test
    fun extraCount_whenOverVisibleLimit() {
        val reminders = (1L..7L).map { id ->
            reminder(id, "Task $id", LocalTime.of(id.toInt(), 0))
        }
        val snapshot = buildTodayWidgetSnapshot(
            reminders = reminders,
            completedIds = emptySet(),
            today = today,
            zoneId = zone,
            timeFormatter = formatter,
            visibleLimit = 3,
        )
        assertEquals(3, snapshot.items.size)
        assertEquals(4, snapshot.extraCount)
    }

    @Test
    fun disabledReminders_areOmitted() {
        val snapshot = buildTodayWidgetSnapshot(
            reminders = listOf(
                reminder(1, "On", LocalTime.of(8, 0)),
                reminder(2, "Off", LocalTime.of(9, 0), enabled = false),
            ),
            completedIds = emptySet(),
            today = today,
            zoneId = zone,
            timeFormatter = formatter,
        )
        assertEquals(listOf("On"), snapshot.items.map { it.title })
    }
}
