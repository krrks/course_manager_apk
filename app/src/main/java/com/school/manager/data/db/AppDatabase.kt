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
        ScheduleEntity::class,
        AttendanceEntity::class
    ],
    version  = 3,
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

        // ── Migration v1 → v2 ─────────────────────────────────────────────────
        // Adds the `code` column to the subjects table.
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE subjects ADD COLUMN code TEXT NOT NULL DEFAULT ''")
            }
        }

        // ── Migration v2 → v3 ─────────────────────────────────────────────────
        // Plan A: remove subjectId from schedule & attendance, remove subject
        // string from classes. SQLite doesn't support DROP COLUMN before API 35,
        // so we recreate each affected table.
        //
        // Data preserved: all rows kept; subjectId values in schedule/attendance
        // are simply dropped (they are now derived at read time via the class FK).
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {

                // ── classes: drop legacy `subject` text column ────────────────
                db.execSQL("""
                    CREATE TABLE classes_new (
                        id            INTEGER PRIMARY KEY NOT NULL,
                        name          TEXT    NOT NULL,
                        grade         TEXT    NOT NULL,
                        count         INTEGER NOT NULL,
                        headTeacherId INTEGER,
                        subjectId     INTEGER,
                        code          TEXT    NOT NULL,
                        FOREIGN KEY(subjectId)     REFERENCES subjects(id)  ON DELETE SET NULL,
                        FOREIGN KEY(headTeacherId) REFERENCES teachers(id)  ON DELETE SET NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO classes_new (id, name, grade, count, headTeacherId, subjectId, code)
                    SELECT                  id, name, grade, count, headTeacherId, subjectId, code
                    FROM classes
                """.trimIndent())
                db.execSQL("DROP TABLE classes")
                db.execSQL("ALTER TABLE classes_new RENAME TO classes")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_classes_subjectId     ON classes(subjectId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_classes_headTeacherId ON classes(headTeacherId)")

                // ── schedule: drop subjectId column ───────────────────────────
                db.execSQL("""
                    CREATE TABLE schedule_new (
                        id        INTEGER PRIMARY KEY NOT NULL,
                        classId   INTEGER NOT NULL,
                        teacherId INTEGER,
                        day       INTEGER NOT NULL,
                        period    INTEGER NOT NULL,
                        startTime TEXT    NOT NULL,
                        endTime   TEXT    NOT NULL,
                        code      TEXT    NOT NULL,
                        FOREIGN KEY(classId)   REFERENCES classes(id)  ON DELETE CASCADE,
                        FOREIGN KEY(teacherId) REFERENCES teachers(id) ON DELETE SET NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO schedule_new (id, classId, teacherId, day, period, startTime, endTime, code)
                    SELECT                   id, classId, teacherId, day, period, startTime, endTime, code
                    FROM schedule
                """.trimIndent())
                db.execSQL("DROP TABLE schedule")
                db.execSQL("ALTER TABLE schedule_new RENAME TO schedule")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_schedule_classId   ON schedule(classId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_schedule_teacherId ON schedule(teacherId)")

                // ── attendance: drop subjectId column ─────────────────────────
                db.execSQL("""
                    CREATE TABLE attendance_new (
                        id           INTEGER PRIMARY KEY NOT NULL,
                        classId      INTEGER NOT NULL,
                        teacherId    INTEGER,
                        date         TEXT    NOT NULL,
                        period       INTEGER NOT NULL,
                        startTime    TEXT    NOT NULL,
                        endTime      TEXT    NOT NULL,
                        topic        TEXT    NOT NULL,
                        status       TEXT    NOT NULL,
                        notes        TEXT    NOT NULL,
                        attendeesJson TEXT   NOT NULL,
                        code         TEXT    NOT NULL,
                        FOREIGN KEY(classId)   REFERENCES classes(id)  ON DELETE CASCADE,
                        FOREIGN KEY(teacherId) REFERENCES teachers(id) ON DELETE SET NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO attendance_new (id, classId, teacherId, date, period, startTime, endTime, topic, status, notes, attendeesJson, code)
                    SELECT                     id, classId, teacherId, date, period, startTime, endTime, topic, status, notes, attendeesJson, code
                    FROM attendance
                """.trimIndent())
                db.execSQL("DROP TABLE attendance")
                db.execSQL("ALTER TABLE attendance_new RENAME TO attendance")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_attendance_classId   ON attendance(classId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_attendance_teacherId ON attendance(teacherId)")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "school_manager.db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
    }
}
