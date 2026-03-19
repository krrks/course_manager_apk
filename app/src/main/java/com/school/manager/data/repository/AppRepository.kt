package com.school.manager.data.repository

import android.content.Context
import com.school.manager.data.*
import com.school.manager.data.db.*
import kotlinx.coroutines.flow.*

class AppRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)

    val appState: Flow<AppState> = combine(
        combine(
            db.subjectDao().allFlow().map { it.map(SubjectEntity::toDomain) },
            db.teacherDao().allFlow().map { it.map(TeacherEntity::toDomain) },
            db.classDao().allFlow().map   { it.map(ClassEntity::toDomain)   }
        ) { subjects, teachers, classes -> Triple(subjects, teachers, classes) },
        combine(
            db.studentDao().allFlow().map { it.map(StudentEntity::toDomain) },
            db.lessonDao().allFlow().map  { it.map(LessonEntity::toDomain)  }
        ) { students, lessons -> students to lessons }
    ) { (subjects, teachers, classes), (students, lessons) ->
        AppState(subjects, teachers, classes, students, lessons)
    }

    suspend fun isEmpty(): Boolean =
        db.subjectDao().count() == 0 && db.teacherDao().all().isEmpty()

    // Subjects
    suspend fun addSubject(s: Subject)    = db.subjectDao().insert(s.toEntity())
    suspend fun updateSubject(s: Subject) = db.subjectDao().update(s.toEntity())
    suspend fun deleteSubject(id: Long)   = db.subjectDao().deleteById(id)

    // Teachers
    suspend fun addTeacher(t: Teacher)    = db.teacherDao().insert(t.toEntity())
    suspend fun updateTeacher(t: Teacher) = db.teacherDao().update(t.toEntity())
    suspend fun deleteTeacher(id: Long)   = db.teacherDao().deleteById(id)

    // Classes
    suspend fun addSchoolClass(c: SchoolClass)    = db.classDao().insert(c.toEntity())
    suspend fun updateSchoolClass(c: SchoolClass) = db.classDao().update(c.toEntity())
    suspend fun deleteSchoolClass(id: Long)        = db.classDao().deleteById(id)

    // Students
    suspend fun addStudent(s: Student)    = db.studentDao().insert(s.toEntity())
    suspend fun updateStudent(s: Student) = db.studentDao().update(s.toEntity())
    suspend fun deleteStudent(id: Long)   = db.studentDao().deleteById(id)

    // Lessons
    suspend fun addLesson(l: Lesson)    = db.lessonDao().insert(l.toEntity())
    suspend fun updateLesson(l: Lesson) = db.lessonDao().update(l.toEntity())
    suspend fun deleteLesson(id: Long)  = db.lessonDao().deleteById(id)
    suspend fun deleteLessonBatch(
        classId: Long, fromDate: String, toDate: String,
        includeNonPending: Boolean, includeModified: Boolean
    ) = db.lessonDao().deleteBatch(classId, fromDate, toDate, includeNonPending, includeModified)

    suspend fun clearAll() {
        db.lessonDao().deleteAll()
        db.classDao().deleteAll()
        db.studentDao().deleteAll()
        db.subjectDao().deleteAll()
        db.teacherDao().deleteAll()
    }

    suspend fun importAll(state: AppState) { clearAll(); mergeAll(state) }

    suspend fun mergeAll(incoming: AppState) {
        fun <E> upsert(insert: suspend (E) -> Long, update: suspend (E) -> Unit, entity: E) {
            // caller runs this in a coroutine
        }
        incoming.subjects.forEach { e -> val ent = e.toEntity(); if (db.subjectDao().insert(ent) == -1L) db.subjectDao().update(ent) }
        incoming.teachers.forEach { e -> val ent = e.toEntity(); if (db.teacherDao().insert(ent) == -1L) db.teacherDao().update(ent) }
        incoming.classes.forEach  { e -> val ent = e.toEntity(); if (db.classDao().insert(ent)   == -1L) db.classDao().update(ent)   }
        incoming.students.forEach { e -> val ent = e.toEntity(); if (db.studentDao().insert(ent) == -1L) db.studentDao().update(ent) }
        incoming.lessons.forEach  { e -> val ent = e.toEntity(); if (db.lessonDao().insert(ent)  == -1L) db.lessonDao().update(ent)  }
    }
}
