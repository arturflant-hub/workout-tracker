# Workout Tracker — Project Context

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 1.9.23 |
| UI | Jetpack Compose (Material 3) |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt 2.51.1 |
| Database | Room v3 (SQLite), 9 entities, 8 DAOs |
| Async | Coroutines + StateFlow |
| Navigation | Jetpack Navigation Compose 2.8.2 |
| Min SDK | 26 / Target SDK 34 / JVM 17 |
| Drag & Drop | `sh.calvin.reorderable:reorderable:2.4.3` |

## Package Structure

```
com.workouttracker/
├── data/
│   ├── db/
│   │   ├── entities/        # 9 Room entities
│   │   ├── dao/             # 8 DAOs
│   │   └── AppDatabase.kt   # v3, migrations 1→2→3
│   └── repository/          # SessionRepository, ProgramRepository,
│                            # ScheduleRepository, BodyTrackerRepository
├── domain/
│   ├── model/               # Recommendation, SessionSummary
│   └── usecase/             # ProgressionUseCase, BodyMetricsCalculator
├── di/
│   └── AppModule.kt         # Hilt bindings
└── ui/
    ├── navigation/NavGraph.kt
    ├── screens/             # 14 Compose screens
    ├── viewmodel/           # 11 ViewModels
    └── theme/Theme.kt       # Dark: bg #0F0F0F, surface #1C1C1E, primary #6C63FF
```

## DB Schema (Room v3)

- **users** — profile (weight units etc.)
- **workout_programs** — A/B program templates
- **program_exercises** — exercises per program (`orderIndex`, `startWeightNote`: "barbell"/"dumbbell")
- **schedule_settings** — bitmask training days, cycleLengthWeeks, startWeekType
- **week_patterns** — weekType (1|2) + dayOfWeek → programType
- **workout_sessions** — date, programType, status: PLANNED | IN_PROGRESS | DONE | SKIPPED
- **workout_session_exercises** — planned sets/reps/weight per session
- **workout_set_facts** — actual reps/weight/RIR per set
- **body_measurements** — weight, waist, neck, chest, hips, thigh, arm, height, age

## Navigation Graph

5 bottom tabs: Dashboard · Workout · Body · Statistics · Settings

Deep screens: Programs → ExerciseEdit, ScheduleSettings, WorkoutSession, History, Calendar, PlannedWorkout, **ActiveWorkout**, **WorkoutDetail**

## Domain Logic

### BodyMetricsCalculator (`domain/usecase/`)
Single source of truth — use everywhere, no duplication in ViewModels:
```kotlin
bodyFatNavy(waist, neck, height)  // US Navy log10 formula
e1rm(weight, reps)                // Epley: weight × (1 + reps/30)
tonnage(weight, reps)             // weight × reps
```

### ProgressionUseCase — RIR-aware logic (matches spreadsheet)
- `avgRIR ≤ 1` + reps below minReps → decrease weight
- all sets hit `maxReps` + `avgRIR ≤ 2` → increase +2.5 kg (barbell) / +1 kg (dumbbell)
- `avgRIR ≥ 3` → too easy, bump weight or reps
- else → stay the course (with SLOW_NEGATIVE / ADD_PAUSE tips)

### Scheduling
- 2-week cycle with week patterns (bitmask days)
- Auto-generates 12 weeks ahead
- `ensureTodaySessionIfScheduled()` auto-creates today's session on app launch

## Implemented Features

### Feature 1 — Drag & Drop in Programs
`ProgramsScreen.kt`: `ReorderableItem` + `rememberReorderableLazyListState` + `draggableHandle()`.
Order persisted via `ProgramsViewModel.persistExerciseOrder()` → `ProgramRepository.updateExerciseOrder()`.

### Feature 2 — Smart Workout Tab
`WorkoutTabScreen.kt`: shows today's session (with Start/View buttons) OR nearest upcoming OR rest-day message.
States: active session → today planned → nearest upcoming → empty.

### Feature 3 — Workout Detail Screen
`WorkoutDetailScreen.kt` + `WorkoutDetailViewModel.kt`:
- Loads current session + previous session of same type (A/A or B/B comparison)
- e1RM (Epley), tonnage, reps comparison per exercise
- **MiniSparkline** — last 6 sessions trend chart (tonnage / e1RM / reps); fallback to prev/current bar
- "Start Workout" button only if today + PLANNED status

