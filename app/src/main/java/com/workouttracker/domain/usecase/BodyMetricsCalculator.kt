package com.workouttracker.domain.usecase

import kotlin.math.log10

/**
 * Domain-layer calculations for body metrics and training analytics.
 * Single source of truth — no duplication across ViewModels.
 */
object BodyMetricsCalculator {

    /**
     * US Navy body fat formula (log10 variant).
     * Returns null when inputs are physically impossible.
     */
    fun bodyFatNavy(waistCm: Float, neckCm: Float, heightCm: Float): Float? {
        val diff = waistCm - neckCm
        if (diff <= 0f || heightCm <= 0f) return null
        return (86.01 * log10(diff.toDouble()) -
                70.041 * log10(heightCm.toDouble()) + 36.76).toFloat()
    }

    /** Epley one-rep-max estimate: weight × (1 + reps / 30). */
    fun e1rm(weight: Float, reps: Int): Float = weight * (1f + reps / 30f)

    /** Session tonnage for a single set. */
    fun tonnage(weight: Float, reps: Int): Float = weight * reps
}
