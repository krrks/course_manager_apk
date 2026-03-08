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
import com.school.manager.ui.components.*
import com.school.manager.ui.theme.*
import com.school.manager.viewmodel.AppViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields

// ── Time-axis layout constants ────────────────────────────────────────────────
// 30 minutes = 40 dp  →  1 hour = 80 dp  →  total 14 h = 1120 dp
private const val DP_PER_HOUR   = 80f
private const val DP_PER_MIN    = DP_PER_HOUR / 60f   // ≈ 1.333
private const val TIME_COL_W    = 52     // dp – left time label column
private const val DAY_COL_W     = 120    // dp – each day column (week view)

/** dp offset from top of calendar (CAL_START_HOUR) for a given "HH:mm" */
private fun minuteOffsetDp(hhmm: String): Float {
    if (hhmm.isBlank()) return 0f
    val mins = timeToMinutes(hhmm) - CAL_START_HOUR * 60
    return (mins * DP_PER_MIN).coerceAtLeast(0f)
}

private fun durationDp(startHhmm: String, endHhmm: String): Float {
    if (startHhmm.isBlank() || endHhmm.isBlank()) return DP_PER_HOUR / 2f  // 30 min default
    val mins = (timeToMinutes(endHhmm) - timeToMinutes(startHhmm)).coerceAtLeast(10)
    return mins * DP_PER_MIN
}

/** Total calendar height in dp */
private const val CAL_V_PAD = 10f  // top+bottom padding so 08:00/22:00 labels are not clipped
private val totalCalHeightDp: Float
    get() = (CAL_END_HOUR - CAL_START_HOUR) * DP_PER_HOUR

// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AttendanceScreen(vm: AppViewModel) {
    val state   by vm.state.collectAsState()
    var view    by remember { mutableStateOf("list") }
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
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon = { Icon(Icons.Default.Add, "") },
                text = { Text("添加记录") },
                containerColor = FluentBlue, contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }
    ) { inner ->
        Column(modifier = Modifier.padding(inner).fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
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
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DropdownFilterChip("全部教师", state.teachers.map { it.id to it.name }, fTeacher) { fTeacher = it }
                DropdownFilterChip("全部学生", state.students.map { it.id to it.name }, fStudent) { fStudent = it }
            }

            when (view) {
                "list"  -> ListAttendanceView(records, vm) { viewing = it }
                "week"  -> WeekView(records, calDate, { calDate = it }, vm) { viewing = it }
                "month" -> MonthCalendarView(records, calDate, { calDate = it }, vm) { viewing = it }
                "day"   -> DayView(records, calDate, { calDate = it }, vm) { viewing = it }
            }
        }
    }

    viewing?.let { rec ->
        AttendanceDetailDialog(rec, state, vm,
            onDismiss = { viewing = null },
            onEdit    = { editing = rec; viewing = null },
            onDelete  = { vm.deleteAttendance(rec.id); viewing = null }
        )
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
private fun ListAttendanceView(records: List<Attendance>, vm: AppViewModel, onClick: (Attendance) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (records.isEmpty()) item { EmptyState("📋", "暂无上课记录") }
        else items(records) { rec -> AttendanceCard(rec, vm) { onClick(rec) } }
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
    val weekStart = current.with(DayOfWeek.MONDAY)
    val weekDays  = (0..6).map { weekStart.plusDays(it.toLong()) }
    val today     = LocalDate.now()
    val fmt       = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val byDate    = records.groupBy { it.date }
    val weekNum   = current.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
    val dayNames  = listOf("周一","周二","周三","周四","周五","周六","周日")

    Column(modifier = Modifier.fillMaxSize()) {
        // Navigation header
        Row(
            modifier = Modifier.fillMaxWidth().background(FluentBlue)
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { onDateChange(current.minusWeeks(1)) }) {
                Icon(Icons.Default.ChevronLeft, null, tint = Color.White) }
            Text("${current.year}年 第${weekNum}周",
                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            IconButton(onClick = { onDateChange(current.plusWeeks(1)) }) {
                Icon(Icons.Default.ChevronRight, null, tint = Color.White) }
        }
        // Day column headers
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(TIME_COL_W.dp))
            weekDays.forEach { date ->
                val isToday = date == today
                Box(
                    modifier = Modifier.weight(1f)
                        .background(if (isToday) FluentBlueLight else MaterialTheme.colorScheme.surfaceVariant)
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(dayNames[date.dayOfWeek.value - 1],
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isToday) FluentBlue else FluentMuted)
                        Text("${date.dayOfMonth}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (isToday) FluentBlue else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
        // Time grid
        val scrollV = rememberScrollState()
        val calH    = totalCalHeightDp
        Box(modifier = Modifier.fillMaxSize().verticalScroll(scrollV)) {
            Row(modifier = Modifier.height((calH + CAL_V_PAD * 2).dp)) {
                // Time column
                Box(modifier = Modifier.width(TIME_COL_W.dp).fillMaxHeight()) {
                    // Hour labels
                    for (h in CAL_START_HOUR..CAL_END_HOUR) {
                        val top = ((h - CAL_START_HOUR) * DP_PER_HOUR).dp
                        Text("%02d:00".format(h), style = MaterialTheme.typography.labelSmall,
                            color = FluentMuted,
                            modifier = Modifier.offset(y = top + CAL_V_PAD.dp - 7.dp).padding(start = 4.dp))
                    }
                    // Half-hour labels
                    for (h in CAL_START_HOUR until CAL_END_HOUR) {
                        val top = ((h - CAL_START_HOUR) * DP_PER_HOUR + DP_PER_HOUR / 2f).dp
                        Text(":%02d".format(30), style = MaterialTheme.typography.labelSmall,
                            color = FluentBorder,
                            modifier = Modifier.offset(y = top + CAL_V_PAD.dp - 5.dp).padding(start = 8.dp))
                    }
                }
                // Day columns
                weekDays.forEach { date ->
                    val dateStr = fmt.format(date)
                    val dayRecs = byDate[dateStr] ?: emptyList()
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().border(0.5.dp, FluentBorder)) {
                        // Hour lines
                        for (h in CAL_START_HOUR..CAL_END_HOUR) {
                            val top = ((h - CAL_START_HOUR) * DP_PER_HOUR).dp
                            Box(modifier = Modifier.offset(y = top + CAL_V_PAD.dp).fillMaxWidth()
                                .height(0.5.dp).background(FluentBorder))
                        }
                        // Half-hour lines
                        for (h in CAL_START_HOUR until CAL_END_HOUR) {
                            val top = ((h - CAL_START_HOUR) * DP_PER_HOUR + DP_PER_HOUR / 2f).dp
                            Box(modifier = Modifier.offset(y = top + CAL_V_PAD.dp).fillMaxWidth()
                                .height(0.5.dp).background(FluentBorder.copy(alpha = 0.4f)))
                        }
                        // Events
                        dayRecs.forEach { rec ->
                            val startStr = rec.resolvedStart()
                            val endStr   = rec.resolvedEnd()
                            if (startStr.isBlank()) return@forEach
                            val top  = minuteOffsetDp(startStr).dp
                            val h    = durationDp(startStr, endStr).dp
                            val sub  = vm.subject(rec.subjectId)
                            val color = packedToColor(sub?.color ?: SUBJECT_COLORS[0])
                            Box(
                                modifier = Modifier
                                    .offset(y = top + CAL_V_PAD.dp)
                                    .fillMaxWidth().height(h)
                                    .padding(horizontal = 2.dp, vertical = 1.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(color.copy(alpha = 0.2f))
                                    .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                    .clickable { onRecordClick(rec) }
                                    .padding(3.dp)
                            ) {
                                Column {
                                    Text(sub?.name ?: "?",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold, color = color, maxLines = 1)
                                    Text(startStr, style = MaterialTheme.typography.labelSmall,
                                        color = FluentMuted, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
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
    val fmt    = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val dayStr = fmt.format(current)
    val dayRecs = records.filter { it.date == dayStr }
    val dayNames = listOf("周一","周二","周三","周四","周五","周六","周日")
    val dow = dayNames[current.dayOfWeek.value - 1]
    val calH = totalCalHeightDp

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().background(FluentBlue)
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { onDateChange(current.minusDays(1)) }) {
                Icon(Icons.Default.ChevronLeft, null, tint = Color.White) }
            Text("${current.year}年${current.monthValue}月${current.dayOfMonth}日  $dow",
                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            IconButton(onClick = { onDateChange(current.plusDays(1)) }) {
                Icon(Icons.Default.ChevronRight, null, tint = Color.White) }
        }

        val scrollV = rememberScrollState()
        Box(modifier = Modifier.fillMaxSize().verticalScroll(scrollV)) {
            Row(modifier = Modifier.height((calH + CAL_V_PAD * 2).dp).fillMaxWidth()) {
                // Time column
                Box(modifier = Modifier.width(TIME_COL_W.dp).fillMaxHeight()) {
                    for (h in CAL_START_HOUR..CAL_END_HOUR) {
                        val top = ((h - CAL_START_HOUR) * DP_PER_HOUR).dp
                        Text("%02d:00".format(h), style = MaterialTheme.typography.labelSmall,
                            color = FluentMuted,
                            modifier = Modifier.offset(y = top + CAL_V_PAD.dp - 7.dp).padding(start = 4.dp))
                    }
                    for (h in CAL_START_HOUR until CAL_END_HOUR) {
                        val top = ((h - CAL_START_HOUR) * DP_PER_HOUR + DP_PER_HOUR / 2f).dp
                        Text(":30", style = MaterialTheme.typography.labelSmall,
                            color = FluentBorder,
                            modifier = Modifier.offset(y = top + CAL_V_PAD.dp - 5.dp).padding(start = 8.dp))
                    }
                }
                // Event column
                Box(modifier = Modifier.weight(1f).fillMaxHeight().border(0.5.dp, FluentBorder)) {
                    // Hour lines
                    for (h in CAL_START_HOUR..CAL_END_HOUR) {
                        val top = ((h - CAL_START_HOUR) * DP_PER_HOUR).dp
                        Box(modifier = Modifier.offset(y = top + CAL_V_PAD.dp).fillMaxWidth()
                            .height(0.5.dp).background(FluentBorder))
                    }
                    // Half-hour lines
                    for (h in CAL_START_HOUR until CAL_END_HOUR) {
                        val top = ((h - CAL_START_HOUR) * DP_PER_HOUR + DP_PER_HOUR / 2f).dp
                        Box(modifier = Modifier.offset(y = top + CAL_V_PAD.dp).fillMaxWidth()
                            .height(0.5.dp).background(FluentBorder.copy(alpha = 0.4f)))
                    }
                    if (dayRecs.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("当天暂无上课记录",
                                style = MaterialTheme.typography.bodyMedium, color = FluentMuted)
                        }
                    }
                    dayRecs.forEach { rec ->
                        val startStr = rec.resolvedStart()
                        val endStr   = rec.resolvedEnd()
                        if (startStr.isBlank()) return@forEach
                        val top     = minuteOffsetDp(startStr).dp
                        val h       = durationDp(startStr, endStr).dp
                        val sub     = vm.subject(rec.subjectId)
                        val cl      = vm.schoolClass(rec.classId)
                        val te      = vm.teacher(rec.teacherId)
                        val color   = packedToColor(sub?.color ?: SUBJECT_COLORS[0])
                        Box(
                            modifier = Modifier
                                .offset(y = top + CAL_V_PAD.dp)
                                .fillMaxWidth(0.96f).height(h)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(color.copy(alpha = 0.15f))
                                .border(1.5.dp, color.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                .clickable { onRecordClick(rec) }
                                .padding(6.dp)
                        ) {
                            Column {
                                Text("${sub?.name ?: "?"} · ${cl?.name ?: "?"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold, color = color, maxLines = 1)
                                Text("$startStr–$endStr  👩‍🏫${te?.name ?: "─"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = FluentMuted, maxLines = 1)
                                StatusBadge(rec.status)
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
private fun MonthCalendarView(
    records: List<Attendance>,
    current: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    vm: AppViewModel,
    onRecordClick: (Attendance) -> Unit
) {
    val ym = YearMonth.of(current.year, current.month)
    val firstDow = ym.atDay(1).dayOfWeek.value
    val daysInMonth = ym.lengthOfMonth()
    val today  = LocalDate.now()
    val fmt    = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val byDate = records.groupBy { it.date }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth().background(FluentBlue).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { onDateChange(current.minusMonths(1)) }) {
                Icon(Icons.Default.ChevronLeft, null, tint = Color.White) }
            Text("${current.year}年 ${current.monthValue}月",
                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            IconButton(onClick = { onDateChange(current.plusMonths(1)) }) {
                Icon(Icons.Default.ChevronRight, null, tint = Color.White) }
        }
        Row(Modifier.fillMaxWidth()) {
            listOf("一","二","三","四","五","六","日").forEach { d ->
                Box(Modifier.weight(1f).padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                    Text(d, style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold, color = FluentMuted)
                }
            }
        }
        val cells  = (1 until firstDow).map { -it } + (1..daysInMonth).toList()
        val padded = cells + List((7 - cells.size % 7) % 7) { 0 }
        padded.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    val dateStr = if (day > 0) fmt.format(ym.atDay(day)) else ""
                    val dayRecs = if (day > 0) byDate[dateStr] ?: emptyList() else emptyList()
                    val isToday = day > 0 && ym.atDay(day) == today
                    Box(
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 60.dp)
                            .border(0.5.dp, FluentBorder)
                            .background(if (isToday) FluentBlueLight else Color.Transparent)
                            .clickable(enabled = day > 0) { onDateChange(ym.atDay(day)) }
                            .padding(3.dp),
                        contentAlignment = Alignment.TopStart
                    ) {
                        if (day > 0) {
                            Column {
                                Box(
                                    modifier = Modifier.size(22.dp)
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(if (isToday) FluentBlue else Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("$day", style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isToday) Color.White else MaterialTheme.colorScheme.onSurface)
                                }
                                dayRecs.take(2).forEach { rec ->
                                    val sub   = vm.subject(rec.subjectId)
                                    val color = packedToColor(sub?.color ?: SUBJECT_COLORS[0])
                                    Box(
                                        modifier = Modifier.fillMaxWidth()
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(color.copy(alpha = 0.2f))
                                            .clickable { onRecordClick(rec) }
                                            .padding(horizontal = 3.dp, vertical = 1.dp)
                                    ) {
                                        Text(sub?.name ?: "?",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = color, maxLines = 1)
                                    }
                                    Spacer(Modifier.height(1.dp))
                                }
                                if (dayRecs.size > 2)
                                    Text("+${dayRecs.size - 2}",
                                        style = MaterialTheme.typography.labelSmall, color = FluentMuted)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Attendance Card ────────────────────────────────────────────────────────────

@Composable
internal fun AttendanceCard(rec: Attendance, vm: AppViewModel, onClick: () -> Unit) {
    val sub   = vm.subject(rec.subjectId)
    val te    = vm.teacher(rec.teacherId)
    val cl    = vm.schoolClass(rec.classId)
    val color = packedToColor(sub?.color ?: SUBJECT_COLORS[0])
    val startStr = rec.resolvedStart()
    FluentCard(modifier = Modifier.fillMaxWidth(), accentColor = color, onClick = onClick) {
        Row(modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(sub?.name ?: "?", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, color = color)
                    Text("· ${cl?.name ?: "?"}",
                        style = MaterialTheme.typography.bodyMedium, color = FluentMuted)
                    StatusBadge(rec.status)
                }
                if (rec.topic.isNotBlank())
                    Text("📌 ${rec.topic}", style = MaterialTheme.typography.bodyMedium, color = FluentMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("👩‍🏫 ${te?.name ?: "─"}",
                        style = MaterialTheme.typography.labelMedium, color = FluentMuted)
                    Text("🗓 ${rec.date}",
                        style = MaterialTheme.typography.labelMedium, color = FluentMuted)
                    if (startStr.isNotBlank())
                        Text("⏰ $startStr",
                            style = MaterialTheme.typography.labelMedium, color = FluentMuted)
                    Text("👥 ${rec.attendees.size}人",
                        style = MaterialTheme.typography.labelMedium, color = FluentMuted)
                }
            }
        }
    }
}

// ── Detail dialog ─────────────────────────────────────────────────────────────

@Composable
private fun AttendanceDetailDialog(
    rec: Attendance,
    state: AppState,   // ← passed in, not vm.state.value
    vm: AppViewModel,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val sub  = vm.subject(rec.subjectId)
    val te   = vm.teacher(rec.teacherId)
    val cl   = vm.schoolClass(rec.classId)
    val ats  = rec.attendees.mapNotNull { vm.student(it) }
    // Lesson count from already-collected state (no vm.state.value in composable)
    val lessonCount = state.attendance.count {
        it.subjectId == rec.subjectId && it.classId == rec.classId && it.status == "completed"
    }

    FluentDialog(title = "上课记录详情", onDismiss = onDismiss) {
        DetailRow("班级", cl?.name ?: "─")
        DetailRow("科目", sub?.name ?: "─")
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
            FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
    title: String,
    initial: Attendance?,
    state: AppState,
    vm: AppViewModel,
    onDismiss: () -> Unit,
    onSave: (Attendance) -> Unit
) {
    var className  by remember { mutableStateOf(state.classes.firstOrNull  { it.id == initial?.classId }?.name ?: "") }
    var subjectName by remember { mutableStateOf(state.subjects.firstOrNull { it.id == initial?.subjectId }?.name ?: "") }
    var teacherName by remember { mutableStateOf(state.teachers.firstOrNull { it.id == initial?.teacherId }?.name ?: "") }
    var date       by remember { mutableStateOf(initial?.date ?: LocalDate.now().toString()) }
    var startTime  by remember { mutableStateOf(initial?.resolvedStart() ?: "08:00") }
    var endTime    by remember { mutableStateOf(initial?.resolvedEnd()   ?: "08:45") }
    var topic      by remember { mutableStateOf(initial?.topic ?: "") }
    var status     by remember { mutableStateOf(initial?.status ?: "completed") }
    var notes      by remember { mutableStateOf(initial?.notes ?: "") }
    var attendees  by remember { mutableStateOf<List<Long>>(initial?.attendees ?: emptyList()) }

    val classStudents = state.students.filter { s ->
        s.classIds.contains(state.classes.firstOrNull { it.name == className }?.id)
    }

    FluentDialog(title = title, onDismiss = onDismiss, onConfirm = {
        val cId   = state.classes.firstOrNull  { it.name == className }?.id  ?: return@FluentDialog
        val sId   = state.subjects.firstOrNull { it.name == subjectName }?.id ?: return@FluentDialog
        val tId   = state.teachers.firstOrNull { it.name == teacherName }?.id
        val newId = if (initial != null && initial.id != 0L) initial.id else System.currentTimeMillis()
        onSave(Attendance(newId, cId, sId, tId, date, 0, startTime, endTime, topic, status, notes, attendees))
    }) {
        FormDropdown("班级", className, state.classes.map { it.name }) { className = it; attendees = emptyList() }
        FormDropdown("科目", subjectName, state.subjects.map { it.name }) { subjectName = it }
        FormDropdown("教师", teacherName, state.teachers.map { it.name }) { teacherName = it }
        FormTextField("日期 (yyyy-MM-dd)", date, { date = it }, "2025-01-01")
        // Free-form time with quick presets
        TimeRangeRow(startTime, endTime,
            onStartChange = { startTime = it },
            onEndChange   = { endTime   = it })
        FormDropdown("状态", status, listOf("completed","cancelled","pending")) { status = it }
        FormTextField("课题", topic, { topic = it }, "本节课主题")
        if (classStudents.isNotEmpty()) {
            SectionHeader("出勤学生")
            FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                classStudents.forEach { s ->
                    val on = attendees.contains(s.id)
                    FilterChip(selected = on, onClick = {
                        attendees = if (on) attendees.filter { it != s.id } else attendees + s.id
                    }, label = { Text(s.name) })
                }
            }
        }
        FormTextField("备注", notes, { notes = it }, "可选")
    }
}

// ── Filter chip helper ────────────────────────────────────────────────────────

@Composable
private fun DropdownFilterChip(
    allLabel: String,
    items: List<Pair<Long, String>>,
    selected: Long,
    onSelect: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val label = items.firstOrNull { it.first == selected }?.second ?: allLabel
    Box {
        FilterChip(selected = selected != 0L, onClick = { expanded = true }, label = { Text(label) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, Modifier.size(16.dp)) })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(allLabel) }, onClick = { onSelect(0L); expanded = false })
            items.forEach { (id, name) ->
                DropdownMenuItem(text = { Text(name) }, onClick = { onSelect(id); expanded = false })
            }
        }
    }
}
