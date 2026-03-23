package com.school.manager.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.school.manager.data.*
import com.school.manager.ui.components.*
import com.school.manager.ui.theme.*
import com.school.manager.viewmodel.AppViewModel
import java.time.LocalDate

// ── Detail dialog ─────────────────────────────────────────────────────────────

@Composable
internal fun LessonDetailDialog(
    l: Lesson, state: AppState, vm: AppViewModel,
    progressMap: Map<Long, Pair<Int, Int>>,
    onDismiss: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit
) {
    val cls     = state.classes.find { it.id == l.classId }
    val sub     = cls?.resolvedSubject(state.subjects)
    val teacher = state.teachers.find { it.id == l.effectiveTeacherId(state.classes) }
    val ats     = l.attendees.mapNotNull { vm.student(it) }
    val (done, total) = progressMap[l.classId] ?: (0 to 0)

    // Local state for status dropdown
    var statusExpanded by remember { mutableStateOf(false) }

    FluentDialog(title = "课次详情", onDismiss = onDismiss) {
        if (l.code.isNotBlank()) DetailRow("编号", l.code)
        DetailRow("班级", cls?.name ?: "─")
        DetailRow("科目", sub?.name ?: "─")
        DetailRow("教师", teacher?.name ?: "─")
        if (l.teacherIdOverride != null) {
            Surface(
                shape    = RoundedCornerShape(6.dp),
                color    = FluentAmber.copy(0.12f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            ) {
                Text("本节课教师已覆盖班级默认设置",
                    style    = MaterialTheme.typography.labelSmall, color = FluentAmber,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
        DetailRow("日期", l.date)
        if (l.startTime.isNotBlank()) DetailRow("时间", "${l.startTime} – ${l.endTime}")

        // ── Compact progress row ──────────────────────────────────────────
        if (total > 0) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("上课进度", style = MaterialTheme.typography.bodyMedium, color = FluentMuted)
                    Text("$done / $total 节",
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = statusColor("completed"))
                }
                Spacer(Modifier.height(3.dp))
                FluentProgressBar(done.toFloat() / total, statusColor("completed"), Modifier.fillMaxWidth())
            }
            HorizontalDivider(color = FluentBorder, thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 16.dp))
        }

        // ── Status row with inline dropdown ───────────────────────────────
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text("状态", style = MaterialTheme.typography.bodyMedium, color = FluentMuted)
            Box {
                Row(
                    Modifier.clickable { statusExpanded = true },
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    StatusChip(l.status)
                    Icon(Icons.Default.ArrowDropDown, null,
                        tint     = statusColor(l.status),
                        modifier = Modifier.size(14.dp))
                }
                DropdownMenu(
                    expanded         = statusExpanded,
                    onDismissRequest = { statusExpanded = false }
                ) {
                    listOf("pending", "completed", "absent", "cancelled", "postponed").forEach { s ->
                        DropdownMenuItem(
                            text    = {
                                Text(statusLabel(s), color = statusColor(s),
                                    fontWeight = FontWeight.SemiBold)
                            },
                            onClick = {
                                vm.updateLesson(l.copy(status = s), markModified = true)
                                statusExpanded = false
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = FluentBorder, thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 16.dp))

        if (l.topic.isNotBlank()) DetailRow("课题", l.topic)
        if (l.notes.isNotBlank()) DetailRow("备注", l.notes)
        if (l.isModified) DetailRow("标记", "已单独修改")

        if (ats.isNotEmpty()) {
            SectionHeader("出勤学生 (${ats.size}人)")
            androidx.compose.foundation.layout.FlowRow(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement   = Arrangement.spacedBy(4.dp)
            ) { ats.forEach { s -> ColorChip(s.name, FluentGreen) } }
        }

        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onEdit, shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)) { Text("✏️ 编辑") }
            OutlinedButton(onClick = onDelete, shape = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = FluentRed),
                modifier = Modifier.weight(1f)) { Text("删除") }
        }
    }
}

// ── Form dialog ───────────────────────────────────────────────────────────────

