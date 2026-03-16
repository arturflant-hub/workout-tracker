package com.workouttracker.data.repository

import com.workouttracker.data.db.dao.ScheduleSettingsDao
import com.workouttracker.data.db.dao.WeekPatternDao
import com.workouttracker.data.db.dao.WorkoutSessionDao
import com.workouttracker.data.db.dao.WorkoutSessionExerciseDao
import com.workouttracker.data.db.entities.*
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRepository @Inject constructor(
    private val settingsDao: ScheduleSettingsDao,
    private val weekPatternDao: WeekPatternDao,
    private val sessionDao: WorkoutSessionDao,
    private val sessionExerciseDao: WorkoutSessionExerciseDao,
    private val programRepository: ProgramRepository
) {
    fun getSettings(): Flow<ScheduleSettings?> = settingsDao.getSettings()
    fun getAllPatterns(): Flow<List<WeekPattern>> = weekPatternDao.getAllPatterns()
    fun getSessionsInRange(from: Long, to: Long): Flow<List<WorkoutSession>> =
        sessionDao.getSessionsInRange(from, to)

    suspend fun saveSettings(settings: ScheduleSettings): Long =
        settingsDao.insertSettings(settings)

    suspend fun savePatterns(patterns: List<WeekPattern>) {
        weekPatternDao.deleteAll()
        weekPatternDao.insertPatterns(patterns)
    }

    suspend fun generateSchedule(weeksAhead: Int = 12) {
        val settings = settingsDao.getSettingsOnce() ?: return
        val patterns = weekPatternDao.getAllPatterns().let {
            weekPatternDao.getPatternsByWeek(1) + weekPatternDao.getPatternsByWeek(2)
        }
        if (patterns.isEmpty()) return

        val now = startOfDay(System.currentTimeMillis())
        sessionDao.deletePlannedFrom(now)

        val cal = Calendar.getInstance()
        cal.timeInMillis = settings.startDate

        val sessions = mutableListOf<WorkoutSession>()
        var currentWeek = settings.startWeekType

        repeat(weeksAhead) { weekOffset ->
            for (dayOfWeek in 1..7) {
                val mask = 1 shl (dayOfWeek - 1)
                if (settings.trainingDaysMask and mask == 0) continue

                val pattern = patterns.find {
                    it.weekType == currentWeek && it.dayOfWeek == dayOfWeek
                } ?: continue

                val sessionCal = Calendar.getInstance()
                sessionCal.timeInMillis = settings.startDate

                // Find the correct date: weekOffset-th week, dayOfWeek
                val startDow = sessionCal.get(Calendar.DAY_OF_WEEK)
                // Monday=2 in Java Calendar, but we use 1=Mon
                val javaDow = if (dayOfWeek == 7) Calendar.SUNDAY else dayOfWeek + 1

                sessionCal.set(Calendar.DAY_OF_WEEK, javaDow)
                sessionCal.add(Calendar.WEEK_OF_YEAR, weekOffset)

                val sessionDate = startOfDay(sessionCal.timeInMillis)
                if (sessionDate >= now) {
                    sessions.add(
                        WorkoutSession(
                            date = sessionDate,
                            programType = pattern.programType,
                            status = SessionStatus.PLANNED
                        )
                    )
                }
            }

            currentWeek = if (currentWeek == 1) 2 else 1
        }

        sessionDao.insertSessions(sessions)

        // Create session exercises for each session
        sessions.forEach { session ->
            val sessionId = sessionDao.getSessionByDate(session.date)?.id ?: return@forEach
            val program = programRepository.getProgramByType(session.programType) ?: return@forEach
            val exercises = mutableListOf<WorkoutSessionExercise>()

            // We need a one-shot query here - using coroutine collect
            var programExercises: List<ProgramExercise> = emptyList()
            programRepository.getExercisesByProgram(program.id).collect { list ->
                programExercises = list
                return@collect
            }

            programExercises.forEach { pe ->
                exercises.add(
                    WorkoutSessionExercise(
                        sessionId = sessionId,
                        programExerciseId = pe.id,
                        orderIndex = pe.orderIndex,
                        name = pe.name,
                        plannedSets = pe.sets,
                        plannedMinReps = pe.minReps,
                        plannedMaxReps = pe.maxReps,
                        plannedWeight = pe.startWeight
                    )
                )
            }
            sessionExerciseDao.insertExercises(exercises)
        }
    }

    private fun startOfDay(millis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getAllSessions(): Flow<List<WorkoutSession>> = sessionDao.getAllSessions()
}
