package com.school.manager.data.db

import androidx.room.*

// ─── Subject ──────────────────────────────────────────────────────────────────

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val color: Long,
    val teacherId: Long?,          // soft ref — no DB FK (avoids circular dep)
    val code: String               // unique display code, e.g. "SBJ00001"
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
//    subjectId    → subjects.id   ON DELETE SET NULL
//    headTeacherId→ teachers.id   ON DELETE SET NULL

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
    val subject: String,           // display fallback — kept for legacy JSON compat
    val code: String
)

// ─── Student ──────────────────────────────────────────────────────────────────
//  classIds stored as JSON string (multi-class membership, no junction table)

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val studentNo: String,
    val gender: String,
    val grade: String,
    val classIdsJson: String,      // e.g. "[1,3]"
    val avatarUri: String?
)

// ─── Schedule ─────────────────────────────────────────────────────────────────
//  FK constraints:
//    classId   → classes.id    ON DELETE CASCADE   (delete class → delete its schedule)
//    subjectId → subjects.id   ON DELETE SET NULL
//    teacherId → teachers.id   ON DELETE SET NULL

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
            entity        = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns  = ["subjectId"],
            onDelete      = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity        = TeacherEntity::class,
            parentColumns = ["id"],
            childColumns  = ["teacherId"],
            onDelete      = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("classId"), Index("subjectId"), Index("teacherId")]
)
data class ScheduleEntity(
    @PrimaryKey val id: Long,
    val classId: Long,
    val subjectId: Long?,          // nullable: SET_NULL when subject deleted
    val teacherId: Long?,
    val day: Int,
    val period: Int,
    val startTime: String,
    val endTime: String,
    val code: String
)

// ─── Attendance ───────────────────────────────────────────────────────────────
//  FK constraints: same pattern as Schedule
//  attendees stored as JSON string (list of student IDs)

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
            entity        = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns  = ["subjectId"],
            onDelete      = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity        = TeacherEntity::class,
            parentColumns = ["id"],
            childColumns  = ["teacherId"],
            onDelete      = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("classId"), Index("subjectId"), Index("teacherId")]
)
data class AttendanceEntity(
    @PrimaryKey val id: Long,
    val classId: Long,
    val subjectId: Long?,          // nullable: SET_NULL when subject deleted
    val teacherId: Long?,
    val date: String,
    val period: Int,
    val startTime: String,
    val endTime: String,
    val topic: String,
    val status: String,
    val notes: String,
    val attendeesJson: String,     // e.g. "[1,2,5]"
    val code: String
)
