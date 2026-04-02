# Setup Wizard — полный бэкап кода и инструкций

> Дата: 2026-04-02
> Статус: отложено, есть 2 нерешённых бага (см. секцию "Известные баги")

---

## Архитектура

Global Overlay + State Machine. Визард НЕ route, а overlay поверх NavHost.
`CompositionLocalProvider(LocalSetupWizard)` прокидывает ViewModel во все экраны.
Якоря (`Modifier.wizardAnchor()`) регистрируют позиции элементов через `onGloballyPositioned`.
Auto-advance через наблюдение за Room Flow (программы, расписание, замеры).

5 шагов (~30 подшагов): Программы → Расписание → Антропометрия → Обзор тренировок → Dashboard+Статистика.
Spotlight (затемнение + вырез) для действий, Bubble для пояснений.

---

## Известные баги

### Баг 1: FeatureOnboarding не появляется после регистрации
- Вероятная причина: `hiltViewModel<SetupWizardViewModel>()` на уровне NavGraph может крашить Composable tree
- try-catch вокруг composable нельзя — ошибка компиляции
- Замена `mutableStateMapOf` на `ConcurrentHashMap + StateFlow` уже сделана (могло быть причиной)
- Нужно проверить, решилось ли

### Баг 2: Кнопка "Сгенерировать расписание" не работает
- Причина: observer настроек сравнивал `settings.id != initialSettingsId` — при REPLACE id не меняется
- Исправление: `drop(1)` в observer-ах (уже применено в коде ниже)
- Observer сессий тоже переведён на `drop(1)` + `sessions.isNotEmpty()`

---

## Новые файлы (пакет `ui/setupwizard/`)

### 1. SetupWizardStep.kt

```kotlin
package com.workouttracker.ui.setupwizard

sealed class SetupWizardStep {
    object Inactive : SetupWizardStep()

    // Step 1: Programs
    sealed class Programs : SetupWizardStep() {
        object Intro : Programs()
        object PromptCreateA : Programs()
        object WaitProgramA : Programs()
        object PromptAddExercisesA : Programs()
        object WaitExercisesA : Programs()
        object PromptCreateB : Programs()
        object WaitProgramB : Programs()
        object PromptAddExercisesB : Programs()
        object WaitExercisesB : Programs()
        object Done : Programs()
    }

    // Step 2: Schedule
    sealed class Schedule : SetupWizardStep() {
        object Intro : Schedule()
        object ExplainDays : Schedule()
        object ExplainCycle : Schedule()
        object ExplainPatterns : Schedule()
        object PromptSave : Schedule()
        object WaitSave : Schedule()
        object PromptGenerate : Schedule()
        object WaitGenerate : Schedule()
        object Done : Schedule()
    }

    // Step 3: Body Measurements
    sealed class Body : SetupWizardStep() {
        object Intro : Body()
        object PromptAdd : Body()
        object ExplainFields : Body()
        object WaitMeasurement : Body()
        object Done : Body()
    }

    // Step 4: Workout UI Tour (no real data entry)
    sealed class Workout : SetupWizardStep() {
        object NavigateToTab : Workout()
        object ExplainTab : Workout()
        object ExplainStart : Workout()
        object ExplainActiveUI : Workout()
        object Done : Workout()
    }

    // Step 5: Dashboard & Statistics Tour
    sealed class Tour : SetupWizardStep() {
        object NavigateDashboard : Tour()
        object ExplainNextWorkout : Tour()
        object ExplainMetrics : Tour()
        object NavigateStatistics : Tour()
        object ExplainCharts : Tour()
        object Complete : Tour()
    }

    val stepGroup: Int
        get() = when (this) {
            is Programs -> 1
            is Schedule -> 2
            is Body -> 3
            is Workout -> 4
            is Tour -> 5
            is Inactive -> 0
        }

    val serialKey: String
        get() = this::class.simpleName ?: "Inactive"

    companion object {
        const val TOTAL_GROUPS = 5
    }
}
```

### 2. SetupWizardAnchor.kt

