package com.workouttracker.data.db.dao

import androidx.room.*
import com.workouttracker.data.db.entities.SessionStatus
import com.workouttracker.data.db.entities.WorkoutSession
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionDao {
    @Query("SELECT * FROM workout_sessions ORDER BY date DESC")
    fun getAllSessions(): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    fun getSessionsInRange(from: Long, to: Long): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): WorkoutSession?

    @Query("SELECT * FROM workout_sessions WHERE date = :date LIMIT 1")
    suspend fun getSessionByDate(date: Long): WorkoutSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSession): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<WorkoutSession>)

    @Update
    suspend fun updateSession(session: WorkoutSession)

    @Query("UPDATE workout_sessions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: SessionStatus)

    @Query("DELETE FROM workout_sessions WHERE status = 'PLANNED' AND date >= :from")
    suspend fun deletePlannedFrom(from: Long)

    @Query("SELECT * FROM workout_sessions WHERE programType = :programType AND status = 'DONE' AND date < :beforeDate ORDER BY date DESC LIMIT 1")
    suspend fun getLastDoneSessionByType(programType: String, beforeDate: Long): WorkoutSession?
}
