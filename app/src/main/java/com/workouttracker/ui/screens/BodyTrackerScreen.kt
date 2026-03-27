package com.workouttracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.workouttracker.data.db.entities.BodyMeasurement
import com.workouttracker.ui.components.LocalTopToastState
import com.workouttracker.ui.components.ToastType
import com.workouttracker.ui.components.TopToastHost
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
    var editingMeasurement by remember { mutableStateOf<BodyMeasurement?>(null) }

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
                    "Замеры тела",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = ColorOnBackground
                )
            }

            if (measurements.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.layout.Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "⚖️",
                                style = MaterialTheme.typography.headlineLarge
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Нет замеров",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = ColorOnBackground
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Нажмите + чтобы добавить первый замер",
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorOnSurface,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
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
            onDismiss = { selectedMeasurement = null },
            onEdit = {
                editingMeasurement = sel.measurement
                selectedMeasurement = null
            }
        )
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
    onDismiss: () -> Unit,
    onEdit: () -> Unit = {}
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
        },
        dismissButton = {
            TextButton(onClick = onEdit) {
                Text("Редактировать", color = ColorOnSurface)
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
    onConfirm: (BodyMeasurement) -> Unit,
    existing: BodyMeasurement? = null
) {
    val toastState = LocalTopToastState.current
    var weight by remember { mutableStateOf(existing?.weight?.let { if (it > 0f) "%.1f".format(it) else "" } ?: "") }
    var height by remember { mutableStateOf(existing?.height?.let { if (it > 0f) "%.0f".format(it) else "" } ?: "") }
    var waist by remember { mutableStateOf(existing?.waist?.let { "%.1f".format(it) } ?: "") }
    var neck by remember { mutableStateOf(existing?.neck?.let { "%.1f".format(it) } ?: "") }
    var chest by remember { mutableStateOf(existing?.chest?.let { "%.1f".format(it) } ?: "") }
    var hips by remember { mutableStateOf(existing?.hips?.let { "%.1f".format(it) } ?: "") }
    var thigh by remember { mutableStateOf(existing?.thigh?.let { "%.1f".format(it) } ?: "") }
    var arm by remember { mutableStateOf(existing?.arm?.let { "%.1f".format(it) } ?: "") }
    var age by remember { mutableStateOf(existing?.age?.toString() ?: "") }
    var errorFields by remember { mutableStateOf(setOf<String>()) }

    fun isZeroValue(value: String, isInt: Boolean = false): Boolean {
        if (value.isBlank()) return false
        return if (isInt) value.toIntOrNull() == 0 else value.toFloatOrNull() == 0f
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ColorSurface)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        if (existing != null) "Редактировать замер" else "Новый замер",
                        color = ColorOnBackground,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.height(16.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    ) {
                        MeasurementField("Вес, кг *", weight, isError = "weight" in errorFields) { weight = it; errorFields = errorFields - "weight" }
                        MeasurementField("Рост, см *", height, isError = "height" in errorFields) { height = it; errorFields = errorFields - "height" }
                        MeasurementField("Талия, см", waist, isError = "waist" in errorFields) { waist = it; errorFields = errorFields - "waist" }
                        MeasurementField("Шея, см", neck, isError = "neck" in errorFields) { neck = it; errorFields = errorFields - "neck" }
                        MeasurementField("Грудь, см", chest, isError = "chest" in errorFields) { chest = it; errorFields = errorFields - "chest" }
                        MeasurementField("Бёдра, см", hips, isError = "hips" in errorFields) { hips = it; errorFields = errorFields - "hips" }
                        MeasurementField("Бедро, см", thigh, isError = "thigh" in errorFields) { thigh = it; errorFields = errorFields - "thigh" }
                        MeasurementField("Рука, см", arm, isError = "arm" in errorFields) { arm = it; errorFields = errorFields - "arm" }
                        MeasurementField("Возраст", age, isInt = true, isError = "age" in errorFields) { age = it; errorFields = errorFields - "age" }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Отмена", color = ColorOnSurface)
                        }
                        TextButton(
                            onClick = {
                                val errors = mutableSetOf<String>()
                                if (isZeroValue(weight)) errors += "weight"
                                if (isZeroValue(height)) errors += "height"
                                if (isZeroValue(waist)) errors += "waist"
                                if (isZeroValue(neck)) errors += "neck"
                                if (isZeroValue(chest)) errors += "chest"
                                if (isZeroValue(hips)) errors += "hips"
                                if (isZeroValue(thigh)) errors += "thigh"
                                if (isZeroValue(arm)) errors += "arm"
                                if (isZeroValue(age, isInt = true)) errors += "age"

                                if (errors.isNotEmpty()) {
                                    errorFields = errors
                                    toastState.show("Значение не может быть 0", ToastType.ERROR)
                                    return@TextButton
                                }

                                val w = weight.toFloatOrNull() ?: return@TextButton
                                val h = height.toFloatOrNull() ?: return@TextButton
                                onConfirm(
                                    BodyMeasurement(
                                        id = existing?.id ?: 0L,
                                        date = existing?.date ?: System.currentTimeMillis(),
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
                    }
                }
            }

            // Toast overlay inside the same Dialog window — always on top
            TopToastHost(state = toastState)
        }
    }
}

@Composable
fun MeasurementField(
    label: String,
    value: String,
    isInt: Boolean = false,
    isError: Boolean = false,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw ->
            val filtered = if (isInt) {
                raw.filter { it.isDigit() }.trimStart('0').ifEmpty { if (raw.isNotEmpty()) "0" else "" }
            } else {
                val digits = raw.filter { it.isDigit() || it == '.' }
                when {
                    digits.startsWith("0") && digits.getOrNull(1)?.isDigit() == true ->
                        digits.trimStart('0').ifEmpty { "0" }
                    else -> digits
                }
            }
            onChange(filtered)
        },
        label = { Text(label, color = if (isError) ColorError else ColorOnSurface) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        isError = isError,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isInt) KeyboardType.Number else KeyboardType.Decimal
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = if (isError) ColorError else ColorPrimary,
            unfocusedBorderColor = if (isError) ColorError else ColorSurfaceVariant,
            errorBorderColor = ColorError,
            focusedTextColor = ColorOnBackground,
            unfocusedTextColor = ColorOnBackground,
            cursorColor = ColorPrimary
        )
    )
}
