package com.school.manager.data.db

import androidx.room.*

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val color: Long,
    val teacherId: Long?,
    val code: String
)

@Entity(tableName = "teachers")
data class TeacherEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val gender: String,
    val phone: String,
    val avatarUri: String?,
    val code: String
)

@Entity(
    tableName = "classes",
    foreignKeys = [
        ForeignKey(SubjectEntity::class, ["id"], ["subjectId"],     onDelete = ForeignKey.SET_NULL),
        ForeignKey(TeacherEntity::class, ["id"], ["headTeacherId"], onDelete = ForeignKey.SET_NULL)
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

/**
 * Lesson replaces both Schedule and Attendance.
 * classId → classes ON DELETE CASCADE.
 * teacherIdOverride: when non-null, overrides the class's headTeacherId for this lesson.
 * knowledgePointIdsJson: JSON array of selected knowledge point IDs.
 */
@Entity(
    tableName = "lessons",
    foreignKeys = [
        ForeignKey(ClassEntity::class, ["id"], ["classId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("classId"), Index("date")]
)
data class LessonEntity(
    @PrimaryKey val id: Long,
    val classId: Long,
    val date: String,           // YYYY-MM-DD
    val startTime: String,
    val endTime: String,
    val status: String,         // pending/completed/absent/cancelled/postponed
    val topic: String,
    val notes: String,
    val attendeesJson: String,
    val isModified: Boolean,
    val code: String,
    val teacherIdOverride: Long? = null,
    val knowledgePointIdsJson: String = "[]"
)

/**
 * A knowledge point entry.
 * isCustom = false → seeded from assets/knowledge_points.json
 * isCustom = true  → user-created at runtime
 */
@Entity(tableName = "knowledge_points")
data class KnowledgePointEntity(
    @PrimaryKey val id: Long,
    val grade: String,
    val chapter: String,
    val section: String,
    val code: String,
    val content: String,
    val isCustom: Boolean
)
