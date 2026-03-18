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
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private fun genCode(prefix: String) =
    "$prefix${System.currentTimeMillis().toString().takeLast(6)}"

// ─── Gson-compatible snapshot types ──────────────────────────────────────────
// Kept flexible so old JSON exports (which still carry subjectId on schedule/
// attendance rows) can be imported and silently ignored.

private data class GsonTeacher(
    val id: Long? = null,
    val name: String? = null,
    val gender: String? = null,
    val phone: String? = null,
    val subjectIds: List<Long>? = null,
    val avatarUri: String? = null,
    val code: String? = null
)

private data class GsonClass(
    val id: Long? = null,
    val name: String? = null,
    val grade: String? = null,
    val count: Int? = null,
    val headTeacherId: Long? = null,
    val subjectId: Long? = null,
    val subject: String? = null,   // legacy field — used only to resolve subjectId
    val code: String? = null
)

// Old exports carry subjectId on schedule/attendance — we read but discard it.
private data class GsonSchedule(
    val id: Long? = null,
    val classId: Long? = null,
    val subjectId: Long? = null,   // ignored on import (Plan A)
    val teacherId: Long? = null,
    val day: Int? = null,
    val period: Int? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val code: String? = null
)

private data class GsonAttendance(
    val id: Long? = null,
    val classId: Long? = null,
    val subjectId: Long? = null,   // ignored on import (Plan A)
    val teacherId: Long? = null,
    val date: String? = null,
    val period: Int? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val topic: String? = null,
    val status: String? = null,
    val notes: String? = null,
    val attendees: List<Long>? = null,
    val code: String? = null
)

