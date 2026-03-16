package com.workouttracker.data.db.dao

import androidx.room.*
import com.workouttracker.data.db.entities.ScheduleSettings
import com.workouttracker.data.db.entities.WeekPattern
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleSettingsDao {
    @Query("SELECT * FROM schedule_settings LIMIT 1")
    fun getSettings(): Flow<ScheduleSettings?>

    @Query("SELECT * FROM schedule_settings LIMIT 1")
    suspend fun getSettingsOnce(): ScheduleSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: ScheduleSettings): Long

    @Update
    suspend fun updateSettings(settings: ScheduleSettings)
}

@Dao
interface WeekPatternDao {
    @Query("SELECT * FROM week_patterns ORDER BY weekType ASC, dayOfWeek ASC")
    fun getAllPatterns(): Flow<List<WeekPattern>>

    @Query("SELECT * FROM week_patterns WHERE weekType = :weekType ORDER BY dayOfWeek ASC")
    suspend fun getPatternsByWeek(weekType: Int): List<WeekPattern>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPattern(pattern: WeekPattern): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatterns(patterns: List<WeekPattern>)

    @Delete
    suspend fun deletePattern(pattern: WeekPattern)

    @Query("DELETE FROM week_patterns")
    suspend fun deleteAll()
}
