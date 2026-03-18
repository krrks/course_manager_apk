package com.school.manager.data.repository

import android.content.Context
import com.school.manager.data.*
import com.school.manager.data.db.*
import kotlinx.coroutines.flow.*

class AppRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)

    // ─── Live AppState — UI collects this via ViewModel ───────────────────────
    //
    // Nested combine: kotlinx.coroutines only provides typed overloads up to 5
    // flows, so we split 6 flows into two groups of 3 and combine the pairs.
    val appState: Flow<AppState> = combine(
        combine(
            db.subjectDao().allFlow().map { list -> list.map(SubjectEntity::toDomain) },
            db.teacherDao().allFlow().map { list -> list.map(TeacherEntity::toDomain) },
            db.classDao().allFlow().map   { list -> list.map(ClassEntity::toDomain)   }
        ) { subjects, teachers, classes ->
            Triple(subjects, teachers, classes)
        },
        combine(
            db.studentDao().allFlow().map    { list -> list.map(StudentEntity::toDomain)    },
            db.scheduleDao().allFlow().map   { list -> list.map(ScheduleEntity::toDomain)   },
            db.attendanceDao().allFlow().map { list -> list.map(AttendanceEntity::toDomain) }
        ) { students, schedule, attendance ->
            Triple(students, schedule, attendance)
        }
    ) { (subjects, teachers, classes), (students, schedule, attendance) ->
        AppState(subjects, teachers, classes, students, schedule, attendance)
    }

    // ─── Empty-check (used by ViewModel init to decide seeding) ──────────────
    suspend fun isEmpty(): Boolean =
        db.subjectDao().count() == 0 && db.teacherDao().all().isEmpty()

    // ─── Subjects ─────────────────────────────────────────────────────────────
    suspend fun addSubject(s: Subject)    = db.subjectDao().upsert(s.toEntity())
    suspend fun updateSubject(s: Subject) = db.subjectDao().upsert(s.toEntity())
    suspend fun deleteSubject(id: Long)   = db.subjectDao().deleteById(id)
    // Room FK (ON DELETE SET_NULL) automatically nullifies:
    //   classes.subjectId, schedule.subjectId, attendance.subjectId

    // ─── Teachers ─────────────────────────────────────────────────────────────
    suspend fun addTeacher(t: Teacher)    = db.teacherDao().upsert(t.toEntity())
    suspend fun updateTeacher(t: Teacher) = db.teacherDao().upsert(t.toEntity())
    suspend fun deleteTeacher(id: Long)   = db.teacherDao().deleteById(id)
    // Room FK (ON DELETE SET_NULL) automatically nullifies:
    //   classes.headTeacherId, schedule.teacherId, attendance.teacherId

    // ─── Classes ──────────────────────────────────────────────────────────────
    suspend fun addSchoolClass(c: SchoolClass)    = db.classDao().upsert(c.toEntity())
    suspend fun updateSchoolClass(c: SchoolClass) = db.classDao().upsert(c.toEntity())
    suspend fun deleteSchoolClass(id: Long)        = db.classDao().deleteById(id)
    // Room FK (ON DELETE CASCADE) automatically deletes:
    //   all schedule rows with this classId
    //   all attendance rows with this classId

    // ─── Students ─────────────────────────────────────────────────────────────
    suspend fun addStudent(s: Student)    = db.studentDao().upsert(s.toEntity())
    suspend fun updateStudent(s: Student) = db.studentDao().upsert(s.toEntity())
    suspend fun deleteStudent(id: Long)   = db.studentDao().deleteById(id)

    // ─── Schedule ─────────────────────────────────────────────────────────────
    suspend fun addSchedule(s: Schedule)    = db.scheduleDao().upsert(s.toEntity())
    suspend fun updateSchedule(s: Schedule) = db.scheduleDao().upsert(s.toEntity())
    suspend fun deleteSchedule(id: Long)    = db.scheduleDao().deleteById(id)

    // ─── Attendance ───────────────────────────────────────────────────────────
    suspend fun addAttendance(a: Attendance)    = db.attendanceDao().upsert(a.toEntity())
    suspend fun updateAttendance(a: Attendance) = db.attendanceDao().upsert(a.toEntity())
    suspend fun deleteAttendance(id: Long)      = db.attendanceDao().deleteById(id)

    // ─── Bulk: clear all tables (respects FK order) ───────────────────────────
    suspend fun clearAll() {
        // Delete children before parents to satisfy FK constraints.
        // schedule/attendance → classes → students → subjects → teachers
        db.attendanceDao().deleteAll()
        db.scheduleDao().deleteAll()
        db.classDao().deleteAll()
        db.studentDao().deleteAll()
        db.subjectDao().deleteAll()
        db.teacherDao().deleteAll()
    }

    // ─── Bulk: full replace (used by reset + ZIP import) ─────────────────────
    suspend fun importAll(state: AppState) {
        clearAll()
        mergeAll(state)
    }

    // ─── Bulk: upsert without clearing (used by JSON merge import) ───────────
    suspend fun mergeAll(incoming: AppState) {
        // Insert in FK dependency order: parents before children
        incoming.subjects.forEach   { db.subjectDao().upsert(it.toEntity()) }
        incoming.teachers.forEach   { db.teacherDao().upsert(it.toEntity()) }
        incoming.classes.forEach    { db.classDao().upsert(it.toEntity()) }
        incoming.students.forEach   { db.studentDao().upsert(it.toEntity()) }
        incoming.schedule.forEach   { db.scheduleDao().upsert(it.toEntity()) }
        incoming.attendance.forEach { db.attendanceDao().upsert(it.toEntity()) }
    }

    // ─── Snapshot (used by export when current StateFlow hasn't propagated) ───
    suspend fun snapshot(): AppState = AppState(
        subjects   = db.subjectDao().all().map(SubjectEntity::toDomain),
        teachers   = db.teacherDao().all().map(TeacherEntity::toDomain),
        classes    = db.classDao().all().map(ClassEntity::toDomain),
        students   = db.studentDao().all().map(StudentEntity::toDomain),
        schedule   = db.scheduleDao().all().map(ScheduleEntity::toDomain),
        attendance = db.attendanceDao().all().map(AttendanceEntity::toDomain)
    )
}
