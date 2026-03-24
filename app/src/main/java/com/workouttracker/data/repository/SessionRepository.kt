package com.workouttracker.data.repository

import com.workouttracker.data.db.dao.WorkoutSessionDao
import com.workouttracker.data.db.dao.WorkoutSessionExerciseDao
import com.workouttracker.data.db.dao.WorkoutSetFactDao
import com.workouttracker.data.db.entities.SessionStatus
import com.workouttracker.data.db.entities.WorkoutSession
import com.workouttracker.data.db.entities.WorkoutSessionExercise
import com.workouttracker.data.db.entities.WorkoutSetFact
import com.workouttracker.domain.model.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val sessionDao: WorkoutSessionDao,
    private val exerciseDao: WorkoutSessionExerciseDao,
    private val setFactDao: WorkoutSetFactDao
) {
    fun getExercisesBySession(sessionId: Long): Flow<List<WorkoutSessionExercise>> =
        exerciseDao.getExercisesBySession(sessionId)

    suspend fun getExercisesBySessionOnce(sessionId: Long): List<WorkoutSessionExercise> =
        exerciseDao.getExercisesBySessionOnce(sessionId)

    fun getSetsByExercise(exerciseId: Long): Flow<List<WorkoutSetFact>> =
        setFactDao.getSetsByExercise(exerciseId)

    suspend fun getSessionById(id: Long): WorkoutSession? = sessionDao.getSessionById(id)

    suspend fun saveSetFact(set: WorkoutSetFact): Long = setFactDao.insertSet(set)

    suspend fun updateSetFact(set: WorkoutSetFact) = setFactDao.updateSet(set)

    suspend fun updateExercise(exercise: WorkoutSessionExercise) =
        exerciseDao.updateExercise(exercise)

    suspend fun completeSession(sessionId: Long) =
        sessionDao.updateStatus(sessionId, SessionStatus.DONE)

    suspend fun skipSession(sessionId: Long) =
        sessionDao.updateStatus(sessionId, SessionStatus.SKIPPED)

    suspend fun getSessionSummary(sessionId: Long): SessionSummary? {
        val session = sessionDao.getSessionById(sessionId) ?: return null
        val exercises = exerciseDao.getExercisesBySessionOnce(sessionId)

        val exerciseSummaries = exercises.map { ex ->
            val sets = setFactDao.getSetsByExerciseOnce(ex.id)
            val setComparisons = sets.map { setFact ->
                val repsStatus = when {
                    setFact.actualReps > setFact.plannedReps -> ComparisonStatus.BETTER
                    setFact.actualReps == setFact.plannedReps -> ComparisonStatus.EQUAL
                    else -> ComparisonStatus.WORSE
                }
                val weightStatus = when {
                    setFact.actualWeight > setFact.plannedWeight -> ComparisonStatus.BETTER
                    setFact.actualWeight == setFact.plannedWeight -> ComparisonStatus.EQUAL
                    else -> ComparisonStatus.WORSE
                }
                SetComparison(setFact, repsStatus, weightStatus)
            }
            ExerciseSummary(ex, setComparisons)
        }

        return SessionSummary(session, exerciseSummaries)
    }

    fun getAllSessions(): Flow<List<WorkoutSession>> = sessionDao.getAllSessions()

    suspend fun startSession(sessionId: Long) =
        sessionDao.updateStatus(sessionId, SessionStatus.IN_PROGRESS)

    suspend fun getHistoryByProgramExercise(programExerciseId: Long): List<WorkoutSessionExercise> =
        exerciseDao.getHistoryByProgramExercise(programExerciseId)

    suspend fun getSetsForExercise(exerciseId: Long): List<WorkoutSetFact> =
        setFactDao.getSetsByExerciseOnce(exerciseId)

    suspend fun getPreviousSessionByType(programType: String, beforeDate: Long): WorkoutSession? =
        sessionDao.getLastDoneSessionByType(programType, beforeDate)

    /**
     * Returns max actualWeight per session for an exercise, ordered from newest to oldest.
     * Used for plateau detection (4+ sessions with same weight).
     */
    suspend fun getWeightHistoryForExercise(programExerciseId: Long, limit: Int = 4): List<Float> {
        val history = exerciseDao.getHistoryByProgramExercise(programExerciseId)
        return history.take(limit).mapNotNull { ex ->
            val sets = setFactDao.getSetsByExerciseOnce(ex.id)
            sets.maxOfOrNull { it.actualWeight }
        }
    }
}
