package com.workouttracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.workouttracker.data.db.entities.ProgramExercise
import com.workouttracker.data.db.entities.WorkoutSetFact
import com.workouttracker.domain.model.RecommendationType
import com.workouttracker.ui.viewmodel.ExerciseHistory
import com.workouttracker.ui.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val programs by viewModel.programs.collectAsState()
    val selectedProgramId by viewModel.selectedProgramId.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val history by viewModel.history.collectAsState()
    val selectedExercise by viewModel.selectedExercise.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("История тренировок") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Program filter
            if (programs.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    programs.forEach { program ->
                        FilterChip(
                            selected = program.id == selectedProgramId,
                            onClick = { viewModel.selectProgram(program.id) },
                            label = { Text("${program.type}: ${program.name}") }
                        )
                    }
                }

                // Exercise filter
                if (exercises.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        exercises.forEach { ex ->
                            FilterChip(
                                selected = selectedExercise?.id == ex.id,
                                onClick = { viewModel.loadHistory(ex) },
                                label = { Text(ex.name, maxLines = 1) }
                            )
                        }
                    }
                }
            }

            // History content
            if (history == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Выберите программу и упражнение для просмотра истории",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                history?.let { h ->
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Recommendation card
                        h.recommendation?.let { rec ->
                            item {
                                RecommendationCard(
                                    type = rec.type,
                                    text = rec.text
                                )
                            }
                        }

                        items(h.sessions, key = { it.first.id }) { (sessionEx, sets) ->
                            SessionHistoryCard(
                                sessionExercise = sessionEx,
                                sets = sets
                            )
                        }

                        if (h.sessions.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Нет завершённых тренировок для этого упражнения")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecommendationCard(type: RecommendationType, text: String) {
    val icon = when (type) {
        RecommendationType.INCREASE_WEIGHT -> Icons.Default.FitnessCenter
        RecommendationType.INCREASE_REPS -> Icons.Default.TrendingUp
        RecommendationType.SLOW_NEGATIVE -> Icons.Default.SlowMotionVideo
        RecommendationType.ADD_PAUSE -> Icons.Default.Pause
    }
    val containerColor = when (type) {
        RecommendationType.INCREASE_WEIGHT -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, "Рекомендация", modifier = Modifier.size(28.dp))
            Column {
                Text(
                    "Рекомендация",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(text, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun SessionHistoryCard(
    sessionExercise: com.workouttracker.data.db.entities.WorkoutSessionExercise,
    sets: List<WorkoutSetFact>
) {
    val sdf = SimpleDateFormat("dd.MM.yyyy", Locale("ru"))

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                sessionExercise.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Запланировано: ${sessionExercise.plannedSets}×${sessionExercise.plannedMinReps}-${sessionExercise.plannedMaxReps} @ ${sessionExercise.plannedWeight}кг",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (sets.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                sets.forEach { set ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Подход ${set.setIndex}", style = MaterialTheme.typography.bodySmall)
                        val repsColor = when {
                            set.actualReps >= set.plannedReps -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.error
                        }
                        Text(
                            "${set.actualReps} повт. × ${set.actualWeight}кг",
                            style = MaterialTheme.typography.bodySmall,
                            color = repsColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                Text(
                    "Факт не записан",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