```kotlin
package com.workouttracker.ui.setupwizard

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

enum class WizardAnchorId {
    PROGRAMS_FAB,
    PROGRAMS_ADD_EXERCISE,
    SCHEDULE_DAYS,
    SCHEDULE_CYCLE,
    SCHEDULE_PATTERNS,
    SCHEDULE_SAVE,
    SCHEDULE_GENERATE,
    BODY_FAB,
    WORKOUT_TODAY_CARD,
    WORKOUT_START_BUTTON,
    DASHBOARD_NEXT_WORKOUT,
    DASHBOARD_METRICS,
    STATISTICS_CHARTS
}

val LocalSetupWizard = staticCompositionLocalOf<SetupWizardViewModel?> { null }

@Composable
fun Modifier.wizardAnchor(id: WizardAnchorId): Modifier {
    val wizard = LocalSetupWizard.current
    val isActive = wizard?.isActive?.collectAsState()?.value ?: false

    // Always call DisposableEffect (same call count regardless of state)
    DisposableEffect(id, isActive) {
        onDispose {
            wizard?.unregisterAnchor(id)
        }
    }

    if (!isActive || wizard == null) return this

    return this.onGloballyPositioned { coordinates ->
        try {
            val bounds = coordinates.boundsInRoot()
            if (bounds.width > 0f && bounds.height > 0f) {
                wizard.registerAnchor(id, bounds)
            }
        } catch (_: Exception) {
            // Layout not yet attached to window
        }
    }
}
```

### 3. WizardTooltipConfig.kt

