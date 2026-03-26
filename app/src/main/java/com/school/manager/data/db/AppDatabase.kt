package com.school.manager.data.db

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SubjectEntity::class, TeacherEntity::class, ClassEntity::class,
        StudentEntity::class, LessonEntity::class,
        KpChapterEntity::class, KpSectionEntity::class, KnowledgePointEntity::class
    ],
    version      = 4,
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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE lessons ADD COLUMN knowledgePointIdsJson TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS knowledge_points (
                        id INTEGER PRIMARY KEY NOT NULL, grade TEXT NOT NULL,
                        chapter TEXT NOT NULL, section TEXT NOT NULL,
                        code TEXT NOT NULL, content TEXT NOT NULL, isCustom INTEGER NOT NULL DEFAULT 0
                    )""".trimIndent())
            }
        }

        /**
         * Migrates knowledge points to a 3-table structure (chapters → sections → points).
         * ⚠ Custom knowledge points created by users are lost in this migration because the
         * old string-based chapter/section fields cannot be auto-mapped to the new numeric IDs.
         * All seed data is re-inserted automatically on next launch.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Drop old flat knowledge_points table
                db.execSQL("DROP TABLE IF EXISTS knowledge_points")

                // Chapter table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS kp_chapters (
                        id INTEGER PRIMARY KEY NOT NULL,
                        grade TEXT NOT NULL, no INTEGER NOT NULL, name TEXT NOT NULL
                    )""".trimIndent())

                // Section table (FK → kp_chapters CASCADE)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS kp_sections (
                        id INTEGER PRIMARY KEY NOT NULL,
                        chapterId INTEGER NOT NULL, no INTEGER NOT NULL, name TEXT NOT NULL,
                        FOREIGN KEY(chapterId) REFERENCES kp_chapters(id) ON DELETE CASCADE
                    )""".trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_kp_sections_chapterId ON kp_sections(chapterId)")

                // Knowledge points table (FK → kp_sections CASCADE)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS knowledge_points (
                        id INTEGER PRIMARY KEY NOT NULL,
                        sectionId INTEGER NOT NULL, no INTEGER NOT NULL,
                        content TEXT NOT NULL, isCustom INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(sectionId) REFERENCES kp_sections(id) ON DELETE CASCADE
                    )""".trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_knowledge_points_sectionId ON knowledge_points(sectionId)")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, "school_manager.db"
                )
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
    }
}
