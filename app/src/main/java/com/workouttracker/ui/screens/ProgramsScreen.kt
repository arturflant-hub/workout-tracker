package com.workouttracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.workouttracker.data.db.entities.WorkoutProgram
import com.workouttracker.ui.navigation.Screen
import com.workouttracker.ui.viewmodel.ProgramsViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramsScreen(
    navController: NavController,
    viewModel: ProgramsViewModel = hiltViewModel()
) {
    val programs by viewModel.programs.collectAsState()
    val selectedProgramId by viewModel.selectedProgramId.collectAsState()
    val exercises by viewModel.exercises.collectAsState()

    var showAddProgramDialog by remember { mutableStateOf(false) }

    // Drag-and-drop state — declared at top level (not inside conditionals)
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(
        lazyListState = listState,
        onMove = { from, to ->
            viewModel.moveExercise(from.index, to.index)
        }
    )

    // Persist order when drag ends
    val wasDragging = remember { mutableStateOf(false) }
    LaunchedEffect(reorderState.isAnyItemDragging) {
        if (wasDragging.value && !reorderState.isAnyItemDragging) {
            viewModel.persistExerciseOrder()
        }
        wasDragging.value = reorderState.isAnyItemDragging
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Программы тренировок") },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.ScheduleSettings.route) }) {
                        Icon(Icons.Default.Schedule, "Расписание")
                    }
                    IconButton(onClick = { navController.navigate(Screen.Calendar.route) }) {
                        Icon(Icons.Default.CalendarMonth, "Календарь")
                    }
                    IconButton(onClick = { navController.navigate(Screen.History.route) }) {
                        Icon(Icons.Default.History, "История")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddProgramDialog = true }) {
                Icon(Icons.Default.Add, "Добавить программу")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (programs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Нет программ. Нажмите + для создания", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                // Programs tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    programs.forEach { program ->
                        FilterChip(
                            selected = program.id == selectedProgramId,
                            onClick = { viewModel.selectProgram(program.id) },
                            label = { Text("${program.type}: ${program.name}") }
                        )
                    }
                }

                val selectedProgram = programs.find { it.id == selectedProgramId }

                if (selectedProgram != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Упражнения",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = {
                                navController.navigate(
                                    Screen.ExerciseEdit.createRoute(selectedProgram.id)
                                )
                            }
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Добавить")
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(exercises, key = { it.id }) { exercise ->
                            ReorderableItem(reorderState, key = exercise.id) { isDragging ->
                                ExerciseCard(
                                    name = exercise.name,
                                    sets = exercise.sets,
                                    minReps = exercise.minReps,
                                    maxReps = exercise.maxReps,
                                    weight = exercise.startWeight,
                                    isDragging = isDragging,
                                    dragHandle = {
                                        Icon(
                                            imageVector = Icons.Default.DragHandle,
                                            contentDescription = "Перетащить",
                                            modifier = Modifier
                                                .draggableHandle()
                                                .padding(horizontal = 4.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    onEdit = {
                                        navController.navigate(
                                            Screen.ExerciseEdit.createRoute(
                                                selectedProgram.id, exercise.id
                                            )
                                        )
                                    },
                                    onDelete = { viewModel.deleteExercise(exercise) }
                                )
                            }
                        }
                    }
                } else if (programs.isNotEmpty()) {
                    LaunchedEffect(programs) {
                        viewModel.selectProgram(programs.first().id)
                    }
                }
            }
        }
    }

    if (showAddProgramDialog) {
        AddProgramDialog(
            onDismiss = { showAddProgramDialog = false },
            onConfirm = { type, name ->
                viewModel.createProgram(type, name)
                showAddProgramDialog = false
            }
        )
    }
}

@Composable
fun ExerciseCard(
    name: String,
    sets: Int,
    minReps: Int,
    maxReps: Int,
    weight: Float,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    isDragging: Boolean = false,
    dragHandle: (@Composable () -> Unit)? = null
) {
    val elevation = if (isDragging) 8.dp else 1.dp

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag handle on the left
            if (dragHandle != null) {
                dragHandle()
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "$sets подх. × $minReps-$maxReps повт. @ ${weight}кг",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Редактировать", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Удалить", modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun AddProgramDialog(
    onDismiss: () -> Unit,
    onConfirm: (type: String, name: String) -> Unit
) {
    var type by remember { mutableStateOf("A") }
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая программа") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Тип: ")
                    listOf("A", "B").forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t) },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(type, name) },
                enabled = name.isNotBlank()
            ) { Text("Создать") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