private data class GsonState(
    val subjects: List<Subject>? = null,
    val teachers: List<GsonTeacher>? = null,
    val classes: List<GsonClass>? = null,
    val students: List<Student>? = null,
    val schedule: List<GsonSchedule>? = null,
    val attendance: List<GsonAttendance>? = null
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AppRepository(app)
    private val gson: Gson = GsonBuilder().create()

    val state: StateFlow<AppState> = repo.appState
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppState())

    val scheduleFilterMode = MutableStateFlow("all")
    val scheduleFilterId   = MutableStateFlow<Long?>(null)

    val scheduleFilterTitle: StateFlow<String> = combine(
        scheduleFilterMode, scheduleFilterId, state
    ) { mode, id, st ->
        when (mode) {
            "teacher" -> st.teachers.firstOrNull { it.id == id }?.let { "${it.name}的课表" } ?: "课表"
            "student" -> st.students.firstOrNull { it.id == id }?.let { "${it.name}的课表" } ?: "课表"
            else      -> "课表"
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "课表")

    fun clearScheduleFilter() {
        scheduleFilterMode.value = "all"
        scheduleFilterId.value   = null
    }

    init {
        viewModelScope.launch { initializeData() }
    }

    private suspend fun initializeData() {
        if (!repo.isEmpty()) return

        val prefs = getApplication<Application>()
            .getSharedPreferences("app_data", Context.MODE_PRIVATE)
        val legacyJson = prefs.getString("state", null)

        val seed = if (legacyJson != null) {
            parseGsonState(legacyJson) ?: sampleAppState()
        } else {
            sampleAppState()
        }
        repo.importAll(seed)
        prefs.edit().remove("state").apply()
    }

    fun subject(id: Long?)     = state.value.subjects.find  { it.id == id }
    fun teacher(id: Long?)     = state.value.teachers.find  { it.id == id }
    fun schoolClass(id: Long?) = state.value.classes.find   { it.id == id }
    fun student(id: Long?)     = state.value.students.find  { it.id == id }

    // ─── Subjects ─────────────────────────────────────────────────────────────

    fun addSubject(name: String, color: Long, teacherId: Long?, code: String = "") {
        val c = code.ifBlank { genCode("SBJ") }
        viewModelScope.launch {
            repo.addSubject(Subject(System.currentTimeMillis(), name, color, teacherId, c))
        }
    }

    fun updateSubject(s: Subject) {
        // No cascade needed — subject name is always read fresh via FK JOIN.
        viewModelScope.launch { repo.updateSubject(s) }
    }

    fun deleteSubject(id: Long) {
        // Room FK ON DELETE SET NULL handles classes.subjectId automatically.
        viewModelScope.launch { repo.deleteSubject(id) }
    }

    // ─── Teachers ─────────────────────────────────────────────────────────────

    fun addTeacher(
        name: String,
        gender: String,
        phone: String = "",
        @Suppress("UNUSED_PARAMETER") subjectIds: List<Long> = emptyList(),
        avatarUri: String? = null,
        code: String = ""
    ) {
        val c = code.ifBlank { genCode("T") }
        viewModelScope.launch {
            repo.addTeacher(Teacher(System.currentTimeMillis(), name, gender, phone, avatarUri, c))
        }
    }

    fun updateTeacher(t: Teacher) {
        viewModelScope.launch { repo.updateTeacher(t) }
    }

    fun deleteTeacher(id: Long) {
        viewModelScope.launch { repo.deleteTeacher(id) }
    }

    // ─── Classes ──────────────────────────────────────────────────────────────
    // Changing subjectId here is all that's needed — schedule and attendance
    // automatically display the updated subject at read time.

    fun addSchoolClass(
        name: String,
        grade: String,
        count: Int,
        headTeacherId: Long?,
        subjectId: Long? = null,
        subject: String = "",      // ignored — kept for call-site compat
        code: String = ""
    ) {
        val c = code.ifBlank { genCode("C") }
        viewModelScope.launch {
            repo.addSchoolClass(
                SchoolClass(System.currentTimeMillis(), name, grade, count, headTeacherId, subjectId, c)
            )
        }
    }

    fun updateSchoolClass(c: SchoolClass) {
        // Single write — no cascade loop required.
        viewModelScope.launch { repo.updateSchoolClass(c) }
    }

    fun deleteSchoolClass(id: Long) {
        viewModelScope.launch { repo.deleteSchoolClass(id) }
    }

    // ─── Students ─────────────────────────────────────────────────────────────

    fun addStudent(
        name: String, studentNo: String, gender: String,
        grade: String, classIds: List<Long>, avatarUri: String? = null,
        code: String = ""
    ) {
        viewModelScope.launch {
            repo.addStudent(
                Student(System.currentTimeMillis(), name, studentNo, gender, grade, classIds, avatarUri)
            )
        }
    }

    fun updateStudent(s: Student) {
        viewModelScope.launch { repo.updateStudent(s) }
    }

    fun deleteStudent(id: Long) {
        viewModelScope.launch { repo.deleteStudent(id) }
    }

    // ─── Schedule ─────────────────────────────────────────────────────────────

    fun addSchedule(
        classId: Long,
        @Suppress("UNUSED_PARAMETER") subjectId: Long = 0L,  // ignored — Plan A
        teacherId: Long?,
        day: Int,
        startTime: String,
        endTime: String,
        code: String = ""
    ) {
        val c = code.ifBlank { genCode("SCH") }
        viewModelScope.launch {
            repo.addSchedule(
                Schedule(System.currentTimeMillis(), classId, teacherId, day,
                    startTime = startTime, endTime = endTime, code = c)
            )
        }
    }

    fun updateSchedule(s: Schedule) {
        viewModelScope.launch { repo.updateSchedule(s) }
    }

    fun deleteSchedule(id: Long) {
        viewModelScope.launch { repo.deleteSchedule(id) }
    }

    // ─── Attendance ───────────────────────────────────────────────────────────

    fun addAttendance(
        classId: Long,
        @Suppress("UNUSED_PARAMETER") subjectId: Long = 0L,  // ignored — Plan A
        teacherId: Long?,
        date: String,
        startTime: String,
        endTime: String,
        topic: String,
        status: String,
        notes: String,
        attendees: List<Long>,
        code: String = ""
    ) {
        val c = code.ifBlank { genCode("ATT") }
        viewModelScope.launch {
            repo.addAttendance(
                Attendance(System.currentTimeMillis(), classId, teacherId,
                    date, 0, startTime, endTime, topic, status, notes, attendees, c)
            )
        }
    }

    fun updateAttendance(a: Attendance) {
        viewModelScope.launch { repo.updateAttendance(a) }
    }

    fun deleteAttendance(id: Long) {
        viewModelScope.launch { repo.deleteAttendance(id) }
    }

    // ─── Stats ────────────────────────────────────────────────────────────────

    fun completedAttendance() = state.value.attendance.filter { it.status == "completed" }

    // ─── Export ───────────────────────────────────────────────────────────────

    fun exportFullStateJson(): String = gson.toJson(state.value)

    fun exportScheduleJson(teacherId: Long?): String {
        val list = if (teacherId == null) state.value.schedule
                   else state.value.schedule.filter { it.teacherId == teacherId }
        return gson.toJson(list)
    }

    fun exportAttendanceJson(teacherId: Long?): String {
        val list = if (teacherId == null) state.value.attendance
                   else state.value.attendance.filter { it.teacherId == teacherId }
        return gson.toJson(list)
    }

    fun exportFullBackupZip(context: Context): ByteArray? {
        return try {
        val json  = gson.toJson(state.value)
        val baos  = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            zip.putNextEntry(ZipEntry("state.json"))
            zip.write(gson.toJson(state.value).toByteArray())
            zip.closeEntry()

            val avatarDir = File(context.filesDir, "avatars")
            if (avatarDir.exists()) {
                avatarDir.listFiles()?.forEach { f ->
                    zip.putNextEntry(ZipEntry("avatars/${f.name}"))
                    zip.write(f.readBytes())
                    zip.closeEntry()
                }
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
                    entry.name == "state.json"          -> json = data.toString(Charsets.UTF_8)
                    entry.name.startsWith("avatars/")   -> avatarEntries[entry.name.removePrefix("avatars/")] = data
                }
                entry = zip.nextEntry
            }
        }
        if (json == null) return false
        val avatarDir = File(context.filesDir, "avatars").also { it.mkdirs() }
        val pathRemap = mutableMapOf<String, String>()
        avatarEntries.forEach { (name, data) ->
            val dest = File(avatarDir, name)
            dest.writeBytes(data)
            pathRemap[name] = dest.absolutePath
        }
        importMerge(json!!, pathRemap)
        } catch (_: Exception) { false }
    }

    fun importMerge(json: String, pathRemap: Map<String, String> = emptyMap()): Boolean {
        val state = parseGsonState(json, pathRemap) ?: return false
        viewModelScope.launch { repo.mergeAll(state) }
        return true
    }

    fun resetToSampleData() {
        viewModelScope.launch { repo.importAll(sampleAppState()) }
    }

    // ─── JSON parsing ─────────────────────────────────────────────────────────

    private fun parseGsonState(
        json: String,
        pathRemap: Map<String, String> = emptyMap()
    ): AppState? {
        return try {
        val raw = gson.fromJson(json, GsonState::class.java) ?: return null

        fun remapPath(old: String?): String? {
            if (old == null) return null
            val name = File(old).name
            return pathRemap[name] ?: old
        }

        val subjects = raw.subjects ?: emptyList()

        val teachers = raw.teachers?.map { gt ->
            Teacher(
                id        = gt.id ?: 0L,
                name      = gt.name ?: "",
                gender    = gt.gender ?: "男",
                phone     = gt.phone ?: "",
                avatarUri = remapPath(gt.avatarUri),
                code      = gt.code ?: ""
            )
        } ?: emptyList()

        val classes = raw.classes?.map { gc ->
            // Resolve subjectId from old exports that may only have a subject string
            val resolvedSubjectId = gc.subjectId
                ?: subjects.find { it.name == gc.subject }?.id
            SchoolClass(
                id            = gc.id ?: 0L,
                name          = gc.name ?: "",
                grade         = gc.grade ?: "",
                count         = gc.count ?: 0,
                headTeacherId = gc.headTeacherId,
                subjectId     = resolvedSubjectId,
                code          = gc.code ?: ""
            )
        } ?: emptyList()

        val students = raw.students?.map { s ->
            s.copy(avatarUri = remapPath(s.avatarUri))
        } ?: emptyList()

        // subjectId on schedule/attendance rows is intentionally ignored (Plan A)
        val schedule = raw.schedule?.map { gs ->
            Schedule(
                id        = gs.id ?: 0L,
                classId   = gs.classId ?: 0L,
                teacherId = gs.teacherId,
                day       = gs.day ?: 1,
                period    = gs.period ?: 0,
                startTime = gs.startTime ?: "",
                endTime   = gs.endTime ?: "",
                code      = gs.code ?: ""
            )
        } ?: emptyList()

        val attendance = raw.attendance?.map { ga ->
            Attendance(
                id        = ga.id ?: 0L,
                classId   = ga.classId ?: 0L,
                teacherId = ga.teacherId,
                date      = ga.date ?: "",
                period    = ga.period ?: 0,
                startTime = ga.startTime ?: "",
                endTime   = ga.endTime ?: "",
                topic     = ga.topic ?: "",
                status    = ga.status ?: "completed",
                notes     = ga.notes ?: "",
                attendees = ga.attendees ?: emptyList(),
                code      = ga.code ?: ""
            )
        } ?: emptyList()

        AppState(subjects, teachers, classes, students, schedule, attendance)
        } catch (_: Exception) { null }
    }
}