```kotlin
package com.workouttracker.ui.setupwizard

enum class TooltipStyle { SPOTLIGHT, BUBBLE }

data class WizardTooltipConfig(
    val text: String,
    val anchorId: WizardAnchorId?,
    val style: TooltipStyle,
    val hasNextButton: Boolean,
    val waitForDataChange: Boolean = false,
    val nextButtonText: String = "Далее"
)

fun getTooltipConfig(step: SetupWizardStep): WizardTooltipConfig? = when (step) {
    // --- Step 1: Programs ---
    is SetupWizardStep.Programs.Intro -> WizardTooltipConfig(
        text = "Давайте создадим программы тренировок!\nВам нужны программа A и программа B — они будут чередоваться.",
        anchorId = null,
        style = TooltipStyle.BUBBLE,
        hasNextButton = true
    )
    is SetupWizardStep.Programs.PromptCreateA -> WizardTooltipConfig(
        text = "Нажмите +, чтобы создать программу A",
        anchorId = WizardAnchorId.PROGRAMS_FAB,
        style = TooltipStyle.SPOTLIGHT,
        hasNextButton = false,
        waitForDataChange = true
    )
    is SetupWizardStep.Programs.WaitProgramA -> null
    is SetupWizardStep.Programs.PromptAddExercisesA -> WizardTooltipConfig(
        text = "Отлично! Теперь добавьте упражнения в программу A",
        anchorId = WizardAnchorId.PROGRAMS_ADD_EXERCISE,
        style = TooltipStyle.SPOTLIGHT,
        hasNextButton = false,
        waitForDataChange = true
    )
    is SetupWizardStep.Programs.WaitExercisesA -> null
    is SetupWizardStep.Programs.PromptCreateB -> WizardTooltipConfig(
        text = "Теперь создайте программу B.\nНажмите + ещё раз.",
        anchorId = WizardAnchorId.PROGRAMS_FAB,
        style = TooltipStyle.SPOTLIGHT,
        hasNextButton = false,
        waitForDataChange = true
    )
    is SetupWizardStep.Programs.WaitProgramB -> null
    is SetupWizardStep.Programs.PromptAddExercisesB -> WizardTooltipConfig(
        text = "Добавьте упражнения в программу B",
        anchorId = WizardAnchorId.PROGRAMS_ADD_EXERCISE,
        style = TooltipStyle.SPOTLIGHT,
        hasNextButton = false,
        waitForDataChange = true
    )
    is SetupWizardStep.Programs.WaitExercisesB -> null
    is SetupWizardStep.Programs.Done -> WizardTooltipConfig(
        text = "Отлично! Программы готовы.\nПереходим к настройке расписания.",
        anchorId = null,
        style = TooltipStyle.BUBBLE,
        hasNextButton = true
    )

    // --- Step 2: Schedule ---
    is SetupWizardStep.Schedule.Intro -> WizardTooltipConfig(
        text = "Настроим расписание тренировок.\nВыберите дни и распределите программы A/B.",
        anchorId = null,
        style = TooltipStyle.BUBBLE,
        hasNextButton = true
    )
    is SetupWizardStep.Schedule.ExplainDays -> WizardTooltipConfig(
        text = "Выберите дни, в которые вы тренируетесь",
        anchorId = WizardAnchorId.SCHEDULE_DAYS,
        style = TooltipStyle.SPOTLIGHT,
        hasNextButton = true
    )
    is SetupWizardStep.Schedule.ExplainCycle -> WizardTooltipConfig(
        text = "Длина цикла — сколько недель чередуются.\nОбычно 1 или 2 недели.",
        anchorId = WizardAnchorId.SCHEDULE_CYCLE,
        style = TooltipStyle.BUBBLE,
        hasNextButton = true
    )
    is SetupWizardStep.Schedule.ExplainPatterns -> WizardTooltipConfig(
        text = "Назначьте тип программы (A или B) на каждый тренировочный день",
        anchorId = WizardAnchorId.SCHEDULE_PATTERNS,
        style = TooltipStyle.BUBBLE,
        hasNextButton = true
    )
    is SetupWizardStep.Schedule.PromptSave -> WizardTooltipConfig(
        text = "Сохраните настройки расписания",
        anchorId = WizardAnchorId.SCHEDULE_SAVE,
        style = TooltipStyle.SPOTLIGHT,
        hasNextButton = false,
        waitForDataChange = true
    )
    is SetupWizardStep.Schedule.WaitSave -> null
    is SetupWizardStep.Schedule.PromptGenerate -> WizardTooltipConfig(
        text = "Теперь сгенерируйте расписание на 12 недель",
        anchorId = WizardAnchorId.SCHEDULE_GENERATE,
        style = TooltipStyle.SPOTLIGHT,
        hasNextButton = false,
        waitForDataChange = true
    )
    is SetupWizardStep.Schedule.WaitGenerate -> null
    is SetupWizardStep.Schedule.Done -> WizardTooltipConfig(
        text = "Расписание готово!\nПереходим к замерам тела.",
        anchorId = null,
        style = TooltipStyle.BUBBLE,
        hasNextButton = true
    )

    // --- Step 3: Body ---
    is SetupWizardStep.Body.Intro -> WizardTooltipConfig(
        text = "Внесём замеры тела.\nЭто нужно для расчёта прогресса и % жира.",
        anchorId = null,
        style = TooltipStyle.BUBBLE,
        hasNextButton = true
    )
    is SetupWizardStep.Body.PromptAdd -> WizardTooltipConfig(
        text = "Нажмите +, чтобы добавить первый замер",
        anchorId = WizardAnchorId.BODY_FAB,
        style = TooltipStyle.SPOTLIGHT,
        hasNextButton = false,
        waitForDataChange = true
    )
    is SetupWizardStep.Body.ExplainFields -> WizardTooltipConfig(
        text = "Вес и рост — обязательны.\nТалия и шея — для расчёта % жира по формуле US Navy.",
        anchorId = null,
        style = TooltipStyle.BUBBLE,
        hasNextButton = true
    )
    is SetupWizardStep.Body.WaitMeasurement -> null
    is SetupWizardStep.Body.Done -> WizardTooltipConfig(
        text = "Данные сохранены!\nТеперь посмотрим как работают тренировки.",
        anchorId = null,
        style = TooltipStyle.BUBBLE,
        hasNextButton = true
    )

    // --- Step 4: Workout Tour ---
    is SetupWizardStep.Workout.NavigateToTab -> null
    is SetupWizardStep.Workout.ExplainTab -> WizardTooltipConfig(
        text = "Здесь вы видите запланированные тренировки на сегодня и ближайшие дни",
        anchorId = WizardAnchorId.WORKOUT_TODAY_CARD,
        style = TooltipStyle.BUBBLE,
        hasNextButton = true
    )
    is SetupWizardStep.Workout.ExplainStart -> WizardTooltipConfig(
        text = "Нажмите «Начать», чтобы запустить тренировку",
        anchorId = WizardAnchorId.WORKOUT_START_BUTTON,
        style = TooltipStyle.SPOTLIGHT,
        hasNextButton = true
    )
    is SetupWizardStep.Workout.ExplainActiveUI -> WizardTooltipConfig(
        text = "В тренировке вы вводите вес, повторения и RIR для каждого подхода.\nТаймер отдыха напомнит вибрацией.",
        anchorId = null,
        style = TooltipStyle.BUBBLE,
        hasNextButton = true
    )
    is SetupWizardStep.Workout.Done -> null

    // --- Step 5: Dashboard & Statistics Tour ---
    is SetupWizardStep.Tour.NavigateDashboard -> null
    is SetupWizardStep.Tour.ExplainNextWorkout -> WizardTooltipConfig(
        text = "Карточка следующей тренировки — покажет дату, тип и упражнения",
        anchorId = WizardAnchorId.DASHBOARD_NEXT_WORKOUT,
        style = TooltipStyle.BUBBLE,
        hasNextButton = true
    )
    is SetupWizardStep.Tour.ExplainMetrics -> WizardTooltipConfig(
        text = "Метрики: вес тела, % жира (формула US Navy),\nтоннаж, средний RIR, объём тренировок",
        anchorId = WizardAnchorId.DASHBOARD_METRICS,
        style = TooltipStyle.BUBBLE,
        hasNextButton = true
    )
    is SetupWizardStep.Tour.NavigateStatistics -> null
    is SetupWizardStep.Tour.ExplainCharts -> WizardTooltipConfig(
        text = "Графики: тоннаж по тренировкам, объём по неделям,\ne1RM упражнений, вес тела, % жира",
        anchorId = WizardAnchorId.STATISTICS_CHARTS,
        style = TooltipStyle.BUBBLE,
        hasNextButton = true
    )
    is SetupWizardStep.Tour.Complete -> WizardTooltipConfig(
        text = "Настройка завершена!\nВы готовы к тренировкам.",
        anchorId = null,
        style = TooltipStyle.BUBBLE,
        hasNextButton = true,
        nextButtonText = "Готово"
    )

    is SetupWizardStep.Inactive -> null
}
```

