# CLAUDE CLI — PROMPT
# Workout Tracker App (Kotlin/Android) — Architecture Review & Feature Implementation

---

## Роль и контекст

Ты — senior Android developer (Kotlin).
Перед тобой Android-приложение для трекинга тренировок.
Твоя задача: **изучить существующую архитектуру**, сразу внести улучшения и **реализовать список фич**, опираясь на логику из Google Таблицы пользователя.

**Не жди подтверждения между шагами. Анализируй и сразу пиши код.**

---

## Шаг 1 — Аудит репозитория

```bash
tree -L 4
cat app/build.gradle.kts 2>/dev/null || cat app/build.gradle
cat gradle/libs.versions.toml 2>/dev/null
```

Определи и зафиксируй в комментарии:
- Architecture pattern: MVI / MVVM / MVP / Clean Architecture
- DI: Hilt / Koin / manual
- Navigation: Jetpack Navigation / Compose Navigation / Fragment-based
- UI: Jetpack Compose / XML Views / смешанный
- State management: StateFlow / LiveData / другое
- Local DB: Room / SQLite / DataStore / SharedPreferences
- Все экраны и граф навигации
- Модели данных (data classes, Room entities, схема БД)
- Проблемы: смешение слоёв, хардкод, отсутствие error handling

---

## Шаг 2 — Парсинг логики из Google Таблицы

**Spreadsheet ID:** `1W8kMFbxeFH9QnzMosuw6O_4NhtcXfg4EIYe5i7LSTOs`

```bash
# Основной лист
curl -L "https://docs.google.com/spreadsheets/d/1W8kMFbxeFH9QnzMosuw6O_4NhtcXfg4EIYe5i7LSTOs/export?format=csv&gid=1806835647" -o sheet_main.csv
cat sheet_main.csv

# Дефолтный лист
curl -L "https://docs.google.com/spreadsheets/d/1W8kMFbxeFH9QnzMosuw6O_4NhtcXfg4EIYe5i7LSTOs/export?format=csv" -o sheet_default.csv
cat sheet_default.csv

# Перебрать другие листы
for gid in 0 1 2 3 4 5 6 7 8; do
  curl -sL "https://docs.google.com/spreadsheets/d/1W8kMFbxeFH9QnzMosuw6O_4NhtcXfg4EIYe5i7LSTOs/export?format=csv&gid=$gid" -o "sheet_gid$gid.csv"
  echo "=== gid=$gid ===" && head -5 "sheet_gid$gid.csv"
done
```

**Что обязательно извлечь:**
- Структура программ тренировок: тип А и тип Б, список упражнений, порядок
- Параметры упражнений: подходы, диапазон повторов, стартовый вес
- **Логика прогрессии нагрузки** — формула расчёта рекомендуемого веса для следующей тренировки
- **Расчёт объёма/тоннажа**: вес × подходы × повторы
- **Navy formula** для % жира
- Что на графиках (оси, агрегация по неделям/месяцам)
- Логика сравнения тренировка-к-тренировке (А с А, Б с Б)

Задокументируй логику как Kotlin data classes и функции в `domain/` слое перед написанием UI.

---

## Шаг 3 — Реализация фич

После каждой фичи: `git add -A && git commit -m "feat: <название>"`

---

### ФИЧА 1: Drag & Drop в списке упражнений программы

**Экран:** Настройки → Программы тренировок → список упражнений

- Если UI на **Compose**: добавить `sh.calvin.reorderable:reorderable:2.1.1`, использовать `ReorderableColumn` / `rememberReorderableLazyListState`
- Если UI на **XML/RecyclerView**: реализовать через `ItemTouchHelper.SimpleCallback`

Убедись что в Room entity упражнений есть поле `position: Int`. Если нет — добавь и напиши миграцию:
```kotlin
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE exercises ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
    }
}
```
Новый порядок сохранять при каждом drop-событии через DAO.

---

### ФИЧА 2: Умный экран "Тренировка"

**Экран:** WorkoutFragment / WorkoutScreen (таб "Тренировка")

Реализовать в ViewModel:

```kotlin
sealed class WorkoutScreenState {
    data class TodayWorkout(val plan: WorkoutPlan) : WorkoutScreenState()
    data class NextWorkout(val date: LocalDate, val plan: WorkoutPlan) : WorkoutScreenState()
    object NoWorkoutsScheduled : WorkoutScreenState()
}

fun loadState() {
    val today = LocalDate.now()
    val todayPlan = scheduleRepo.getForDate(today)
    _state.value = when {
        todayPlan != null -> TodayWorkout(todayPlan)
        else -> {
            val next = scheduleRepo.getNextAfter(today)
            if (next != null) NextWorkout(next.date, next.plan)
            else NoWorkoutsScheduled
        }
    }
}
```

UI:
- `TodayWorkout` → плашка с типом + кнопка "Начать тренировку" → навигация на ФИЧУ 4
- `NextWorkout` → плашка с датой и типом, кликабельна → навигация на ФИЧУ 3 (только просмотр)
- `NoWorkoutsScheduled` → текущее сообщение "Сегодня тренировок нет 🧘"

---

### ФИЧА 3: Экран детального просмотра тренировки

**Создать:** `WorkoutDetailFragment` / `WorkoutDetailScreen`
**Навигация:** получает `workoutId: String` + `mode: ViewMode` (PAST / FUTURE)

Содержимое:
```
[Тип: А]  [пн, 16 мар.]  [✅ Завершена / 📅 Запланирована]

── Жим лёжа ────────────────────────────
  Рекомендовано:  3 × 8-12 @ 27.5 кг
  Выполнено:      3 × 10 @ 25.0 кг       (только для PAST)

  [Объём]  [Макс. вес]  [Повторы]         ← мини-графики
   (5 последних тренировок типа А)
─────────────────────────────────────────
```

