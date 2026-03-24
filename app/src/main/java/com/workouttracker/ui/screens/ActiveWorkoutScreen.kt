package com.workouttracker.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.workouttracker.domain.model.Recommendation
import com.workouttracker.domain.model.RecommendationType
import com.workouttracker.ui.theme.*
import com.workouttracker.ui.viewmodel.ActiveExerciseWithSets
import com.workouttracker.ui.viewmodel.ActiveSetInput
import com.workouttracker.ui.viewmodel.ActiveWorkoutViewModel

// ──────────────────────────────────────────────
//  Helpers
// ──────────────────────────────────────────────

private fun formatElapsed(secs: Long): String {
    val h = secs / 3600
    val m = (secs % 3600) / 60
    val s = secs % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

private fun formatWeight(w: Float): String =
    if (w == w.toLong().toFloat()) w.toLong().toString() else "%.1f".format(w)

// ──────────────────────────────────────────────
//  Main Screen
// ──────────────────────────────────────────────

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

    // Exercise detail dialog
    val selectedIdx = uiState.selectedExerciseIndex
    if (selectedIdx != null && selectedIdx < uiState.exercisesWithSets.size) {
        ExerciseDetailDialog(
            exerciseWithSets = uiState.exercisesWithSets[selectedIdx],
            exerciseIndex = selectedIdx,
            restTimerRunning = uiState.restTimerRunning,
            restTimerSeconds = uiState.restTimerSeconds,
            restTimerDuration = uiState.restTimerDuration,
            onSkipRest = { viewModel.skipRestTimer() },
            onSetChanged = { setIdx, updated -> viewModel.updateSetInput(selectedIdx, setIdx, updated) },
            onSetDone = { setIdx, updated ->
                viewModel.updateSetInput(selectedIdx, setIdx, updated.copy(isDone = true))
                viewModel.markSetDone(uiState.exercisesWithSets[selectedIdx].exercise.id, updated)
            },
            onDismiss = { viewModel.selectExercise(null) }
        )
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "${uiState.exercisesWithSets.size} упражнений",
                                style = MaterialTheme.typography.labelSmall,
                                color = ColorOnSurface
                            )
                            Text("·", color = ColorOnSurface, style = MaterialTheme.typography.labelSmall)
                            Text(
                                formatElapsed(uiState.elapsedSeconds),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (uiState.isPaused) ColorOnSurface else ColorPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            if (uiState.isPaused) {
                                Text(
                                    "⏸ пауза",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ColorOnSurface
                                )
                            }
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
            Column {
                // Rest timer (global — visible when no dialog is open)
                if (uiState.restTimerRunning && selectedIdx == null) {
                    RestTimerBar(
                        seconds = uiState.restTimerSeconds,
                        totalSeconds = uiState.restTimerDuration,
                        onSkip = { viewModel.skipRestTimer() }
                    )
                }
                Surface(color = ColorBackground) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Pause / Resume button
                        OutlinedButton(
                            onClick = {
                                if (uiState.isPaused) viewModel.resumeWorkout()
                                else viewModel.pauseWorkout()
                            },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, ColorSurfaceVariant),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (uiState.isPaused) ColorSecondary else ColorOnSurface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = if (uiState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (uiState.isPaused) "Продолжить" else "Пауза",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        // Finish button
                        Button(
                            onClick = { showCompleteDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = ColorSecondary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Завершить",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(
                uiState.exercisesWithSets,
                key = { _, item -> item.exercise.id }
            ) { exIdx, exWithSets ->
                ExerciseSummaryCard(
                    exerciseWithSets = exWithSets,
                    onClick = { viewModel.selectExercise(exIdx) }
                )
            }
        }
    }

    // Confirm finish dialog
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

// ──────────────────────────────────────────────
//  Exercise summary card (in main list)
// ──────────────────────────────────────────────

@Composable
fun ExerciseSummaryCard(
    exerciseWithSets: ActiveExerciseWithSets,
    onClick: () -> Unit
) {
    val ex = exerciseWithSets.exercise
    val rec = exerciseWithSets.recommendation
    val doneSets = exerciseWithSets.sets.count { it.isDone }
    val totalSets = exerciseWithSets.sets.size
    val allDone = doneSets == totalSets && totalSets > 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ColorSurface),
        border = BorderStroke(
            width = 1.dp,
            color = if (allDone) ColorSecondary.copy(alpha = 0.5f) else ColorSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ── Header row: name + sets counter ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    ex.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ColorOnBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$doneSets/$totalSets",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (allDone) ColorSecondary else ColorPrimary
                    )
                    Text(
                        "подходов",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorOnSurface
                    )
                }
                if (allDone) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Выполнено",
                        tint = ColorSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // ── Plan line ──
            Spacer(Modifier.height(4.dp))
            Text(
                "${ex.plannedSets}×${ex.plannedMinReps}-${ex.plannedMaxReps} @ ${formatWeight(ex.plannedWeight)} кг",
                style = MaterialTheme.typography.bodySmall,
                color = ColorOnSurface
            )

            // ── Previous workout data ──
            if (rec?.prevWeight != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    "Прошлая: ${formatWeight(rec.prevWeight)} кг × ${rec.prevReps} повт · RIR ${rec.prevRir}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorOnSurface
                )
            }

            // ── Recommendation badges ──
            val showBadges = rec != null || exerciseWithSets.hasPlateau
            if (showBadges) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (exerciseWithSets.hasPlateau) {
                        RecommendationBadge(
                            bgColor = ColorError.copy(alpha = 0.15f),
                            textColor = ColorError,
                            label = "⚠ Плато"
                        )
                    }
                    if (rec != null) {
                        val (bg, tc, label) = recommendationBadgeParams(rec)
                        RecommendationBadge(bgColor = bg, textColor = tc, label = label)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationBadge(bgColor: androidx.compose.ui.graphics.Color, textColor: androidx.compose.ui.graphics.Color, label: String) {
    Surface(color = bgColor, shape = RoundedCornerShape(6.dp)) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}

private data class BadgeParams(
    val bgColor: androidx.compose.ui.graphics.Color,
    val textColor: androidx.compose.ui.graphics.Color,
    val label: String
)

private fun recommendationBadgeParams(rec: Recommendation): BadgeParams {
    return when (rec.type) {
        RecommendationType.INCREASE_WEIGHT -> {
            val weightStr = rec.nextWeight?.let { " ${formatWeight(it)} кг" } ?: ""
            BadgeParams(ColorSecondary.copy(alpha = 0.15f), ColorSecondary, "⬆$weightStr")
        }
        RecommendationType.DECREASE_WEIGHT -> {
            val weightStr = rec.nextWeight?.let { " ${formatWeight(it)} кг" } ?: " -5 кг"
            BadgeParams(ColorError.copy(alpha = 0.15f), ColorError, "⬇$weightStr")
        }
        RecommendationType.INCREASE_REPS ->
            BadgeParams(ColorPrimary.copy(alpha = 0.12f), ColorPrimary, "👍 продолжать")
        RecommendationType.SLOW_NEGATIVE ->
            BadgeParams(ColorSurfaceVariant, ColorOnSurface, "🐢 замедлить")
        RecommendationType.ADD_PAUSE ->
            BadgeParams(ColorSurfaceVariant, ColorOnSurface, "⏸ с паузой")
        RecommendationType.PLATEAU ->
            BadgeParams(ColorError.copy(alpha = 0.15f), ColorError, "⚠ Плато")
    }
}

// ──────────────────────────────────────────────
//  Exercise detail dialog
// ──────────────────────────────────────────────

@Composable
fun ExerciseDetailDialog(
    exerciseWithSets: ActiveExerciseWithSets,
    exerciseIndex: Int,
    restTimerRunning: Boolean,
    restTimerSeconds: Int,
    restTimerDuration: Int,
    onSkipRest: () -> Unit,
    onSetChanged: (Int, ActiveSetInput) -> Unit,
    onSetDone: (Int, ActiveSetInput) -> Unit,
    onDismiss: () -> Unit
) {
    val ex = exerciseWithSets.exercise
    var localSets by remember(exerciseWithSets.sets) { mutableStateOf(exerciseWithSets.sets) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(20.dp),
            color = ColorSurface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Header ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            ex.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ColorOnBackground
                        )
                        Text(
                            "План: ${ex.plannedSets}×${ex.plannedMinReps}-${ex.plannedMaxReps} @ ${formatWeight(ex.plannedWeight)} кг",
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorOnSurface
                        )
                        // Previous workout data (reference line)
                        exerciseWithSets.recommendation?.prevWeight?.let { pw ->
                            val rec = exerciseWithSets.recommendation
                            Text(
                                "Прошлая: ${formatWeight(pw)} кг × ${rec.prevReps} повт · RIR ${rec.prevRir}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ColorOnSurface
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Закрыть", tint = ColorOnSurface)
                    }
                }

                // ── Today's goal block ──
                run {
                    val rec = exerciseWithSets.recommendation
                    val todayWeight = rec?.nextWeight ?: exerciseWithSets.exercise.plannedWeight
                    val repsMin = rec?.targetRepsMin ?: exerciseWithSets.exercise.plannedMinReps
                    val repsMax = rec?.targetRepsMax ?: exerciseWithSets.exercise.plannedMaxReps
                    val weightColor = when (rec?.type) {
                        RecommendationType.INCREASE_WEIGHT -> ColorSecondary
                        RecommendationType.DECREASE_WEIGHT -> ColorError
                        else -> ColorOnBackground
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        color = ColorSurfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                            Text(
                                "🎯 ЦЕЛЬ СЕГОДНЯ",
                                style = MaterialTheme.typography.labelSmall,
                                color = ColorOnSurface,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                                Column {
                                    Text(
                                        "ВЕС",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ColorOnSurface
                                    )
                                    Text(
                                        "${formatWeight(todayWeight)} кг",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = weightColor
                                    )
                                }
                                Column {
                                    Text(
                                        "ПОВТОРЫ",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ColorOnSurface
                                    )
                                    Text(
                                        "$repsMin–$repsMax",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Recommendation explanation ──
                exerciseWithSets.recommendation?.let { rec ->
                    val (icon, bgColor) = when (rec.type) {
                        RecommendationType.INCREASE_WEIGHT -> "⬆️" to ColorSecondary.copy(alpha = 0.12f)
                        RecommendationType.DECREASE_WEIGHT -> "⚠️" to ColorError.copy(alpha = 0.12f)
                        RecommendationType.INCREASE_REPS -> "👍" to ColorPrimary.copy(alpha = 0.10f)
                        RecommendationType.PLATEAU -> "⚠️" to ColorError.copy(alpha = 0.12f)
                        RecommendationType.SLOW_NEGATIVE -> "🐢" to ColorSurfaceVariant
                        RecommendationType.ADD_PAUSE -> "⏸️" to ColorSurfaceVariant
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        color = bgColor,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(icon, fontSize = 18.sp)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                rec.text,
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorOnBackground
                            )
                        }
                    }
                }

                // ── Rest timer (inside dialog) ──
                if (restTimerRunning) {
                    RestTimerBar(
                        seconds = restTimerSeconds,
                        totalSeconds = restTimerDuration,
                        onSkip = onSkipRest
                    )
                }

                HorizontalDivider(
                    color = ColorSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // ── Column headers ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("№", style = MaterialTheme.typography.labelSmall, color = ColorOnSurface,
                        modifier = Modifier.weight(0.4f))
                    Text("Вес, кг", style = MaterialTheme.typography.labelSmall, color = ColorOnSurface,
                        modifier = Modifier.weight(1.3f), textAlign = TextAlign.Center)
                    Text("Повт.", style = MaterialTheme.typography.labelSmall, color = ColorOnSurface,
                        modifier = Modifier.weight(1.1f), textAlign = TextAlign.Center)
                    Text("RIR", style = MaterialTheme.typography.labelSmall, color = ColorOnSurface,
                        modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.weight(0.8f))
                }

                Spacer(Modifier.height(4.dp))

                // ── Set rows (scrollable) ──
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    localSets.forEachIndexed { idx, setInput ->
                        ActiveSetRow(
                            setInput = setInput,
                            onChanged = { updated ->
                                val newList = localSets.toMutableList().also { it[idx] = updated }
                                localSets = newList
                                onSetChanged(idx, updated)
                            },
                            onDone = { updated ->
                                val newList = localSets.toMutableList()
                                    .also { it[idx] = updated.copy(isDone = true) }
                                localSets = newList
                                onSetDone(idx, updated)
                            }
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
//  Set row (with RIR)
// ──────────────────────────────────────────────

@Composable
fun ActiveSetRow(
    setInput: ActiveSetInput,
    onChanged: (ActiveSetInput) -> Unit,
    onDone: (ActiveSetInput) -> Unit
) {
    var weightText by remember(setInput.setIndex, setInput.isDone) {
        mutableStateOf(setInput.actualWeight.let {
            if (it == it.toLong().toFloat()) it.toLong().toString() else it.toString()
        })
    }
    var repsText by remember(setInput.setIndex, setInput.isDone) {
        mutableStateOf(setInput.actualReps.toString())
    }
    var rirText by remember(setInput.setIndex, setInput.isDone) {
        mutableStateOf(setInput.rir.toString())
    }

    val rowColor = if (setInput.isDone) ColorSecondary.copy(alpha = 0.08f) else ColorBackground.copy(alpha = 0f)

    Surface(
        color = rowColor,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.padding(vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Set index
            Text(
                "${setInput.setIndex}",
                modifier = Modifier.weight(0.4f).padding(start = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = if (setInput.isDone) ColorSecondary else ColorOnSurface,
                fontWeight = if (setInput.isDone) FontWeight.Bold else FontWeight.Normal
            )

            // Weight field
            CompactTextField(
                value = weightText,
                onValueChange = { v ->
                    weightText = v
                    onChanged(setInput.copy(
                        actualWeight = v.toFloatOrNull() ?: setInput.actualWeight,
                        actualReps = repsText.toIntOrNull() ?: setInput.actualReps,
                        rir = rirText.toIntOrNull() ?: setInput.rir
                    ))
                },
                modifier = Modifier.weight(1.3f).padding(horizontal = 2.dp),
                enabled = !setInput.isDone,
                isDone = setInput.isDone,
                keyboardType = KeyboardType.Decimal
            )

            // Reps field
            CompactTextField(
                value = repsText,
                onValueChange = { v ->
                    repsText = v
                    onChanged(setInput.copy(
                        actualReps = v.toIntOrNull() ?: setInput.actualReps,
                        actualWeight = weightText.toFloatOrNull() ?: setInput.actualWeight,
                        rir = rirText.toIntOrNull() ?: setInput.rir
                    ))
                },
                modifier = Modifier.weight(1.1f).padding(horizontal = 2.dp),
                enabled = !setInput.isDone,
                isDone = setInput.isDone,
                keyboardType = KeyboardType.Number
            )

            // RIR field
            CompactTextField(
                value = rirText,
                onValueChange = { v ->
                    rirText = v
                    onChanged(setInput.copy(
                        rir = v.toIntOrNull() ?: setInput.rir,
                        actualWeight = weightText.toFloatOrNull() ?: setInput.actualWeight,
                        actualReps = repsText.toIntOrNull() ?: setInput.actualReps
                    ))
                },
                modifier = Modifier.weight(0.9f).padding(horizontal = 2.dp),
                enabled = !setInput.isDone,
                isDone = setInput.isDone,
                keyboardType = KeyboardType.Number
            )

            // Done button
            IconButton(
                onClick = {
                    val current = setInput.copy(
                        actualWeight = weightText.toFloatOrNull() ?: setInput.actualWeight,
                        actualReps = repsText.toIntOrNull() ?: setInput.actualReps,
                        rir = rirText.toIntOrNull() ?: setInput.rir
                    )
                    onDone(current)
                },
                modifier = Modifier.weight(0.8f),
                enabled = !setInput.isDone
            ) {
                Icon(
                    Icons.Default.Check,
                    "Выполнено",
                    tint = if (setInput.isDone) ColorSecondary else ColorOnSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isDone: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Number
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.Center),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ColorPrimary,
            unfocusedBorderColor = if (isDone) ColorSecondary else ColorSurfaceVariant,
            focusedTextColor = ColorOnBackground,
            unfocusedTextColor = ColorOnBackground,
            cursorColor = ColorPrimary,
            disabledTextColor = ColorOnSurface,
            disabledBorderColor = if (isDone) ColorSecondary.copy(alpha = 0.4f) else ColorSurfaceVariant
        ),
        enabled = enabled
    )
}

// ──────────────────────────────────────────────
//  Rest timer bar
// ──────────────────────────────────────────────

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
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Circular timer
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(56.dp)
            ) {
                Canvas(modifier = Modifier.size(56.dp)) {
                    val strokeWidth = 5.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    val center = Offset(size.width / 2, size.height / 2)
                    drawCircle(
                        color = ColorSurfaceVariant,
                        radius = radius,
                        center = center,
                        style = Stroke(width = strokeWidth)
                    )
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
                    fontSize = 12.sp
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
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorOnSurface),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Пропустить", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
