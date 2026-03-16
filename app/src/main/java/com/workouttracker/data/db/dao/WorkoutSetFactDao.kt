package com.workouttracker.data.db.dao

import androidx.room.*
import com.workouttracker.data.db.entities.WorkoutSetFact
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSetFactDao {
    @Query("SELECT * FROM workout_set_facts WHERE sessionExerciseId = :exerciseId ORDER BY setIndex ASC")
    fun getSetsByExercise(exerciseId: Long): Flow<List<WorkoutSetFact>>

    @Query("SELECT * FROM workout_set_facts WHERE sessionExerciseId = :exerciseId ORDER BY setIndex ASC")
    suspend fun getSetsByExerciseOnce(exerciseId: Long): List<WorkoutSetFact>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: WorkoutSetFact): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<WorkoutSetFact>)

    @Update
    suspend fun updateSet(set: WorkoutSetFact)

    @Delete
    suspend fun deleteSet(set: WorkoutSetFact)

    @Query("DELETE FROM workout_set_facts WHERE sessionExerciseId = :exerciseId")
    suspend fun deleteAllForExercise(exerciseId: Long)
}
