package com.workouttracker.data.repository

import com.workouttracker.data.db.dao.ProgramExerciseDao
import com.workouttracker.data.db.dao.WorkoutProgramDao
import com.workouttracker.data.db.entities.ProgramExercise
import com.workouttracker.data.db.entities.WorkoutProgram
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgramRepository @Inject constructor(
    private val programDao: WorkoutProgramDao,
    private val exerciseDao: ProgramExerciseDao
) {
    fun getAllPrograms(): Flow<List<WorkoutProgram>> = programDao.getAllPrograms()

    fun getExercisesByProgram(programId: Long): Flow<List<ProgramExercise>> =
        exerciseDao.getExercisesByProgram(programId)

    suspend fun getProgramByType(type: String): WorkoutProgram? =
        programDao.getProgramByType(type)

    suspend fun insertProgram(program: WorkoutProgram): Long =
        programDao.insertProgram(program)

    suspend fun updateProgram(program: WorkoutProgram) =
        programDao.updateProgram(program)

    suspend fun deleteProgram(program: WorkoutProgram) =
        programDao.deleteProgram(program)

    suspend fun getExerciseById(id: Long): ProgramExercise? =
        exerciseDao.getExerciseById(id)

    suspend fun insertExercise(exercise: ProgramExercise): Long =
        exerciseDao.insertExercise(exercise)

    suspend fun updateExercise(exercise: ProgramExercise) =
        exerciseDao.updateExercise(exercise)

    suspend fun deleteExercise(exercise: ProgramExercise) =
        exerciseDao.deleteExercise(exercise)
}
