package com.workouttracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.workouttracker.ui.navigation.Screen
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
    val todayStart = remember {
        val c = java.util.Calendar.getInstance()
        c.set(java.util.Calendar.HOUR_OF_DAY, 0); c.set(java.util.Calendar.MINUTE, 0)
        c.set(java.util.Calendar.SECOND, 0); c.set(java.util.Calendar.MILLISECOND, 0)
        c.timeInMillis
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            val greeting = remember(state.userName) {
                val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                val timeGreeting = when (hour) {
                    in 6..11 -> "Доброе утро"
                    in 12..17 -> "Добрый день"
                    in 18..21 -> "Добрый вечер"
                    else -> "Доброй ночи"
                }
                if (state.userName.isNotBlank()) "$timeGreeting, ${state.userName}!" else timeGreeting
            }
            Text(
                greeting,
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
                        "Следующая",
                        style = MaterialTheme.typography.labelMedium,
                        color = ColorPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    if (state.nextSession != null) {
                        val session = state.nextSession!!
                        val isToday = session.date >= todayStart && session.date < todayStart + 86_400_000L
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    if (isToday) "Сегодня" else sdf.format(Date(session.date))
                                        .replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isToday) ColorSecondary else ColorOnBackground
                                )
                                if (!isToday) {
                                    Text(
                                        sdf.format(Date(session.date)).replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ColorOnSurface
                                    )
                                }
                            }
                            Surface(
                                color = ColorPrimary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "Тип ${session.programType}",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ColorPrimary
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
                        OutlinedButton(
                            onClick = {
                                state.nextSession?.id?.let { id ->
                                    navController.navigate(Screen.WorkoutDetail.createRoute(id))
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, ColorPrimary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Смотреть план", fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(18.dp))
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
            DarkCard(
                modifier = Modifier.clickable(enabled = state.lastSession != null) {
                    state.lastSession?.id?.let { id ->
                        navController.navigate(Screen.WorkoutDetail.createRoute(id))
                    }
                }
            ) {
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
                                    if (state.lastDaySessionCount > 1)
                                        "${state.lastDaySessionCount} тренировки за день"
                                    else
                                        "Тип ${session.programType}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ColorOnSurface
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "%.0f кг".format(state.lastSessionTonnage),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ColorSecondary
                                )
                                Text(
                                    "суммарный тоннаж",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ColorOnSurface
                                )
                                Spacer(Modifier.height(6.dp))
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = ColorOnSurface,
                                    modifier = Modifier.size(20.dp)
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
                            "$sign%.1f кг от старта".format(it)
                        },
                        subColor = when {
                            state.weightChange == null -> ColorOnSurface
                            state.weightChange!! > 0 -> ColorError
                            else -> ColorSecondary
                        },
                        trendUp = state.weightChange?.let { it > 0 },
                        trendColor = when {
                            state.weightChange == null -> ColorOnSurface
                            state.weightChange!! > 0 -> ColorError
                            else -> ColorSecondary
                        }
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        label = "% жира (Navy)",
                        value = state.bodyFatPercent?.let { "%.1f%%".format(it) } ?: "—",
                        trendUp = state.bodyFatChange?.let { it > 0 },
                        trendColor = when {
                            state.bodyFatChange == null -> ColorOnSurface
                            state.bodyFatChange!! > 0 -> ColorError
                            else -> ColorSecondary
                        }
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
                        },
                        trendUp = state.waistChange?.let { it > 0 },
                        trendColor = when {
                            state.waistChange == null -> ColorOnSurface
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
                MetricCard(
                    modifier = Modifier.fillMaxWidth(),
                    label = "Средний RIR",
                    value = state.avgRir?.let { "%.1f".format(it) } ?: "—",
                    subValue = state.avgRir?.let {
                        when {
                            it <= 1f -> "Слишком тяжело — снизьте вес"
                            it <= 2f -> "Рабочая зона — всё правильно"
                            else -> "Слишком легко — можно добавить вес"
                        }
                    },
                    subColor = state.avgRir?.let {
                        when {
                            it <= 1f -> ColorError
                            it <= 2f -> ColorSecondary
                            else -> ColorOnSurface
                        }
                    } ?: ColorOnSurface,
                )
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
    valueColor: Color = ColorOnBackground,
    subColor: Color = ColorOnSurface,
    trendUp: Boolean? = null,
    trendColor: Color = ColorOnSurface
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = valueColor,
                    fontSize = 22.sp,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (trendUp != null) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = if (trendUp) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = trendColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
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
