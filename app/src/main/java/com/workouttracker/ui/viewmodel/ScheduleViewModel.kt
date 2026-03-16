package com.workouttracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workouttracker.data.db.entities.ScheduleSettings
import com.workouttracker.data.db.entities.WeekPattern
import com.workouttracker.data.db.entities.WorkoutSession
import com.workouttracker.data.repository.ScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val repository: ScheduleRepository
) : ViewModel() {

    val settings: StateFlow<ScheduleSettings?> = repository.getSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val patterns: StateFlow<List<WeekPattern>> = repository.getAllPatterns()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSessions: StateFlow<List<WorkoutSession>> = repository.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    fun saveSettings(settings: ScheduleSettings) {
        viewModelScope.launch { repository.saveSettings(settings) }
    }

    fun savePatterns(patterns: List<WeekPattern>) {
        viewModelScope.launch { repository.savePatterns(patterns) }
    }

    fun generateSchedule() {
        viewModelScope.launch {
            _isGenerating.value = true
            try {
                repository.generateSchedule()
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun getSessionsInRange(from: Long, to: Long): Flow<List<WorkoutSession>> =
        repository.getSessionsInRange(from, to)
}
