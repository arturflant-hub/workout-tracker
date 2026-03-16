package com.workouttracker.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "body_measurements")
data class BodyMeasurement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,       // timestamp
    val weight: Float,    // кг
    val chest: Float?,    // грудь, см
    val waist: Float?,    // талия, см
    val hips: Float?,     // бёдра, см
    val thigh: Float?,    // бедро, см
    val arm: Float?,      // рука, см
    val neck: Float?,     // шея, см
    val height: Float,    // рост, см
    val age: Int?         // возраст
)