### 4. SetupWizardViewModel.kt

```kotlin
package com.workouttracker.ui.setupwizard

import android.app.Application
import android.content.Context
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.workouttracker.data.repository.BodyTrackerRepository
import com.workouttracker.data.repository.ProgramRepository
import com.workouttracker.data.repository.ScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

sealed class WizardNavEvent {
    data class NavigateTo(val route: String) : WizardNavEvent()
    data class NavigateToTab(val route: String) : WizardNavEvent()
}

@HiltViewModel
class SetupWizardViewModel @Inject constructor(
    application: Application,
    private val programRepository: ProgramRepository,
    private val scheduleRepository: ScheduleRepository,
    private val bodyTrackerRepository: BodyTrackerRepository
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("workout_prefs", Context.MODE_PRIVATE)

    private val _currentStep = MutableStateFlow<SetupWizardStep>(SetupWizardStep.Inactive)
    val currentStep: StateFlow<SetupWizardStep> = _currentStep.asStateFlow()

    val isActive: StateFlow<Boolean> = _currentStep.map { it !is SetupWizardStep.Inactive }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Thread-safe backing map + StateFlow for Compose recomposition
    private val _anchors = ConcurrentHashMap<WizardAnchorId, Rect>()
    private val _anchorVersion = MutableStateFlow(0L)
    val anchorVersion: StateFlow<Long> = _anchorVersion.asStateFlow()

    fun getAnchorRect(id: WizardAnchorId): Rect? = _anchors[id]

    private val _navigationEvents = Channel<WizardNavEvent>(Channel.BUFFERED)
    val navigationEvents = _navigationEvents.receiveAsFlow()

    // Track data for auto-advance
    private var programAId: Long? = null
    private var programBId: Long? = null
    private var initialMeasurementCount = 0

    fun startWizard() {
        viewModelScope.launch {
            val programs = programRepository.getAllPrograms().first()
            programAId = programs.find { it.type == "A" }?.id
            programBId = programs.find { it.type == "B" }?.id
            initialMeasurementCount = bodyTrackerRepository.getAll().first().size

            _currentStep.value = SetupWizardStep.Programs.Intro
            _navigationEvents.send(WizardNavEvent.NavigateTo("programs"))
            observeDataChanges()
        }
    }

    fun skipWizard() {
        prefs.edit().putBoolean("setup_wizard_completed", true).apply()
        _currentStep.value = SetupWizardStep.Inactive
        viewModelScope.launch {
            _navigationEvents.send(WizardNavEvent.NavigateToTab("dashboard"))
        }
    }

    fun nextStep() {
        viewModelScope.launch {
            val next = getNextStep(_currentStep.value)
            if (next != null) {
                handleStepTransition(next)
            }
        }
    }

    fun registerAnchor(id: WizardAnchorId, rect: Rect) {
        _anchors[id] = rect
        _anchorVersion.value++
    }

    fun unregisterAnchor(id: WizardAnchorId) {
        _anchors.remove(id)
        _anchorVersion.value++
    }

    private suspend fun handleStepTransition(step: SetupWizardStep) {
        when (step) {
            is SetupWizardStep.Programs.Intro,
            is SetupWizardStep.Programs.PromptCreateA -> {
                _currentStep.value = step
                _navigationEvents.send(WizardNavEvent.NavigateTo("programs"))
            }
            is SetupWizardStep.Programs.PromptAddExercisesA,
            is SetupWizardStep.Programs.PromptAddExercisesB -> {
                _currentStep.value = step
                delay(500)
                _navigationEvents.send(WizardNavEvent.NavigateTo("programs"))
            }
            is SetupWizardStep.Programs.PromptCreateB -> {
                _currentStep.value = step
                delay(300)
                _navigationEvents.send(WizardNavEvent.NavigateTo("programs"))
            }
            is SetupWizardStep.Schedule.Intro -> {
                _currentStep.value = step
                _navigationEvents.send(WizardNavEvent.NavigateTo("schedule_settings"))
            }
            is SetupWizardStep.Body.Intro -> {
                _currentStep.value = step
                _navigationEvents.send(WizardNavEvent.NavigateToTab("body"))
            }
            is SetupWizardStep.Workout.NavigateToTab -> {
                _currentStep.value = SetupWizardStep.Workout.ExplainTab
                _navigationEvents.send(WizardNavEvent.NavigateToTab("workout_tab"))
            }
            is SetupWizardStep.Tour.NavigateDashboard -> {
                _currentStep.value = SetupWizardStep.Tour.ExplainNextWorkout
                _navigationEvents.send(WizardNavEvent.NavigateToTab("dashboard"))
            }
            is SetupWizardStep.Tour.NavigateStatistics -> {
                _currentStep.value = SetupWizardStep.Tour.ExplainCharts
                _navigationEvents.send(WizardNavEvent.NavigateToTab("statistics"))
            }
            is SetupWizardStep.Tour.Complete -> {
                prefs.edit().putBoolean("setup_wizard_completed", true).apply()
                _currentStep.value = SetupWizardStep.Inactive
                _navigationEvents.send(WizardNavEvent.NavigateToTab("dashboard"))
                return
            }
            else -> {
                _currentStep.value = step
            }
        }
    }

    private fun getNextStep(current: SetupWizardStep): SetupWizardStep? = when (current) {
        is SetupWizardStep.Programs.Intro -> SetupWizardStep.Programs.PromptCreateA
        is SetupWizardStep.Programs.PromptCreateA -> SetupWizardStep.Programs.WaitProgramA
        is SetupWizardStep.Programs.WaitProgramA -> SetupWizardStep.Programs.PromptAddExercisesA
        is SetupWizardStep.Programs.PromptAddExercisesA -> SetupWizardStep.Programs.WaitExercisesA
        is SetupWizardStep.Programs.WaitExercisesA -> SetupWizardStep.Programs.PromptCreateB
        is SetupWizardStep.Programs.PromptCreateB -> SetupWizardStep.Programs.WaitProgramB
        is SetupWizardStep.Programs.WaitProgramB -> SetupWizardStep.Programs.PromptAddExercisesB
        is SetupWizardStep.Programs.PromptAddExercisesB -> SetupWizardStep.Programs.WaitExercisesB
        is SetupWizardStep.Programs.WaitExercisesB -> SetupWizardStep.Programs.Done
        is SetupWizardStep.Programs.Done -> SetupWizardStep.Schedule.Intro

        is SetupWizardStep.Schedule.Intro -> SetupWizardStep.Schedule.ExplainDays
        is SetupWizardStep.Schedule.ExplainDays -> SetupWizardStep.Schedule.ExplainCycle
        is SetupWizardStep.Schedule.ExplainCycle -> SetupWizardStep.Schedule.ExplainPatterns
        is SetupWizardStep.Schedule.ExplainPatterns -> SetupWizardStep.Schedule.PromptSave
        is SetupWizardStep.Schedule.PromptSave -> SetupWizardStep.Schedule.WaitSave
        is SetupWizardStep.Schedule.WaitSave -> SetupWizardStep.Schedule.PromptGenerate
        is SetupWizardStep.Schedule.PromptGenerate -> SetupWizardStep.Schedule.WaitGenerate
        is SetupWizardStep.Schedule.WaitGenerate -> SetupWizardStep.Schedule.Done
        is SetupWizardStep.Schedule.Done -> SetupWizardStep.Body.Intro

        is SetupWizardStep.Body.Intro -> SetupWizardStep.Body.PromptAdd
        is SetupWizardStep.Body.PromptAdd -> SetupWizardStep.Body.ExplainFields
        is SetupWizardStep.Body.ExplainFields -> SetupWizardStep.Body.WaitMeasurement
        is SetupWizardStep.Body.WaitMeasurement -> SetupWizardStep.Body.Done
        is SetupWizardStep.Body.Done -> SetupWizardStep.Workout.NavigateToTab

        is SetupWizardStep.Workout.NavigateToTab -> SetupWizardStep.Workout.ExplainTab
        is SetupWizardStep.Workout.ExplainTab -> SetupWizardStep.Workout.ExplainStart
        is SetupWizardStep.Workout.ExplainStart -> SetupWizardStep.Workout.ExplainActiveUI
        is SetupWizardStep.Workout.ExplainActiveUI -> SetupWizardStep.Workout.Done
        is SetupWizardStep.Workout.Done -> SetupWizardStep.Tour.NavigateDashboard

        is SetupWizardStep.Tour.NavigateDashboard -> SetupWizardStep.Tour.ExplainNextWorkout
        is SetupWizardStep.Tour.ExplainNextWorkout -> SetupWizardStep.Tour.ExplainMetrics
        is SetupWizardStep.Tour.ExplainMetrics -> SetupWizardStep.Tour.NavigateStatistics
        is SetupWizardStep.Tour.NavigateStatistics -> SetupWizardStep.Tour.ExplainCharts
        is SetupWizardStep.Tour.ExplainCharts -> SetupWizardStep.Tour.Complete
        is SetupWizardStep.Tour.Complete -> null
        is SetupWizardStep.Inactive -> null
    }

    private fun observeDataChanges() {
        // Watch for program A creation
        viewModelScope.launch {
            programRepository.getAllPrograms().collect { programs ->
                val step = _currentStep.value
                val progA = programs.find { it.type == "A" }
                val progB = programs.find { it.type == "B" }

                when (step) {
                    is SetupWizardStep.Programs.PromptCreateA,
                    is SetupWizardStep.Programs.WaitProgramA -> {
                        if (progA != null && progA.id != programAId) {
                            programAId = progA.id
                            handleStepTransition(SetupWizardStep.Programs.PromptAddExercisesA)
                        }
                    }
                    is SetupWizardStep.Programs.PromptCreateB,
                    is SetupWizardStep.Programs.WaitProgramB -> {
                        if (progB != null && progB.id != programBId) {
                            programBId = progB.id
                            handleStepTransition(SetupWizardStep.Programs.PromptAddExercisesB)
                        }
                    }
                    else -> { /* no-op */ }
                }
            }
        }

        // Watch for exercises added to program A
        viewModelScope.launch {
            programRepository.getAllPrograms().flatMapLatest { programs ->
                val progA = programs.find { it.type == "A" }
                if (progA != null) programRepository.getExercisesByProgram(progA.id)
                else flowOf(emptyList())
            }.collect { exercises ->
                val step = _currentStep.value
                if ((step is SetupWizardStep.Programs.PromptAddExercisesA ||
                            step is SetupWizardStep.Programs.WaitExercisesA) && exercises.isNotEmpty()
                ) {
                    handleStepTransition(SetupWizardStep.Programs.PromptCreateB)
                }
            }
        }

        // Watch for exercises added to program B
        viewModelScope.launch {
            programRepository.getAllPrograms().flatMapLatest { programs ->
                val progB = programs.find { it.type == "B" }
                if (progB != null) programRepository.getExercisesByProgram(progB.id)
                else flowOf(emptyList())
            }.collect { exercises ->
                val step = _currentStep.value
                if ((step is SetupWizardStep.Programs.PromptAddExercisesB ||
                            step is SetupWizardStep.Programs.WaitExercisesB) && exercises.isNotEmpty()
                ) {
                    handleStepTransition(SetupWizardStep.Programs.Done)
                }
            }
        }

        // Watch for schedule settings save — drop(1) skips initial emission
        viewModelScope.launch {
            scheduleRepository.getSettings().drop(1).collect { settings ->
                val step = _currentStep.value
                if ((step is SetupWizardStep.Schedule.PromptSave ||
                            step is SetupWizardStep.Schedule.WaitSave) &&
                    settings != null
                ) {
                    handleStepTransition(SetupWizardStep.Schedule.PromptGenerate)
                }
            }
        }

        // Watch for sessions generated — drop(1) skips initial emission
        viewModelScope.launch {
            scheduleRepository.getAllSessions().drop(1).collect { sessions ->
                val step = _currentStep.value
                if ((step is SetupWizardStep.Schedule.PromptGenerate ||
                            step is SetupWizardStep.Schedule.WaitGenerate) &&
                    sessions.isNotEmpty()
                ) {
                    handleStepTransition(SetupWizardStep.Schedule.Done)
                }
            }
        }

        // Watch for body measurements
        viewModelScope.launch {
            bodyTrackerRepository.getAll().collect { measurements ->
                val step = _currentStep.value
                if ((step is SetupWizardStep.Body.PromptAdd ||
                            step is SetupWizardStep.Body.ExplainFields ||
                            step is SetupWizardStep.Body.WaitMeasurement) &&
                    measurements.size > initialMeasurementCount
                ) {
                    handleStepTransition(SetupWizardStep.Body.Done)
                }
            }
        }
    }
}
```