@Composable
internal fun LessonFormDialog(
    title: String, initial: Lesson?,
    state: AppState, vm: AppViewModel,
    onDismiss: () -> Unit, onSave: (Lesson) -> Unit
) {
    var classId   by remember { mutableLongStateOf(initial?.classId ?: state.classes.firstOrNull()?.id ?: 0L) }
    var date      by remember { mutableStateOf(initial?.date      ?: LocalDate.now().toString()) }
    var startTime by remember { mutableStateOf(initial?.startTime ?: "08:00") }
    var endTime   by remember { mutableStateOf(initial?.endTime   ?: "10:00") }
    var status    by remember { mutableStateOf(initial?.status    ?: "pending") }
    var topic     by remember { mutableStateOf(initial?.topic     ?: "") }
    var notes     by remember { mutableStateOf(initial?.notes     ?: "") }
    var attendees by remember { mutableStateOf<List<Long>>(initial?.attendees ?: emptyList()) }
    var code      by remember { mutableStateOf(initial?.code?.ifBlank { null } ?: genCode("L")) }

    val selectedClass = state.classes.find { it.id == classId }

    var teacherOverrideId by remember {
        mutableStateOf(initial?.teacherIdOverride ?: selectedClass?.headTeacherId)
    }
    LaunchedEffect(classId) {
        if (initial == null || initial.teacherIdOverride == null)
            teacherOverrideId = state.classes.find { it.id == classId }?.headTeacherId
    }

    val classStudents           = state.students.filter { it.classIds.contains(classId) }
    val effectiveTeacherName    = state.teachers.find { it.id == teacherOverrideId }?.name ?: ""
    val classDefaultTeacherName = state.teachers.find {
        it.id == state.classes.find { c -> c.id == classId }?.headTeacherId
    }?.name

    FluentDialog(title = title, onDismiss = onDismiss, onConfirm = {
        if (classId != 0L) {
            val classDefaultTeacherId = state.classes.find { it.id == classId }?.headTeacherId
            val overrideToSave = if (teacherOverrideId == classDefaultTeacherId) null
                                 else teacherOverrideId
            onSave(Lesson(
                id                = initial?.id ?: System.currentTimeMillis(),
                classId           = classId,
                date              = date,
                startTime         = startTime,
                endTime           = endTime,
                status            = status,
                topic             = topic,
                notes             = notes,
                attendees         = attendees,
                isModified        = initial != null,
                code              = code.trim().ifBlank { genCode("L") },
                teacherIdOverride = overrideToSave
            ))
        }
    }) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top) {
            Box(Modifier.weight(1f)) { FormTextField("编号", code, { code = it }, "自动生成") }
            Box(Modifier.weight(1f)) {
                FormDropdown("状态", status,
                    listOf("pending", "completed", "absent", "cancelled", "postponed")) { status = it }
            }
        }
        FormDropdown("班级", selectedClass?.name ?: "",
            state.classes.map { it.name }) { name ->
            classId = state.classes.firstOrNull { it.name == name }?.id ?: classId
            attendees = emptyList()
            teacherOverrideId = state.classes.firstOrNull { it.name == name }?.headTeacherId
        }
        // ── Compact subject badge ─────────────────────────────────────────
        selectedClass?.resolvedSubject(state.subjects)?.let { sub ->
            Row(Modifier.padding(horizontal = 16.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("科目", style = MaterialTheme.typography.labelSmall, color = FluentMuted)
                ColorChip(sub.name, FluentPurple)
            }
        }
        FormDropdown(
            label    = if (classDefaultTeacherName != null) "教师（默认：$classDefaultTeacherName）" else "教师",
            selected = effectiveTeacherName.ifBlank { "无" },
            options  = listOf("无") + state.teachers.map { it.name }
        ) { picked ->
            teacherOverrideId = if (picked == "无") null
                                else state.teachers.firstOrNull { it.name == picked }?.id
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top) {
            Box(Modifier.weight(1f)) { DatePickerField("日期", date) { date = it } }
            Box(Modifier.weight(1f)) {
                StartTimeCompact(startTime) { newStart ->
                    val dur = minutesBetween(startTime, endTime).coerceAtLeast(30)
                    startTime = newStart
                    endTime   = addMinutesToTime(newStart, dur)
                }
            }
        }
        DurationChipsCompact(startTime, endTime) { endTime = it }
        FormTextField("课题", topic, { topic = it }, "本节课主题")
        if (classStudents.isNotEmpty()) {
            SectionHeader("出勤学生")
            androidx.compose.foundation.layout.FlowRow(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),  // ← was 6.dp
                verticalArrangement   = Arrangement.spacedBy(4.dp)
            ) {
                classStudents.forEach { s ->
                    val on = attendees.contains(s.id)
                    FilterChip(selected = on, onClick = {
                        attendees = if (on) attendees.filter { it != s.id } else attendees + s.id
                    }, label = { Text(s.name) })
                }
            }
        }
        FormTextField("备注", notes, { notes = it }, "可选")
    }
}
