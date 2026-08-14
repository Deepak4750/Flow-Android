package com.deepak.flow.core.database

import androidx.room.TypeConverter
import com.deepak.flow.core.model.ActiveHours
import com.deepak.flow.core.model.Category
import com.deepak.flow.core.model.LocalTimeSerializer
import com.deepak.flow.core.model.Schedule
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class DatabaseConverters {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
    }

    @TypeConverter
    fun fromCategory(category: Category): String = category.name

    @TypeConverter
    fun toCategory(value: String): Category = Category.valueOf(value)

    @TypeConverter
    fun fromSchedule(schedule: Schedule): String = json.encodeToString(Schedule.serializer(), schedule)

    @TypeConverter
    fun toSchedule(value: String): Schedule = json.decodeFromString(Schedule.serializer(), value)

    @TypeConverter
    fun fromLocalTimeList(times: List<java.time.LocalTime>): String =
        json.encodeToString(ListSerializer(LocalTimeSerializer), times)

    @TypeConverter
    fun toLocalTimeList(value: String): List<java.time.LocalTime> =
        json.decodeFromString(ListSerializer(LocalTimeSerializer), value)

    @TypeConverter
    fun fromActiveHours(activeHours: ActiveHours?): String? =
        activeHours?.let { json.encodeToString(ActiveHours.serializer(), it) }

    @TypeConverter
    fun toActiveHours(value: String?): ActiveHours? =
        value?.let { json.decodeFromString(ActiveHours.serializer(), it) }
}
