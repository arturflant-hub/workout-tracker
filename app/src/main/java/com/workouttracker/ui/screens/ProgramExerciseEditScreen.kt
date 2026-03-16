package com.workouttracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.workouttracker.data.db.entities.ProgramExercise
import com.workouttracker.ui.viewmodel.ExerciseEditViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramExerciseEditScreen(
    programId: Long,
    exerciseId: Long?,
    navController: NavController,
    viewModel: ExerciseEditViewModel = hiltViewModel()
) {
    val exercise by viewModel.exercise.collectAsState()

    var name by remember { mutableStateOf("") }
    var sets by remember { mutableStateOf("3") }
    var minReps by remember { mutableStateOf("8") }
    var maxReps by remember { mutableStateOf("12") }
    var startWeight by remember { mutableStateOf("20") }
    var weightNote by remember { mutableStateOf("barbell") }

    LaunchedEffect(exerciseId) {
        if (exerciseId != null) {
            viewModel.loadExercise(exerciseId)
        }
    }

    LaunchedEffect(exercise) {
        exercise?.let { ex ->
            name = ex.name
            sets = ex.sets.toString()
            minReps = ex.minReps.toString()
            maxReps = ex.maxReps.toString()
            startWeight = ex.startWeight.toString()
            weightNote = ex.startWeightNote.ifEmpty { "barbell" }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (exerciseId == null) "Новое упражнение" else "Редактировать") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Назад")
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
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название упражнения") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = sets,
                    onValueChange = { sets = it },
                    label = { Text("Подходы") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = minReps,
                    onValueChange = { minReps = it },
                    label = { Text("Мин повт.") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = maxReps,
                    onValueChange = { maxReps = it },
                    label = { Text("Макс повт.") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = startWeight,
                onValueChange = { startWeight = it },
                label = { Text("Начальный вес (кг)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )

            Text("Тип снаряда:", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("barbell" to "Штанга", "dumbbell" to "Гантели", "other" to "Другое").forEach { (key, label) ->
                    FilterChip(
                        selected = weightNote == key,
                        onClick = { weightNote = key },
                        label = { Text(label) }
                    )
                }
            }

            Button(
                onClick = {
                    val ex = ProgramExercise(
                        id = exercise?.id ?: 0L,
                        programId = programId,
                        orderIndex = exercise?.orderIndex ?: 0,
                        name = name.trim(),
                        sets = sets.toIntOrNull() ?: 3,
                        minReps = minReps.toIntOrNull() ?: 8,
                        maxReps = maxReps.toIntOrNull() ?: 12,
                        startWeight = startWeight.toFloatOrNull() ?: 20f,
                        startWeightNote = weightNote
                    )
                    viewModel.saveExercise(ex) { navController.popBackStack() }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank()
            ) {
                Text("Сохранить")
            }
        }
    }
}
