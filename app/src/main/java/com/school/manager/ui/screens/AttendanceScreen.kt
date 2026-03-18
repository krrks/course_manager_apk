package com.school.manager.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.school.manager.data.*
import com.school.manager.ui.screens.DatePickerField
import com.school.manager.ui.screens.TimeRangeRow
import com.school.manager.ui.components.*
import com.school.manager.ui.theme.*
import com.school.manager.viewmodel.AppViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields

// ── Time-axis layout constants ─────────────────────────────────────────────────
private const val DP_PER_HOUR = 80f
private const val DP_PER_MIN  = DP_PER_HOUR / 60f
private const val TIME_COL_W  = 52
private const val DAY_COL_W   = 120

private fun minuteOffsetDp(hhmm: String): Float {
    if (hhmm.isBlank()) return 0f
    val mins = timeToMinutes(hhmm) - CAL_START_HOUR * 60
    return (mins * DP_PER_MIN).coerceAtLeast(0f)
}

private fun durationDp(start: String, end: String): Float {
    if (start.isBlank() || end.isBlank()) return DP_PER_HOUR / 2f
    return (timeToMinutes(end) - timeToMinutes(start)).coerceAtLeast(10) * DP_PER_MIN
}

private const val CAL_V_PAD = 10f
private val totalCalHeightDp: Float get() = (CAL_END_HOUR - CAL_START_HOUR) * DP_PER_HOUR

private fun packedToColor(packed: Long): Color = Color(packed.toInt())

// ─────────────────────────────────────────────────────────────────────────────


// ── Time helpers (mirrors of ScheduleScreen private helpers) ─────────────────
private fun addMinutesToTime(hhmm: String, minutes: Int): String {
    val base  = if (hhmm.isBlank()) 8 * 60 else timeToMinutes(hhmm)
    val total = (base + minutes).coerceIn(0, 23 * 60 + 59)
    return "%02d:%02d".format(total / 60, total % 60)
}

private fun minutesBetween(start: String, end: String): Int =
    if (start.isBlank() || end.isBlank()) 60
    else (timeToMinutes(end) - timeToMinutes(start)).coerceAtLeast(0)

@Composable
fun AttendanceScreen(vm: AppViewModel, onOpenDrawer: () -> Unit = {}) {
    val state    by vm.state.collectAsState()
    var view     by remember { mutableStateOf("list") }
    var showAdd  by remember { mutableStateOf(false) }
    var viewing  by remember { mutableStateOf<Attendance?>(null) }
    var editing  by remember { mutableStateOf<Attendance?>(null) }
    var calDate  by remember { mutableStateOf(LocalDate.now()) }
    var fTeacher by remember { mutableLongStateOf(0L) }
    var fStudent by remember { mutableLongStateOf(0L) }
    var sortDesc by remember { mutableStateOf(true) }

    val records = state.attendance
        .filter { r ->
            (fTeacher == 0L || r.teacherId == fTeacher) &&
            (fStudent == 0L || r.attendees.contains(fStudent))
        }
        .sortedWith(if (sortDesc) compareByDescending { it.date } else compareBy { it.date })

    Scaffold(
        floatingActionButton = {
            ScreenSpeedDialFab(
                addLabel     = "添加记录",
                addIcon      = Icons.Default.Add,
                onAdd        = { showAdd = true },
                onOpenDrawer = onOpenDrawer
            )
        }
    ) { inner ->
        Column(modifier = Modifier.fillMaxSize()
            .padding(top = inner.calculateTopPadding(), bottom = inner.calculateBottomPadding())) {
            // View-mode chips
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = view == "list",  onClick = { view = "list"  }, label = { Text("📋 列表") })
                FilterChip(selected = view == "week",  onClick = { view = "week"  }, label = { Text("📅 周视图") })
                FilterChip(selected = view == "month", onClick = { view = "month" }, label = { Text("🗓 月视图") })
                FilterChip(selected = view == "day",   onClick = { view = "day"   }, label = { Text("☀️ 日视图") })
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { sortDesc = !sortDesc }) {
                    Icon(if (sortDesc) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        "排序", tint = FluentBlue)
                }
            }
            // Filter chips
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DropdownFilterChip("全部教师", state.teachers.map { it.id to it.name }, fTeacher) { fTeacher = it }
                DropdownFilterChip("全部学生", state.students.map { it.id to it.name }, fStudent) { fStudent = it }
            }

            when (view) {
                "list"  -> ListView(records, vm)     { viewing = it }
                "week"  -> WeekView(records, calDate, { calDate = it }, vm) { viewing = it }
                "month" -> MonthView(records, calDate, { calDate = it }, vm) { viewing = it }
                "day"   -> DayView(records,  calDate, { calDate = it }, vm) { viewing = it }
            }
        }
    }

    viewing?.let { rec ->
        AttendanceDetailDialog(rec, state, vm,
            onDismiss = { viewing = null },
            onEdit    = { editing = rec; viewing = null },
            onDelete  = { vm.deleteAttendance(rec.id); viewing = null })
    }
    editing?.let { rec ->
        AttendanceFormDialog("编辑记录", rec, state, vm,
            onDismiss = { editing = null },
            onSave    = { updated -> vm.updateAttendance(updated); editing = null })
    }
    if (showAdd) {
        AttendanceFormDialog("添加记录", null, state, vm,
            onDismiss = { showAdd = false },
            onSave    = { a ->
                vm.addAttendance(a.classId, a.subjectId, a.teacherId, a.date,
                    a.startTime, a.endTime, a.topic, a.status, a.notes, a.attendees)
                showAdd = false
            })
    }
}

