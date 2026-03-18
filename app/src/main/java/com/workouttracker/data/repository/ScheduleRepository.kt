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
        val patterns = weekPatternDao.getPatternsByWeek(1) + weekPatternDao.getPatternsByWeek(2)
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

            // FIX: use suspend once-query instead of Flow.collect anti-pattern
            val programExercises = programRepository.getExercisesByProgramOnce(program.id)
            if (programExercises.isEmpty()) return@forEach

            val exercises = programExercises.map { pe ->
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

    /**
     * Creates a session for today from the given programType.
     * Returns the session ID (existing or newly created).
     * FIX: replaced Flow.collect anti-pattern with getExercisesByProgramOnce().
     */
    suspend fun createQuickSession(programType: String): Long {
        val today = startOfDay(System.currentTimeMillis())
        // Check if session for today already exists
        val existing = sessionDao.getSessionByDate(today)
        if (existing != null) return existing.id

        val session = WorkoutSession(
            date = today,
            programType = programType,
            status = SessionStatus.PLANNED
        )
        val sessionId = sessionDao.insertSession(session)

        // Copy exercises from program — use suspend once-query (not Flow.collect)
        val program = programRepository.getProgramByType(programType) ?: return sessionId
        val programExercises = programRepository.getExercisesByProgramOnce(program.id)

        if (programExercises.isNotEmpty()) {
            val exercises = programExercises.map { pe ->
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
            }
            sessionExerciseDao.insertExercises(exercises)
        }

        return sessionId
    }

    /**
     * FIX for Bug 1: Checks if today has a scheduled workout via WeekPattern/ScheduleSettings
     * and auto-creates a session if one is due but missing from the DB.
     */
    suspend fun ensureTodaySessionIfScheduled() {
        val todayStart = startOfDay(System.currentTimeMillis())

        // Already have a session for today? Do nothing.
        val existing = sessionDao.getSessionByDate(todayStart)
        if (existing != null) return

        // Load schedule settings
        val settings = settingsDao.getSettingsOnce() ?: return

        // Determine today's day of week (1=Mon … 7=Sun)
        val cal = Calendar.getInstance()
        cal.timeInMillis = todayStart
        val javaDow = cal.get(Calendar.DAY_OF_WEEK) // Calendar: 1=Sun, 2=Mon … 7=Sat
        val dayOfWeek = if (javaDow == Calendar.SUNDAY) 7 else javaDow - 1 // → 1=Mon, 7=Sun

        // Check if today is a training day per the bitmask
        val mask = 1 shl (dayOfWeek - 1)
        if (settings.trainingDaysMask and mask == 0) return

        // Determine the current week type (1 or 2) based on weeks elapsed since schedule start
        val startDay = startOfDay(settings.startDate)
        val weeksSinceStart = ((todayStart - startDay) / (7L * 24 * 60 * 60 * 1000)).toInt()
            .coerceAtLeast(0)
        val weekType = if (weeksSinceStart % 2 == 0) {
            settings.startWeekType
        } else {
            if (settings.startWeekType == 1) 2 else 1
        }

        // Look up pattern for (weekType, dayOfWeek)
        val patterns = weekPatternDao.getPatternsByWeek(weekType)
        val pattern = patterns.find { it.dayOfWeek == dayOfWeek } ?: return

        // Create the session (with exercises)
        createQuickSession(pattern.programType)
    }
}
