package com.school.manager.data.db

import com.school.manager.data.*
import org.json.JSONArray

private fun String.toLongList(): List<Long> =
    try { val a = JSONArray(this); (0 until a.length()).map { a.getLong(it) } }
    catch (_: Exception) { emptyList() }

private fun List<Long>.toJson(): String = JSONArray(this).toString()

fun SubjectEntity.toDomain(): Subject = Subject(id, name, color, teacherId, code)
fun Subject.toEntity(): SubjectEntity  = SubjectEntity(id, name, color, teacherId, code)

fun TeacherEntity.toDomain(): Teacher = Teacher(id, name, gender, phone, avatarUri, code)
fun Teacher.toEntity(): TeacherEntity  = TeacherEntity(id, name, gender, phone, avatarUri, code)

fun ClassEntity.toDomain(): SchoolClass = SchoolClass(id, name, grade, count, headTeacherId, subjectId, code)
fun SchoolClass.toEntity(): ClassEntity  = ClassEntity(id, name, grade, count, headTeacherId, subjectId, code)

fun StudentEntity.toDomain(): Student = Student(id, name, studentNo, gender, grade, classIdsJson.toLongList(), avatarUri)
fun Student.toEntity(): StudentEntity  = StudentEntity(id, name, studentNo, gender, grade, classIds.toJson(), avatarUri)

fun LessonEntity.toDomain(): Lesson = Lesson(
    id, classId, date, startTime, endTime, status,
    topic, notes, attendeesJson.toLongList(), isModified, code, teacherIdOverride,
    knowledgePointIdsJson.toLongList()
)

fun Lesson.toEntity(): LessonEntity = LessonEntity(
    id, classId, date, startTime, endTime, status,
    topic, notes, attendees.toJson(), isModified, code, teacherIdOverride,
    knowledgePointIds.toJson()
)

fun KnowledgePointEntity.toDomain(): KnowledgePoint =
    KnowledgePoint(id, grade, chapter, section, code, content, isCustom)

fun KnowledgePoint.toEntity(): KnowledgePointEntity =
    KnowledgePointEntity(id, grade, chapter, section, code, content, isCustom)
