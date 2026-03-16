package com.workouttracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.workouttracker.data.db.entities.ScheduleSettings
import com.workouttracker.data.db.entities.WeekPattern
import com.workouttracker.ui.viewmodel.ScheduleViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleSettingsScreen(
    navController: NavController,
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val patterns by viewModel.patterns.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()

    val dayNames = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    var daysMask by remember { mutableStateOf(settings?.trainingDaysMask ?: 0b0010101) } // Mon, Wed, Fri
    var startWeekType by remember { mutableStateOf(settings?.startWeekType ?: 1) }
    var startDate by remember { mutableStateOf(settings?.startDate ?: System.currentTimeMillis()) }
    var cycleLengthWeeks by remember { mutableStateOf(settings?.cycleLengthWeeks ?: 2) }

    // Week patterns: weekType -> dayOfWeek -> programType
    val patternMap = remember(patterns) {
        val map = mutableMapOf<Pair<Int, Int>, String>()
        patterns.forEach { map[Pair(it.weekType, it.dayOfWeek)] = it.programType }
        map
    }
    var editablePatternMap by remember(patternMap) {
        mutableStateOf(patternMap.toMutableMap())
    }

    LaunchedEffect(settings) {
        settings?.let {
            daysMask = it.trainingDaysMask
            startWeekType = it.startWeekType
            startDate = it.startDate
            cycleLengthWeeks = it.cycleLengthWeeks
        }
    }

    val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки расписания") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Назад")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.generateSchedule() },
                        enabled = !isGenerating
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.Refresh, "Сгенерировать расписание")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Training days
            Text("Тренировочные дни:", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dayNames.forEachIndexed { idx, dayName ->
                    val bit = 1 shl idx
                    FilterChip(
                        selected = daysMask and bit != 0,
                        onClick = { daysMask = daysMask xor bit },
                        label = { Text(dayName) }
                    )
                }
            }

            // Cycle length
            Text("Длина цикла: $cycleLengthWeeks нед.", style = MaterialTheme.typography.titleSmall)
            Slider(
                value = cycleLengthWeeks.toFloat(),
                onValueChange = { cycleLengthWeeks = it.toInt() },
                valueRange = 1f..4f,
                steps = 2
            )

            // Start week type
            Text("Начальная неделя:", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 2).forEach { wt ->
                    FilterChip(
                        selected = startWeekType == wt,
                        onClick = { startWeekType = wt },
                        label = { Text("Неделя $wt") }
                    )
                }
            }

            // Start date (simple display)
            Text(
                "Дата начала: ${sdf.format(Date(startDate))}",
                style = MaterialTheme.typography.titleSmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    startDate -= 7 * 24 * 60 * 60 * 1000L
                }) { Text("–7 дн") }
                OutlinedButton(onClick = {
                    startDate += 7 * 24 * 60 * 60 * 1000L
                }) { Text("+7 дн") }
                OutlinedButton(onClick = {
                    startDate = System.currentTimeMillis()
                }) { Text("Сегодня") }
            }

            HorizontalDivider()

            // Week patterns
            Text("Программа по неделям:", style = MaterialTheme.typography.titleSmall)

            for (weekType in 1..cycleLengthWeeks) {
                Text("Неделя $weekType:", style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    dayNames.forEachIndexed { idx, dayName ->
                        val dayOfWeek = idx + 1
                        val bit = 1 shl idx
                        if (daysMask and bit != 0) {
                            val key = Pair(weekType, dayOfWeek)
                            val currentType = editablePatternMap[key] ?: "A"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(dayName, modifier = Modifier.width(40.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("A", "B").forEach { pt ->
                                        FilterChip(
                                            selected = currentType == pt,
                                            onClick = {
                                                editablePatternMap = editablePatternMap.toMutableMap()
                                                    .also { it[key] = pt }
                                            },
                                            label = { Text(pt) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val newSettings = ScheduleSettings(
                        id = settings?.id ?: 0L,
                        trainingDaysMask = daysMask,
                        startDate = startDate,
                        cycleLengthWeeks = cycleLengthWeeks,
                        startWeekType = startWeekType
                    )
                    viewModel.saveSettings(newSettings)

                    val newPatterns = editablePatternMap.map { (key, programType) ->
                        WeekPattern(
                            weekType = key.first,
                            dayOfWeek = key.second,
                            programType = programType
                        )
                    }
                    viewModel.savePatterns(newPatterns)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сохранить настройки")
            }

            OutlinedButton(
                onClick = { viewModel.generateSchedule() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isGenerating
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text("Сгенерировать расписание")
            }
        }
    }
}
