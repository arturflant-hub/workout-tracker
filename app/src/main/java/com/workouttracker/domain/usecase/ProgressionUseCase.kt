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
     *
     * Also fills targetRepsMin/targetRepsMax based on prevRir (training_logic.md section 4.4):
     *  - prevRir 1 (hard)    → 8–9
     *  - prevRir 2 (normal)  → 9–11
     *  - prevRir 3+ (easy)   → 10–12
     *  - no data             → 10–12
     */
    suspend fun getProgressionRecommendation(programExercise: ProgramExercise): Recommendation {
        val history = sessionRepository.getHistoryByProgramExercise(programExercise.id)
        if (history.isEmpty()) {
            return Recommendation(
                type = RecommendationType.INCREASE_REPS,
                text = "Первая тренировка — работайте с плановым весом, следите за техникой",
                targetRepsMin = 10,
                targetRepsMax = 12
            )
        }

        val lastExercise = history.first()
        val lastSets = sessionRepository.getSetsForExercise(lastExercise.id)
        if (lastSets.isEmpty()) {
            return Recommendation(
                type = RecommendationType.INCREASE_REPS,
                text = "Нет данных по прошлой тренировке — работайте по плану",
                targetRepsMin = 10,
                targetRepsMax = 12
            )
        }

        val avgRir = lastSets.map { it.rir }.average().toFloat()
        val allHitMaxReps = lastSets.all { it.actualReps >= lastExercise.plannedMaxReps }

        val prevWeight = lastSets.maxOfOrNull { it.actualWeight }
        val prevReps = lastSets.map { it.actualReps }.average().roundToInt()
        val prevRirInt = avgRir.roundToInt()

        // Target reps for TODAY based on previous RIR (training_logic.md section 4.4)
        val (targetMin, targetMax) = targetRepsForToday(prevRirInt)

        return when {
            avgRir <= 1f -> {
                val nextW = (prevWeight ?: programExercise.startWeight) - 5f
                Recommendation(
                    type = RecommendationType.DECREASE_WEIGHT,
                    text = "Снизьте вес до ${formatW(nextW)} кг — прошлый раз было слишком тяжело (RIR ${prevRirInt})",
                    nextWeight = nextW,
                    prevWeight = prevWeight,
                    prevReps = prevReps,
                    prevRir = prevRirInt,
                    targetRepsMin = targetMin,
                    targetRepsMax = targetMax
                )
            }
            avgRir <= 2f && allHitMaxReps -> {
                val nextW = nextRecommendedWeight(programExercise, lastSets)
                Recommendation(
                    type = RecommendationType.INCREASE_WEIGHT,
                    text = "Добавьте вес! Прошлый раз вы дошли до ${lastExercise.plannedMaxReps} повт с RIR ${prevRirInt} — берите ${formatW(nextW)} кг",
                    nextWeight = nextW,
                    prevWeight = prevWeight,
                    prevReps = prevReps,
                    prevRir = prevRirInt,
                    targetRepsMin = targetMin,
                    targetRepsMax = targetMax
                )
            }
            avgRir <= 2f -> {
                Recommendation(
                    type = RecommendationType.INCREASE_REPS,
                    text = "Тот же вес. Цель — дойти до ${lastExercise.plannedMaxReps} повт. Прошлый раз: ${prevReps} повт, RIR ${prevRirInt}",
                    nextWeight = prevWeight,
                    prevWeight = prevWeight,
                    prevReps = prevReps,
                    prevRir = prevRirInt,
                    targetRepsMin = targetMin,
                    targetRepsMax = targetMax
                )
            }
            else -> {
                val nextW = nextRecommendedWeight(programExercise, lastSets)
                Recommendation(
                    type = RecommendationType.INCREASE_WEIGHT,
                    text = "Прошлый раз было легко (RIR ${prevRirInt}) — добавьте вес до ${formatW(nextW)} кг",
                    nextWeight = nextW,
                    prevWeight = prevWeight,
                    prevReps = prevReps,
                    prevRir = prevRirInt,
                    targetRepsMin = targetMin,
                    targetRepsMax = targetMax
                )
            }
        }
    }

    /**
     * Target rep range for today based on previous session's average RIR.
     * Source: training_logic.md section 4.4
     */
    private fun targetRepsForToday(prevRirInt: Int): Pair<Int, Int> {
        return when {
            prevRirInt <= 1 -> 8 to 9
            prevRirInt == 2 -> 9 to 11
            else -> 10 to 12
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

    private fun formatW(w: Float): String =
        if (w == w.toLong().toFloat()) w.toLong().toString() else "%.1f".format(w)
}
