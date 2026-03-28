package com.workouttracker.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

enum class ToastType { SUCCESS, ERROR }

data class ToastData(
    val message: String,
    val type: ToastType = ToastType.SUCCESS
)

@Stable
class TopToastState {
    var current by mutableStateOf<ToastData?>(null)
        private set

    fun show(message: String, type: ToastType = ToastType.SUCCESS) {
        current = ToastData(message, type)
    }

    fun dismiss() {
        current = null
    }
}

@Composable
fun rememberTopToastState(): TopToastState {
    return remember { TopToastState() }
}

val LocalTopToastState = compositionLocalOf<TopToastState> {
    error("No TopToastState provided")
}

@Composable
fun TopToastHost(
    state: TopToastState,
    modifier: Modifier = Modifier
) {
    val data = state.current

    LaunchedEffect(data) {
        if (data != null) {
            delay(2500)
            state.dismiss()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = data != null,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it }
        ) {
            // Remember last non-null data for exit animation
            val animData = remember { mutableStateOf(data) }
            if (data != null) animData.value = data
            val d = animData.value ?: return@AnimatedVisibility

            val bgColor = when (d.type) {
                ToastType.SUCCESS -> Color(0xFF1B3A2A)
                ToastType.ERROR -> Color(0xFF3A1B1B)
            }
            val borderColor = when (d.type) {
                ToastType.SUCCESS -> Color(0xFF30D158)
                ToastType.ERROR -> Color(0xFFFF453A)
            }
            val textColor = when (d.type) {
                ToastType.SUCCESS -> Color(0xFF30D158)
                ToastType.ERROR -> Color(0xFFFF453A)
            }

            Surface(
                modifier = Modifier.padding(horizontal = 24.dp),
                shape = RoundedCornerShape(12.dp),
                color = bgColor,
                border = BorderStroke(1.dp, borderColor.copy(alpha = 0.5f)),
                shadowElevation = 8.dp
            ) {
                Text(
                    text = d.message,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
            }
        }
    }
}
