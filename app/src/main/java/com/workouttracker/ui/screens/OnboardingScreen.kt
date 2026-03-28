package com.workouttracker.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.workouttracker.ui.theme.*
import com.workouttracker.ui.viewmodel.OnboardingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.onSignInResult(result.data)
    }

    val recoveryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.onRecoveryResult(result.data)
    }

    LaunchedEffect(state.needsSignIn) {
        if (state.needsSignIn) {
            signInLauncher.launch(viewModel.getSignInIntent())
        }
    }

    LaunchedEffect(state.recoveryIntent) {
        state.recoveryIntent?.let { recoveryLauncher.launch(it) }
    }

    LaunchedEffect(state.isComplete) {
        if (state.isComplete) onComplete()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(80.dp))

            Text(
                "Workout Tracker",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = ColorPrimary
            )

            Spacer(Modifier.height(8.dp))

            // Step indicator
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { i ->
                    val stepNum = i + 1
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (stepNum <= state.currentStep) ColorPrimary else ColorSurfaceVariant,
                        modifier = Modifier.size(width = 40.dp, height = 4.dp)
                    ) {}
                }
            }

            Spacer(Modifier.height(48.dp))

            AnimatedContent(
                targetState = state.currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "step"
            ) { step ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (step) {
                        1 -> StepName(
                            name = state.name,
                            onNameChange = viewModel::updateName,
                            onNext = { viewModel.nextStep() },
                            onRestore = { viewModel.requestRestore() }
                        )
                        2 -> StepGender(
                            selectedGender = state.gender,
                            onSelect = { viewModel.selectGender(it); viewModel.nextStep() },
                            onBack = { viewModel.prevStep() }
                        )
                        3 -> StepAge(
                            age = state.age,
                            onAgeChange = viewModel::updateAge,
                            onComplete = { viewModel.completeOnboarding() },
                            onBack = { viewModel.prevStep() }
                        )
                    }
                }
            }

            // Error
            if (state.error != null) {
                Spacer(Modifier.height(16.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ColorError.copy(alpha = 0.15f))
                ) {
                    Text(
                        state.error!!,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ColorError
                    )
                }
            }
        }

        // Loading overlay
        if (state.isLoading) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = ColorBackground.copy(alpha = 0.7f)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = ColorPrimary)
                    Spacer(Modifier.height(16.dp))
                    Text(state.loadingMessage, style = MaterialTheme.typography.bodyMedium, color = ColorOnBackground)
                }
            }
        }
    }

    // Backup list dialog
    if (state.showBackupListDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialogs() },
            containerColor = ColorSurface,
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.fillMaxWidth(0.92f).heightIn(max = 480.dp),
            title = { Text("Резервные копии (${state.backupFiles.size})", color = ColorOnBackground, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.backupFiles) { backup ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = ColorSurfaceVariant),
                            modifier = Modifier.fillMaxWidth().clickable { viewModel.selectBackupForRestore(backup) }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(backup.date, style = MaterialTheme.typography.bodyMedium, color = ColorOnBackground, fontWeight = FontWeight.Medium)
                                Text(backup.name, style = MaterialTheme.typography.labelSmall, color = ColorOnSurface)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissDialogs() }) {
                    Text("Закрыть", color = ColorOnSurface)
                }
            }
        )
    }

    // Restore confirm dialog
    if (state.showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialogs() },
            containerColor = ColorSurface,
            title = { Text("Восстановить данные?", color = ColorOnBackground) },
            text = { Text("Все данные будут загружены из резервной копии.", color = ColorOnSurface) },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmRestore() }) {
                    Text("Восстановить", color = ColorPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDialogs() }) {
                    Text("Отмена", color = ColorOnSurface)
                }
            }
        )
    }

    // Not found dialog
    if (state.showNotFoundDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialogs() },
            containerColor = ColorSurface,
            title = { Text("Резервная копия не найдена", color = ColorOnBackground) },
            text = { Text("В Google Drive не найдено резервных копий Workout Tracker.", color = ColorOnSurface) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissDialogs() }) {
                    Text("Понятно", color = ColorPrimary)
                }
            }
        )
    }
}

@Composable
private fun StepName(
    name: String,
    onNameChange: (String) -> Unit,
    onNext: () -> Unit,
    onRestore: () -> Unit
) {
    Text(
        "Как вас зовут?",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = ColorOnBackground
    )

    Spacer(Modifier.height(32.dp))

    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text("Имя") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
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

    Spacer(Modifier.height(24.dp))

    Button(
        onClick = onNext,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary),
        enabled = name.isNotBlank()
    ) {
        Text("Далее", fontSize = 16.sp)
    }

    Spacer(Modifier.height(32.dp))

    OutlinedButton(
        onClick = onRestore,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ColorSurfaceVariant)
    ) {
        Text("Восстановить данные", color = ColorOnSurface, fontSize = 14.sp)
    }
}

@Composable
private fun StepGender(
    selectedGender: String,
    onSelect: (String) -> Unit,
    onBack: () -> Unit
) {
    Text(
        "Ваш пол",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = ColorOnBackground
    )

    Spacer(Modifier.height(32.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        GenderCard(
            label = "Мужской",
            icon = Icons.Default.Male,
            isSelected = selectedGender == "male",
            onClick = { onSelect("male") },
            modifier = Modifier.weight(1f)
        )
        GenderCard(
            label = "Женский",
            icon = Icons.Default.Female,
            isSelected = selectedGender == "female",
            onClick = { onSelect("female") },
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(Modifier.height(32.dp))

    TextButton(onClick = onBack) {
        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = ColorOnSurface, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text("Назад", color = ColorOnSurface)
    }
}

@Composable
private fun GenderCard(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) ColorPrimary.copy(alpha = 0.15f) else ColorSurface
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) ColorPrimary else ColorSurfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (isSelected) ColorPrimary else ColorOnSurface,
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) ColorPrimary else ColorOnBackground,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun StepAge(
    age: String,
    onAgeChange: (String) -> Unit,
    onComplete: () -> Unit,
    onBack: () -> Unit
) {
    Text(
        "Ваш возраст",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = ColorOnBackground
    )

    Spacer(Modifier.height(32.dp))

    OutlinedTextField(
        value = age,
        onValueChange = onAgeChange,
        label = { Text("Возраст") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
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

    Spacer(Modifier.height(24.dp))

    Button(
        onClick = onComplete,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary),
        enabled = age.isNotBlank() && (age.toIntOrNull() ?: 0) in 10..120
    ) {
        Text("Начать", fontSize = 16.sp)
    }

    Spacer(Modifier.height(16.dp))

    TextButton(onClick = onBack) {
        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = ColorOnSurface, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text("Назад", color = ColorOnSurface)
    }
}
