package com.school.manager.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.google.gson.Gson
import com.school.manager.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

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

    // ─── Subjects (kept for backward compat) ─────────────────────────────────
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
    fun addTeacher(name: String, gender: String, phone: String,
                   subjectIds: List<Long> = emptyList(), avatarUri: String? = null,
                   code: String = "") {
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
    fun addClass(name: String, grade: String, count: Int, headTeacherId: Long?,
                 subject: String = "", code: String = "") {
        val c = code.ifBlank { genCode("C") }
        _state.update { it.copy(classes = it.classes +
            SchoolClass(System.currentTimeMillis(), name, grade, count, headTeacherId, subject, c)) }
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
        data class Row(val code: String, val day: String, val startTime: String, val endTime: String,
                       val subject: String, val className: String, val teacher: String)
        val src = if (teacherId != null) _state.value.schedule.filter { it.teacherId == teacherId }
                  else _state.value.schedule
        return gson.toJson(src.map { s ->
            Row(s.code,
                DAYS.getOrElse(s.day - 1) { "?" },
                s.resolvedStart(), s.resolvedEnd(),
                s.resolvedSubjectName(_state.value.subjects, _state.value.classes),
                schoolClass(s.classId)?.name ?: "?",
                teacher(s.teacherId)?.name ?: "─")
        })
    }

    /** Attendance records with resolved names */
    fun exportAttendanceJson(teacherId: Long? = null): String {
        data class Row(val code: String, val date: String, val startTime: String, val endTime: String,
                       val subject: String, val className: String, val teacher: String,
                       val topic: String, val status: String, val attendeeCount: Int, val notes: String)
        val src = if (teacherId != null) _state.value.attendance.filter { it.teacherId == teacherId }
                  else _state.value.attendance
        return gson.toJson(src.map { a ->
            Row(a.code, a.date, a.resolvedStart(), a.resolvedEnd(),
                subject(a.subjectId)?.name
                    ?: schoolClass(a.classId)?.subject?.takeIf { it.isNotBlank() } ?: "?",
                schoolClass(a.classId)?.name ?: "?",
                teacher(a.teacherId)?.name ?: "─",
                a.topic, a.status, a.attendees.size, a.notes)
        })
    }

    /**
     * Merge-import: existing records with the same [id] are updated,
     * new records (id not present) are appended. Nothing is deleted.
     */
    fun mergeImport(json: String): Boolean {
        return try {
            val raw = gson.fromJson(json, GsonState::class.java)
            _state.update { cur ->
                fun <T : Any> merge(current: List<T>, incoming: List<T>?, getId: (T) -> Long): List<T> {
                    if (incoming.isNullOrEmpty()) return current
                    val map = current.associateBy(getId).toMutableMap()
                    incoming.forEach { item -> map[getId(item)] = item }
                    return map.values.toList()
                }
                cur.copy(
                    subjects   = merge(cur.subjects,   raw.subjects)   { it.id },
                    teachers   = merge(cur.teachers,   raw.teachers)   { it.id },
                    classes    = merge(cur.classes,    raw.classes)    { it.id },
                    students   = merge(cur.students,   raw.students)   { it.id },
                    schedule   = merge(cur.schedule,   raw.schedule)   { it.id },
                    attendance = merge(cur.attendance, raw.attendance) { it.id }
                )
            }
            save()
            true
        } catch (_: Exception) { false }
    }

    // ─── ZIP 完整备份（含头像图片）────────────────────────────────────────────

    /**
     * 导出完整备份 ZIP：
     *   state.json  — 全部结构化数据
     *   avatars/    — 教师 / 学生头像原始文件
     *
     * 返回 ZIP 字节数组，失败返回 null。
     */
    fun exportFullBackupZip(context: Context): ByteArray? = try {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            // ── 1. state.json ──────────────────────────────────────────────
            zos.putNextEntry(ZipEntry("state.json"))
            zos.write(gson.toJson(_state.value).toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // ── 2. avatars/ ────────────────────────────────────────────────
            val avatarDir = File(context.filesDir, "avatars")
            if (avatarDir.exists()) {
                avatarDir.listFiles()?.forEach { file ->
                    zos.putNextEntry(ZipEntry("avatars/${file.name}"))
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }
        baos.toByteArray()
    } catch (_: Exception) { null }

    /**
     * 导入完整备份 ZIP（由 [exportFullBackupZip] 生成）：
     *   1. 提取头像图片到 app 私有目录，并重建路径映射
     *   2. 按 ID 合并导入 state.json（与 [mergeImport] 逻辑相同）
     *
     * 返回 true 表示成功。
     */
    fun importFullBackupZip(context: Context, zipBytes: ByteArray): Boolean = try {
        val avatarDir = File(context.filesDir, "avatars").also { it.mkdirs() }
        var stateJson: String? = null
        // 文件名 → 新绝对路径（用于修复 avatarUri 字段）
        val pathRemap = mutableMapOf<String, String>()

        ZipInputStream(zipBytes.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                when {
                    entry.name == "state.json" ->
                        stateJson = zis.readBytes().toString(Charsets.UTF_8)

                    entry.name.startsWith("avatars/") && !entry.isDirectory -> {
                        val name = entry.name.removePrefix("avatars/")
                        val dest = File(avatarDir, name)
                        dest.outputStream().use { out -> zis.copyTo(out) }
                        pathRemap[name] = dest.absolutePath
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        val json = stateJson ?: return false
        val raw  = gson.fromJson(json, GsonState::class.java)

        // 将旧路径中的文件名映射到新绝对路径
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
