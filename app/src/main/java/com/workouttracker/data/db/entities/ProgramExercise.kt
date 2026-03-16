package com.workouttracker.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "program_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutProgram::class,
            parentColumns = ["id"],
            childColumns = ["programId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("programId")]
)
data class ProgramExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val programId: Long,
    val orderIndex: Int,
    val name: String,
    val sets: Int,
    val minReps: Int,
    val maxReps: Int,
    val startWeight: Float,
    val startWeightNote: String = "" // "barbell" or "dumbbell"
)
