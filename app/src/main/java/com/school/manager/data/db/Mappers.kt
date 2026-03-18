package com.school.manager.data.db

import com.school.manager.data.*
import org.json.JSONArray

// ─── List<Long> ↔ JSON string ─────────────────────────────────────────────────
// Use Android built-in JSONArray instead of Gson TypeToken<List<Long>>.
// Gson TypeToken with generics is erased by R8/ProGuard in release builds,
// causing IllegalStateException on class initialization.

private fun String.toLongList(): List<Long> =
    try {
        val arr = JSONArray(this)
        (0 until arr.length()).map { arr.getLong(it) }
    } catch (_: Exception) { emptyList() }

private fun List<Long>.toJson(): String =
    JSONArray(this).toString()

// ─── Subject ──────────────────────────────────────────────────────────────────

fun SubjectEntity.toDomain(): Subject =
    Subject(id, name, color, teacherId, code)

fun Subject.toEntity(): SubjectEntity =
    SubjectEntity(id, name, color, teacherId, code)

// ─── Teacher ──────────────────────────────────────────────────────────────────

fun TeacherEntity.toDomain(): Teacher =
    Teacher(id, name, gender, phone, avatarUri, code)

fun Teacher.toEntity(): TeacherEntity =
    TeacherEntity(id, name, gender, phone, avatarUri, code)

// ─── SchoolClass ──────────────────────────────────────────────────────────────

fun ClassEntity.toDomain(): SchoolClass =
    SchoolClass(id, name, grade, count, headTeacherId, subjectId, subject, code)

fun SchoolClass.toEntity(): ClassEntity =
    ClassEntity(id, name, grade, count, headTeacherId, subjectId, subject, code)

// ─── Student ──────────────────────────────────────────────────────────────────

fun StudentEntity.toDomain(): Student =
    Student(id, name, studentNo, gender, grade, classIdsJson.toLongList(), avatarUri)

fun Student.toEntity(): StudentEntity =
    StudentEntity(id, name, studentNo, gender, grade, classIds.toJson(), avatarUri)

// ─── Schedule ─────────────────────────────────────────────────────────────────
// subjectId nullable in DB (SET_NULL on FK delete); map null → 0L for domain model.

fun ScheduleEntity.toDomain(): Schedule =
    Schedule(id, classId, subjectId ?: 0L, teacherId, day, period, startTime, endTime, code)

fun Schedule.toEntity(): ScheduleEntity =
    ScheduleEntity(
        id, classId,
        subjectId = subjectId.takeIf { it != 0L },
        teacherId, day, period, startTime, endTime, code
    )

// ─── Attendance ───────────────────────────────────────────────────────────────

fun AttendanceEntity.toDomain(): Attendance =
    Attendance(
        id, classId, subjectId ?: 0L, teacherId,
        date, period, startTime, endTime,
        topic, status, notes,
        attendeesJson.toLongList(), code
    )

fun Attendance.toEntity(): AttendanceEntity =
    AttendanceEntity(
        id, classId,
        subjectId = subjectId.takeIf { it != 0L },
        teacherId, date, period, startTime, endTime,
        topic, status, notes,
        attendees.toJson(), code
    )
