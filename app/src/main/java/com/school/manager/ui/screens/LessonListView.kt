package com.school.manager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.school.manager.data.*
import com.school.manager.ui.components.*
import com.school.manager.ui.theme.*
import java.time.LocalDate

// ── List view ─────────────────────────────────────────────────────────────────

@Composable
internal fun ListView(
    lessons: List<Lesson>,
    state: AppState,
    progressMap: Map<Long, Pair<Int, Int>>,
    currentView: String,
    onViewChange: (String) -> Unit,
    onLessonClick: (Lesson) -> Unit,
    onBatchModify: (Long) -> Unit,
    onBatchDelete: (Long) -> Unit,
    isSelectionMode: Boolean,
    selectedIds: Set<Long>,
    onSelectionChange: (Set<Long>) -> Unit,
    onEnterSelectionMode: () -> Unit,
    onExitSelectionMode: () -> Unit,
    onBatchAction: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {

        // ── Header ────────────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth().background(FluentBlue)
                                        .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (isSelectionMode) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier              = Modifier.weight(1f).padding(start = 4.dp)
                ) {
                    IconButton(onClick = onExitSelectionMode, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, null, tint = Color.White,
                            modifier = Modifier.size(18.dp))
                    }
                    Text("已选 ${selectedIds.size} 节",
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    TextButton(
                        onClick        = { onSelectionChange(lessons.map { it.id }.toSet()) },
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Text("全选", color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.labelMedium)
                    }
                }
            } else {
                Text("课次列表",
                    color    = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    modifier = Modifier.padding(start = 8.dp).weight(1f))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isSelectionMode) {
                    IconButton(onClick = onEnterSelectionMode, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.CheckBox, null,
                            tint     = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp))
                    }
                }
                ViewSwitchIcons(currentView, onViewChange)
            }
        }

        // ── Course list ───────────────────────────────────────────────────
        LazyColumn(
            contentPadding      = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier            = Modifier.weight(1f)
        ) {
            if (lessons.isEmpty()) {
                item { EmptyState("📅", "暂无课次") }
            } else {
                val grouped = lessons.groupBy { it.classId }
                    .toSortedMap(compareBy { state.classes.find { c -> c.id == it }?.name ?: "" })

                grouped.forEach { (classId, classLessons) ->
                    val cls           = state.classes.find { it.id == classId }
                    val (done, total) = progressMap[classId] ?: (0 to 0)
                    val color         = cls?.resolvedSubject(state.subjects)?.let {
                        Color(SUBJECT_COLORS[
                            (state.subjects.indexOf(it)).coerceAtLeast(0) % SUBJECT_COLORS.size
                        ])
                    } ?: FluentBlue
                    val groupIds    = classLessons.map { it.id }.toSet()
                    val allSelected = groupIds.isNotEmpty() && groupIds.all { it in selectedIds }

                    item(key = "header_$classId") {
                        Row(
                            Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (isSelectionMode) {
                                Checkbox(
                                    checked         = allSelected,
                                    onCheckedChange = { checked ->
                                        onSelectionChange(
                                            if (checked) selectedIds + groupIds
                                            else         selectedIds - groupIds
                                        )
                                    }
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(cls?.name ?: "班级 $classId",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = color, fontWeight = FontWeight.Bold)
                                if (total > 0)
                                    Text("已完成 $done / $total 节",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = FluentMuted)
                            }
                        }
                        if (total > 0)
                            FluentProgressBar(done.toFloat() / total, color,
                                Modifier.fillMaxWidth().padding(bottom = 4.dp))
                    }

                    items(classLessons.sortedBy { it.date }, key = { "lesson_${it.id}" }) { l ->
                        LessonCard(
                            l               = l,
                            state           = state,
                            accentColor     = color,
                            isSelectionMode = isSelectionMode,
                            isSelected      = l.id in selectedIds,
                            onToggleSelect  = {
                                onSelectionChange(
                                    if (l.id in selectedIds) selectedIds - l.id
                                    else                     selectedIds + l.id
                                )
                            },
                            onClick = { onLessonClick(it) }
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(if (isSelectionMode) 120.dp else 80.dp)) }
        }

        // ── Selection bottom bar ──────────────────────────────────────────
        if (isSelectionMode) {
            SelectionBottomBar(
                count         = selectedIds.size,
                onBatchAction = onBatchAction,
                onClearAll    = { onSelectionChange(emptySet()) }
            )
        }
    }
}

// ── Selection bottom bar ──────────────────────────────────────────────────────

@Composable
private fun SelectionBottomBar(count: Int, onBatchAction: () -> Unit, onClearAll: () -> Unit) {
    Surface(
        shadowElevation = 8.dp,
        color           = MaterialTheme.colorScheme.surface,
        modifier        = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            TextButton(onClick = onClearAll) {
                Text("取消选择", color = FluentMuted)
            }
            Text("已选 $count 节",
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color      = FluentMuted)
            Button(
                onClick  = onBatchAction,
                enabled  = count > 0,
                shape    = RoundedCornerShape(10.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = FluentBlue)
            ) { Text("批量操作") }
        }
    }
}

// ── Lesson card ───────────────────────────────────────────────────────────────

@Composable
internal fun LessonCard(
    l: Lesson,
    state: AppState,
    accentColor: Color,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onClick: (Lesson) -> Unit
) {
    // Resolve day-of-week label from date string
    val dowLabel = remember(l.date) {
        runCatching {
            val d = LocalDate.parse(l.date)
            "周${DAYS.getOrElse(d.dayOfWeek.value - 1) { "?" }}"
        }.getOrDefault("")
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        if (isSelectionMode) {
            Checkbox(
                checked         = isSelected,
                onCheckedChange = { onToggleSelect() },
                modifier        = Modifier.padding(end = 4.dp)
            )
        }
        FluentCard(
            accentColor = accentColor,
            modifier    = Modifier.weight(1f),
            onClick     = { if (isSelectionMode) onToggleSelect() else onClick(l) }
        ) {
            Row(Modifier.padding(12.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Date column: day number + month + day-of-week
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(44.dp)) {
                    val parts = l.date.split("-")
                    Text(parts.getOrElse(2) { "?" },
                        style      = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold, color = accentColor)
                    Text("${parts.getOrElse(1) { "?" }}月",
                        style = MaterialTheme.typography.labelSmall, color = FluentMuted)
                    if (dowLabel.isNotBlank())
                        Text(dowLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor, fontWeight = FontWeight.SemiBold)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment     = Alignment.CenterVertically) {
                        Text(l.subjectName(state.classes, state.subjects),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, color = accentColor)
                        StatusChip(l.status)
                        if (l.isModified)
                            Surface(shape = RoundedCornerShape(4.dp),
                                color = FluentAmber.copy(0.15f)) {
                                Text("已改", style = MaterialTheme.typography.labelSmall,
                                    color    = FluentAmber,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                    }
                    if (l.topic.isNotBlank())
                        Text("📌 ${l.topic}",
                            style = MaterialTheme.typography.bodyMedium, color = FluentMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (l.startTime.isNotBlank())
                            Text("🕐 ${l.startTime}–${l.endTime}",
                                style = MaterialTheme.typography.bodySmall, color = FluentMuted)
                        Text("👩‍🏫 ${l.teacherName(state.classes, state.teachers)}",
                            style = MaterialTheme.typography.bodySmall, color = FluentMuted)
                    }
                }
            }
        }
    }
}
