package com.workouttracker.domain.model

enum class RecommendationType {
    INCREASE_WEIGHT,
    DECREASE_WEIGHT,
    INCREASE_REPS,
    SLOW_NEGATIVE,
    ADD_PAUSE,
    PLATEAU
}

data class Recommendation(
    val type: RecommendationType,
    val text: String,
    val nextWeight: Float? = null,
    val prevWeight: Float? = null,
    val prevReps: Int? = null,
    val prevRir: Int? = null,
    val targetRepsMin: Int? = null,
    val targetRepsMax: Int? = null
)
