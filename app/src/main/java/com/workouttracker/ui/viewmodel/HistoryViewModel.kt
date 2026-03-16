package com.workouttracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workouttracker.data.db.entities.ProgramExercise
import com.workouttracker.data.db.entities.WorkoutSessionExercise
import com.workouttracker.data.db.entities.WorkoutSetFact
import com.workouttracker.data.repository.ProgramRepository
import com.workouttracker.data.repository.SessionRepository
import com.workouttracker.domain.model.Recommendation
import com.workouttracker.domain.usecase.ProgressionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExerciseHistory(
    val programExercise: ProgramExercise,
    val sessions: List<Pair<WorkoutSessionExercise, List<WorkoutSetFact>>>,
    val recommendation: Recommendation?
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val programRepository: ProgramRepository,
    private val progressionUseCase: ProgressionUseCase
) : ViewModel() {

    val programs = programRepository.getAllPrograms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedProgramId = MutableStateFlow<Long?>(null)
    val selectedProgramId: StateFlow<Long?> = _selectedProgramId

    val exercises: StateFlow<List<ProgramExercise>> = _selectedProgramId
        .flatMapLatest { id ->
            if (id != null) programRepository.getExercisesByProgram(id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedExercise = MutableStateFlow<ProgramExercise?>(null)
    val selectedExercise: StateFlow<ProgramExercise?> = _selectedExercise

    private val _history = MutableStateFlow<ExerciseHistory?>(null)
    val history: StateFlow<ExerciseHistory?> = _history

    fun selectProgram(id: Long) { _selectedProgramId.value = id }

    fun loadHistory(programExercise: ProgramExercise) {
        _selectedExercise.value = programExercise
        viewModelScope.launch {
            val sessions = sessionRepository.getHistoryByProgramExercise(programExercise.id)
            val sessionPairs = sessions.map { ex ->
                val sets = sessionRepository.getSetsForExercise(ex.id)
                Pair(ex, sets)
            }
            val recommendation = progressionUseCase.getProgressionRecommendation(programExercise)
            _history.value = ExerciseHistory(programExercise, sessionPairs, recommendation)
        }
    }
}
