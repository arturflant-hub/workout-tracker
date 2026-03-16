package com.workouttracker.ui.viewmodel

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workouttracker.data.db.entities.SessionStatus
import com.workouttracker.data.db.entities.WorkoutSession
import com.workouttracker.data.db.entities.WorkoutSessionExercise
import com.workouttracker.data.db.entities.WorkoutSetFact
import com.workouttracker.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class ActiveSetInput(
    val setIndex: Int,
    val plannedReps: Int,
    val plannedWeight: Float,
    var actualReps: Int,
    var actualWeight: Float,
    var factId: Long = 0L,
    var isDone: Boolean = false
)

data class ActiveExerciseWithSets(
    val exercise: WorkoutSessionExercise,
    val sets: List<ActiveSetInput>
)

data class ActiveWorkoutUiState(
    val session: WorkoutSession? = null,
    val exercisesWithSets: List<ActiveExerciseWithSets> = emptyList(),
    val restTimerSeconds: Int = 0,
    val restTimerRunning: Boolean = false,
    val restTimerDuration: Int = 90,
    val isCompleted: Boolean = false
)

@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    private val repository: SessionRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("workout_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    val restTimerDuration: Int get() = prefs.getInt("rest_timer_duration", 90)

    fun loadSession(sessionId: Long) {
        viewModelScope.launch {
            val session = repository.getSessionById(sessionId) ?: return@launch
            // Mark session as IN_PROGRESS immediately so it persists across navigation
            if (session.status == SessionStatus.PLANNED) {
                repository.startSession(sessionId)
            }
            val updatedSession = repository.getSessionById(sessionId) ?: session
            _uiState.update { it.copy(session = updatedSession, restTimerDuration = restTimerDuration) }

            repository.getExercisesBySession(sessionId).collect { exercises ->
                val result = exercises.map { ex ->
                    val existingSets = repository.getSetsForExercise(ex.id)
                    val sets = if (existingSets.isNotEmpty()) {
                        existingSets.map { sf ->
                            ActiveSetInput(
                                setIndex = sf.setIndex,
                                plannedReps = sf.plannedReps,
                                plannedWeight = sf.plannedWeight,
                                actualReps = sf.actualReps,
                                actualWeight = sf.actualWeight,
                                factId = sf.id,
                                isDone = sf.actualReps > 0
                            )
                        }
                    } else {
                        (1..ex.plannedSets).map { idx ->
                            ActiveSetInput(
                                setIndex = idx,
                                plannedReps = ex.plannedMinReps,
                                plannedWeight = ex.plannedWeight,
                                actualReps = ex.plannedMinReps,
                                actualWeight = ex.plannedWeight
                            )
                        }
                    }
                    ActiveExerciseWithSets(ex, sets)
                }
                _uiState.update { it.copy(exercisesWithSets = result) }
            }
        }
    }

    fun markSetDone(exerciseId: Long, setInput: ActiveSetInput) {
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
            startRestTimer()
        }
    }

    fun updateSetInput(exerciseIdx: Int, setIdx: Int, updated: ActiveSetInput) {
        val current = _uiState.value.exercisesWithSets.toMutableList()
        val ex = current[exerciseIdx]
        val newSets = ex.sets.toMutableList()
        newSets[setIdx] = updated
        current[exerciseIdx] = ex.copy(sets = newSets)
        _uiState.update { it.copy(exercisesWithSets = current) }
    }

    fun startRestTimer() {
        timerJob?.cancel()
        val duration = _uiState.value.restTimerDuration
        _uiState.update { it.copy(restTimerRunning = true, restTimerSeconds = duration) }
        timerJob = viewModelScope.launch {
            var remaining = duration
            while (remaining > 0) {
                delay(1000)
                remaining--
                _uiState.update { it.copy(restTimerSeconds = remaining) }
            }
            _uiState.update { it.copy(restTimerRunning = false, restTimerSeconds = 0) }
            vibrate()
        }
    }

    fun skipRestTimer() {
        timerJob?.cancel()
        _uiState.update { it.copy(restTimerRunning = false, restTimerSeconds = 0) }
    }

    fun setRestTimerDuration(seconds: Int) {
        prefs.edit().putInt("rest_timer_duration", seconds).apply()
        _uiState.update { it.copy(restTimerDuration = seconds) }
    }

    fun completeWorkout(onDone: () -> Unit) {
        viewModelScope.launch {
            _uiState.value.session?.id?.let { id ->
                repository.completeSession(id)
                _uiState.update { it.copy(isCompleted = true) }
                onDone()
            }
        }
    }

    private fun vibrate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(VibratorManager::class.java)
                vm?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(500)
                }
            }
        } catch (_: Exception) {}
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
