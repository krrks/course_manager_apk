package com.school.manager.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
fun SubjectsScreen(vm: AppViewModel, onOpenDrawer: () -> Unit) {
    val state   by vm.state.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var viewing by remember { mutableStateOf<Subject?>(null) }
    var editing by remember { mutableStateOf<Subject?>(null) }

    Scaffold(
        floatingActionButton = {
            ScreenSpeedDialFab(
                addLabel     = "添加科目",
                addIcon      = Icons.Default.Add,
                onAdd        = { showAdd = true },
                onOpenDrawer = onOpenDrawer
            )
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
            if (state.subjects.isEmpty()) {
                item { EmptyState("📚", "暂无科目") }
            } else {
                items(state.subjects, key = { it.id }) { s ->
                    SubjectRow(s, state, vm, onView = { viewing = s }, onEdit = { editing = s })
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
            vm.addSubject(s.name, s.color, s.teacherId, s.code); showAdd = false
        }
    }
}

// ─── Row card ─────────────────────────────────────────────────────────────────

@Composable
private fun SubjectRow(
    s: Subject, state: AppState, vm: AppViewModel,
    onView: () -> Unit, onEdit: () -> Unit
) {
    val te    = vm.teacher(s.teacherId)
    val color = packedToColor(s.color)
    // In the new model there is no separate schedule table.
    // Use lessons total / completed counts as proxies for "排课" / "已完成".
    val totalCount = state.lessons.count { l ->
        state.classes.find { it.id == l.classId }?.subjectId == s.id
    }
    val doneCount = state.lessons.count { l ->
        state.classes.find { it.id == l.classId }?.subjectId == s.id && l.status == "completed"
    }

    FluentCard(modifier = Modifier.fillMaxWidth(), onClick = onView) {
        Row(
            modifier              = Modifier.padding(14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(Modifier.width(4.dp).height(52.dp)
                .clip(RoundedCornerShape(2.dp)).background(color))

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📚 ${s.name}",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, color = color)
                    if (s.code.isNotBlank()) {
                        Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.12f)) {
                            Text(s.code, style = MaterialTheme.typography.labelSmall, color = color,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
                Text("主讲：${te?.name ?: "未分配"}",
                    style = MaterialTheme.typography.bodySmall, color = FluentMuted)
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SubjectStatChip("课次 $totalCount", color)
                SubjectStatChip("完成 $doneCount",  color)
            }
        }
    }
}

@Composable
private fun SubjectStatChip(label: String, color: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.10f)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

// ─── Detail dialog ────────────────────────────────────────────────────────────

@Composable
private fun SubjectDetailDialog(
    s: Subject, state: AppState, vm: AppViewModel,
    onDismiss: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit
) {
    val te = vm.teacher(s.teacherId)
    val totalCount = state.lessons.count { l ->
        state.classes.find { it.id == l.classId }?.subjectId == s.id
    }
    val doneCount = state.lessons.count { l ->
        state.classes.find { it.id == l.classId }?.subjectId == s.id && l.status == "completed"
    }
    FluentDialog(title = "科目详情", onDismiss = onDismiss) {
        if (s.code.isNotBlank()) DetailRow("编号", s.code)
        DetailRow("科目名称", s.name)
        DetailRow("主讲教师", te?.name ?: "未分配")
        DetailRow("课次总数", "$totalCount 节")
        DetailRow("已完成",   "$doneCount 节")
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onEdit,   shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) { Text("✏️ 编辑") }
            OutlinedButton(onClick = onDelete, shape = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = FluentRed),
                modifier = Modifier.weight(1f)) { Text("删除") }
        }
    }
}

// ─── Form dialog ──────────────────────────────────────────────────────────────

@Composable
private fun SubjectFormDialog(
    title: String, initial: Subject?,
    state: AppState, vm: AppViewModel,
    onDismiss: () -> Unit, onSave: (Subject) -> Unit
) {
    var name     by remember { mutableStateOf(initial?.name ?: "") }
    var teacher  by remember { mutableStateOf(
        state.teachers.firstOrNull { it.id == initial?.teacherId }?.name ?: "") }
    var code     by remember { mutableStateOf(initial?.code?.ifBlank { null } ?: genCode("SBJ")) }
    var colorIdx by remember { mutableStateOf(
        SUBJECT_COLORS.indexOfFirst { it == initial?.color }.coerceAtLeast(0)) }

    FluentDialog(title = title, onDismiss = onDismiss, onConfirm = {
        if (name.isNotBlank()) {
            val tId = state.teachers.firstOrNull { it.name == teacher }?.id
            onSave(Subject(initial?.id ?: System.currentTimeMillis(),
                name.trim(), SUBJECT_COLORS[colorIdx], tId, code.trim()))
        }
    }) {
        FluentTextField("科目编号", code, { code = it })
        FluentTextField("科目名称", name, { name = it })
        DropdownField("主讲教师", teacher, listOf("") + state.teachers.map { it.name }) { teacher = it }

        SectionHeader("颜色")
        androidx.compose.foundation.layout.FlowRow(
            modifier              = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement   = Arrangement.spacedBy(8.dp)
        ) {
            SUBJECT_COLORS.forEachIndexed { idx, packed ->
                val col        = packedToColor(packed)
                val isSelected = idx == colorIdx
                Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(col)
                    .clickable { colorIdx = idx }
                    .then(if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(8.dp)) else Modifier))
            }
        }
    }
}
