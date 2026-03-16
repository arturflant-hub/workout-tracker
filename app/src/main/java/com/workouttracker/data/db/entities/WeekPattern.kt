package com.workouttracker.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "week_patterns")
data class WeekPattern(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weekType: Int,       // 1 or 2
    val dayOfWeek: Int,      // 1=Mon, 7=Sun
    val programType: String  // "A" or "B"
)
