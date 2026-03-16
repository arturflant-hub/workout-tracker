package com.workouttracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workouttracker.data.db.entities.ProgramExercise
import com.workouttracker.data.db.entities.WorkoutProgram
import com.workouttracker.data.repository.ProgramRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProgramsViewModel @Inject constructor(
    private val repository: ProgramRepository
) : ViewModel() {

    val programs: StateFlow<List<WorkoutProgram>> = repository.getAllPrograms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedProgramId = MutableStateFlow<Long?>(null)
    val selectedProgramId: StateFlow<Long?> = _selectedProgramId

    val exercises: StateFlow<List<ProgramExercise>> = _selectedProgramId
        .flatMapLatest { id ->
            if (id != null) repository.getExercisesByProgram(id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectProgram(id: Long) { _selectedProgramId.value = id }

    fun createProgram(type: String, name: String) {
        viewModelScope.launch {
            repository.insertProgram(WorkoutProgram(type = type, name = name))
        }
    }

    fun updateProgram(program: WorkoutProgram) {
        viewModelScope.launch { repository.updateProgram(program) }
    }

    fun deleteProgram(program: WorkoutProgram) {
        viewModelScope.launch { repository.deleteProgram(program) }
    }

    fun deleteExercise(exercise: ProgramExercise) {
        viewModelScope.launch { repository.deleteExercise(exercise) }
    }
}
