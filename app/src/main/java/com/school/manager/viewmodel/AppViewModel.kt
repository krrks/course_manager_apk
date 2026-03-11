package com.school.manager.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.school.manager.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

private fun genCode(prefix: String) =
    "$prefix${System.currentTimeMillis().toString().takeLast(6)}"

// Gson-friendly snapshot (all fields nullable for safe deserialization)
private data class GsonState(
    val subjects:   List<Subject>?    = null,
    val teachers:   List<Teacher>?    = null,
    val classes:    List<SchoolClass>? = null,
    val students:   List<Student>?    = null,
    val schedule:   List<Schedule>?   = null,
    val attendance: List<Attendance>? = null
)

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("app_data", Context.MODE_PRIVATE)
    private val gson: Gson = GsonBuilder().create()

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    // ─── Schedule filter ──────────────────────────────────────────────────────
    val scheduleFilterMode = MutableStateFlow("all")   // "all" | "teacher" | "student"
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
    fun updateSubject(s: Subject) {
        _state.update { st -> st.copy(subjects = st.subjects.map { if (it.id == s.id) s else it }) }
        save()
    }
    fun deleteSubject(id: Long) {
        _state.update { it.copy(subjects = it.subjects.filter { s -> s.id != id }) }
        save()
    }

    // ─── Teachers ─────────────────────────────────────────────────────────────
    // Teacher(id, name, gender, phone, subjectIds, avatarUri, code)
    fun addTeacher(
        name: String,
        gender: String,
        phone: String = "",
        subjectIds: List<Long> = emptyList(),
        avatarUri: String? = null,
        code: String = ""
    ) {
        val c = code.ifBlank { genCode("T") }
        _state.update { it.copy(teachers = it.teachers +
            Teacher(System.currentTimeMillis(), name, gender, phone, subjectIds, avatarUri, c)) }
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
    // SchoolClass(id, name, grade, count, headTeacherId, subject, code)
    fun addSchoolClass(
        name: String,
        grade: String,
        count: Int,
        headTeacherId: Long?,
        subject: String = "",
        code: String = ""
    ) {
        val c = code.ifBlank { genCode("C") }
        _state.update { it.copy(classes = it.classes +
            SchoolClass(System.currentTimeMillis(), name, grade, count, headTeacherId, subject, c)) }
        save()
    }
    fun updateSchoolClass(c: SchoolClass) {
        _state.update { st -> st.copy(classes = st.classes.map { if (it.id == c.id) c else it }) }
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

    /** Full AppState JSON — supports merge import */
    fun exportFullStateJson(): String = gson.toJson(_state.value)

    /** Schedule records with resolved names */
    fun exportScheduleJson(teacherId: Long? = null): String {
        val st = _state.value
        val rows = st.schedule
            .filter { teacherId == null || it.teacherId == teacherId }
            .map { s ->
                mapOf(
                    "id"        to s.id,
                    "class"     to (st.classes.find { it.id == s.classId }?.name ?: ""),
                    "subject"   to (st.subjects.find { it.id == s.subjectId }?.name ?: ""),
                    "teacher"   to (st.teachers.find { it.id == s.teacherId }?.name ?: ""),
                    "day"       to s.day,
                    "startTime" to s.startTime,
                    "endTime"   to s.endTime
                )
            }
        return gson.toJson(rows)
    }

    /** Attendance records with resolved names */
    fun exportAttendanceJson(teacherId: Long? = null): String {
        val st = _state.value
        val rows = st.attendance
            .filter { teacherId == null || it.teacherId == teacherId }
            .map { a ->
                mapOf(
                    "id"        to a.id,
                    "date"      to a.date,
                    "class"     to (st.classes.find { it.id == a.classId }?.name ?: ""),
                    "subject"   to (st.subjects.find { it.id == a.subjectId }?.name ?: ""),
                    "teacher"   to (st.teachers.find { it.id == a.teacherId }?.name ?: ""),
                    "topic"     to a.topic,
                    "status"    to a.status,
                    "notes"     to a.notes,
                    "attendees" to a.attendees.size
                )
            }
        return gson.toJson(rows)
    }

    // ─── Persistence ─────────────────────────────────────────────────────────
    private fun save() {
        viewModelScope.launch {
            prefs.edit().putString("state", gson.toJson(_state.value)).apply()
        }
    }

    private fun load() {
        val json = prefs.getString("state", null) ?: return
        try {
            val type = object : TypeToken<AppState>() {}.type
            val loaded: AppState = gson.fromJson(json, type)
            _state.value = loaded
        } catch (_: Exception) { /* ignore corrupt data */ }
    }

    // ─── Import / merge ───────────────────────────────────────────────────────
    fun importMerge(json: String, pathRemap: Map<String, String> = emptyMap()): Boolean = try {
        if (json.isBlank()) return false
        val raw  = gson.fromJson(json, GsonState::class.java)

        fun remapPath(old: String?): String? {
            if (old == null) return null
            val name = File(old).name
            return pathRemap[name] ?: old
        }

        _state.update { cur ->
            fun <T : Any> merge(current: List<T>, incoming: List<T>?, getId: (T) -> Long): List<T> {
                if (incoming.isNullOrEmpty()) return current
                val map = current.associateBy(getId).toMutableMap()
                incoming.forEach { map[getId(it)] = it }
                return map.values.toList()
            }
            val teachers = raw.teachers?.map { it.copy(avatarUri = remapPath(it.avatarUri)) }
            val students = raw.students?.map { it.copy(avatarUri = remapPath(it.avatarUri)) }
            cur.copy(
                subjects   = merge(cur.subjects,   raw.subjects)   { it.id },
                teachers   = merge(cur.teachers,   teachers)       { it.id },
                classes    = merge(cur.classes,    raw.classes)    { it.id },
                students   = merge(cur.students,   students)       { it.id },
                schedule   = merge(cur.schedule,   raw.schedule)   { it.id },
                attendance = merge(cur.attendance, raw.attendance) { it.id }
            )
        }
        save()
        true
    } catch (_: Exception) { false }
}
