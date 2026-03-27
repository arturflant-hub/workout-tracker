package com.workouttracker.data.db.dao

import androidx.room.*
import com.workouttracker.data.db.entities.BodyMeasurement
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyMeasurementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(measurement: BodyMeasurement): Long

    @Query("SELECT * FROM body_measurements ORDER BY date DESC")
    fun getAll(): Flow<List<BodyMeasurement>>

    @Query("SELECT * FROM body_measurements ORDER BY date DESC LIMIT 1")
    suspend fun getLatest(): BodyMeasurement?

    @Query("SELECT * FROM body_measurements ORDER BY date ASC LIMIT 1")
    suspend fun getFirst(): BodyMeasurement?

    @Update
    suspend fun update(measurement: BodyMeasurement)

    @Delete
    suspend fun delete(measurement: BodyMeasurement)

    @Query("DELETE FROM body_measurements")
    suspend fun deleteAll()
}
