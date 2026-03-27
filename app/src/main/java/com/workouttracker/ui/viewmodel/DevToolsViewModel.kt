package com.workouttracker.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workouttracker.data.db.dao.UserDao
import com.workouttracker.data.db.entities.*
import com.workouttracker.data.repository.BodyTrackerRepository
import com.workouttracker.data.repository.ProgramRepository
import com.workouttracker.data.repository.ScheduleRepository
import com.workouttracker.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class DevToolsViewModel @Inject constructor(
    private val application: Application,
    private val sessionRepository: SessionRepository,
    private val scheduleRepository: ScheduleRepository,
    private val programRepository: ProgramRepository,
    private val bodyTrackerRepository: BodyTrackerRepository,
    private val userDao: UserDao
) : ViewModel() {

    fun resetWorkouts(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            sessionRepository.deleteAllWorkouts()
            onDone()
        }
    }

    fun resetSchedule(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            scheduleRepository.deleteSchedule()
            onDone()
        }
    }

    fun resetPrograms(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            programRepository.deleteAllPrograms()
            onDone()
        }
    }

    fun resetBodyMeasurements(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            bodyTrackerRepository.deleteAll()
            onDone()
        }
    }

    fun resetAll(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            sessionRepository.deleteAllWorkouts()
            scheduleRepository.deleteSchedule()
            programRepository.deleteAllPrograms()
            bodyTrackerRepository.deleteAll()
            userDao.deleteAll()
            application.getSharedPreferences("workout_prefs", Context.MODE_PRIVATE)
                .edit().clear().apply()
            onDone()
        }
    }

    fun generateMockData(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            // 1. Clear everything first
            sessionRepository.deleteAllWorkouts()
            scheduleRepository.deleteSchedule()
            programRepository.deleteAllPrograms()
            bodyTrackerRepository.deleteAll()
            userDao.deleteAll()

            // 1.5. Create user
            userDao.insertUser(User(name = "Артём", units = "kg", gender = "male"))
            application.getSharedPreferences("workout_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("feature_onboarding_seen", true).apply()

            // 2. Create programs
            val programAId = programRepository.insertProgram(
                WorkoutProgram(type = "A", name = "Верх тела")
            )
            val programBId = programRepository.insertProgram(
                WorkoutProgram(type = "B", name = "Низ тела")
            )

            // 3. Create exercises for Program A (upper body)
            val exA1 = programRepository.insertExercise(ProgramExercise(
                programId = programAId, orderIndex = 0, name = "Жим штанги лёжа",
                sets = 3, minReps = 8, maxReps = 12, startWeight = 60f, startWeightNote = "barbell"
            ))
            val exA2 = programRepository.insertExercise(ProgramExercise(
                programId = programAId, orderIndex = 1, name = "Тяга штанги в наклоне",
                sets = 3, minReps = 8, maxReps = 12, startWeight = 50f, startWeightNote = "barbell"
            ))
            val exA3 = programRepository.insertExercise(ProgramExercise(
                programId = programAId, orderIndex = 2, name = "Жим гантелей сидя",
                sets = 3, minReps = 10, maxReps = 15, startWeight = 14f, startWeightNote = "dumbbell"
            ))

            // 4. Create exercises for Program B (lower body)
            val exB1 = programRepository.insertExercise(ProgramExercise(
                programId = programBId, orderIndex = 0, name = "Приседания со штангой",
                sets = 3, minReps = 6, maxReps = 10, startWeight = 80f, startWeightNote = "barbell"
            ))
            val exB2 = programRepository.insertExercise(ProgramExercise(
                programId = programBId, orderIndex = 1, name = "Румынская тяга",
                sets = 3, minReps = 8, maxReps = 12, startWeight = 60f, startWeightNote = "barbell"
            ))
            val exB3 = programRepository.insertExercise(ProgramExercise(
                programId = programBId, orderIndex = 2, name = "Жим ногами",
                sets = 3, minReps = 10, maxReps = 15, startWeight = 100f, startWeightNote = "barbell"
            ))

            // 5. Create schedule: Mon/Wed/Fri, 2-week cycle
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val today = cal.timeInMillis
            val twoWeeksAgo = today - 14L * 24 * 60 * 60 * 1000

            scheduleRepository.saveSettings(ScheduleSettings(
                trainingDaysMask = 1 + 4 + 16, // Mon + Wed + Fri
                startDate = twoWeeksAgo,
                cycleLengthWeeks = 2,
                startWeekType = 1
            ))

            scheduleRepository.savePatterns(listOf(
                WeekPattern(weekType = 1, dayOfWeek = 1, programType = "A"),
                WeekPattern(weekType = 1, dayOfWeek = 3, programType = "B"),
                WeekPattern(weekType = 1, dayOfWeek = 5, programType = "A"),
                WeekPattern(weekType = 2, dayOfWeek = 1, programType = "B"),
                WeekPattern(weekType = 2, dayOfWeek = 3, programType = "A"),
                WeekPattern(weekType = 2, dayOfWeek = 5, programType = "B")
            ))

            // 6. Create 6 completed sessions over 2 weeks
            data class MockExerciseData(
                val programExerciseId: Long,
                val name: String,
                val sets: Int,
                val minReps: Int,
                val maxReps: Int,
                val weight: Float
            )

            val exercisesA = listOf(
                MockExerciseData(exA1, "Жим штанги лёжа", 3, 8, 12, 60f),
                MockExerciseData(exA2, "Тяга штанги в наклоне", 3, 8, 12, 50f),
                MockExerciseData(exA3, "Жим гантелей сидя", 3, 10, 15, 14f)
            )
            val exercisesB = listOf(
                MockExerciseData(exB1, "Приседания со штангой", 3, 6, 10, 80f),
                MockExerciseData(exB2, "Румынская тяга", 3, 8, 12, 60f),
                MockExerciseData(exB3, "Жим ногами", 3, 10, 15, 100f)
            )

            // Training days: Mon/Wed/Fri from 2 weeks ago
            val trainingDays = mutableListOf<Pair<Long, String>>() // date to programType
            cal.timeInMillis = twoWeeksAgo
            // Find first Monday
            while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                cal.add(Calendar.DAY_OF_MONTH, 1)
            }

            // Week 1: A, B, A
            val week1Mon = cal.timeInMillis
            trainingDays.add(week1Mon to "A")
            cal.add(Calendar.DAY_OF_MONTH, 2) // Wed
            trainingDays.add(cal.timeInMillis to "B")
            cal.add(Calendar.DAY_OF_MONTH, 2) // Fri
            trainingDays.add(cal.timeInMillis to "A")

            // Week 2: B, A, B
            cal.add(Calendar.DAY_OF_MONTH, 3) // next Mon
            trainingDays.add(cal.timeInMillis to "B")
            cal.add(Calendar.DAY_OF_MONTH, 2) // Wed
            trainingDays.add(cal.timeInMillis to "A")
            cal.add(Calendar.DAY_OF_MONTH, 2) // Fri
            trainingDays.add(cal.timeInMillis to "B")

            // Only create sessions that are in the past
            val pastDays = trainingDays.filter { it.first < today }

            var sessionNum = 0
            for ((date, programType) in pastDays) {
                val sessionId = sessionRepository.insertSession(
                    WorkoutSession(date = date, programType = programType, status = SessionStatus.DONE)
                )

                val exercises = if (programType == "A") exercisesA else exercisesB
                for ((orderIdx, exData) in exercises.withIndex()) {
                    // Slight progression over sessions
                    val weightBonus = sessionNum * 2.5f
                    val sessionWeight = exData.weight + weightBonus

                    val sessionExId = sessionRepository.insertSessionExercise(
                        WorkoutSessionExercise(
                            sessionId = sessionId,
                            programExerciseId = exData.programExerciseId,
                            orderIndex = orderIdx,
                            name = exData.name,
                            plannedSets = exData.sets,
                            plannedMinReps = exData.minReps,
                            plannedMaxReps = exData.maxReps,
                            plannedWeight = sessionWeight,
                            actualSetsDone = exData.sets
                        )
                    )

                    // Create set facts
                    val setFacts = (1..exData.sets).map { setIndex ->
                        val repsVariation = listOf(-1, 0, 0, 1).random()
                        val actualReps = (exData.minReps + 2 + repsVariation).coerceIn(exData.minReps, exData.maxReps)
                        val rir = listOf(1, 2, 2, 3).random()
                        WorkoutSetFact(
                            sessionExerciseId = sessionExId,
                            setIndex = setIndex,
                            plannedReps = exData.minReps,
                            plannedWeight = sessionWeight,
                            actualReps = actualReps,
                            actualWeight = sessionWeight,
                            rir = rir
                        )
                    }
                    sessionRepository.insertSetFacts(setFacts)
                }
                sessionNum++
            }

            // 7. Generate planned schedule
            scheduleRepository.generateSchedule()

            // 8. Create body measurements (4 entries over ~2 weeks)
            val dayMs = 24L * 60 * 60 * 1000
            val measurements = listOf(
                BodyMeasurement(date = today - 13 * dayMs, weight = 82.0f, height = 178f,
                    waist = 88.0f, neck = 38.0f, chest = 102.0f, hips = 98.0f, thigh = 58.0f, arm = 35.0f, age = 28),
                BodyMeasurement(date = today - 9 * dayMs, weight = 81.5f, height = 178f,
                    waist = 87.5f, neck = 38.0f, chest = 102.0f, hips = 97.5f, thigh = 58.0f, arm = 35.2f, age = 28),
                BodyMeasurement(date = today - 4 * dayMs, weight = 81.2f, height = 178f,
                    waist = 87.0f, neck = 38.5f, chest = 102.5f, hips = 97.0f, thigh = 58.5f, arm = 35.5f, age = 28),
                BodyMeasurement(date = today, weight = 80.8f, height = 178f,
                    waist = 86.5f, neck = 38.5f, chest = 103.0f, hips = 97.0f, thigh = 59.0f, arm = 35.8f, age = 28)
            )
            for (m in measurements) {
                bodyTrackerRepository.insert(m)
            }

            onDone()
        }
    }
}
