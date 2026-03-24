package com.school.manager.data.db

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SubjectEntity::class,
        TeacherEntity::class,
        ClassEntity::class,
        StudentEntity::class,
        LessonEntity::class,
        KnowledgePointEntity::class
    ],
    version      = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun teacherDao(): TeacherDao
    abstract fun classDao(): ClassDao
    abstract fun studentDao(): StudentDao
    abstract fun lessonDao(): LessonDao
    abstract fun knowledgePointDao(): KnowledgePointDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add knowledgePointIdsJson column to lessons
                db.execSQL(
                    "ALTER TABLE lessons ADD COLUMN knowledgePointIdsJson TEXT NOT NULL DEFAULT '[]'"
                )
                // Create knowledge_points table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS knowledge_points (
                        id      INTEGER PRIMARY KEY NOT NULL,
                        grade   TEXT    NOT NULL,
                        chapter TEXT    NOT NULL,
                        section TEXT    NOT NULL,
                        code    TEXT    NOT NULL,
                        content TEXT    NOT NULL,
                        isCustom INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "school_manager.db"
                )
                .addMigrations(MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
    }
}
