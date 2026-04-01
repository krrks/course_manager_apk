package com.school.manager.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
    val state by vm.state.collectAsState()
    var fGrade by remember { mutableStateOf("") }
    var query  by remember { mutableStateOf("") }

    var expandedChapters by remember { mutableStateOf(emptySet<Long>()) }
    var expandedSections by remember { mutableStateOf(emptySet<Long>()) }

    var showAddChapter by remember { mutableStateOf(false) }
    var showAddSection by remember { mutableStateOf(false) }
    var showAddPoint   by remember { mutableStateOf(false) }
    var viewingChapter by remember { mutableStateOf<KpChapter?>(null) }
    var editingChapter by remember { mutableStateOf<KpChapter?>(null) }
    var viewingSection by remember { mutableStateOf<KpSection?>(null) }
    var editingSection by remember { mutableStateOf<KpSection?>(null) }
    var viewingPoint   by remember { mutableStateOf<KpFull?>(null) }
    var editingPoint   by remember { mutableStateOf<KpFull?>(null) }

    // Derive available grades dynamically from existing chapters
    val availableGrades = remember(state.kpChapters) {
        state.kpChapters.map { it.grade }.distinct().sorted()
    }

    // Precompute all KpFull for search filtering
    val allKpFull = remember(state.knowledgePoints, state.kpChapters, state.kpSections) {
        state.knowledgePoints.mapNotNull { state.kpFull(it) }
    }

    val visibleChapters = state.kpChapters
        .filter { fGrade.isBlank() || it.grade == fGrade }
        .sortedBy { it.no }

    // When searching, auto-expand chapters/sections with matches
    LaunchedEffect(query) {
        if (query.isNotBlank()) {
            val matchIds = allKpFull.filter {
                it.content.contains(query, ignoreCase = true) ||
                it.title.contains(query, ignoreCase = true) ||
                it.code.contains(query, ignoreCase = true) ||
                it.chapter.name.contains(query, ignoreCase = true) ||
                it.section.name.contains(query, ignoreCase = true)
            }
            expandedChapters = matchIds.map { it.chapter.id }.toSet()
            expandedSections = matchIds.map { it.section.id }.toSet()
        } else {
            expandedChapters = emptySet(); expandedSections = emptySet()
        }
    }
    LaunchedEffect(fGrade) { expandedChapters = emptySet(); expandedSections = emptySet() }

    Scaffold(
        floatingActionButton = {
            ScreenSpeedDialFab(
                addLabel = "添加知识点", addIcon = Icons.Default.Add,
                onAdd = { showAddPoint = true }, onOpenDrawer = onOpenDrawer,
                extraItems = {
                    SpeedDialItem("添加节", Icons.Default.Bookmarks, FluentTeal) { showAddSection = true }
                    SpeedDialItem("添加章", Icons.Default.AutoStories, FluentPurple) { showAddChapter = true }
                }
            )
        }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(top = inner.calculateTopPadding(), bottom = inner.calculateBottomPadding())) {
            // Grade filter chips — derived from existing chapters
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = fGrade.isBlank(), onClick = { fGrade = "" }, label = { Text("全部学段") })
                availableGrades.forEach { g ->
                    FilterChip(selected = fGrade == g, onClick = { fGrade = if (fGrade == g) "" else g }, label = { Text(g) })
                }
            }
            // Search
            OutlinedTextField(
                value = query, onValueChange = { query = it }, label = { Text("搜索") },
                singleLine = true, shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (query.isNotBlank()) IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Clear, null, modifier = Modifier.size(16.dp))
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FluentBlue, unfocusedBorderColor = FluentBorder)
            )
            // Summary
            val totalPoints = allKpFull.count { fGrade.isBlank() || it.grade == fGrade }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${visibleChapters.size} 章 · $totalPoints 个知识点",
                    style = MaterialTheme.typography.labelMedium, color = FluentMuted)
                if (visibleChapters.isNotEmpty()) {
                    val allExp = visibleChapters.all { it.id in expandedChapters }
                    TextButton(
                        onClick = { expandedChapters = if (allExp) emptySet() else visibleChapters.map { it.id }.toSet() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(if (allExp) "全部折叠" else "全部展开", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxSize()) {
                if (visibleChapters.isEmpty()) { item { EmptyState("📖", "暂无知识点，点击右下角按钮添加章节") } }
                else {
                    visibleChapters.forEach { chapter ->
                        val chExpanded = chapter.id in expandedChapters
                        val sections = state.kpSections.filter { it.chapterId == chapter.id }.sortedBy { it.no }
                        val chapterPointCount = allKpFull.count { it.chapter.id == chapter.id }

                        item(key = "ch_${chapter.id}") {
                            KpChapterCard(chapter = chapter, pointCount = chapterPointCount, isExpanded = chExpanded,
                                onClick = { expandedChapters = if (chExpanded) expandedChapters - chapter.id else expandedChapters + chapter.id },
                                onLongClick = { viewingChapter = chapter })
                        }

                        if (chExpanded) {
                            sections.forEach { section ->
                                val secExpanded = section.id in expandedSections
                                val points = allKpFull.filter { it.section.id == section.id }
                                    .let { list -> if (query.isBlank()) list else list.filter {
                                        it.content.contains(query, ignoreCase = true) ||
                                        it.title.contains(query, ignoreCase = true) ||
                                        it.code.contains(query, ignoreCase = true)
                                    }}.sortedBy { it.point.no }

                                item(key = "sec_${section.id}") {
                                    AnimatedVisibility(visible = true, enter = expandVertically() + fadeIn()) {
                                        KpSectionCard(section = section, pointCount = points.size, isExpanded = secExpanded,
                                            onClick = { expandedSections = if (secExpanded) expandedSections - section.id else expandedSections + section.id },
                                            onLongClick = { viewingSection = section })
                                    }
                                }

                                if (secExpanded) {
                                    points.forEach { kpFull ->
                                        item(key = "kp_${kpFull.point.id}") {
                                            AnimatedVisibility(visible = true, enter = expandVertically() + fadeIn()) {
                                                KpPointCard(kpFull = kpFull, onClick = { viewingPoint = kpFull })
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    viewingChapter?.let { ch ->
        ChapterDetailDialog(ch, vm,
            onDismiss = { viewingChapter = null },
            onEdit    = { editingChapter = ch; viewingChapter = null },
            onDelete  = { vm.deleteKpChapter(ch.id); viewingChapter = null })
    }
    editingChapter?.let { ch ->
        ChapterFormDialog("编辑章", ch, existingGrades = availableGrades,
            onDismiss = { editingChapter = null },
            onSave    = { vm.updateKpChapter(it); editingChapter = null })
    }
    viewingSection?.let { sec ->
        SectionDetailDialog(sec, state.kpChapters, vm,
            onDismiss = { viewingSection = null },
            onEdit    = { editingSection = sec; viewingSection = null },
            onDelete  = { vm.deleteKpSection(sec.id); viewingSection = null })
    }
    editingSection?.let { sec ->
        SectionFormDialog("编辑节", sec, state.kpChapters,
            onDismiss = { editingSection = null },
            onSave    = { vm.updateKpSection(it); editingSection = null })
    }
    viewingPoint?.let { kp ->
        PointDetailDialog(kp, vm,
            onDismiss = { viewingPoint = null },
            onEdit    = { editingPoint = kp; viewingPoint = null },
            onDelete  = { vm.deleteKnowledgePoint(kp.point.id); viewingPoint = null })
    }
    editingPoint?.let { kp ->
        PointFormDialog("编辑知识点", kp.point, state.kpChapters, state.kpSections,
            onDismiss = { editingPoint = null },
            onSave    = { vm.updateKnowledgePoint(it); editingPoint = null })
    }
    if (showAddChapter) ChapterFormDialog("添加章", null, existingGrades = availableGrades,
        onDismiss = { showAddChapter = false },
        onSave    = { vm.addKpChapter(it.grade, it.no, it.name); showAddChapter = false })
    if (showAddSection) SectionFormDialog("添加节", null, state.kpChapters,
        onDismiss = { showAddSection = false },
        onSave    = { vm.addKpSection(it.chapterId, it.no, it.name); showAddSection = false })
    if (showAddPoint) PointFormDialog("添加知识点", null, state.kpChapters, state.kpSections,
        onDismiss = { showAddPoint = false },
        onSave    = { vm.addKnowledgePoint(it.sectionId, it.no, it.title, it.content); showAddPoint = false })
}

// ── Chapter card ──────────────────────────────────────────────────────────────

@Composable
internal fun KpChapterCard(chapter: KpChapter, pointCount: Int, isExpanded: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(10.dp),
        color = if (isExpanded) FluentBlue.copy(.12f) else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(shape = RoundedCornerShape(6.dp), color = if (isExpanded) FluentBlue else FluentMuted.copy(.2f)) {
                Text("第${chapter.no}章", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                    color = if (isExpanded) androidx.compose.ui.graphics.Color.White else FluentMuted,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(chapter.name, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isExpanded) FontWeight.Bold else FontWeight.Normal,
                    color = if (isExpanded) FluentBlue else MaterialTheme.colorScheme.onSurface)
                Text(chapter.grade, style = MaterialTheme.typography.labelSmall, color = FluentMuted)
            }
            Surface(shape = RoundedCornerShape(20.dp), color = if (isExpanded) FluentBlue.copy(.15f) else FluentMuted.copy(.12f)) {
                Text("$pointCount 个", style = MaterialTheme.typography.labelSmall,
                    color = if (isExpanded) FluentBlue else FluentMuted, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
            }
            TextButton(onClick = onLongClick, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.MoreVert, null, tint = FluentMuted, modifier = Modifier.size(16.dp))
            }
            Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null,
                tint = if (isExpanded) FluentBlue else FluentMuted, modifier = Modifier.size(20.dp))
        }
    }
}

// ── Section card ──────────────────────────────────────────────────────────────

@Composable
internal fun KpSectionCard(section: KpSection, pointCount: Int, isExpanded: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(8.dp),
        color = if (isExpanded) FluentTeal.copy(.10f) else MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp).clickable(onClick = onClick)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(shape = RoundedCornerShape(4.dp), color = if (isExpanded) FluentTeal else FluentMuted.copy(.15f)) {
                Text("第${section.no}节", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold,
                    color = if (isExpanded) androidx.compose.ui.graphics.Color.White else FluentMuted,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
            }
            Text(section.name, style = MaterialTheme.typography.bodyMedium,
                color = if (isExpanded) FluentTeal else MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Surface(shape = RoundedCornerShape(20.dp), color = FluentMuted.copy(.10f)) {
                Text("$pointCount 条", style = MaterialTheme.typography.labelSmall, color = FluentMuted,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
            TextButton(onClick = onLongClick, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.MoreVert, null, tint = FluentMuted, modifier = Modifier.size(14.dp))
            }
            Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null,
                tint = if (isExpanded) FluentTeal else FluentMuted, modifier = Modifier.size(18.dp))
        }
    }
}

// ── Point card ────────────────────────────────────────────────────────────────

@Composable
internal fun KpPointCard(kpFull: KpFull, onClick: () -> Unit) {
    FluentCard(modifier = Modifier.fillMaxWidth().padding(start = 32.dp), onClick = onClick) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                ColorChip(kpFull.code, FluentBlue)
                if (kpFull.isCustom) ColorChip("自定义", FluentAmber)
            }
            Text(kpFull.displayTitle, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            if (kpFull.title.isNotBlank()) {
                Text(kpFull.content, style = MaterialTheme.typography.bodySmall, color = FluentMuted, maxLines = 3)
            }
        }
    }
}
