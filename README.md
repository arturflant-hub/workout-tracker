# Workout Tracker — Дневник тренировок 🏋️

Android-приложение для ведения дневника силовых тренировок.

## Технологии

- **Kotlin** + Jetpack Compose
- **Navigation Compose** — навигация
- **Room** — локальная база данных
- **Hilt** — Dependency Injection
- **MVVM** архитектура
- **Coroutines + Flow/StateFlow**

## Структура

```
com.workouttracker/
  data/
    db/
      entities/   — Room сущности
      dao/        — Data Access Objects
    AppDatabase   — Room база данных
    repository/   — репозитории
  domain/
    model/        — доменные модели (SessionSummary, Recommendation)
    usecase/      — ProgressionUseCase
  ui/
    screens/      — Compose-экраны
    viewmodel/    — ViewModel'и
    navigation/   — NavGraph
    theme/        — Material3 тема
  di/             — Hilt модули
```

## Экраны

1. **ProgramsScreen** — список программ A/B с упражнениями
2. **ProgramExerciseEditScreen** — создание/редактирование упражнения
3. **ScheduleSettingsScreen** — настройка расписания и недельных паттернов
4. **CalendarScreen** — просмотр тренировок по неделям
5. **WorkoutSessionScreen** — ввод факта по подходам
6. **HistoryScreen** — история по упражнению + рекомендации прогрессии

## Логика прогрессии

- Если все подходы выполнены с `actualReps >= plannedMaxReps` → рекомендация увеличить вес
  - Штанга: +2.5 кг
  - Гантели: +1-2 кг
- Иначе → советы по технике (медленный негатив, пауза, добавить повторы)

## База данных

8 Room-таблиц:
- `users` — пользователь (имя, единицы измерения)
- `workout_programs` — программы A/B
- `program_exercises` — упражнения в программе
- `schedule_settings` — настройки расписания
- `week_patterns` — паттерны по неделям цикла
- `workout_sessions` — тренировочные сессии (PLANNED/DONE/SKIPPED)
- `workout_session_exercises` — упражнения в сессии с плановыми/фактическими данными
- `workout_set_facts` — факт по каждому подходу

## Сборка

```bash
./gradlew assembleDebug
```

Требуется Android Studio Hedgehog (2023.1.1+) или выше.
