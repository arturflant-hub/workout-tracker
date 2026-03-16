package com.workouttracker.domain.usecase

import com.workouttracker.data.db.entities.ProgramExercise
import com.workouttracker.data.repository.SessionRepository
import com.workouttracker.domain.model.Recommendation
import com.workouttracker.domain.model.RecommendationType
import javax.inject.Inject

class ProgressionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    suspend fun getProgressionRecommendation(
        programExercise: ProgramExercise
    ): Recommendation {
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

        // Check if ALL sets hit max reps
        val allHitMaxReps = lastSets.all { set ->
            set.actualReps >= lastExercise.plannedMaxReps
        }

        return if (allHitMaxReps) {
            val isBarbell = programExercise.startWeightNote.lowercase().contains("barbell") ||
                    programExercise.startWeightNote.isEmpty()
            if (isBarbell) {
                Recommendation(
                    RecommendationType.INCREASE_WEIGHT,
                    "Отличная работа! Увеличьте вес на +2.5 кг на следующей тренировке"
                )
            } else {
                Recommendation(
                    RecommendationType.INCREASE_WEIGHT,
                    "Отличная работа! Увеличьте вес на +1-2 кг (гантели) на следующей тренировке"
                )
            }
        } else {
            // Rotate between improvement tips
            val tips = listOf(
                Recommendation(
                    RecommendationType.INCREASE_REPS,
                    "Сосредоточьтесь на увеличении повторений до ${lastExercise.plannedMaxReps}"
                ),
                Recommendation(
                    RecommendationType.SLOW_NEGATIVE,
                    "Замедлите негативную фазу (опускание) до 3-4 секунд для лучшей нагрузки"
                ),
                Recommendation(
                    RecommendationType.ADD_PAUSE,
                    "Добавьте паузу 1-2 сек в нижней точке движения для усиления стимула"
                )
            )
            val idx = (lastSets.size % tips.size)
            tips[idx]
        }
    }
}
