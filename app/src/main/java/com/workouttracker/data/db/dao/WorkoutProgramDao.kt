package com.workouttracker.data.db.dao

import androidx.room.*
import com.workouttracker.data.db.entities.WorkoutProgram
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutProgramDao {
    @Query("SELECT * FROM workout_programs ORDER BY type ASC")
    fun getAllPrograms(): Flow<List<WorkoutProgram>>

    @Query("SELECT * FROM workout_programs WHERE type = :type LIMIT 1")
    suspend fun getProgramByType(type: String): WorkoutProgram?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgram(program: WorkoutProgram): Long

    @Update
    suspend fun updateProgram(program: WorkoutProgram)

    @Delete
    suspend fun deleteProgram(program: WorkoutProgram)
}
