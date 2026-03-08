package com.school.manager.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.school.manager.data.*
import com.school.manager.ui.components.*
import com.school.manager.ui.theme.*
import com.school.manager.viewmodel.AppViewModel

@Composable
fun SubjectsScreen(vm: AppViewModel) {
    val state  by vm.state.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var viewing by remember { mutableStateOf<Subject?>(null) }
    var editing by remember { mutableStateOf<Subject?>(null) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon = { Icon(Icons.Default.Add, "") },
                text = { Text("添加科目") },
                containerColor = FluentBlue, contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }
    ) { inner ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            contentPadding = PaddingValues(inner.calculateTopPadding() + 12.dp, 12.dp, 12.dp, 80.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (state.subjects.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { EmptyState("📚", "暂无科目") }
            } else {
                items(state.subjects) { s ->
                    val te     = vm.teacher(s.teacherId)
                    val color  = packedToColor(s.color)
                    val schedCount = state.schedule.count   { it.subjectId == s.id }
                    val attCount   = state.attendance.count { it.subjectId == s.id }

                    FluentCard(modifier = Modifier.fillMaxWidth(), onClick = { viewing = s }) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(color))
                            Text("📚", fontSize = 28.sp)
                            Text(s.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
                            Text("主讲：${te?.name ?: "未分配"}", style = MaterialTheme.typography.bodyMedium, color = FluentMuted)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                SubjectStatMini("排课", schedCount, color)
                                SubjectStatMini("记录", attCount, color)
                            }
                        }
                    }
                }
            }
        }
    }

    viewing?.let { s ->
        SubjectDetailDialog(s, state, vm,
            onDismiss = { viewing = null },
            onEdit    = { editing = s; viewing = null },
            onDelete  = { vm.deleteSubject(s.id); viewing = null }
        )
    }
    editing?.let { s ->
        SubjectFormDialog("编辑科目", s, state, vm, onDismiss = { editing = null }) { updated ->
            vm.updateSubject(updated); editing = null
        }
    }
    if (showAdd) {
        SubjectFormDialog("添加科目", null, state, vm, onDismiss = { showAdd = false }) { s ->
            vm.addSubject(s.name, s.color, s.teacherId); showAdd = false
        }
    }
}

@Composable
private fun SubjectStatMini(label: String, value: Int, color: Color) {
    Column(
        modifier = Modifier.background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("$value", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = FluentMuted)
    }
}

@Composable
private fun SubjectDetailDialog(
    s: Subject, state: com.school.manager.data.AppState, vm: AppViewModel,
    onDismiss: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit
) {
    val te = vm.teacher(s.teacherId)
    FluentDialog(title = "科目详情", onDismiss = onDismiss) {
        DetailRow("科目名称", s.name)
        DetailRow("主讲教师", te?.name ?: "未分配")
        DetailRow("排课数量", "${state.schedule.count { it.subjectId == s.id }} 节")
        DetailRow("上课次数", "${state.attendance.count { it.subjectId == s.id }} 次")
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onEdit, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) { Text("✏️ 编辑") }
            OutlinedButton(onClick = onDelete, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = FluentRed), modifier = Modifier.weight(1f)) { Text("删除") }
        }
    }
}

@Composable
private fun SubjectFormDialog(
    title: String, initial: Subject?,
    state: com.school.manager.data.AppState, vm: AppViewModel,
    onDismiss: () -> Unit, onSave: (Subject) -> Unit
) {
    var name     by remember { mutableStateOf(initial?.name ?: "") }
    var teacher  by remember { mutableStateOf(state.teachers.firstOrNull { it.id == initial?.teacherId }?.name ?: "") }
    var selColor by remember { mutableLongStateOf(initial?.color ?: SUBJECT_COLORS[0]) }

    FluentDialog(title = title, onDismiss = onDismiss, onConfirm = {
        if (name.isNotBlank()) {
            val tId = state.teachers.firstOrNull { it.name == teacher }?.id
            val nextColor = if (initial == null) SUBJECT_COLORS[state.subjects.size % SUBJECT_COLORS.size] else selColor
            onSave(Subject(initial?.id ?: System.currentTimeMillis(), name, nextColor, tId))
        }
    }) {
        FormTextField("科目名称", name, { name = it }, "如: 历史")
        FormDropdown("主讲教师", teacher, state.teachers.map { it.name }) { teacher = it }
        if (initial != null) {
            SectionHeader("颜色标签")
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SUBJECT_COLORS.forEach { c ->
                    val color = packedToColor(c)
                    Box(
                        modifier = Modifier
                            .size(if (selColor == c) 32.dp else 26.dp)
                            .clip(RoundedCornerShape(50))
                            .background(color)
                            .clickable { selColor = c }
                    )
                }
            }
        }
    }
}
