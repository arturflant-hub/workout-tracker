package com.workouttracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.workouttracker.ui.theme.*
import com.workouttracker.ui.viewmodel.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(Modifier.height(20.dp))

        Text(
            "Статистика",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = ColorOnBackground
        )

        // Tonnage chart
        StatCard(title = "Тоннаж по тренировкам") {
            if (state.tonnagePoints.isEmpty()) {
                EmptyChartMessage()
            } else {
                LineChart(
                    points = state.tonnagePoints.map { it.date.toFloat() to it.tonnage },
                    lineColor = ColorPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
                ChartDateLabels(
                    dates = state.tonnagePoints.map { it.date },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Exercise progress
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
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ColorPrimary,
                            unfocusedBorderColor = ColorSurfaceVariant,
                            focusedTextColor = ColorOnBackground,
                            unfocusedTextColor = ColorOnBackground,
                            cursorColor = ColorPrimary
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
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

        // Body weight chart
        StatCard(title = "Динамика веса тела") {
            if (state.bodyWeightPoints.isEmpty()) {
                EmptyChartMessage()
            } else {
                LineChart(
                    points = state.bodyWeightPoints.map { it.date.toFloat() to it.weight },
                    lineColor = ColorPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
                ChartDateLabels(
                    dates = state.bodyWeightPoints.map { it.date },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Body fat chart
        StatCard(title = "Динамика % жира (Navy)") {
            if (state.bodyFatPoints.isEmpty()) {
                EmptyChartMessage("Добавьте замеры талии и шеи в разделе Тело")
            } else {
                LineChart(
                    points = state.bodyFatPoints.map { it.date.toFloat() to it.bodyFat },
                    lineColor = ColorError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
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
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = ColorOnSurface)
    }
}

@Composable
fun LineChart(
    points: List<Pair<Float, Float>>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    if (points.size < 2) {
        // Draw single point
        Canvas(modifier = modifier) {
            drawCircle(color = lineColor, radius = 6.dp.toPx(), center = Offset(size.width / 2, size.height / 2))
        }
        return
    }

    val minX = points.minOf { it.first }
    val maxX = points.maxOf { it.first }
    val minY = points.minOf { it.second }
    val maxY = points.maxOf { it.second }
    val rangeX = (maxX - minX).takeIf { it > 0 } ?: 1f
    val rangeY = (maxY - minY).takeIf { it > 0 } ?: 1f

    Canvas(modifier = modifier) {
        val padding = 16.dp.toPx()
        val drawWidth = size.width - padding * 2
        val drawHeight = size.height - padding * 2

        fun xFor(x: Float) = padding + (x - minX) / rangeX * drawWidth
        fun yFor(y: Float) = padding + drawHeight - (y - minY) / rangeY * drawHeight

        // Grid lines
        val gridLines = 4
        repeat(gridLines + 1) { i ->
            val y = padding + drawHeight * i / gridLines
            drawLine(
                color = ColorSurfaceVariant,
                start = Offset(padding, y),
                end = Offset(padding + drawWidth, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Line
        val path = Path()
        points.forEachIndexed { i, (x, y) ->
            val cx = xFor(x)
            val cy = yFor(y)
            if (i == 0) path.moveTo(cx, cy) else path.lineTo(cx, cy)
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // Fill under line
        val fillPath = Path()
        fillPath.addPath(path)
        fillPath.lineTo(xFor(points.last().first), padding + drawHeight)
        fillPath.lineTo(xFor(points.first().first), padding + drawHeight)
        fillPath.close()
        drawPath(fillPath, color = lineColor.copy(alpha = 0.1f))

        // Dots
        points.forEach { (x, y) ->
            drawCircle(
                color = lineColor,
                radius = 4.dp.toPx(),
                center = Offset(xFor(x), yFor(y))
            )
        }
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
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
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
