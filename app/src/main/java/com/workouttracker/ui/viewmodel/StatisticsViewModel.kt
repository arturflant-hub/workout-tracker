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
import com.workouttracker.domain.usecase.BodyMetricsCalculator
import java.util.Calendar
import javax.inject.Inject

data class TonnagePoint(val date: Long, val tonnage: Float, val programType: String = "")
data class ExerciseProgressPoint(val date: Long, val weight: Float, val e1rm: Float)
data class BodyWeightPoint(val date: Long, val weight: Float)
data class BodyFatPoint(val date: Long, val bodyFat: Float)
data class WeeklyVolumePoint(val weekStart: Long, val tonnage: Float, val programType: String = "")

data class StatisticsUiState(
    val tonnagePoints: List<TonnagePoint> = emptyList(),
    val weeklyVolumePoints: List<WeeklyVolumePoint> = emptyList(),
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
                            val bestSet = sets.maxByOrNull { it.actualWeight }
                            val e1rm = bestSet?.let {
                                BodyMetricsCalculator.e1rm(it.actualWeight, it.actualReps)
                            } ?: maxWeight
                            exerciseHistoryMap.getOrPut(ex.name) { mutableListOf() }
                                .add(ExerciseProgressPoint(session.date, maxWeight, e1rm))
                        }
                    }
                    tonnagePoints.add(TonnagePoint(session.date, sessionTonnage, session.programType))
                }

                // Weekly volume aggregation — sum tonnage per calendar week, pick dominant type
                val weeklyMap = mutableMapOf<Long, Pair<Float, String>>()
                tonnagePoints.forEach { point ->
                    val weekStart = startOfWeek(point.date)
                    val existing = weeklyMap[weekStart]
                    weeklyMap[weekStart] = if (existing == null) {
                        point.tonnage to point.programType
                    } else {
                        (existing.first + point.tonnage) to existing.second
                    }
                }
                val weeklyVolumePoints = weeklyMap.entries
                    .sortedBy { it.key }
                    .map { (weekStart, pair) -> WeeklyVolumePoint(weekStart, pair.first, pair.second) }

                val exerciseNames = exerciseHistoryMap.keys.toList().sorted()
                val currentExercise = _state.value.selectedExercise ?: exerciseNames.firstOrNull()
                val progressPoints = currentExercise?.let { exerciseHistoryMap[it] } ?: emptyList()

                _state.update {
                    it.copy(
                        tonnagePoints = tonnagePoints,
                        weeklyVolumePoints = weeklyVolumePoints,
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
                        val e1rm = BodyMetricsCalculator.e1rm(bestSet.actualWeight, bestSet.actualReps)
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
        return BodyMetricsCalculator.bodyFatNavy(waist, neck, m.height)
    }

    private fun startOfWeek(millis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
