package com.workouttracker.di

import android.content.Context
import androidx.room.Room
import com.workouttracker.data.db.AppDatabase
import com.workouttracker.data.db.MIGRATION_1_2
import com.workouttracker.data.db.MIGRATION_2_3
import com.workouttracker.data.db.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "workout_tracker.db"
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    @Provides
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()

    @Provides
    fun provideWorkoutProgramDao(db: AppDatabase): WorkoutProgramDao = db.workoutProgramDao()

    @Provides
    fun provideProgramExerciseDao(db: AppDatabase): ProgramExerciseDao = db.programExerciseDao()

    @Provides
    fun provideScheduleSettingsDao(db: AppDatabase): ScheduleSettingsDao = db.scheduleSettingsDao()

    @Provides
    fun provideWeekPatternDao(db: AppDatabase): WeekPatternDao = db.weekPatternDao()

    @Provides
    fun provideWorkoutSessionDao(db: AppDatabase): WorkoutSessionDao = db.workoutSessionDao()

    @Provides
    fun provideWorkoutSessionExerciseDao(db: AppDatabase): WorkoutSessionExerciseDao =
        db.workoutSessionExerciseDao()

    @Provides
    fun provideWorkoutSetFactDao(db: AppDatabase): WorkoutSetFactDao = db.workoutSetFactDao()

    @Provides
    fun provideBodyMeasurementDao(db: AppDatabase): BodyMeasurementDao = db.bodyMeasurementDao()
}
