package com.workouttracker.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.workouttracker.ui.theme.*
import com.workouttracker.ui.viewmodel.ActiveExerciseWithSets
import com.workouttracker.ui.viewmodel.ActiveSetInput
import com.workouttracker.ui.viewmodel.ActiveWorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    sessionId: Long,
    navController: NavController,
    viewModel: ActiveWorkoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCompleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(sessionId) {
        viewModel.loadSession(sessionId)
    }

    Scaffold(
        containerColor = ColorBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Тренировка ${uiState.session?.programType ?: ""}",
                            color = ColorOnBackground,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "${uiState.exercisesWithSets.size} упражнений",
                            style = MaterialTheme.typography.labelSmall,
                            color = ColorOnSurface
                        )
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
            Column {
                // Rest timer
                if (uiState.restTimerRunning) {
                    RestTimerBar(
                        seconds = uiState.restTimerSeconds,
                        totalSeconds = uiState.restTimerDuration,
                        onSkip = { viewModel.skipRestTimer() }
                    )
                }
                Surface(color = ColorBackground) {
                    Button(
                        onClick = { showCompleteDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ColorSecondary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Завершить тренировку",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(uiState.exercisesWithSets, key = { _, item -> item.exercise.id }) { exIdx, exWithSets ->
                ActiveExerciseCard(
                    exerciseWithSets = exWithSets,
                    onSetDone = { updatedSet ->
                        viewModel.updateSetInput(exIdx, updatedSet.setIndex - 1, updatedSet.copy(isDone = true))
                        viewModel.markSetDone(exWithSets.exercise.id, updatedSet)
                    },
                    onSetChanged = { setIdx, updatedSet ->
                        viewModel.updateSetInput(exIdx, setIdx, updatedSet)
                    }
                )
            }
        }
    }

    if (showCompleteDialog) {
        AlertDialog(
            onDismissRequest = { showCompleteDialog = false },
            containerColor = ColorSurface,
            title = {
                Text("Завершить тренировку?", color = ColorOnBackground, fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Все результаты будут сохранены", color = ColorOnSurface)
            },
            confirmButton = {
                TextButton(onClick = {
                    showCompleteDialog = false
                    viewModel.completeWorkout { navController.popBackStack() }
                }) {
                    Text("Завершить", color = ColorSecondary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCompleteDialog = false }) {
                    Text("Отмена", color = ColorOnSurface)
                }
            }
        )
    }
}

@Composable
fun RestTimerBar(
    seconds: Int,
    totalSeconds: Int,
    onSkip: () -> Unit
) {
    val progress = if (totalSeconds > 0) seconds.toFloat() / totalSeconds else 0f

    Surface(
        color = ColorSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Circular timer
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(64.dp)
            ) {
                Canvas(modifier = Modifier.size(64.dp)) {
                    val strokeWidth = 6.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    val center = Offset(size.width / 2, size.height / 2)
                    // Background circle
                    drawCircle(
                        color = ColorSurfaceVariant,
                        radius = radius,
                        center = center,
                        style = Stroke(width = strokeWidth)
                    )
                    // Progress arc
                    drawArc(
                        color = ColorPrimary,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Text(
                    text = "${seconds}с",
                    color = ColorOnBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    "Отдых",
                    style = MaterialTheme.typography.labelMedium,
                    color = ColorOnSurface
                )
                Text(
                    "$seconds / $totalSeconds сек",
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorOnBackground
                )
            }

            OutlinedButton(
                onClick = onSkip,
                border = BorderStroke(1.dp, ColorSurfaceVariant),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorOnSurface)
            ) {
                Text("Пропустить", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun ActiveExerciseCard(
    exerciseWithSets: ActiveExerciseWithSets,
    onSetDone: (ActiveSetInput) -> Unit,
    onSetChanged: (Int, ActiveSetInput) -> Unit
) {
    val ex = exerciseWithSets.exercise
    var localSets by remember(exerciseWithSets.sets) { mutableStateOf(exerciseWithSets.sets) }

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
            Text(
                "План: ${ex.plannedSets}×${ex.plannedMinReps}-${ex.plannedMaxReps} @ ${ex.plannedWeight}кг",
                style = MaterialTheme.typography.bodySmall,
                color = ColorOnSurface
            )

            Spacer(Modifier.height(12.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text("№", style = MaterialTheme.typography.labelSmall, color = ColorOnSurface, modifier = Modifier.weight(0.5f))
                Text("Повт.", style = MaterialTheme.typography.labelSmall, color = ColorOnSurface, modifier = Modifier.weight(1.5f), textAlign = TextAlign.Center)
                Text("Вес, кг", style = MaterialTheme.typography.labelSmall, color = ColorOnSurface, modifier = Modifier.weight(1.5f), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.weight(1f))
            }

            HorizontalDivider(color = ColorSurfaceVariant, modifier = Modifier.padding(vertical = 4.dp))

            localSets.forEachIndexed { idx, setInput ->
                ActiveSetRow(
                    setInput = setInput,
                    onChanged = { updated ->
                        val newList = localSets.toMutableList().also { it[idx] = updated }
                        localSets = newList
                        onSetChanged(idx, updated)
                    },
                    onDone = { updated ->
                        val newList = localSets.toMutableList().also { it[idx] = updated.copy(isDone = true) }
                        localSets = newList
                        onSetDone(updated)
                    }
                )
            }
        }
    }
}

@Composable
fun ActiveSetRow(
    setInput: ActiveSetInput,
    onChanged: (ActiveSetInput) -> Unit,
    onDone: (ActiveSetInput) -> Unit
) {
    var repsText by remember(setInput.setIndex) { mutableStateOf(setInput.actualReps.toString()) }
    var weightText by remember(setInput.setIndex) { mutableStateOf(setInput.actualWeight.toString()) }

    val rowColor = if (setInput.isDone) ColorSecondary.copy(alpha = 0.1f) else ColorBackground.copy(alpha = 0f)

    Surface(
        color = rowColor,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${setInput.setIndex}",
                modifier = Modifier.weight(0.5f),
                style = MaterialTheme.typography.bodySmall,
                color = if (setInput.isDone) ColorSecondary else ColorOnSurface,
                fontWeight = if (setInput.isDone) FontWeight.Bold else FontWeight.Normal
            )

            OutlinedTextField(
                value = repsText,
                onValueChange = { v ->
                    repsText = v
                    val current = setInput.copy(
                        actualReps = v.toIntOrNull() ?: setInput.actualReps,
                        actualWeight = weightText.toFloatOrNull() ?: setInput.actualWeight
                    )
                    onChanged(current)
                },
                modifier = Modifier
                    .weight(1.5f)
                    .padding(horizontal = 2.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.Center),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ColorPrimary,
                    unfocusedBorderColor = if (setInput.isDone) ColorSecondary else ColorSurfaceVariant,
                    focusedTextColor = ColorOnBackground,
                    unfocusedTextColor = ColorOnBackground,
                    cursorColor = ColorPrimary
                ),
                enabled = !setInput.isDone
            )

            OutlinedTextField(
                value = weightText,
                onValueChange = { v ->
                    weightText = v
                    val current = setInput.copy(
                        actualReps = repsText.toIntOrNull() ?: setInput.actualReps,
                        actualWeight = v.toFloatOrNull() ?: setInput.actualWeight
                    )
                    onChanged(current)
                },
                modifier = Modifier
                    .weight(1.5f)
                    .padding(horizontal = 2.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.Center),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ColorPrimary,
                    unfocusedBorderColor = if (setInput.isDone) ColorSecondary else ColorSurfaceVariant,
                    focusedTextColor = ColorOnBackground,
                    unfocusedTextColor = ColorOnBackground,
                    cursorColor = ColorPrimary
                ),
                enabled = !setInput.isDone
            )

            IconButton(
                onClick = {
                    val current = setInput.copy(
                        actualReps = repsText.toIntOrNull() ?: setInput.actualReps,
                        actualWeight = weightText.toFloatOrNull() ?: setInput.actualWeight
                    )
                    onDone(current)
                },
                modifier = Modifier.weight(1f),
                enabled = !setInput.isDone
            ) {
                Icon(
                    Icons.Default.Check,
                    "Выполнено",
                    tint = if (setInput.isDone) ColorSecondary else ColorOnSurface
                )
            }
        }
    }
}
