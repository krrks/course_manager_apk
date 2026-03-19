package com.school.manager.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import java.time.LocalDate

// ── Batch generate dialog ─────────────────────────────────────────────────────

@Composable
internal fun BatchGenerateDialog(
    state: AppState, vm: AppViewModel, onDismiss: () -> Unit
) {
    var classId   by remember { mutableLongStateOf(state.classes.firstOrNull()?.id ?: 0L) }
    var recType   by remember { mutableStateOf("WEEKLY") }
    var dayOfWeek by remember { mutableIntStateOf(1) }
    var startDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var endDate   by remember { mutableStateOf(LocalDate.now().plusWeeks(12).toString()) }
    var startTime by remember { mutableStateOf("08:00") }
    var endTime   by remember { mutableStateOf("08:45") }
    var excludeSet by remember { mutableStateOf(emptySet<String>()) }

    var teacherOverrideId by remember {
        mutableStateOf(state.classes.firstOrNull()?.headTeacherId)
    }
    LaunchedEffect(classId) {
        teacherOverrideId = state.classes.find { it.id == classId }?.headTeacherId
    }

    val selectedClass           = state.classes.find { it.id == classId }
    val classDefaultTeacherName = state.teachers.find { it.id == selectedClass?.headTeacherId }?.name
    val effectiveTeacherName    = state.teachers.find { it.id == teacherOverrideId }?.name ?: ""

    FluentDialog(title = "批量生成课次", onDismiss = onDismiss, confirmText = "生成", onConfirm = {
        if (classId == 0L) return@FluentDialog
        val sDate = runCatching { LocalDate.parse(startDate) }.getOrNull() ?: LocalDate.now()
        val eDate = runCatching { LocalDate.parse(endDate) }.getOrNull() ?: sDate.plusWeeks(12)
        val classDefaultTeacherId = state.classes.find { it.id == classId }?.headTeacherId
        val overrideToSave = if (teacherOverrideId == classDefaultTeacherId) null else teacherOverrideId
        vm.batchGenerateLessons(
            classId           = classId,
            recurrenceType    = AppViewModel.RecurrenceType.valueOf(recType),
            startDate         = sDate, endDate = eDate,
            dayOfWeek         = dayOfWeek,
            startTime         = startTime, endTime = endTime,
            excludeDates      = excludeSet,
            teacherIdOverride = overrideToSave
        )
        onDismiss()
    }) {
        FormDropdown("班级", selectedClass?.name ?: "", state.classes.map { it.name }) { name ->
            classId = state.classes.firstOrNull { it.name == name }?.id ?: classId
        }
        FormDropdown(
            label    = if (classDefaultTeacherName != null) "教师（默认：$classDefaultTeacherName）" else "教师",
            selected = effectiveTeacherName.ifBlank { "无" },
            options  = listOf("无") + state.teachers.map { it.name }
        ) { picked ->
            teacherOverrideId = if (picked == "无") null
                                else state.teachers.firstOrNull { it.name == picked }?.id
        }

        SectionHeader("重复类型")
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("WEEKLY" to "每周", "DAILY" to "连续每天", "ONCE" to "单次").forEach { (v, label) ->
                FilterChip(selected = recType == v, onClick = { recType = v },
                    label = { Text(label) })
            }
        }

        if (recType == "WEEKLY") {
            SectionHeader("星期")
            Row(
                Modifier.padding(horizontal = 16.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DAYS.forEachIndexed { i, d ->
                    FilterChip(selected = dayOfWeek == i + 1, onClick = { dayOfWeek = i + 1 },
                        label = { Text("周$d") })
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top) {
            Box(Modifier.weight(1f)) {
                DatePickerField(if (recType == "ONCE") "日期" else "开始日期", startDate) { startDate = it }
            }
            if (recType != "ONCE") {
                Box(Modifier.weight(1f)) {
                    DatePickerField("结束日期", endDate) { endDate = it }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top) {
            Box(Modifier.weight(1f)) {
                StartTimeCompact(startTime) { newStart ->
                    val dur = minutesBetween(startTime, endTime).coerceAtLeast(30)
                    startTime = newStart; endTime = addMinutesToTime(newStart, dur)
                }
            }
        }
        DurationChipsCompact(startTime, endTime) { endTime = it }

        if (recType != "ONCE") {
            SectionHeader("跳过日期")
            ExcludeDatePicker(excludeSet) { date -> excludeSet = excludeSet + date }
            if (excludeSet.isNotEmpty()) {
                androidx.compose.foundation.layout.FlowRow(
                    modifier              = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement   = Arrangement.spacedBy(4.dp)
                ) {
                    excludeSet.sorted().forEach { d ->
                        InputChip(
                            selected     = false,
                            onClick      = { excludeSet = excludeSet - d },
                            label        = { Text(d, style = MaterialTheme.typography.labelSmall) },
                            trailingIcon = {
                                Icon(Icons.Default.Close, "删除", modifier = Modifier.size(14.dp))
                            }
                        )
                    }
                }
            } else {
                Text("暂未跳过任何日期",
                    style    = MaterialTheme.typography.bodySmall, color = FluentMuted,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExcludeDatePicker(current: Set<String>, onAdd: (String) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )
    OutlinedButton(
        onClick  = { showPicker = true },
        shape    = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("添加跳过日期", style = MaterialTheme.typography.labelMedium)
    }
    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton    = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { ms ->
                        val cal = java.util.Calendar.getInstance().apply { timeInMillis = ms }
                        val dateStr = "%04d-%02d-%02d".format(
                            cal.get(java.util.Calendar.YEAR),
                            cal.get(java.util.Calendar.MONTH) + 1,
                            cal.get(java.util.Calendar.DAY_OF_MONTH)
                        )
                        onAdd(dateStr)
                    }
                    showPicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("取消") } }
        ) { DatePicker(state = pickerState) }
    }
}

// ── Batch modify dialog ───────────────────────────────────────────────────────

@Composable
internal fun BatchModifyDialog(
    classId: Long, state: AppState, vm: AppViewModel, onDismiss: () -> Unit
) {
    val cls         = state.classes.find { it.id == classId }
    val allDates    = state.lessons.filter { it.classId == classId }.map { it.date }.sorted()
    val today       = LocalDate.now().toString()
    var fromDate    by remember { mutableStateOf(today) }
    var toDate      by remember { mutableStateOf(allDates.lastOrNull() ?: today) }
    var newStart    by remember { mutableStateOf("") }
    var newEnd      by remember { mutableStateOf("") }
    var skipMod     by remember { mutableStateOf(true) }
    var inclNonPend by remember { mutableStateOf(false) }

    FluentDialog(
        title       = "批量修改 — ${cls?.name ?: "班级"}",
        onDismiss   = onDismiss,
        confirmText = "批量修改",
        onConfirm   = {
            vm.batchModifyLessons(classId, fromDate, toDate,
                newStart.ifBlank { null }, newEnd.ifBlank { null },
                skipMod, inclNonPend)
            onDismiss()
        }
    ) {
        Text("默认不修改今天之前的课次。留空表示不修改该字段。",
            style    = MaterialTheme.typography.bodySmall, color = FluentMuted,
            modifier = Modifier.padding(horizontal = 16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top) {
            Box(Modifier.weight(1f)) { DatePickerField("开始日期", fromDate) { fromDate = it } }
            Box(Modifier.weight(1f)) { DatePickerField("结束日期", toDate)   { toDate   = it } }
        }
        SectionHeader("修改时间（留空则不修改）")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top) {
            Box(Modifier.weight(1f)) { StartTimeCompact(newStart.ifBlank { "08:00" }) { newStart = it } }
            Box(Modifier.weight(1f)) { StartTimeCompact(newEnd.ifBlank   { "08:45" }) { newEnd   = it } }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("跳过已单独修改的课次", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = skipMod, onCheckedChange = { skipMod = it })
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("包含已完成/已取消课次", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = inclNonPend, onCheckedChange = { inclNonPend = it })
        }
    }
}

// ── Batch delete dialog ───────────────────────────────────────────────────────

@Composable
internal fun BatchDeleteDialog(
    classId: Long, state: AppState, vm: AppViewModel, onDismiss: () -> Unit
) {
    val cls         = state.classes.find { it.id == classId }
    val allDates    = state.lessons.filter { it.classId == classId }.map { it.date }.sorted()
    val today       = LocalDate.now().toString()
    var fromDate    by remember { mutableStateOf(today) }
    var toDate      by remember { mutableStateOf(allDates.lastOrNull() ?: today) }
    var inclNonPend by remember { mutableStateOf(false) }
    var inclMod     by remember { mutableStateOf(false) }
    var confirmed   by remember { mutableStateOf(false) }

    val targetCount = state.lessons.count { l ->
        l.classId == classId && l.date >= fromDate && l.date <= toDate &&
        (inclNonPend || l.status == "pending") && (inclMod || !l.isModified)
    }

    FluentDialog(
        title       = "批量删除 — ${cls?.name ?: "班级"}",
        onDismiss   = onDismiss,
        confirmText = if (!confirmed) "确认删除 $targetCount 节" else "最终确认",
        onConfirm   = {
            if (!confirmed) { confirmed = true; return@FluentDialog }
            vm.batchDeleteLessons(classId, fromDate, toDate, inclNonPend, inclMod)
            onDismiss()
        }
    ) {
        Surface(shape = RoundedCornerShape(8.dp), color = FluentRed.copy(0.1f),
            modifier = Modifier.fillMaxWidth()) {
            Text("⚠️ 此操作不可撤销！将删除 $targetCount 节课次。",
                style    = MaterialTheme.typography.bodySmall, color = FluentRed,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top) {
            Box(Modifier.weight(1f)) {
                DatePickerField("开始日期", fromDate) { fromDate = it; confirmed = false }
            }
            Box(Modifier.weight(1f)) {
                DatePickerField("结束日期", toDate) { toDate = it; confirmed = false }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("包含已完成/已取消课次", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = inclNonPend, onCheckedChange = { inclNonPend = it; confirmed = false })
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("包含已单独修改的课次", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = inclMod, onCheckedChange = { inclMod = it; confirmed = false })
        }
    }
}
