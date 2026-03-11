package com.school.manager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.school.manager.data.*
import com.school.manager.ui.components.*
import com.school.manager.ui.theme.*
import com.school.manager.viewmodel.AppViewModel

private fun genCode(prefix: String) =
    "$prefix${System.currentTimeMillis().toString().takeLast(6)}"

private val GRADES = listOf(
    "初一", "初二", "初三",
    "高一", "高二", "高三"
)

@Composable
fun ClassesScreen(vm: AppViewModel) {
    val state   by vm.state.collectAsState()
    var showAdd  by remember { mutableStateOf(false) }
    var viewing  by remember { mutableStateOf<SchoolClass?>(null) }
    var editing  by remember { mutableStateOf<SchoolClass?>(null) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon    = { Icon(Icons.Default.Add, "") },
                text    = { Text("添加班级") },
                containerColor = FluentBlue, contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }
    ) { inner ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(160.dp),
            contentPadding = PaddingValues(inner.calculateTopPadding() + 12.dp, 12.dp, 12.dp, 80.dp),
            verticalArrangement   = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (state.classes.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { EmptyState("🏫", "暂无班级") }
            } else {
                items(state.classes) { cls ->
                    val ht       = vm.teacher(cls.headTeacherId)
                    val sts      = state.students.filter { s -> s.classIds.contains(cls.id) }
                    val gradeCol = gradeColor(cls.grade)

                    FluentCard(modifier = Modifier.fillMaxWidth(), onClick = { viewing = cls }) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()) {
                                ColorChip(cls.grade, gradeCol)
                                Text("${sts.size}/${cls.count}人",
                                    style = MaterialTheme.typography.labelSmall, color = FluentMuted)
                            }
                            Text(cls.name, style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold)
                            if (cls.subject.isNotBlank()) {
                                Text("科目：${cls.subject}",
                                    style = MaterialTheme.typography.bodySmall, color = FluentPurple)
                            }
                            Text("班主任：${ht?.name ?: "未设置"}",
                                style = MaterialTheme.typography.bodyMedium, color = FluentMuted)
                            LinearProgressIndicator(
                                progress = { if (cls.count > 0) sts.size.toFloat() / cls.count else 0f },
                                modifier  = Modifier.fillMaxWidth().height(4.dp),
                                color     = gradeCol,
                                trackColor = gradeCol.copy(alpha = 0.15f),
                            )
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
            vm.addSchoolClass(c.name, c.grade, c.count, c.headTeacherId, c.subject, c.code)
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
    FluentDialog(title = "班级详情", onDismiss = onDismiss) {
        if (cls.code.isNotBlank()) DetailRow("编号", cls.code)
        DetailRow("班级名称", cls.name)
        DetailRow("年级",     cls.grade)
        if (cls.subject.isNotBlank()) DetailRow("科目", cls.subject)
        DetailRow("班主任",   ht?.name ?: "未设置")
        DetailRow("编制人数", "${cls.count} 人")
        DetailRow("在籍学生", "${sts.size} 人")
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
    val existingSubjects = remember(state) {
        (state.subjects.map { it.name } +
         state.classes.map { it.subject }.filter { it.isNotBlank() })
            .distinct().sorted()
    }

    var name    by remember { mutableStateOf(initial?.name    ?: "") }
    var grade   by remember { mutableStateOf(initial?.grade   ?: GRADES[0]) }
    var count   by remember { mutableStateOf(initial?.count?.toString() ?: "") }
    var teacher by remember { mutableStateOf(
        state.teachers.firstOrNull { it.id == initial?.headTeacherId }?.name ?: "") }
    var subject by remember { mutableStateOf(initial?.subject ?: "") }
    var code    by remember { mutableStateOf(initial?.code    ?: genCode("C")) }

    FluentDialog(title = title, onDismiss = onDismiss, onConfirm = {
        if (name.isNotBlank()) {
            val tId = state.teachers.firstOrNull { it.name == teacher }?.id
            onSave(SchoolClass(
                id            = initial?.id ?: System.currentTimeMillis(),
                name          = name.trim(),
                grade         = grade,
                count         = count.toIntOrNull() ?: 0,
                headTeacherId = tId,
                subject       = subject.trim(),
                code          = code.trim()
            ))
        }
    }) {
        FluentTextField("班级名称", name, { name = it })
        DropdownField("年级", grade, GRADES) { grade = it }
        FluentTextField("编制人数", count, { count = it })
        DropdownField("班主任", teacher,
            listOf("") + state.teachers.map { it.name }) { teacher = it }
        AutocompleteTextField("科目", subject, existingSubjects) { subject = it }
        FluentTextField("班级编号", code, { code = it })
    }
}