**Расчёт рекомендованного веса** — реализовать `WorkoutRecommendationUseCase`:
- Найти последнюю тренировку того же типа из Room
- Применить формулу прогрессии из таблицы
- Fallback: стартовый вес из программы

**Мини-графики** (последние 5-6 тренировок того же типа):
- Проверить есть ли в проекте `MPAndroidChart` или `Vico`
- Если нет — добавить `com.patrykandpatrick.vico:compose:1.15.0` (Compose) или `com.github.PhilJay:MPAndroidChart:v3.1.0` (XML)
- 3 SparkLine-графика на упражнение: тоннаж, макс. вес, среднее кол-во повторов

---

### ФИЧА 4: Активная тренировка

**Создать:** `ActiveWorkoutFragment` / `ActiveWorkoutScreen`

```kotlin
data class ActiveWorkoutUiState(
    val elapsedSeconds: Long = 0,
    val isPaused: Boolean = false,
    val exercises: List<ActiveExerciseUi> = emptyList(),
    val isFinished: Boolean = false
)

data class ActiveExerciseUi(
    val exercise: Exercise,
    val recommendation: ExerciseRecommendation,  // из WorkoutRecommendationUseCase
    val sets: List<SetInput> = listOf(SetInput())
)

data class SetInput(
    val weight: String = "",
    val reps: String = "",
    val rir: String = "",
    val isCompleted: Boolean = false
)
```

Таймер в ViewModel:
```kotlin
private fun startTimer() {
    viewModelScope.launch {
        while (_state.value.isFinished.not()) {
            delay(1000)
            if (!_state.value.isPaused) {
                _state.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        }
    }
}
```

При "Завершить" → диалог подтверждения → сохранить в Room:
```kotlin
suspend fun finishWorkout() {
    val result = WorkoutResult(
        date = LocalDate.now(),
        type = workoutType,
        durationSeconds = state.elapsedSeconds,
        totalTonnage = exercises.flatMap { it.sets }
            .filter { it.isCompleted }
            .sumOf { (it.weight.toFloatOrNull() ?: 0f) * (it.reps.toIntOrNull() ?: 0) }
    )
    workoutRepository.save(result)
    // + сохранить каждый set отдельно для статистики
}
```

---

### ФИЧА 5: Дашборд — плашки и метрики

**Экран:** DashboardFragment / DashboardScreen (таб "Главная")

Добавить над блоком "Метрики":
```kotlin
data class DashboardUiState(
    val lastWorkout: WorkoutSummary?,    // последняя завершённая
    val nextWorkout: ScheduledWorkout?,  // ближайшая запланированная
    val metrics: BodyMetrics
)
```

Обе плашки — кликабельны, навигируют на WorkoutDetailScreen.

**Navy formula** — реализовать в `BodyMetricsCalculator.kt`:
```kotlin
fun bodyFatNavy(waistCm: Float, neckCm: Float, heightCm: Float): Float =
    (86.01 * log10((waistCm - neckCm).toDouble()) -
     70.041 * log10(heightCm.toDouble()) + 36.76).toFloat()
```

Убедиться что все 7 метрик на экране берутся из Room, а не из моков/хардкода:
вес тела, % жира, Δ талия, кол-во тренировок, общий тоннаж, средний объём, средний RIR.

---

### ФИЧА 6: Статистика — графики

**Экран:** StatisticsFragment / StatisticsScreen (таб "Статистика")

| График | Тип | X | Y |
|--------|-----|---|---|
| Вес тела | Line | дата | кг |
| % жира | Line | дата | % |
| Тоннаж | Bar | дата | кг, цвет по типу А/Б |
| Прогресс упражнения | Line | дата | макс. вес |
| Объём по неделям | Bar | неделя | суммарный тоннаж |

Для "Прогресс упражнения" — добавить Spinner / DropdownMenu с выбором упражнения из Room (все когда-либо выполненные).

Использовать ту же библиотеку что выбрана в ФИЧЕ 3.

---

## Шаг 4 — Архитектурные улучшения (по ходу, не отдельным проходом)

- Вся бизнес-логика (прогрессия, Navy, расчёт тоннажа) — только в `domain/`, не в ViewModel
- При изменении схемы Room — писать `Migration`, не `fallbackToDestructiveMigration()`
- `LiveData` → `StateFlow` везде где затрагиваешь код
- Репозитории возвращают `Result<T>` или `sealed class` вместо nullable
- Строки форматирования дат — через `DateTimeFormatter`, не хардкод

---

## Ограничения

- **Читай файл перед изменением** — не перезаписывай не посмотрев содержимое
- **Сохраняй дизайн:** тёмный фон (`#1C1C1E`), фиолетовые акценты (`#7B61FF`), скругления 12dp+
- **Проверяй `build.gradle` перед добавлением зависимости** — не дублируй существующие
- Если логика таблицы неоднозначна — добавь `// TODO CLARIFY: ...` и продолжай с разумным допущением
- После каждой фичи: `git add -A && git commit -m "feat: <фича>"`

---

## Порядок выполнения

```
1. Аудит → описание архитектуры в комментарии
2. Парсинг таблицы → Kotlin код в domain/
3. Фича 1 (DnD) → commit
4. Фича 2 (умный экран тренировки) → commit
5. Фича 3 (детальный просмотр) → commit
6. Фича 4 (активная тренировка) → commit
7. Фича 5 (дашборд) → commit
8. Фича 6 (статистика) → commit
9. Архитектурные улучшения → commit
```
