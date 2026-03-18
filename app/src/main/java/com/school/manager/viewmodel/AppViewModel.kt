package com.school.manager.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.school.manager.data.*
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

// ─── Gson-friendly snapshot ───────────────────────────────────────────────────
// All fields nullable for safe deserialization.
// Legacy fields (subjectIds on Teacher, subject String on SchoolClass) kept
// so old backups can still be loaded without crashing.
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
    val subjectId: Long? = null,          // new FK
    val subject: String? = null,          // legacy string — kept for migration
    val code: String? = null
)

private data class GsonState(
    val subjects:   List<Subject>?     = null,
    val teachers:   List<GsonTeacher>? = null,
    val classes:    List<GsonClass>?   = null,
    val students:   List<Student>?     = null,
    val schedule:   List<Schedule>?    = null,
    val attendance: List<Attendance>?  = null
)

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("app_data", Context.MODE_PRIVATE)
    private val gson: Gson = GsonBuilder().create()

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    // ─── Schedule filter ──────────────────────────────────────────────────────
    val scheduleFilterMode = MutableStateFlow("all")
    val scheduleFilterId   = MutableStateFlow<Long?>(null)

    val scheduleFilterTitle: StateFlow<String> = combine(
        scheduleFilterMode, scheduleFilterId, _state
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

    init { load() }

    // ─── Lookups ─────────────────────────────────────────────────────────────
    fun subject(id: Long?)     = _state.value.subjects.find  { it.id == id }
    fun teacher(id: Long?)     = _state.value.teachers.find  { it.id == id }
    fun schoolClass(id: Long?) = _state.value.classes.find   { it.id == id }
    fun student(id: Long?)     = _state.value.students.find  { it.id == id }

    // ─── Subjects ─────────────────────────────────────────────────────────────
    fun addSubject(name: String, color: Long, teacherId: Long?) {
        _state.update { it.copy(subjects = it.subjects +
            Subject(System.currentTimeMillis(), name, color, teacherId)) }
        save()
    }

    /** Update subject and cascade name into SchoolClass.subject for display fallback */
    fun updateSubject(s: Subject) {
        _state.update { st ->
            // Cascade: keep SchoolClass.subject string in sync for legacy display
            val updatedClasses = st.classes.map { cls ->
                if (cls.subjectId == s.id) cls.copy(subject = s.name) else cls
            }
            st.copy(
                subjects = st.subjects.map { if (it.id == s.id) s else it },
                classes  = updatedClasses
            )
        }
        save()
    }

    fun deleteSubject(id: Long) {
        _state.update { it.copy(subjects = it.subjects.filter { s -> s.id != id }) }
        save()
    }

    // ─── Teachers ─────────────────────────────────────────────────────────────
    fun addTeacher(
        name: String,
        gender: String,
        phone: String = "",
        @Suppress("UNUSED_PARAMETER") subjectIds: List<Long> = emptyList(), // kept for call-site compat
        avatarUri: String? = null,
        code: String = ""
    ) {
        val c = code.ifBlank { genCode("T") }
        _state.update { it.copy(teachers = it.teachers +
            Teacher(System.currentTimeMillis(), name, gender, phone, avatarUri, c)) }
        save()
    }

    fun updateTeacher(t: Teacher) {
        _state.update { st -> st.copy(teachers = st.teachers.map { if (it.id == t.id) t else it }) }
        save()
    }

    fun deleteTeacher(id: Long) {
        _state.update { it.copy(teachers = it.teachers.filter { t -> t.id != id }) }
        save()
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
        // Resolve display string from subjectId if not provided
        val subjectName = subject.ifBlank {
            _state.value.subjects.find { it.id == subjectId }?.name ?: ""
        }
        _state.update { it.copy(classes = it.classes +
            SchoolClass(System.currentTimeMillis(), name, grade, count,
                headTeacherId, subjectId, subjectName, c)) }
        save()
    }

    fun updateSchoolClass(c: SchoolClass) {
        // Keep subject string in sync with subjectId
        val resolved = c.subjectId?.let { sid ->
            _state.value.subjects.find { it.id == sid }?.name ?: c.subject
        } ?: c.subject
        _state.update { st -> st.copy(classes = st.classes.map {
            if (it.id == c.id) c.copy(subject = resolved) else it
        }) }
        save()
    }

    fun deleteSchoolClass(id: Long) {
        _state.update { it.copy(classes = it.classes.filter { c -> c.id != id }) }
        save()
    }

    // ─── Students ─────────────────────────────────────────────────────────────
    fun addStudent(name: String, studentNo: String, gender: String, grade: String,
                   classIds: List<Long>, avatarUri: String? = null) {
        _state.update { it.copy(students = it.students +
            Student(System.currentTimeMillis(), name, studentNo, gender, grade, classIds, avatarUri)) }
        save()
    }

    fun updateStudent(s: Student) {
        _state.update { st -> st.copy(students = st.students.map { if (it.id == s.id) s else it }) }
        save()
    }

    fun deleteStudent(id: Long) {
        _state.update { it.copy(students = it.students.filter { s -> s.id != id }) }
        save()
    }

    // ─── Schedule ─────────────────────────────────────────────────────────────
    fun addSchedule(classId: Long, subjectId: Long, teacherId: Long?, day: Int,
                    startTime: String, endTime: String, code: String = "") {
        val c = code.ifBlank { genCode("SCH") }
        _state.update { it.copy(schedule = it.schedule +
            Schedule(System.currentTimeMillis(), classId, subjectId, teacherId, day,
                startTime = startTime, endTime = endTime, code = c)) }
        save()
    }

    fun updateSchedule(s: Schedule) {
        _state.update { st -> st.copy(schedule = st.schedule.map { if (it.id == s.id) s else it }) }
        save()
    }

    fun deleteSchedule(id: Long) {
        _state.update { it.copy(schedule = it.schedule.filter { s -> s.id != id }) }
        save()
    }

    // ─── Attendance ───────────────────────────────────────────────────────────
    fun addAttendance(classId: Long, subjectId: Long, teacherId: Long?,
                      date: String, startTime: String, endTime: String,
                      topic: String, status: String, notes: String,
                      attendees: List<Long>, code: String = "") {
        val c = code.ifBlank { genCode("ATT") }
        _state.update { it.copy(attendance = it.attendance +
            Attendance(System.currentTimeMillis(), classId, subjectId, teacherId,
                date, 0, startTime, endTime, topic, status, notes, attendees, c)) }
        save()
    }

    fun updateAttendance(a: Attendance) {
        _state.update { st -> st.copy(attendance = st.attendance.map { if (it.id == a.id) a else it }) }
        save()
    }

    fun deleteAttendance(id: Long) {
        _state.update { it.copy(attendance = it.attendance.filter { a -> a.id != id }) }
        save()
    }

    // ─── Stats ────────────────────────────────────────────────────────────────
    fun completedAttendance() = _state.value.attendance.filter { it.status == "completed" }

    // ─── Export helpers ───────────────────────────────────────────────────────
    fun exportFullStateJson(): String = gson.toJson(_state.value)

    fun exportScheduleJson(teacherId: Long?): String {
        val st = _state.value
        val list = st.schedule
            .filter { teacherId == null || it.teacherId == teacherId }
            .map { s -> mapOf(
                "id"        to s.id,
                "code"      to s.code,
                "day"       to s.day,
                "startTime" to s.startTime,
                "endTime"   to s.endTime,
                "class"     to (st.classes.find { it.id == s.classId }?.name ?: ""),
                "subject"   to (st.subjects.find { it.id == s.subjectId }?.name ?: ""),
                "teacher"   to (st.teachers.find { it.id == s.teacherId }?.name ?: "")
            )}
        return gson.toJson(list)
    }

    fun exportAttendanceJson(teacherId: Long?): String {
        val st = _state.value
        val list = st.attendance
            .filter { teacherId == null || it.teacherId == teacherId }
            .map { a -> mapOf(
                "id"        to a.id,
                "code"      to a.code,
                "date"      to a.date,
                "startTime" to a.startTime,
                "endTime"   to a.endTime,
                "topic"     to a.topic,
                "status"    to a.status,
                "notes"     to a.notes,
                "class"     to (st.classes.find { it.id == a.classId }?.name ?: ""),
                "subject"   to (st.subjects.find { it.id == a.subjectId }?.name ?: ""),
                "teacher"   to (st.teachers.find { it.id == a.teacherId }?.name ?: ""),
                "attendees" to a.attendees.mapNotNull { sid -> st.students.find { it.id == sid }?.name }
            )}
        return gson.toJson(list)
    }

    fun exportFullBackupZip(context: android.content.Context): ByteArray? {
        return try {
            val out = ByteArrayOutputStream()
            ZipOutputStream(out).use { zip ->
                zip.putNextEntry(ZipEntry("state.json"))
                zip.write(gson.toJson(_state.value).toByteArray())
                zip.closeEntry()
                val avatarDir = File(context.filesDir, "avatars")
                if (avatarDir.exists()) {
                    avatarDir.listFiles()?.forEach { file ->
                        zip.putNextEntry(ZipEntry("avatars/${file.name}"))
                        zip.write(file.readBytes())
                        zip.closeEntry()
                    }
                }
            }
            out.toByteArray()
        } catch (_: Exception) { null }
    }

    fun importFullBackupZip(context: android.content.Context, bytes: ByteArray): Boolean {
        return try {
            var stateJson: String? = null
            val avatarEntries = mutableMapOf<String, ByteArray>()
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    when {
                        entry.name == "state.json" -> {
                            stateJson = zip.readBytes().toString(Charsets.UTF_8)
                        }
                        entry.name.startsWith("avatars/") && !entry.isDirectory -> {
                            avatarEntries[File(entry.name).name] = zip.readBytes()
                        }
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
    }

    // ─── Persistence ─────────────────────────────────────────────────────────
    private fun save() {
        prefs.edit().putString("state", gson.toJson(_state.value)).commit()
    }

    private fun load() {
        val json = prefs.getString("state", null)
        if (json == null) {
            _state.value = sampleAppState()
            save()
            return
        }
        try {
            val raw = gson.fromJson(json, GsonState::class.java)
            val subjects = raw.subjects ?: emptyList()

            // Convert GsonTeacher → Teacher (drop legacy subjectIds)
            val teachers = raw.teachers?.map { gt ->
                Teacher(
                    id        = gt.id ?: 0L,
                    name      = gt.name ?: "",
                    gender    = gt.gender ?: "男",
                    phone     = gt.phone ?: "",
                    avatarUri = gt.avatarUri,
                    code      = gt.code ?: ""
                )
            } ?: emptyList()

            // Convert GsonClass → SchoolClass; migrate legacy subject string → subjectId
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

            _state.value = AppState(
                subjects   = subjects,
                teachers   = teachers,
                classes    = classes,
                students   = raw.students   ?: emptyList(),
                schedule   = raw.schedule   ?: emptyList(),
                attendance = raw.attendance ?: emptyList()
            )
        } catch (_: Exception) {
            _state.value = sampleAppState()
            save()
        }
    }

    // ─── Import / merge ───────────────────────────────────────────────────────
    fun importMerge(json: String, pathRemap: Map<String, String> = emptyMap()): Boolean {
        return try {
            if (json.isBlank()) return false
            val raw = gson.fromJson(json, GsonState::class.java)

            fun remapPath(old: String?): String? {
                if (old == null) return null
                val name = File(old).name
                return pathRemap[name] ?: old
            }

            _state.update { cur ->
                fun <T : Any> merge(existing: List<T>, incoming: List<T>?, getId: (T) -> Long): List<T> {
                    if (incoming.isNullOrEmpty()) return existing
                    val map = existing.associateBy(getId).toMutableMap()
                    incoming.forEach { map[getId(it)] = it }
                    return map.values.toList()
                }

                val incomingSubjects = raw.subjects ?: emptyList()

                val newTeachers = merge(cur.teachers, raw.teachers?.map { gt ->
                    Teacher(
                        id        = gt.id ?: 0L,
                        name      = gt.name ?: "",
                        gender    = gt.gender ?: "男",
                        phone     = gt.phone ?: "",
                        avatarUri = remapPath(gt.avatarUri),
                        code      = gt.code ?: ""
                    )
                }, Teacher::id)

                val newClasses = merge(cur.classes, raw.classes?.map { gc ->
                    val resolvedSubjectId = gc.subjectId
                        ?: incomingSubjects.find { it.name == gc.subject }?.id
                    val resolvedSubjectName = gc.subject?.ifBlank { null }
                        ?: incomingSubjects.find { it.id == resolvedSubjectId }?.name ?: ""
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
                }, SchoolClass::id)

                val newStudents = merge(cur.students, raw.students?.map { s ->
                    s.copy(avatarUri = remapPath(s.avatarUri))
                }, Student::id)

                cur.copy(
                    subjects   = merge(cur.subjects, incomingSubjects, Subject::id),
                    teachers   = newTeachers,
                    classes    = newClasses,
                    students   = newStudents,
                    schedule   = merge(cur.schedule,   raw.schedule,   Schedule::id),
                    attendance = merge(cur.attendance, raw.attendance, Attendance::id)
                )
            }
            save()
            true
        } catch (_: Exception) { false }
    }

    fun mergeImport(json: String, pathRemap: Map<String, String> = emptyMap()): Boolean =
        importMerge(json, pathRemap)

    fun resetToSampleData() {
        _state.value = sampleAppState()
        save()
    }
}
