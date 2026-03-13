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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private fun genCode(prefix: String) =
    "$prefix${System.currentTimeMillis().toString().takeLast(6)}"

// Gson-friendly snapshot (all fields nullable for safe deserialization)
private data class GsonState(
    val subjects:   List<Subject>?     = null,
    val teachers:   List<Teacher>?     = null,
    val classes:    List<SchoolClass>? = null,
    val students:   List<Student>?     = null,
    val schedule:   List<Schedule>?    = null,
    val attendance: List<Attendance>?  = null
)

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("app_data", Context.MODE_PRIVATE)
    private val gson: Gson = GsonBuilder().create()

    // 初始值用空 AppState，load() 里会覆盖
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
    fun updateSubject(s: Subject) {
        _state.update { st -> st.copy(subjects = st.subjects.map { if (it.id == s.id) s else it }) }
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

    /** Attendance records with resolved names */
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

    /** Export full backup as ZIP (state.json + avatars/) */
    fun exportFullBackupZip(context: android.content.Context): ByteArray? {
        return try {
            val out = ByteArrayOutputStream()
            ZipOutputStream(out).use { zip ->
                // state.json
                zip.putNextEntry(ZipEntry("state.json"))
                zip.write(gson.toJson(_state.value).toByteArray())
                zip.closeEntry()
                // avatars/
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

    /** Import full backup ZIP */
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
    // 使用 commit()（同步写入）代替 apply()（异步写入），
    // 确保强制退出前数据已写入磁盘，彻底避免数据丢失。
    private fun save() {
        prefs.edit().putString("state", gson.toJson(_state.value)).commit()
    }

    // load()：用 GsonState（全字段可空）解析，避免 AppState 默认构造函数
    // 用 sampleData 覆盖已保存数据的问题。
    // 若 prefs 中无数据（首次安装），填入示例数据并立即保存。
    private fun load() {
        val json = prefs.getString("state", null)
        if (json == null) {
            // 首次安装：填入示例数据
            _state.value = sampleAppState()
            save()
            return
        }
        try {
            val raw = gson.fromJson(json, GsonState::class.java)
            _state.value = AppState(
                subjects   = raw.subjects   ?: emptyList(),
                teachers   = raw.teachers   ?: emptyList(),
                classes    = raw.classes    ?: emptyList(),
                students   = raw.students   ?: emptyList(),
                schedule   = raw.schedule   ?: emptyList(),
                attendance = raw.attendance ?: emptyList()
            )
        } catch (_: Exception) {
            // JSON 损坏时 fallback 到示例数据，并重新保存
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

                val newTeachers = merge(cur.teachers, raw.teachers?.map { t ->
                    t.copy(avatarUri = remapPath(t.avatarUri))
                }, Teacher::id)

                val newStudents = merge(cur.students, raw.students?.map { s ->
                    s.copy(avatarUri = remapPath(s.avatarUri))
                }, Student::id)

                cur.copy(
                    subjects   = merge(cur.subjects,   raw.subjects,   Subject::id),
                    teachers   = newTeachers,
                    classes    = merge(cur.classes,    raw.classes,    SchoolClass::id),
                    students   = newStudents,
                    schedule   = merge(cur.schedule,   raw.schedule,   Schedule::id),
                    attendance = merge(cur.attendance, raw.attendance, Attendance::id)
                )
            }
            save()
            true
        } catch (_: Exception) { false }
    }

    /** Alias used by ExportScreen */
    fun mergeImport(json: String, pathRemap: Map<String, String> = emptyMap()): Boolean =
        importMerge(json, pathRemap)

    // ─── Reset to factory sample data ────────────────────────────────────────
    fun resetToSampleData() {
        _state.value = sampleAppState()
        save()
    }
}
