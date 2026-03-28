package com.workouttracker.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.workouttracker.ui.theme.*

private data class OnboardingStep(
    val title: String,
    val description: String,
    val illustration: @Composable () -> Unit
)

private val steps = listOf(
    OnboardingStep(
        title = "Программы A и B",
        description = "Создайте две чередующиеся программы тренировок.\nНастройте упражнения, подходы и вес под себя.",
        illustration = { IllustrationProgramsAB() }
    ),
    OnboardingStep(
        title = "Умное расписание",
        description = "Выберите дни тренировок — приложение создаст\nрасписание на 12 недель вперёд.",
        illustration = { IllustrationSmartSchedule() }
    ),
    OnboardingStep(
        title = "Тренировка с таймером",
        description = "Записывайте вес, повторения и RIR для каждого\nподхода. Таймер отдыха напомнит вибрацией.",
        illustration = { IllustrationActiveWorkout() }
    ),
    OnboardingStep(
        title = "Умная прогрессия",
        description = "Приложение подскажет когда увеличить вес\nили добавить повторения на основе RIR.",
        illustration = { IllustrationProgression() }
    ),
    OnboardingStep(
        title = "Замеры и статистика",
        description = "Отслеживайте вес тела и замеры.\nГрафики покажут ваш прогресс.",
        illustration = { IllustrationBodyStats() }
    )
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun FeatureOnboardingScreen(onFinish: () -> Unit) {
    var currentStep by remember { mutableIntStateOf(0) }
    val isLastStep = currentStep == steps.size - 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top bar: Skip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onFinish) {
                Text(
                    "Пропустить",
                    color = ColorOnSurface,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Dot indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(steps.size) { i ->
                val isActive = i == currentStep
                val width by animateDpAsState(
                    targetValue = if (isActive) 24.dp else 8.dp,
                    animationSpec = tween(300)
                )
                Surface(
                    modifier = Modifier.size(width = width, height = 4.dp),
                    shape = RoundedCornerShape(2.dp),
                    color = if (isActive) ColorPrimary else ColorSurfaceVariant
                ) {}
            }
        }

        // Illustration + Text with swipe and AnimatedContent
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { totalDrag = 0f },
                        onDragEnd = {
                            if (totalDrag < -80f && currentStep < steps.size - 1) {
                                currentStep++
                            } else if (totalDrag > 80f && currentStep > 0) {
                                currentStep--
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            totalDrag += dragAmount
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it / 2 } + fadeIn() togetherWith
                                slideOutHorizontally { -it / 2 } + fadeOut()
                    } else {
                        slideInHorizontally { -it / 2 } + fadeIn() togetherWith
                                slideOutHorizontally { it / 2 } + fadeOut()
                    }
                },
                label = "feature_step"
            ) { step ->
                val s = steps[step]
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Illustration
                    Box(
                        modifier = Modifier.padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        key(step) { s.illustration() }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Title
                    Text(
                        s.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = ColorOnBackground,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(12.dp))

                    // Description
                    Text(
                        s.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ColorOnSurface,
                        textAlign = TextAlign.Center,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                    )
                }
            }
        }

        // Bottom buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentStep > 0) {
                TextButton(
                    onClick = { currentStep-- },
                    modifier = Modifier.width(80.dp)
                ) {
                    Text("Назад", color = ColorOnSurface)
                }
            } else {
                Spacer(Modifier.width(80.dp))
            }

            Button(
                onClick = {
                    if (isLastStep) onFinish() else currentStep++
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary)
            ) {
                Text(
                    if (isLastStep) "Начать" else "Далее",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            // Balance spacer
            if (currentStep > 0) {
                Spacer(Modifier.width(0.dp))
            } else {
                Spacer(Modifier.width(0.dp))
            }
        }
    }
}
