package com.workouttracker.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.workouttracker.ui.theme.*

@Composable
fun IllustrationProgramsAB() {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) { anim.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
    val progress = anim.value

    Box(
        modifier = Modifier.size(220.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cardW = w * 0.35f
            val cardH = h * 0.55f
            val gap = w * 0.06f
            val leftX = w / 2 - cardW - gap / 2
            val rightX = w / 2 + gap / 2
            val topY = h * 0.18f

            // Card A
            drawRoundRect(
                color = ColorPrimary.copy(alpha = 0.15f * progress),
                topLeft = Offset(leftX, topY),
                size = Size(cardW, cardH * progress),
                cornerRadius = CornerRadius(16f)
            )
            drawRoundRect(
                color = ColorPrimary.copy(alpha = progress),
                topLeft = Offset(leftX, topY),
                size = Size(cardW, cardH * progress),
                cornerRadius = CornerRadius(16f),
                style = Stroke(3f)
            )

            // Card B
            drawRoundRect(
                color = ColorSecondary.copy(alpha = 0.15f * progress),
                topLeft = Offset(rightX, topY),
                size = Size(cardW, cardH * progress),
                cornerRadius = CornerRadius(16f)
            )
            drawRoundRect(
                color = ColorSecondary.copy(alpha = progress),
                topLeft = Offset(rightX, topY),
                size = Size(cardW, cardH * progress),
                cornerRadius = CornerRadius(16f),
                style = Stroke(3f)
            )

            // Exercise lines in Card A
            val lineStartX = leftX + cardW * 0.15f
            val lineEndX = leftX + cardW * 0.85f
            for (i in 0..2) {
                val ly = topY + cardH * 0.3f + i * cardH * 0.18f
                if (ly < topY + cardH * progress) {
                    drawRoundRect(
                        color = ColorPrimary.copy(alpha = 0.6f * progress),
                        topLeft = Offset(lineStartX, ly),
                        size = Size(lineEndX - lineStartX, 6f),
                        cornerRadius = CornerRadius(3f)
                    )
                }
            }

            // Exercise lines in Card B
            val lineStartX2 = rightX + cardW * 0.15f
            val lineEndX2 = rightX + cardW * 0.85f
            for (i in 0..2) {
                val ly = topY + cardH * 0.3f + i * cardH * 0.18f
                if (ly < topY + cardH * progress) {
                    drawRoundRect(
                        color = ColorSecondary.copy(alpha = 0.6f * progress),
                        topLeft = Offset(lineStartX2, ly),
                        size = Size(lineEndX2 - lineStartX2, 6f),
                        cornerRadius = CornerRadius(3f)
                    )
                }
            }

            // Curved arrow A → B
            if (progress > 0.5f) {
                val arrowAlpha = ((progress - 0.5f) * 2f).coerceIn(0f, 1f)
                val arrowPath = Path().apply {
                    moveTo(leftX + cardW * 0.5f, topY + cardH * progress + 16f)
                    cubicTo(
                        leftX + cardW * 0.5f, topY + cardH * progress + 40f,
                        rightX + cardW * 0.5f, topY + cardH * progress + 40f,
                        rightX + cardW * 0.5f, topY + cardH * progress + 16f
                    )
                }
                drawPath(
                    arrowPath,
                    color = ColorOnSurface.copy(alpha = 0.5f * arrowAlpha),
                    style = Stroke(2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                // Arrowhead
                val ax = rightX + cardW * 0.5f
                val ay = topY + cardH * progress + 16f
                drawLine(ColorOnSurface.copy(alpha = 0.5f * arrowAlpha), Offset(ax - 6f, ay + 6f), Offset(ax, ay), 2.5f, StrokeCap.Round)
                drawLine(ColorOnSurface.copy(alpha = 0.5f * arrowAlpha), Offset(ax + 6f, ay + 6f), Offset(ax, ay), 2.5f, StrokeCap.Round)
            }
        }

        // Labels
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 46.dp),
            horizontalArrangement = Arrangement.spacedBy(48.dp)
        ) {
            Text("A", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ColorPrimary.copy(alpha = progress))
            Text("B", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ColorSecondary.copy(alpha = progress))
        }
    }
}

