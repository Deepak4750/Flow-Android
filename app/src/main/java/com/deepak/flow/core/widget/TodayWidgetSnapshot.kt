package com.deepak.flow.core.widget

import com.deepak.flow.core.model.DailyProgress
import com.deepak.flow.core.model.Reminder
import com.deepak.flow.core.model.activeOn
import com.deepak.flow.core.scheduling.SchedulingEngine
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class TodayWidgetItem(
    val id: Long,
    val title: String,
    val timeLabel: String,
    val completed: Boolean,
    val isNext: Boolean = false,
)

data class TodayWidgetSnapshot(
    val items: List<TodayWidgetItem>,
    val extraCount: Int,
    val progress: DailyProgress,
)

fun nextUpReminderId(
    reminders: List<Reminder>,
    now: Instant,
    zoneId: ZoneId,
    engine: SchedulingEngine = SchedulingEngine(),
): Long? = reminders
    .filter { it.enabled }
    .mapNotNull { reminder ->
        engine.calculateNextOccurrence(reminder, now, zoneId)?.let { reminder.id to it }
    }
    .minByOrNull { it.second }
    ?.first

fun buildTodayWidgetSnapshot(
    reminders: List<Reminder>,
    completedIds: Set<Long>,
    today: LocalDate,
    zoneId: ZoneId,
    timeFormatter: DateTimeFormatter,
    visibleLimit: Int = Int.MAX_VALUE,
    engine: SchedulingEngine = SchedulingEngine(),
    now: Instant = Instant.now(),
): TodayWidgetSnapshot {
    val scheduled = reminders
        .filter { it.enabled && engine.isScheduledOnDate(it, today, zoneId) }
        .sortedWith(
            compareBy<Reminder> { it.id in completedIds }
                .thenBy { it.reminderTimes.minOrNull() ?: LocalTime.MAX },
        )
    val nextId = nextUpReminderId(reminders, now, zoneId, engine)
    val items = scheduled.take(visibleLimit).map { reminder ->
        TodayWidgetItem(
            id = reminder.id,
            title = reminder.title,
            timeLabel = reminder.reminderTimes.joinToString(", ") { it.format(timeFormatter) },
            completed = reminder.id in completedIds,
            isNext = reminder.id == nextId,
        )
    }
    return TodayWidgetSnapshot(
        items = items,
        extraCount = (scheduled.size - visibleLimit).coerceAtLeast(0),
        progress = DailyProgress(
            totalTasks = scheduled.size,
            completedTasks = scheduled.count { it.id in completedIds },
        ),
    )
}
