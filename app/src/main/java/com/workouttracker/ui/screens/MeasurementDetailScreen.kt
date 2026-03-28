package com.workouttracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.workouttracker.data.db.entities.BodyMeasurement
import com.workouttracker.ui.theme.*
import com.workouttracker.ui.viewmodel.BodyTrackerViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementDetailScreen(
    measurementId: Long,
    navController: NavController,
    viewModel: BodyTrackerViewModel = hiltViewModel()
) {
    val measurements by viewModel.measurements.collectAsState()
    val isLoaded = measurements.isNotEmpty() || viewModel.isInitialLoadDone
    val item = measurements.find { it.measurement.id == measurementId }
    var editingMeasurement by remember { mutableStateOf<BodyMeasurement?>(null) }
    val sdf = remember { SimpleDateFormat("d MMM yyyy", Locale("ru")) }

    // Still loading data from Room
    if (!isLoaded) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator(color = ColorPrimary)
        }
        return
    }

    // Data loaded but measurement not found (deleted)
    if (item == null) {
        LaunchedEffect(Unit) { navController.popBackStack() }
        return
    }

    val m = item.measurement
    val weightChange = viewModel.weightChangeFromStart(m)
    val waistChange = viewModel.waistChangeFromStart(m)

    Scaffold(
        containerColor = ColorBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Замер",
                        color = ColorOnBackground,
                        fontWeight = FontWeight.Bold
                    )
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorBackground)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Date header
            Text(
                sdf.format(Date(m.date)),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = ColorOnBackground
            )

            // Main measurements card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ColorSurface),
                border = BorderStroke(1.dp, ColorSurfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Основные замеры",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    DetailRow("Вес", "%.1f кг".format(m.weight))
                    DetailRow("Рост", "%.0f см".format(m.height))
                    m.waist?.let { DetailRow("Талия", "%.1f см".format(it)) }
                    m.neck?.let { DetailRow("Шея", "%.1f см".format(it)) }
                    m.chest?.let { DetailRow("Грудь", "%.1f см".format(it)) }
                    m.hips?.let { DetailRow("Бёдра", "%.1f см".format(it)) }
                    m.thigh?.let { DetailRow("Бедро", "%.1f см".format(it)) }
                    m.arm?.let { DetailRow("Рука", "%.1f см".format(it)) }
                    m.age?.let { DetailRow("Возраст", "$it лет") }
                }
            }

            // Calculated metrics card
            val hasCalculated = item.bodyFatNavy != null || item.waistToHeight != null
                    || weightChange != null || waistChange != null
            if (hasCalculated) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ColorSurface),
                    border = BorderStroke(1.dp, ColorSurfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Расчётные показатели",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = ColorPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        item.bodyFatNavy?.let {
                            DetailRow("% жира (Navy)", "%.1f%%".format(it))
                        }
                        item.waistToHeight?.let {
                            DetailRow("Талия / Рост", "%.2f".format(it))
                        }
                        weightChange?.let {
                            val sign = if (it >= 0) "+" else ""
                            DetailRow("Δ вес (от старта)", "$sign%.1f кг".format(it))
                        }
                        waistChange?.let {
                            val sign = if (it >= 0) "+" else ""
                            DetailRow("Δ талия (от старта)", "$sign%.1f см".format(it))
                        }
                    }
                }
            }

            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ColorSurfaceVariant),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ColorOnSurface
                    )
                ) {
                    Text("Закрыть")
                }
                Button(
                    onClick = { editingMeasurement = m },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorPrimary,
                        contentColor = ColorOnBackground
                    )
                ) {
                    Text("Редактировать")
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }

    editingMeasurement?.let { existing ->
        AddMeasurementDialog(
            onDismiss = { editingMeasurement = null },
            onConfirm = { measurement ->
                viewModel.update(measurement)
                editingMeasurement = null
            },
            existing = existing
        )
    }
}
