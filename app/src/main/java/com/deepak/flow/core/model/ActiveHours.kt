package com.deepak.flow.core.model

import kotlinx.serialization.Serializable
import java.time.LocalTime

@Serializable
data class ActiveHours(
    val startTime: @Serializable(with = LocalTimeSerializer::class) LocalTime,
    val endTime: @Serializable(with = LocalTimeSerializer::class) LocalTime,
) {
    fun isActive(time: LocalTime): Boolean {
        if (startTime == endTime) return true
        return if (startTime.isBefore(endTime)) {
            !time.isBefore(startTime) && time.isBefore(endTime)
        } else {
            !time.isBefore(startTime) || time.isBefore(endTime)
        }
    }
}
