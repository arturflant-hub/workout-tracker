package com.workouttracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.workouttracker.data.db.entities.SessionStatus
import com.workouttracker.ui.navigation.Screen
import com.workouttracker.ui.theme.*
import com.workouttracker.ui.viewmodel.ExerciseDetailItem
import com.workouttracker.ui.viewmodel.WorkoutDetailViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    sessionId: Long,
    navController: NavController,
    viewModel: WorkoutDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val sdf = remember { SimpleDateFormat("EEE, d MMMM yyyy", Locale("ru")) }

    LaunchedEffect(sessionId) {
        viewModel.load(sessionId)
    }

    val session = state.session
    val isPlanned = session?.status == SessionStatus.PLANNED
    val showStartButton = isPlanned && state.isToday

    Scaffold(
        containerColor = ColorBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (session != null) "Тренировка ${session.programType}" else "Тренировка",
                            color = ColorOnBackground,
                            fontWeight = FontWeight.Bold
                        )
                        if (session != null) {
                            Text(
                                text = sdf.format(Date(session.date)),
                                style = MaterialTheme.typography.labelSmall,
                                color = ColorOnSurface
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = ColorOnBackground
                        )
                    }
                },
                actions = {
                    session?.let {
                        val (statusText, statusColor) = when (it.status) {
                            SessionStatus.PLANNED -> "Запланировано" to ColorOnSurface
                            SessionStatus.IN_PROGRESS -> "В процессе" to ColorSecondary
                            SessionStatus.DONE -> "Завершено" to ColorSecondary
                            SessionStatus.SKIPPED -> "Пропущено" to ColorError
                        }
                        Text(
                            text = statusText,
                            color = statusColor,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorBackground)
            )
        },
        bottomBar = {
            if (showStartButton) {
                Surface(color = ColorBackground) {
                    Button(
                        onClick = {
                            viewModel.startWorkout(sessionId) {
                                navController.navigate(Screen.ActiveWorkout.createRoute(sessionId)) {
                                    popUpTo(Screen.WorkoutDetail.createRoute(sessionId)) { inclusive = true }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "💪 Запустить тренировку",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ColorPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Session summary header
                session?.let { s ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = ColorSurface),
                            border = BorderStroke(1.dp, ColorPrimary.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                SummaryMetric(
                                    label = "Упражнений",
                                    value = "${state.exercises.size}"
                                )
                                SummaryMetric(
                                    label = "Тип",
                                    value = s.programType
                                )
                                SummaryMetric(
                                    label = "Подходов",
                                    value = "${state.exercises.sumOf { it.exercise.plannedSets }}"
                                )
                            }
                        }
                    }
                }

                if (state.exercises.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Упражнения не найдены",
                                color = ColorOnSurface,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    items(state.exercises, key = { it.exercise.id }) { item ->
                        ExerciseDetailCard(
                            item = item,
                            isCompleted = session?.status == SessionStatus.DONE
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = ColorPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = ColorOnSurface
        )
    }
}

@Composable
private fun ExerciseDetailCard(
    item: ExerciseDetailItem,
    isCompleted: Boolean
) {
    val ex = item.exercise
    val hasPrevData = item.prevE1RM > 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ColorSurface),
        border = BorderStroke(1.dp, ColorSurfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: name + recommendation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = ex.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ColorOnBackground,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(8.dp))
                val recColor = when (item.recommendation) {
                    "⬆" -> ColorSecondary
                    "⚠" -> ColorError
                    else -> ColorPrimary
                }
                Text(
                    text = item.recommendation,
                    fontSize = 20.sp,
                    color = recColor
                )
            }

            Spacer(Modifier.height(8.dp))

            // Plan row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📋 План: ",
                    style = MaterialTheme.typography.labelMedium,
                    color = ColorOnSurface
                )
                Text(
                    text = "${ex.plannedSets} × ${ex.plannedMinReps}-${ex.plannedMaxReps} повт  •  ${ex.plannedWeight} кг",
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorOnBackground
                )
            }

            // Actual row (if completed)
            if (isCompleted && item.actualSets.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                val totalActualReps = item.actualSets.sumOf { it.actualReps }
                val avgActualWeight = item.actualSets
                    .filter { it.actualReps > 0 }
                    .let { sets ->
                        if (sets.isEmpty()) 0f
                        else sets.sumOf { it.actualWeight.toDouble() }.toFloat() / sets.size
                    }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✅ Факт: ",
                        style = MaterialTheme.typography.labelMedium,
                        color = ColorSecondary
                    )
                    Text(
                        text = "${item.actualSets.size} подх  •  $totalActualReps повт  •  ${"%.1f".format(avgActualWeight)} кг",
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorOnBackground
                    )
                }
            }

            // e1RM info
            if (hasPrevData) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "e1RM: ",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorOnSurface
                    )
                    Text(
                        text = "${"%.1f".format(item.prevE1RM)} → ${"%.1f".format(item.currentE1RM)} кг",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorOnBackground
                    )
                }
            }

            // Mini bar charts (only if we have previous data)
            if (hasPrevData) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = ColorSurfaceVariant, thickness = 0.5.dp)
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MiniBarChart(
                        prevValue = item.prevTonnage,
                        currentValue = item.currentTonnage,
                        label = "Тоннаж",
                        unit = "кг"
                    )
                    MiniBarChart(
                        prevValue = item.prevE1RM,
                        currentValue = item.currentE1RM,
                        label = "e1RM",
                        unit = "кг"
                    )
                    MiniBarChart(
                        prevValue = item.prevReps.toFloat(),
                        currentValue = item.currentReps.toFloat(),
                        label = "Повторы",
                        unit = ""
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniBarChart(
    prevValue: Float,
    currentValue: Float,
    label: String,
    unit: String
) {
    val maxVal = maxOf(prevValue, currentValue, 1f)
    val barColor = ColorPrimary
    val prevBarColor = Color(0xFF3A3A3C)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = ColorOnSurface,
            fontSize = 10.sp
        )
        Spacer(Modifier.height(4.dp))

        Canvas(
            modifier = Modifier
                .width(64.dp)
                .height(52.dp)
        ) {
            val chartWidth = size.width
            val chartHeight = size.height
            val barWidth = chartWidth * 0.35f
            val gap = chartWidth * 0.1f
            val totalBarsWidth = barWidth * 2 + gap
            val startX = (chartWidth - totalBarsWidth) / 2f

            // Prev bar (gray)
            val prevHeightPx = (prevValue / maxVal) * chartHeight
            if (prevHeightPx > 0f) {
                drawRect(
                    color = prevBarColor,
                    topLeft = Offset(startX, chartHeight - prevHeightPx),
                    size = Size(barWidth, prevHeightPx)
                )
            }

            // Current bar (primary)
            val currHeightPx = (currentValue / maxVal) * chartHeight
            if (currHeightPx > 0f) {
                drawRect(
                    color = barColor,
                    topLeft = Offset(startX + barWidth + gap, chartHeight - currHeightPx),
                    size = Size(barWidth, currHeightPx)
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Labels row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = formatChartValue(prevValue, unit),
                fontSize = 9.sp,
                color = ColorOnSurface,
                maxLines = 1
            )
            Text(
                text = formatChartValue(currentValue, unit),
                fontSize = 9.sp,
                color = ColorPrimary,
                maxLines = 1
            )
        }
    }
}

private fun formatChartValue(value: Float, unit: String): String {
    return if (unit.isEmpty()) {
        value.toInt().toString()
    } else if (value >= 1000f) {
        "${"%.0f".format(value / 1000f)}k$unit"
    } else {
        "${"%.0f".format(value)}$unit"
    }
}