### Feature 4 — Active Workout
`ActiveWorkoutScreen.kt` + `ActiveWorkoutViewModel.kt`:
- Workout elapsed timer (pause/resume)
- Rest timer (default 90s, configurable, vibration on finish)
- Per-set input: weight, reps, RIR
- Exercise detail dialog
- Complete → confirm dialog → `completeSession()` → Room

### Feature 5 — Dashboard
`DashboardScreen.kt` + `DashboardViewModel.kt`:
- Next workout card (clickable → WorkoutDetail)
- Last completed workout card (clickable → WorkoutDetail) with tonnage
- 7 metrics from Room: weight, % fat (Navy), Δwaist, workout count, total tonnage, avg volume, avg RIR

### Feature 6 — Statistics
`StatisticsScreen.kt` + `StatisticsViewModel.kt`:
- **TypedBarChart** — tonnage per session, colored by type A (purple #6C63FF) / B (green #30D158)
- **Weekly volume bar chart** — aggregated by calendar week
- **Exercise e1RM** — line chart with exercise dropdown (all ever-performed exercises)
- Body weight line chart
- Body fat % line chart (Navy formula)

## Правило самопроверки

**После каждого изменения кода — обязательно перепроверить свою работу:**
1. Перечитать изменённые файлы и убедиться в корректности синтаксиса Kotlin
2. Проверить: нет ли `return` внутри expression body (`= ...`)
3. Проверить: все новые символы (классы, функции, переменные) импортированы
4. Проверить: типы совпадают (особенно `Float`/`Double`, `Long`/`Int`)
5. Если найдена ошибка — исправить до коммита, не оставлять на потом

## Правило ведения истории

**После каждого изменения в проекте — обновить этот файл:**
- Добавить строку в таблицу `## История изменений` (дата, что сделано, затронутые файлы)
- Если добавлена новая фича или изменена архитектура — обновить соответствующий раздел выше
- Это правило распространяется на Claude и на любого разработчика

## Key Architectural Rules

- Business logic only in `domain/` — NOT in ViewModel
- All body fat / e1RM calculations → `BodyMetricsCalculator`
- Room schema changes → write `Migration`, never `fallbackToDestructiveMigration()`
- Repositories return data or null/empty — no raw exceptions to UI
- `StateFlow` everywhere, no `LiveData`
- Date formatting → `SimpleDateFormat` with `Locale("ru")`
- Design: dark bg `#0F0F0F`, surface `#1C1C1E`, primary `#6C63FF`, secondary `#30D158`, rounded corners 12-16dp

## История изменений

| Дата | Commit | Что сделано | Файлы |
|------|--------|-------------|-------|
| 2026-03-24 | feat/ver2 | feat: кнопка "Сохранить изменения" в диалоге упражнения — появляется когда данные сетов изменились относительно состояния при открытии, сохраняет все сеты в БД | ActiveWorkoutScreen.kt, ActiveWorkoutViewModel.kt |
| 2026-03-24 | feat/ver2 | fix: "Последняя тренировка" — тоннаж суммируется по всем сессиям за последний тренировочный день, показывает "N тренировки за день" если > 1 | DashboardViewModel.kt, DashboardScreen.kt |
| 2026-03-24 | feat/ver2 | fix: карточка рекомендации в истории — заголовок + явные метки (↑ Увеличить вес / ↓ Снизить вес / → Добавить повторения) вместо непонятных эмодзи | StatisticsScreen.kt |
| 2026-03-25 | — | fix: revert enableEdgeToEdge (ломало AppBar), adjustResize в Manifest, удаление программ, тост в дебаг меню | MainActivity.kt, AndroidManifest.xml, ProgramsScreen.kt, SettingsScreen.kt |
| 2026-03-25 | — | fix: 6 багфиксов — редактирование замеров тела, кнопка «Назад» в Программах, добавление/отмена подходов в активной тренировке, автовыбор таба новой программы, IME padding (кнопки над клавиатурой), моковые данные в дебаг меню | BodyTrackerScreen.kt, BodyTrackerViewModel.kt, ProgramsScreen.kt, ProgramsViewModel.kt, ActiveWorkoutScreen.kt, ActiveWorkoutViewModel.kt, MainActivity.kt, DevToolsViewModel.kt, SettingsScreen.kt, SessionRepository.kt |
| 2026-03-24 | feat/ver2 | feat: все сегодняшние тренировки в WorkoutTab, тренды (стрелки ↑↓) в метриках Dashboard, подтаб "По тренировкам/упражнениям" в History, bodyFatChange в DashboardState | WorkoutTabScreen.kt, DashboardScreen.kt, DashboardViewModel.kt, StatisticsScreen.kt, HistoryViewModel.kt |
| 2026-03-24 | feat/ver2 | refactor: UX рефакторинг всего приложения — тёмные фильтр-чипы, новые заголовки, пустые состояния с эмодзи, OutlinedButton/ChevronRight навигация, удалены лишние иконки из TopBar | DashboardScreen.kt, WorkoutTabScreen.kt, BodyTrackerScreen.kt, ProgramsScreen.kt, HistoryScreen.kt |
| 2026-03-24 | feat/ver2 | feat: Статистика переработана в 3 таба (Графики / Календарь / История) — месячный календарь, история упражнений с датами и RIR, тёмная тема | StatisticsScreen.kt, HistoryViewModel.kt, HistoryScreen.kt, NavGraph.kt |
| 2026-03-24 | feat/ver2 | feat: меню разработчика (4 тапа по версии) в настройках — сброс тренировок/расписания/программ/антропометрии/всего; версия приложения в настройках | SettingsScreen.kt, DevToolsViewModel.kt, все DAO (deleteAll), все репозитории (reset методы) |
| 2026-03-24 | feat/ver2 | feat: RIR=2 в блоке «ЦЕЛЬ СЕГОДНЯ», комментарии к упражнениям (с историей), режим редактирования DONE тренировки, кнопка «Редактировать» в WorkoutTab, баг createQuickSession | ActiveWorkoutScreen.kt, ActiveWorkoutViewModel.kt, WorkoutTabScreen.kt, ScheduleRepository.kt |
| 2026-03-24 | feat/ver2 | feat: блок «ЦЕЛЬ СЕГОДНЯ» в диалоге упражнения — вес и повторы для текущей тренировки, предзаполнение сетов рекомендуемыми значениями, targetRepsMin/Max по RIR | Recommendation.kt, ProgressionUseCase.kt, ActiveWorkoutViewModel.kt, ActiveWorkoutScreen.kt |
| 2026-03-24 | feat/ver2 | feat: рекомендации на карточках тренировки, исправлена логика прогрессии по training_logic.md, детектор плато, данные прошлой тренировки | Recommendation.kt, ProgressionUseCase.kt, SessionRepository.kt, ActiveWorkoutViewModel.kt, ActiveWorkoutScreen.kt |
| 2026-03-21 | cabef5a | fix: calcBodyFat — expression body с early return заменена на block body | StatisticsViewModel.kt |
|------|--------|-------------|-------|
| 2026-03-21 | d4a3afe | Добавлен BodyMetricsCalculator (domain), RIR-aware ProgressionUseCase, MiniSparkline в WorkoutDetail, TypedBarChart + weekly volume в Statistics | BodyMetricsCalculator.kt, ProgressionUseCase.kt, StatisticsScreen.kt, StatisticsViewModel.kt, WorkoutDetailScreen.kt, WorkoutDetailViewModel.kt, DashboardViewModel.kt |
| 2026-03-21 | 5b0d146 | Создан CLAUDE.md с контекстом проекта | CLAUDE.md |
| 2026-03-21 | 9d3e9e3 | Исправлены 3 критических бага: авто-создание сессии из расписания, anti-pattern Flow.collect, загрузка упражнений | ScheduleRepository.kt |
| 2026-03-21 | 36ca790 | Dashboard карточки с навигацией на WorkoutDetailScreen | DashboardScreen.kt, DashboardViewModel.kt |
| 2026-03-21 | 85b291f | Рефакторинг ActiveWorkoutScreen | ActiveWorkoutScreen.kt, ActiveWorkoutViewModel.kt |
| 2026-03-21 | 29288bc | WorkoutDetailScreen + WorkoutDetailViewModel | WorkoutDetailScreen.kt, WorkoutDetailViewModel.kt |
