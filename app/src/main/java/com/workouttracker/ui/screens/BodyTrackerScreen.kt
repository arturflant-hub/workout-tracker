package com.workouttracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.workouttracker.data.db.entities.BodyMeasurement
import com.workouttracker.ui.theme.*
import com.workouttracker.ui.viewmodel.BodyMeasurementUi
import com.workouttracker.ui.viewmodel.BodyTrackerViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BodyTrackerScreen(
    viewModel: BodyTrackerViewModel = hiltViewModel()
) {
    val measurements by viewModel.measurements.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedMeasurement by remember { mutableStateOf<BodyMeasurementUi?>(null) }

    val sdf = remember { SimpleDateFormat("d MMM yyyy", Locale("ru")) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 20.dp, horizontal = 0.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Антропометрия",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = ColorOnBackground
                )
            }

            if (measurements.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Нет замеров.\nНажмите + чтобы добавить",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ColorOnSurface,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            items(measurements, key = { it.measurement.id }) { item ->
                BodyMeasurementCard(
                    item = item,
                    sdf = sdf,
                    onClick = { selectedMeasurement = item },
                    onDelete = { viewModel.delete(item.measurement) }
                )
            }
        }

        // FAB at bottom right
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = ColorPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(Icons.Default.Add, "Добавить замер", tint = ColorOnBackground)
        }
    }

    if (showAddDialog) {
        AddMeasurementDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { measurement ->
                viewModel.insert(measurement)
                showAddDialog = false
            }
        )
    }

    selectedMeasurement?.let { sel ->
        MeasurementDetailDialog(
            item = sel,
            sdf = sdf,
            weightChange = viewModel.weightChangeFromStart(sel.measurement),
            waistChange = viewModel.waistChangeFromStart(sel.measurement),
            onDismiss = { selectedMeasurement = null }
        )
    }
}

@Composable
fun BodyMeasurementCard(
    item: BodyMeasurementUi,
    sdf: SimpleDateFormat,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val m = item.measurement
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ColorSurface),
        border = BorderStroke(1.dp, ColorSurfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    sdf.format(Date(m.date)),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorOnBackground
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "%.1f кг".format(m.weight),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorOnBackground
                )
                item.bodyFatNavy?.let {
                    Text(
                        "%.1f%% жира".format(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorPrimary
                    )
                }
                m.waist?.let {
                    Text(
                        "Талия: %.1f см".format(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorOnSurface
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Удалить", tint = ColorError)
            }
        }
    }
}

@Composable
fun MeasurementDetailDialog(
    item: BodyMeasurementUi,
    sdf: SimpleDateFormat,
    weightChange: Float?,
    waistChange: Float?,
    onDismiss: () -> Unit
) {
    val m = item.measurement
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ColorSurface,
        title = {
            Text(
                sdf.format(Date(m.date)),
                color = ColorOnBackground,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DetailRow("Вес", "%.1f кг".format(m.weight))
                DetailRow("Рост", "%.0f см".format(m.height))
                m.waist?.let { DetailRow("Талия", "%.1f см".format(it)) }
                m.neck?.let { DetailRow("Шея", "%.1f см".format(it)) }
                m.chest?.let { DetailRow("Грудь", "%.1f см".format(it)) }
                m.hips?.let { DetailRow("Бёдра", "%.1f см".format(it)) }
                m.thigh?.let { DetailRow("Бедро", "%.1f см".format(it)) }
                m.arm?.let { DetailRow("Рука", "%.1f см".format(it)) }
                m.age?.let { DetailRow("Возраст", "$it лет") }
                HorizontalDivider(color = ColorSurfaceVariant)
                item.bodyFatNavy?.let { DetailRow("% жира (Navy)", "%.1f%%".format(it)) }
                item.waistToHeight?.let { DetailRow("Талия/Рост", "%.2f".format(it)) }
                weightChange?.let {
                    val sign = if (it >= 0) "+" else ""
                    DetailRow("Δ вес (от старта)", "$sign%.1f кг".format(it))
                }
                waistChange?.let {
                    val sign = if (it >= 0) "+" else ""
                    DetailRow("Δ талия (от старта)", "$sign%.1f см".format(it))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть", color = ColorPrimary)
            }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = ColorOnSurface)
        Text(value, style = MaterialTheme.typography.bodySmall, color = ColorOnBackground, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun AddMeasurementDialog(
    onDismiss: () -> Unit,
    onConfirm: (BodyMeasurement) -> Unit
) {
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var waist by remember { mutableStateOf("") }
    var neck by remember { mutableStateOf("") }
    var chest by remember { mutableStateOf("") }
    var hips by remember { mutableStateOf("") }
    var thigh by remember { mutableStateOf("") }
    var arm by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ColorSurface,
        title = {
            Text("Новый замер", color = ColorOnBackground, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                MeasurementField("Вес, кг *", weight) { weight = it }
                MeasurementField("Рост, см *", height) { height = it }
                MeasurementField("Талия, см", waist) { waist = it }
                MeasurementField("Шея, см", neck) { neck = it }
                MeasurementField("Грудь, см", chest) { chest = it }
                MeasurementField("Бёдра, см", hips) { hips = it }
                MeasurementField("Бедро, см", thigh) { thigh = it }
                MeasurementField("Рука, см", arm) { arm = it }
                MeasurementField("Возраст", age, isInt = true) { age = it }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val w = weight.toFloatOrNull() ?: return@TextButton
                    val h = height.toFloatOrNull() ?: return@TextButton
                    onConfirm(
                        BodyMeasurement(
                            date = System.currentTimeMillis(),
                            weight = w,
                            height = h,
                            waist = waist.toFloatOrNull(),
                            neck = neck.toFloatOrNull(),
                            chest = chest.toFloatOrNull(),
                            hips = hips.toFloatOrNull(),
                            thigh = thigh.toFloatOrNull(),
                            arm = arm.toFloatOrNull(),
                            age = age.toIntOrNull()
                        )
                    )
                }
            ) {
                Text("Сохранить", color = ColorPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = ColorOnSurface)
            }
        }
    )
}

@Composable
fun MeasurementField(
    label: String,
    value: String,
    isInt: Boolean = false,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, color = ColorOnSurface) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isInt) KeyboardType.Number else KeyboardType.Decimal
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ColorPrimary,
            unfocusedBorderColor = ColorSurfaceVariant,
            focusedTextColor = ColorOnBackground,
            unfocusedTextColor = ColorOnBackground,
            cursorColor = ColorPrimary
        )
    )
}
