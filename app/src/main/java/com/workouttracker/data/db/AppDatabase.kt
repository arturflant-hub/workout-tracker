package com.workouttracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.workouttracker.data.db.dao.*
import com.workouttracker.data.db.entities.*

class Converters {
    @TypeConverter
    fun fromSessionStatus(status: SessionStatus): String = status.name

    @TypeConverter
    fun toSessionStatus(value: String): SessionStatus =
        try { SessionStatus.valueOf(value) } catch (e: IllegalArgumentException) { SessionStatus.PLANNED }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS body_measurements (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                date INTEGER NOT NULL,
                weight REAL NOT NULL,
                chest REAL,
                waist REAL,
                hips REAL,
                thigh REAL,
                arm REAL,
                neck REAL,
                height REAL NOT NULL,
                age INTEGER
            )
        """.trimIndent())
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE workout_set_facts ADD COLUMN rir INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE users ADD COLUMN gender TEXT NOT NULL DEFAULT ''")
    }
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
        WorkoutSetFact::class,
        BodyMeasurement::class
    ],
    version = 4,
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
    abstract fun bodyMeasurementDao(): BodyMeasurementDao
}
