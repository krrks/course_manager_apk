package com.school.manager.viewmodel

import com.google.gson.Gson
import com.school.manager.data.*
import java.io.File

// ── Gson transfer models ──────────────────────────────────────────────────────

internal data class GsonTeacher(
    val id: Long? = null, val name: String? = null, val gender: String? = null,
    val phone: String? = null, val avatarUri: String? = null, val code: String? = null
)
internal data class GsonClass(
    val id: Long? = null, val name: String? = null, val grade: String? = null,
    val count: Int? = null, val headTeacherId: Long? = null,
    val subjectId: Long? = null, val code: String? = null
)
internal data class GsonLesson(
    val id: Long? = null, val classId: Long? = null, val date: String? = null,
    val startTime: String? = null, val endTime: String? = null,
    val status: String? = null, val topic: String? = null, val notes: String? = null,
    val attendees: List<Long>? = null, val isModified: Boolean? = null,
    val code: String? = null, val teacherIdOverride: Long? = null,
    val knowledgePointIds: List<Long>? = null
)
internal data class GsonKpChapter(
    val id: Long? = null, val grade: String? = null,
    val no: Int? = null, val name: String? = null
)
internal data class GsonKpSection(
    val id: Long? = null, val chapterId: Long? = null,
    val no: Int? = null, val name: String? = null
)
internal data class GsonKnowledgePoint(
    val id: Long? = null, val sectionId: Long? = null, val no: Int? = null,
    val title: String? = null, val content: String? = null, val isCustom: Boolean? = null
)

internal data class GsonState(
    val subjects:        List<Subject>?              = null,
    val teachers:        List<GsonTeacher>?          = null,
    val classes:         List<GsonClass>?            = null,
    val students:        List<Student>?              = null,
    val lessons:         List<GsonLesson>?           = null,
    val knowledgePoints: List<GsonKnowledgePoint>?   = null,
    val kpChapters:      List<GsonKpChapter>?        = null,
    val kpSections:      List<GsonKpSection>?        = null
)

// ── Parser ────────────────────────────────────────────────────────────────────

internal fun parseGsonState(
    json: String,
    gson: Gson,
    pathRemap: Map<String, String> = emptyMap()
): AppState? = runCatching {
    val raw = gson.fromJson(json, GsonState::class.java) ?: return null
    fun remap(old: String?) = old?.let { pathRemap[File(it).name] ?: it }

    AppState(
        subjects = raw.subjects ?: emptyList(),
        teachers = raw.teachers?.map {
            Teacher(it.id ?: 0L, it.name ?: "", it.gender ?: "男",
                    it.phone ?: "", remap(it.avatarUri), it.code ?: "")
        } ?: emptyList(),
        classes = raw.classes?.map {
            SchoolClass(it.id ?: 0L, it.name ?: "", it.grade ?: "",
                        it.count ?: 0, it.headTeacherId, it.subjectId, it.code ?: "")
        } ?: emptyList(),
        students = raw.students?.map { it.copy(avatarUri = remap(it.avatarUri)) } ?: emptyList(),
        lessons = raw.lessons?.map { gl ->
            Lesson(gl.id ?: 0L, gl.classId ?: 0L, gl.date ?: "",
                   gl.startTime ?: "", gl.endTime ?: "",
                   gl.status ?: "pending", gl.topic ?: "", gl.notes ?: "",
                   gl.attendees ?: emptyList(), gl.isModified ?: false,
                   gl.code ?: "", gl.teacherIdOverride,
                   gl.knowledgePointIds ?: emptyList())
        } ?: emptyList(),
        kpChapters = raw.kpChapters?.map {
            KpChapter(it.id ?: 0L, it.grade ?: "", it.no ?: 0, it.name ?: "")
        } ?: emptyList(),
        kpSections = raw.kpSections?.map {
            KpSection(it.id ?: 0L, it.chapterId ?: 0L, it.no ?: 0, it.name ?: "")
        } ?: emptyList(),
        knowledgePoints = raw.knowledgePoints?.map { gkp ->
            KnowledgePoint(gkp.id ?: 0L, gkp.sectionId ?: 0L, gkp.no ?: 0,
                           gkp.title ?: "", gkp.content ?: "", gkp.isCustom ?: false)
        } ?: emptyList()
    )
}.getOrNull()
