package com.workouttracker.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.workouttracker.ui.theme.*
import com.workouttracker.ui.viewmodel.BackupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    navController: NavController,
    viewModel: BackupViewModel = hiltViewModel()
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

    // Handle needsSignIn
    LaunchedEffect(state.needsSignIn) {
        if (state.needsSignIn) {
            signInLauncher.launch(viewModel.getSignInIntent())
        }
    }

    // Handle recovery intent (re-auth)
    LaunchedEffect(state.recoveryIntent) {
        state.recoveryIntent?.let { intent ->
            recoveryLauncher.launch(intent)
        }
    }

    Scaffold(
        containerColor = ColorBackground,
        topBar = {
            TopAppBar(
                title = { Text("Резервное копирование", color = ColorOnBackground) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = ColorOnBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorBackground)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(4.dp))

                // Info card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ColorSurface),
                    border = BorderStroke(1.dp, ColorSurfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Что сохраняется",
                            style = MaterialTheme.typography.titleSmall,
                            color = ColorOnBackground,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "- База тренировок и история\n- Программы и расписание\n- Замеры тела\n- Настройки приложения",
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorOnSurface,
                            lineHeight = 20.sp
                        )
                    }
                }

                // Google account card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ColorSurface),
                    border = BorderStroke(1.dp, ColorSurfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (state.isSignedIn) Icons.Default.CloudDone else Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = if (state.isSignedIn) ColorSecondary else ColorOnSurface
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (state.isSignedIn) "Google Drive" else "Не подключено",
                                style = MaterialTheme.typography.titleSmall,
                                color = ColorOnBackground,
                                fontWeight = FontWeight.Bold
                            )
                            if (state.accountEmail != null) {
                                Text(
                                    state.accountEmail!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ColorOnSurface
                                )
                            }
                        }
                        if (!state.isSignedIn) {
                            TextButton(onClick = {
                                signInLauncher.launch(viewModel.getSignInIntent())
                            }) {
                                Text("Войти", color = ColorPrimary)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Create backup button
                Button(
                    onClick = { viewModel.createBackup() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary),
                    enabled = !state.isLoading
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Создать резервную копию", fontSize = 16.sp)
                }

                // Restore button
                OutlinedButton(
                    onClick = { viewModel.requestRestore() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, ColorPrimary),
                    enabled = !state.isLoading
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, tint = ColorPrimary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Восстановить из резервной копии", color = ColorPrimary, fontSize = 16.sp)
                }

                // Status message
                if (state.statusMessage != null) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = ColorSecondary.copy(alpha = 0.15f))
                    ) {
                        Text(
                            state.statusMessage!!,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = ColorSecondary
                        )
                    }
                }

                // Error message
                if (state.error != null) {
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

                Spacer(Modifier.height(40.dp))
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
                        Text(
                            state.loadingMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ColorOnBackground
                        )
                    }
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
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 480.dp),
            title = {
                Text(
                    "Резервные копии (${state.backupFiles.size})",
                    color = ColorOnBackground,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.backupFiles) { backup ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = ColorSurfaceVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectBackupForRestore(backup) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        backup.date,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ColorOnBackground,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        backup.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ColorOnSurface
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.requestDelete(backup) }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Удалить",
                                        tint = ColorError,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
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
            text = {
                Text(
                    "Все текущие данные будут заменены данными из резервной копии. Это действие нельзя отменить.",
                    color = ColorOnSurface
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmRestore() }) {
                    Text("Восстановить", color = ColorError)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDialogs() }) {
                    Text("Отмена", color = ColorOnSurface)
                }
            }
        )
    }

    // Delete confirm dialog
    if (state.showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialogs() },
            containerColor = ColorSurface,
            title = { Text("Удалить резервную копию?", color = ColorOnBackground) },
            text = {
                Text(
                    "Файл \"${state.deleteTargetName}\" будет удалён с Google Drive. Это действие нельзя отменить.",
                    color = ColorOnSurface
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDelete() }) {
                    Text("Удалить", color = ColorError)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.dismissDialogs()
                    // Re-show backup list after cancel
                    viewModel.requestRestore()
                }) {
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
            text = {
                Text(
                    "В Google Drive не найдено резервных копий Workout Tracker. Сначала создайте резервную копию.",
                    color = ColorOnSurface
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissDialogs() }) {
                    Text("Понятно", color = ColorPrimary)
                }
            }
        )
    }
}
