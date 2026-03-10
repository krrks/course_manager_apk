package com.school.manager.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ─── Domain Models ────────────────────────────────────────────────────────────

/** Subject kept for backward-compat with old saved data; no longer shown in nav */
data class Subject(
    val id: Long,
    val name: String,
    val color: Long,
    val teacherId: Long?
)

data class Teacher(
    val id: Long,
    val name: String,
    val gender: String,
    val phone: String,
    val subjectIds: List<Long> = emptyList(),
    val avatarUri: String? = null,
    val code: String = ""        // display / editable ID, e.g. "T001"
)

data class SchoolClass(
    val id: Long,
    val name: String,
    val grade: String,
    val count: Int,
    val headTeacherId: Long?,
    val subject: String = "",    // free-text subject for this class
    val code: String = ""        // display / editable ID, e.g. "C001"
)

data class Student(
    val id: Long,
    val name: String,
    val studentNo: String,
    val gender: String,
    val grade: String,
    val classIds: List<Long> = emptyList(),
    val avatarUri: String? = null
)

data class Schedule(
    val id: Long,
    val classId: Long,
    val subjectId: Long,
    val teacherId: Long?,
    val day: Int,
    val period: Int = 0,
    val startTime: String = "",
    val endTime: String = "",
    val code: String = ""        // display / editable ID, e.g. "SCH001"
)

data class Attendance(
    val id: Long,
    val classId: Long,
    val subjectId: Long,
    val teacherId: Long?,
    val date: String,
    val period: Int = 0,
    val startTime: String = "",
    val endTime: String = "",
    val topic: String,
    val status: String,
    val notes: String,
    val attendees: List<Long> = emptyList(),
    val code: String = ""        // display / editable ID, e.g. "ATT001"
)

// ─── App State ────────────────────────────────────────────────────────────────

data class AppState(
    val subjects:   List<Subject>     = sampleSubjects,
    val teachers:   List<Teacher>     = sampleTeachers,
    val classes:    List<SchoolClass> = sampleClasses,
    val students:   List<Student>     = sampleStudents,
    val schedule:   List<Schedule>    = sampleSchedule,
    val attendance: List<Attendance>  = sampleAttendance
)

// ─── Subject Colors (ARGB) ────────────────────────────────────────────────────

val SUBJECT_COLORS = listOf(
    0xFF1A56DBL, 0xFF0E9F6EL, 0xFF7E3AF2L, 0xFFFF5A1FL,
    0xFFE3A008L, 0xFFE11D48L, 0xFF0891B2L, 0xFF65A30DL
)

val GRADES = listOf("高一","高二","高三","初一","初二","初三")
val DAYS   = listOf("周二","周三","周四","周五","周六","周日","周一")

val PERIOD_TIMES     = listOf("08:00","09:00","10:00","11:00","14:00","15:00","16:00","19:00")
val PERIOD_END_TIMES = listOf("08:45","09:45","10:45","11:45","14:45","15:45","16:45","19:45")
val PERIODS          = PERIOD_TIMES.zip(PERIOD_END_TIMES).map { (s, e) -> "$s-$e" }

val CAL_START_HOUR = 8
val CAL_END_HOUR   = 22

const val PERIOD_MINUTES = 45
const val PERIOD_HOURS   = 0.75f

fun timeToMinutes(hhmm: String): Int {
    if (!hhmm.contains(":")) return 0
    val parts = hhmm.split(":")
    return parts[0].toIntOrNull()?.times(60)?.plus(parts[1].toIntOrNull() ?: 0) ?: 0
}

fun Schedule.resolvedStart(): String =
    startTime.ifBlank { PERIOD_TIMES.getOrElse(period - 1) { "" } }
fun Schedule.resolvedEnd(): String =
    endTime.ifBlank { PERIOD_END_TIMES.getOrElse(period - 1) { "" } }
fun Attendance.resolvedStart(): String =
    startTime.ifBlank { PERIOD_TIMES.getOrElse(period - 1) { "" } }
fun Attendance.resolvedEnd(): String =
    endTime.ifBlank { PERIOD_END_TIMES.getOrElse(period - 1) { "" } }

/** Effective subject name: from Subject lookup, then class.subject, then "?" */
fun Schedule.resolvedSubjectName(subjects: List<Subject>, classes: List<SchoolClass>): String =
    subjects.find { it.id == subjectId }?.name
        ?: classes.find { it.id == classId }?.subject?.takeIf { it.isNotBlank() }
        ?: "?"

/** Auto-generate a short code from timestamp */
fun genCode(prefix: String): String {
    val t = System.currentTimeMillis()
    return "$prefix${(t % 100000).toString().padStart(5, '0')}"
}

