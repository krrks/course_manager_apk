package com.school.manager.data.db

import android.content.Context
import androidx.room.*

@Database(
    entities = [
        SubjectEntity::class, TeacherEntity::class, ClassEntity::class,
        StudentEntity::class, LessonEntity::class,
        KpChapterEntity::class, KpSectionEntity::class, KnowledgePointEntity::class
    ],
    version      = 1,
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
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
    }
}
