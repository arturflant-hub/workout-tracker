package com.workouttracker.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_session_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class WorkoutSessionExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val programExerciseId: Long,
    val orderIndex: Int,
    val name: String,
    val plannedSets: Int,
    val plannedMinReps: Int,
    val plannedMaxReps: Int,
    val plannedWeight: Float,
    val actualSetsDone: Int = 0,
    val comment: String = ""
)
