package com.school.manager.data.db

import androidx.room.*

@Entity(tableName = "subjects")
data class SubjectEntity(@PrimaryKey val id: Long, val name: String, val color: Long, val teacherId: Long?, val code: String)

@Entity(tableName = "teachers")
data class TeacherEntity(@PrimaryKey val id: Long, val name: String, val gender: String, val phone: String, val avatarUri: String?, val code: String)

@Entity(
    tableName = "classes",
    foreignKeys = [
        ForeignKey(SubjectEntity::class, ["id"], ["subjectId"],     onDelete = ForeignKey.SET_NULL),
        ForeignKey(TeacherEntity::class, ["id"], ["headTeacherId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("subjectId"), Index("headTeacherId")]
)
data class ClassEntity(@PrimaryKey val id: Long, val name: String, val grade: String, val count: Int, val headTeacherId: Long?, val subjectId: Long?, val code: String)

@Entity(tableName = "students")
data class StudentEntity(@PrimaryKey val id: Long, val name: String, val studentNo: String, val gender: String, val grade: String, val classIdsJson: String, val avatarUri: String?)

@Entity(
    tableName = "lessons",
    foreignKeys = [ForeignKey(ClassEntity::class, ["id"], ["classId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("classId"), Index("date")]
)
data class LessonEntity(
    @PrimaryKey val id: Long,
    val classId: Long, val date: String, val startTime: String, val endTime: String,
    val status: String, val topic: String, val notes: String,
    val attendeesJson: String, val isModified: Boolean, val code: String,
    val teacherIdOverride: Long? = null,
    val knowledgePointIdsJson: String = "[]"
)

/** Chapter grouping for knowledge points (e.g. "第1章 机械运动"). */
@Entity(tableName = "kp_chapters")
data class KpChapterEntity(
    @PrimaryKey val id: Long,
    val grade: String,   // 初中 / 高中
    val no: Int,
    val name: String     // title without number prefix
)

/** Section within a chapter (e.g. "第1节 长度和时间的测量"). */
@Entity(
    tableName = "kp_sections",
    foreignKeys = [ForeignKey(KpChapterEntity::class, ["id"], ["chapterId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("chapterId")]
)
data class KpSectionEntity(
    @PrimaryKey val id: Long,
    val chapterId: Long,
    val no: Int,
    val name: String
)

/**
 * Individual knowledge point.
 * code ("X.Y.Z") is derived at runtime from chapter.no, section.no, point.no — not stored.
 * isCustom = false → seeded from assets; true → user-created.
 */
@Entity(
    tableName = "knowledge_points",
    foreignKeys = [ForeignKey(KpSectionEntity::class, ["id"], ["sectionId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("sectionId")]
)
data class KnowledgePointEntity(
    @PrimaryKey val id: Long,
    val sectionId: Long,
    val no: Int,
    val content: String,
    val isCustom: Boolean
)
