package com.school.manager.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ─── Domain Models ────────────────────────────────────────────────────────────

data class Subject(val id: Long, val name: String, val color: Long, val teacherId: Long?, val code: String = "")
data class Teacher(val id: Long, val name: String, val gender: String, val phone: String, val avatarUri: String? = null, val code: String = "")
data class SchoolClass(val id: Long, val name: String, val grade: String, val count: Int, val headTeacherId: Long?, val subjectId: Long? = null, val code: String = "")
data class Student(val id: Long, val name: String, val studentNo: String, val gender: String, val grade: String, val classIds: List<Long> = emptyList(), val avatarUri: String? = null, val code: String = "")

data class Lesson(
    val id: Long, val classId: Long, val date: String,
    val startTime: String = "", val endTime: String = "",
    val status: String = "pending", val topic: String = "", val notes: String = "",
    val attendees: List<Long> = emptyList(), val isModified: Boolean = false,
    val code: String = "", val teacherIdOverride: Long? = null,
    val knowledgePointIds: List<Long> = emptyList()
)

/** Knowledge point chapter — e.g. chapter 1 "机械运动" */
data class KpChapter(
    val id: Long,
    val grade: String,  // e.g. 初中 / 高中 / user-defined
    val no: Int,        // chapter number
    val name: String    // title without number prefix
)

/** Knowledge point section within a chapter */
data class KpSection(
    val id: Long,
    val chapterId: Long,
    val no: Int,
    val name: String
)

/**
 * A single knowledge point with a short title and full content.
 *
 * title   — concise label shown in compact contexts (lesson records, chips).
 * content — full explanation shown in the knowledge points screen.
 * isCustom = true → user-created.
 */
data class KnowledgePoint(
    val id: Long,
    val sectionId: Long,
    val no: Int,
    val title: String,
    val content: String,
    val isCustom: Boolean = false
) {
    /** Display label: title if set, else first 20 chars of content. */
    val displayTitle: String get() = title.ifBlank { content.take(20).let { if (content.length > 20) "$it…" else it } }
}

/**
 * Fully resolved knowledge point with chapter + section context.
 */
data class KpFull(
    val chapter: KpChapter,
    val section: KpSection,
    val point: KnowledgePoint
) {
    val code: String get() = "${chapter.no}.${section.no}.${point.no}"
    val grade: String get() = chapter.grade
    val isCustom: Boolean get() = point.isCustom
    val title: String get() = point.title
    val content: String get() = point.content
    val displayTitle: String get() = point.displayTitle
}

// ─── App State ────────────────────────────────────────────────────────────────

data class AppState(
    val subjects:        List<Subject>        = emptyList(),
    val teachers:        List<Teacher>        = emptyList(),
    val classes:         List<SchoolClass>    = emptyList(),
    val students:        List<Student>        = emptyList(),
    val lessons:         List<Lesson>         = emptyList(),
    val knowledgePoints: List<KnowledgePoint> = emptyList(),
    val kpChapters:      List<KpChapter>      = emptyList(),
    val kpSections:      List<KpSection>      = emptyList()
)

/** Resolve a KnowledgePoint to its fully-joined KpFull, or null if refs are broken. */
fun AppState.kpFull(kp: KnowledgePoint): KpFull? {
    val section = kpSections.find { it.id == kp.sectionId } ?: return null
    val chapter = kpChapters.find { it.id == section.chapterId } ?: return null
    return KpFull(chapter, section, kp)
}

// ─── Constants ────────────────────────────────────────────────────────────────

val SUBJECT_COLORS = listOf(
    0xFF1A56DBL, 0xFF0E9F6EL, 0xFF7E3AF2L, 0xFFFF5A1FL,
    0xFFE3A008L, 0xFFE11D48L, 0xFF0891B2L, 0xFF65A30DL
)

val GRADES         = listOf("高一", "高二", "高三", "初一", "初二", "初三")
val DAYS           = listOf("一", "二", "三", "四", "五", "六", "日")

/** Default grade suggestions for the KP chapter form — user can type any value. */
val PHYSICS_GRADES = listOf("初中", "高中")

const val CAL_START_HOUR = 8
const val CAL_END_HOUR   = 22

// ─── Helper functions ─────────────────────────────────────────────────────────

fun timeToMinutes(hhmm: String): Int {
    if (!hhmm.contains(":")) return 0
    val parts = hhmm.split(":")
    return (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0)
}

fun SchoolClass.resolvedSubject(subjects: List<Subject>): Subject? = subjects.find { it.id == subjectId }
fun Lesson.subjectName(classes: List<SchoolClass>, subjects: List<Subject>): String =
    classes.find { it.id == classId }?.resolvedSubject(subjects)?.name ?: "?"
fun Lesson.effectiveTeacherId(classes: List<SchoolClass>): Long? =
    teacherIdOverride ?: classes.find { it.id == classId }?.headTeacherId
fun Lesson.teacherName(classes: List<SchoolClass>, teachers: List<Teacher>): String {
    val tid = effectiveTeacherId(classes)
    return teachers.find { it.id == tid }?.name ?: "─"
}
fun Lesson.durationMinutes(): Int {
    if (startTime.isBlank() || endTime.isBlank()) return 45
    return (timeToMinutes(endTime) - timeToMinutes(startTime)).coerceAtLeast(0)
}
fun Lesson.durationHours(): Float = durationMinutes() / 60f
fun List<Lesson>.progressFor(classId: Long): Pair<Int, Int> {
    val all = filter { it.classId == classId }
    return all.count { it.status == "completed" } to all.size
}
fun genCode(prefix: String): String =
    "$prefix${(System.currentTimeMillis() % 100000).toString().padStart(5, '0')}"
