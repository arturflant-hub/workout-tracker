package com.workouttracker.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.workouttracker.ui.navigation.Screen
import com.workouttracker.ui.components.LocalTopToastState
import com.workouttracker.ui.components.ToastType
import com.workouttracker.ui.components.TopToastHost
import com.workouttracker.ui.theme.*
import com.workouttracker.ui.viewmodel.DevToolsViewModel
import com.workouttracker.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    navController: NavController,
    devToolsViewModel: DevToolsViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val toastState = LocalTopToastState.current
    val prefs = remember { context.getSharedPreferences("workout_prefs", Context.MODE_PRIVATE) }
    var restDuration by remember { mutableStateOf(prefs.getInt("rest_timer_duration", 90)) }
    var showTimerDialog by remember { mutableStateOf(false) }

    // Request notification permission when opening rest timer settings
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* user's choice */ }

    fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    var showDevMenu by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var tapCount by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    var tapResetJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val profileState by settingsViewModel.profile.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(20.dp))

        Text(
            "Настройки",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = ColorOnBackground
        )

        Spacer(Modifier.height(8.dp))

        // Profile section
        Text(
            "Профиль",
            style = MaterialTheme.typography.labelMedium,
            color = ColorOnSurface
        )

        profileState.user?.let { user ->
            val genderText = when (user.gender) {
                "male" -> "Мужской"
                "female" -> "Женский"
                else -> ""
            }
            val ageText = profileState.age?.let { "${it} лет" } ?: ""
            val subtitle = listOf(genderText, ageText).filter { it.isNotEmpty() }.joinToString(", ")

            SettingsItem(
                title = user.name.ifBlank { "Не указано" },
                subtitle = subtitle.ifBlank { "Нажмите для редактирования" },
                onClick = { showProfileDialog = true }
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            "Программы",
            style = MaterialTheme.typography.labelMedium,
            color = ColorOnSurface
        )

        SettingsItem(
            title = "Программы тренировок",
            subtitle = "Управление программами A/B",
            onClick = { navController.navigate("programs") }
        )

        SettingsItem(
            title = "Расписание тренировок",
            subtitle = "Настройка дней и генерация расписания",
            onClick = { navController.navigate("schedule_settings") }
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Тренировки",
            style = MaterialTheme.typography.labelMedium,
            color = ColorOnSurface
        )

        SettingsItem(
            title = "Время отдыха",
            subtitle = "Сейчас: ${restDuration} сек",
            onClick = {
                ensureNotificationPermission()
                showTimerDialog = true
            }
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Данные",
            style = MaterialTheme.typography.labelMedium,
            color = ColorOnSurface
        )

        SettingsItem(
            title = "Резервное копирование",
            subtitle = "Google Drive: сохранение и восстановление",
            onClick = { navController.navigate("backup") }
        )

        Spacer(Modifier.height(16.dp))

        // Version block — tap 4 times to open dev menu
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    tapResetJob?.cancel()
                    tapCount++
                    if (tapCount >= 4) {
                        tapCount = 0
                        showDevMenu = true
                    } else {
                        tapResetJob = scope.launch {
                            delay(1500)
                            tapCount = 0
                        }
                    }
                },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ColorSurface),
            border = BorderStroke(1.dp, ColorSurfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Workout Tracker",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = ColorOnBackground
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Версия ${com.workouttracker.BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorOnSurface
                )
                if (tapCount in 1..3) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Ещё ${4 - tapCount} нажатия...",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorPrimary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        val annotatedText = buildAnnotatedString {
            withStyle(SpanStyle(color = ColorOnSurface.copy(alpha = 0.4f))) {
                append("app by ")
            }
            pushStringAnnotation(tag = "URL", annotation = "https://t.me/ArtCla")
            withStyle(SpanStyle(color = ColorPrimary, textDecoration = TextDecoration.Underline)) {
                append("Cla")
            }
            pop()
        }
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            ClickableText(
                text = annotatedText,
                style = MaterialTheme.typography.bodySmall,
                onClick = { offset ->
                    annotatedText.getStringAnnotations("URL", offset, offset)
                        .firstOrNull()?.let {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.item)))
                        }
                }
            )
        }

        Spacer(Modifier.height(20.dp))
    }

    if (showTimerDialog) {
        RestTimerDialog(
            current = restDuration,
            onSelect = { duration ->
                restDuration = duration
                prefs.edit().putInt("rest_timer_duration", duration).apply()
                showTimerDialog = false
            },
            onDismiss = { showTimerDialog = false }
        )
    }

    if (showDevMenu) {
        DevToolsDialog(
            viewModel = devToolsViewModel,
            navController = navController,
            onDismiss = { showDevMenu = false }
        )
    }

    if (showProfileDialog) {
        ProfileEditDialog(
            currentName = profileState.user?.name ?: "",
            currentGender = profileState.user?.gender ?: "",
            currentAge = profileState.age?.toString() ?: "",
            onSave = { name, gender, age ->
                settingsViewModel.saveProfile(name, gender, age.toIntOrNull())
                showProfileDialog = false
            },
            onDismiss = { showProfileDialog = false }
        )
    }
}

