package com.workouttracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workouttracker.data.db.entities.WorkoutSession
import com.workouttracker.data.db.entities.WorkoutSessionExercise
import com.workouttracker.data.db.entities.WorkoutSetFact
import com.workouttracker.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetInput(
    val setIndex: Int,
    val plannedReps: Int,
    val plannedWeight: Float,
    var actualReps: Int = 0,
    var actualWeight: Float = 0f,
    var factId: Long = 0L
)

data class ExerciseWithSets(
    val exercise: WorkoutSessionExercise,
    val sets: List<SetInput>
)

@HiltViewModel
class WorkoutSessionViewModel @Inject constructor(
    private val repository: SessionRepository
) : ViewModel() {

    private val _session = MutableStateFlow<WorkoutSession?>(null)
    val session: StateFlow<WorkoutSession?> = _session

    private val _exercisesWithSets = MutableStateFlow<List<ExerciseWithSets>>(emptyList())
    val exercisesWithSets: StateFlow<List<ExerciseWithSets>> = _exercisesWithSets

    fun loadSession(sessionId: Long) {
        viewModelScope.launch {
            _session.value = repository.getSessionById(sessionId)
            repository.getExercisesBySession(sessionId)
                .collect { exercises ->
                    val result = exercises.map { ex ->
                        val existingSets = repository.getSetsForExercise(ex.id)
                        val sets = if (existingSets.isNotEmpty()) {
                            existingSets.map { sf ->
                                SetInput(
                                    setIndex = sf.setIndex,
                                    plannedReps = sf.plannedReps,
                                    plannedWeight = sf.plannedWeight,
                                    actualReps = sf.actualReps,
                                    actualWeight = sf.actualWeight,
                                    factId = sf.id
                                )
                            }
                        } else {
                            (1..ex.plannedSets).map { idx ->
                                SetInput(
                                    setIndex = idx,
                                    plannedReps = ex.plannedMinReps,
                                    plannedWeight = ex.plannedWeight
                                )
                            }
                        }
                        ExerciseWithSets(ex, sets)
                    }
                    _exercisesWithSets.value = result
                }
        }
    }

    fun updateSetFact(exerciseId: Long, setInput: SetInput) {
        viewModelScope.launch {
            val fact = WorkoutSetFact(
                id = setInput.factId,
                sessionExerciseId = exerciseId,
                setIndex = setInput.setIndex,
                plannedReps = setInput.plannedReps,
                plannedWeight = setInput.plannedWeight,
                actualReps = setInput.actualReps,
                actualWeight = setInput.actualWeight
            )
            if (setInput.factId == 0L) {
                repository.saveSetFact(fact)
            } else {
                repository.updateSetFact(fact)
            }
        }
    }

    fun completeSession(onDone: () -> Unit) {
        viewModelScope.launch {
            _session.value?.id?.let { id ->
                repository.completeSession(id)
                onDone()
            }
        }
    }

    fun skipSession(onDone: () -> Unit) {
        viewModelScope.launch {
            _session.value?.id?.let { id ->
                repository.skipSession(id)
                onDone()
            }
        }
    }
}
