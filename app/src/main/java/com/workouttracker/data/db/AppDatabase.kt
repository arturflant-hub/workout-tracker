package com.workouttracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.workouttracker.data.db.dao.*
import com.workouttracker.data.db.entities.*

class Converters {
    @TypeConverter
    fun fromSessionStatus(status: SessionStatus): String = status.name

    @TypeConverter
    fun toSessionStatus(value: String): SessionStatus = SessionStatus.valueOf(value)
}

@Database(
    entities = [
        User::class,
        WorkoutProgram::class,
        ProgramExercise::class,
        ScheduleSettings::class,
        WeekPattern::class,
        WorkoutSession::class,
        WorkoutSessionExercise::class,
        WorkoutSetFact::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun workoutProgramDao(): WorkoutProgramDao
    abstract fun programExerciseDao(): ProgramExerciseDao
    abstract fun scheduleSettingsDao(): ScheduleSettingsDao
    abstract fun weekPatternDao(): WeekPatternDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun workoutSessionExerciseDao(): WorkoutSessionExerciseDao
    abstract fun workoutSetFactDao(): WorkoutSetFactDao
}
