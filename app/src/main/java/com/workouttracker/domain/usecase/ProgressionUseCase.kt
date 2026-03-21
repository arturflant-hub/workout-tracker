package com.workouttracker.domain.usecase

import com.workouttracker.data.db.entities.ProgramExercise
import com.workouttracker.data.db.entities.WorkoutSetFact
import com.workouttracker.data.repository.SessionRepository
import com.workouttracker.domain.model.Recommendation
import com.workouttracker.domain.model.RecommendationType
import javax.inject.Inject

class ProgressionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    /**
     * Full progression recommendation using RIR-aware logic from the training spreadsheet.
     *
     * Rules (matches spreadsheet columns "ПОРА + ВЕС?" and "Рекомендация"):
     *  - avgRir >= 3                         → ⬆ МОЖНО ДОБАВИТЬ  (too easy, bump weight/reps)
     *  - all sets hit maxReps AND avgRir <= 2 → ⬆ УВЕЛИЧИТЬ ВЕС  (+2.5 kg)
     *  - avgRir <= 1 AND reps < minReps       → ⚠ УМЕНЬШИТЬ ВЕС  (too heavy)
     *  - otherwise                            → 👍 ОСТАВИТЬ ВЕС   (stay the course)
     */
    suspend fun getProgressionRecommendation(programExercise: ProgramExercise): Recommendation {
        val history = sessionRepository.getHistoryByProgramExercise(programExercise.id)
        if (history.isEmpty()) {
            return Recommendation(
                RecommendationType.INCREASE_REPS,
                "Начните с планового веса и работайте над техникой"
            )
        }

        val lastExercise = history.first()
        val lastSets = sessionRepository.getSetsForExercise(lastExercise.id)
        if (lastSets.isEmpty()) {
            return Recommendation(
                RecommendationType.INCREASE_REPS,
                "Увеличивайте количество повторений до верхней границы"
            )
        }

        val avgRir = lastSets.map { it.rir }.average().toFloat()
        val allHitMaxReps = lastSets.all { it.actualReps >= lastExercise.plannedMaxReps }
        val anyBelowMinReps = lastSets.any { it.actualReps < lastExercise.plannedMinReps }

        return when {
            avgRir <= 1f && anyBelowMinReps -> Recommendation(
                RecommendationType.INCREASE_REPS,
                "⚠ Вес слишком большой — уменьшите на 2.5 кг на следующей тренировке"
            )
            allHitMaxReps && avgRir <= 2f -> {
                val nextWeight = nextRecommendedWeight(programExercise, lastSets)
                Recommendation(
                    RecommendationType.INCREASE_WEIGHT,
                    "⬆ Отлично! Увеличьте вес до ${"%.1f".format(nextWeight)} кг на следующей тренировке"
                )
            }
            avgRir >= 3f -> Recommendation(
                RecommendationType.INCREASE_REPS,
                "⬆ Легко даётся — добавьте повторения или повысьте вес"
            )
            else -> {
                val tips = listOf(
                    Recommendation(
                        RecommendationType.INCREASE_REPS,
                        "👍 Продолжайте — цель: дойти до ${lastExercise.plannedMaxReps} повторений"
                    ),
                    Recommendation(
                        RecommendationType.SLOW_NEGATIVE,
                        "Замедлите опускание до 3-4 сек для большей нагрузки"
                    ),
                    Recommendation(
                        RecommendationType.ADD_PAUSE,
                        "Добавьте паузу 1-2 сек в нижней точке для усиления стимула"
                    )
                )
                tips[lastSets.size % tips.size]
            }
        }
    }

    /**
     * Calculates next recommended weight.
     * Default step: +2.5 kg (matches spreadsheet "Следующий вес" column).
     * For dumbbells the increment is smaller — uses startWeightNote hint.
     */
    fun nextRecommendedWeight(exercise: ProgramExercise, lastSets: List<WorkoutSetFact>): Float {
        val currentWeight = lastSets.maxOfOrNull { it.actualWeight } ?: exercise.startWeight
        val isDumbbell = exercise.startWeightNote.lowercase().contains("dumbbell")
        val step = if (isDumbbell) 1f else 2.5f
        return currentWeight + step
    }
}
