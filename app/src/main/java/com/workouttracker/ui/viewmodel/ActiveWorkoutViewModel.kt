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
import com.workouttracker.data.repository.ProgramRepository
import com.workouttracker.data.repository.SessionRepository
import com.workouttracker.domain.model.Recommendation
import com.workouttracker.domain.model.RecommendationType
import com.workouttracker.domain.usecase.ProgressionUseCase
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
    var rir: Int = 0,
    var factId: Long = 0L,
    var isDone: Boolean = false
)

data class ActiveExerciseWithSets(
    val exercise: WorkoutSessionExercise,
    val sets: List<ActiveSetInput>,
    val recommendation: Recommendation? = null,
    val hasPlateau: Boolean = false,
    val prevComment: String? = null,
    val currentComment: String = ""
)

data class ActiveWorkoutUiState(
    val session: WorkoutSession? = null,
    val exercisesWithSets: List<ActiveExerciseWithSets> = emptyList(),
    val restTimerSeconds: Int = 0,
    val restTimerRunning: Boolean = false,
    val restTimerDuration: Int = 90,
    val isCompleted: Boolean = false,
    val elapsedSeconds: Long = 0L,
    val isPaused: Boolean = false,
    val selectedExerciseIndex: Int? = null,
    val isEditMode: Boolean = false
)