### 5. SetupWizardOverlay.kt

```kotlin
package com.workouttracker.ui.setupwizard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.workouttracker.ui.theme.*

private val SPOTLIGHT_PADDING = 8.dp
private val TOOLTIP_WIDTH = 280.dp

@Composable
fun SetupWizardOverlay(viewModel: SetupWizardViewModel) {
    val step by viewModel.currentStep.collectAsState()
    val config = getTooltipConfig(step) ?: return

    val density = LocalDensity.current
    val screenHeight = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val screenWidth = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }

    // Read anchor version to trigger recomposition when anchors change
    val anchorVersion by viewModel.anchorVersion.collectAsState()
    val anchorRect = config.anchorId?.let { viewModel.getAnchorRect(it) }
    @Suppress("UNUSED_EXPRESSION")
    anchorVersion
    val spotlightPaddingPx = with(density) { SPOTLIGHT_PADDING.toPx() }

    // If step waits for data change AND anchor is missing -> user navigated away
    if (config.waitForDataChange && config.anchorId != null && anchorRect == null) {
        return
    }

    val showSpotlight = config.style == TooltipStyle.SPOTLIGHT && anchorRect != null
    val forceNextButton = config.style == TooltipStyle.SPOTLIGHT && anchorRect == null

    Box(modifier = Modifier.fillMaxSize()) {
        if (showSpotlight) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            ) {
                drawRect(Color.Black.copy(alpha = 0.55f))

                val expanded = Rect(
                    left = anchorRect!!.left - spotlightPaddingPx,
                    top = anchorRect.top - spotlightPaddingPx,
                    right = anchorRect.right + spotlightPaddingPx,
                    bottom = anchorRect.bottom + spotlightPaddingPx
                )
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(expanded.left, expanded.top),
                    size = Size(expanded.width, expanded.height),
                    cornerRadius = CornerRadius(16f, 16f),
                    blendMode = BlendMode.Clear
                )
            }
        }

        TooltipBubble(
            config = config,
            anchorRect = anchorRect,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            stepGroup = step.stepGroup,
            forceNextButton = forceNextButton,
            onNext = { viewModel.nextStep() },
            onSkip = { viewModel.skipWizard() }
        )
    }
}

@Composable
private fun BoxScope.TooltipBubble(
    config: WizardTooltipConfig,
    anchorRect: Rect?,
    screenWidth: Float,
    screenHeight: Float,
    stepGroup: Int,
    forceNextButton: Boolean,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    val density = LocalDensity.current
    val tooltipWidthPx = with(density) { TOOLTIP_WIDTH.toPx() }

    val showBelow = if (anchorRect != null) {
        anchorRect.center.y < screenHeight / 2
    } else true

    val offsetX = if (anchorRect != null) {
        val centerX = anchorRect.center.x
        val halfWidth = tooltipWidthPx / 2
        val x = (centerX - halfWidth).coerceIn(
            with(density) { 16.dp.toPx() },
            screenWidth - tooltipWidthPx - with(density) { 16.dp.toPx() }
        )
        with(density) { x.toDp() }
    } else 0.dp

    val offsetY = if (anchorRect != null) {
        val gap = with(density) { SPOTLIGHT_PADDING.toPx() + 12.dp.toPx() }
        if (showBelow) {
            with(density) { (anchorRect.bottom + gap).toDp() }
        } else {
            with(density) { (anchorRect.top - gap - 180.dp.toPx()).coerceAtLeast(16.dp.toPx()).toDp() }
        }
    } else 0.dp

    val tooltipModifier = if (anchorRect != null) {
        Modifier
            .offset(x = offsetX, y = offsetY)
            .width(TOOLTIP_WIDTH)
    } else {
        Modifier
            .align(Alignment.Center)
            .width(TOOLTIP_WIDTH)
    }

    val showNextButton = config.hasNextButton || forceNextButton

    Surface(
        modifier = tooltipModifier,
        shape = RoundedCornerShape(12.dp),
        color = ColorSurface,
        border = BorderStroke(1.dp, ColorPrimary),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Шаг $stepGroup из ${SetupWizardStep.TOTAL_GROUPS}",
                style = MaterialTheme.typography.labelSmall,
                color = ColorPrimary
            )

            Text(
                text = config.text,
                style = MaterialTheme.typography.bodyMedium,
                color = ColorOnBackground,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onSkip,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "Пропустить",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorOnSurface
                    )
                }

                if (showNextButton) {
                    Button(
                        onClick = onNext,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(
                            config.nextButtonText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
```