// ── List view ─────────────────────────────────────────────────────────────────

@Composable
private fun ListView(records: List<Attendance>, vm: AppViewModel, onClick: (Attendance) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()) {
        if (records.isEmpty()) item { EmptyState("📋", "暂无上课记录") }
        else items(records) { rec -> AttendanceCard(rec, vm) { onClick(rec) } }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun AttendanceCard(rec: Attendance, vm: AppViewModel, onClick: () -> Unit) {
    val sub      = vm.subject(rec.subjectId)
    val te       = vm.teacher(rec.teacherId)
    val cl       = vm.schoolClass(rec.classId)
    val colorIdx = (vm.state.value.subjects.indexOfFirst { it.id == rec.subjectId }.takeIf { it >= 0 }
        ?: (rec.classId % SUBJECT_COLORS.size).toInt())
    val color    = Color(SUBJECT_COLORS[colorIdx % SUBJECT_COLORS.size])
    val startStr = rec.resolvedStart()
    FluentCard(accentColor = color, modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    // BUG-5 FIX: resolve via subjectId FK first
                    Text(rec.resolvedSubjectName(vm.state.value.subjects, vm.state.value.classes),
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
                    Text("· ${cl?.name ?: "?"}",
                        style = MaterialTheme.typography.bodyMedium, color = FluentMuted)
                    StatusBadge(rec.status)
                }
                if (rec.topic.isNotBlank())
                    Text("📌 ${rec.topic}", style = MaterialTheme.typography.bodyMedium, color = FluentMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("👩‍🏫 ${te?.name ?: "─"}",
                        style = MaterialTheme.typography.bodySmall, color = FluentMuted)
                    Text("📅 ${rec.date}",
                        style = MaterialTheme.typography.bodySmall, color = FluentMuted)
                    if (startStr.isNotBlank())
                        Text("🕐 $startStr",
                            style = MaterialTheme.typography.bodySmall, color = FluentMuted)
                }
            }
        }
    }
}

// ── Week view ─────────────────────────────────────────────────────────────────

