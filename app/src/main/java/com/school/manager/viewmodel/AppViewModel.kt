package com.school.manager.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.google.gson.Gson
import com.school.manager.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// Nullable mirror of AppState for safe Gson deserialization
private data class GsonState(
    val subjects:   List<Subject>?,
    val teachers:   List<Teacher>?,
    val classes:    List<SchoolClass>?,
    val students:   List<Student>?,
    val schedule:   List<Schedule>?,
    val attendance: List<Attendance>?
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("school_state_v2", Context.MODE_PRIVATE)
    private val gson  = Gson()

    private fun loadState(): AppState {
        val json = prefs.getString("state", null) ?: return AppState()
        return try {
            val raw = gson.fromJson(json, GsonState::class.java)
            AppState(
                subjects   = raw.subjects   ?: sampleSubjects,
                teachers   = raw.teachers   ?: sampleTeachers,
                classes    = raw.classes    ?: sampleClasses,
                students   = raw.students   ?: sampleStudents,
                schedule   = raw.schedule   ?: sampleSchedule,
                attendance = raw.attendance ?: sampleAttendance
            )
        } catch (_: Exception) { AppState() }
    }

    private val _state = MutableStateFlow(loadState())
    val state = _state.asStateFlow()

    private fun save() {
        prefs.edit().putString("state", gson.toJson(_state.value)).apply()
    }

    // ─── Lookups ─────────────────────────────────────────────────────────────

    fun subject(id: Long?)     = _state.value.subjects.find  { it.id == id }
    fun teacher(id: Long?)     = _state.value.teachers.find  { it.id == id }
    fun schoolClass(id: Long?) = _state.value.classes.find   { it.id == id }
    fun student(id: Long?)     = _state.value.students.find  { it.id == id }

    // ─── Subjects ─────────────────────────────────────────────────────────────

    fun addSubject(name: String, color: Long, teacherId: Long?) {
        _state.update { it.copy(subjects = it.subjects + Subject(System.currentTimeMillis(), name, color, teacherId)) }
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

    fun addTeacher(name: String, gender: String, phone: String, subjectIds: List<Long> = emptyList(), avatarUri: String? = null) {
        _state.update { it.copy(teachers = it.teachers + Teacher(System.currentTimeMillis(), name, gender, phone, subjectIds, avatarUri)) }
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

    fun addClass(name: String, grade: String, count: Int, headTeacherId: Long?) {
        _state.update { it.copy(classes = it.classes + SchoolClass(System.currentTimeMillis(), name, grade, count, headTeacherId)) }
        save()
    }
    fun updateClass(c: SchoolClass) {
        _state.update { st -> st.copy(classes = st.classes.map { if (it.id == c.id) c else it }) }
        save()
    }
    fun deleteClass(id: Long) {
        _state.update { it.copy(classes = it.classes.filter { c -> c.id != id }) }
        save()
    }

    // ─── Students ─────────────────────────────────────────────────────────────

    fun addStudent(name: String, studentNo: String, gender: String, grade: String, classIds: List<Long>, avatarUri: String? = null) {
        _state.update { it.copy(students = it.students + Student(System.currentTimeMillis(), name, studentNo, gender, grade, classIds, avatarUri)) }
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
                    startTime: String, endTime: String) {
        _state.update { it.copy(schedule = it.schedule + Schedule(
            System.currentTimeMillis(), classId, subjectId, teacherId, day,
            startTime = startTime, endTime = endTime)) }
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

    fun addAttendance(
        classId: Long, subjectId: Long, teacherId: Long?,
        date: String, startTime: String, endTime: String,
        topic: String, status: String, notes: String, attendees: List<Long>
    ) {
        _state.update {
            it.copy(attendance = it.attendance + Attendance(
                System.currentTimeMillis(), classId, subjectId, teacherId,
                date, 0, startTime, endTime, topic, status, notes, attendees
            ))
        }
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

    // ─── Stats helpers ────────────────────────────────────────────────────────

    fun completedAttendance() = _state.value.attendance.filter { it.status == "completed" }

    // ─── Export helpers ───────────────────────────────────────────────────────

    /** Pretty-printed JSON of schedule entries (uses resolvedStart/End for correct times). */
    fun exportScheduleJson(): String {
        data class ScheduleExport(
            val id: Long, val day: String, val startTime: String, val endTime: String,
            val subject: String, val className: String, val teacher: String
        )
        val rows = _state.value.schedule.map { s ->
            ScheduleExport(
                id        = s.id,
                day       = com.school.manager.data.DAYS.getOrElse(s.day - 1) { "?" },
                startTime = s.resolvedStart(),
                endTime   = s.resolvedEnd(),
                subject   = subject(s.subjectId)?.name ?: "?",
                className = schoolClass(s.classId)?.name ?: "?",
                teacher   = teacher(s.teacherId)?.name ?: "─"
            )
        }
        return gson.toJson(rows)
    }

    /** Pretty-printed JSON of attendance records (uses resolvedStart/End for correct times). */
    fun exportAttendanceJson(): String {
        data class AttendanceExport(
            val id: Long, val date: String, val startTime: String, val endTime: String,
            val subject: String, val className: String, val teacher: String,
            val topic: String, val status: String, val attendeeCount: Int, val notes: String
        )
        val rows = _state.value.attendance.map { a ->
            AttendanceExport(
                id            = a.id,
                date          = a.date,
                startTime     = a.resolvedStart(),
                endTime       = a.resolvedEnd(),
                subject       = subject(a.subjectId)?.name ?: "?",
                className     = schoolClass(a.classId)?.name ?: "?",
                teacher       = teacher(a.teacherId)?.name ?: "─",
                topic         = a.topic,
                status        = a.status,
                attendeeCount = a.attendees.size,
                notes         = a.notes
            )
        }
        return gson.toJson(rows)
    }

    /** Full AppState serialised as JSON — suitable for import round-trip. */
    fun exportFullStateJson(): String = gson.toJson(_state.value)

    /**
     * Import from a full-state JSON string (as produced by [exportFullStateJson]).
     * Returns true on success, false if parsing failed.
     */
    fun importFullState(json: String): Boolean {
        return try {
            val raw = gson.fromJson(json, GsonState::class.java)
            _state.update { current ->
                current.copy(
                    subjects   = raw.subjects   ?: current.subjects,
                    teachers   = raw.teachers   ?: current.teachers,
                    classes    = raw.classes    ?: current.classes,
                    students   = raw.students   ?: current.students,
                    schedule   = raw.schedule   ?: current.schedule,
                    attendance = raw.attendance ?: current.attendance
                )
            }
            save()
            true
        } catch (_: Exception) { false }
    }
}