---

## Изменения в существующих файлах (дифы)

### NavGraph.kt

Добавить:
- `import androidx.hilt.navigation.compose.hiltViewModel`
- `import com.workouttracker.ui.setupwizard.*`
- `val setupWizardVm: SetupWizardViewModel = hiltViewModel()` перед CompositionLocalProvider
- `LocalSetupWizard provides setupWizardVm` в CompositionLocalProvider
- LaunchedEffect для сбора navigationEvents (NavigateTo / NavigateToTab)
- `onStartSetup` callback в FeatureOnboardingScreen: marks seen, navigates to dashboard, calls startWizard()
- SetupWizardOverlay после TopToastHost (внутри Box)

### FeatureOnboardingScreen.kt

- Добавить параметр `onStartSetup: () -> Unit = {}`
- Добавить `import androidx.compose.foundation.BorderStroke`
- Последний слайд: Column с "Выполнить настройку" (Button, primary) + "Начать без настройки" (OutlinedButton)
- Не-последние слайды: Row с "Назад" + "Далее" (без изменений логики)

### Якоря на экранах

| Файл | Элемент | Якорь | Как добавить |
|------|---------|-------|-------------|
| ProgramsScreen.kt | FloatingActionButton | PROGRAMS_FAB | `modifier = Modifier.wizardAnchor(WizardAnchorId.PROGRAMS_FAB)` |
| ProgramsScreen.kt | TextButton "+Добавить" | PROGRAMS_ADD_EXERCISE | `modifier = Modifier.wizardAnchor(WizardAnchorId.PROGRAMS_ADD_EXERCISE)` |
| ScheduleSettingsScreen.kt | Row дней | SCHEDULE_DAYS | `.wizardAnchor(WizardAnchorId.SCHEDULE_DAYS)` к modifier Row |
| ScheduleSettingsScreen.kt | Slider | SCHEDULE_CYCLE | `modifier = Modifier.wizardAnchor(WizardAnchorId.SCHEDULE_CYCLE)` |
| ScheduleSettingsScreen.kt | Text "Программа по неделям" | SCHEDULE_PATTERNS | `modifier = Modifier.wizardAnchor(WizardAnchorId.SCHEDULE_PATTERNS)` |
| ScheduleSettingsScreen.kt | Button "Сохранить" | SCHEDULE_SAVE | `.wizardAnchor(WizardAnchorId.SCHEDULE_SAVE)` к modifier |
| ScheduleSettingsScreen.kt | OutlinedButton "Сгенерировать" | SCHEDULE_GENERATE | `.wizardAnchor(WizardAnchorId.SCHEDULE_GENERATE)` к modifier |
| BodyTrackerScreen.kt | FloatingActionButton | BODY_FAB | `.wizardAnchor(WizardAnchorId.BODY_FAB)` к modifier |
| WorkoutTabScreen.kt | DarkCard сегодня | WORKOUT_TODAY_CARD | `.wizardAnchor(WizardAnchorId.WORKOUT_TODAY_CARD)` к modifier |
| WorkoutTabScreen.kt | Button "Начать" | WORKOUT_START_BUTTON | `.wizardAnchor(WizardAnchorId.WORKOUT_START_BUTTON)` к modifier |
| DashboardScreen.kt | DarkCard "Следующая" | DASHBOARD_NEXT_WORKOUT | `modifier = Modifier.wizardAnchor(...)` в DarkCard |
| DashboardScreen.kt | Text "Метрики" | DASHBOARD_METRICS | `modifier = Modifier.wizardAnchor(...)` |
| StatisticsScreen.kt | HorizontalPager | STATISTICS_CHARTS | `.wizardAnchor(WizardAnchorId.STATISTICS_CHARTS)` к modifier |

Все экраны требуют два импорта:
```kotlin
import com.workouttracker.ui.setupwizard.WizardAnchorId
import com.workouttracker.ui.setupwizard.wizardAnchor
```
