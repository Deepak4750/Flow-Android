package com.deepak.flow.core.database

import androidx.room.TypeConverter
import com.deepak.flow.core.model.Category

// Category is the only non-primitive column type in the schema: schedules, reminder times
// and active hours are stored as pre-serialized JSON `String` columns that
// ReminderRepositoryImpl encodes and decodes itself, so Room never needs a converter for
// them.
class DatabaseConverters {
    @TypeConverter
    fun fromCategory(category: Category): String = category.name

    @TypeConverter
    fun toCategory(value: String): Category = Category.valueOf(value)
}
