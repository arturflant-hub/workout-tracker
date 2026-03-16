package com.workouttracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.workouttracker.data.db.entities.SessionStatus
import com.workouttracker.data.db.entities.WorkoutSession
import com.workouttracker.ui.navigation.Screen
import com.workouttracker.ui.viewmodel.ScheduleViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    navController: NavController,
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    var weekOffset by remember { mutableStateOf(0) }

    val cal = Calendar.getInstance()
    cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    cal.add(Calendar.WEEK_OF_YEAR, weekOffset)
    val weekStart = startOfDay(cal.timeInMillis)
    val weekEnd = weekStart + 7 * 24 * 60 * 60 * 1000L - 1

    val sessions by viewModel.getSessionsInRange(weekStart, weekEnd).collectAsState(emptyList())

    val dayNames = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    val sdf = SimpleDateFormat("dd MMM", Locale("ru"))
    val weekLabel = SimpleDateFormat("d MMM", Locale("ru"))
    val weekText = "${weekLabel.format(Date(weekStart))} – ${weekLabel.format(Date(weekEnd))}"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Календарь: $weekText") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { weekOffset-- }) {
                        Icon(Icons.Default.ChevronLeft, "Пред. неделя")
                    }
                    IconButton(onClick = { weekOffset = 0 }) {
                        Icon(Icons.Default.Today, "Сегодня")
                    }
                    IconButton(onClick = { weekOffset++ }) {
                        Icon(Icons.Default.ChevronRight, "След. неделя")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Week day headers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dayNames.forEachIndexed { idx, dayName ->
                    val dayStart = weekStart + idx * 24 * 60 * 60 * 1000L
                    val session = sessions.find {
                        it.date >= dayStart && it.date < dayStart + 24 * 60 * 60 * 1000L
                    }
                    val today = startOfDay(System.currentTimeMillis())
                    val isToday = dayStart == today

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            dayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                        )
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        session?.status == SessionStatus.DONE ->
                                            MaterialTheme.colorScheme.primary
                                        session?.status == SessionStatus.SKIPPED ->
                                            MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                                        session != null ->
                                            MaterialTheme.colorScheme.primaryContainer
                                        isToday ->
                                            MaterialTheme.colorScheme.secondaryContainer
                                        else -> MaterialTheme.colorScheme.surface
                                    }
                                )
                                .clickable(enabled = session != null) {
                                    session?.let {
                                        navController.navigate(
                                            Screen.WorkoutSession.createRoute(it.id)
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (session != null) {
                                Text(
                                    session.programType,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = when (session.status) {
                                        SessionStatus.DONE -> MaterialTheme.colorScheme.onPrimary
                                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                                    }
                                )
                            } else {
                                Text(
                                    SimpleDateFormat("d", Locale.getDefault()).format(Date(dayStart)),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            // Sessions list
            if (sessions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Нет тренировок на этой неделе", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sessions, key = { it.id }) { session ->
                        SessionCard(
                            session = session,
                            onClick = {
                                navController.navigate(Screen.WorkoutSession.createRoute(session.id))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SessionCard(session: WorkoutSession, onClick: () -> Unit) {
    val sdf = SimpleDateFormat("EEE, d MMM", Locale("ru"))
    val statusColor = when (session.status) {
        SessionStatus.DONE -> MaterialTheme.colorScheme.primary
        SessionStatus.SKIPPED -> MaterialTheme.colorScheme.error
        SessionStatus.PLANNED -> MaterialTheme.colorScheme.secondary
        SessionStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiary
    }
    val statusText = when (session.status) {
        SessionStatus.DONE -> "✅ Выполнено"
        SessionStatus.SKIPPED -> "⏭ Пропущено"
        SessionStatus.PLANNED -> "📅 Запланировано"
        SessionStatus.IN_PROGRESS -> "🏋 В процессе"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    sdf.format(Date(session.date)),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Программа ${session.programType}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                statusText,
                style = MaterialTheme.typography.labelMedium,
                color = statusColor
            )
        }
    }
}

private fun startOfDay(millis: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = millis
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}
