package com.workouttracker.domain.usecase

import com.workouttracker.data.db.entities.ProgramExercise
import com.workouttracker.data.db.entities.WorkoutSetFact
import com.workouttracker.data.repository.SessionRepository
import com.workouttracker.domain.model.Recommendation
import com.workouttracker.domain.model.RecommendationType
import javax.inject.Inject
import kotlin.math.roundToInt

class ProgressionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    /**
     * Full progression recommendation using RIR-aware logic from training_logic.md.
     *
     * Rules (exact match with spreadsheet "Рекомендация" column):
     *  - avgRir <= 1                          → ⚠ УМЕНЬШИТЬ ВЕС  (-5 кг)
     *  - avgRir <= 2 AND all sets hit maxReps → ⬆ УВЕЛИЧИТЬ ВЕС  (+2.5 кг / +1 кг dumbbell)
     *  - avgRir <= 2 AND reps < maxReps       → 👍 ОСТАВИТЬ ВЕС   (stay the course)
     *  - avgRir >= 3                          → ⬆ МОЖНО ДОБАВИТЬ (+2.5 кг / +1 кг, too easy)
     */
    suspend fun getProgressionRecommendation(programExercise: ProgramExercise): Recommendation {
        val history = sessionRepository.getHistoryByProgramExercise(programExercise.id)
        if (history.isEmpty()) {
            return Recommendation(
                type = RecommendationType.INCREASE_REPS,
                text = "Начните с планового веса и работайте над техникой"
            )
        }

        val lastExercise = history.first()
        val lastSets = sessionRepository.getSetsForExercise(lastExercise.id)
        if (lastSets.isEmpty()) {
            return Recommendation(
                type = RecommendationType.INCREASE_REPS,
                text = "Увеличивайте количество повторений до верхней границы"
            )
        }

        val avgRir = lastSets.map { it.rir }.average().toFloat()
        val allHitMaxReps = lastSets.all { it.actualReps >= lastExercise.plannedMaxReps }

        val prevWeight = lastSets.maxOfOrNull { it.actualWeight }
        val prevReps = lastSets.map { it.actualReps }.average().roundToInt()
        val prevRirInt = avgRir.roundToInt()

        return when {
            avgRir <= 1f -> {
                val nextW = (prevWeight ?: programExercise.startWeight) - 5f
                Recommendation(
                    type = RecommendationType.DECREASE_WEIGHT,
                    text = "⚠ Вес слишком большой — уменьшите до ${"%.1f".format(nextW)} кг",
                    nextWeight = nextW,
                    prevWeight = prevWeight,
                    prevReps = prevReps,
                    prevRir = prevRirInt
                )
            }
            avgRir <= 2f && allHitMaxReps -> {
                val nextW = nextRecommendedWeight(programExercise, lastSets)
                Recommendation(
                    type = RecommendationType.INCREASE_WEIGHT,
                    text = "⬆ Отлично! Увеличьте вес до ${"%.1f".format(nextW)} кг",
                    nextWeight = nextW,
                    prevWeight = prevWeight,
                    prevReps = prevReps,
                    prevRir = prevRirInt
                )
            }
            avgRir <= 2f -> {
                Recommendation(
                    type = RecommendationType.INCREASE_REPS,
                    text = "👍 Продолжайте — цель: дойти до ${lastExercise.plannedMaxReps} повт",
                    nextWeight = prevWeight,
                    prevWeight = prevWeight,
                    prevReps = prevReps,
                    prevRir = prevRirInt
                )
            }
            else -> {
                val nextW = nextRecommendedWeight(programExercise, lastSets)
                Recommendation(
                    type = RecommendationType.INCREASE_WEIGHT,
                    text = "⬆ Слишком легко — увеличьте вес до ${"%.1f".format(nextW)} кг",
                    nextWeight = nextW,
                    prevWeight = prevWeight,
                    prevReps = prevReps,
                    prevRir = prevRirInt
                )
            }
        }
    }

    /**
     * Detects weight plateau: same max weight in the last [threshold] sessions.
     */
    suspend fun detectPlateau(programExerciseId: Long, threshold: Int = 4): Boolean {
        val weights = sessionRepository.getWeightHistoryForExercise(programExerciseId, threshold)
        if (weights.size < threshold) return false
        val first = weights.first()
        return weights.all { it == first }
    }

    /**
     * Calculates next recommended weight.
     * Default step: +2.5 кг (barbell), +1 кг (dumbbell).
     * Matches spreadsheet "Следующий вес" column.
     */
    fun nextRecommendedWeight(exercise: ProgramExercise, lastSets: List<WorkoutSetFact>): Float {
        val currentWeight = lastSets.maxOfOrNull { it.actualWeight } ?: exercise.startWeight
        val isDumbbell = exercise.startWeightNote.lowercase().contains("dumbbell")
        val step = if (isDumbbell) 1f else 2.5f
        return currentWeight + step
    }
}
