package com.school.manager.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
fun KnowledgePointsScreen(vm: AppViewModel, onOpenDrawer: () -> Unit = {}) {
    val state   by vm.state.collectAsState()
    var fGrade  by remember { mutableStateOf("") }
    var query   by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<KnowledgePoint?>(null) }
    var viewing by remember { mutableStateOf<KnowledgePoint?>(null) }

    val filtered = state.knowledgePoints.filter { kp ->
        (fGrade.isBlank() || kp.grade == fGrade) &&
        (query.isBlank()  || kp.content.contains(query, ignoreCase = true)
                          || kp.code.contains(query, ignoreCase = true)
                          || kp.chapter.contains(query, ignoreCase = true))
    }

    val grouped = filtered.groupBy { it.chapter }.toSortedMap()

    Scaffold(
        floatingActionButton = {
            ScreenSpeedDialFab(
                addLabel     = "添加知识点",
                addIcon      = Icons.Default.Add,
                onAdd        = { showAdd = true },
                onOpenDrawer = onOpenDrawer
            )
        }
    ) { inner ->
        Column(
            Modifier.fillMaxSize()
                .padding(top = inner.calculateTopPadding(), bottom = inner.calculateBottomPadding())
        ) {
            // ── Grade filter row ──────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(selected = fGrade.isBlank(), onClick = { fGrade = "" },
                    label = { Text("全部学段") })
                PHYSICS_GRADES.forEach { g ->
                    FilterChip(
                        selected = fGrade == g,
                        onClick  = { fGrade = if (fGrade == g) "" else g },
                        label    = { Text(g) }
                    )
                }
            }

            // ── Search ────────────────────────────────────────────────────
            OutlinedTextField(
                value         = query,
                onValueChange = { query = it },
                label         = { Text("搜索知识点") },
                singleLine    = true,
                shape         = RoundedCornerShape(12.dp),
                modifier      = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                leadingIcon   = { Icon(Icons.Default.Search, null) },
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = FluentBlue,
                    unfocusedBorderColor = FluentBorder
                )
            )

            Text("共 ${filtered.size} 个知识点",
                style    = MaterialTheme.typography.labelMedium,
                color    = FluentMuted,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))

            // ── List grouped by chapter ───────────────────────────────────
            LazyColumn(
                contentPadding      = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier            = Modifier.fillMaxSize()
            ) {
                if (grouped.isEmpty()) {
                    item { EmptyState("📖", "暂无知识点") }
                } else {
                    grouped.forEach { (chapter, points) ->
                        item(key = "h_$chapter") {
                            Surface(
                                shape    = RoundedCornerShape(8.dp),
                                color    = FluentBlue.copy(alpha = 0.10f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(chapter,
                                    style      = MaterialTheme.typography.labelMedium,
                                    color      = FluentBlue,
                                    fontWeight = FontWeight.Bold,
                                    modifier   = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                            }
                        }
                        items(points, key = { it.id }) { kp ->
                            KnowledgePointCard(
                                kp      = kp,
                                onClick = { viewing = kp }
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    viewing?.let { kp ->
        KnowledgePointDetailDialog(
            kp        = kp,
            onDismiss = { viewing = null },
            onEdit    = { editing = kp; viewing = null },
            onDelete  = { vm.deleteKnowledgePoint(kp.id); viewing = null }
        )
    }
    editing?.let { kp ->
        KnowledgePointFormDialog(
            title   = "编辑知识点",
            initial = kp,
            onDismiss = { editing = null },
            onSave    = { vm.updateKnowledgePoint(it); editing = null }
        )
    }
    if (showAdd) {
        KnowledgePointFormDialog(
            title   = "添加知识点",
            initial = null,
            onDismiss = { showAdd = false },
            onSave    = { kp ->
                vm.addKnowledgePoint(kp.grade, kp.chapter, kp.section, kp.code, kp.content)
                showAdd = false
            }
        )
    }
}

// ── Card ──────────────────────────────────────────────────────────────────────

@Composable
private fun KnowledgePointCard(kp: KnowledgePoint, onClick: () -> Unit) {
    FluentCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                ColorChip(kp.code, FluentBlue)
                ColorChip(kp.grade, FluentGreen)
                ColorChip(kp.section.take(14), FluentTeal)
                if (kp.isCustom) ColorChip("自定义", FluentAmber)
            }
            Text(kp.content,
                style   = MaterialTheme.typography.bodyMedium,
                maxLines = 3)
        }
    }
}

// ── Detail dialog ─────────────────────────────────────────────────────────────

@Composable
private fun KnowledgePointDetailDialog(
    kp: KnowledgePoint,
    onDismiss: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit
) {
    FluentDialog(title = "知识点详情", onDismiss = onDismiss) {
        DetailRowPair("学段", kp.grade, "编号", kp.code)
        DetailRow("章", kp.chapter)
        DetailRow("节", kp.section)
        SectionHeader("内容")
        Text(kp.content,
            style    = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        if (kp.isCustom) {
            Surface(shape = RoundedCornerShape(6.dp), color = FluentAmber.copy(0.12f),
                modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("自定义知识点",
                    style    = MaterialTheme.typography.labelSmall, color = FluentAmber,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
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

// ── Form dialog ───────────────────────────────────────────────────────────────

@Composable
internal fun KnowledgePointFormDialog(
    title: String,
    initial: KnowledgePoint?,
    onDismiss: () -> Unit,
    onSave: (KnowledgePoint) -> Unit
) {
    var grade   by remember { mutableStateOf(initial?.grade   ?: PHYSICS_GRADES[0]) }
    var chapter by remember { mutableStateOf(initial?.chapter ?: "") }
    var section by remember { mutableStateOf(initial?.section ?: "") }
    var code    by remember { mutableStateOf(initial?.code    ?: "") }
    var content by remember { mutableStateOf(initial?.content ?: "") }

    FluentDialog(title = title, onDismiss = onDismiss, onConfirm = {
        if (content.isNotBlank()) {
            onSave(KnowledgePoint(
                id       = initial?.id ?: System.currentTimeMillis(),
                grade    = grade,
                chapter  = chapter.trim(),
                section  = section.trim(),
                code     = code.trim(),
                content  = content.trim(),
                isCustom = true
            ))
        }
    }) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top) {
            Box(Modifier.weight(1f)) {
                FormDropdown("学段", grade, PHYSICS_GRADES) { grade = it }
            }
            Box(Modifier.weight(2f)) {
                FormTextField("编号", code, { code = it }, "如 1.1.1")
            }
        }
        FormTextField("章", chapter, { chapter = it }, "如 第1章 机械运动")
        FormTextField("节", section, { section = it }, "如 第1节 长度和时间的测量")
        OutlinedTextField(
            value         = content,
            onValueChange = { content = it },
            label         = { Text("知识点内容") },
            shape         = RoundedCornerShape(12.dp),
            modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            minLines      = 3, maxLines = 6,
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = FluentBlue,
                unfocusedBorderColor = FluentBorder
            )
        )
    }
}
