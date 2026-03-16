package com.workouttracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.workouttracker.data.db.entities.SessionStatus
import com.workouttracker.ui.viewmodel.ExerciseWithSets
import com.workouttracker.ui.viewmodel.SetInput
import com.workouttracker.ui.viewmodel.WorkoutSessionViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSessionScreen(
    sessionId: Long,
    navController: NavController,
    viewModel: WorkoutSessionViewModel = hiltViewModel()
) {
    val session by viewModel.session.collectAsState()
    val exercisesWithSets by viewModel.exercisesWithSets.collectAsState()
    var showCompleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(sessionId) {
        viewModel.loadSession(sessionId)
    }

    val sdf = SimpleDateFormat("EEE, d MMMM", Locale("ru"))
    val isEditable = session?.status == SessionStatus.PLANNED || session?.status == SessionStatus.IN_PROGRESS || session?.status == null

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Тренировка ${session?.programType ?: ""}")
                        session?.let {
                            Text(
                                sdf.format(Date(it.date)),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Назад")
                    }
                },
                actions = {
                    if (isEditable) {
                        TextButton(onClick = {
                            viewModel.skipSession { navController.popBackStack() }
                        }) {
                            Text("Пропустить")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (isEditable) {
                Surface(shadowElevation = 8.dp) {
                    Button(
                        onClick = { showCompleteDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Завершить тренировку")
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (session?.status == SessionStatus.DONE) {
                item {
                    Card(colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )) {
                        Text(
                            "✅ Тренировка завершена",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            items(exercisesWithSets, key = { it.exercise.id }) { exerciseWithSets ->
                ExerciseSetCard(
                    exerciseWithSets = exerciseWithSets,
                    isEditable = isEditable,
                    onSetChanged = { updatedSet ->
                        viewModel.updateSetFact(exerciseWithSets.exercise.id, updatedSet)
                    }
                )
            }
        }
    }

    if (showCompleteDialog) {
        AlertDialog(
            onDismissRequest = { showCompleteDialog = false },
            title = { Text("Завершить тренировку?") },
            text = { Text("Результаты будут сохранены") },
            confirmButton = {
                TextButton(onClick = {
                    showCompleteDialog = false
                    viewModel.completeSession { navController.popBackStack() }
                }) { Text("Завершить") }
            },
            dismissButton = {
                TextButton(onClick = { showCompleteDialog = false }) { Text("Отмена") }
            }
        )
    }
}

@Composable
fun ExerciseSetCard(
    exerciseWithSets: ExerciseWithSets,
    isEditable: Boolean,
    onSetChanged: (SetInput) -> Unit
) {
    val exercise = exerciseWithSets.exercise
    var sets by remember(exerciseWithSets) { mutableStateOf(exerciseWithSets.sets) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                exercise.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${exercise.plannedSets} × ${exercise.plannedMinReps}-${exercise.plannedMaxReps} @ ${exercise.plannedWeight}кг",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text("Подход", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                Text("План повт.", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1.5f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Text("Факт повт.", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1.5f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Text("Вес, кг", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1.5f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            sets.forEachIndexed { idx, setInput ->
                SetRow(
                    setInput = setInput,
                    isEditable = isEditable,
                    onChanged = { updated ->
                        val newSets = sets.toMutableList().also { it[idx] = updated }
                        sets = newSets
                        onSetChanged(updated)
                    }
                )
            }
        }
    }
}

@Composable
fun SetRow(
    setInput: SetInput,
    isEditable: Boolean,
    onChanged: (SetInput) -> Unit
) {
    var actualReps by remember(setInput) { mutableStateOf(
        if (setInput.actualReps > 0) setInput.actualReps.toString() else ""
    ) }
    var actualWeight by remember(setInput) { mutableStateOf(
        if (setInput.actualWeight > 0f) setInput.actualWeight.toString() else ""
    ) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Text(
            "${setInput.setIndex}",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            "${setInput.plannedReps}",
            modifier = Modifier.weight(1.5f),
            style = MaterialTheme.typography.bodySmall,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (isEditable) {
            OutlinedTextField(
                value = actualReps,
                onValueChange = { v ->
                    actualReps = v
                    val reps = v.toIntOrNull() ?: 0
                    val weight = actualWeight.toFloatOrNull() ?: setInput.plannedWeight
                    onChanged(setInput.copy(actualReps = reps, actualWeight = weight))
                },
                modifier = Modifier
                    .weight(1.5f)
                    .padding(horizontal = 2.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                value = actualWeight,
                onValueChange = { v ->
                    actualWeight = v
                    val reps = actualReps.toIntOrNull() ?: 0
                    val weight = v.toFloatOrNull() ?: setInput.plannedWeight
                    onChanged(setInput.copy(actualReps = reps, actualWeight = weight))
                },
                modifier = Modifier
                    .weight(1.5f)
                    .padding(horizontal = 2.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall
            )
        } else {
            Text(
                "${setInput.actualReps}",
                modifier = Modifier.weight(1.5f),
                style = MaterialTheme.typography.bodySmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                "${setInput.actualWeight}",
                modifier = Modifier.weight(1.5f),
                style = MaterialTheme.typography.bodySmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
