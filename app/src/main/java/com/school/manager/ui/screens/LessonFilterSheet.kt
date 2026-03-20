package com.school.manager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.school.manager.data.AppState
import com.school.manager.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilterBottomSheet(
    state: AppState,
    fClass: Long,         onClassChange: (Long) -> Unit,
    fStatus: String,      onStatusChange: (String) -> Unit,
    fTeacher: Long,       onTeacherChange: (Long) -> Unit,
    fStudent: Long,       onStudentChange: (Long) -> Unit,
    fSubjects: Set<Long>, onSubjectsChange: (Set<Long>) -> Unit,
    fDayOfWeek: Set<Int>, onDayOfWeekChange: (Set<Int>) -> Unit,
    fFromDate: String,    onFromDateChange: (String) -> Unit,
    fToDate: String,      onToDateChange: (String) -> Unit,
    hasActive: Boolean,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("筛选条件",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                if (hasActive) {
                    TextButton(onClick = onClearAll) {
                        Text("清除全部", color = FluentRed,
                            style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            HorizontalDivider(color = FluentBorder)

            // ── 日期范围 ──────────────────────────────────────────────────────
            Text("日期范围", style = MaterialTheme.typography.labelSmall, color = FluentMuted)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    DatePickerField("开始日期", fFromDate) { onFromDateChange(it) }
                }
                Box(Modifier.weight(1f)) {
                    DatePickerField("结束日期", fToDate) { onToDateChange(it) }
                }
            }

            // ── 星期 ──────────────────────────────────────────────────────────
            Text("星期", style = MaterialTheme.typography.labelSmall, color = FluentMuted)
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement   = Arrangement.spacedBy(4.dp)
            ) {
                listOf(1 to "周一", 2 to "周二", 3 to "周三", 4 to "周四",
                       5 to "周五", 6 to "周六", 7 to "周日").forEach { (d, label) ->
                    FilterChip(
                        selected = fDayOfWeek.contains(d),
                        onClick  = {
                            onDayOfWeekChange(
                                if (fDayOfWeek.contains(d)) fDayOfWeek - d else fDayOfWeek + d
                            )
                        },
                        label = { Text(label) }
                    )
                }
            }

            // ── 科目 ──────────────────────────────────────────────────────────
            if (state.subjects.isNotEmpty()) {
                Text("科目", style = MaterialTheme.typography.labelSmall, color = FluentMuted)
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement   = Arrangement.spacedBy(4.dp)
                ) {
                    state.subjects.forEach { sub ->
                        FilterChip(
                            selected = fSubjects.contains(sub.id),
                            onClick  = {
                                onSubjectsChange(
                                    if (fSubjects.contains(sub.id)) fSubjects - sub.id
                                    else fSubjects + sub.id
                                )
                            },
                            label = { Text(sub.name) }
                        )
                    }
                }
            }

            // ── 班级 ──────────────────────────────────────────────────────────
            Text("班级", style = MaterialTheme.typography.labelSmall, color = FluentMuted)
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement   = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(selected = fClass == 0L, onClick = { onClassChange(0L) },
                    label = { Text("全部") })
                state.classes.forEach { cls ->
                    FilterChip(
                        selected = fClass == cls.id,
                        onClick  = { onClassChange(if (fClass == cls.id) 0L else cls.id) },
                        label    = { Text(cls.name) }
                    )
                }
            }

            // ── 状态 ──────────────────────────────────────────────────────────
            Text("状态", style = MaterialTheme.typography.labelSmall, color = FluentMuted)
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement   = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "" to "全部", "pending" to "待上课", "completed" to "已完成",
                    "absent" to "缺席", "cancelled" to "已取消", "postponed" to "已延期"
                ).forEach { (v, label) ->
                    FilterChip(
                        selected = fStatus == v,
                        onClick  = { onStatusChange(v) },
                        label    = { Text(label) }
                    )
                }
            }

            // ── 教师 ──────────────────────────────────────────────────────────
            if (state.teachers.isNotEmpty()) {
                Text("教师", style = MaterialTheme.typography.labelSmall, color = FluentMuted)
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement   = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(selected = fTeacher == 0L, onClick = { onTeacherChange(0L) },
                        label = { Text("全部") })
                    state.teachers.forEach { t ->
                        FilterChip(
                            selected = fTeacher == t.id,
                            onClick  = { onTeacherChange(if (fTeacher == t.id) 0L else t.id) },
                            label    = { Text(t.name) }
                        )
                    }
                }
            }

            // ── 学生 ──────────────────────────────────────────────────────────
            if (state.students.isNotEmpty()) {
                Text("学生", style = MaterialTheme.typography.labelSmall, color = FluentMuted)
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement   = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(selected = fStudent == 0L, onClick = { onStudentChange(0L) },
                        label = { Text("全部") })
                    state.students.forEach { s ->
                        FilterChip(
                            selected = fStudent == s.id,
                            onClick  = { onStudentChange(if (fStudent == s.id) 0L else s.id) },
                            label    = { Text(s.name) }
                        )
                    }
                }
            }
        }
    }
}