@Composable
fun DevToolsDialog(
    viewModel: DevToolsViewModel,
    navController: NavController,
    onDismiss: () -> Unit
) {
    val toastState = LocalTopToastState.current
    var pendingAction by remember { mutableStateOf<DevAction?>(null) }

    if (pendingAction != null) {
        val action = pendingAction!!
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            containerColor = ColorSurface,
            icon = {
                Icon(
                    if (action.isPositive) Icons.Default.Add else Icons.Default.Warning,
                    null,
                    tint = if (action.isPositive) ColorSecondary else ColorError
                )
            },
            title = {
                Text(action.title, color = ColorOnBackground, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(action.confirmText, color = ColorOnSurface)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        action.execute(viewModel) {
                            if (action.isFullReset) {
                                pendingAction = null
                                onDismiss()
                                navController.navigate(Screen.Onboarding.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            } else {
                                toastState.show(
                                    action.toastMessage,
                                    if (action.isPositive) ToastType.SUCCESS else ToastType.SUCCESS
                                )
                                pendingAction = null
                            }
                        }
                    }
                ) {
                    Text(
                        action.confirmButtonText,
                        color = if (action.isPositive) ColorSecondary else ColorError,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingAction = null }) {
                    Text("Отмена", color = ColorOnSurface)
                }
            }
        )
        return
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.BugReport, null, tint = ColorPrimary)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Меню разработчика",
                        color = ColorOnBackground,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DevActions.all.forEach { action ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { pendingAction = action },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = when {
                                        action.isDangerous -> ColorError.copy(alpha = 0.08f)
                                        action.isPositive -> ColorSecondary.copy(alpha = 0.08f)
                                        else -> ColorSurfaceVariant
                                    }
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    when {
                                        action.isDangerous -> ColorError.copy(alpha = 0.3f)
                                        action.isPositive -> ColorSecondary.copy(alpha = 0.3f)
                                        else -> ColorSurfaceVariant
                                    }
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            action.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = when {
                                                action.isDangerous -> ColorError
                                                action.isPositive -> ColorSecondary
                                                else -> ColorOnBackground
                                            }
                                        )
                                        Text(
                                            action.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ColorOnSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Закрыть", color = ColorOnSurface)
                        }
                    }
                }
            }

            // Toast overlay inside the same Dialog window
            TopToastHost(state = toastState)
        }
    }
}

private data class DevAction(
    val title: String,
    val subtitle: String,
    val confirmText: String,
    val isDangerous: Boolean = false,
    val isPositive: Boolean = false,
    val isFullReset: Boolean = false,
    val confirmButtonText: String = "Удалить",
    val toastMessage: String = "Готово",
    val execute: (DevToolsViewModel, () -> Unit) -> Unit
)

