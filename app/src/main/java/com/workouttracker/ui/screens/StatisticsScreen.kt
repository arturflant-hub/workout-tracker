package com.workouttracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.workouttracker.data.db.entities.SessionStatus
import com.workouttracker.ui.navigation.Screen
import com.workouttracker.ui.theme.*
import com.workouttracker.ui.viewmodel.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Colors for program types in bar charts
private val ColorTypeA = Color(0xFF6C63FF)
private val ColorTypeB = Color(0xFF30D158)

// ──────────────────────────────────────────────
//  Root screen with tab navigation
// ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    navController: NavController,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val tabs = listOf("Графики", "Календарь", "История")
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { tabs.size }
    )
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground)
    ) {
        // Sticky header: title + tab bar
        Surface(color = ColorSurface, shadowElevation = 0.dp) {
            Column {
                Text(
                    "Статистика",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = ColorOnBackground,
                    modifier = Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 8.dp)
                )
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = ColorSurface,
                    contentColor = ColorPrimary,
                    divider = { HorizontalDivider(color = ColorSurfaceVariant) }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                coroutineScope.launch { pagerState.animateScrollToPage(index) }
                            },
                            text = {
                                Text(
                                    title,
                                    fontWeight = if (pagerState.currentPage == index) FontWeight.SemiBold
                                                 else FontWeight.Normal,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            selectedContentColor = ColorPrimary,
                            unselectedContentColor = ColorOnSurface
                        )
                    }
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> ChartsTabContent(state = state, viewModel = viewModel)
                1 -> CalendarTabContent(navController = navController)
                2 -> HistoryTabContent(navController = navController)
            }
        }
    }
}

