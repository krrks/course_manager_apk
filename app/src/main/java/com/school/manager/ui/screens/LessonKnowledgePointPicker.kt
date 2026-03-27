package com.school.manager.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.state.ToggleableState

// ─── Data structures ──────────────────────────────────────────────────────────

private data class SectionGroup(
    val section: KpSection,
    val points: List<KpFull>
)

private data class ChapterGroup(
    val chapter: KpChapter,
    val sections: List<SectionGroup>
) {
    val allPoints get() = sections.flatMap { it.points }
}

// ─── Main sheet ───────────────────────────────────────────────────────────────

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
    var query       by remember { mutableStateOf("") }
    var showAddForm by remember { mutableStateOf(false) }
    val sheetState  = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Precompute KpFull
    val allKpFull = remember(allPoints, allChapters, allSections) {
        allPoints.mapNotNull { kp ->
            val sec = allSections.find { it.id == kp.sectionId } ?: return@mapNotNull null
            val ch  = allChapters.find { it.id == sec.chapterId } ?: return@mapNotNull null
            KpFull(ch, sec, kp)
        }
    }

    // Build chapter→section→point tree, filtered by grade + query
    val chapterGroups = remember(allKpFull, fGrade, query) {
        val filtered = allKpFull.filter { kp ->
            (fGrade.isBlank() || kp.grade == fGrade) &&
            (query.isBlank()  || kp.content.contains(query, ignoreCase = true)
                              || kp.code.contains(query, ignoreCase = true)
                              || kp.chapter.name.contains(query, ignoreCase = true)
                              || kp.section.name.contains(query, ignoreCase = true))
        }
        filtered
            .groupBy { it.chapter }
            .entries
            .sortedBy { it.key.no }
            .map { (chapter, pts) ->
                ChapterGroup(
                    chapter  = chapter,
                    sections = pts.groupBy { it.section }
                        .entries
                        .sortedBy { it.key.no }
                        .map { (section, spts) ->
                            SectionGroup(section, spts.sortedBy { it.point.no })
                        }
                )
            }
    }

    // Expansion state — auto-expand on search, collapse on grade change
    var expandedChapters by remember { mutableStateOf(emptySet<Long>()) }
    var expandedSections by remember { mutableStateOf(emptySet<Long>()) }

    LaunchedEffect(query) {
        if (query.isNotBlank()) {
            expandedChapters = chapterGroups.map { it.chapter.id }.toSet()
            expandedSections = chapterGroups.flatMap { it.sections }.map { it.section.id }.toSet()
        } else {
            expandedChapters = emptySet()
            expandedSections = emptySet()
        }
    }
    LaunchedEffect(fGrade) {
        expandedChapters = emptySet()
        expandedSections = emptySet()
    }

    val totalVisible = chapterGroups.sumOf { it.allPoints.size }

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
            // ── Header ─────────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    "选择知识点",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = { onConfirm(draft) },
                    shape   = RoundedCornerShape(10.dp),
                    colors  = ButtonDefaults.buttonColors(containerColor = FluentBlue)
                ) { Text("确定（${draft.size} 个）") }
            }

            HorizontalDivider(color = FluentBorder)

            // ── Grade filter ───────────────────────────────────────────────
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = fGrade.isBlank(),
                    onClick  = { fGrade = "" },
                    label    = { Text("全部学段") }
                )
                PHYSICS_GRADES.forEach { g ->
                    FilterChip(
                        selected = fGrade == g,
                        onClick  = { fGrade = if (fGrade == g) "" else g },
                        label    = { Text(g) }
                    )
                }
            }

            // ── Search ─────────────────────────────────────────────────────
            OutlinedTextField(
                value         = query,
                onValueChange = { query = it },
                label         = { Text("搜索知识点") },
                singleLine    = true,
                shape         = RoundedCornerShape(12.dp),
                modifier      = Modifier.fillMaxWidth(),
                leadingIcon   = { Icon(Icons.Default.Search, null) },
                trailingIcon  = {
                    if (query.isNotBlank())
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Clear, null, modifier = Modifier.size(16.dp))
                        }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = FluentBlue,
                    unfocusedBorderColor = FluentBorder
                )
            )

            // ── Summary + expand-all toggle ────────────────────────────────
            if (chapterGroups.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        "${chapterGroups.size} 章 · $totalVisible 个知识点",
                        style = MaterialTheme.typography.labelSmall,
                        color = FluentMuted
                    )
                    val allExpanded = chapterGroups.all { it.chapter.id in expandedChapters }
                    TextButton(
                        onClick        = {
                            if (allExpanded) {
                                expandedChapters = emptySet()
                                expandedSections = emptySet()
                            } else {
                                expandedChapters = chapterGroups.map { it.chapter.id }.toSet()
                                expandedSections = chapterGroups
                                    .flatMap { it.sections }
                                    .map { it.section.id }.toSet()
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            if (allExpanded) "全部折叠" else "全部展开",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            // ── Tree list ──────────────────────────────────────────────────
            LazyColumn(
                modifier            = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (chapterGroups.isEmpty()) {
                    item {
                        Box(
                            Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "没有符合条件的知识点",
                                color = FluentMuted,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    chapterGroups.forEach { cg ->
                        val chExpanded  = cg.chapter.id in expandedChapters
                        val chapterIds  = cg.allPoints.map { it.point.id }.toSet()
                        val chAllSel    = chapterIds.isNotEmpty() && chapterIds.all { it in draft }
                        val chSomeSel   = chapterIds.any { it in draft }

                        // Chapter row
                        item(key = "ch_${cg.chapter.id}") {
                            ChapterRow(
                                chapter    = cg.chapter,
                                pointCount = cg.allPoints.size,
                                isExpanded = chExpanded,
                                allSelected = chAllSel,
                                someSelected = chSomeSel,
                                onToggleExpand = {
                                    expandedChapters = if (chExpanded)
                                        expandedChapters - cg.chapter.id
                                    else
                                        expandedChapters + cg.chapter.id
                                },
                                onToggleAll = {
                                    draft = if (chAllSel)
                                        draft - chapterIds
                                    else
                                        draft + chapterIds
                                }
                            )
                        }

                        // Section rows (animated)
                        if (chExpanded) {
                            cg.sections.forEach { sg ->
                                val secExpanded = sg.section.id in expandedSections
                                val sectionIds  = sg.points.map { it.point.id }.toSet()
                                val secAllSel   = sectionIds.isNotEmpty() && sectionIds.all { it in draft }
                                val secSomeSel  = sectionIds.any { it in draft }

                                item(key = "sec_${sg.section.id}") {
                                    SectionRow(
                                        section      = sg.section,
                                        pointCount   = sg.points.size,
                                        isExpanded   = secExpanded,
                                        allSelected  = secAllSel,
                                        someSelected = secSomeSel,
                                        onToggleExpand = {
                                            expandedSections = if (secExpanded)
                                                expandedSections - sg.section.id
                                            else
                                                expandedSections + sg.section.id
                                        },
                                        onToggleAll = {
                                            draft = if (secAllSel)
                                                draft - sectionIds
                                            else
                                                draft + sectionIds
                                        }
                                    )
                                }

                                // Point rows
                                if (secExpanded) {
                                    items(sg.points, key = { "kp_${it.point.id}" }) { kpFull ->
                                        val checked = kpFull.point.id in draft
                                        PointRow(
                                            kpFull  = kpFull,
                                            checked = checked,
                                            onToggle = {
                                                draft = if (checked)
                                                    draft - kpFull.point.id
                                                else
                                                    draft + kpFull.point.id
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(4.dp)) }
            }

            HorizontalDivider(color = FluentBorder)

            // ── Add new point inline ───────────────────────────────────────
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
                AddKpInlineForm(
                    allChapters = allChapters,
                    allSections = allSections,
                    onSave      = { secId, no, content ->
                        onAddNew(secId, no, content)
                        showAddForm = false
                    },
                    onCancel    = { showAddForm = false }
                )
            }
        }
    }
}

// ─── Chapter row ──────────────────────────────────────────────────────────────

@Composable
private fun ChapterRow(
    chapter: KpChapter,
    pointCount: Int,
    isExpanded: Boolean,
    allSelected: Boolean,
    someSelected: Boolean,
    onToggleExpand: () -> Unit,
    onToggleAll: () -> Unit
) {
    Surface(
        shape  = RoundedCornerShape(10.dp),
        color  = if (isExpanded) FluentBlue.copy(.10f)
                 else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .clickable(onClick = onToggleExpand)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Tri-state checkbox
            TriStateCheckbox(
                state    = when {
                    allSelected  -> ToggleableState.On
                    someSelected -> ToggleableState.Indeterminate
                    else         -> ToggleableState.Off
                },
                onClick  = onToggleAll,
                modifier = Modifier.size(20.dp)
            )
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (isExpanded) FluentBlue else FluentMuted.copy(.2f)
            ) {
                Text(
                    "第${chapter.no}章",
                    style      = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color      = if (isExpanded) androidx.compose.ui.graphics.Color.White
                                 else FluentMuted,
                    modifier   = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
            Text(
                chapter.name,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isExpanded) FontWeight.Bold else FontWeight.Normal,
                color      = if (isExpanded) FluentBlue
                             else MaterialTheme.colorScheme.onSurface,
                modifier   = Modifier.weight(1f)
            )
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = FluentMuted.copy(.12f)
            ) {
                Text(
                    "$pointCount 个",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = FluentMuted,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess
                              else Icons.Default.ExpandMore,
                contentDescription = null,
                tint     = if (isExpanded) FluentBlue else FluentMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ─── Section row ──────────────────────────────────────────────────────────────

@Composable
private fun SectionRow(
    section: KpSection,
    pointCount: Int,
    isExpanded: Boolean,
    allSelected: Boolean,
    someSelected: Boolean,
    onToggleExpand: () -> Unit,
    onToggleAll: () -> Unit
) {
    Surface(
        shape    = RoundedCornerShape(8.dp),
        color    = if (isExpanded) FluentTeal.copy(.08f)
                   else MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp)
    ) {
        Row(
            Modifier
                .clickable(onClick = onToggleExpand)
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TriStateCheckbox(
                state    = when {
                    allSelected  -> ToggleableState.On
                    someSelected -> ToggleableState.Indeterminate
                    else         -> ToggleableState.Off
                },
                onClick  = onToggleAll,
                modifier = Modifier.size(18.dp)
            )
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (isExpanded) FluentTeal else FluentMuted.copy(.15f)
            ) {
                Text(
                    "第${section.no}节",
                    style      = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = if (isExpanded) androidx.compose.ui.graphics.Color.White
                                 else FluentMuted,
                    modifier   = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
            Text(
                section.name,
                style    = MaterialTheme.typography.bodySmall,
                color    = if (isExpanded) FluentTeal
                           else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = FluentMuted.copy(.08f)
            ) {
                Text(
                    "$pointCount 条",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = FluentMuted,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess
                              else Icons.Default.ExpandMore,
                contentDescription = null,
                tint     = if (isExpanded) FluentTeal else FluentMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ─── Point row ────────────────────────────────────────────────────────────────

@Composable
private fun PointRow(
    kpFull: KpFull,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp)
            .clickable(onClick = onToggle)
            .padding(vertical = 5.dp, horizontal = 4.dp),
        verticalAlignment     = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Checkbox(
            checked         = checked,
            onCheckedChange = { onToggle() },
            modifier        = Modifier.size(20.dp).padding(top = 1.dp)
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                ColorChip(kpFull.code, FluentBlue)
                if (kpFull.isCustom) ColorChip("自定义", FluentAmber)
            }
            Text(
                kpFull.content,
                style   = MaterialTheme.typography.bodySmall,
                maxLines = 3
            )
        }
    }
    HorizontalDivider(
        color    = FluentBorder.copy(alpha = 0.4f),
        modifier = Modifier.padding(start = 24.dp)
    )
}

// ─── Inline add form (unchanged) ─────────────────────────────────────────────

@Composable
private fun AddKpInlineForm(
    allChapters: List<KpChapter>,
    allSections: List<KpSection>,
    onSave: (sectionId: Long, no: Int, content: String) -> Unit,
    onCancel: () -> Unit
) {
    val sortedChapters  = remember(allChapters) { allChapters.sortedBy { it.no } }
    var selectedChapter by remember { mutableStateOf(sortedChapters.firstOrNull()) }
    val sectionsForCh   = remember(selectedChapter, allSections) {
        allSections.filter { it.chapterId == selectedChapter?.id }.sortedBy { it.no }
    }
    var selectedSection by remember(selectedChapter) { mutableStateOf(sectionsForCh.firstOrNull()) }
    var no      by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    Surface(shape = RoundedCornerShape(12.dp), color = FluentBlueLight) {
        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "添加知识点",
                style      = MaterialTheme.typography.labelMedium,
                color      = FluentBlue,
                fontWeight = FontWeight.Bold
            )
            if (sortedChapters.isEmpty()) {
                Text(
                    "请先在知识点管理页添加章节",
                    style = MaterialTheme.typography.bodySmall,
                    color = FluentAmber
                )
            } else {
                FormDropdown(
                    "章",
                    selectedChapter?.let { "第${it.no}章 ${it.name}" } ?: "",
                    sortedChapters.map { "第${it.no}章 ${it.name}" }
                ) { sel ->
                    selectedChapter = sortedChapters.firstOrNull { "第${it.no}章 ${it.name}" == sel }
                    selectedSection = sectionsForCh.firstOrNull()
                }
                if (sectionsForCh.isEmpty()) {
                    Text("此章暂无节", style = MaterialTheme.typography.bodySmall, color = FluentAmber)
                } else {
                    FormDropdown(
                        "节",
                        selectedSection?.let { "第${it.no}节 ${it.name}" } ?: "",
                        sectionsForCh.map { "第${it.no}节 ${it.name}" }
                    ) { sel ->
                        selectedSection = sectionsForCh.firstOrNull { "第${it.no}节 ${it.name}" == sel }
                    }
                }
                FormTextField("条目号", no, { no = it }, "如 1")
                val preview = "${selectedChapter?.no ?: "?"}.${selectedSection?.no ?: "?"}.${no.ifBlank { "?" }}"
                Text(
                    "编号：$preview",
                    style      = MaterialTheme.typography.labelSmall,
                    color      = FluentBlue,
                    fontWeight = FontWeight.SemiBold
                )
            }
            OutlinedTextField(
                value         = content,
                onValueChange = { content = it },
                label         = { Text("知识点内容") },
                shape         = RoundedCornerShape(12.dp),
                modifier      = Modifier.fillMaxWidth(),
                minLines      = 2,
                maxLines      = 4,
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = FluentBlue,
                    unfocusedBorderColor = FluentBorder
                )
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick  = onCancel,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(10.dp)
                ) { Text("取消") }
                Button(
                    onClick  = {
                        val secId = selectedSection?.id ?: return@Button
                        val n     = no.toIntOrNull()    ?: return@Button
                        if (content.isNotBlank()) onSave(secId, n, content.trim())
                    },
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = FluentBlue)
                ) { Text("保存") }
            }
        }
    }
}
