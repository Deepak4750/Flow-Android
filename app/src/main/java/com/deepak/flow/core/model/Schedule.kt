package com.deepak.flow.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.DayOfWeek

@Serializable
sealed class Schedule {
    @Serializable
    @SerialName("daily")
    data object Daily : Schedule()

    @Serializable
    @SerialName("weekly")
    data class Weekly(
        val daysOfWeek: Set<@Serializable(with = DayOfWeekSerializer::class) DayOfWeek>,
    ) : Schedule()

    @Serializable
    @SerialName("monthly")
    data class Monthly(val dayOfMonth: Int) : Schedule()

    @Serializable
    @SerialName("every_x_days")
    data class EveryXDays(val intervalDays: Int) : Schedule()

    @Serializable
    @SerialName("every_x_hours")
    data class EveryXHours(val intervalHours: Int) : Schedule()
}
