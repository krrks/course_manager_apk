package com.school.manager.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ─── Domain Models ────────────────────────────────────────────────────────────

data class Subject(
    val id: Long,
    val name: String,
    val color: Long,
    val teacherId: Long?,
    val code: String = ""
)

data class Teacher(
    val id: Long,
    val name: String,
    val gender: String,
    val phone: String,
    val avatarUri: String? = null,
    val code: String = ""
)

/**
 * One SchoolClass = one fixed time-slot teaching unit.
 * headTeacherId is the default teacher; individual lessons may override it.
 * subjectId is the fixed subject. classId == groupId for batch operations.
 */
data class SchoolClass(
    val id: Long,
    val name: String,
    val grade: String,
    val count: Int,
    val headTeacherId: Long?,
    val subjectId: Long? = null,
    val code: String = ""
)

data class Student(
    val id: Long,
    val name: String,
    val studentNo: String,
    val gender: String,
    val grade: String,
    val classIds: List<Long> = emptyList(),
    val avatarUri: String? = null,
    val code: String = ""
)

/**
 * A single concrete lesson instance.
 * status: pending / completed / absent / cancelled / postponed
 * isModified: true when this row was individually edited (not batch-generated).
 * teacherIdOverride: when non-null, overrides the class's headTeacherId for this lesson.
 * knowledgePointIds: IDs of knowledge points covered in this lesson.
 */
data class Lesson(
    val id: Long,
    val classId: Long,
    val date: String,                           // YYYY-MM-DD
    val startTime: String = "",                 // HH:mm
    val endTime: String = "",                   // HH:mm
    val status: String = "pending",
    val topic: String = "",
    val notes: String = "",
    val attendees: List<Long> = emptyList(),
    val isModified: Boolean = false,
    val code: String = "",
    val teacherIdOverride: Long? = null,        // null = use SchoolClass.headTeacherId
    val knowledgePointIds: List<Long> = emptyList()
)

/**
 * A single knowledge point entry.
 * isCustom: false = seeded from asset; true = user-created.
 */
data class KnowledgePoint(
    val id: Long,
    val grade: String,       // 初中 / 高中
    val chapter: String,
    val section: String,
    val code: String,        // e.g. "1.1.1"
    val content: String,
    val isCustom: Boolean = false
)

// ─── App State ────────────────────────────────────────────────────────────────

data class AppState(
    val subjects:        List<Subject>        = emptyList(),
    val teachers:        List<Teacher>        = emptyList(),
    val classes:         List<SchoolClass>    = emptyList(),
    val students:        List<Student>        = emptyList(),
    val lessons:         List<Lesson>         = emptyList(),
    val knowledgePoints: List<KnowledgePoint> = emptyList()
)

// ─── Constants ────────────────────────────────────────────────────────────────

val SUBJECT_COLORS = listOf(
    0xFF1A56DBL, 0xFF0E9F6EL, 0xFF7E3AF2L, 0xFFFF5A1FL,
    0xFFE3A008L, 0xFFE11D48L, 0xFF0891B2L, 0xFF65A30DL
)

val GRADES         = listOf("高一", "高二", "高三", "初一", "初二", "初三")
val DAYS           = listOf("一", "二", "三", "四", "五", "六", "日")
val PHYSICS_GRADES = listOf("初中", "高中")

const val CAL_START_HOUR = 8
const val CAL_END_HOUR   = 22

// ─── Helper functions ─────────────────────────────────────────────────────────

fun timeToMinutes(hhmm: String): Int {
    if (!hhmm.contains(":")) return 0
    val parts = hhmm.split(":")
    return (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0)
}

fun SchoolClass.resolvedSubject(subjects: List<Subject>): Subject? =
    subjects.find { it.id == subjectId }

fun Lesson.subjectName(classes: List<SchoolClass>, subjects: List<Subject>): String =
    classes.find { it.id == classId }?.resolvedSubject(subjects)?.name ?: "?"

/** Returns the effective teacher id: override first, then class default. */
fun Lesson.effectiveTeacherId(classes: List<SchoolClass>): Long? =
    teacherIdOverride ?: classes.find { it.id == classId }?.headTeacherId

/** Returns the effective teacher name. */
fun Lesson.teacherName(classes: List<SchoolClass>, teachers: List<Teacher>): String {
    val tid = effectiveTeacherId(classes)
    return teachers.find { it.id == tid }?.name ?: "─"
}

fun Lesson.durationMinutes(): Int {
    if (startTime.isBlank() || endTime.isBlank()) return 45
    return (timeToMinutes(endTime) - timeToMinutes(startTime)).coerceAtLeast(0)
}

