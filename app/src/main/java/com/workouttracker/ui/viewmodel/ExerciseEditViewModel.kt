package com.workouttracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workouttracker.data.db.entities.ProgramExercise
import com.workouttracker.data.repository.ProgramRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExerciseEditViewModel @Inject constructor(
    private val repository: ProgramRepository
) : ViewModel() {

    private val _exercise = MutableStateFlow<ProgramExercise?>(null)
    val exercise: StateFlow<ProgramExercise?> = _exercise

    fun loadExercise(id: Long) {
        viewModelScope.launch {
            _exercise.value = repository.getExerciseById(id)
        }
    }

    fun saveExercise(exercise: ProgramExercise, onDone: () -> Unit) {
        viewModelScope.launch {
            if (exercise.id == 0L) {
                repository.insertExercise(exercise)
            } else {
                repository.updateExercise(exercise)
            }
            onDone()
        }
    }
}
