package com.school.manager.data.repository

import android.content.Context
import com.school.manager.data.*
import com.school.manager.data.db.*
import kotlinx.coroutines.flow.*

class AppRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)

    val appState: Flow<AppState> = combine(
        combine(
            db.subjectDao().allFlow().map  { it.map(SubjectEntity::toDomain) },
            db.teacherDao().allFlow().map  { it.map(TeacherEntity::toDomain) },
            db.classDao().allFlow().map    { it.map(ClassEntity::toDomain)   }
        ) { subjects, teachers, classes -> Triple(subjects, teachers, classes) },
        combine(
            db.studentDao().allFlow().map  { it.map(StudentEntity::toDomain)          },
            db.lessonDao().allFlow().map   { it.map(LessonEntity::toDomain)           },
            db.knowledgePointDao().allFlow().map { it.map(KnowledgePointEntity::toDomain) }
        ) { students, lessons, kps -> Triple(students, lessons, kps) },
        combine(
            db.kpChapterDao().allFlow().map { it.map(KpChapterEntity::toDomain) },
            db.kpSectionDao().allFlow().map { it.map(KpSectionEntity::toDomain) }
        ) { chapters, sections -> Pair(chapters, sections) }
    ) { (subjects, teachers, classes), (students, lessons, kps), (chapters, sections) ->
        AppState(subjects, teachers, classes, students, lessons, kps, chapters, sections)
    }

    suspend fun isEmpty(): Boolean =
        db.subjectDao().count() == 0 && db.teacherDao().all().isEmpty()

    // ── Subjects ──────────────────────────────────────────────────────────────
    suspend fun addSubject(s: Subject)    = db.subjectDao().insert(s.toEntity())
    suspend fun updateSubject(s: Subject) = db.subjectDao().update(s.toEntity())
    suspend fun deleteSubject(id: Long)   = db.subjectDao().deleteById(id)

    // ── Teachers ──────────────────────────────────────────────────────────────
    suspend fun addTeacher(t: Teacher)    = db.teacherDao().insert(t.toEntity())
    suspend fun updateTeacher(t: Teacher) = db.teacherDao().update(t.toEntity())
    suspend fun deleteTeacher(id: Long)   = db.teacherDao().deleteById(id)

    // ── Classes ───────────────────────────────────────────────────────────────
    suspend fun addSchoolClass(c: SchoolClass)    = db.classDao().insert(c.toEntity())
    suspend fun updateSchoolClass(c: SchoolClass) = db.classDao().update(c.toEntity())
    suspend fun deleteSchoolClass(id: Long)        = db.classDao().deleteById(id)

    // ── Students ──────────────────────────────────────────────────────────────
    suspend fun addStudent(s: Student)    = db.studentDao().insert(s.toEntity())
    suspend fun updateStudent(s: Student) = db.studentDao().update(s.toEntity())
    suspend fun deleteStudent(id: Long)   = db.studentDao().deleteById(id)

    // ── Lessons ───────────────────────────────────────────────────────────────
    suspend fun addLesson(l: Lesson)    = db.lessonDao().insert(l.toEntity())
    suspend fun updateLesson(l: Lesson) = db.lessonDao().update(l.toEntity())
    suspend fun deleteLesson(id: Long)  = db.lessonDao().deleteById(id)
    suspend fun deleteLessonBatch(
        classId: Long, fromDate: String, toDate: String,
        includeNonPending: Boolean, includeModified: Boolean
    ) = db.lessonDao().deleteBatch(classId, fromDate, toDate, includeNonPending, includeModified)

    // ── KP Chapters ───────────────────────────────────────────────────────────
    suspend fun addKpChapter(c: KpChapter)    = db.kpChapterDao().insert(c.toEntity())
    suspend fun updateKpChapter(c: KpChapter) = db.kpChapterDao().update(c.toEntity())
    suspend fun deleteKpChapter(id: Long)      = db.kpChapterDao().deleteById(id)

    // ── KP Sections ───────────────────────────────────────────────────────────
    suspend fun addKpSection(s: KpSection)    = db.kpSectionDao().insert(s.toEntity())
    suspend fun updateKpSection(s: KpSection) = db.kpSectionDao().update(s.toEntity())
    suspend fun deleteKpSection(id: Long)      = db.kpSectionDao().deleteById(id)

    // ── Knowledge Points ──────────────────────────────────────────────────────
    suspend fun addKnowledgePoint(kp: KnowledgePoint)    = db.knowledgePointDao().insert(kp.toEntity())
    suspend fun updateKnowledgePoint(kp: KnowledgePoint) = db.knowledgePointDao().update(kp.toEntity())
    suspend fun deleteKnowledgePoint(id: Long)            = db.knowledgePointDao().deleteById(id)

    // ── Reset / Import ────────────────────────────────────────────────────────

    private suspend fun resetAll() {
        db.lessonDao().deleteAll()
        db.classDao().deleteAll()
        db.studentDao().deleteAll()
        db.subjectDao().deleteAll()
        db.teacherDao().deleteAll()
        db.knowledgePointDao().deleteAll()
        db.kpSectionDao().deleteAll()
        db.kpChapterDao().deleteAll()
    }

    /** Deletes all data from every table. */
    suspend fun deleteAllData() = resetAll()

    /** Full reset-and-replace. */
    suspend fun importAll(state: AppState) {
        resetAll()
        mergeAll(state)
    }

    /** Merge-by-ID import: same ID → overwrite; new ID → insert. */
    suspend fun mergeAll(incoming: AppState) {
        incoming.subjects.forEach        { e -> val en = e.toEntity(); if (db.subjectDao().insert(en)        == -1L) db.subjectDao().update(en) }
        incoming.teachers.forEach        { e -> val en = e.toEntity(); if (db.teacherDao().insert(en)        == -1L) db.teacherDao().update(en) }
        incoming.classes.forEach         { e -> val en = e.toEntity(); if (db.classDao().insert(en)          == -1L) db.classDao().update(en)   }
        incoming.students.forEach        { e -> val en = e.toEntity(); if (db.studentDao().insert(en)        == -1L) db.studentDao().update(en) }
        incoming.lessons.forEach         { e -> val en = e.toEntity(); if (db.lessonDao().insert(en)         == -1L) db.lessonDao().update(en)  }
        incoming.kpChapters.forEach      { e -> val en = e.toEntity(); if (db.kpChapterDao().insert(en)      == -1L) db.kpChapterDao().update(en) }
        incoming.kpSections.forEach      { e -> val en = e.toEntity(); if (db.kpSectionDao().insert(en)      == -1L) db.kpSectionDao().update(en) }
        incoming.knowledgePoints.forEach { e -> val en = e.toEntity(); if (db.knowledgePointDao().insert(en) == -1L) db.knowledgePointDao().update(en) }
    }

    /**
     * GitHub sync pull — imports all non-KP tables (full replace),
     * leaving kp_chapters / kp_sections / knowledge_points untouched.
     */
    suspend fun importStateOnly(state: AppState) {
        db.lessonDao().deleteAll()
        db.classDao().deleteAll()
        db.studentDao().deleteAll()
        db.subjectDao().deleteAll()
        db.teacherDao().deleteAll()
        state.subjects.forEach { e -> val en = e.toEntity(); if (db.subjectDao().insert(en) == -1L) db.subjectDao().update(en) }
        state.teachers.forEach { e -> val en = e.toEntity(); if (db.teacherDao().insert(en) == -1L) db.teacherDao().update(en) }
        state.classes.forEach  { e -> val en = e.toEntity(); if (db.classDao().insert(en)   == -1L) db.classDao().update(en)   }
        state.students.forEach { e -> val en = e.toEntity(); if (db.studentDao().insert(en) == -1L) db.studentDao().update(en) }
        state.lessons.forEach  { e -> val en = e.toEntity(); if (db.lessonDao().insert(en)  == -1L) db.lessonDao().update(en)  }
    }

    /**
     * Replaces all custom KPs with the supplied list.
     * Built-in KPs (isCustom=false) are never touched.
     */
    suspend fun mergeCustomKps(points: List<KnowledgePoint>) {
        db.knowledgePointDao().deleteAllCustom()
        points.forEach { db.knowledgePointDao().insert(it.toEntity()) }
    }
}
