package com.workouttracker.data.db.dao

import androidx.room.*
import com.workouttracker.data.db.entities.ProgramExercise
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramExerciseDao {
    @Query("SELECT * FROM program_exercises WHERE programId = :programId ORDER BY orderIndex ASC")
    fun getExercisesByProgram(programId: Long): Flow<List<ProgramExercise>>

    @Query("SELECT * FROM program_exercises WHERE programId = :programId ORDER BY orderIndex ASC")
    suspend fun getExercisesByProgramOnce(programId: Long): List<ProgramExercise>

    @Query("SELECT * FROM program_exercises WHERE id = :id")
    suspend fun getExerciseById(id: Long): ProgramExercise?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ProgramExercise): Long

    @Update
    suspend fun updateExercise(exercise: ProgramExercise)

    @Delete
    suspend fun deleteExercise(exercise: ProgramExercise)

    @Query("DELETE FROM program_exercises WHERE programId = :programId")
    suspend fun deleteAllForProgram(programId: Long)
}
