package com.workouttracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workouttracker.data.db.entities.SessionStatus
import com.workouttracker.data.db.entities.WorkoutSession
import com.workouttracker.data.db.entities.WorkoutSessionExercise
import com.workouttracker.data.repository.BodyTrackerRepository
import com.workouttracker.data.repository.ScheduleRepository
import com.workouttracker.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.workouttracker.domain.usecase.BodyMetricsCalculator

data class DashboardState(
    val nextSession: WorkoutSession? = null,
    val nextSessionExercises: List<WorkoutSessionExercise> = emptyList(),
    val lastSession: WorkoutSession? = null,
    val lastSessionTonnage: Float = 0f,
    val currentWeight: Float? = null,
    val weightChange: Float? = null,
    val waistChange: Float? = null,
    val bodyFatPercent: Float? = null,
    val totalTonnage: Float = 0f,
    val workoutCount: Int = 0,
    val avgRir: Float? = null,
    val avgVolume: Float? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val sessionRepository: SessionRepository,
    private val bodyTrackerRepository: BodyTrackerRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            // Load sessions
            scheduleRepository.getAllSessions().collect { allSessions ->
                val now = System.currentTimeMillis()

                val nextSession = allSessions
                    .filter { it.status == SessionStatus.PLANNED && it.date >= now }
                    .minByOrNull { it.date }

                val lastSession = allSessions
                    .filter { it.status == SessionStatus.DONE }
                    .maxByOrNull { it.date }

                val doneSessions = allSessions.filter { it.status == SessionStatus.DONE }
                val workoutCount = doneSessions.size

                // Compute tonnage per session
                var totalTonnage = 0f
                var lastSessionTonnage = 0f
                var totalRir = 0f
                var rirCount = 0

                doneSessions.forEach { session ->
                    val exercises = sessionRepository.getExercisesBySessionOnce(session.id)
                    var sessionTonnage = 0f
                    exercises.forEach { ex ->
                        val sets = sessionRepository.getSetsForExercise(ex.id)
                        sets.forEach { set ->
                            sessionTonnage += set.actualReps * set.actualWeight
                            // RIR approximation: max_reps - actual_reps (if planned known)
                            val rir = (ex.plannedMaxReps - set.actualReps).coerceAtLeast(0)
                            totalRir += rir
                            rirCount++
                        }
                    }
                    totalTonnage += sessionTonnage
                    if (session.id == lastSession?.id) lastSessionTonnage = sessionTonnage
                }

                val avgVolume = if (workoutCount > 0) totalTonnage / workoutCount else null
                val avgRir = if (rirCount > 0) totalRir / rirCount else null

                // Next session exercises
                val nextSessionExercises = nextSession?.let {
                    sessionRepository.getExercisesBySessionOnce(it.id)
                } ?: emptyList()

                // Body metrics
                val latest = bodyTrackerRepository.getLatest()
                val first = bodyTrackerRepository.getFirst()
                val currentWeight = latest?.weight
                val weightChange = if (latest != null && first != null && latest.id != first.id)
                    latest.weight - first.weight else null
                val waistChange = if (latest?.waist != null && first?.waist != null && latest.id != first.id)
                    latest.waist - first.waist else null
                val bodyFat = if (latest != null && latest.waist != null && latest.neck != null)
                    BodyMetricsCalculator.bodyFatNavy(latest.waist, latest.neck, latest.height) else null

                _state.value = DashboardState(
                    nextSession = nextSession,
                    nextSessionExercises = nextSessionExercises,
                    lastSession = lastSession,
                    lastSessionTonnage = lastSessionTonnage,
                    currentWeight = currentWeight,
                    weightChange = weightChange,
                    waistChange = waistChange,
                    bodyFatPercent = bodyFat,
                    totalTonnage = totalTonnage,
                    workoutCount = workoutCount,
                    avgRir = avgRir,
                    avgVolume = avgVolume
                )
            }
        }
    }

}
