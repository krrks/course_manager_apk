package com.school.manager.data.db

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.school.manager.data.*

// ─── Shared helpers ───────────────────────────────────────────────────────────

private val gson = Gson()
private val longListType = object : TypeToken<List<Long>>() {}.type

private fun String.toLongList(): List<Long> =
    try { gson.fromJson(this, longListType) ?: emptyList() } catch (_: Exception) { emptyList() }

private fun List<Long>.toJson(): String =
    gson.toJson(this) ?: "[]"

// ─── Subject ──────────────────────────────────────────────────────────────────

fun SubjectEntity.toDomain(): Subject =
    Subject(id, name, color, teacherId)

fun Subject.toEntity(): SubjectEntity =
    SubjectEntity(id, name, color, teacherId)

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
// subjectId is nullable in DB (SET_NULL on FK delete) but Long in domain model;
// map null → 0L so existing UI code never sees a null.

fun ScheduleEntity.toDomain(): Schedule =
    Schedule(id, classId, subjectId ?: 0L, teacherId, day, period, startTime, endTime, code)

fun Schedule.toEntity(): ScheduleEntity =
    ScheduleEntity(
        id, classId,
        subjectId = subjectId.takeIf { it != 0L },   // store 0L as NULL
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
