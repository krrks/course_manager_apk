package com.school.manager.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
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

// ── Calendar layout constants ─────────────────────────────────────────────────
private const val DP_PER_HOUR = 80f
private const val DP_PER_MIN  = DP_PER_HOUR / 60f
private const val TIME_COL_W  = 52
private const val DAY_COL_W   = 120
private const val CAL_V_PAD   = 10f
private val CAL_TOTAL_HEIGHT  get() = (CAL_END_HOUR - CAL_START_HOUR) * DP_PER_HOUR

private fun minuteOffsetDp(hhmm: String): Float {
    if (hhmm.isBlank()) return 0f
    return ((timeToMinutes(hhmm) - CAL_START_HOUR * 60) * DP_PER_MIN).coerceAtLeast(0f)
}
private fun durationDp(start: String, end: String): Float {
    if (start.isBlank() || end.isBlank()) return DP_PER_HOUR / 2f
    return (timeToMinutes(end) - timeToMinutes(start)).coerceAtLeast(10) * DP_PER_MIN
}
private fun addMinutesToTime(hhmm: String, minutes: Int): String {
    val base  = if (hhmm.isBlank()) 8 * 60 else timeToMinutes(hhmm)
    val total = (base + minutes).coerceIn(0, 23 * 60 + 59)
    return "%02d:%02d".format(total / 60, total % 60)
}
private fun minutesBetween(start: String, end: String): Int =
    if (start.isBlank() || end.isBlank()) 45
    else (timeToMinutes(end) - timeToMinutes(start)).coerceAtLeast(0)

// ── Status helpers ────────────────────────────────────────────────────────────
private fun statusColor(status: String): Color = when (status) {
    "completed" -> FluentGreen
    "absent"    -> FluentAmber
    "cancelled" -> FluentRed
    "postponed" -> FluentMuted
    else        -> FluentBlue
}
private fun statusLabel(status: String): String = when (status) {
    "completed" -> "✅ 已完成"
    "absent"    -> "⚠️ 缺席"
    "cancelled" -> "❌ 已取消"
    "postponed" -> "⏸ 已延期"
    else        -> "⏳ 待上课"
}

@Composable
private fun StatusChip(status: String) = ColorChip(statusLabel(status), statusColor(status))

// ── Main screen ───────────────────────────────────────────────────────────────

