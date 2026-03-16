package com.workouttracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workouttracker.data.db.entities.BodyMeasurement
import com.workouttracker.data.db.entities.SessionStatus
import com.workouttracker.data.db.entities.WorkoutSession
import com.workouttracker.data.repository.BodyTrackerRepository
import com.workouttracker.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.log10

data class TonnagePoint(val date: Long, val tonnage: Float)
data class ExerciseProgressPoint(val date: Long, val weight: Float, val e1rm: Float)
data class BodyWeightPoint(val date: Long, val weight: Float)
data class BodyFatPoint(val date: Long, val bodyFat: Float)

data class StatisticsUiState(
    val tonnagePoints: List<TonnagePoint> = emptyList(),
    val exerciseNames: List<String> = emptyList(),
    val selectedExercise: String? = null,
    val exerciseProgressPoints: List<ExerciseProgressPoint> = emptyList(),
    val bodyWeightPoints: List<BodyWeightPoint> = emptyList(),
    val bodyFatPoints: List<BodyFatPoint> = emptyList()
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val bodyTrackerRepository: BodyTrackerRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StatisticsUiState())
    val state: StateFlow<StatisticsUiState> = _state.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            // Body measurements
            bodyTrackerRepository.getAll().collect { measurements ->
                val weightPoints = measurements.map { BodyWeightPoint(it.date, it.weight) }.sortedBy { it.date }
                val fatPoints = measurements.mapNotNull { m ->
                    val fat = calcBodyFat(m) ?: return@mapNotNull null
                    BodyFatPoint(m.date, fat)
                }.sortedBy { it.date }
                _state.update { it.copy(bodyWeightPoints = weightPoints, bodyFatPoints = fatPoints) }
            }
        }

        viewModelScope.launch {
            sessionRepository.getAllSessions().collect { allSessions ->
                val doneSessions = allSessions.filter { it.status == SessionStatus.DONE }.sortedBy { it.date }

                val tonnagePoints = mutableListOf<TonnagePoint>()
                val exerciseHistoryMap = mutableMapOf<String, MutableList<ExerciseProgressPoint>>()

                doneSessions.forEach { session ->
                    val exercises = sessionRepository.getExercisesBySessionOnce(session.id)
                    var sessionTonnage = 0f
                    exercises.forEach { ex ->
                        val sets = sessionRepository.getSetsForExercise(ex.id)
                        var maxWeight = 0f
                        sets.forEach { set ->
                            sessionTonnage += set.actualReps * set.actualWeight
                            if (set.actualWeight > maxWeight) maxWeight = set.actualWeight
                        }
                        if (maxWeight > 0f) {
                            // e1RM = weight * (1 + reps/30) — Epley formula, use best set
                            val bestSet = sets.maxByOrNull { it.actualWeight }
                            val e1rm = bestSet?.let { it.actualWeight * (1 + it.actualReps / 30f) } ?: maxWeight
                            exerciseHistoryMap.getOrPut(ex.name) { mutableListOf() }
                                .add(ExerciseProgressPoint(session.date, maxWeight, e1rm))
                        }
                    }
                    tonnagePoints.add(TonnagePoint(session.date, sessionTonnage))
                }

                val exerciseNames = exerciseHistoryMap.keys.toList().sorted()
                val currentExercise = _state.value.selectedExercise ?: exerciseNames.firstOrNull()
                val progressPoints = currentExercise?.let { exerciseHistoryMap[it] } ?: emptyList()

                _state.update {
                    it.copy(
                        tonnagePoints = tonnagePoints,
                        exerciseNames = exerciseNames,
                        selectedExercise = currentExercise,
                        exerciseProgressPoints = progressPoints
                    )
                }
            }
        }
    }

    fun selectExercise(name: String) {
        viewModelScope.launch {
            val allSessions = sessionRepository.getAllSessions().first()
            val doneSessions = allSessions.filter { it.status == SessionStatus.DONE }.sortedBy { it.date }
            val points = mutableListOf<ExerciseProgressPoint>()
            doneSessions.forEach { session ->
                val exercises = sessionRepository.getExercisesBySessionOnce(session.id)
                exercises.filter { it.name == name }.forEach { ex ->
                    val sets = sessionRepository.getSetsForExercise(ex.id)
                    val bestSet = sets.maxByOrNull { it.actualWeight }
                    if (bestSet != null && bestSet.actualWeight > 0f) {
                        val e1rm = bestSet.actualWeight * (1 + bestSet.actualReps / 30f)
                        points.add(ExerciseProgressPoint(session.date, bestSet.actualWeight, e1rm))
                    }
                }
            }
            _state.update { it.copy(selectedExercise = name, exerciseProgressPoints = points) }
        }
    }

    private fun calcBodyFat(m: BodyMeasurement): Float? {
        val waist = m.waist ?: return null
        val neck = m.neck ?: return null
        val diff = waist - neck
        if (diff <= 0f || m.height <= 0f) return null
        return (495.0 / (1.0324 - 0.19077 * log10(diff.toDouble()) + 0.15456 * log10(m.height.toDouble())) - 450.0).toFloat()
    }
}
