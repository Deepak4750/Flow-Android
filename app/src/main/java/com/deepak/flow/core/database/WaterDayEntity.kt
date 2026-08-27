package com.deepak.flow.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "water_days")
data class WaterDayEntity(
    @PrimaryKey
    val dateEpochDay: Long,
    val intakeMl: Int,
    val addLog: String = "",
    val goalMl: Int? = null,
)
