package com.workouttracker.data.db.dao

import androidx.room.*
import com.workouttracker.data.db.entities.WorkoutSessionExercise
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionExerciseDao {
    @Query("SELECT * FROM workout_session_exercises WHERE sessionId = :sessionId ORDER BY orderIndex ASC")
    fun getExercisesBySession(sessionId: Long): Flow<List<WorkoutSessionExercise>>

    @Query("SELECT * FROM workout_session_exercises WHERE sessionId = :sessionId ORDER BY orderIndex ASC")
    suspend fun getExercisesBySessionOnce(sessionId: Long): List<WorkoutSessionExercise>

    @Query("SELECT * FROM workout_session_exercises WHERE id = :id")
    suspend fun getById(id: Long): WorkoutSessionExercise?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: WorkoutSessionExercise): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<WorkoutSessionExercise>)

    @Update
    suspend fun updateExercise(exercise: WorkoutSessionExercise)

    @Query("SELECT wse.* FROM workout_session_exercises wse " +
           "INNER JOIN workout_sessions ws ON ws.id = wse.sessionId " +
           "WHERE wse.programExerciseId = :programExerciseId AND ws.status = 'DONE' " +
           "ORDER BY ws.date DESC")
    suspend fun getHistoryByProgramExercise(programExerciseId: Long): List<WorkoutSessionExercise>
}
