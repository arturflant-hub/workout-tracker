package com.workouttracker.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val ColorBackground = Color(0xFF0F0F0F)
val ColorSurface = Color(0xFF1C1C1E)
val ColorSurfaceVariant = Color(0xFF2C2C2E)
val ColorPrimary = Color(0xFF6C63FF)
val ColorSecondary = Color(0xFF30D158)
val ColorError = Color(0xFFFF453A)
val ColorOnBackground = Color(0xFFE5E5EA)
val ColorOnSurface = Color(0xFFAEAEB2)

private val AppDarkColorScheme = darkColorScheme(
    primary = ColorPrimary,
    onPrimary = Color.White,
    secondary = ColorSecondary,
    onSecondary = Color.White,
    error = ColorError,
    onError = Color.White,
    background = ColorBackground,
    onBackground = ColorOnBackground,
    surface = ColorSurface,
    onSurface = ColorOnBackground,
    surfaceVariant = ColorSurfaceVariant,
    onSurfaceVariant = ColorOnSurface,
    outline = ColorSurfaceVariant
)

@Composable
fun WorkoutTrackerTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = ColorBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = AppDarkColorScheme,
        content = content
    )
}
