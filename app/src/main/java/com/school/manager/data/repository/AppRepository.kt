package com.school.manager.data.repository

import android.content.Context
import com.school.manager.data.*
import com.school.manager.data.db.*
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import org.json.JSONObject

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
            db.lessonDao().allFlow().map  { it.map(LessonEntity::toDomain)  },
            db.knowledgePointDao().allFlow().map { it.map(KnowledgePointEntity::toDomain) }
        ) { students, lessons, kps -> Triple(students, lessons, kps) }
    ) { (subjects, teachers, classes), (students, lessons, kps) ->
        AppState(subjects, teachers, classes, students, lessons, kps)
    }

    suspend fun isEmpty(): Boolean =
        db.subjectDao().count() == 0 && db.teacherDao().all().isEmpty()

    // ── Knowledge point seeding ───────────────────────────────────────────

    suspend fun seedKnowledgePoints(context: Context) {
        if (db.knowledgePointDao().count() > 0) return
        try {
            val json = context.assets.open("knowledge_points.json")
                .bufferedReader().use { it.readText() }
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                db.knowledgePointDao().insert(
                    KnowledgePointEntity(
                        id       = obj.getLong("id"),
                        grade    = obj.getString("grade"),
                        chapter  = obj.getString("chapter"),
                        section  = obj.getString("section"),
                        code     = obj.getString("code"),
                        content  = obj.getString("content"),
                        isCustom = obj.optBoolean("isCustom", false)
                    )
                )
            }
        } catch (_: Exception) { /* asset missing or malformed — silently skip */ }
    }

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

    // Knowledge Points
    suspend fun addKnowledgePoint(kp: KnowledgePoint)    = db.knowledgePointDao().insert(kp.toEntity())
    suspend fun updateKnowledgePoint(kp: KnowledgePoint) = db.knowledgePointDao().update(kp.toEntity())
    suspend fun deleteKnowledgePoint(id: Long)            = db.knowledgePointDao().deleteById(id)

    suspend fun clearAll() {
        db.lessonDao().deleteAll()
        db.classDao().deleteAll()
        db.studentDao().deleteAll()
        db.subjectDao().deleteAll()
        db.teacherDao().deleteAll()
        // knowledge_points intentionally NOT cleared — user keeps their custom points
    }

    suspend fun importAll(state: AppState) { clearAll(); mergeAll(state) }

    suspend fun mergeAll(incoming: AppState) {
        incoming.subjects.forEach { e -> val ent = e.toEntity(); if (db.subjectDao().insert(ent) == -1L) db.subjectDao().update(ent) }
        incoming.teachers.forEach { e -> val ent = e.toEntity(); if (db.teacherDao().insert(ent) == -1L) db.teacherDao().update(ent) }
        incoming.classes.forEach  { e -> val ent = e.toEntity(); if (db.classDao().insert(ent)   == -1L) db.classDao().update(ent)   }
        incoming.students.forEach { e -> val ent = e.toEntity(); if (db.studentDao().insert(ent) == -1L) db.studentDao().update(ent) }
        incoming.lessons.forEach  { e -> val ent = e.toEntity(); if (db.lessonDao().insert(ent)  == -1L) db.lessonDao().update(ent)  }
        incoming.knowledgePoints.forEach { e ->
            val ent = e.toEntity()
            if (db.knowledgePointDao().insert(ent) == -1L) db.knowledgePointDao().update(ent)
        }
    }
}
