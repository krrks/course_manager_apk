package com.school.manager.data.db

import android.content.Context
import androidx.room.*

@Database(
    entities = [
        SubjectEntity::class,
        TeacherEntity::class,
        ClassEntity::class,
        StudentEntity::class,
        ScheduleEntity::class,
        AttendanceEntity::class
    ],
    version  = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun subjectDao(): SubjectDao
    abstract fun teacherDao(): TeacherDao
    abstract fun classDao(): ClassDao
    abstract fun studentDao(): StudentDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun attendanceDao(): AttendanceDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "school_manager.db"
                )
                // Future schema changes: add Migration objects here instead of
                // fallbackToDestructiveMigration, e.g.:
                //   .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
    }
}