private object DevActions {
    val all = listOf(
        DevAction(
            title = "Заполнить тестовыми данными",
            subtitle = "Создаёт программы, тренировки за 2 недели и замеры тела",
            confirmText = "Все текущие данные будут удалены и заменены тестовыми: 2 программы (A/B), 6 тренировок, 4 замера тела.",
            isPositive = true,
            confirmButtonText = "Создать",
            toastMessage = "Тестовые данные созданы",
            execute = { vm, done -> vm.generateMockData(done) }
        ),
        DevAction(
            title = "Сброс тренировок",
            subtitle = "Удаляет все завершённые и активные тренировки",
            confirmText = "Все тренировки, подходы и результаты будут удалены без возможности восстановления.",
            toastMessage = "Тренировки удалены",
            execute = { vm, done -> vm.resetWorkouts(done) }
        ),
        DevAction(
            title = "Сброс расписания",
            subtitle = "Удаляет настройки расписания и запланированные тренировки",
            confirmText = "Расписание, шаблоны недель и все запланированные тренировки будут удалены.",
            toastMessage = "Расписание удалено",
            execute = { vm, done -> vm.resetSchedule(done) }
        ),
        DevAction(
            title = "Сброс программ",
            subtitle = "Удаляет программы тренировок A/B и все упражнения",
            confirmText = "Все программы тренировок и упражнения в них будут удалены.",
            toastMessage = "Программы удалены",
            execute = { vm, done -> vm.resetPrograms(done) }
        ),
        DevAction(
            title = "Сброс антропометрии",
            subtitle = "Удаляет все замеры тела и вес",
            confirmText = "Все замеры тела (вес, обхваты, возраст) будут удалены.",
            toastMessage = "Антропометрия удалена",
            execute = { vm, done -> vm.resetBodyMeasurements(done) }
        ),
        DevAction(
            title = "Полный сброс",
            subtitle = "Удаляет ВСЕ данные и открывает регистрацию",
            confirmText = "ВСЕ данные приложения будут удалены: тренировки, расписание, программы, антропометрия, профиль пользователя. После сброса откроется экран регистрации. Это действие необратимо.",
            isDangerous = true,
            isFullReset = true,
            toastMessage = "Все данные удалены",
            execute = { vm, done -> vm.resetAll(done) }
        )
    )
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = ColorOnBackground
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorOnSurface
                )
            }
            Icon(Icons.Default.ChevronRight, null, tint = ColorOnSurface)
        }
    }
}

@Composable
fun RestTimerDialog(
    current: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(60, 90, 120, 180)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ColorSurface,
        title = {
            Text("Время отдыха", color = ColorOnBackground, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                options.forEach { sec ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(sec) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = sec == current,
                            onClick = { onSelect(sec) },
                            colors = RadioButtonDefaults.colors(selectedColor = ColorPrimary)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "$sec секунд",
                            color = ColorOnBackground,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть", color = ColorOnSurface)
            }
        }
    )
}

@Composable
private fun ProfileEditDialog(
    currentName: String,
    currentGender: String,
    currentAge: String,
    onSave: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var gender by remember { mutableStateOf(currentGender) }
    var age by remember { mutableStateOf(currentAge) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ColorSurface,
        title = {
            Text("Редактировать профиль", color = ColorOnBackground, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Имя") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ColorPrimary,
                        unfocusedBorderColor = ColorSurfaceVariant,
                        focusedTextColor = ColorOnBackground,
                        unfocusedTextColor = ColorOnBackground,
                        focusedLabelColor = ColorPrimary,
                        unfocusedLabelColor = ColorOnSurface,
                        cursorColor = ColorPrimary
                    )
                )

                Text("Пол", style = MaterialTheme.typography.labelMedium, color = ColorOnSurface)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf("male" to "Мужской", "female" to "Женский").forEach { (value, label) ->
                        val selected = gender == value
                        OutlinedButton(
                            onClick = { gender = value },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                if (selected) 2.dp else 1.dp,
                                if (selected) ColorPrimary else ColorSurfaceVariant
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selected) ColorPrimary.copy(alpha = 0.15f) else ColorSurface
                            )
                        ) {
                            Icon(
                                if (value == "male") Icons.Default.Male else Icons.Default.Female,
                                contentDescription = null,
                                tint = if (selected) ColorPrimary else ColorOnSurface,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                label,
                                color = if (selected) ColorPrimary else ColorOnSurface,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it.filter { c -> c.isDigit() } },
                    label = { Text("Возраст") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ColorPrimary,
                        unfocusedBorderColor = ColorSurfaceVariant,
                        focusedTextColor = ColorOnBackground,
                        unfocusedTextColor = ColorOnBackground,
                        focusedLabelColor = ColorPrimary,
                        unfocusedLabelColor = ColorOnSurface,
                        cursorColor = ColorPrimary
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, gender, age) },
                enabled = name.isNotBlank()
            ) {
                Text("Сохранить", color = ColorPrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = ColorOnSurface)
            }
        }
    )
}
