package com.school.manager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.school.manager.data.*
import com.school.manager.ui.components.*
import com.school.manager.ui.theme.*
import com.school.manager.viewmodel.AppViewModel

@Composable
fun ClassesScreen(vm: AppViewModel, onOpenDrawer: () -> Unit) {
    val state   by vm.state.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var viewing by remember { mutableStateOf<SchoolClass?>(null) }
    var editing by remember { mutableStateOf<SchoolClass?>(null) }

    Scaffold(
        floatingActionButton = {
            ScreenSpeedDialFab(onOpenDrawer = onOpenDrawer)
        }
    ) { inner ->
        LazyColumn(
            contentPadding = PaddingValues(
                top    = inner.calculateTopPadding() + 8.dp,
                bottom = inner.calculateBottomPadding() + 80.dp,
                start  = 12.dp, end = 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (state.classes.isEmpty()) {
                item { EmptyState("🏫", "暂无班级") }
            } else {
                items(state.classes) { cls ->
                    val sts      = state.students.filter { s -> s.classIds.contains(cls.id) }
                    val gradeCol = gradeColor(cls.grade)
                    // Resolve subject name via FK, fall back to legacy string
                    val subjectDisplay = cls.resolvedSubject(state.subjects)?.name
                        ?: cls.subject.ifBlank { null }

                    FluentCard(modifier = Modifier.fillMaxWidth(), onClick = { viewing = cls }) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Text(cls.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold)
                                Surface(shape = RoundedCornerShape(8.dp),
                                    color = gradeCol.copy(alpha = 0.15f)) {
                                    Text(cls.grade,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = gradeCol,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                }
                            }
                            Text("👩‍🏫 班主任：${vm.teacher(cls.headTeacherId)?.name ?: "未设置"}" +
                                if (subjectDisplay != null) "   📚 $subjectDisplay" else "",
                                style = MaterialTheme.typography.bodyMedium, color = FluentMuted)
                            LinearProgressIndicator(
                                progress = { if (cls.count > 0) sts.size.toFloat() / cls.count else 0f },
                                modifier  = Modifier.fillMaxWidth().height(4.dp),
                                color     = gradeCol,
                                trackColor = gradeCol.copy(alpha = 0.15f),
                            )
                            Text("${sts.size} / ${cls.count} 人",
                                style = MaterialTheme.typography.labelSmall, color = FluentMuted)
                        }
                    }
                }
            }
        }
    }

    viewing?.let { cls ->
        ClassDetailDialog(cls, state, vm,
            onDismiss = { viewing = null },
            onEdit    = { editing = cls; viewing = null },
            onDelete  = { vm.deleteSchoolClass(cls.id); viewing = null }
        )
    }

    editing?.let { cls ->
        ClassFormDialog("编辑班级", cls, state, vm, onDismiss = { editing = null }) { updated ->
            vm.updateSchoolClass(updated); editing = null
        }
    }

    if (showAdd) {
        ClassFormDialog("添加班级", null, state, vm, onDismiss = { showAdd = false }) { c ->
            vm.addSchoolClass(c.name, c.grade, c.count, c.headTeacherId, c.subjectId, c.subject, c.code)
            showAdd = false
        }
    }
}

private fun gradeColor(grade: String) = when {
    grade.startsWith("高一") -> FluentBlue
    grade.startsWith("高二") -> FluentPurple
    grade.startsWith("高三") -> FluentOrange
    grade.startsWith("初一") -> FluentGreen
    grade.startsWith("初二") -> FluentTeal
    else                     -> FluentAmber
}

@Composable
private fun ClassDetailDialog(
    cls: SchoolClass, state: AppState, vm: AppViewModel,
    onDismiss: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit
) {
    val ht  = vm.teacher(cls.headTeacherId)
    val sts = state.students.filter { s -> s.classIds.contains(cls.id) }
    val subjectDisplay = cls.resolvedSubject(state.subjects)?.name ?: cls.subject.ifBlank { null }
    FluentDialog(title = "班级详情", onDismiss = onDismiss) {
        if (cls.code.isNotBlank()) DetailRow("编号", cls.code)
        DetailRow("班级名称", cls.name)
        DetailRow("年级",     cls.grade)
        if (subjectDisplay != null) DetailRow("科目", subjectDisplay)
        DetailRow("班主任",   ht?.name ?: "未设置")
        DetailRow("编制人数", "${cls.count} 人")
        DetailRow("在籍学生", "${sts.size} 人")
        if (sts.isNotEmpty()) {
            SectionHeader("班级学生")
            androidx.compose.foundation.layout.FlowRow(Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                sts.forEach { s -> ColorChip(s.name, FluentBlue) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onEdit, shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)) { Text("✏️ 编辑") }
            OutlinedButton(onClick = onDelete, shape = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = FluentRed),
                modifier = Modifier.weight(1f)) { Text("删除") }
        }
    }
}

