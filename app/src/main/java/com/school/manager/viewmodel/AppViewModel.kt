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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AppRepository(app)
    internal val gson: Gson = GsonBuilder().create()

    val state: StateFlow<AppState> = repo.appState
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppState())

    init { viewModelScope.launch { if (repo.isEmpty()) repo.importAll(sampleAppState()) } }

    // ─── Lookups ──────────────────────────────────────────────────────────────
    fun subject(id: Long?)     = state.value.subjects.find { it.id == id }
    fun teacher(id: Long?)     = state.value.teachers.find { it.id == id }
    fun schoolClass(id: Long?) = state.value.classes.find  { it.id == id }
    fun student(id: Long?)     = state.value.students.find { it.id == id }

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
        teacherIdOverride: Long? = null
    ) {
        viewModelScope.launch {
            repo.addLesson(Lesson(System.currentTimeMillis(), classId, date,
                startTime, endTime, status, topic, notes, attendees, false,
                code.ifBlank { genCode("L") }, teacherIdOverride))
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
        classId: Long,
        recurrenceType: RecurrenceType,
        startDate: LocalDate,
        endDate: LocalDate,
        dayOfWeek: Int = 1,
        startTime: String,
        endTime: String,
        excludeDates: Set<String> = emptySet(),
        teacherIdOverride: Long? = null
    ) {
        viewModelScope.launch {
            val dates: List<LocalDate> = when (recurrenceType) {
                RecurrenceType.WEEKLY -> buildList {
                    var d = startDate
                    while (!d.isAfter(endDate)) {
                        if (d.dayOfWeek.value == dayOfWeek) add(d)
                        d = d.plusDays(1)
                    }
                }
                RecurrenceType.DAILY  -> buildList {
                    var d = startDate
                    while (!d.isAfter(endDate)) { add(d); d = d.plusDays(1) }
                }
                RecurrenceType.ONCE   -> listOf(startDate)
            }
            val existing = state.value.lessons
                .filter { it.classId == classId }.map { it.date }.toSet()
            val baseTime = System.currentTimeMillis()
            dates.forEachIndexed { index, d ->
                val dateStr = d.toString()
                if (dateStr !in excludeDates && dateStr !in existing) {
                    repo.addLesson(Lesson(
                        id = baseTime + index, classId = classId, date = dateStr,
                        startTime = startTime, endTime = endTime, status = "pending",
                        code = genCode("L"), teacherIdOverride = teacherIdOverride
                    ))
                }
            }
        }
    }

    // ─── Lessons — batch modify ───────────────────────────────────────────────
    fun batchModifyLessons(
        classId: Long, fromDate: String, toDate: String,
        newStartTime: String? = null, newEndTime: String? = null,
        skipModified: Boolean = true, includeNonPending: Boolean = false
    ) {
        viewModelScope.launch {
            state.value.lessons.filter { l ->
                l.classId == classId &&
                l.date >= fromDate && l.date <= toDate &&
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

    // ─── Lessons — batch delete ───────────────────────────────────────────────
    fun batchDeleteLessons(
        classId: Long, fromDate: String, toDate: String,
        includeNonPending: Boolean = false, includeModified: Boolean = false
    ) {
        viewModelScope.launch {
            repo.deleteLessonBatch(classId, fromDate, toDate, includeNonPending, includeModified)
        }
    }

    // ─── Export ───────────────────────────────────────────────────────────────
    fun exportFullStateJson(): String = gson.toJson(state.value)

    fun exportLessonsJson(teacherId: Long?): String {
        val cids = if (teacherId == null) null
                   else state.value.classes.filter { it.headTeacherId == teacherId }.map { it.id }.toSet()
        val list = if (cids == null) state.value.lessons
                   else state.value.lessons.filter { it.classId in cids }
        return gson.toJson(list)
    }

    fun exportFullBackupZip(context: Context): ByteArray? {
        return try {
            val baos = ByteArrayOutputStream()
            ZipOutputStream(baos).use { zip ->
                zip.putNextEntry(ZipEntry("state.json"))
                zip.write(gson.toJson(state.value).toByteArray())
                zip.closeEntry()
                File(context.filesDir, "avatars").takeIf { it.exists() }
                    ?.listFiles()?.forEach { f ->
                        zip.putNextEntry(ZipEntry("avatars/${f.name}"))
                        zip.write(f.readBytes())
                        zip.closeEntry()
                    }
            }
            baos.toByteArray()
        } catch (_: Exception) { null }
    }

    fun importFullBackupZip(context: Context, bytes: ByteArray): Boolean {
        return try {
            var json: String? = null
            val avatarEntries = mutableMapOf<String, ByteArray>()
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val data = zip.readBytes()
                    when {
                        entry.name == "state.json"        -> json = data.toString(Charsets.UTF_8)
                        entry.name.startsWith("avatars/") ->
                            avatarEntries[entry.name.removePrefix("avatars/")] = data
                    }
                    entry = zip.nextEntry
                }
            }
            if (json == null) return false
            val avatarDir = File(context.filesDir, "avatars").also { it.mkdirs() }
            val pathRemap = avatarEntries.mapValues { (name, data) ->
                File(avatarDir, name).also { it.writeBytes(data) }.absolutePath
            }
            importMerge(json!!, pathRemap)
        } catch (_: Exception) { false }
    }

    fun importMerge(json: String, pathRemap: Map<String, String> = emptyMap()): Boolean {
        val parsed = parseGsonState(json, gson, pathRemap) ?: return false
        viewModelScope.launch { repo.mergeAll(parsed) }
        return true
    }

    fun resetToSampleData() { viewModelScope.launch { repo.importAll(sampleAppState()) } }
}
