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

    private val _exercises = MutableStateFlow<List<ProgramExercise>>(emptyList())
    val exercises: StateFlow<List<ProgramExercise>> = _exercises.asStateFlow()

    init {
        viewModelScope.launch {
            _selectedProgramId.flatMapLatest { id ->
                if (id != null) repository.getExercisesByProgram(id)
                else flowOf(emptyList())
            }.collect { list ->
                _exercises.value = list
            }
        }
    }

    fun selectProgram(id: Long) { _selectedProgramId.value = id }

    fun createProgram(type: String, name: String) {
        viewModelScope.launch {
            val newId = repository.insertProgram(WorkoutProgram(type = type, name = name))
            _selectedProgramId.value = newId
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

    /** Called during drag to reorder the list in memory (no DB write). */
    fun moveExercise(fromIndex: Int, toIndex: Int) {
        val list = _exercises.value.toMutableList()
        if (fromIndex < 0 || toIndex < 0 || fromIndex >= list.size || toIndex >= list.size) return
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        _exercises.value = list
    }

    /** Called when drag ends — persists the current order to DB. */
    fun persistExerciseOrder() {
        viewModelScope.launch {
            val updated = _exercises.value.mapIndexed { idx, ex -> ex.copy(orderIndex = idx) }
            _exercises.value = updated
            repository.updateExerciseOrder(updated)
        }
    }
}
