package com.workouttracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.workouttracker.ui.theme.*
import com.workouttracker.ui.viewmodel.DashboardState
import com.workouttracker.ui.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val sdf = remember { SimpleDateFormat("EEE, d MMM", Locale("ru")) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Дашборд",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = ColorOnBackground
            )
        }

        // Next workout card
        item {
            DarkCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Следующая тренировка",
                        style = MaterialTheme.typography.labelMedium,
                        color = ColorOnSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    if (state.nextSession != null) {
                        val session = state.nextSession!!
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                sdf.format(Date(session.date)),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = ColorOnBackground
                            )
                            Badge(containerColor = ColorPrimary) {
                                Text(
                                    "Тип ${session.programType}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = ColorOnBackground
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        state.nextSessionExercises.take(4).forEach { ex ->
                            Text(
                                "• ${ex.name}  ${ex.plannedSets}×${ex.plannedMinReps}-${ex.plannedMaxReps} @ ${ex.plannedWeight}кг",
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorOnSurface
                            )
                        }
                        if (state.nextSessionExercises.size > 4) {
                            Text(
                                "+ ${state.nextSessionExercises.size - 4} упр.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorPrimary
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                state.nextSession?.id?.let { id ->
                                    navController.navigate("planned_workout/$id")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary)
                        ) {
                            Text("Посмотреть план")
                        }
                    } else {
                        Text(
                            "Нет запланированных тренировок",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ColorOnSurface
                        )
                    }
                }
            }
        }

        // Last workout card
        item {
            DarkCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Последняя тренировка",
                        style = MaterialTheme.typography.labelMedium,
                        color = ColorOnSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    if (state.lastSession != null) {
                        val session = state.lastSession!!
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    sdf.format(Date(session.date)),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ColorOnBackground
                                )
                                Text(
                                    "Тип ${session.programType}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ColorOnSurface
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "✅ Завершена",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ColorSecondary
                                )
                                Text(
                                    "%.0f кг тоннаж".format(state.lastSessionTonnage),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ColorOnSurface
                                )
                            }
                        }
                    } else {
                        Text(
                            "Нет завершённых тренировок",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ColorOnSurface
                        )
                    }
                }
            }
        }

        // Metrics grid
        item {
            Text(
                "Метрики",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = ColorOnBackground
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        label = "Вес тела",
                        value = state.currentWeight?.let { "%.1f кг".format(it) } ?: "—",
                        subValue = state.weightChange?.let {
                            val sign = if (it >= 0) "+" else ""
                            "$sign%.1f кг".format(it)
                        },
                        subColor = when {
                            state.weightChange == null -> ColorOnSurface
                            state.weightChange!! > 0 -> ColorError
                            else -> ColorSecondary
                        }
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        label = "% жира (Navy)",
                        value = state.bodyFatPercent?.let { "%.1f%%".format(it) } ?: "—"
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        label = "Δ талия",
                        value = state.waistChange?.let {
                            val sign = if (it >= 0) "+" else ""
                            "$sign%.1f см".format(it)
                        } ?: "—",
                        valueColor = when {
                            state.waistChange == null -> ColorOnBackground
                            state.waistChange!! < 0 -> ColorSecondary
                            else -> ColorError
                        }
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        label = "Тренировок",
                        value = "${state.workoutCount}"
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        label = "Общий тоннаж",
                        value = "%.0f кг".format(state.totalTonnage)
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        label = "Средний объём",
                        value = state.avgVolume?.let { "%.0f кг".format(it) } ?: "—"
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        label = "Средний RIR",
                        value = state.avgRir?.let { "%.1f".format(it) } ?: "—"
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun DarkCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ColorSurface),
        border = BorderStroke(1.dp, ColorSurfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}

@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    subValue: String? = null,
    valueColor: androidx.compose.ui.graphics.Color = ColorOnBackground,
    subColor: androidx.compose.ui.graphics.Color = ColorOnSurface
) {
    DarkCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = ColorOnSurface
            )
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                fontSize = 22.sp
            )
            if (subValue != null) {
                Text(
                    subValue,
                    style = MaterialTheme.typography.bodySmall,
                    color = subColor
                )
            }
        }
    }
}
