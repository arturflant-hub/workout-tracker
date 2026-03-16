package com.workouttracker.domain.model

import com.workouttracker.data.db.entities.WorkoutSession
import com.workouttracker.data.db.entities.WorkoutSessionExercise
import com.workouttracker.data.db.entities.WorkoutSetFact

enum class ComparisonStatus { BETTER, EQUAL, WORSE }

data class SetComparison(
    val setFact: WorkoutSetFact,
    val repsStatus: ComparisonStatus,
    val weightStatus: ComparisonStatus
)

data class ExerciseSummary(
    val exercise: WorkoutSessionExercise,
    val sets: List<SetComparison>
)

data class SessionSummary(
    val session: WorkoutSession,
    val exercises: List<ExerciseSummary>
)
