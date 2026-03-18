package com.workouttracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.workouttracker.data.db.entities.SessionStatus
import com.workouttracker.data.db.entities.WorkoutSession
import com.workouttracker.data.repository.ScheduleRepository
import com.workouttracker.ui.navigation.Screen
import com.workouttracker.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class WorkoutTabState(
    val todaySession: WorkoutSession? = null,
    val activeSession: WorkoutSession? = null,
    val upcomingSessions: List<WorkoutSession> = emptyList()
)

@HiltViewModel
class WorkoutTabViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {

    private val _state = MutableStateFlow(WorkoutTabState())
    val state: StateFlow<WorkoutTabState> = _state.asStateFlow()

    fun createQuickSession(programType: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val sessionId = scheduleRepository.createQuickSession(programType)
            onCreated(sessionId)
        }
    }

    init {
        viewModelScope.launch {
            scheduleRepository.getAllSessions().collect { sessions ->
                val now = System.currentTimeMillis()
                val todayStart = startOfDay(now)
                val todayEnd = todayStart + 24 * 60 * 60 * 1000 - 1

                val today = sessions.firstOrNull {
                    it.date in todayStart..todayEnd && it.status != SessionStatus.SKIPPED
                }
                val active = sessions.firstOrNull { it.status == SessionStatus.IN_PROGRESS }
                val upcoming = sessions
                    .filter { it.status == SessionStatus.PLANNED && it.date > todayEnd }
                    .sortedBy { it.date }
                    .take(5)

                _state.value = WorkoutTabState(
                    todaySession = today,
                    activeSession = active,
                    upcomingSessions = upcoming
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
}

@Composable
fun WorkoutTabScreen(
    navController: NavController,
    viewModel: WorkoutTabViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val sdf = remember { SimpleDateFormat("EEE, d MMM", Locale("ru")) }
    var showProgramDialog by remember { mutableStateOf(false) }

    // Dialog to pick program A or B
    if (showProgramDialog) {
        AlertDialog(
            onDismissRequest = { showProgramDialog = false },
            containerColor = ColorSurface,
            title = {
                Text("Начать тренировку", color = ColorOnBackground, fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Выбери программу на сегодня:", color = ColorOnSurface)
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            showProgramDialog = false
                            viewModel.createQuickSession("A") { sessionId ->
                                navController.navigate(Screen.ActiveWorkout.createRoute(sessionId))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary)
                    ) { Text("Программа A") }
                    Button(
                        onClick = {
                            showProgramDialog = false
                            viewModel.createQuickSession("B") { sessionId ->
                                navController.navigate(Screen.ActiveWorkout.createRoute(sessionId))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary)
                    ) { Text("Программа B") }
                }
            },
            dismissButton = {
                TextButton(onClick = { showProgramDialog = false }) {
                    Text("Отмена", color = ColorOnSurface)
                }
            }
        )
    }

    Scaffold(
        containerColor = ColorBackground,
        floatingActionButton = {
            if (state.activeSession == null && state.todaySession?.status != SessionStatus.IN_PROGRESS) {
                FloatingActionButton(
                    onClick = {
                        val today = state.todaySession
                        if (today != null && today.status == SessionStatus.PLANNED) {
                            navController.navigate(Screen.ActiveWorkout.createRoute(today.id))
                        } else {
                            showProgramDialog = true
                        }
                    },
                    containerColor = ColorPrimary,
                    contentColor = ColorOnBackground
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Начать тренировку")
                }
            }
        }
    ) { scaffoldPadding ->
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(scaffoldPadding)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Тренировка",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = ColorOnBackground
                )
                IconButton(onClick = { navController.navigate(Screen.History.route) }) {
                    Icon(Icons.Default.History, "История", tint = ColorOnSurface)
                }
            }
        }

        // Active session
        state.activeSession?.let { session ->
            item {
                DarkCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "▶ Активная тренировка",
                            style = MaterialTheme.typography.labelMedium,
                            color = ColorSecondary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Тип ${session.programType} • ${sdf.format(Date(session.date))}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = ColorOnBackground
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { navController.navigate(Screen.ActiveWorkout.createRoute(session.id)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = ColorSecondary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Продолжить тренировку", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Today's session
        if (state.activeSession == null) {
            state.todaySession?.let { session ->
                item {
                    DarkCard(
                        modifier = Modifier.clickable {
                            navController.navigate(Screen.WorkoutDetail.createRoute(session.id))
                        }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Сегодня",
                                style = MaterialTheme.typography.labelMedium,
                                color = ColorPrimary
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Тренировка ${session.programType}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ColorOnBackground
                                    )
                                    Text(
                                        sdf.format(Date(session.date)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ColorOnSurface
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    when (session.status) {
                                        SessionStatus.DONE -> Badge(containerColor = ColorSecondary) {
                                            Text("Завершена", modifier = Modifier.padding(horizontal = 8.dp))
                                        }
                                        else -> {}
                                    }
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = ColorOnSurface
                                    )
                                }
                            }
                            if (session.status == SessionStatus.PLANNED) {
                                Spacer(Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { navController.navigate(Screen.WorkoutDetail.createRoute(session.id)) },
                                        modifier = Modifier.weight(1f),
                                        border = BorderStroke(1.dp, ColorPrimary),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorPrimary)
                                    ) {
                                        Text("Смотреть план")
                                    }
                                    Button(
                                        onClick = { navController.navigate(Screen.ActiveWorkout.createRoute(session.id)) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary)
                                    ) {
                                        Text("Начать")
                                    }
                                }
                            }
                        }
                    }
                }
            } ?: run {
                // No today's session — show nearest upcoming session
                val nearest = state.upcomingSessions.firstOrNull()
                item {
                    if (nearest != null) {
                        DarkCard(
                            modifier = Modifier.clickable {
                                navController.navigate(Screen.WorkoutDetail.createRoute(nearest.id))
                            }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Сегодня тренировки нет 🧘",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ColorOnSurface
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Следующая: Тренировка ${nearest.programType}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = ColorOnBackground
                                        )
                                        Text(
                                            sdf.format(Date(nearest.date)),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ColorOnSurface
                                        )
                                    }
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = "Подробнее",
                                        tint = ColorOnSurface
                                    )
                                }
                            }
                        }
                    } else {
                        DarkCard {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Сегодня тренировки нет 🧘",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ColorOnSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Upcoming sessions
        if (state.upcomingSessions.isNotEmpty()) {
            item {
                Text(
                    "Ближайшие тренировки",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorOnBackground
                )
            }

            items(state.upcomingSessions, key = { it.id }) { session ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate(Screen.WorkoutDetail.createRoute(session.id)) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ColorSurface),
                    border = BorderStroke(1.dp, ColorSurfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp, 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            sdf.format(Date(session.date)),
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorOnBackground
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Тип ${session.programType}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorOnSurface
                            )
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = ColorOnSurface,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    } // end LazyColumn
    } // end Scaffold
}