// ──────────────────────────────────────────────
//  Tab 0 — Charts
// ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChartsTabContent(
    state: StatisticsUiState,
    viewModel: StatisticsViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(4.dp))

        StatCard(title = "Тоннаж по тренировкам") {
            if (state.tonnagePoints.isEmpty()) {
                EmptyChartMessage()
            } else {
                TypedBarChart(
                    points = state.tonnagePoints.map { Triple(it.date, it.tonnage, it.programType) },
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    yAxisLabel = "кг",
                    tooltipLabel = "Тоннаж"
                )
                ChartDateLabels(
                    dates = state.tonnagePoints.map { it.date },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Canvas(Modifier.size(10.dp)) { drawCircle(ColorTypeA) }
                        Text("Тип A", style = MaterialTheme.typography.labelSmall, color = ColorOnSurface)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Canvas(Modifier.size(10.dp)) { drawCircle(ColorTypeB) }
                        Text("Тип B", style = MaterialTheme.typography.labelSmall, color = ColorOnSurface)
                    }
                }
            }
        }

        StatCard(title = "Объём по неделям") {
            if (state.weeklyVolumePoints.isEmpty()) {
                EmptyChartMessage()
            } else {
                TypedBarChart(
                    points = state.weeklyVolumePoints.map { Triple(it.weekStart, it.tonnage, it.programType) },
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    yAxisLabel = "кг",
                    tooltipLabel = "Объём"
                )
                ChartDateLabels(
                    dates = state.weeklyVolumePoints.map { it.weekStart },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        StatCard(title = "Прогресс по упражнению") {
            if (state.exerciseNames.isEmpty()) {
                EmptyChartMessage()
            } else {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = state.selectedExercise ?: "Выберите упражнение",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ColorPrimary,
                            unfocusedBorderColor = ColorSurfaceVariant,
                            focusedTextColor = ColorOnBackground,
                            unfocusedTextColor = ColorOnBackground
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        containerColor = ColorSurface
                    ) {
                        state.exerciseNames.forEach { name ->
                            DropdownMenuItem(
                                text = { Text(name, color = ColorOnBackground) },
                                onClick = {
                                    viewModel.selectExercise(name)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (state.exerciseProgressPoints.isEmpty()) {
                    EmptyChartMessage()
                } else {
                    LineChart(
                        points = state.exerciseProgressPoints.map { it.date.toFloat() to it.e1rm },
                        lineColor = ColorSecondary,
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        yAxisLabel = "кг",
                        tooltipLabel = "e1RM",
                        rawValues = state.exerciseProgressPoints.map { it.e1rm },
                        dates = state.exerciseProgressPoints.map { it.date }
                    )
                    ChartDateLabels(
                        dates = state.exerciseProgressPoints.map { it.date },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "e1RM (Epley): вес × (1 + повт/30)",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorOnSurface
                    )
                }
            }
        }

        StatCard(title = "Динамика веса тела") {
            if (state.bodyWeightPoints.isEmpty()) {
                EmptyChartMessage()
            } else {
                LineChart(
                    points = state.bodyWeightPoints.map { it.date.toFloat() to it.weight },
                    lineColor = ColorPrimary,
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    yAxisLabel = "кг",
                    tooltipLabel = "Вес",
                    rawValues = state.bodyWeightPoints.map { it.weight },
                    dates = state.bodyWeightPoints.map { it.date }
                )
                ChartDateLabels(
                    dates = state.bodyWeightPoints.map { it.date },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        StatCard(title = "Динамика % жира (Navy)") {
            if (state.bodyFatPoints.isEmpty()) {
                EmptyChartMessage("Добавьте замеры талии и шеи в разделе Тело")
            } else {
                LineChart(
                    points = state.bodyFatPoints.map { it.date.toFloat() to it.bodyFat },
                    lineColor = ColorError,
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    yAxisLabel = "%",
                    tooltipLabel = "Жир",
                    rawValues = state.bodyFatPoints.map { it.bodyFat },
                    dates = state.bodyFatPoints.map { it.date }
                )
                ChartDateLabels(
                    dates = state.bodyFatPoints.map { it.date },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

// ──────────────────────────────────────────────
//  Tab 1 — Calendar (month view)
// ──────────────────────────────────────────────

@Composable
private fun CalendarTabContent(
    navController: NavController,
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    var monthOffset by remember { mutableStateOf(0) }
    val today = remember { calStartOfDay(System.currentTimeMillis()) }

    // Compute month range
    val (monthStart, monthEnd, monthLabel, dayCells) = remember(monthOffset) {
        val c = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, monthOffset)
        }
        val start = c.timeInMillis
        val label = SimpleDateFormat("LLLL yyyy", Locale("ru"))
            .format(Date(start))
            .replaceFirstChar { it.uppercase() }

        // Build grid cells (Mon-first, null = padding)
        val daysInMonth = c.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDow = c.get(Calendar.DAY_OF_WEEK)
        val offset = if (firstDow == Calendar.SUNDAY) 6 else firstDow - 2
        val cells = buildList<Long?> {
            repeat(offset) { add(null) }
            repeat(daysInMonth) { day ->
                c.set(Calendar.DAY_OF_MONTH, day + 1)
                add(c.timeInMillis)
            }
            while (size % 7 != 0) add(null)
        }

        c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH))
        c.set(Calendar.HOUR_OF_DAY, 23); c.set(Calendar.MINUTE, 59); c.set(Calendar.SECOND, 59)
        val end = c.timeInMillis

        CalendarMonthData(start, end, label, cells)
    }

    val sessions by viewModel.getSessionsInRange(monthStart, monthEnd).collectAsState(emptyList())
    val doneSessions = sessions.filter { it.status == SessionStatus.DONE }
    val dayNames = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

    Column(modifier = Modifier.fillMaxSize()) {
        // Month navigation header
        Surface(color = ColorSurface) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { monthOffset-- }) {
                    Icon(Icons.Default.ChevronLeft, "Предыдущий месяц", tint = ColorOnSurface)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        monthLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorOnBackground
                    )
                    if (monthOffset != 0) {
                        Text(
                            "Нажмите для возврата к текущему",
                            style = MaterialTheme.typography.labelSmall,
                            color = ColorOnSurface,
                            modifier = Modifier.clickable { monthOffset = 0 }
                        )
                    }
                }
                IconButton(onClick = { monthOffset++ }) {
                    Icon(Icons.Default.ChevronRight, "Следующий месяц", tint = ColorOnSurface)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Day headers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                dayNames.forEach { name ->
                    Text(
                        name,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorOnSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Calendar grid
            val weeks = dayCells.chunked(7)
            weeks.forEach { week ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                ) {
                    week.forEach { dayTimestamp ->
                        val session = dayTimestamp?.let { ts ->
                            sessions.find { calStartOfDay(it.date) == ts }
                        }
                        val isToday = dayTimestamp == today
                        CalendarDayCell(
                            timestamp = dayTimestamp,
                            session = session,
                            isToday = isToday,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                session?.let {
                                    navController.navigate(Screen.WorkoutDetail.createRoute(it.id))
                                }
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(
                color = ColorSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Session list for the month
            if (sessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📅", fontSize = 36.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Тренировок в этом месяце нет",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ColorOnSurface
                        )
                    }
                }
            } else {
                // Stats summary
                val doneCnt = sessions.count { it.status == SessionStatus.DONE }
                val plannedCnt = sessions.count { it.status == SessionStatus.PLANNED }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (doneCnt > 0) {
                        Surface(
                            color = ColorSecondary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "$doneCnt",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorSecondary
                                )
                                Text(
                                    "выполнено",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ColorOnSurface
                                )
                            }
                        }
                    }
                    if (plannedCnt > 0) {
                        Surface(
                            color = ColorPrimary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "$plannedCnt",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorPrimary
                                )
                                Text(
                                    "запланировано",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ColorOnSurface
                                )
                            }
                        }
                    }
                }

                // Sessions list
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sessions.sortedBy { it.date }.forEach { session ->
                        CalendarSessionRow(
                            session = session,
                            onClick = {
                                navController.navigate(Screen.WorkoutDetail.createRoute(session.id))
                            }
                        )
                    }
                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }
}

private data class CalendarMonthData(
    val monthStart: Long,
    val monthEnd: Long,
    val label: String,
    val dayCells: List<Long?>
)

@Composable
private fun CalendarDayCell(
    timestamp: Long?,
    session: com.workouttracker.data.db.entities.WorkoutSession?,
    isToday: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor = when {
        session?.status == SessionStatus.DONE -> ColorSecondary
        session?.status == SessionStatus.IN_PROGRESS -> Color(0xFFFF9F0A)
        session != null -> ColorPrimary
        isToday -> ColorSurfaceVariant
        else -> Color.Transparent
    }
    val textColor = when {
        session != null -> Color.White
        isToday -> ColorPrimary
        else -> ColorOnBackground
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(3.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(enabled = timestamp != null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (timestamp != null) {
            val dayNum = SimpleDateFormat("d", Locale.getDefault()).format(Date(timestamp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    dayNum,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isToday || session != null) FontWeight.Bold else FontWeight.Normal,
                    color = textColor
                )
                if (session != null) {
                    Text(
                        session.programType,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = textColor.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarSessionRow(
    session: com.workouttracker.data.db.entities.WorkoutSession,
    onClick: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("EEE, d MMM", Locale("ru")) }
    val (statusColor, statusLabel) = when (session.status) {
        SessionStatus.DONE -> ColorSecondary to "✓ Выполнена"
        SessionStatus.SKIPPED -> ColorError to "Пропущена"
        SessionStatus.PLANNED -> ColorPrimary to "Запланирована"
        SessionStatus.IN_PROGRESS -> Color(0xFFFF9F0A) to "В процессе"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ColorSurface),
        border = BorderStroke(1.dp, ColorSurfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    sdf.format(Date(session.date)).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorOnBackground
                )
                Text(
                    "Тренировка ${session.programType}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorOnSurface
                )
            }
            Surface(
                color = statusColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    statusLabel,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun calStartOfDay(millis: Long): Long {
    val c = Calendar.getInstance()
    c.timeInMillis = millis
    c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

// ──────────────────────────────────────────────
//  Tab 2 — History
// ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryTabContent(
    navController: NavController,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val programs by viewModel.programs.collectAsState()
    val selectedProgramId by viewModel.selectedProgramId.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val selectedExercise by viewModel.selectedExercise.collectAsState()
    val history by viewModel.history.collectAsState()
    val completedSessions by viewModel.completedSessions.collectAsState()

    var historyMode by remember { mutableStateOf(0) } // 0 = Тренировки, 1 = Упражнения

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Mode toggle ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("По тренировкам", "По упражнениям").forEachIndexed { idx, label ->
                FilterChip(
                    selected = historyMode == idx,
                    onClick = { historyMode = idx },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ColorPrimary.copy(alpha = 0.18f),
                        selectedLabelColor = ColorPrimary,
                        containerColor = ColorSurfaceVariant,
                        labelColor = ColorOnSurface
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = historyMode == idx,
                        selectedBorderColor = ColorPrimary.copy(alpha = 0.4f),
                        borderColor = ColorSurfaceVariant
                    )
                )
            }
        }
        HorizontalDivider(color = ColorSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))

        if (historyMode == 0) {
            // ── Sessions list ──
            if (completedSessions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text("🏋️", fontSize = 48.sp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Нет завершённых тренировок",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ColorOnSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(completedSessions, key = { it.id }) { session ->
                        CalendarSessionRow(
                            session = session,
                            onClick = { navController.navigate(Screen.WorkoutDetail.createRoute(session.id)) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        } else {

        // ── Program filter chips ──
        if (programs.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                programs.forEach { program ->
                    FilterChip(
                        selected = program.id == selectedProgramId,
                        onClick = { viewModel.selectProgram(program.id) },
                        label = { Text("${program.type}: ${program.name}") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ColorPrimary.copy(alpha = 0.18f),
                            selectedLabelColor = ColorPrimary,
                            containerColor = ColorSurfaceVariant,
                            labelColor = ColorOnSurface
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = program.id == selectedProgramId,
                            selectedBorderColor = ColorPrimary.copy(alpha = 0.4f),
                            borderColor = ColorSurfaceVariant
                        )
                    )
                }
            }
        }

        // ── Exercise filter chips ──
        if (exercises.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                exercises.forEach { ex ->
                    FilterChip(
                        selected = selectedExercise?.id == ex.id,
                        onClick = { viewModel.loadHistory(ex) },
                        label = { Text(ex.name, maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ColorSecondary.copy(alpha = 0.18f),
                            selectedLabelColor = ColorSecondary,
                            containerColor = ColorSurfaceVariant,
                            labelColor = ColorOnSurface
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedExercise?.id == ex.id,
                            selectedBorderColor = ColorSecondary.copy(alpha = 0.4f),
                            borderColor = ColorSurfaceVariant
                        )
                    )
                }
            }
            HorizontalDivider(
                color = ColorSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // ── Content ──
        if (history == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text("📋", fontSize = 48.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        when {
                            programs.isEmpty() -> "Нет данных — добавьте программу тренировок"
                            selectedProgramId == null -> "Выберите программу выше"
                            exercises.isEmpty() -> "В программе нет упражнений"
                            else -> "Выберите упражнение для просмотра истории"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = ColorOnSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            history?.let { h ->
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Exercise title
                    item {
                        Text(
                            h.programExercise.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ColorOnBackground
                        )
                        Text(
                            "План: ${h.programExercise.sets}×${h.programExercise.minReps}–${h.programExercise.maxReps}" +
                            " @ ${h.programExercise.startWeight} кг",
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorOnSurface
                        )
                    }

                    // Recommendation
                    h.recommendation?.let { rec ->
                        item {
                            val (bgColor, accentColor, label) = when (rec.type) {
                                com.workouttracker.domain.model.RecommendationType.INCREASE_WEIGHT ->
                                    Triple(ColorSecondary.copy(alpha = 0.12f), ColorSecondary, "↑ Увеличить вес")
                                com.workouttracker.domain.model.RecommendationType.DECREASE_WEIGHT ->
                                    Triple(ColorError.copy(alpha = 0.12f), ColorError, "↓ Снизить вес")
                                com.workouttracker.domain.model.RecommendationType.INCREASE_REPS ->
                                    Triple(ColorPrimary.copy(alpha = 0.10f), ColorPrimary, "→ Добавить повторения")
                                com.workouttracker.domain.model.RecommendationType.SLOW_NEGATIVE ->
                                    Triple(ColorSurfaceVariant, ColorOnSurface, "⏱ Медленный негатив")
                                com.workouttracker.domain.model.RecommendationType.ADD_PAUSE ->
                                    Triple(ColorSurfaceVariant, ColorOnSurface, "⏸ Добавить паузу")
                                com.workouttracker.domain.model.RecommendationType.PLATEAU ->
                                    Triple(ColorSurfaceVariant, ColorOnSurface, "↔ Плато")
                            }
                            Surface(
                                color = bgColor,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        "Рекомендация на следующую тренировку",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ColorOnSurface
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        rec.text,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ColorOnBackground
                                    )
                                }
                            }
                        }
                    }

                    if (h.sessions.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Нет завершённых тренировок по этому упражнению",
                                    color = ColorOnSurface,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(h.sessions, key = { it.sessionExercise.id }) { entry ->
                            HistoryEntryCard(entry = entry)
                        }
                    }

                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        } // end if (history == null) else
        } // end else (exercises mode, historyMode != 0)
    }
}

@Composable
private fun HistoryEntryCard(entry: HistorySessionEntry) {
    val sdf = remember { SimpleDateFormat("d MMM yyyy, EEE", Locale("ru")) }
    val ex = entry.sessionExercise
    val sets = entry.sets

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ColorSurface),
        border = BorderStroke(1.dp, ColorSurfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Date header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (entry.sessionDate > 0L)
                        sdf.format(Date(entry.sessionDate)).replaceFirstChar { it.uppercase() }
                    else
                        "Дата неизвестна",
                    style = MaterialTheme.typography.labelMedium,
                    color = ColorPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                // Volume summary
                if (sets.isNotEmpty()) {
                    val maxWeight = sets.maxOf { it.actualWeight }
                    val totalReps = sets.sumOf { it.actualReps }
                    Text(
                        "${formatW(maxWeight)} кг · $totalReps повт",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorOnSurface
                    )
                }
            }

            if (sets.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                // Column headers
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Подход",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorOnSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "Вес",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorOnSurface,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Повт",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorOnSurface,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "RIR",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorOnSurface,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )
                }
                Spacer(Modifier.height(4.dp))
                sets.forEach { set ->
                    val hitTarget = set.actualReps >= ex.plannedMinReps
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "№${set.setIndex}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorOnSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${formatW(set.actualWeight)} кг",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = ColorOnBackground,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "${set.actualReps}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (hitTarget) ColorSecondary else ColorError,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "${set.rir}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorOnSurface,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End
                        )
                    }
                }

                // Comment if present
                if (ex.comment.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    HorizontalDivider(color = ColorSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "💬 ${ex.comment}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorOnSurface
                    )
                }
            } else {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Данные не записаны",
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorOnSurface
                )
            }
        }
    }
}

private fun formatW(w: Float): String =
    if (w == w.toLong().toFloat()) w.toLong().toString() else "%.1f".format(w)

// ──────────────────────────────────────────────
//  Shared composables (charts, cards)
// ──────────────────────────────────────────────

@Composable
fun StatCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ColorSurface),
        border = BorderStroke(1.dp, ColorSurfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = ColorOnBackground
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun EmptyChartMessage(text: String = "Недостаточно данных") {
    Box(
        modifier = Modifier.fillMaxWidth().height(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = ColorOnSurface)
    }
}

@Composable
fun LineChart(
    points: List<Pair<Float, Float>>,
    lineColor: Color,
    modifier: Modifier = Modifier,
    yAxisLabel: String = "",
    tooltipLabel: String = "",
    rawValues: List<Float> = emptyList(),
    dates: List<Long> = emptyList()
) {
    if (points.size < 2) {
        Canvas(modifier = modifier) {
            drawCircle(color = lineColor, radius = 6.dp.toPx(), center = Offset(size.width / 2, size.height / 2))
        }
        return
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val sdf = remember { SimpleDateFormat("d.MM", Locale("ru")) }

    val minX = points.minOf { it.first }
    val maxX = points.maxOf { it.first }
    val minY = points.minOf { it.second }
    val maxY = points.maxOf { it.second }
    val rangeX = (maxX - minX).takeIf { it > 0 } ?: 1f
    val rangeY = (maxY - minY).takeIf { it > 0 } ?: 1f

    Canvas(
        modifier = modifier.pointerInput(points) {
            detectTapGestures { tapOffset ->
                val yAxisWidth = if (yAxisLabel.isNotEmpty()) 40.dp.toPx() else 16.dp.toPx()
                val padding = 16.dp.toPx()
                val drawWidth = size.width - yAxisWidth - padding
                val drawHeight = size.height.toFloat() - padding * 2

                var closest = -1
                var minDist = Float.MAX_VALUE
                points.forEachIndexed { i, (x, y) ->
                    val cx = yAxisWidth + (x - minX) / rangeX * drawWidth
                    val cy = padding + drawHeight - (y - minY) / rangeY * drawHeight
                    val dist = kotlin.math.sqrt((tapOffset.x - cx) * (tapOffset.x - cx) + (tapOffset.y - cy) * (tapOffset.y - cy))
                    if (dist < minDist) { minDist = dist; closest = i }
                }
                selectedIndex = if (minDist < 40.dp.toPx() && closest >= 0) closest else null
            }
        }
    ) {
        val yAxisWidth = if (yAxisLabel.isNotEmpty()) 40.dp.toPx() else 16.dp.toPx()
        val padding = 16.dp.toPx()
        val drawWidth = size.width - yAxisWidth - padding
        val drawHeight = size.height - padding * 2

        fun xFor(x: Float) = yAxisWidth + (x - minX) / rangeX * drawWidth
        fun yFor(y: Float) = padding + drawHeight - (y - minY) / rangeY * drawHeight

        // Y-axis labels
        if (yAxisLabel.isNotEmpty()) {
            val textPaint = android.graphics.Paint().apply {
                color = 0xFF8E8E93.toInt()
                textSize = 10.sp.toPx()
                isAntiAlias = true
            }
            val gridLines = 4
            repeat(gridLines + 1) { i ->
                val y = padding + drawHeight * i / gridLines
                drawLine(
                    color = ColorSurfaceVariant,
                    start = Offset(yAxisWidth, y),
                    end = Offset(yAxisWidth + drawWidth, y),
                    strokeWidth = 1.dp.toPx()
                )
                val value = maxY - (maxY - minY) * i / gridLines
                val label = if (value >= 100) value.roundToInt().toString() else String.format("%.1f", value)
                drawContext.canvas.nativeCanvas.drawText(
                    label, 2.dp.toPx(), y + 4.dp.toPx(), textPaint
                )
            }
            // Unit label at top
            val unitPaint = android.graphics.Paint().apply {
                color = 0xFF8E8E93.toInt()
                textSize = 9.sp.toPx()
                isAntiAlias = true
            }
            drawContext.canvas.nativeCanvas.drawText(
                yAxisLabel, 2.dp.toPx(), padding - 4.dp.toPx(), unitPaint
            )
        } else {
            val gridLines = 4
            repeat(gridLines + 1) { i ->
                val y = padding + drawHeight * i / gridLines
                drawLine(
                    color = ColorSurfaceVariant,
                    start = Offset(yAxisWidth, y),
                    end = Offset(yAxisWidth + drawWidth, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }

        val path = Path()
        points.forEachIndexed { i, (x, y) ->
            val cx = xFor(x); val cy = yFor(y)
            if (i == 0) path.moveTo(cx, cy) else path.lineTo(cx, cy)
        }
        drawPath(path = path, color = lineColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))

        val fillPath = Path()
        fillPath.addPath(path)
        fillPath.lineTo(xFor(points.last().first), padding + drawHeight)
        fillPath.lineTo(xFor(points.first().first), padding + drawHeight)
        fillPath.close()
        drawPath(fillPath, color = lineColor.copy(alpha = 0.1f))

        points.forEachIndexed { i, (x, y) ->
            val isSelected = i == selectedIndex
            drawCircle(
                color = lineColor,
                radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                center = Offset(xFor(x), yFor(y))
            )
            if (isSelected) {
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = Offset(xFor(x), yFor(y))
                )
            }
        }

        // Tooltip
        val si = selectedIndex
        if (si != null && si in points.indices) {
            val (px, py) = points[si]
            val cx = xFor(px)
            val cy = yFor(py)

            val displayValue = rawValues.getOrNull(si) ?: py
            val dateStr = if (dates.isNotEmpty() && si in dates.indices) sdf.format(Date(dates[si])) else ""
            val tooltipText = if (dateStr.isNotEmpty()) "$dateStr\n$tooltipLabel: ${formatChartValue(displayValue)} $yAxisLabel"
                              else "$tooltipLabel: ${formatChartValue(displayValue)} $yAxisLabel"

            val tooltipPaint = android.graphics.Paint().apply {
                color = 0xFFFFFFFF.toInt()
                textSize = 12.sp.toPx()
                isAntiAlias = true
            }
            val lines = tooltipText.split("\n")
            val lineHeight = tooltipPaint.fontMetrics.bottom - tooltipPaint.fontMetrics.top
            val textWidths = lines.map { tooltipPaint.measureText(it) }
            val maxTextWidth = textWidths.max()
            val tooltipPadding = 8.dp.toPx()
            val tooltipWidth = maxTextWidth + tooltipPadding * 2
            val tooltipHeight = lineHeight * lines.size + tooltipPadding * 2

            var tx = cx - tooltipWidth / 2
            if (tx < 0f) tx = 4.dp.toPx()
            if (tx + tooltipWidth > size.width) tx = size.width - tooltipWidth - 4.dp.toPx()
            val ty = cy - tooltipHeight - 12.dp.toPx()
            val finalTy = if (ty < 0f) cy + 12.dp.toPx() else ty

            drawRoundRect(
                color = Color(0xE62C2C2E),
                topLeft = Offset(tx, finalTy),
                size = Size(tooltipWidth, tooltipHeight),
                cornerRadius = CornerRadius(8.dp.toPx())
            )
            lines.forEachIndexed { lineIdx, line ->
                drawContext.canvas.nativeCanvas.drawText(
                    line,
                    tx + tooltipPadding,
                    finalTy + tooltipPadding - tooltipPaint.fontMetrics.top + lineIdx * lineHeight,
                    tooltipPaint
                )
            }
        }
    }
}

@Composable
fun TypedBarChart(
    points: List<Triple<Long, Float, String>>,
    modifier: Modifier = Modifier,
    yAxisLabel: String = "",
    tooltipLabel: String = ""
) {
    if (points.isEmpty()) return

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val sdf = remember { SimpleDateFormat("d.MM", Locale("ru")) }
    val maxVal = points.maxOf { it.second }.takeIf { it > 0f } ?: 1f

    Canvas(
        modifier = modifier.pointerInput(points) {
            detectTapGestures { tapOffset ->
                val yAxisWidth = if (yAxisLabel.isNotEmpty()) 40.dp.toPx() else 8.dp.toPx()
                val padH = 8.dp.toPx()
                val padV = 12.dp.toPx()
                val drawWidth = size.width - yAxisWidth - padH
                val spacing = drawWidth / points.size

                var found = -1
                points.forEachIndexed { idx, _ ->
                    val barX = yAxisWidth + idx * spacing
                    if (tapOffset.x >= barX && tapOffset.x < barX + spacing) {
                        found = idx
                    }
                }
                selectedIndex = if (found >= 0) found else null
            }
        }
    ) {
        val yAxisWidth = if (yAxisLabel.isNotEmpty()) 40.dp.toPx() else 8.dp.toPx()
        val padH = 8.dp.toPx()
        val padV = 12.dp.toPx()
        val drawWidth = size.width - yAxisWidth - padH
        val drawHeight = size.height - padV

        val barWidth = (drawWidth / points.size * 0.6f).coerceAtLeast(4.dp.toPx())
        val spacing = drawWidth / points.size

        // Y-axis labels
        if (yAxisLabel.isNotEmpty()) {
            val textPaint = android.graphics.Paint().apply {
                color = 0xFF8E8E93.toInt()
                textSize = 10.sp.toPx()
                isAntiAlias = true
            }
            repeat(5) { i ->
                val y = padV + drawHeight * i / 4
                drawLine(
                    color = ColorSurfaceVariant,
                    start = Offset(yAxisWidth, y),
                    end = Offset(yAxisWidth + drawWidth, y),
                    strokeWidth = 1.dp.toPx()
                )
                val value = maxVal - maxVal * i / 4
                val label = if (value >= 100) value.roundToInt().toString() else String.format("%.1f", value)
                drawContext.canvas.nativeCanvas.drawText(
                    label, 2.dp.toPx(), y + 4.dp.toPx(), textPaint
                )
            }
            val unitPaint = android.graphics.Paint().apply {
                color = 0xFF8E8E93.toInt()
                textSize = 9.sp.toPx()
                isAntiAlias = true
            }
            drawContext.canvas.nativeCanvas.drawText(
                yAxisLabel, 2.dp.toPx(), padV - 4.dp.toPx(), unitPaint
            )
        } else {
            repeat(4) { i ->
                val y = padV + drawHeight * i / 4
                drawLine(
                    color = ColorSurfaceVariant,
                    start = Offset(yAxisWidth, y),
                    end = Offset(yAxisWidth + drawWidth, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }

        points.forEachIndexed { idx, (_, value, type) ->
            val barColor = when (type) {
                "A" -> ColorTypeA
                "B" -> ColorTypeB
                else -> Color(0xFF3A3A3C)
            }
            val barHeight = (value / maxVal) * drawHeight
            val x = yAxisWidth + idx * spacing + (spacing - barWidth) / 2
            val y = padV + drawHeight - barHeight
            val isSelected = idx == selectedIndex
            drawRect(
                color = if (isSelected) barColor.copy(alpha = 1f) else barColor.copy(alpha = 0.8f),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight)
            )
            if (isSelected) {
                drawRect(
                    color = Color.White.copy(alpha = 0.3f),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight)
                )
            }
        }

        // Tooltip
        val si = selectedIndex
        if (si != null && si in points.indices) {
            val (date, value, _) = points[si]
            val barHeight = (value / maxVal) * drawHeight
            val barX = yAxisWidth + si * spacing + spacing / 2
            val barY = padV + drawHeight - barHeight

            val dateStr = sdf.format(Date(date))
            val tooltipText = "$dateStr\n$tooltipLabel: ${formatChartValue(value)} $yAxisLabel"

            val tooltipPaint = android.graphics.Paint().apply {
                color = 0xFFFFFFFF.toInt()
                textSize = 12.sp.toPx()
                isAntiAlias = true
            }
            val lines = tooltipText.split("\n")
            val lineHeight = tooltipPaint.fontMetrics.bottom - tooltipPaint.fontMetrics.top
            val textWidths = lines.map { tooltipPaint.measureText(it) }
            val maxTextWidth = textWidths.max()
            val tooltipPadding = 8.dp.toPx()
            val tooltipW = maxTextWidth + tooltipPadding * 2
            val tooltipH = lineHeight * lines.size + tooltipPadding * 2

            var tx = barX - tooltipW / 2
            if (tx < 0f) tx = 4.dp.toPx()
            if (tx + tooltipW > size.width) tx = size.width - tooltipW - 4.dp.toPx()
            val ty = barY - tooltipH - 8.dp.toPx()
            val finalTy = if (ty < 0f) barY + 8.dp.toPx() else ty

            drawRoundRect(
                color = Color(0xE62C2C2E),
                topLeft = Offset(tx, finalTy),
                size = Size(tooltipW, tooltipH),
                cornerRadius = CornerRadius(8.dp.toPx())
            )
            lines.forEachIndexed { lineIdx, line ->
                drawContext.canvas.nativeCanvas.drawText(
                    line,
                    tx + tooltipPadding,
                    finalTy + tooltipPadding - tooltipPaint.fontMetrics.top + lineIdx * lineHeight,
                    tooltipPaint
                )
            }
        }
    }
}

private fun formatChartValue(value: Float): String {
    return if (value >= 100 || value == value.roundToInt().toFloat()) {
        value.roundToInt().toString()
    } else {
        String.format("%.1f", value)
    }
}

@Composable
fun ChartDateLabels(
    dates: List<Long>,
    modifier: Modifier = Modifier
) {
    if (dates.isEmpty()) return
    val sdf = remember { SimpleDateFormat("d.MM", Locale("ru")) }
    val indicesToShow = when {
        dates.size <= 5 -> dates.indices.toList()
        else -> listOf(0, dates.size / 4, dates.size / 2, dates.size * 3 / 4, dates.size - 1)
    }
    Row(modifier = modifier, horizontalArrangement = Arrangement.SpaceBetween) {
        dates.forEachIndexed { i, date ->
            if (i in indicesToShow) {
                Text(
                    sdf.format(Date(date)),
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorOnSurface
                )
            }
        }
    }
}
