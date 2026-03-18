package com.school.manager.data.db

import com.school.manager.data.*
import org.json.JSONArray

// ─── List<Long> ↔ JSON string ─────────────────────────────────────────────────

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
// `subject` string column removed in v3 migration; only subjectId FK remains.

fun ClassEntity.toDomain(): SchoolClass =
    SchoolClass(id, name, grade, count, headTeacherId, subjectId, code)

fun SchoolClass.toEntity(): ClassEntity =
    ClassEntity(id, name, grade, count, headTeacherId, subjectId, code)

// ─── Student ──────────────────────────────────────────────────────────────────

fun StudentEntity.toDomain(): Student =
    Student(id, name, studentNo, gender, grade, classIdsJson.toLongList(), avatarUri)

fun Student.toEntity(): StudentEntity =
    StudentEntity(id, name, studentNo, gender, grade, classIds.toJson(), avatarUri)

// ─── Schedule ─────────────────────────────────────────────────────────────────
// subjectId removed from ScheduleEntity in v3 — no mapping needed.

fun ScheduleEntity.toDomain(): Schedule =
    Schedule(id, classId, teacherId, day, period, startTime, endTime, code)

fun Schedule.toEntity(): ScheduleEntity =
    ScheduleEntity(id, classId, teacherId, day, period, startTime, endTime, code)

// ─── Attendance ───────────────────────────────────────────────────────────────
// subjectId removed from AttendanceEntity in v3 — no mapping needed.

fun AttendanceEntity.toDomain(): Attendance =
    Attendance(
        id, classId, teacherId,
        date, period, startTime, endTime,
        topic, status, notes,
        attendeesJson.toLongList(), code
    )

fun Attendance.toEntity(): AttendanceEntity =
    AttendanceEntity(
        id, classId, teacherId,
        date, period, startTime, endTime,
        topic, status, notes,
        attendees.toJson(), code
    )
