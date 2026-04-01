package com.school.manager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.school.manager.data.*
import com.school.manager.ui.components.*
import com.school.manager.ui.theme.*
import com.school.manager.viewmodel.AppViewModel

// ── Chapter dialogs ───────────────────────────────────────────────────────────

@Composable
internal fun ChapterFormDialog(
    title: String,
    initial: KpChapter?,
    existingGrades: List<String>,
    onDismiss: () -> Unit,
    onSave: (KpChapter) -> Unit
) {
    var grade by remember { mutableStateOf(initial?.grade ?: existingGrades.firstOrNull() ?: PHYSICS_GRADES[0]) }
    var no    by remember { mutableStateOf(initial?.no?.toString() ?: "") }
    var name  by remember { mutableStateOf(initial?.name ?: "") }

    // Combine existing grades with default suggestions, deduplicated
    val gradeSuggestions = remember(existingGrades) {
        (existingGrades + PHYSICS_GRADES).distinct()
    }

    FluentDialog(title = title, onDismiss = onDismiss, onConfirm = {
        val n = no.toIntOrNull() ?: return@FluentDialog
        if (name.isNotBlank() && grade.isNotBlank())
            onSave(KpChapter(initial?.id ?: System.currentTimeMillis(), grade.trim(), n, name.trim()))
    }) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.weight(1f)) {
                AutocompleteTextField(
                    label         = "学段",
                    value         = grade,
                    suggestions   = gradeSuggestions,
                    onValueChange = { grade = it }
                )
            }
            Box(Modifier.weight(1f)) { FormTextField("章号", no, { no = it }, "如 1") }
        }
        FormTextField("章名", name, { name = it }, "如 机械运动")
        Text("完整显示：第${no.ifBlank { "?" }}章 ${name.ifBlank { "?" }}  ·  $grade",
            style = MaterialTheme.typography.labelSmall, color = FluentMuted,
            modifier = Modifier.padding(horizontal = 4.dp))
    }
}

@Composable
internal fun ChapterDetailDialog(chapter: KpChapter, vm: AppViewModel, onDismiss: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val state by vm.state.collectAsState()
    val sectionCount = state.kpSections.count { it.chapterId == chapter.id }
    val pointCount   = state.knowledgePoints.count { kp -> state.kpSections.any { it.id == kp.sectionId && it.chapterId == chapter.id } }
    FluentDialog(title = "章节详情", onDismiss = onDismiss) {
        DetailRowPair("学段", chapter.grade, "章号", "第${chapter.no}章")
        DetailRow("章名", chapter.name)
        DetailRowPair("节数", "$sectionCount 节", "知识点", "$pointCount 条")
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onEdit,   shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) { Text("✏️ 编辑") }
            OutlinedButton(onClick = onDelete, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = FluentRed), modifier = Modifier.weight(1f)) { Text("删除") }
        }
        if (sectionCount > 0) {
            Surface(shape = RoundedCornerShape(8.dp), color = FluentAmber.copy(.10f), modifier = Modifier.fillMaxWidth()) {
                Text("⚠️ 删除章将同时删除所含节和知识点", style = MaterialTheme.typography.labelSmall,
                    color = FluentAmber, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
            }
        }
    }
}

// ── Section dialogs ───────────────────────────────────────────────────────────

