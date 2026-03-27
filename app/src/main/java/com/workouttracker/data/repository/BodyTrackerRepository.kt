package com.workouttracker.data.repository

import com.workouttracker.data.db.dao.BodyMeasurementDao
import com.workouttracker.data.db.entities.BodyMeasurement
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BodyTrackerRepository @Inject constructor(
    private val dao: BodyMeasurementDao
) {
    fun getAll(): Flow<List<BodyMeasurement>> = dao.getAll()

    suspend fun getLatest(): BodyMeasurement? = dao.getLatest()

    fun getLatestFlow(): Flow<BodyMeasurement?> = dao.getLatestFlow()

    suspend fun getFirst(): BodyMeasurement? = dao.getFirst()

    suspend fun insert(measurement: BodyMeasurement): Long = dao.insert(measurement)

    suspend fun delete(measurement: BodyMeasurement) = dao.delete(measurement)

    suspend fun deleteAll() = dao.deleteAll()
}