@Composable
fun IllustrationSmartSchedule() {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) { anim.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
    val progress = anim.value

    val trainingDays1 = listOf(true, false, true, false, true, false, false) // Week 1: Mon, Wed, Fri
    val trainingDays2 = listOf(false, true, false, true, false, false, false) // Week 2: Tue, Thu
    val dayLabels = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

    Box(
        modifier = Modifier.size(220.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Неделя 1", style = MaterialTheme.typography.labelSmall, color = ColorOnSurface.copy(alpha = progress))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                trainingDays1.forEachIndexed { i, isTraining ->
                    val delay = i * 60
                    val dotAnim = remember { Animatable(0f) }
                    LaunchedEffect(Unit) { dotAnim.animateTo(1f, tween(400, delayMillis = delay)) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(dayLabels[i], fontSize = 9.sp, color = ColorOnSurface.copy(alpha = 0.5f * dotAnim.value))
                        Spacer(Modifier.height(2.dp))
                        Surface(
                            modifier = Modifier.size(24.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isTraining) ColorPrimary.copy(alpha = dotAnim.value) else ColorSurface.copy(alpha = dotAnim.value),
                            border = BorderStroke(
                                1.dp,
                                if (isTraining) ColorPrimary.copy(alpha = dotAnim.value) else ColorSurfaceVariant.copy(alpha = dotAnim.value)
                            )
                        ) {}
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Text("Неделя 2", style = MaterialTheme.typography.labelSmall, color = ColorOnSurface.copy(alpha = progress))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                trainingDays2.forEachIndexed { i, isTraining ->
                    val delay = 420 + i * 60
                    val dotAnim = remember { Animatable(0f) }
                    LaunchedEffect(Unit) { dotAnim.animateTo(1f, tween(400, delayMillis = delay)) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(dayLabels[i], fontSize = 9.sp, color = ColorOnSurface.copy(alpha = 0.5f * dotAnim.value))
                        Spacer(Modifier.height(2.dp))
                        Surface(
                            modifier = Modifier.size(24.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isTraining) ColorSecondary.copy(alpha = dotAnim.value) else ColorSurface.copy(alpha = dotAnim.value),
                            border = BorderStroke(
                                1.dp,
                                if (isTraining) ColorSecondary.copy(alpha = dotAnim.value) else ColorSurfaceVariant.copy(alpha = dotAnim.value)
                            )
                        ) {}
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "12 недель вперёд",
                style = MaterialTheme.typography.labelMedium,
                color = ColorPrimary.copy(alpha = progress),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun IllustrationActiveWorkout() {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) { anim.animateTo(1f, tween(800, easing = FastOutSlowInEasing)) }
    val progress = anim.value

    Box(
        modifier = Modifier.size(220.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Timer circle
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 8f
                    val pad = stroke / 2
                    // Track
                    drawArc(
                        color = ColorSurfaceVariant,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(pad, pad),
                        size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                    // Progress arc (75% = 270 degrees)
                    drawArc(
                        color = ColorPrimary,
                        startAngle = -90f,
                        sweepAngle = 270f * progress,
                        useCenter = false,
                        topLeft = Offset(pad, pad),
                        size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
                Text(
                    "1:30",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorOnBackground.copy(alpha = progress)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Set chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(true, true, false).forEachIndexed { i, done ->
                    val chipAnim = remember { Animatable(0f) }
                    LaunchedEffect(Unit) { chipAnim.animateTo(1f, tween(300, delayMillis = 500 + i * 150)) }
                    Surface(
                        modifier = Modifier.size(width = 52.dp, height = 28.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = if (done) ColorSecondary.copy(alpha = 0.2f * chipAnim.value) else ColorSurface.copy(alpha = chipAnim.value),
                        border = BorderStroke(
                            1.dp,
                            if (done) ColorSecondary.copy(alpha = chipAnim.value) else ColorSurfaceVariant.copy(alpha = chipAnim.value)
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                if (done) "done" else "set ${i + 1}",
                                fontSize = 10.sp,
                                color = if (done) ColorSecondary.copy(alpha = chipAnim.value) else ColorOnSurface.copy(alpha = chipAnim.value)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IllustrationProgression() {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) { anim.animateTo(1f, tween(800, easing = FastOutSlowInEasing)) }
    val progress = anim.value

    Box(
        modifier = Modifier.size(220.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val padX = w * 0.15f
            val padY = h * 0.2f
            val chartW = w - padX * 2
            val chartH = h * 0.45f
            val startY = padY + chartH

            // Points going up
            val points = listOf(0.7f, 0.55f, 0.45f, 0.3f, 0.15f)
            val drawn = (points.size * progress).toInt().coerceAtMost(points.size)

            // Grid lines
            for (i in 0..3) {
                val gy = padY + chartH * i / 3
                drawLine(
                    ColorSurfaceVariant.copy(alpha = 0.3f * progress),
                    Offset(padX, gy),
                    Offset(padX + chartW, gy),
                    1f
                )
            }

            // Trend line
            if (drawn >= 2) {
                for (i in 0 until drawn - 1) {
                    val x1 = padX + chartW * i / (points.size - 1)
                    val y1 = padY + chartH * points[i]
                    val x2 = padX + chartW * (i + 1) / (points.size - 1)
                    val y2 = padY + chartH * points[i + 1]
                    drawLine(ColorSecondary, Offset(x1, y1), Offset(x2, y2), 3f, StrokeCap.Round)
                }
            }

            // Points
            for (i in 0 until drawn) {
                val x = padX + chartW * i / (points.size - 1)
                val y = padY + chartH * points[i]
                drawCircle(ColorSecondary, 6f, Offset(x, y))
                drawCircle(ColorBackground, 3f, Offset(x, y))
            }

            // Arrow up
            if (progress > 0.6f) {
                val arrowAlpha = ((progress - 0.6f) * 2.5f).coerceIn(0f, 1f)
                val ax = w * 0.82f
                val abottom = padY + chartH * 0.6f
                val atop = padY + chartH * 0.1f
                drawLine(ColorSecondary.copy(alpha = arrowAlpha), Offset(ax, abottom), Offset(ax, atop), 3f, StrokeCap.Round)
                drawLine(ColorSecondary.copy(alpha = arrowAlpha), Offset(ax - 8f, atop + 10f), Offset(ax, atop), 3f, StrokeCap.Round)
                drawLine(ColorSecondary.copy(alpha = arrowAlpha), Offset(ax + 8f, atop + 10f), Offset(ax, atop), 3f, StrokeCap.Round)
            }
        }

        // Weight chip
        if (progress > 0.7f) {
            val chipAlpha = ((progress - 0.7f) * 3.3f).coerceIn(0f, 1f)
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(10.dp),
                color = ColorPrimary.copy(alpha = 0.15f * chipAlpha),
                border = BorderStroke(1.dp, ColorPrimary.copy(alpha = 0.5f * chipAlpha))
            ) {
                Text(
                    "40 кг  →  42.5 кг",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorPrimary.copy(alpha = chipAlpha)
                )
            }
        }
    }
}

@Composable
fun IllustrationBodyStats() {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) { anim.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
    val progress = anim.value

    Box(
        modifier = Modifier.size(220.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val padX = w * 0.2f
            val barAreaW = w - padX * 2
            val barW = barAreaW / 6.5f
            val barMaxH = h * 0.45f
            val baseY = h * 0.7f

            val heights = listOf(0.5f, 0.65f, 0.55f, 0.8f, 0.7f, 0.95f)
            val colors = listOf(ColorPrimary, ColorSecondary, ColorPrimary, ColorSecondary, ColorPrimary, ColorSecondary)

            heights.forEachIndexed { i, ratio ->
                val delayedProgress = ((progress - i * 0.08f) / 0.5f).coerceIn(0f, 1f)
                val barH = barMaxH * ratio * delayedProgress
                val x = padX + i * (barW + barW * 0.3f)
                drawRoundRect(
                    color = colors[i].copy(alpha = 0.8f),
                    topLeft = Offset(x, baseY - barH),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(4f)
                )
            }

            // Baseline
            drawLine(
                ColorSurfaceVariant.copy(alpha = 0.5f * progress),
                Offset(padX - 8f, baseY),
                Offset(w - padX + 8f, baseY),
                1.5f
            )
        }

        // Metric labels
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendDot(ColorPrimary, "Тип A", progress)
                LegendDot(ColorSecondary, "Тип B", progress)
            }
        }
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String, alpha: Float) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            modifier = Modifier.size(8.dp),
            shape = RoundedCornerShape(4.dp),
            color = color.copy(alpha = alpha)
        ) {}
        Text(label, fontSize = 10.sp, color = ColorOnSurface.copy(alpha = alpha))
    }
}