@Composable
internal fun SectionFormDialog(title: String, initial: KpSection?, allChapters: List<KpChapter>, onDismiss: () -> Unit, onSave: (KpSection) -> Unit) {
    val sortedChapters = remember(allChapters) { allChapters.sortedBy { it.no } }
    var selectedChapter by remember { mutableStateOf(sortedChapters.firstOrNull { it.id == initial?.chapterId } ?: sortedChapters.firstOrNull()) }
    var no   by remember { mutableStateOf(initial?.no?.toString() ?: "") }
    var name by remember { mutableStateOf(initial?.name ?: "") }

    FluentDialog(title = title, onDismiss = onDismiss, onConfirm = {
        val chId = selectedChapter?.id ?: return@FluentDialog
        val n    = no.toIntOrNull()    ?: return@FluentDialog
        if (name.isNotBlank()) onSave(KpSection(initial?.id ?: System.currentTimeMillis(), chId, n, name.trim()))
    }) {
        if (sortedChapters.isEmpty()) {
            Text("请先添加章", style = MaterialTheme.typography.bodyMedium, color = FluentAmber, modifier = Modifier.padding(horizontal = 4.dp))
        } else {
            FormDropdown("所属章", selectedChapter?.let { "第${it.no}章 ${it.name}" } ?: "",
                sortedChapters.map { "第${it.no}章 ${it.name}" }) { sel ->
                selectedChapter = sortedChapters.firstOrNull { "第${it.no}章 ${it.name}" == sel }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.weight(1f)) { FormTextField("节号", no, { no = it }, "如 1") }
            Box(Modifier.weight(3f)) { FormTextField("节名", name, { name = it }, "如 长度和时间的测量") }
        }
        val chNo = selectedChapter?.no ?: "?"
        Text("完整显示：第${no.ifBlank { "?" }}节 ${name.ifBlank { "?" }}  (${chNo}.${no.ifBlank { "?" }})",
            style = MaterialTheme.typography.labelSmall, color = FluentMuted, modifier = Modifier.padding(horizontal = 4.dp))
    }
}

@Composable
internal fun SectionDetailDialog(section: KpSection, allChapters: List<KpChapter>, vm: AppViewModel, onDismiss: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val state      by vm.state.collectAsState()
    val chapter     = allChapters.find { it.id == section.chapterId }
    val pointCount  = state.knowledgePoints.count { it.sectionId == section.id }
    FluentDialog(title = "节详情", onDismiss = onDismiss) {
        DetailRow("所属章", chapter?.let { "第${it.no}章 ${it.name}" } ?: "─")
        DetailRowPair("节号", "第${section.no}节", "知识点", "$pointCount 条")
        DetailRow("节名", section.name)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onEdit,   shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) { Text("✏️ 编辑") }
            OutlinedButton(onClick = onDelete, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = FluentRed), modifier = Modifier.weight(1f)) { Text("删除") }
        }
        if (pointCount > 0) {
            Surface(shape = RoundedCornerShape(8.dp), color = FluentAmber.copy(.10f), modifier = Modifier.fillMaxWidth()) {
                Text("⚠️ 删除节将同时删除所含知识点", style = MaterialTheme.typography.labelSmall,
                    color = FluentAmber, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
            }
        }
    }
}

// ── Point dialogs ─────────────────────────────────────────────────────────────