// ─── Sample Data ──────────────────────────────────────────────────────────────

val sampleSubjects = listOf(
    Subject(1, "数学", SUBJECT_COLORS[0], 1),
    Subject(2, "语文", SUBJECT_COLORS[1], 2),
    Subject(3, "英语", SUBJECT_COLORS[2], 3),
    Subject(4, "物理", SUBJECT_COLORS[3], 1),
    Subject(5, "化学", SUBJECT_COLORS[4], 4),
)

val sampleTeachers = listOf(
    Teacher(1, "王老师", "男", "138****0001", listOf(1,4), code = "T00001"),
    Teacher(2, "李老师", "女", "139****0002", listOf(2),   code = "T00002"),
    Teacher(3, "张老师", "女", "137****0003", listOf(3),   code = "T00003"),
    Teacher(4, "刘老师", "男", "136****0004", listOf(5),   code = "T00004"),
)

val sampleClasses = listOf(
    SchoolClass(1, "高一(1)班", "高一", 45, 1, subject = "数学", code = "C00001"),
    SchoolClass(2, "高一(2)班", "高一", 43, 2, subject = "语文", code = "C00002"),
    SchoolClass(3, "高二(1)班", "高二", 47, 3, subject = "英语", code = "C00003"),
)

val sampleStudents = listOf(
    Student(1, "陈小明", "20240101", "男", "高一", listOf(1)),
    Student(2, "李小红", "20240102", "女", "高一", listOf(1,2)),
    Student(3, "张伟",   "20240201", "男", "高一", listOf(2)),
    Student(4, "王芳",   "20240202", "女", "高二", listOf(2,3)),
    Student(5, "赵磊",   "20240301", "男", "高二", listOf(3)),
)

val sampleSchedule = listOf(
    Schedule(1, 1, 1, 1, 1, startTime = "08:00", endTime = "08:45", code = "SCH0001"),
    Schedule(2, 1, 2, 2, 1, startTime = "09:00", endTime = "09:45", code = "SCH0002"),
    Schedule(3, 1, 3, 3, 2, startTime = "08:00", endTime = "08:45", code = "SCH0003"),
    Schedule(4, 1, 4, 1, 3, startTime = "10:00", endTime = "10:45", code = "SCH0004"),
    Schedule(5, 1, 5, 4, 4, startTime = "09:00", endTime = "09:45", code = "SCH0005"),
    Schedule(6, 2, 1, 1, 1, startTime = "10:00", endTime = "10:45", code = "SCH0006"),
    Schedule(7, 2, 3, 3, 2, startTime = "11:00", endTime = "11:45", code = "SCH0007"),
    Schedule(8, 3, 2, 2, 1, startTime = "08:00", endTime = "08:45", code = "SCH0008"),
)

// ── Fix: 使用动态日期，确保预设记录始终出现在当月视图中 ──────────────────────
// 每次 AppState() 被调用时（首次安装 / 清空数据）重新计算日期，
// 相对 today 偏移，使大部分记录落在本月，一条落在上月。
val sampleAttendance: List<Attendance>
    get() {
        val fmt   = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val today = LocalDate.now()
        return listOf(
            Attendance(1, 1, 1, 1, fmt.format(today.minusDays(7)),
                startTime="08:00", endTime="08:45", topic="函数与极限",
                status="completed", notes="讲解了基本函数类型",
                attendees=listOf(1,2), code="ATT0001"),
            Attendance(2, 1, 2, 2, fmt.format(today.minusDays(7)),
                startTime="09:00", endTime="09:45", topic="古诗文鉴赏",
                status="completed", notes="",
                attendees=listOf(1), code="ATT0002"),
            Attendance(3, 1, 3, 3, fmt.format(today.minusDays(6)),
                startTime="08:00", endTime="08:45", topic="时态复习",
                status="completed", notes="重点复习过去完成时",
                attendees=listOf(2), code="ATT0003"),
            Attendance(4, 2, 1, 1, fmt.format(today.minusDays(6)),
                startTime="10:00", endTime="10:45", topic="方程组",
                status="cancelled", notes="教师请假",
                attendees=emptyList(), code="ATT0004"),
            Attendance(5, 1, 4, 1, fmt.format(today),
                startTime="10:00", endTime="10:45", topic="牛顿运动定律",
                status="completed", notes="",
                attendees=listOf(1,2), code="ATT0005"),
            // 刻意放到上个月，验证跨月导航
            Attendance(6, 3, 2, 2, fmt.format(today.minusDays(32)),
                startTime="08:00", endTime="08:45", topic="现代文阅读",
                status="completed", notes="",
                attendees=listOf(5), code="ATT0006"),
        )
    }
