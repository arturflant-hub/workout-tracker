package com.workouttracker.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SessionStatus { PLANNED, IN_PROGRESS, DONE, SKIPPED }

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long, // epoch millis (start of day)
    val programType: String, // "A" or "B"
    val status: SessionStatus = SessionStatus.PLANNED
)
