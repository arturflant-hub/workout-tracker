package com.workouttracker.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedule_settings")
data class ScheduleSettings(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trainingDaysMask: Int, // bitmask: Mon=1, Tue=2, Wed=4, Thu=8, Fri=16, Sat=32, Sun=64
    val startDate: Long, // epoch millis
    val cycleLengthWeeks: Int = 2,
    val startWeekType: Int = 1 // 1 or 2
)