@Composable
private fun WeekView(
    records: List<Attendance>,
    current: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    vm: AppViewModel,
    onRecordClick: (Attendance) -> Unit
) {
    val monday = current.with(DayOfWeek.MONDAY)
    val days   = (0..6).map { monday.plusDays(it.toLong()) }
    val fmt    = DateTimeFormatter.ofPattern("M/d")

    Column(Modifier.fillMaxSize()) {
        // Header nav
        Row(Modifier.fillMaxWidth().background(FluentBlue).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = { onDateChange(current.minusWeeks(1)) }) {
                Icon(Icons.Default.ChevronLeft, null, tint = Color.White) }
            val weekNum = current.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
            Text("第 $weekNum 周  ${monday.format(fmt)} – ${monday.plusDays(6).format(fmt)}",
                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            IconButton(onClick = { onDateChange(current.plusWeeks(1)) }) {
                Icon(Icons.Default.ChevronRight, null, tint = Color.White) }
        }
        // Day columns
        val scrollV = rememberScrollState()
        Row(Modifier.fillMaxSize()) {
            // Fixed time column
            Box(Modifier.width(TIME_COL_W.dp).verticalScroll(scrollV)
                .background(MaterialTheme.colorScheme.surfaceVariant)) {
                Box(Modifier.height((totalCalHeightDp + CAL_V_PAD * 2).dp)) {
                    for (h in CAL_START_HOUR..CAL_END_HOUR) {
                        val top = ((h - CAL_START_HOUR) * DP_PER_HOUR + CAL_V_PAD - 7f).dp
                        Text("%02d:00".format(h), style = MaterialTheme.typography.labelSmall,
                            color = FluentMuted, modifier = Modifier.offset(y = top).padding(start = 4.dp))
                    }
                }
            }
            // Scrollable day columns
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                days.forEach { day ->
                    val dayRecs = records.filter { it.date == day.toString() }
                    val isToday = day == LocalDate.now()
                    Column(Modifier.width(DAY_COL_W.dp)) {
                        Box(Modifier.fillMaxWidth().background(
                            if (isToday) FluentBlue.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant).padding(4.dp),
                            contentAlignment = Alignment.Center) {
                            Text("${day.dayOfWeek.value}  ${day.format(fmt)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (isToday) FluentBlue else FluentMuted)
                        }
                        Box(Modifier.width(DAY_COL_W.dp).verticalScroll(scrollV)
                            .height((totalCalHeightDp + CAL_V_PAD * 2).dp)
                            .border(0.5.dp, FluentBorder)) {
                            for (h in CAL_START_HOUR..CAL_END_HOUR) {
                                val top = ((h - CAL_START_HOUR) * DP_PER_HOUR + CAL_V_PAD).dp
                                Box(Modifier.offset(y = top).fillMaxWidth().height(0.5.dp).background(FluentBorder))
                            }
                            dayRecs.forEach { rec ->
                                val startStr = rec.resolvedStart()
                                val endStr   = rec.resolvedEnd()
                                if (startStr.isBlank()) return@forEach
                                val sub   = vm.subject(rec.subjectId)
                                val cl    = vm.schoolClass(rec.classId)
                                val color = packedToColor(sub?.color ?: SUBJECT_COLORS[0])
                                Box(Modifier
                                    .offset(y = (minuteOffsetDp(startStr) + CAL_V_PAD).dp)
                                    .fillMaxWidth().height(durationDp(startStr, endStr).coerceAtLeast(56f).dp)
                                    .padding(horizontal = 2.dp, vertical = 1.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(color.copy(alpha = 0.15f))
                                    .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    .clickable { onRecordClick(rec) }
                                    .padding(4.dp)) {
                                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                        // BUG-5 FIX: use resolvedSubjectName (Attendance extension in Models.kt)
                    Text(rec.resolvedSubjectName(vm.state.value.subjects, vm.state.value.classes),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold, color = color, maxLines = 1)
                                        Text("$startStr–$endStr",
                                            style = MaterialTheme.typography.labelSmall, color = FluentMuted, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Month view ────────────────────────────────────────────────────────────────

@Composable
private fun MonthView(
    records: List<Attendance>,
    current: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    vm: AppViewModel,
    onRecordClick: (Attendance) -> Unit
) {
    val ym     = YearMonth.from(current)
    val first  = ym.atDay(1)
    val offset = (first.dayOfWeek.value % 7)
    val days   = ym.lengthOfMonth()

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().background(FluentBlue).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = { onDateChange(current.minusMonths(1)) }) {
                Icon(Icons.Default.ChevronLeft, null, tint = Color.White) }
            Text("${current.year}年${current.monthValue}月",
                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            IconButton(onClick = { onDateChange(current.plusMonths(1)) }) {
                Icon(Icons.Default.ChevronRight, null, tint = Color.White) }
        }
        // Day-of-week header
        Row(Modifier.fillMaxWidth()) {
            listOf("日","一","二","三","四","五","六").forEach { d ->
                Text(d, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall,
                    color = FluentMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        // Grid
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            val cells = offset + days
            val rows  = (cells + 6) / 7
            items(rows) { row ->
                Row(Modifier.fillMaxWidth()) {
                    for (col in 0..6) {
                        val idx = row * 7 + col
                        val day = if (idx < offset || idx >= offset + days) null
                        else first.plusDays((idx - offset).toLong())
                        Box(Modifier.weight(1f).height(64.dp)
                            .border(0.25.dp, FluentBorder)
                            .background(if (day == LocalDate.now()) FluentBlue.copy(0.08f) else Color.Transparent)
                            .clickable(enabled = day != null) { day?.let { onDateChange(it) } }
                            .padding(2.dp)) {
                            if (day != null) {
                                val dayRecs = records.filter { it.date == day.toString() }
                                Column {
                                    Text(day.dayOfMonth.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (day == LocalDate.now()) FontWeight.Bold else FontWeight.Normal,
                                        color = if (day == LocalDate.now()) FluentBlue else FluentMuted)
                                    dayRecs.take(3).forEach { rec ->
                                        val sub = vm.subject(rec.subjectId)
                                        val cl  = vm.schoolClass(rec.classId)
                                        val color = packedToColor(sub?.color ?: SUBJECT_COLORS[0])
                                        Text(rec.resolvedSubjectName(vm.state.value.subjects, vm.state.value.classes),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = color, maxLines = 1,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(color.copy(0.12f))
                                                .padding(horizontal = 2.dp)
                                                .clickable { onRecordClick(rec) })
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

// ── Day view ──────────────────────────────────────────────────────────────────

@Composable
private fun DayView(
    records: List<Attendance>,
    current: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    vm: AppViewModel,
    onRecordClick: (Attendance) -> Unit
) {
    val dow    = listOf("","一","二","三","四","五","六","日")[current.dayOfWeek.value]
    val dayRecs = records.filter { it.date == current.toString() }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().background(FluentBlue).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = { onDateChange(current.minusDays(1)) }) {
                Icon(Icons.Default.ChevronLeft, null, tint = Color.White) }
            Text("${current.monthValue}月${current.dayOfMonth}日  $dow",
                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            IconButton(onClick = { onDateChange(current.plusDays(1)) }) {
                Icon(Icons.Default.ChevronRight, null, tint = Color.White) }
        }

        val scrollV = rememberScrollState()
        Row(Modifier.fillMaxSize()) {
            // Fixed time column
            Box(Modifier.width(TIME_COL_W.dp).verticalScroll(scrollV)
                .background(MaterialTheme.colorScheme.surfaceVariant)) {
                Box(Modifier.height((totalCalHeightDp + CAL_V_PAD * 2).dp)) {
                    for (h in CAL_START_HOUR..CAL_END_HOUR) {
                        val top = ((h - CAL_START_HOUR) * DP_PER_HOUR + CAL_V_PAD - 7f).dp
                        Text("%02d:00".format(h), style = MaterialTheme.typography.labelSmall,
                            color = FluentMuted, modifier = Modifier.offset(y = top).padding(start = 4.dp))
                    }
                }
            }
            // Event column
            Box(Modifier.weight(1f).verticalScroll(scrollV)
                .height((totalCalHeightDp + CAL_V_PAD * 2).dp).border(0.5.dp, FluentBorder)) {
                for (h in CAL_START_HOUR..CAL_END_HOUR) {
                    val top = ((h - CAL_START_HOUR) * DP_PER_HOUR + CAL_V_PAD).dp
                    Box(Modifier.offset(y = top).fillMaxWidth().height(0.5.dp).background(FluentBorder))
                }
                dayRecs.forEach { rec ->
                    val startStr = rec.resolvedStart()
                    val endStr   = rec.resolvedEnd()
                    if (startStr.isBlank()) return@forEach
                    val sub   = vm.subject(rec.subjectId)
                    val cl    = vm.schoolClass(rec.classId)
                    val te    = vm.teacher(rec.teacherId)
                    val color = packedToColor(sub?.color ?: SUBJECT_COLORS[0])
                    Box(Modifier
                        .offset(y = (minuteOffsetDp(startStr) + CAL_V_PAD).dp)
                        .fillMaxWidth().height(durationDp(startStr, endStr).coerceAtLeast(56f).dp)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color.copy(alpha = 0.15f))
                        .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable { onRecordClick(rec) }
                        .padding(8.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("${sub?.name ?: "?"} · ${cl?.name ?: "?"}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold, color = color, maxLines = 1)
                            // FIX: show start-end times in day event block
                            Text("$startStr – $endStr  👩‍🏫${te?.name ?: "─"}",
                                style = MaterialTheme.typography.labelSmall, color = FluentMuted, maxLines = 1)
                            StatusBadge(rec.status)
                        }
                    }
                }
            }
        }
    }
}

// ── Detail dialog ─────────────────────────────────────────────────────────────

@Composable
private fun AttendanceDetailDialog(
    rec: Attendance, state: AppState, vm: AppViewModel,
    onDismiss: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit
) {
    val sub  = vm.subject(rec.subjectId)
    val te   = vm.teacher(rec.teacherId)
    val cl   = vm.schoolClass(rec.classId)
    val ats  = rec.attendees.mapNotNull { vm.student(it) }
    val lessonCount = state.attendance.count {
        it.subjectId == rec.subjectId && it.classId == rec.classId && it.status == "completed"
    }

    FluentDialog(title = "上课记录详情", onDismiss = onDismiss) {
        if (rec.code.isNotBlank()) DetailRow("编号", rec.code)
        DetailRow("班级", cl?.name ?: "─")
        val classSubject = cl?.subject?.takeIf { it.isNotBlank() }
        DetailRow("科目", sub?.name ?: classSubject ?: "─")
        DetailRow("教师", te?.name ?: "─")
        DetailRow("日期", rec.date)
        val s = rec.resolvedStart(); val e = rec.resolvedEnd()
        DetailRow("时间", if (s.isBlank()) "─" else "$s – $e")
        DetailRow("该科已上课次", "$lessonCount 次")
        if (rec.topic.isNotBlank()) DetailRow("课题", rec.topic)
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("状态", style = MaterialTheme.typography.bodyMedium, color = FluentMuted)
            StatusBadge(rec.status)
        }
        if (ats.isNotEmpty()) {
            SectionHeader("出勤学生 (${ats.size}人)")
            androidx.compose.foundation.layout.FlowRow(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ats.forEach { s -> ColorChip(s.name, FluentGreen) }
            }
        }
        if (rec.notes.isNotBlank()) DetailRow("备注", rec.notes)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onEdit, shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)) { Text("✏️ 编辑") }
            OutlinedButton(onClick = onDelete, shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = FluentRed),
                modifier = Modifier.weight(1f)) { Text("删除") }
        }
    }
}

// ── Form dialog ───────────────────────────────────────────────────────────────

@Composable
internal fun AttendanceFormDialog(
    title: String, initial: Attendance?,
    state: AppState, vm: AppViewModel,
    onDismiss: () -> Unit, onSave: (Attendance) -> Unit
) {
    var className   by remember { mutableStateOf(state.classes.firstOrNull { it.id == initial?.classId }?.name ?: "") }
    var teacherName by remember { mutableStateOf(state.teachers.firstOrNull { it.id == initial?.teacherId }?.name ?: "") }
    var date        by remember { mutableStateOf(initial?.date ?: LocalDate.now().toString()) }
    var startTime   by remember { mutableStateOf(initial?.resolvedStart() ?: "08:00") }
    var endTime     by remember { mutableStateOf(initial?.resolvedEnd()   ?: "08:45") }
    var topic       by remember { mutableStateOf(initial?.topic  ?: "") }
    var status      by remember { mutableStateOf(initial?.status ?: "completed") }
    var notes       by remember { mutableStateOf(initial?.notes  ?: "") }
    var attendees   by remember { mutableStateOf<List<Long>>(initial?.attendees ?: emptyList()) }
    var code        by remember { mutableStateOf(initial?.code?.ifBlank { null } ?: genCode("ATT")) }

    val selectedClass  = state.classes.firstOrNull { it.name == className }
    val classStudents  = state.students.filter { s -> s.classIds.contains(selectedClass?.id) }

    FluentDialog(title = title, onDismiss = onDismiss, onConfirm = {
        val cls  = state.classes.firstOrNull { it.name == className } ?: return@FluentDialog
        val sId  = cls.subjectId
                   ?: state.subjects.firstOrNull { it.name == cls.subject }?.id
                   ?: initial?.subjectId ?: state.subjects.firstOrNull()?.id ?: 1L
        val tId  = state.teachers.firstOrNull { it.name == teacherName }?.id
        val newId = if (initial != null && initial.id != 0L) initial.id else System.currentTimeMillis()
        onSave(Attendance(newId, cls.id, sId, tId, date, 0, startTime, endTime,
                          topic, status, notes, attendees, code.trim().ifBlank { genCode("ATT") }))
    }) {
        // ── 行1：编号½ + 状态½ ────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(Modifier.weight(1f)) {
                FormTextField("编号", code, { code = it }, "自动生成")
            }
            Box(Modifier.weight(1f)) {
                FormDropdown("状态", status, listOf("completed", "cancelled", "pending")) { status = it }
            }
        }

        // ── 行2：班级½ + 教师½ ────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(Modifier.weight(1f)) {
                FormDropdown("班级", className, state.classes.map { it.name }) {
                    className = it; attendees = emptyList()
                }
            }
            Box(Modifier.weight(1f)) {
                FormDropdown("教师", teacherName, listOf("") + state.teachers.map { it.name }) { teacherName = it }
            }
        }

        // ── 科目 badge ────────────────────────────────────────────────
        val resolvedSubjectName = selectedClass?.let { cls ->
            state.subjects.find { it.id == cls.subjectId }?.name ?: cls.subject.ifBlank { null }
        }
        if (resolvedSubjectName?.isNotBlank() == true) {
            Surface(shape = RoundedCornerShape(8.dp), color = FluentPurple.copy(alpha = 0.1f)) {
                Text(
                    "科目：$resolvedSubjectName",
                    style      = MaterialTheme.typography.bodySmall,
                    color      = FluentPurple,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        // ── 行3：日期½ + 开始时间½ ────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(Modifier.weight(1f)) {
                DatePickerField("日期", date) { date = it }
            }
            Box(Modifier.weight(1f)) {
                StartTimeCompact(startTime) { newStart ->
                    val dur = minutesBetween(startTime, endTime).coerceAtLeast(30)
                    startTime = newStart
                    endTime   = addMinutesToTime(newStart, dur)
                }
            }
        }

        // ── 时长（独立行）────────────────────────────────────────────
        DurationChipsCompact(startTime = startTime, endTime = endTime) { endTime = it }

        // ── 课题（独立行）────────────────────────────────────────────
        FormTextField("课题", topic, { topic = it }, "本节课主题")

        // ── 出勤学生 ─────────────────────────────────────────────────
        if (classStudents.isNotEmpty()) {
            SectionHeader("出勤学生")
            androidx.compose.foundation.layout.FlowRow(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                classStudents.forEach { s ->
                    val on = attendees.contains(s.id)
                    FilterChip(selected = on, onClick = {
                        attendees = if (on) attendees.filter { it != s.id } else attendees + s.id
                    }, label = { Text(s.name) })
                }
            }
        }

        // ── 备注 ─────────────────────────────────────────────────────
        FormTextField("备注", notes, { notes = it }, "可选")
    }
}
