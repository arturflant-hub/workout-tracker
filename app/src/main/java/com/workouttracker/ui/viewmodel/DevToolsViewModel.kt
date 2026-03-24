package com.workouttracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workouttracker.data.repository.BodyTrackerRepository
import com.workouttracker.data.repository.ProgramRepository
import com.workouttracker.data.repository.ScheduleRepository
import com.workouttracker.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DevToolsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val scheduleRepository: ScheduleRepository,
    private val programRepository: ProgramRepository,
    private val bodyTrackerRepository: BodyTrackerRepository
) : ViewModel() {

    fun resetWorkouts(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            sessionRepository.deleteAllWorkouts()
            onDone()
        }
    }

    fun resetSchedule(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            scheduleRepository.deleteSchedule()
            onDone()
        }
    }

    fun resetPrograms(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            programRepository.deleteAllPrograms()
            onDone()
        }
    }

    fun resetBodyMeasurements(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            bodyTrackerRepository.deleteAll()
            onDone()
        }
    }

    fun resetAll(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            sessionRepository.deleteAllWorkouts()
            scheduleRepository.deleteSchedule()
            programRepository.deleteAllPrograms()
            bodyTrackerRepository.deleteAll()
            onDone()
        }
    }
}