@Composable
internal fun PointFormDialog(
    title: String, initial: KnowledgePoint?,
    allChapters: List<KpChapter>, allSections: List<KpSection>,
    onDismiss: () -> Unit, onSave: (KnowledgePoint) -> Unit
) {
    val sortedChapters = remember(allChapters) { allChapters.sortedBy { it.no } }
    var selectedChapter by remember { mutableStateOf(
        sortedChapters.firstOrNull { ch -> allSections.any { it.id == initial?.sectionId && it.chapterId == ch.id } } ?: sortedChapters.firstOrNull()
    ) }
    val sectionsForChapter = remember(selectedChapter, allSections) {
        allSections.filter { it.chapterId == selectedChapter?.id }.sortedBy { it.no }
    }
    var selectedSection by remember(selectedChapter) { mutableStateOf(
        sectionsForChapter.firstOrNull { it.id == initial?.sectionId } ?: sectionsForChapter.firstOrNull()
    ) }
    var no      by remember { mutableStateOf(initial?.no?.toString() ?: "") }
    var kpTitle by remember { mutableStateOf(initial?.title ?: "") }
    var content by remember { mutableStateOf(initial?.content ?: "") }

    FluentDialog(title = title, onDismiss = onDismiss, onConfirm = {
        val secId = selectedSection?.id ?: return@FluentDialog
        val n     = no.toIntOrNull()    ?: return@FluentDialog
        if (content.isNotBlank()) onSave(KnowledgePoint(
            id = initial?.id ?: System.currentTimeMillis(), sectionId = secId,
            no = n, title = kpTitle.trim(), content = content.trim(), isCustom = true
        ))
    }) {
        if (sortedChapters.isEmpty()) { Text("请先添加章", style = MaterialTheme.typography.bodyMedium, color = FluentAmber, modifier = Modifier.padding(horizontal = 4.dp)); return@FluentDialog }
        FormDropdown("所属章", selectedChapter?.let { "第${it.no}章 ${it.name}" } ?: "", sortedChapters.map { "第${it.no}章 ${it.name}" }) { sel ->
            selectedChapter = sortedChapters.firstOrNull { "第${it.no}章 ${it.name}" == sel }
            selectedSection = sectionsForChapter.firstOrNull()
        }
        if (sectionsForChapter.isEmpty()) {
            Text("此章暂无节，请先添加节", style = MaterialTheme.typography.bodyMedium, color = FluentAmber, modifier = Modifier.padding(horizontal = 4.dp))
        } else {
            FormDropdown("所属节", selectedSection?.let { "第${it.no}节 ${it.name}" } ?: "", sectionsForChapter.map { "第${it.no}节 ${it.name}" }) { sel ->
                selectedSection = sectionsForChapter.firstOrNull { "第${it.no}节 ${it.name}" == sel }
            }
        }
        FormTextField("条目号", no, { no = it }, "如 1")
        val chNo = selectedChapter?.no ?: "?"; val secNo = selectedSection?.no ?: "?"
        Text("编号将自动生成为 $chNo.$secNo.${no.ifBlank { "?" }}", style = MaterialTheme.typography.labelSmall,
            color = FluentBlue, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 4.dp))
        FormTextField("标题", kpTitle, { kpTitle = it }, "如：摄氏温度温标")
        OutlinedTextField(
            value = content, onValueChange = { content = it }, label = { Text("内容") },
            placeholder = { Text("详细解释，在知识点页面中展开显示", color = FluentMuted) },
            shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            minLines = 3, maxLines = 6,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FluentBlue, unfocusedBorderColor = FluentBorder)
        )
        if (kpTitle.isBlank()) {
            Text("⚠️ 简短标题为空时，上课记录中显示完整内容的前20字",
                style = MaterialTheme.typography.labelSmall, color = FluentAmber,
                modifier = Modifier.padding(horizontal = 4.dp))
        }
    }
}

@Composable
internal fun PointDetailDialog(kpFull: KpFull, vm: AppViewModel, onDismiss: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    FluentDialog(title = "知识点详情", onDismiss = onDismiss) {
        DetailRowPair("学段", kpFull.grade, "编号", kpFull.code)
        DetailRow("章", "第${kpFull.chapter.no}章 ${kpFull.chapter.name}")
        DetailRow("节", "第${kpFull.section.no}节 ${kpFull.section.name}")
        if (kpFull.title.isNotBlank()) DetailRow("标题", kpFull.title)
        SectionHeader("内容")
        Text(kpFull.content, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        if (kpFull.isCustom) {
            Surface(shape = RoundedCornerShape(6.dp), color = FluentAmber.copy(.12f), modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("自定义知识点", style = MaterialTheme.typography.labelSmall, color = FluentAmber,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onEdit,   shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) { Text("✏️ 编辑") }
            OutlinedButton(onClick = onDelete, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = FluentRed), modifier = Modifier.weight(1f)) { Text("删除") }
        }
    }
}

/** Backward-compat alias */
@Composable
internal fun KnowledgePointFormDialog(
    title: String, initial: KnowledgePoint?,
    allChapters: List<KpChapter>, allSections: List<KpSection>,
    onDismiss: () -> Unit, onSave: (KnowledgePoint) -> Unit
) = PointFormDialog(title, initial, allChapters, allSections, onDismiss, onSave)