@Composable
fun LessonScreen(vm: AppViewModel, onOpenDrawer: () -> Unit = {}) {
    val state   by vm.state.collectAsState()
    var view    by remember { mutableStateOf("week") }
    var calDate by remember { mutableStateOf(LocalDate.now()) }

    // Filters
    var fClass   by remember { mutableLongStateOf(0L) }
    var fStatus  by remember { mutableStateOf("") }
    var fTeacher by remember { mutableLongStateOf(0L) }
    var fStudent by remember { mutableLongStateOf(0L) }

    // Dialog state
    var viewing      by remember { mutableStateOf<Lesson?>(null) }
    var editing      by remember { mutableStateOf<Lesson?>(null) }
    var showAdd      by remember { mutableStateOf(false) }
    var showBatchGen by remember { mutableStateOf(false) }
    var batchModCls  by remember { mutableLongStateOf(0L) }
    var batchDelCls  by remember { mutableLongStateOf(0L) }

    // Pre-computed progress map: classId → (completed, total)
    val progressMap: Map<Long, Pair<Int, Int>> = remember(state.lessons) {
        state.lessons.groupBy { it.classId }.mapValues { (_, ls) ->
            ls.count { it.status == "completed" } to ls.size
        }
    }

    val filtered = state.lessons
        .filter { l ->
            (fClass == 0L   || l.classId == fClass) &&
            (fStatus.isBlank() || l.status == fStatus) &&
            (fTeacher == 0L || l.effectiveTeacherId(state.classes) == fTeacher) &&
            (fStudent == 0L || run {
                val s = state.students.find { it.id == fStudent }
                s != null && s.classIds.contains(l.classId)
            })
        }
        .sortedWith(compareBy({ it.date }, { it.startTime }))

    Scaffold(
        floatingActionButton = {
            ScreenSpeedDialFab(
                addLabel     = "添加单节课",
                addIcon      = Icons.Default.Add,
                onAdd        = { showAdd = true },
                onOpenDrawer = onOpenDrawer,
                extraItems   = {
                    SpeedDialItem("批量生成课次", Icons.Default.AutoAwesome, FluentGreen) {
                        showBatchGen = true
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(top = inner.calculateTopPadding(), bottom = inner.calculateBottomPadding())
        ) {
            // View tab row
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = view == "week",  onClick = { view = "week" },  label = { Text("📅 周视图") })
                FilterChip(selected = view == "month", onClick = { view = "month" }, label = { Text("🗓 月视图") })
                FilterChip(selected = view == "day",   onClick = { view = "day" },   label = { Text("☀️ 日视图") })
                FilterChip(selected = view == "list",  onClick = { view = "list" },  label = { Text("📋 列表") })
            }

            // Filter chips row — class, status, teacher, student
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Class filter
                DropdownFilterChip(
                    allLabel = "全部班级",
                    items    = state.classes.map { it.id to it.name },
                    selected = fClass
                ) { fClass = it }

                // Status filter
                val statusOpts = listOf(
                    "" to "全部状态", "pending" to "待上课", "completed" to "已完成",
                    "absent" to "缺席", "cancelled" to "已取消", "postponed" to "已延期"
                )
                DropdownFilterChip(
                    allLabel = "全部状态",
                    items    = statusOpts.drop(1).mapIndexed { i, (_, label) -> (i + 1).toLong() to label },
                    selected = statusOpts.indexOfFirst { it.first == fStatus }.let { if (it <= 0) 0L else it.toLong() }
                ) { idx -> fStatus = if (idx == 0L) "" else statusOpts.getOrNull(idx.toInt())?.first ?: "" }

                // Teacher filter
                DropdownFilterChip(
                    allLabel = "全部教师",
                    items    = state.teachers.map { it.id to it.name },
                    selected = fTeacher
                ) { fTeacher = it }

                // Student filter
                DropdownFilterChip(
                    allLabel = "全部学生",
                    items    = state.students.map { it.id to it.name },
                    selected = fStudent
                ) { fStudent = it }
            }

            when (view) {
                "week"  -> WeekView(filtered,  calDate, { calDate = it }, state, progressMap) { viewing = it }
                "month" -> MonthView(filtered, calDate, { calDate = it }, state, progressMap) { viewing = it }
                "day"   -> DayView(filtered,   calDate, { calDate = it }, state, progressMap) { viewing = it }
                "list"  -> ListView(filtered, state, progressMap,
                    onLessonClick = { viewing = it },
                    onBatchModify = { batchModCls = it },
                    onBatchDelete = { batchDelCls = it }
                )
            }
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────
    viewing?.let { l ->
        LessonDetailDialog(l, state, vm, progressMap,
            onDismiss = { viewing = null },
            onEdit    = { editing = l; viewing = null },
            onDelete  = { vm.deleteLesson(l.id); viewing = null }
        )
    }
    editing?.let { l ->
        LessonFormDialog("编辑课次", l, state, vm,
            onDismiss = { editing = null },
            onSave    = { updated -> vm.updateLesson(updated, markModified = true); editing = null }
        )
    }
    if (showAdd) {
        LessonFormDialog("添加课次", null, state, vm,
            onDismiss = { showAdd = false },
            onSave    = { l ->
                vm.addLesson(l.classId, l.date, l.startTime, l.endTime,
                    l.status, l.topic, l.notes, l.attendees,
                    teacherIdOverride = l.teacherIdOverride)
                showAdd = false
            }
        )
    }
    if (showBatchGen) {
        BatchGenerateDialog(state, vm, onDismiss = { showBatchGen = false })
    }
    if (batchModCls > 0L) {
        BatchModifyDialog(batchModCls, state, vm, onDismiss = { batchModCls = 0L })
    }
    if (batchDelCls > 0L) {
        BatchDeleteDialog(batchDelCls, state, vm, onDismiss = { batchDelCls = 0L })
    }
}

// ── Week view ─────────────────────────────────────────────────────────────────

@Composable
private fun WeekView(
    lessons: List<Lesson>, current: LocalDate, onDateChange: (LocalDate) -> Unit,
    state: AppState, progressMap: Map<Long, Pair<Int, Int>>, onClick: (Lesson) -> Unit
) {
    val monday = current.with(DayOfWeek.MONDAY)
    val days   = (0..6).map { monday.plusDays(it.toLong()) }
    val fmt    = DateTimeFormatter.ofPattern("M/d")

    Column(Modifier.fillMaxSize()) {
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

        val scrollV = rememberScrollState()
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.width(TIME_COL_W.dp).verticalScroll(scrollV)
                .background(MaterialTheme.colorScheme.surfaceVariant)) {
                Box(Modifier.height((CAL_TOTAL_HEIGHT + CAL_V_PAD * 2).dp)) {
                    for (h in CAL_START_HOUR..CAL_END_HOUR) {
                        Text("%02d:00".format(h), style = MaterialTheme.typography.labelSmall,
                            color = FluentMuted,
                            modifier = Modifier.offset(y = ((h - CAL_START_HOUR) * DP_PER_HOUR + CAL_V_PAD - 7f).dp)
                                .padding(start = 4.dp))
                    }
                }
            }
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                days.forEach { day ->
                    val dayLessons = lessons.filter { it.date == day.toString() }
                    val isToday    = day == LocalDate.now()
                    Column(Modifier.width(DAY_COL_W.dp)) {
                        Box(Modifier.fillMaxWidth()
                            .background(if (isToday) FluentBlue.copy(0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                            .padding(4.dp), contentAlignment = Alignment.Center) {
                            Text("${DAYS.getOrElse(day.dayOfWeek.value - 1){"?"}}  ${day.format(fmt)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (isToday) FluentBlue else FluentMuted)
                        }
                        Box(Modifier.width(DAY_COL_W.dp).verticalScroll(scrollV)
                            .height((CAL_TOTAL_HEIGHT + CAL_V_PAD * 2).dp)
                            .border(0.5.dp, FluentBorder)) {
                            for (h in CAL_START_HOUR..CAL_END_HOUR) {
                                Box(Modifier.offset(y = ((h - CAL_START_HOUR) * DP_PER_HOUR + CAL_V_PAD).dp)
                                    .fillMaxWidth().height(0.5.dp).background(FluentBorder))
                            }
                            dayLessons.forEach { l ->
                                if (l.startTime.isBlank()) return@forEach
                                val color   = statusColor(l.status)
                                val subName = l.subjectName(state.classes, state.subjects)
                                val (done, total) = progressMap[l.classId] ?: (0 to 0)
                                Box(Modifier
                                    .offset(y = (minuteOffsetDp(l.startTime) + CAL_V_PAD).dp)
                                    .fillMaxWidth()
                                    .height(durationDp(l.startTime, l.endTime).coerceAtLeast(42f).dp)
                                    .padding(horizontal = 2.dp, vertical = 1.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(color.copy(alpha = 0.15f))
                                    .border(1.dp, color.copy(0.5f), RoundedCornerShape(6.dp))
                                    .clickable { onClick(l) }
                                    .padding(4.dp)) {
                                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                        Text(subName, style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold, color = color, maxLines = 1)
                                        Text("${l.startTime}–${l.endTime}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = FluentMuted, maxLines = 1)
                                        if (total > 0)
                                            Text("$done/$total",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = color, fontWeight = FontWeight.SemiBold)
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
    lessons: List<Lesson>, current: LocalDate, onDateChange: (LocalDate) -> Unit,
    state: AppState, progressMap: Map<Long, Pair<Int, Int>>, onClick: (Lesson) -> Unit
) {
    val ym     = YearMonth.from(current)
    val first  = ym.atDay(1)
    val offset = first.dayOfWeek.value % 7
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
        Row(Modifier.fillMaxWidth()) {
            listOf("日","一","二","三","四","五","六").forEach { d ->
                Text(d, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall,
                    color = FluentMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            val rows = ((offset + days) + 6) / 7
            items(rows) { row ->
                Row(Modifier.fillMaxWidth()) {
                    for (col in 0..6) {
                        val idx = row * 7 + col
                        val day = if (idx < offset || idx >= offset + days) null
                        else first.plusDays((idx - offset).toLong())
                        Box(Modifier.weight(1f).height(68.dp)
                            .border(0.25.dp, FluentBorder)
                            .background(if (day == LocalDate.now()) FluentBlue.copy(0.08f) else Color.Transparent)
                            .clickable(enabled = day != null) { day?.let { onDateChange(it) } }
                            .padding(2.dp)) {
                            if (day != null) {
                                val dayLessons = lessons.filter { it.date == day.toString() }
                                Column {
                                    Text(day.dayOfMonth.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (day == LocalDate.now()) FontWeight.Bold else FontWeight.Normal,
                                        color = if (day == LocalDate.now()) FluentBlue else FluentMuted)
                                    dayLessons.take(3).forEach { l ->
                                        val color = statusColor(l.status)
                                        Text(l.subjectName(state.classes, state.subjects),
                                            style = MaterialTheme.typography.labelSmall, color = color,
                                            maxLines = 1,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(color.copy(0.12f))
                                                .padding(horizontal = 2.dp)
                                                .clickable { onClick(l) })
                                    }
                                    if (dayLessons.size > 3)
                                        Text("+${dayLessons.size - 3}",
                                            style = MaterialTheme.typography.labelSmall, color = FluentMuted)
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
    lessons: List<Lesson>, current: LocalDate, onDateChange: (LocalDate) -> Unit,
    state: AppState, progressMap: Map<Long, Pair<Int, Int>>, onClick: (Lesson) -> Unit
) {
    val dow        = DAYS.getOrElse(current.dayOfWeek.value - 1) { "" }
    val dayLessons = lessons.filter { it.date == current.toString() }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().background(FluentBlue).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = { onDateChange(current.minusDays(1)) }) {
                Icon(Icons.Default.ChevronLeft, null, tint = Color.White) }
            Text("${current.monthValue}月${current.dayOfMonth}日  周$dow",
                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            IconButton(onClick = { onDateChange(current.plusDays(1)) }) {
                Icon(Icons.Default.ChevronRight, null, tint = Color.White) }
        }
        val scrollV = rememberScrollState()
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.width(TIME_COL_W.dp).verticalScroll(scrollV)
                .background(MaterialTheme.colorScheme.surfaceVariant)) {
                Box(Modifier.height((CAL_TOTAL_HEIGHT + CAL_V_PAD * 2).dp)) {
                    for (h in CAL_START_HOUR..CAL_END_HOUR) {
                        Text("%02d:00".format(h), style = MaterialTheme.typography.labelSmall,
                            color = FluentMuted,
                            modifier = Modifier.offset(y = ((h - CAL_START_HOUR) * DP_PER_HOUR + CAL_V_PAD - 7f).dp)
                                .padding(start = 4.dp))
                    }
                }
            }
            Box(Modifier.weight(1f).verticalScroll(scrollV)
                .height((CAL_TOTAL_HEIGHT + CAL_V_PAD * 2).dp).border(0.5.dp, FluentBorder)) {
                for (h in CAL_START_HOUR..CAL_END_HOUR) {
                    Box(Modifier.offset(y = ((h - CAL_START_HOUR) * DP_PER_HOUR + CAL_V_PAD).dp)
                        .fillMaxWidth().height(0.5.dp).background(FluentBorder))
                }
                dayLessons.forEach { l ->
                    if (l.startTime.isBlank()) return@forEach
                    val color   = statusColor(l.status)
                    val cls     = state.classes.find { it.id == l.classId }
                    val teacher = state.teachers.find { it.id == l.effectiveTeacherId(state.classes) }
                    val (done, total) = progressMap[l.classId] ?: (0 to 0)
                    Box(Modifier
                        .offset(y = (minuteOffsetDp(l.startTime) + CAL_V_PAD).dp)
                        .fillMaxWidth()
                        .height(durationDp(l.startTime, l.endTime).coerceAtLeast(56f).dp)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color.copy(0.15f))
                        .border(1.dp, color.copy(0.5f), RoundedCornerShape(8.dp))
                        .clickable { onClick(l) }
                        .padding(8.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("${l.subjectName(state.classes, state.subjects)} · ${cls?.name ?: "?"}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold, color = color, maxLines = 1)
                            Text("${l.startTime} – ${l.endTime}  👩‍🏫${teacher?.name ?: "─"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = FluentMuted, maxLines = 1)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                StatusChip(l.status)
                                if (total > 0)
                                    Surface(shape = RoundedCornerShape(8.dp),
                                        color = color.copy(0.1f)) {
                                        Text("已上 $done/$total 节",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = color, fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                    }
                            }
                            if (l.topic.isNotBlank())
                                Text("📌 ${l.topic}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = FluentMuted, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

// ── List view ─────────────────────────────────────────────────────────────────

@Composable
private fun ListView(
    lessons: List<Lesson>, state: AppState,
    progressMap: Map<Long, Pair<Int, Int>>,
    onLessonClick: (Lesson) -> Unit,
    onBatchModify: (Long) -> Unit,
    onBatchDelete: (Long) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()) {

        if (lessons.isEmpty()) {
            item { EmptyState("📅", "暂无课次") }
        } else {
            val grouped = lessons.groupBy { it.classId }
                .toSortedMap(compareBy { state.classes.find { c -> c.id == it }?.name ?: "" })

            grouped.forEach { (classId, classLessons) ->
                val cls           = state.classes.find { it.id == classId }
                val (done, total) = progressMap[classId] ?: (0 to 0)
                val color         = cls?.resolvedSubject(state.subjects)?.let {
                    Color(SUBJECT_COLORS[(state.subjects.indexOf(it)).coerceAtLeast(0) % SUBJECT_COLORS.size])
                } ?: FluentBlue

                item(key = "header_$classId") {
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(cls?.name ?: "班级 $classId",
                                style = MaterialTheme.typography.titleSmall,
                                color = color, fontWeight = FontWeight.Bold)
                            if (total > 0)
                                Text("已完成 $done / $total 节",
                                    style = MaterialTheme.typography.labelSmall, color = FluentMuted)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = { onBatchModify(classId) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                                Text("批量修改", style = MaterialTheme.typography.labelSmall,
                                    color = FluentBlue)
                            }
                            TextButton(onClick = { onBatchDelete(classId) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                                Text("批量删除", style = MaterialTheme.typography.labelSmall,
                                    color = FluentRed)
                            }
                        }
                    }
                    if (total > 0)
                        FluentProgressBar(done.toFloat() / total, color,
                            Modifier.fillMaxWidth().padding(bottom = 4.dp))
                }

                items(classLessons.sortedBy { it.date }, key = { "lesson_${it.id}" }) { l ->
                    LessonCard(l, state, color, onLessonClick)
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun LessonCard(l: Lesson, state: AppState, accentColor: Color, onClick: (Lesson) -> Unit) {
    FluentCard(accentColor = accentColor, modifier = Modifier.fillMaxWidth(),
        onClick = { onClick(l) }) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(44.dp)) {
                val parts = l.date.split("-")
                Text(parts.getOrElse(2){"?"}, style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, color = accentColor)
                Text("${parts.getOrElse(1){"?"}}月",
                    style = MaterialTheme.typography.labelSmall, color = FluentMuted)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(l.subjectName(state.classes, state.subjects),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, color = accentColor)
                    StatusChip(l.status)
                    if (l.isModified)
                        Surface(shape = RoundedCornerShape(4.dp), color = FluentAmber.copy(0.15f)) {
                            Text("已改", style = MaterialTheme.typography.labelSmall,
                                color = FluentAmber,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                }
                if (l.topic.isNotBlank())
                    Text("📌 ${l.topic}", style = MaterialTheme.typography.bodyMedium,
                        color = FluentMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (l.startTime.isNotBlank())
                        Text("🕐 ${l.startTime}–${l.endTime}",
                            style = MaterialTheme.typography.bodySmall, color = FluentMuted)
                    val teacherName = l.teacherName(state.classes, state.teachers)
                    Text("👩‍🏫 $teacherName",
                        style = MaterialTheme.typography.bodySmall, color = FluentMuted)
                }
            }
        }
    }
}

// ── Detail dialog ─────────────────────────────────────────────────────────────

@Composable
private fun LessonDetailDialog(
    l: Lesson, state: AppState, vm: AppViewModel,
    progressMap: Map<Long, Pair<Int, Int>>,
    onDismiss: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit
) {
    val cls     = state.classes.find { it.id == l.classId }
    val sub     = cls?.resolvedSubject(state.subjects)
    val teacher = state.teachers.find { it.id == l.effectiveTeacherId(state.classes) }
    val ats     = l.attendees.mapNotNull { vm.student(it) }
    val (done, total) = progressMap[l.classId] ?: (0 to 0)

    FluentDialog(title = "课次详情", onDismiss = onDismiss) {
        if (l.code.isNotBlank()) DetailRow("编号", l.code)
        DetailRow("班级", cls?.name ?: "─")
        DetailRow("科目", sub?.name ?: "─")
        DetailRow("教师", teacher?.name ?: "─")
        if (l.teacherIdOverride != null) {
            // Show a badge indicating teacher was overridden for this lesson
            Surface(shape = RoundedCornerShape(6.dp), color = FluentAmber.copy(0.12f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
                Text("本节课教师已覆盖班级默认设置",
                    style = MaterialTheme.typography.labelSmall, color = FluentAmber,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
        DetailRow("日期", l.date)
        if (l.startTime.isNotBlank()) DetailRow("时间", "${l.startTime} – ${l.endTime}")
        if (total > 0) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("上课进度", style = MaterialTheme.typography.bodyMedium, color = FluentMuted)
                Text("已完成 $done / $total 节",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor("completed"))
            }
            FluentProgressBar(if (total > 0) done.toFloat() / total else 0f,
                statusColor("completed"),
                Modifier.fillMaxWidth().padding(horizontal = 16.dp))
            HorizontalDivider(color = FluentBorder, thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 16.dp))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("状态", style = MaterialTheme.typography.bodyMedium, color = FluentMuted)
            StatusChip(l.status)
        }
        if (l.topic.isNotBlank()) DetailRow("课题", l.topic)
        if (l.notes.isNotBlank()) DetailRow("备注", l.notes)
        if (l.isModified) DetailRow("标记", "已单独修改")
        if (ats.isNotEmpty()) {
            SectionHeader("出勤学生 (${ats.size}人)")
            androidx.compose.foundation.layout.FlowRow(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ats.forEach { s -> ColorChip(s.name, FluentGreen) }
            }
        }
        Spacer(Modifier.height(8.dp))
        SectionHeader("快速更改状态")
        androidx.compose.foundation.layout.FlowRow(
            Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement   = Arrangement.spacedBy(6.dp)) {
            listOf("pending","completed","absent","cancelled","postponed").forEach { s ->
                FilterChip(
                    selected = l.status == s,
                    onClick  = { vm.updateLesson(l.copy(status = s), markModified = true); onDismiss() },
                    label    = { Text(statusLabel(s), style = MaterialTheme.typography.labelSmall) },
                    colors   = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = statusColor(s), selectedLabelColor = Color.White)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
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
// Used for both add and edit. Includes teacher override field (req 2 & 5).

@Composable
private fun LessonFormDialog(
    title: String, initial: Lesson?,
    state: AppState, vm: AppViewModel,
    onDismiss: () -> Unit, onSave: (Lesson) -> Unit
) {
    var classId   by remember { mutableLongStateOf(initial?.classId ?: state.classes.firstOrNull()?.id ?: 0L) }
    var date      by remember { mutableStateOf(initial?.date ?: LocalDate.now().toString()) }
    var startTime by remember { mutableStateOf(initial?.startTime ?: "08:00") }
    var endTime   by remember { mutableStateOf(initial?.endTime   ?: "08:45") }
    var status    by remember { mutableStateOf(initial?.status    ?: "pending") }
    var topic     by remember { mutableStateOf(initial?.topic     ?: "") }
    var notes     by remember { mutableStateOf(initial?.notes     ?: "") }
    var attendees by remember { mutableStateOf<List<Long>>(initial?.attendees ?: emptyList()) }
    var code      by remember { mutableStateOf(initial?.code?.ifBlank { null } ?: genCode("L")) }

    val selectedClass = state.classes.find { it.id == classId }

    // Teacher: default = class headTeacher, but can be overridden per-lesson
    var teacherOverrideId by remember {
        mutableStateOf(
            initial?.teacherIdOverride
                ?: selectedClass?.headTeacherId
        )
    }
    // When class changes, reset teacher to new class default
    LaunchedEffect(classId) {
        if (initial == null || initial.teacherIdOverride == null) {
            teacherOverrideId = state.classes.find { it.id == classId }?.headTeacherId
        }
    }

    val classStudents = state.students.filter { it.classIds.contains(classId) }
    val effectiveTeacherName = state.teachers.find { it.id == teacherOverrideId }?.name ?: ""
    val classDefaultTeacherName = state.teachers.find {
        it.id == state.classes.find { c -> c.id == classId }?.headTeacherId
    }?.name

    FluentDialog(title = title, onDismiss = onDismiss, onConfirm = {
        if (classId != 0L) {
            val classDefaultTeacherId = state.classes.find { it.id == classId }?.headTeacherId
            // Only store override if it differs from class default
            val overrideToSave = if (teacherOverrideId == classDefaultTeacherId) null
                                 else teacherOverrideId
            onSave(Lesson(
                id                = initial?.id ?: System.currentTimeMillis(),
                classId           = classId,
                date              = date,
                startTime         = startTime,
                endTime           = endTime,
                status            = status,
                topic             = topic,
                notes             = notes,
                attendees         = attendees,
                isModified        = initial != null,
                code              = code.trim().ifBlank { genCode("L") },
                teacherIdOverride = overrideToSave
            ))
        }
    }) {
        // Row 1: code + status
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top) {
            Box(Modifier.weight(1f)) { FormTextField("编号", code, { code = it }, "自动生成") }
            Box(Modifier.weight(1f)) {
                FormDropdown("状态", status,
                    listOf("pending","completed","absent","cancelled","postponed")) { status = it }
            }
        }
        // Row 2: class selector + subject badge
        FormDropdown("班级", selectedClass?.name ?: "",
            state.classes.map { it.name }) { name ->
            classId = state.classes.firstOrNull { it.name == name }?.id ?: classId
            attendees = emptyList()
            // Reset teacher to new class default
            teacherOverrideId = state.classes.firstOrNull { it.name == name }?.headTeacherId
        }
        selectedClass?.resolvedSubject(state.subjects)?.let { sub ->
            Surface(shape = RoundedCornerShape(8.dp), color = FluentPurple.copy(0.1f)) {
                Text("科目：${sub.name}",
                    style = MaterialTheme.typography.bodySmall, color = FluentPurple,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
            }
        }
        // Teacher override selector
        FormDropdown(
            label    = if (classDefaultTeacherName != null) "教师（默认：$classDefaultTeacherName）" else "教师",
            selected = effectiveTeacherName.ifBlank { "无" },
            options  = listOf("无") + state.teachers.map { it.name }
        ) { picked ->
            teacherOverrideId = if (picked == "无") null
                                else state.teachers.firstOrNull { it.name == picked }?.id
        }
        // Row 3: date + start time
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top) {
            Box(Modifier.weight(1f)) { DatePickerField("日期", date) { date = it } }
            Box(Modifier.weight(1f)) {
                StartTimeCompact(startTime) { newStart ->
                    val dur   = minutesBetween(startTime, endTime).coerceAtLeast(30)
                    startTime = newStart
                    endTime   = addMinutesToTime(newStart, dur)
                }
            }
        }
        DurationChipsCompact(startTime, endTime) { endTime = it }
        FormTextField("课题", topic, { topic = it }, "本节课主题")
        if (classStudents.isNotEmpty()) {
            SectionHeader("出勤学生")
            androidx.compose.foundation.layout.FlowRow(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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

// ── Batch generate dialog ─────────────────────────────────────────────────────

@Composable
private fun BatchGenerateDialog(state: AppState, vm: AppViewModel, onDismiss: () -> Unit) {
    var classId     by remember { mutableLongStateOf(state.classes.firstOrNull()?.id ?: 0L) }
    var recType     by remember { mutableStateOf("WEEKLY") }
    var dayOfWeek   by remember { mutableIntStateOf(1) }
    var startDate   by remember { mutableStateOf(LocalDate.now().toString()) }
    var endDate     by remember { mutableStateOf(LocalDate.now().plusWeeks(12).toString()) }
    var startTime   by remember { mutableStateOf("08:00") }
    var endTime     by remember { mutableStateOf("08:45") }
    // Exclude dates stored as a set; also editable as text
    var excludeSet  by remember { mutableStateOf(emptySet<String>()) }
    val excludeText = excludeSet.sorted().joinToString(", ")

    // Teacher override for batch-generated lessons; default = class headTeacher
    var teacherOverrideId by remember {
        mutableStateOf(state.classes.firstOrNull()?.headTeacherId)
    }
    LaunchedEffect(classId) {
        teacherOverrideId = state.classes.find { it.id == classId }?.headTeacherId
    }
    val selectedClass = state.classes.find { it.id == classId }
    val classDefaultTeacherName = state.teachers.find {
        it.id == selectedClass?.headTeacherId
    }?.name
    val effectiveTeacherName = state.teachers.find { it.id == teacherOverrideId }?.name ?: ""

    FluentDialog(title = "批量生成课次", onDismiss = onDismiss, confirmText = "生成", onConfirm = {
        if (classId == 0L) return@FluentDialog
        val sDate = runCatching { LocalDate.parse(startDate) }.getOrNull() ?: LocalDate.now()
        val eDate = runCatching { LocalDate.parse(endDate)   }.getOrNull() ?: sDate.plusWeeks(12)
        val classDefaultTeacherId = state.classes.find { it.id == classId }?.headTeacherId
        val overrideToSave = if (teacherOverrideId == classDefaultTeacherId) null
                             else teacherOverrideId
        vm.batchGenerateLessons(
            classId           = classId,
            recurrenceType    = AppViewModel.RecurrenceType.valueOf(recType),
            startDate         = sDate,
            endDate           = eDate,
            dayOfWeek         = dayOfWeek,
            startTime         = startTime,
            endTime           = endTime,
            excludeDates      = excludeSet,
            teacherIdOverride = overrideToSave
        )
        onDismiss()
    }) {
        FormDropdown("班级", selectedClass?.name ?: "",
            state.classes.map { it.name }) { name ->
            classId = state.classes.firstOrNull { it.name == name }?.id ?: classId
        }

        // Teacher override
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
            Row(Modifier.padding(horizontal = 16.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DAYS.forEachIndexed { i, d ->
                    FilterChip(selected = dayOfWeek == i + 1,
                        onClick = { dayOfWeek = i + 1 },
                        label   = { Text("周$d") })
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

        // ── Exclude dates section ─────────────────────────────────────────────
        if (recType != "ONCE") {
            SectionHeader("跳过日期")
            // Date picker button to add an exclusion date
            ExcludeDatePicker(excludeSet) { date ->
                excludeSet = excludeSet + date
            }
            // Display + remove chips for each excluded date
            if (excludeSet.isNotEmpty()) {
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement   = Arrangement.spacedBy(4.dp)
                ) {
                    excludeSet.sorted().forEach { d ->
                        InputChip(
                            selected     = false,
                            onClick      = { excludeSet = excludeSet - d },
                            label        = { Text(d, style = MaterialTheme.typography.labelSmall) },
                            trailingIcon = {
                                Icon(Icons.Default.Close, "删除",
                                    modifier = Modifier.size(14.dp))
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

/** Small inline date-picker button that appends a picked date to excludeSet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExcludeDatePicker(
    current: Set<String>,
    onAdd: (String) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    OutlinedButton(
        onClick = { showPicker = true },
        shape   = RoundedCornerShape(12.dp),
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
private fun BatchModifyDialog(classId: Long, state: AppState, vm: AppViewModel, onDismiss: () -> Unit) {
    val cls         = state.classes.find { it.id == classId }
    val allDates    = state.lessons.filter { it.classId == classId }.map { it.date }.sorted()
    val today       = LocalDate.now().toString()
    var fromDate    by remember { mutableStateOf(today) }
    var toDate      by remember { mutableStateOf(allDates.lastOrNull() ?: today) }
    var newStart    by remember { mutableStateOf("") }
    var newEnd      by remember { mutableStateOf("") }
    var skipMod     by remember { mutableStateOf(true) }
    var inclNonPend by remember { mutableStateOf(false) }

    FluentDialog(title = "批量修改 — ${cls?.name ?: "班级"}", onDismiss = onDismiss,
        confirmText = "批量修改", onConfirm = {
            vm.batchModifyLessons(classId, fromDate, toDate,
                newStart.ifBlank { null }, newEnd.ifBlank { null },
                skipMod, inclNonPend)
            onDismiss()
        }) {
        Text("默认不修改今天之前的课次。留空表示不修改该字段。",
            style = MaterialTheme.typography.bodySmall, color = FluentMuted,
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("跳过已单独修改的课次", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = skipMod, onCheckedChange = { skipMod = it })
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("包含已完成/已取消课次", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = inclNonPend, onCheckedChange = { inclNonPend = it })
        }
    }
}

// ── Batch delete dialog ───────────────────────────────────────────────────────

@Composable
private fun BatchDeleteDialog(classId: Long, state: AppState, vm: AppViewModel, onDismiss: () -> Unit) {
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

    FluentDialog(title = "批量删除 — ${cls?.name ?: "班级"}", onDismiss = onDismiss,
        confirmText = if (!confirmed) "确认删除 $targetCount 节" else "最终确认",
        onConfirm = {
            if (!confirmed) { confirmed = true; return@FluentDialog }
            vm.batchDeleteLessons(classId, fromDate, toDate, inclNonPend, inclMod)
            onDismiss()
        }) {
        Surface(shape = RoundedCornerShape(8.dp), color = FluentRed.copy(0.1f),
            modifier = Modifier.fillMaxWidth()) {
            Text("⚠️ 此操作不可撤销！将删除 $targetCount 节课次。",
                style = MaterialTheme.typography.bodySmall, color = FluentRed,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top) {
            Box(Modifier.weight(1f)) {
                DatePickerField("开始日期", fromDate) { fromDate = it; confirmed = false } }
            Box(Modifier.weight(1f)) {
                DatePickerField("结束日期", toDate)   { toDate   = it; confirmed = false } }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("包含已完成/已取消课次", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = inclNonPend, onCheckedChange = { inclNonPend = it; confirmed = false })
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("包含已单独修改的课次", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = inclMod, onCheckedChange = { inclMod = it; confirmed = false })
        }
    }
}

// ── Time / date picker helpers ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StartTimeCompact(startTime: String, onStartChange: (String) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = startTime, onValueChange = onStartChange,
        label = { Text("时间") }, singleLine = true,
        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            IconButton(onClick = { showPicker = true }) {
                Icon(Icons.Default.Schedule, null, tint = FluentBlue,
                    modifier = Modifier.size(18.dp)) }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = FluentBlue, unfocusedBorderColor = FluentBorder)
    )
    if (showPicker) LessonTimePickerDialog(startTime,
        onConfirm = { onStartChange(it); showPicker = false },
        onDismiss = { showPicker = false })
}

@Composable
internal fun DurationChipsCompact(startTime: String, endTime: String, onEndChange: (String) -> Unit) {
    var durMins by remember {
        mutableIntStateOf(minutesBetween(startTime, endTime).takeIf { it > 0 } ?: 45)
    }
    fun push(m: Int) { onEndChange(addMinutesToTime(startTime.ifBlank { "08:00" }, m)) }
    val h = durMins / 60; val m = durMins % 60

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()) {
        Text("时长", style = MaterialTheme.typography.labelMedium,
            color = FluentBlue, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(4.dp))
        OutlinedTextField(value = h.toString(),
            onValueChange = { raw ->
                val newH = raw.filter { it.isDigit() }.toIntOrNull()?.coerceIn(0, 23) ?: h
                durMins = newH * 60 + m; push(durMins) },
            label = { Text("时", style = MaterialTheme.typography.labelSmall) },
            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(8.dp), modifier = Modifier.width(52.dp),
            textStyle = MaterialTheme.typography.bodySmall,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FluentBlue, unfocusedBorderColor = FluentBorder))
        OutlinedTextField(value = "%02d".format(m),
            onValueChange = { raw ->
                val newM = raw.filter { it.isDigit() }.toIntOrNull()?.coerceIn(0, 59) ?: m
                durMins = h * 60 + newM; push(durMins) },
            label = { Text("分", style = MaterialTheme.typography.labelSmall) },
            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(8.dp), modifier = Modifier.width(52.dp),
            textStyle = MaterialTheme.typography.bodySmall,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FluentBlue, unfocusedBorderColor = FluentBorder))
        Spacer(Modifier.width(2.dp))
        FilledTonalIconButton(onClick = { durMins = (durMins - 10).coerceAtLeast(10); push(durMins) },
            modifier = Modifier.size(32.dp), shape = RoundedCornerShape(8.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = FluentBorder, contentColor = FluentMuted)) {
            Icon(Icons.Default.Remove, "-10分钟", Modifier.size(16.dp)) }
        FilledTonalIconButton(onClick = { durMins = (durMins + 10).coerceAtMost(23 * 60); push(durMins) },
            modifier = Modifier.size(32.dp), shape = RoundedCornerShape(8.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = FluentBlueLight, contentColor = FluentBlue)) {
            Icon(Icons.Default.Add, "+10分钟", Modifier.size(16.dp)) }
        Text("→ ${addMinutesToTime(startTime.ifBlank { "08:00" }, durMins)}",
            style = MaterialTheme.typography.labelSmall, color = FluentMuted)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DatePickerField(label: String, value: String, onChange: (String) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val epochMs: Long? = runCatching {
        val p = value.split("-")
        java.util.Calendar.getInstance()
            .apply { set(p[0].toInt(), p[1].toInt() - 1, p[2].toInt()) }.timeInMillis
    }.getOrNull()
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = epochMs)

    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) },
        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), singleLine = true,
        trailingIcon = {
            IconButton(onClick = { showPicker = true }) {
                Icon(Icons.Default.DateRange, null, tint = FluentBlue) } },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = FluentBlue, unfocusedBorderColor = FluentBorder))

    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { ms ->
                        val cal = java.util.Calendar.getInstance().apply { timeInMillis = ms }
                        onChange("%04d-%02d-%02d".format(
                            cal.get(java.util.Calendar.YEAR),
                            cal.get(java.util.Calendar.MONTH) + 1,
                            cal.get(java.util.Calendar.DAY_OF_MONTH)))
                    }
                    showPicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("取消") } }
        ) { DatePicker(state = pickerState) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LessonTimePickerDialog(initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val parts = initial.split(":").mapNotNull { it.toIntOrNull() }
    val ts    = rememberTimePickerState(parts.getOrElse(0){8}, parts.getOrElse(1){0}, is24Hour = true)
    AlertDialog(onDismissRequest = onDismiss, shape = RoundedCornerShape(20.dp),
        title = { Text("选择时间", fontWeight = FontWeight.Bold) },
        text  = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = ts) }
        },
        confirmButton = {
            Button(onClick = { onConfirm("%02d:%02d".format(ts.hour, ts.minute)) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FluentBlue)) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = FluentMuted) } }
    )
}
