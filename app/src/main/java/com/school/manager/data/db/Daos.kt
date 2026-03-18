package com.school.manager.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ─── Subject ──────────────────────────────────────────────────────────────────

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY id")
    fun allFlow(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects ORDER BY id")
    suspend fun all(): List<SubjectEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(e: SubjectEntity): Long

    @Update
    suspend fun update(e: SubjectEntity)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM subjects")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM subjects")
    suspend fun count(): Int
}

// ─── Teacher ──────────────────────────────────────────────────────────────────

@Dao
interface TeacherDao {
    @Query("SELECT * FROM teachers ORDER BY id")
    fun allFlow(): Flow<List<TeacherEntity>>

    @Query("SELECT * FROM teachers ORDER BY id")
    suspend fun all(): List<TeacherEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(e: TeacherEntity): Long

    @Update
    suspend fun update(e: TeacherEntity)

    @Query("DELETE FROM teachers WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM teachers")
    suspend fun deleteAll()
}

// ─── SchoolClass ──────────────────────────────────────────────────────────────
//
//  CRITICAL: schedule.classId → classes.id  ON DELETE CASCADE.
//  Always use update() for edits — never INSERT(REPLACE).

@Dao
interface ClassDao {
    @Query("SELECT * FROM classes ORDER BY id")
    fun allFlow(): Flow<List<ClassEntity>>

    @Query("SELECT * FROM classes ORDER BY id")
    suspend fun all(): List<ClassEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(e: ClassEntity): Long

    @Update
    suspend fun update(e: ClassEntity)

    @Query("DELETE FROM classes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM classes")
    suspend fun deleteAll()
}

// ─── Student ──────────────────────────────────────────────────────────────────

@Dao
interface StudentDao {
    @Query("SELECT * FROM students ORDER BY id")
    fun allFlow(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students ORDER BY id")
    suspend fun all(): List<StudentEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(e: StudentEntity): Long

    @Update
    suspend fun update(e: StudentEntity)

    @Query("DELETE FROM students WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM students")
    suspend fun deleteAll()
}

// ─── Schedule ─────────────────────────────────────────────────────────────────
//
//  subjectId removed from schedule table (Plan A).
//  Subject is derived at read time via classId → classes.subjectId.
//  No subject-cascade logic needed anywhere.

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedule ORDER BY day, startTime")
    fun allFlow(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedule ORDER BY day, startTime")
    suspend fun all(): List<ScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(e: ScheduleEntity): Long

    @Update
    suspend fun update(e: ScheduleEntity)

    @Query("DELETE FROM schedule WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM schedule")
    suspend fun deleteAll()
}

// ─── Attendance ───────────────────────────────────────────────────────────────
//
//  subjectId removed from attendance table (Plan A).

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance ORDER BY date DESC")
    fun allFlow(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance ORDER BY date DESC")
    suspend fun all(): List<AttendanceEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(e: AttendanceEntity): Long

    @Update
    suspend fun update(e: AttendanceEntity)

    @Query("DELETE FROM attendance WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM attendance")
    suspend fun deleteAll()
}
