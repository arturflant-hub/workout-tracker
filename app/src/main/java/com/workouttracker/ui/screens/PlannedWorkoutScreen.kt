package com.workouttracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.workouttracker.data.db.entities.WorkoutSessionExercise
import com.workouttracker.data.repository.ProgramRepository
import com.workouttracker.data.repository.SessionRepository
import com.workouttracker.domain.model.Recommendation
import com.workouttracker.domain.model.RecommendationType
import com.workouttracker.domain.usecase.ProgressionUseCase
import com.workouttracker.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class PlannedExerciseItem(
    val exercise: WorkoutSessionExercise,
    val recommendation: Recommendation?
)

data class PlannedWorkoutUiState(
    val sessionId: Long = 0,
    val programType: String = "",
    val date: Long = 0,
    val exercises: List<PlannedExerciseItem> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class PlannedWorkoutViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val programRepository: ProgramRepository,
    private val progressionUseCase: ProgressionUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(PlannedWorkoutUiState())
    val state: StateFlow<PlannedWorkoutUiState> = _state.asStateFlow()

    fun load(sessionId: Long) {
        viewModelScope.launch {
            val session = sessionRepository.getSessionById(sessionId) ?: return@launch
            val exercises = sessionRepository.getExercisesBySessionOnce(sessionId)

            val items = exercises.map { ex ->
                val programExercise = programRepository.getExerciseById(ex.programExerciseId)
                val rec = programExercise?.let { progressionUseCase.getProgressionRecommendation(it) }
                PlannedExerciseItem(ex, rec)
            }

            _state.value = PlannedWorkoutUiState(
                sessionId = sessionId,
                programType = session.programType,
                date = session.date,
                exercises = items,
                isLoading = false
            )
        }
    }

    fun startWorkout(sessionId: Long, onStarted: () -> Unit) {
        viewModelScope.launch {
            sessionRepository.startSession(sessionId)
            onStarted()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannedWorkoutScreen(
    sessionId: Long,
    navController: NavController,
    viewModel: PlannedWorkoutViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val sdf = remember { SimpleDateFormat("EEE, d MMMM", Locale("ru")) }

    LaunchedEffect(sessionId) {
        viewModel.load(sessionId)
    }

    Scaffold(
        containerColor = ColorBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "План: Тренировка ${state.programType}",
                            color = ColorOnBackground,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (state.date > 0) {
                            Text(
                                sdf.format(Date(state.date)),
                                style = MaterialTheme.typography.labelSmall,
                                color = ColorOnSurface
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = ColorOnBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorBackground)
            )
        },
        bottomBar = {
            Surface(color = ColorBackground) {
                Button(
                    onClick = {
                        viewModel.startWorkout(sessionId) {
                            navController.navigate("active_workout/$sessionId") {
                                popUpTo("planned_workout/$sessionId") { inclusive = true }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "💪 Начать тренировку",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ColorPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.exercises) { item ->
                    PlannedExerciseCard(item = item)
                }
            }
        }
    }
}

@Composable
fun PlannedExerciseCard(item: PlannedExerciseItem) {
    val ex = item.exercise
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ColorSurface),
        border = BorderStroke(1.dp, ColorSurfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                ex.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = ColorOnBackground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${ex.plannedSets} × ${ex.plannedMinReps}-${ex.plannedMaxReps} повт @ ${ex.plannedWeight} кг",
                style = MaterialTheme.typography.bodySmall,
                color = ColorOnSurface
            )
            item.recommendation?.let { rec ->
                Spacer(Modifier.height(8.dp))
                val (icon, color) = when (rec.type) {
                    RecommendationType.INCREASE_WEIGHT -> "⬆ УВЕЛИЧИТЬ ВЕС" to ColorSecondary
                    RecommendationType.INCREASE_REPS -> "👍 РАБОТАТЬ НА ПОВТОРЕНИЯ" to ColorPrimary
                    RecommendationType.SLOW_NEGATIVE -> "⚠ ЗАМЕДЛИТЬ НЕГАТИВ" to ColorOnSurface
                    RecommendationType.ADD_PAUSE -> "⚠ ДОБАВИТЬ ПАУЗУ" to ColorOnSurface
                }
                Text(
                    icon,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    rec.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorOnSurface
                )
            }
        }
    }
}
