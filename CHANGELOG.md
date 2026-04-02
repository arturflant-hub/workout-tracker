# Changelog — Workout Tracker

## История изменений

| Дата | Commit | Что сделано | Файлы |
|------|--------|-------------|-------|
| 2026-04-02 | — | feat: сильная вибрация (3x паттерн) + уведомление при окончании отдыха. Запрос разрешения POST_NOTIFICATIONS после онбординга и в настройках "Время отдыха" | ActiveWorkoutViewModel.kt, AndroidManifest.xml, NavGraph.kt, SettingsScreen.kt |
| 2026-03-24 | feat/ver2 | feat: кнопка "Сохранить изменения" в диалоге упражнения — появляется когда данные сетов изменились относительно состояния при открытии, сохраняет все сеты в БД | ActiveWorkoutScreen.kt, ActiveWorkoutViewModel.kt |
| 2026-03-24 | feat/ver2 | fix: "Последняя тренировка" — тоннаж суммируется по всем сессиям за последний тренировочный день, показывает "N тренировки за день" если > 1 | DashboardViewModel.kt, DashboardScreen.kt |
| 2026-03-24 | feat/ver2 | fix: карточка рекомендации в истории — заголовок + явные метки (↑ Увеличить вес / ↓ Снизить вес / → Добавить повторения) вместо непонятных эмодзи | StatisticsScreen.kt |
| 2026-03-24 | feat/ver2 | feat: все сегодняшние тренировки в WorkoutTab, тренды (стрелки ↑↓) в метриках Dashboard, подтаб "По тренировкам/упражнениям" в History, bodyFatChange в DashboardState | WorkoutTabScreen.kt, DashboardScreen.kt, DashboardViewModel.kt, StatisticsScreen.kt, HistoryViewModel.kt |
| 2026-03-24 | feat/ver2 | refactor: UX рефакторинг всего приложения — тёмные фильтр-чипы, новые заголовки, пустые состояния с эмодзи, OutlinedButton/ChevronRight навигация, удалены лишние иконки из TopBar | DashboardScreen.kt, WorkoutTabScreen.kt, BodyTrackerScreen.kt, ProgramsScreen.kt, HistoryScreen.kt |
| 2026-03-24 | feat/ver2 | feat: Статистика переработана в 3 таба (Графики / Календарь / История) — месячный календарь, история упражнений с датами и RIR, тёмная тема | StatisticsScreen.kt, HistoryViewModel.kt, HistoryScreen.kt, NavGraph.kt |
| 2026-03-24 | feat/ver2 | feat: меню разработчика (4 тапа по версии) в настройках — сброс тренировок/расписания/программ/антропометрии/всего; версия приложения в настройках | SettingsScreen.kt, DevToolsViewModel.kt, все DAO (deleteAll), все репозитории (reset методы) |
| 2026-03-24 | feat/ver2 | feat: RIR=2 в блоке «ЦЕЛЬ СЕГОДНЯ», комментарии к упражнениям (с историей), режим редактирования DONE тренировки, кнопка «Редактировать» в WorkoutTab, баг createQuickSession | ActiveWorkoutScreen.kt, ActiveWorkoutViewModel.kt, WorkoutTabScreen.kt, ScheduleRepository.kt |
| 2026-03-24 | feat/ver2 | feat: блок «ЦЕЛЬ СЕГОДНЯ» в диалоге упражнения — вес и повторы для текущей тренировки, предзаполнение сетов рекомендуемыми значениями, targetRepsMin/Max по RIR | Recommendation.kt, ProgressionUseCase.kt, ActiveWorkoutViewModel.kt, ActiveWorkoutScreen.kt |
| 2026-03-24 | feat/ver2 | feat: рекомендации на карточках тренировки, исправлена логика прогрессии по training_logic.md, детектор плато, данные прошлой тренировки | Recommendation.kt, ProgressionUseCase.kt, SessionRepository.kt, ActiveWorkoutViewModel.kt, ActiveWorkoutScreen.kt |
| 2026-03-21 | cabef5a | fix: calcBodyFat — expression body с early return заменена на block body | StatisticsViewModel.kt |
| 2026-03-21 | d4a3afe | Добавлен BodyMetricsCalculator (domain), RIR-aware ProgressionUseCase, MiniSparkline в WorkoutDetail, TypedBarChart + weekly volume в Statistics | BodyMetricsCalculator.kt, ProgressionUseCase.kt, StatisticsScreen.kt, StatisticsViewModel.kt, WorkoutDetailScreen.kt, WorkoutDetailViewModel.kt, DashboardViewModel.kt |
| 2026-03-21 | 5b0d146 | Создан CLAUDE.md с контекстом проекта | CLAUDE.md |
| 2026-03-21 | 9d3e9e3 | Исправлены 3 критических бага: авто-создание сессии из расписания, anti-pattern Flow.collect, загрузка упражнений | ScheduleRepository.kt |
| 2026-03-21 | 36ca790 | Dashboard карточки с навигацией на WorkoutDetailScreen | DashboardScreen.kt, DashboardViewModel.kt |
| 2026-03-21 | 85b291f | Рефакторинг ActiveWorkoutScreen | ActiveWorkoutScreen.kt, ActiveWorkoutViewModel.kt |
| 2026-03-21 | 29288bc | WorkoutDetailScreen + WorkoutDetailViewModel | WorkoutDetailScreen.kt, WorkoutDetailViewModel.kt |
