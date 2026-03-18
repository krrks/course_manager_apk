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
// Kept for JSON import / export and for migrating legacy SharedPreferences data.
// All fields nullable for safe deserialization of old backups.

private data class GsonTeacher(
    val id: Long? = null,
    val name: String? = null,
    val gender: String? = null,
    val phone: String? = null,
    val subjectIds: List<Long>? = null,   // legacy — ignored on load
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
    val subject: String? = null,
    val code: String? = null
)

private data class GsonState(
    val subjects: List<Subject>? = null,
    val teachers: List<GsonTeacher>? = null,
    val classes: List<GsonClass>? = null,
    val students: List<Student>? = null,
    val schedule: List<Schedule>? = null,
    val attendance: List<Attendance>? = null
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AppRepository(app)
    private val gson: Gson = GsonBuilder().create()

    // ─── Live state — all UI screens collect this unchanged ───────────────────
    val state: StateFlow<AppState> = repo.appState
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppState())

    // ─── Schedule filter ──────────────────────────────────────────────────────
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

    // ─── Init: migrate legacy SharedPreferences → Room, or seed sample data ──
    init {
        viewModelScope.launch { initializeData() }
    }

    private suspend fun initializeData() {
        if (!repo.isEmpty()) return  // DB already populated — nothing to do

        val prefs = getApplication<Application>()
            .getSharedPreferences("app_data", Context.MODE_PRIVATE)
        val legacyJson = prefs.getString("state", null)

        val seed = if (legacyJson != null) {
            parseGsonState(legacyJson) ?: sampleAppState()
        } else {
            sampleAppState()
        }
        repo.importAll(seed)
        // Clear the old SharedPreferences entry to avoid re-migration on next launch
        prefs.edit().remove("state").apply()
    }

    // ─── Lookups (synchronous, from in-memory StateFlow) ─────────────────────
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
        viewModelScope.launch {
            repo.updateSubject(s)
            // Class.subject string is a display fallback; keep it in sync so
            // legacy code paths (attndance form, schedule form) still resolve correctly.
            val updatedClasses = state.value.classes.filter { it.subjectId == s.id }
            updatedClasses.forEach { cls ->
                repo.updateSchoolClass(cls.copy(subject = s.name))
            }
        }
    }

    fun deleteSubject(id: Long) {
        viewModelScope.launch { repo.deleteSubject(id) }
        // Room FK ON DELETE SET_NULL automatically nullifies:
        //   classes.subjectId, schedule.subjectId, attendance.subjectId
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
        // Room FK ON DELETE SET_NULL automatically nullifies:
        //   classes.headTeacherId, schedule.teacherId, attendance.teacherId
    }

    // ─── Classes ──────────────────────────────────────────────────────────────

    fun addSchoolClass(
        name: String,
        grade: String,
        count: Int,
        headTeacherId: Long?,
        subjectId: Long? = null,
        subject: String = "",
        code: String = ""
    ) {
        val c = code.ifBlank { genCode("C") }
        val subjectName = subject.ifBlank {
            state.value.subjects.find { it.id == subjectId }?.name ?: ""
        }
        viewModelScope.launch {
            repo.addSchoolClass(
                SchoolClass(System.currentTimeMillis(), name, grade, count,
                    headTeacherId, subjectId, subjectName, c)
            )
        }
    }

    fun updateSchoolClass(c: SchoolClass) {
        // Keep subject display string in sync with subjectId FK.
        // This does NOT delete+reinsert (repo.updateSchoolClass calls @Update),
        // so schedule rows for this class are never cascade-deleted.
        val resolved = c.subjectId?.let { sid ->
            state.value.subjects.find { it.id == sid }?.name ?: c.subject
        } ?: c.subject
        viewModelScope.launch {
            repo.updateSchoolClass(c.copy(subject = resolved))
        }
    }

    fun deleteSchoolClass(id: Long) {
        viewModelScope.launch { repo.deleteSchoolClass(id) }
        // Room FK ON DELETE CASCADE automatically deletes:
        //   all schedule rows with this classId
        //   all attendance rows with this classId
    }

    // ─── Students ─────────────────────────────────────────────────────────────

    fun addStudent(
        name: String, studentNo: String, gender: String,
        grade: String, classIds: List<Long>, avatarUri: String? = null
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
        classId: Long, subjectId: Long, teacherId: Long?, day: Int,
        startTime: String, endTime: String, code: String = ""
    ) {
        val c = code.ifBlank { genCode("SCH") }
        viewModelScope.launch {
            repo.addSchedule(
                Schedule(System.currentTimeMillis(), classId, subjectId,
                    teacherId, day, startTime = startTime, endTime = endTime, code = c)
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
        classId: Long, subjectId: Long, teacherId: Long?,
        date: String, startTime: String, endTime: String,
        topic: String, status: String, notes: String,
        attendees: List<Long>, code: String = ""
    ) {
        val c = code.ifBlank { genCode("ATT") }
        viewModelScope.launch {
            repo.addAttendance(
                Attendance(System.currentTimeMillis(), classId, subjectId, teacherId,
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

    fun exportFullBackupZip(context: Context): ByteArray? = try {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            zip.putNextEntry(ZipEntry("state.json"))
            zip.write(exportFullStateJson().toByteArray())
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

    // ─── Import ───────────────────────────────────────────────────────────────

    /** Parse JSON → upsert all records (merge, not full replace). Returns false on parse error. */
    fun importMerge(json: String, pathRemap: Map<String, String> = emptyMap()): Boolean {
        if (json.isBlank()) return false
        val parsed = try { parseGsonState(json, pathRemap) } catch (_: Exception) { null }
            ?: return false
        viewModelScope.launch { repo.mergeAll(parsed) }
        return true
    }

    fun mergeImport(json: String, pathRemap: Map<String, String> = emptyMap()): Boolean =
        importMerge(json, pathRemap)

    fun importFullBackupZip(context: Context, bytes: ByteArray): Boolean = try {
        var stateJson: String? = null
        val avatarEntries = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                when {
                    entry.name == "state.json" ->
                        stateJson = zip.readBytes().toString(Charsets.UTF_8)
                    entry.name.startsWith("avatars/") && !entry.isDirectory ->
                        avatarEntries[File(entry.name).name] = zip.readBytes()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        val json = stateJson ?: return false
        val avatarDir = File(context.filesDir, "avatars").also { it.mkdirs() }
        val pathRemap = mutableMapOf<String, String>()
        avatarEntries.forEach { (name, data) ->
            val dest = File(avatarDir, name)
            dest.writeBytes(data)
            pathRemap[name] = dest.absolutePath
        }
        importMerge(json, pathRemap)
    } catch (_: Exception) { false }

    fun resetToSampleData() {
        viewModelScope.launch { repo.importAll(sampleAppState()) }
    }

    // ─── JSON parsing ─────────────────────────────────────────────────────────

    private fun parseGsonState(
        json: String,
        pathRemap: Map<String, String> = emptyMap()
    ): AppState? = try {
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
            val resolvedSubjectId = gc.subjectId
                ?: subjects.find { it.name == gc.subject }?.id
            val resolvedSubjectName = gc.subject?.ifBlank { null }
                ?: subjects.find { it.id == resolvedSubjectId }?.name ?: ""
            SchoolClass(
                id            = gc.id ?: 0L,
                name          = gc.name ?: "",
                grade         = gc.grade ?: "",
                count         = gc.count ?: 0,
                headTeacherId = gc.headTeacherId,
                subjectId     = resolvedSubjectId,
                subject       = resolvedSubjectName,
                code          = gc.code ?: ""
            )
        } ?: emptyList()

        val students = raw.students?.map { s ->
            s.copy(avatarUri = remapPath(s.avatarUri))
        } ?: emptyList()

        AppState(
            subjects   = subjects,
            teachers   = teachers,
            classes    = classes,
            students   = students,
            schedule   = raw.schedule   ?: emptyList(),
            attendance = raw.attendance ?: emptyList()
        )
    } catch (_: Exception) { null }
}
