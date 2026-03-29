package com.school.manager.data

const val BACKUP_SCHEMA_VERSION = 3   // v3: knowledge points included in full backup

data class BackupMeta(
    val schemaVersion: Int         = BACKUP_SCHEMA_VERSION,
    val exportedAt: String         = "",
    val appVersion: String         = "",
    val counts: BackupCounts       = BackupCounts(),
    val filter: FilterDescription? = null
)

data class BackupCounts(
    val subjects: Int        = 0,
    val teachers: Int        = 0,
    val classes: Int         = 0,
    val students: Int        = 0,
    val lessons: Int         = 0,
    val kpChapters: Int      = 0,
    val kpSections: Int      = 0,
    val knowledgePoints: Int = 0
) {
    val total get() = subjects + teachers + classes + students + lessons +
                      kpChapters + kpSections + knowledgePoints
}

data class FilterDescription(
    val teacherName: String? = null,
    val className: String?   = null,
    val fromDate: String?    = null,
    val toDate: String?      = null
) {
    fun describe(): String = listOfNotNull(
        teacherName?.let { "教师：$it" },
        className?.let   { "班级：$it" },
        fromDate?.let    { "从 $it" },
        toDate?.let      { "至 $it" }
    ).joinToString(" · ").ifBlank { "全部数据" }
}

sealed class ImportResult {
    data class Success(
        val counts: BackupCounts,
        val schemaVersion: Int,
        val exportedAt: String,
        val filter: FilterDescription?
    ) : ImportResult()
    data class Failure(val reason: String) : ImportResult()
}

fun AppState.toBackupCounts() = BackupCounts(
    subjects        = subjects.size,
    teachers        = teachers.size,
    classes         = classes.size,
    students        = students.size,
    lessons         = lessons.size,
    kpChapters      = kpChapters.size,
    kpSections      = kpSections.size,
    knowledgePoints = knowledgePoints.size
)

/** Returns a filtered subset for lesson-scoped exports. KP data not included. */
fun AppState.filterForExport(
    teacherId: Long?,
    classId: Long?,
    fromDate: String?,
    toDate: String?
): AppState {
    val filteredLessons = lessons.filter { l ->
        (teacherId == null || l.effectiveTeacherId(classes) == teacherId) &&
        (classId   == null || l.classId == classId) &&
        (fromDate  == null || l.date >= fromDate) &&
        (toDate    == null || l.date <= toDate)
    }
    val usedClassIds   = filteredLessons.map { it.classId }.toSet()
    val usedClasses    = classes.filter { it.id in usedClassIds }
    val usedTeacherIds = (usedClasses.mapNotNull { it.headTeacherId } +
                         filteredLessons.mapNotNull { it.teacherIdOverride }).toSet()
    val usedSubjectIds = usedClasses.mapNotNull { it.subjectId }.toSet()
    val usedStudents   = students.filter { s -> s.classIds.any { it in usedClassIds } }
    return AppState(
        subjects = subjects.filter { it.id in usedSubjectIds },
        teachers = teachers.filter { it.id in usedTeacherIds },
        classes  = usedClasses,
        students = usedStudents,
        lessons  = filteredLessons
        // kpChapters / kpSections / knowledgePoints intentionally omitted — full export only
    )
}
