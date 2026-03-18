package com.school.manager.data.db

import androidx.room.*

// ─── Subject ──────────────────────────────────────────────────────────────────

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val color: Long,
    val teacherId: Long?,
    val code: String
)

// ─── Teacher ──────────────────────────────────────────────────────────────────

@Entity(tableName = "teachers")
data class TeacherEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val gender: String,
    val phone: String,
    val avatarUri: String?,
    val code: String
)

// ─── SchoolClass ──────────────────────────────────────────────────────────────
//  FK constraints:
//    subjectId     → subjects.id   ON DELETE SET NULL
//    headTeacherId → teachers.id   ON DELETE SET NULL
//  `subject` string field removed — subjectId is the single source of truth.

@Entity(
    tableName = "classes",
    foreignKeys = [
        ForeignKey(
            entity        = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns  = ["subjectId"],
            onDelete      = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity        = TeacherEntity::class,
            parentColumns = ["id"],
            childColumns  = ["headTeacherId"],
            onDelete      = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("subjectId"), Index("headTeacherId")]
)
data class ClassEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val grade: String,
    val count: Int,
    val headTeacherId: Long?,
    val subjectId: Long?,
    val code: String
)

// ─── Student ──────────────────────────────────────────────────────────────────

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val studentNo: String,
    val gender: String,
    val grade: String,
    val classIdsJson: String,
    val avatarUri: String?
)

// ─── Schedule ─────────────────────────────────────────────────────────────────
//  FK constraints:
//    classId   → classes.id    ON DELETE CASCADE
//    teacherId → teachers.id   ON DELETE SET NULL
//
//  subjectId removed — subject is always resolved via classId JOIN classes.subjectId.
//  This eliminates the class-of-truth split and removes all manual cascade logic.

@Entity(
    tableName = "schedule",
    foreignKeys = [
        ForeignKey(
            entity        = ClassEntity::class,
            parentColumns = ["id"],
            childColumns  = ["classId"],
            onDelete      = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity        = TeacherEntity::class,
            parentColumns = ["id"],
            childColumns  = ["teacherId"],
            onDelete      = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("classId"), Index("teacherId")]
)
data class ScheduleEntity(
    @PrimaryKey val id: Long,
    val classId: Long,
    val teacherId: Long?,
    val day: Int,
    val period: Int,
    val startTime: String,
    val endTime: String,
    val code: String
)

// ─── Attendance ───────────────────────────────────────────────────────────────
//  FK constraints: same pattern as Schedule
//  subjectId removed — resolved at display time via classId → classes.subjectId.

@Entity(
    tableName = "attendance",
    foreignKeys = [
        ForeignKey(
            entity        = ClassEntity::class,
            parentColumns = ["id"],
            childColumns  = ["classId"],
            onDelete      = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity        = TeacherEntity::class,
            parentColumns = ["id"],
            childColumns  = ["teacherId"],
            onDelete      = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("classId"), Index("teacherId")]
)
data class AttendanceEntity(
    @PrimaryKey val id: Long,
    val classId: Long,
    val teacherId: Long?,
    val date: String,
    val period: Int,
    val startTime: String,
    val endTime: String,
    val topic: String,
    val status: String,
    val notes: String,
    val attendeesJson: String,
    val code: String
)
