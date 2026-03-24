package com.school.manager.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.school.manager.data.*
import com.school.manager.data.repository.AppRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AppRepository(app)
    internal val gson: Gson = GsonBuilder().create()

    val state: StateFlow<AppState> = repo.appState
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppState())

    init {
        viewModelScope.launch {
            if (repo.isEmpty()) repo.importAll(sampleAppState())
            repo.seedKnowledgePoints(app)
        }
    }

    // ─── Lookups ──────────────────────────────────────────────────────────────
    fun subject(id: Long?)        = state.value.subjects.find        { it.id == id }
    fun teacher(id: Long?)        = state.value.teachers.find        { it.id == id }
    fun schoolClass(id: Long?)    = state.value.classes.find         { it.id == id }
    fun student(id: Long?)        = state.value.students.find        { it.id == id }
    fun knowledgePoint(id: Long?) = state.value.knowledgePoints.find { it.id == id }

    fun lessonProgress(classId: Long): Pair<Int, Int> =
        state.value.lessons.progressFor(classId)

    // ─── Subjects ─────────────────────────────────────────────────────────────
    fun addSubject(name: String, color: Long, teacherId: Long?, code: String = "") {
        viewModelScope.launch {
            repo.addSubject(Subject(System.currentTimeMillis(), name, color, teacherId,
                code.ifBlank { genCode("SBJ") }))
        }
    }
    fun updateSubject(s: Subject) { viewModelScope.launch { repo.updateSubject(s) } }
    fun deleteSubject(id: Long)   { viewModelScope.launch { repo.deleteSubject(id) } }

    // ─── Teachers ─────────────────────────────────────────────────────────────
    fun addTeacher(name: String, gender: String, phone: String = "",
                   avatarUri: String? = null, code: String = "") {
        viewModelScope.launch {
            repo.addTeacher(Teacher(System.currentTimeMillis(), name, gender, phone,
                avatarUri, code.ifBlank { genCode("T") }))
        }
    }
    fun updateTeacher(t: Teacher) { viewModelScope.launch { repo.updateTeacher(t) } }
    fun deleteTeacher(id: Long)   { viewModelScope.launch { repo.deleteTeacher(id) } }

    // ─── Classes ──────────────────────────────────────────────────────────────
    fun addSchoolClass(name: String, grade: String, count: Int,
                       headTeacherId: Long?, subjectId: Long? = null, code: String = "") {
        viewModelScope.launch {
            repo.addSchoolClass(SchoolClass(System.currentTimeMillis(), name, grade, count,
                headTeacherId, subjectId, code.ifBlank { genCode("C") }))
        }
    }
    fun updateSchoolClass(c: SchoolClass) { viewModelScope.launch { repo.updateSchoolClass(c) } }
    fun deleteSchoolClass(id: Long)        { viewModelScope.launch { repo.deleteSchoolClass(id) } }

    // ─── Students ─────────────────────────────────────────────────────────────
    fun addStudent(name: String, studentNo: String, gender: String,
                   grade: String, classIds: List<Long>, avatarUri: String? = null) {
        viewModelScope.launch {
            repo.addStudent(Student(System.currentTimeMillis(), name, studentNo,
                gender, grade, classIds, avatarUri))
        }
    }
    fun updateStudent(s: Student) { viewModelScope.launch { repo.updateStudent(s) } }
    fun deleteStudent(id: Long)   { viewModelScope.launch { repo.deleteStudent(id) } }

    // ─── Lessons — single ─────────────────────────────────────────────────────
    fun addLesson(
        classId: Long, date: String, startTime: String, endTime: String,
        status: String = "pending", topic: String = "", notes: String = "",
        attendees: List<Long> = emptyList(), code: String = "",
        teacherIdOverride: Long? = null, knowledgePointIds: List<Long> = emptyList()
    ) {
        viewModelScope.launch {
            repo.addLesson(Lesson(System.currentTimeMillis(), classId, date,
                startTime, endTime, status, topic, notes, attendees, false,
                code.ifBlank { genCode("L") }, teacherIdOverride, knowledgePointIds))
        }
    }

    fun updateLesson(l: Lesson, markModified: Boolean = true) {
        val updated = if (markModified) l.copy(isModified = true) else l
        viewModelScope.launch { repo.updateLesson(updated) }
    }

    fun deleteLesson(id: Long) { viewModelScope.launch { repo.deleteLesson(id) } }

    // ─── Lessons — batch generate ─────────────────────────────────────────────
    enum class RecurrenceType { WEEKLY, DAILY, ONCE }

    fun batchGenerateLessons(
        classId: Long, recurrenceType: RecurrenceType,
        startDate: LocalDate, endDate: LocalDate,
        dayOfWeek: Int = 1, startTime: String, endTime: String,
        excludeDates: Set<String> = emptySet(), teacherIdOverride: Long? = null
    ) {
        viewModelScope.launch {
            val dates: List<LocalDate> = when (recurrenceType) {
                RecurrenceType.WEEKLY -> buildList {
                    var d = startDate
                    while (!d.isAfter(endDate)) { if (d.dayOfWeek.value == dayOfWeek) add(d); d = d.plusDays(1) }
                }
                RecurrenceType.DAILY  -> buildList {
                    var d = startDate; while (!d.isAfter(endDate)) { add(d); d = d.plusDays(1) }
                }
                RecurrenceType.ONCE   -> listOf(startDate)
            }
            val existing = state.value.lessons.filter { it.classId == classId }.map { it.date }.toSet()
            val baseTime = System.currentTimeMillis()
            dates.forEachIndexed { i, d ->
                val ds = d.toString()
                if (ds !in excludeDates && ds !in existing)
                    repo.addLesson(Lesson(baseTime + i, classId, ds, startTime, endTime,
                        "pending", code = genCode("L"), teacherIdOverride = teacherIdOverride))
            }
        }
    }

    // ─── Lessons — batch modify / delete ──────────────────────────────────────
    fun batchModifyLessons(
        classId: Long, fromDate: String, toDate: String,
        newStartTime: String? = null, newEndTime: String? = null,
        skipModified: Boolean = true, includeNonPending: Boolean = false
    ) {
        viewModelScope.launch {
            state.value.lessons.filter { l ->
                l.classId == classId && l.date in fromDate..toDate &&
                (includeNonPending || l.status == "pending") &&
                (!skipModified || !l.isModified)
            }.forEach { l ->
                repo.updateLesson(l.copy(
                    startTime = newStartTime ?: l.startTime,
                    endTime   = newEndTime   ?: l.endTime
                ))
            }
        }
    }

    fun batchDeleteLessons(
        classId: Long, fromDate: String, toDate: String,
        includeNonPending: Boolean = false, includeModified: Boolean = false
    ) {
        viewModelScope.launch {
            repo.deleteLessonBatch(classId, fromDate, toDate, includeNonPending, includeModified)
        }
    }

    // ─── Knowledge Points ─────────────────────────────────────────────────────
    fun addKnowledgePoint(
        grade: String, chapter: String, section: String,
        code: String, content: String
    ) {
        viewModelScope.launch {
            repo.addKnowledgePoint(KnowledgePoint(
                id       = System.currentTimeMillis(),
                grade    = grade,
                chapter  = chapter,
                section  = section,
                code     = code,
                content  = content,
                isCustom = true
            ))
        }
    }

    fun updateKnowledgePoint(kp: KnowledgePoint) {
        viewModelScope.launch { repo.updateKnowledgePoint(kp) }
    }

    fun deleteKnowledgePoint(id: Long) {
        viewModelScope.launch { repo.deleteKnowledgePoint(id) }
    }

    // ─── Backup — export ──────────────────────────────────────────────────────

    fun exportFullZip(context: Context): ByteArray? =
        backupManager(context).buildFullZip(state.value)

    fun exportFilteredZip(
        context: Context,
        teacherId: Long?,
        classId: Long?,
        fromDate: String?,
        toDate: String?
    ): ByteArray? {
        val filtered = state.value.filterForExport(teacherId, classId, fromDate, toDate)
        val filter = FilterDescription(
            teacherName = teacherId?.let { id -> state.value.teachers.find { it.id == id }?.name },
            className   = classId?.let   { id -> state.value.classes.find  { it.id == id }?.name },
            fromDate    = fromDate,
            toDate      = toDate
        )
        return backupManager(context).buildFilteredZip(filtered, filter)
    }

    fun peekImportZip(bytes: ByteArray, context: Context): ImportResult =
        backupManager(context).peekZip(bytes)

    fun commitImportZip(bytes: ByteArray, context: Context): Boolean {
        val appState = backupManager(context).extractState(bytes) ?: return false
        viewModelScope.launch { repo.mergeAll(appState) }
        return true
    }

    fun resetToSampleData() { viewModelScope.launch { repo.importAll(sampleAppState()) } }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private fun backupManager(context: Context) = BackupManager(
        context    = context,
        gson       = gson,
        appVersion = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        }.getOrDefault("")
    )
}
