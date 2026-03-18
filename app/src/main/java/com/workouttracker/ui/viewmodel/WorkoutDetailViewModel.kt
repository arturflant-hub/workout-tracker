package com.workouttracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workouttracker.data.db.entities.SessionStatus
import com.workouttracker.data.db.entities.WorkoutSession
import com.workouttracker.data.db.entities.WorkoutSessionExercise
import com.workouttracker.data.db.entities.WorkoutSetFact
import com.workouttracker.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class ExerciseDetailItem(
    val exercise: WorkoutSessionExercise,
    val actualSets: List<WorkoutSetFact> = emptyList(),
    val prevExercise: WorkoutSessionExercise? = null,
    val prevSets: List<WorkoutSetFact> = emptyList(),
    val recommendation: String = "👍",
    val currentE1RM: Float = 0f,
    val prevE1RM: Float = 0f,
    val currentTonnage: Float = 0f,
    val prevTonnage: Float = 0f,
    val currentReps: Int = 0,
    val prevReps: Int = 0
)

data class WorkoutDetailUiState(
    val session: WorkoutSession? = null,
    val exercises: List<ExerciseDetailItem> = emptyList(),
    val isLoading: Boolean = true,
    val isToday: Boolean = false
)

@HiltViewModel
class WorkoutDetailViewModel @Inject constructor(
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(WorkoutDetailUiState())
    val state: StateFlow<WorkoutDetailUiState> = _state.asStateFlow()

    fun load(sessionId: Long) {
        viewModelScope.launch {
            val session = sessionRepository.getSessionById(sessionId) ?: return@launch
            val exercises = sessionRepository.getExercisesBySessionOnce(sessionId)
            val isToday = isTodayDate(session.date)

            // Get previous completed session of same type (before current session date)
            val prevSession = sessionRepository.getPreviousSessionByType(
                programType = session.programType,
                beforeDate = session.date + 1 // include same-day sessions in case
            )
            val prevExercises: List<WorkoutSessionExercise> = if (prevSession != null) {
                sessionRepository.getExercisesBySessionOnce(prevSession.id)
            } else emptyList()

            val items = exercises.map { ex ->
                // Actual sets — load if session is DONE or IN_PROGRESS
                val actualSets: List<WorkoutSetFact> =
                    if (session.status == SessionStatus.DONE || session.status == SessionStatus.IN_PROGRESS) {
                        sessionRepository.getSetsForExercise(ex.id)
                    } else emptyList()

                // Match prev exercise by programExerciseId
                val prevEx = prevExercises.find { it.programExerciseId == ex.programExerciseId }
                val prevSets: List<WorkoutSetFact> = if (prevEx != null) {
                    sessionRepository.getSetsForExercise(prevEx.id)
                } else emptyList()

                // e1RM = weight × (1 + reps / 30)
                val currentE1RM: Float = if (actualSets.isNotEmpty()) {
                    actualSets.maxOf { s -> s.actualWeight * (1f + s.actualReps / 30f) }
                } else {
                    ex.plannedWeight * (1f + ex.plannedMaxReps / 30f)
                }

                val prevE1RM: Float = if (prevSets.isNotEmpty()) {
                    prevSets.maxOf { s -> s.actualWeight * (1f + s.actualReps / 30f) }
                } else if (prevEx != null) {
                    prevEx.plannedWeight * (1f + prevEx.plannedMaxReps / 30f)
                } else 0f

                // Recommendation
                val recommendation: String = if (prevE1RM <= 0f) {
                    "👍"
                } else {
                    val change = (currentE1RM - prevE1RM) / prevE1RM
                    when {
                        change > 0.025f -> "⬆"
                        change < -0.025f -> "⚠"
                        else -> "👍"
                    }
                }

                // Tonnage (kg × reps per set, summed)
                val currentTonnage: Float = if (actualSets.isNotEmpty()) {
                    actualSets.sumOf { s -> (s.actualWeight * s.actualReps).toDouble() }.toFloat()
                } else {
                    ex.plannedWeight * ex.plannedSets * ex.plannedMaxReps
                }

                val prevTonnage: Float = if (prevSets.isNotEmpty()) {
                    prevSets.sumOf { s -> (s.actualWeight * s.actualReps).toDouble() }.toFloat()
                } else if (prevEx != null) {
                    prevEx.plannedWeight * prevEx.plannedSets * prevEx.plannedMaxReps
                } else 0f

                // Total reps
                val currentReps: Int = if (actualSets.isNotEmpty()) {
                    actualSets.sumOf { s -> s.actualReps }
                } else {
                    ex.plannedSets * ex.plannedMaxReps
                }

                val prevReps: Int = if (prevSets.isNotEmpty()) {
                    prevSets.sumOf { s -> s.actualReps }
                } else if (prevEx != null) {
                    prevEx.plannedSets * prevEx.plannedMaxReps
                } else 0

                ExerciseDetailItem(
                    exercise = ex,
                    actualSets = actualSets,
                    prevExercise = prevEx,
                    prevSets = prevSets,
                    recommendation = recommendation,
                    currentE1RM = currentE1RM,
                    prevE1RM = prevE1RM,
                    currentTonnage = currentTonnage,
                    prevTonnage = prevTonnage,
                    currentReps = currentReps,
                    prevReps = prevReps
                )
            }

            _state.value = WorkoutDetailUiState(
                session = session,
                exercises = items,
                isLoading = false,
                isToday = isToday
            )
        }
    }

    fun startWorkout(sessionId: Long, onStarted: () -> Unit) {
        viewModelScope.launch {
            sessionRepository.startSession(sessionId)
            onStarted()
        }
    }

    private fun isTodayDate(dateMillis: Long): Boolean {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis
        val todayEnd = todayStart + 24L * 60 * 60 * 1000 - 1
        return dateMillis in todayStart..todayEnd
    }
}
