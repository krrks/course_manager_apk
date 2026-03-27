package com.school.manager.data.db

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add title column with empty default; existing rows will show content as fallback in UI
        db.execSQL("ALTER TABLE knowledge_points ADD COLUMN title TEXT NOT NULL DEFAULT ''")
    }
}

@Database(
    entities = [
        SubjectEntity::class, TeacherEntity::class, ClassEntity::class,
        StudentEntity::class, LessonEntity::class,
        KpChapterEntity::class, KpSectionEntity::class, KnowledgePointEntity::class
    ],
    version      = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun teacherDao(): TeacherDao
    abstract fun classDao(): ClassDao
    abstract fun studentDao(): StudentDao
    abstract fun lessonDao(): LessonDao
    abstract fun kpChapterDao(): KpChapterDao
    abstract fun kpSectionDao(): KpSectionDao
    abstract fun knowledgePointDao(): KnowledgePointDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, "school_manager.db"
                )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
    }
}
