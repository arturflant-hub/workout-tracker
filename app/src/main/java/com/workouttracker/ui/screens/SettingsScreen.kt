package com.workouttracker.ui.screens

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.workouttracker.ui.theme.*

@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("workout_prefs", Context.MODE_PRIVATE) }
    var restDuration by remember { mutableStateOf(prefs.getInt("rest_timer_duration", 90)) }
    var showTimerDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
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
            onClick = { showTimerDialog = true }
        )
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
