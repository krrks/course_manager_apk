package com.school.manager.ui.screens

import androidx.compose.foundation.clickable
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KnowledgePointPickerSheet(
    allChapters: List<KpChapter>,
    allSections: List<KpSection>,
    allPoints: List<KnowledgePoint>,
    selected: Set<Long>,
    onConfirm: (Set<Long>) -> Unit,
    onAddNew: (sectionId: Long, no: Int, content: String) -> Unit,
    onDismiss: () -> Unit
) {
    var draft       by remember { mutableStateOf(selected) }
    var fGrade      by remember { mutableStateOf("") }
    var fChapterId  by remember { mutableLongStateOf(0L) }
    var query       by remember { mutableStateOf("") }
    var showAddForm by remember { mutableStateOf(false) }
    val sheetState  = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(fGrade) { fChapterId = 0L }

    // Precompute KpFull for all points
    val allKpFull = remember(allPoints, allChapters, allSections) {
        allPoints.mapNotNull { kp ->
            val sec = allSections.find { it.id == kp.sectionId } ?: return@mapNotNull null
            val ch  = allChapters.find { it.id == sec.chapterId } ?: return@mapNotNull null
            KpFull(ch, sec, kp)
        }
    }

    val filteredChapters = remember(allChapters, fGrade) {
        allChapters.filter { fGrade.isBlank() || it.grade == fGrade }.sortedBy { it.no }
    }

    val filtered = allKpFull.filter { kp ->
        (fGrade.isBlank()     || kp.grade == fGrade) &&
        (fChapterId == 0L     || kp.chapter.id == fChapterId) &&
        (query.isBlank()      || kp.content.contains(query, ignoreCase = true)
                              || kp.code.contains(query, ignoreCase = true)
                              || kp.chapter.name.contains(query, ignoreCase = true)
                              || kp.section.name.contains(query, ignoreCase = true))
    }

    val grouped = filtered.groupBy { it.chapter }.entries.sortedBy { it.key.no }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // Header
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("选择知识点", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Button(onClick = { onConfirm(draft) }, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = FluentBlue)) { Text("确定（${draft.size} 个）") }
            }

            HorizontalDivider(color = FluentBorder)

            // Grade filter
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = fGrade.isBlank(), onClick = { fGrade = "" }, label = { Text("全部学段") })
                PHYSICS_GRADES.forEach { g -> FilterChip(selected = fGrade == g, onClick = { fGrade = if (fGrade == g) "" else g }, label = { Text(g) }) }
            }

            // Chapter filter
            if (filteredChapters.size > 1) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = fChapterId == 0L, onClick = { fChapterId = 0L }, label = { Text("全部章") })
                    filteredChapters.forEach { ch ->
                        FilterChip(selected = fChapterId == ch.id, onClick = { fChapterId = if (fChapterId == ch.id) 0L else ch.id }, label = { Text("第${ch.no}章") })
                    }
                }
            }

            // Search
            OutlinedTextField(
                value = query, onValueChange = { query = it }, label = { Text("搜索知识点") },
                singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = { if (query.isNotBlank()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Clear, null, modifier = Modifier.size(16.dp)) } },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FluentBlue, unfocusedBorderColor = FluentBorder)
            )

            // Active filter summary
            if (fChapterId != 0L || query.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${filtered.size} 个知识点", style = MaterialTheme.typography.labelSmall, color = FluentMuted)
                    if (fChapterId != 0L) {
                        val chName = filteredChapters.find { it.id == fChapterId }?.let { "第${it.no}章" } ?: ""
                        InputChip(selected = true, onClick = { fChapterId = 0L }, label = { Text(chName, style = MaterialTheme.typography.labelSmall) }, trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(14.dp)) })
                    }
                }
            }

            // List grouped by chapter
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (grouped.isEmpty()) {
                    item { Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) { Text("没有符合条件的知识点", color = FluentMuted, style = MaterialTheme.typography.bodyMedium) } }
                } else {
                    grouped.forEach { (chapter, points) ->
                        if (grouped.size > 1) {
                            item(key = "hdr_${chapter.id}") {
                                Text("第${chapter.no}章 ${chapter.name}", style = MaterialTheme.typography.labelMedium, color = FluentBlue, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                            }
                        }
                        items(points.sortedWith(compareBy({ it.section.no }, { it.point.no })), key = { it.point.id }) { kpFull ->
                            val checked = kpFull.point.id in draft
                            Row(modifier = Modifier.fillMaxWidth().clickable { draft = if (checked) draft - kpFull.point.id else draft + kpFull.point.id }.padding(vertical = 3.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Checkbox(checked = checked, onCheckedChange = { draft = if (checked) draft - kpFull.point.id else draft + kpFull.point.id }, modifier = Modifier.size(20.dp).padding(top = 2.dp))
                                Column(Modifier.weight(1f)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        ColorChip(kpFull.code, FluentBlue)
                                        ColorChip(kpFull.section.name.take(8), FluentTeal)
                                        if (kpFull.isCustom) ColorChip("自定义", FluentAmber)
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(kpFull.content, style = MaterialTheme.typography.bodySmall, maxLines = 3)
                                }
                            }
                            HorizontalDivider(color = FluentBorder.copy(alpha = 0.5f))
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            HorizontalDivider(color = FluentBorder)

            // Add new point inline
            if (!showAddForm) {
                OutlinedButton(onClick = { showAddForm = true }, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("添加新知识点")
                }
            } else {
                AddKpInlineForm(allChapters = allChapters, allSections = allSections,
                    onSave = { secId, no, content -> onAddNew(secId, no, content); showAddForm = false },
                    onCancel = { showAddForm = false })
            }
        }
    }
}

@Composable
private fun AddKpInlineForm(allChapters: List<KpChapter>, allSections: List<KpSection>, onSave: (sectionId: Long, no: Int, content: String) -> Unit, onCancel: () -> Unit) {
    val sortedChapters = remember(allChapters) { allChapters.sortedBy { it.no } }
    var selectedChapter by remember { mutableStateOf(sortedChapters.firstOrNull()) }
    val sectionsForCh   = remember(selectedChapter, allSections) { allSections.filter { it.chapterId == selectedChapter?.id }.sortedBy { it.no } }
    var selectedSection by remember(selectedChapter) { mutableStateOf(sectionsForCh.firstOrNull()) }
    var no      by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    Surface(shape = RoundedCornerShape(12.dp), color = FluentBlueLight) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("添加知识点", style = MaterialTheme.typography.labelMedium, color = FluentBlue, fontWeight = FontWeight.Bold)
            if (sortedChapters.isEmpty()) { Text("请先在知识点管理页添加章节", style = MaterialTheme.typography.bodySmall, color = FluentAmber) }
            else {
                FormDropdown("章", selectedChapter?.let { "第${it.no}章 ${it.name}" } ?: "", sortedChapters.map { "第${it.no}章 ${it.name}" }) { sel -> selectedChapter = sortedChapters.firstOrNull { "第${it.no}章 ${it.name}" == sel }; selectedSection = sectionsForCh.firstOrNull() }
                if (sectionsForCh.isEmpty()) Text("此章暂无节", style = MaterialTheme.typography.bodySmall, color = FluentAmber)
                else FormDropdown("节", selectedSection?.let { "第${it.no}节 ${it.name}" } ?: "", sectionsForCh.map { "第${it.no}节 ${it.name}" }) { sel -> selectedSection = sectionsForCh.firstOrNull { "第${it.no}节 ${it.name}" == sel } }
                FormTextField("条目号", no, { no = it }, "如 1")
                val preview = "${selectedChapter?.no ?: "?"}.${selectedSection?.no ?: "?"}.${no.ifBlank { "?" }}"
                Text("编号：$preview", style = MaterialTheme.typography.labelSmall, color = FluentBlue, fontWeight = FontWeight.SemiBold)
            }
            OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("知识点内容") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FluentBlue, unfocusedBorderColor = FluentBorder))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) { Text("取消") }
                Button(onClick = { val secId = selectedSection?.id ?: return@Button; val n = no.toIntOrNull() ?: return@Button; if (content.isNotBlank()) onSave(secId, n, content.trim()) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = FluentBlue)) { Text("保存") }
            }
        }
    }
}
