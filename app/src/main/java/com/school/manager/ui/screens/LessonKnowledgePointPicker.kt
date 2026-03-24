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

/**
 * Bottom sheet for selecting knowledge points when editing a lesson.
 * Shows points grouped by chapter, filtered by grade, searchable.
 * Also exposes an inline "add new point" form.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KnowledgePointPickerSheet(
    allPoints: List<KnowledgePoint>,
    selected: Set<Long>,
    onConfirm: (Set<Long>) -> Unit,
    onAddNew: (grade: String, chapter: String, section: String, code: String, content: String) -> Unit,
    onDismiss: () -> Unit
) {
    var draft      by remember { mutableStateOf(selected) }
    var fGrade     by remember { mutableStateOf("") }
    var query      by remember { mutableStateOf("") }
    var showAddForm by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val filtered = allPoints.filter { kp ->
        (fGrade.isBlank() || kp.grade == fGrade) &&
        (query.isBlank()  || kp.content.contains(query, ignoreCase = true)
                          || kp.code.contains(query, ignoreCase = true)
                          || kp.chapter.contains(query, ignoreCase = true))
    }

    // Group by chapter
    val grouped = filtered.groupBy { it.chapter }
        .toSortedMap()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Header ────────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("选择知识点",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Button(
                    onClick = { onConfirm(draft) },
                    shape   = RoundedCornerShape(10.dp),
                    colors  = ButtonDefaults.buttonColors(containerColor = FluentBlue)
                ) { Text("确定（${draft.size} 个）") }
            }

            HorizontalDivider(color = FluentBorder)

            // ── Grade filter ──────────────────────────────────────────────
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(selected = fGrade.isBlank(), onClick = { fGrade = "" },
                    label = { Text("全部") })
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
                modifier      = Modifier.fillMaxWidth(),
                leadingIcon   = { Icon(Icons.Default.Search, null) },
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = FluentBlue,
                    unfocusedBorderColor = FluentBorder
                )
            )

            // ── Points list grouped by chapter ────────────────────────────
            LazyColumn(
                modifier            = Modifier.fillMaxWidth().heightIn(max = 340.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (grouped.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center) {
                            Text("没有符合条件的知识点", color = FluentMuted,
                                style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    grouped.forEach { (chapter, points) ->
                        item(key = "header_$chapter") {
                            Text(chapter,
                                style    = MaterialTheme.typography.labelMedium,
                                color    = FluentBlue, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                        }
                        items(points, key = { it.id }) { kp ->
                            val checked = kp.id in draft
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { draft = if (checked) draft - kp.id else draft + kp.id }
                                    .padding(vertical = 3.dp),
                                verticalAlignment     = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked         = checked,
                                    onCheckedChange = { draft = if (checked) draft - kp.id else draft + kp.id },
                                    modifier        = Modifier.size(20.dp).padding(top = 2.dp)
                                )
                                Column(Modifier.weight(1f)) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment     = Alignment.CenterVertically
                                    ) {
                                        ColorChip(kp.code, FluentBlue)
                                        ColorChip(kp.section.take(10), FluentTeal)
                                        if (kp.isCustom) ColorChip("自定义", FluentAmber)
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(kp.content,
                                        style   = MaterialTheme.typography.bodySmall,
                                        color   = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 3)
                                }
                            }
                            HorizontalDivider(color = FluentBorder.copy(alpha = 0.5f))
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            HorizontalDivider(color = FluentBorder)

            // ── Add new point inline ──────────────────────────────────────
            if (!showAddForm) {
                OutlinedButton(
                    onClick  = { showAddForm = true },
                    shape    = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("添加新知识点")
                }
            } else {
                AddKnowledgePointInlineForm(
                    onSave = { grade, chapter, section, code, content ->
                        onAddNew(grade, chapter, section, code, content)
                        showAddForm = false
                    },
                    onCancel = { showAddForm = false }
                )
            }
        }
    }
}

@Composable
private fun AddKnowledgePointInlineForm(
    onSave: (grade: String, chapter: String, section: String, code: String, content: String) -> Unit,
    onCancel: () -> Unit
) {
    var grade   by remember { mutableStateOf(PHYSICS_GRADES[0]) }
    var chapter by remember { mutableStateOf("") }
    var section by remember { mutableStateOf("") }
    var code    by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = FluentBlueLight
    ) {
        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("添加知识点",
                style = MaterialTheme.typography.labelMedium,
                color = FluentBlue, fontWeight = FontWeight.Bold)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                modifier      = Modifier.fillMaxWidth(),
                minLines      = 2, maxLines = 4,
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = FluentBlue,
                    unfocusedBorderColor = FluentBorder
                )
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)) { Text("取消") }
                Button(
                    onClick  = {
                        if (content.isNotBlank())
                            onSave(grade, chapter.trim(), section.trim(), code.trim(), content.trim())
                    },
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = FluentBlue)
                ) { Text("保存") }
            }
        }
    }
}