fun Lesson.durationHours(): Float = durationMinutes() / 60f

/**
 * Returns (completedCount, totalCount) for the given classId.
 */
fun List<Lesson>.progressFor(classId: Long): Pair<Int, Int> {
    val all  = filter { it.classId == classId }
    val done = all.count { it.status == "completed" }
    return done to all.size
}

fun genCode(prefix: String): String =
    "$prefix${(System.currentTimeMillis() % 100000).toString().padStart(5, '0')}"

// ─── Sample Data ──────────────────────────────────────────────────────────────

val sampleSubjects = listOf(
    Subject(1, "数学", SUBJECT_COLORS[0], 1, "SBJ00001"),
    Subject(2, "语文", SUBJECT_COLORS[1], 2, "SBJ00002"),
    Subject(3, "英语", SUBJECT_COLORS[2], 3, "SBJ00003"),
)

val sampleTeachers = listOf(
    Teacher(1, "王老师", "男", "138****0001", code = "T00001"),
    Teacher(2, "李老师", "女", "139****0002", code = "T00002"),
    Teacher(3, "张老师", "女", "137****0003", code = "T00003"),
)

val sampleClasses = listOf(
    SchoolClass(1, "高一(1)班·数学·周一", "高一", 45, 1, subjectId = 1, code = "C00001"),
    SchoolClass(2, "高一(2)班·语文·周三", "高一", 43, 2, subjectId = 2, code = "C00002"),
    SchoolClass(3, "高二(1)班·英语·连续", "高二", 47, 3, subjectId = 3, code = "C00003"),
)

val sampleStudents = listOf(
    Student(1, "陈小明", "20240101", "男", "高一", listOf(1)),
    Student(2, "李小红", "20240102", "女", "高一", listOf(1, 2)),
    Student(3, "张伟",   "20240201", "男", "高一", listOf(2)),
    Student(4, "王芳",   "20240202", "女", "高二", listOf(2, 3)),
    Student(5, "赵磊",   "20240301", "男", "高二", listOf(3)),
)

val sampleLessons: List<Lesson>
    get() {
        val fmt   = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val today = LocalDate.now()
        return listOf(
            Lesson(1,  1, fmt.format(today.minusDays(14)), "08:00", "08:45", "completed", "函数与极限",  "讲解基本函数类型", listOf(1, 2), false, "L00001"),
            Lesson(2,  1, fmt.format(today.minusDays(7)),  "08:00", "08:45", "completed", "导数基础",    "",                 listOf(1, 2), false, "L00002"),
            Lesson(3,  1, fmt.format(today),               "08:00", "08:45", "pending",   "",            "",                 emptyList(), false, "L00003"),
            Lesson(4,  1, fmt.format(today.plusDays(7)),   "08:00", "08:45", "pending",   "",            "",                 emptyList(), false, "L00004"),
            Lesson(5,  2, fmt.format(today.minusDays(11)), "10:00", "10:45", "completed", "古诗文鉴赏",  "",                 listOf(2, 3), false, "L00005"),
            Lesson(6,  2, fmt.format(today.minusDays(4)),  "10:00", "10:45", "cancelled", "",            "教师请假",          emptyList(), false, "L00006"),
            Lesson(7,  2, fmt.format(today.plusDays(3)),   "10:00", "10:45", "pending",   "",            "",                 emptyList(), false, "L00007"),
            Lesson(8,  3, fmt.format(today.minusDays(4)),  "09:00", "09:45", "completed", "时态复习",    "",                 listOf(4, 5), false, "L00008"),
            Lesson(9,  3, fmt.format(today.minusDays(3)),  "09:00", "09:45", "completed", "阅读理解",    "",                 listOf(4, 5), false, "L00009"),
            Lesson(10, 3, fmt.format(today.minusDays(2)),  "09:00", "09:45", "absent",    "",            "学生缺席",          emptyList(), false, "L00010"),
            Lesson(11, 3, fmt.format(today.minusDays(1)),  "09:00", "09:45", "completed", "写作训练",    "",                 listOf(5),   false, "L00011"),
            Lesson(12, 3, fmt.format(today),               "09:00", "09:45", "pending",   "",            "",                 emptyList(), false, "L00012"),
        )
    }

fun sampleAppState(): AppState = AppState(
    subjects = sampleSubjects,
    teachers = sampleTeachers,
    classes  = sampleClasses,
    students = sampleStudents,
    lessons  = sampleLessons
)