@Composable
private fun ClassFormDialog(
    title: String, initial: SchoolClass?,
    state: AppState, vm: AppViewModel,
    onDismiss: () -> Unit, onSave: (SchoolClass) -> Unit
) {
    var name    by remember { mutableStateOf(initial?.name    ?: "") }
    var grade   by remember { mutableStateOf(initial?.grade   ?: GRADES[0]) }
    var count   by remember { mutableStateOf(initial?.count?.toString() ?: "") }
    var teacher by remember { mutableStateOf(
        state.teachers.firstOrNull { it.id == initial?.headTeacherId }?.name ?: "") }
    var code    by remember { mutableStateOf(initial?.code    ?: genCode("C")) }

    // ── Subject: resolved via FK, fall back to legacy string match ────────────
    val initialSubject = remember(state, initial) {
        initial?.resolvedSubject(state.subjects) ?: state.subjects.find { it.name == initial?.subject }
    }
    var selectedSubjectName by remember {
        mutableStateOf(initialSubject?.name ?: initial?.subject ?: "")
    }

    // ── Student membership for this class ────────────────────────────────────
    val initialStudentIds = remember(state, initial) {
        if (initial == null) emptySet()
        else state.students.filter { it.classIds.contains(initial.id) }.map { it.id }.toSet()
    }
    var selectedStudents by remember { mutableStateOf(initialStudentIds) }

    FluentDialog(title = title, onDismiss = onDismiss, onConfirm = {
        if (name.isNotBlank()) {
            val tId = state.teachers.firstOrNull { it.name == teacher }?.id
            val chosenSubject = state.subjects.firstOrNull { it.name == selectedSubjectName }
            val savedClass = SchoolClass(
                id            = initial?.id ?: System.currentTimeMillis(),
                name          = name.trim(),
                grade         = grade,
                count         = count.toIntOrNull() ?: 0,
                headTeacherId = tId,
                subjectId     = chosenSubject?.id,
                subject       = chosenSubject?.name ?: selectedSubjectName.trim(),
                code          = code.trim()
            )
            // Apply student membership changes when editing
            if (initial != null) {
                val classId = initial.id
                (selectedStudents - initialStudentIds).forEach { sId ->
                    val s = state.students.find { it.id == sId }
                    if (s != null && !s.classIds.contains(classId))
                        vm.updateStudent(s.copy(classIds = s.classIds + classId))
                }
                (initialStudentIds - selectedStudents).forEach { sId ->
                    val s = state.students.find { it.id == sId }
                    if (s != null)
                        vm.updateStudent(s.copy(classIds = s.classIds.filter { it != classId }))
                }
            }
            onSave(savedClass)
        }
    }) {
        FluentTextField("班级名称", name, { name = it })
        DropdownField("年级", grade, GRADES) { grade = it }
        FluentTextField("编制人数", count, { count = it })
        DropdownField("班主任", teacher,
            listOf("") + state.teachers.map { it.name }) { teacher = it }

        // ── 科目：优先从 Subject 列表选择，兼容手动输入 ────────────────────
        if (state.subjects.isNotEmpty()) {
            AutocompleteTextField(
                label         = "科目",
                value         = selectedSubjectName,
                suggestions   = state.subjects.map { it.name },
                onValueChange = { selectedSubjectName = it }
            )
        } else {
            FluentTextField("科目", selectedSubjectName, { selectedSubjectName = it })
        }

        FluentTextField("班级编号", code, { code = it })

        // Student section (edit mode only)
        if (initial != null && state.students.isNotEmpty()) {
            SectionHeader("班级学生")
            Text(
                "勾选属于此班级的学生",
                style    = MaterialTheme.typography.bodySmall,
                color    = FluentMuted,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement   = Arrangement.spacedBy(4.dp)
            ) {
                state.students.forEach { s ->
                    val isSelected = s.id in selectedStudents
                    FilterChip(
                        selected = isSelected,
                        onClick  = {
                            selectedStudents = if (isSelected)
                                selectedStudents - s.id
                            else
                                selectedStudents + s.id
                        },
                        label = { Text(s.name) }
                    )
                }
            }
        }
    }
}
