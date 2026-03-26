package com.school.manager.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY id") fun allFlow(): Flow<List<SubjectEntity>>
    @Query("SELECT * FROM subjects ORDER BY id") suspend fun all(): List<SubjectEntity>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(e: SubjectEntity): Long
    @Update suspend fun update(e: SubjectEntity)
    @Query("DELETE FROM subjects WHERE id = :id") suspend fun deleteById(id: Long)
    @Query("DELETE FROM subjects") suspend fun deleteAll()
    @Query("SELECT COUNT(*) FROM subjects") suspend fun count(): Int
}

@Dao interface TeacherDao {
    @Query("SELECT * FROM teachers ORDER BY id") fun allFlow(): Flow<List<TeacherEntity>>
    @Query("SELECT * FROM teachers ORDER BY id") suspend fun all(): List<TeacherEntity>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(e: TeacherEntity): Long
    @Update suspend fun update(e: TeacherEntity)
    @Query("DELETE FROM teachers WHERE id = :id") suspend fun deleteById(id: Long)
    @Query("DELETE FROM teachers") suspend fun deleteAll()
}

@Dao interface ClassDao {
    @Query("SELECT * FROM classes ORDER BY id") fun allFlow(): Flow<List<ClassEntity>>
    @Query("SELECT * FROM classes ORDER BY id") suspend fun all(): List<ClassEntity>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(e: ClassEntity): Long
    @Update suspend fun update(e: ClassEntity)
    @Query("DELETE FROM classes WHERE id = :id") suspend fun deleteById(id: Long)
    @Query("DELETE FROM classes") suspend fun deleteAll()
}

@Dao interface StudentDao {
    @Query("SELECT * FROM students ORDER BY id") fun allFlow(): Flow<List<StudentEntity>>
    @Query("SELECT * FROM students ORDER BY id") suspend fun all(): List<StudentEntity>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(e: StudentEntity): Long
    @Update suspend fun update(e: StudentEntity)
    @Query("DELETE FROM students WHERE id = :id") suspend fun deleteById(id: Long)
    @Query("DELETE FROM students") suspend fun deleteAll()
}

@Dao interface LessonDao {
    @Query("SELECT * FROM lessons ORDER BY date, startTime") fun allFlow(): Flow<List<LessonEntity>>
    @Query("SELECT * FROM lessons ORDER BY date, startTime") suspend fun all(): List<LessonEntity>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(e: LessonEntity): Long
    @Update suspend fun update(e: LessonEntity)
    @Query("DELETE FROM lessons WHERE id = :id") suspend fun deleteById(id: Long)
    @Query("""
        DELETE FROM lessons
        WHERE classId  = :classId AND date >= :fromDate AND date <= :toDate
          AND (:includeNonPending = 1 OR status = 'pending')
          AND (:includeModified   = 1 OR isModified = 0)
    """)
    suspend fun deleteBatch(classId: Long, fromDate: String, toDate: String, includeNonPending: Boolean, includeModified: Boolean)
    @Query("DELETE FROM lessons") suspend fun deleteAll()
}

@Dao interface KpChapterDao {
    @Query("SELECT * FROM kp_chapters ORDER BY grade, no") fun allFlow(): Flow<List<KpChapterEntity>>
    @Query("SELECT * FROM kp_chapters ORDER BY grade, no") suspend fun all(): List<KpChapterEntity>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(e: KpChapterEntity): Long
    @Update suspend fun update(e: KpChapterEntity)
    @Query("DELETE FROM kp_chapters WHERE id = :id") suspend fun deleteById(id: Long)
    @Query("DELETE FROM kp_chapters") suspend fun deleteAll()
    @Query("SELECT COUNT(*) FROM kp_chapters") suspend fun count(): Int
}

@Dao interface KpSectionDao {
    @Query("SELECT * FROM kp_sections ORDER BY chapterId, no") fun allFlow(): Flow<List<KpSectionEntity>>
    @Query("SELECT * FROM kp_sections ORDER BY chapterId, no") suspend fun all(): List<KpSectionEntity>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(e: KpSectionEntity): Long
    @Update suspend fun update(e: KpSectionEntity)
    @Query("DELETE FROM kp_sections WHERE id = :id") suspend fun deleteById(id: Long)
    @Query("DELETE FROM kp_sections") suspend fun deleteAll()
}

@Dao interface KnowledgePointDao {
    @Query("SELECT * FROM knowledge_points ORDER BY sectionId, no") fun allFlow(): Flow<List<KnowledgePointEntity>>
    @Query("SELECT * FROM knowledge_points ORDER BY sectionId, no") suspend fun all(): List<KnowledgePointEntity>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(e: KnowledgePointEntity): Long
    @Update suspend fun update(e: KnowledgePointEntity)
    @Query("DELETE FROM knowledge_points WHERE id = :id") suspend fun deleteById(id: Long)
    @Query("DELETE FROM knowledge_points") suspend fun deleteAll()
    @Query("SELECT COUNT(*) FROM knowledge_points") suspend fun count(): Int
}
