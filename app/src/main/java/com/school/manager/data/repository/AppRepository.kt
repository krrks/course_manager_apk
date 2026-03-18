package com.school.manager.data.repository

import android.content.Context
import com.school.manager.data.*
import com.school.manager.data.db.*
import kotlinx.coroutines.flow.*

class AppRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)

    // ─── Live AppState ────────────────────────────────────────────────────────
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

    suspend fun isEmpty(): Boolean =
        db.subjectDao().count() == 0 && db.teacherDao().all().isEmpty()

    // ─── Subjects ─────────────────────────────────────────────────────────────
    suspend fun addSubject(s: Subject)    = db.subjectDao().insert(s.toEntity())
    suspend fun updateSubject(s: Subject) = db.subjectDao().update(s.toEntity())
    suspend fun deleteSubject(id: Long)   = db.subjectDao().deleteById(id)
    // Room FK (ON DELETE SET NULL) nullifies classes.subjectId automatically.
    // No manual cascade needed — UI reads subject name via JOIN at display time.

    // ─── Teachers ─────────────────────────────────────────────────────────────
    suspend fun addTeacher(t: Teacher)    = db.teacherDao().insert(t.toEntity())
    suspend fun updateTeacher(t: Teacher) = db.teacherDao().update(t.toEntity())
    suspend fun deleteTeacher(id: Long)   = db.teacherDao().deleteById(id)

    // ─── Classes ──────────────────────────────────────────────────────────────
    // Changing a class's subjectId automatically updates what all its schedule
    // and attendance rows display — no extra writes needed.
    suspend fun addSchoolClass(c: SchoolClass)    = db.classDao().insert(c.toEntity())
    suspend fun updateSchoolClass(c: SchoolClass) = db.classDao().update(c.toEntity())
    suspend fun deleteSchoolClass(id: Long)        = db.classDao().deleteById(id)

    // ─── Students ─────────────────────────────────────────────────────────────
    suspend fun addStudent(s: Student)    = db.studentDao().insert(s.toEntity())
    suspend fun updateStudent(s: Student) = db.studentDao().update(s.toEntity())
    suspend fun deleteStudent(id: Long)   = db.studentDao().deleteById(id)

    // ─── Schedule ─────────────────────────────────────────────────────────────
    suspend fun addSchedule(s: Schedule)    = db.scheduleDao().insert(s.toEntity())
    suspend fun updateSchedule(s: Schedule) = db.scheduleDao().update(s.toEntity())
    suspend fun deleteSchedule(id: Long)    = db.scheduleDao().deleteById(id)

    // ─── Attendance ───────────────────────────────────────────────────────────
    suspend fun addAttendance(a: Attendance)    = db.attendanceDao().insert(a.toEntity())
    suspend fun updateAttendance(a: Attendance) = db.attendanceDao().update(a.toEntity())
    suspend fun deleteAttendance(id: Long)      = db.attendanceDao().deleteById(id)

    // ─── Bulk: clear all tables ───────────────────────────────────────────────
    suspend fun clearAll() {
        db.attendanceDao().deleteAll()
        db.scheduleDao().deleteAll()
        db.classDao().deleteAll()
        db.studentDao().deleteAll()
        db.subjectDao().deleteAll()
        db.teacherDao().deleteAll()
    }

    suspend fun importAll(state: AppState) {
        clearAll()
        mergeAll(state)
    }

    suspend fun mergeAll(incoming: AppState) {
        incoming.subjects.forEach { e ->
            val entity = e.toEntity()
            if (db.subjectDao().insert(entity) == -1L) db.subjectDao().update(entity)
        }
        incoming.teachers.forEach { e ->
            val entity = e.toEntity()
            if (db.teacherDao().insert(entity) == -1L) db.teacherDao().update(entity)
        }
        incoming.classes.forEach { e ->
            val entity = e.toEntity()
            if (db.classDao().insert(entity) == -1L) db.classDao().update(entity)
        }
        incoming.students.forEach { e ->
            val entity = e.toEntity()
            if (db.studentDao().insert(entity) == -1L) db.studentDao().update(entity)
        }
        incoming.schedule.forEach { e ->
            val entity = e.toEntity()
            if (db.scheduleDao().insert(entity) == -1L) db.scheduleDao().update(entity)
        }
        incoming.attendance.forEach { e ->
            val entity = e.toEntity()
            if (db.attendanceDao().insert(entity) == -1L) db.attendanceDao().update(entity)
        }
    }

    suspend fun snapshot(): AppState = AppState(
        subjects   = db.subjectDao().all().map(SubjectEntity::toDomain),
        teachers   = db.teacherDao().all().map(TeacherEntity::toDomain),
        classes    = db.classDao().all().map(ClassEntity::toDomain),
        students   = db.studentDao().all().map(StudentEntity::toDomain),
        schedule   = db.scheduleDao().all().map(ScheduleEntity::toDomain),
        attendance = db.attendanceDao().all().map(AttendanceEntity::toDomain)
    )
}