@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    private val repository: SessionRepository,
    private val programRepository: ProgramRepository,
    private val progressionUseCase: ProgressionUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("workout_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var workoutTimerJob: Job? = null

    val restTimerDuration: Int get() = prefs.getInt("rest_timer_duration", 90)

    fun loadSession(sessionId: Long) {
        viewModelScope.launch {
            val session = repository.getSessionById(sessionId) ?: return@launch
            val isEditMode = session.status == SessionStatus.DONE
            if (session.status == SessionStatus.PLANNED) {
                repository.startSession(sessionId)
            }
            val updatedSession = repository.getSessionById(sessionId) ?: session
            _uiState.update {
                it.copy(
                    session = updatedSession,
                    restTimerDuration = restTimerDuration,
                    isEditMode = isEditMode
                )
            }

            // Don't start workout timer in edit mode
            if (!isEditMode) {
                startWorkoutTimer()
            }

            repository.getExercisesBySession(sessionId).collect { exercises ->
                val result = exercises.map { ex ->
                    // Load recommendation first — needed for pre-filling new sets
                    val programEx = programRepository.getExerciseById(ex.programExerciseId)
                    val recommendation = programEx?.let {
                        try { progressionUseCase.getProgressionRecommendation(it) } catch (_: Exception) { null }
                    }
                    val hasPlateau = programEx?.let {
                        try { progressionUseCase.detectPlateau(it.id) } catch (_: Exception) { false }
                    } ?: false
                    val prevComment = programEx?.let {
                        try {
                            val history = repository.getHistoryByProgramExercise(it.id)
                            history.firstOrNull()?.comment?.takeIf { c -> c.isNotEmpty() }
                        } catch (_: Exception) { null }
                    }

                    val existingSets = repository.getSetsForExercise(ex.id)
                    val sets = if (existingSets.isNotEmpty()) {
                        existingSets.map { sf ->
                            ActiveSetInput(
                                setIndex = sf.setIndex,
                                plannedReps = sf.plannedReps,
                                plannedWeight = sf.plannedWeight,
                                actualReps = sf.actualReps,
                                actualWeight = sf.actualWeight,
                                rir = sf.rir,
                                factId = sf.id,
                                isDone = sf.actualReps > 0
                            )
                        }
                    } else {
                        val suggestedWeight = recommendation?.nextWeight ?: ex.plannedWeight
                        val suggestedReps = recommendation?.targetRepsMin ?: ex.plannedMinReps
                        (1..ex.plannedSets).map { idx ->
                            ActiveSetInput(
                                setIndex = idx,
                                plannedReps = ex.plannedMinReps,
                                plannedWeight = ex.plannedWeight,
                                actualReps = suggestedReps,
                                actualWeight = suggestedWeight
                            )
                        }
                    }

                    ActiveExerciseWithSets(
                        exercise = ex,
                        sets = sets,
                        recommendation = recommendation,
                        hasPlateau = hasPlateau,
                        prevComment = prevComment,
                        currentComment = ex.comment
                    )
                }
                _uiState.update { it.copy(exercisesWithSets = result) }
            }
        }
    }

    // ---------- Workout timer ----------

    private fun startWorkoutTimer() {
        workoutTimerJob?.cancel()
        workoutTimerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                if (!_uiState.value.isPaused) {
                    _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
                }
            }
        }
    }

    fun pauseWorkout() {
        _uiState.update { it.copy(isPaused = true) }
    }

    fun resumeWorkout() {
        _uiState.update { it.copy(isPaused = false) }
    }

    // ---------- Exercise selection ----------

    fun selectExercise(index: Int?) {
        _uiState.update { it.copy(selectedExerciseIndex = index) }
    }

    // ---------- Set management ----------

    fun markSetDone(exerciseId: Long, setInput: ActiveSetInput) {
        viewModelScope.launch {
            val fact = WorkoutSetFact(
                id = setInput.factId,
                sessionExerciseId = exerciseId,
                setIndex = setInput.setIndex,
                plannedReps = setInput.plannedReps,
                plannedWeight = setInput.plannedWeight,
                actualReps = setInput.actualReps,
                actualWeight = setInput.actualWeight,
                rir = setInput.rir
            )
            if (setInput.factId == 0L) {
                val newId = repository.saveSetFact(fact)
                val exList = _uiState.value.exercisesWithSets.toMutableList()
                val exIdx = exList.indexOfFirst { it.exercise.id == exerciseId }
                if (exIdx >= 0) {
                    val ex = exList[exIdx]
                    val setIdx = ex.sets.indexOfFirst { it.setIndex == setInput.setIndex }
                    if (setIdx >= 0) {
                        val newSets = ex.sets.toMutableList()
                        newSets[setIdx] = newSets[setIdx].copy(factId = newId, isDone = true)
                        exList[exIdx] = ex.copy(sets = newSets)
                        _uiState.update { it.copy(exercisesWithSets = exList) }
                    }
                }
            } else {
                repository.updateSetFact(fact)
            }
            if (!_uiState.value.isEditMode) {
                startRestTimer()
            }
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

    fun addSet(exerciseIdx: Int) {
        val current = _uiState.value.exercisesWithSets.toMutableList()
        val ex = current[exerciseIdx]
        val lastSet = ex.sets.lastOrNull()
        val newIndex = (lastSet?.setIndex ?: 0) + 1
        val newSet = ActiveSetInput(
            setIndex = newIndex,
            plannedReps = lastSet?.plannedReps ?: ex.exercise.plannedMinReps,
            plannedWeight = lastSet?.plannedWeight ?: ex.exercise.plannedWeight,
            actualReps = lastSet?.actualReps ?: ex.exercise.plannedMinReps,
            actualWeight = lastSet?.actualWeight ?: ex.exercise.plannedWeight
        )
        current[exerciseIdx] = ex.copy(sets = ex.sets + newSet)
        _uiState.update { it.copy(exercisesWithSets = current) }
    }

    fun saveAllSets(exerciseIdx: Int) {
        val ex = _uiState.value.exercisesWithSets.getOrNull(exerciseIdx) ?: return
        viewModelScope.launch {
            val updatedSets = mutableListOf<ActiveSetInput>()
            ex.sets.filter { it.isDone || it.factId != 0L }.forEach { set ->
                val fact = WorkoutSetFact(
                    id = set.factId,
                    sessionExerciseId = ex.exercise.id,
                    setIndex = set.setIndex,
                    plannedReps = set.plannedReps,
                    plannedWeight = set.plannedWeight,
                    actualReps = set.actualReps,
                    actualWeight = set.actualWeight,
                    rir = set.rir
                )
                val savedId = if (set.factId == 0L) repository.saveSetFact(fact) else {
                    repository.updateSetFact(fact)
                    set.factId
                }
                updatedSets += set.copy(factId = savedId, isDone = true)
            }
            val savedById = updatedSets.associateBy { it.setIndex }
            val mergedSets = ex.sets.map { savedById[it.setIndex] ?: it }
            val current = _uiState.value.exercisesWithSets.toMutableList()
            current[exerciseIdx] = ex.copy(sets = mergedSets)
            _uiState.update { it.copy(exercisesWithSets = current) }
        }
    }

    fun updateComment(exerciseIdx: Int, comment: String) {
        val current = _uiState.value.exercisesWithSets.toMutableList()
        val ex = current[exerciseIdx]
        current[exerciseIdx] = ex.copy(currentComment = comment)
        _uiState.update { it.copy(exercisesWithSets = current) }
        viewModelScope.launch {
            val updated = ex.exercise.copy(comment = comment)
            repository.updateExercise(updated)
        }
    }

    // ---------- Rest timer ----------

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

    // ---------- Complete ----------

    fun completeWorkout(onDone: () -> Unit) {
        viewModelScope.launch {
            _uiState.value.session?.id?.let { id ->
                repository.completeSession(id)
                _uiState.update { it.copy(isCompleted = true) }
                onDone()
            }
        }
    }

    // ---------- Vibration ----------

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
        workoutTimerJob?.cancel()
    }
}
