package com.workouttracker.domain.model

enum class RecommendationType { INCREASE_WEIGHT, INCREASE_REPS, SLOW_NEGATIVE, ADD_PAUSE }

data class Recommendation(
    val type: RecommendationType,
    val text: String
)
