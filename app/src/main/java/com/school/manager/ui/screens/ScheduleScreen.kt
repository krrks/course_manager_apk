package com.school.manager.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.school.manager.data.*
import com.school.manager.data.genCode
import com.school.manager.ui.components.*
import com.school.manager.ui.theme.*
import com.school.manager.viewmodel.AppViewModel
import java.time.LocalDate
import kotlin.math.roundToInt

// ── Calendar layout constants ──────────────────────────────────────────────────
private const val DP_PER_HOUR        = 80
private const val TIME_COL_W         = 52
private const val DAY_COL_W          = 110
private const val HEADER_H           = 36
private val CAL_TOTAL_HOURS          = CAL_END_HOUR - CAL_START_HOUR
private val CAL_TOTAL_HEIGHT_DP      = CAL_TOTAL_HOURS * DP_PER_HOUR
private const val CAL_V_PAD          = 10
private val CAL_BOX_HEIGHT_DP        = CAL_TOTAL_HEIGHT_DP + CAL_V_PAD * 2

private val TIME_TICKS: List<Pair<String, Int>> by lazy {
    val list = mutableListOf<Pair<String, Int>>()
    for (h in CAL_START_HOUR..CAL_END_HOUR) {
        list += "%02d:00".format(h) to (h - CAL_START_HOUR) * DP_PER_HOUR
        if (h < CAL_END_HOUR)
            list += "%02d:30".format(h) to (h - CAL_START_HOUR) * DP_PER_HOUR + DP_PER_HOUR / 2
    }
    list
}

private fun minuteOffsetDp(hhmm: String): Float {
    if (hhmm.isBlank()) return 0f
    val mins = timeToMinutes(hhmm) - CAL_START_HOUR * 60
    return (mins * (DP_PER_HOUR / 60f)).coerceAtLeast(0f)
}

private fun durationDp(startHhmm: String, endHhmm: String): Float {
    if (startHhmm.isBlank() || endHhmm.isBlank()) return DP_PER_HOUR / 2f
    val mins = (timeToMinutes(endHhmm) - timeToMinutes(startHhmm)).coerceAtLeast(10)
    return mins * (DP_PER_HOUR / 60f)
}

// ── Duration arithmetic helpers ────────────────────────────────────────────────

private fun addMinutesToTime(hhmm: String, minutes: Int): String {
    val base  = if (hhmm.isBlank()) 8 * 60 else timeToMinutes(hhmm)
    val total = (base + minutes).coerceIn(0, 23 * 60 + 59)
    return "%02d:%02d".format(total / 60, total % 60)
}

private fun minutesBetween(start: String, end: String): Int =
    if (start.isBlank() || end.isBlank()) 60
    else (timeToMinutes(end) - timeToMinutes(start)).coerceAtLeast(0)

// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ScheduleScreen(vm: AppViewModel, onOpenDrawer: () -> Unit = {}) {
    val state by vm.state.collectAsState()

    var viewMode   by remember { mutableStateOf("calendar") }
    var filterMode by remember { mutableStateOf("all") }
    var filterId   by remember { mutableLongStateOf(0L) }
    var showAdd    by remember { mutableStateOf(false) }
    var menuOpen   by remember { mutableStateOf(false) }
    var toast      by remember { mutableStateOf<String?>(null) }

    var addAttendanceForSlot by remember { mutableStateOf<Schedule?>(null) }

    val context = LocalContext.current
    var pendingBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val saveBitmapLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/jpeg")
    ) { uri ->
        if (uri != null && pendingBitmap != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    pendingBitmap!!.compress(Bitmap.CompressFormat.JPEG, 92, out)
                }
                toast = "✅ 已保存为 JPG"
            } catch (_: Exception) { toast = "❌ 保存失败" }
        }
    }

    val slots = when (filterMode) {
        "teacher" -> state.schedule.filter { it.teacherId == filterId }
        "student" -> state.schedule.filter { s ->
            state.students.find { it.id == filterId }?.classIds?.contains(s.classId) == true
        }
        else -> state.schedule
    }

    fun exportJpg() {
        val filterLabel = when (filterMode) {
            "teacher" -> vm.teacher(filterId)?.name ?: ""
            "student" -> vm.student(filterId)?.name ?: ""
            else      -> ""
        }
        val title = if (filterLabel.isNotBlank()) "${filterLabel}课表" else "全部课表"
        pendingBitmap = renderScheduleBitmap(context, slots, state, title)
        saveBitmapLauncher.launch("schedule_${filterLabel.ifBlank { "all" }}.jpg")
    }

    val screenTitle = when (filterMode) {
        "teacher" -> state.teachers.firstOrNull { it.id == filterId }?.let { "${it.name}的课表" } ?: "课表"
        "student" -> state.students.firstOrNull { it.id == filterId }?.let { "${it.name}的课表" } ?: "课表"
        else      -> "课表"
    }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                AnimatedVisibility(
                    visible = menuOpen,
                    enter   = fadeIn() + slideInVertically { it / 2 },
                    exit    = fadeOut() + slideOutVertically { it / 2 }
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 4.dp)
                    ) {
                        SpeedDialItem(
                            if (viewMode == "calendar") "切换列表视图" else "切换日历视图",
                            if (viewMode == "calendar") Icons.AutoMirrored.Filled.ViewList
                            else Icons.Default.CalendarViewMonth,
                            FluentBlue
                        ) { viewMode = if (viewMode == "calendar") "list" else "calendar" }

                        HorizontalDivider(color = FluentBorder.copy(alpha = 0.4f))

                        SpeedDialItem("全部课表", Icons.Default.CalendarMonth, FluentBlue,
                            selected = filterMode == "all"
                        ) { filterMode = "all"; filterId = 0; menuOpen = false }

                        var teacherMenuOpen by remember { mutableStateOf(false) }
                        Box {
                            SpeedDialItem("按教师筛选", Icons.Default.Person, FluentGreen,
                                selected = filterMode == "teacher"
                            ) { teacherMenuOpen = true }
                            DropdownMenu(
                                expanded         = teacherMenuOpen,
                                onDismissRequest = { teacherMenuOpen = false }
                            ) {
                                state.teachers.forEach { t ->
                                    DropdownMenuItem(
                                        text    = { Text(t.name) },
                                        onClick = { filterMode = "teacher"; filterId = t.id; teacherMenuOpen = false; menuOpen = false }
                                    )
                                }
                            }
                        }

                        var studentMenuOpen by remember { mutableStateOf(false) }
                        Box {
                            SpeedDialItem("按学生筛选", Icons.Default.School, FluentAmber,
                                selected = filterMode == "student"
                            ) { studentMenuOpen = true }
                            DropdownMenu(
                                expanded         = studentMenuOpen,
                                onDismissRequest = { studentMenuOpen = false }
                            ) {
                                state.students.forEach { s ->
                                    DropdownMenuItem(
                                        text    = { Text(s.name) },
                                        onClick = { filterMode = "student"; filterId = s.id; studentMenuOpen = false; menuOpen = false }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = FluentBorder.copy(alpha = 0.4f))

                        SpeedDialItem("导出为 JPG", Icons.Default.Image, FluentAmber) { exportJpg(); menuOpen = false }
                        SpeedDialItem("添加课程",   Icons.Default.Add,   FluentBlue)  { showAdd = true; menuOpen = false }

                        HorizontalDivider(color = FluentBorder.copy(alpha = 0.4f))

                        SpeedDialItem("导航菜单", Icons.Default.Menu, FluentMuted) { onOpenDrawer(); menuOpen = false }
                    }
                }

                FloatingActionButton(
                    onClick        = { menuOpen = !menuOpen },
                    containerColor = FluentBlue,
                    contentColor   = Color.White,
                    shape          = CircleShape
                ) {
                    Icon(if (menuOpen) Icons.Default.Close else Icons.Default.Add, null)
                }
            }
        }
    ) { inner ->
        Box(Modifier.fillMaxSize()) {
            if (screenTitle != "课表") {
                Text(
                    screenTitle,
                    style    = MaterialTheme.typography.labelMedium,
                    color    = FluentBlue,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = inner.calculateTopPadding() + 4.dp)
                )
            }
            if (viewMode == "calendar") CalendarGrid(slots, vm, state) { addAttendanceForSlot = it }
            else                        ScheduleListView(slots, state, vm)
        }
    }

    // ── FIX: pass slot directly; vm.addAttendance takes expanded params, not an Attendance object
    addAttendanceForSlot?.let { slot ->
        AddAttendanceFromScheduleDialog(
            slot      = slot,
            state     = state,
            vm        = vm,
            onDismiss = { addAttendanceForSlot = null },
            onSave    = { a ->
                vm.addAttendance(
                    a.classId, a.subjectId, a.teacherId, a.date,
                    a.startTime, a.endTime, a.topic, a.status,
                    a.notes, a.attendees, a.code
                )
                addAttendanceForSlot = null
            }
        )
    }

    if (showAdd) { AddScheduleDialog(state, vm, onDismiss = { showAdd = false }) }

    toast?.let { msg ->
        LaunchedEffect(msg) { kotlinx.coroutines.delay(2500); toast = null }
        Box(
            modifier = Modifier.fillMaxSize().padding(bottom = 32.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF323232)) {
                Text(msg, color = Color.White,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
            }
        }
    }
}

// ── List view ─────────────────────────────────────────────────────────────────

@Composable
private fun ScheduleListView(slots: List<Schedule>, state: AppState, vm: AppViewModel) {
    var editing by remember { mutableStateOf<Schedule?>(null) }

    LazyColumn(
        contentPadding      = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val grouped = slots.groupBy { it.day }.toSortedMap()
        grouped.forEach { (day, daySlots) ->
            item {
                Text(
                    DAYS.getOrElse(day - 1) { "?" },
                    style    = MaterialTheme.typography.titleSmall,
                    color    = FluentBlue,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
                )
            }
            items(daySlots.sortedBy { it.startTime }) { slot ->
                ScheduleCard(slot, state) { editing = slot }
            }
        }
    }

    editing?.let { slot ->
        EditScheduleDialog(slot, state, vm, onDismiss = { editing = null })
    }
}

@Composable
private fun ScheduleCard(slot: Schedule, state: AppState, onEdit: () -> Unit) {
    val cls = state.classes.find { it.id == slot.classId }
    val sub = state.subjects.find { it.id == slot.subjectId }
    val tea = state.teachers.find { it.id == slot.teacherId }

    Surface(
        onClick         = onEdit,
        shape           = RoundedCornerShape(14.dp),
        color           = MaterialTheme.colorScheme.surface,
        tonalElevation  = 1.dp,
        shadowElevation = 1.dp,
        modifier        = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(cls?.name ?: "未知班级", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(sub?.name ?: cls?.subject ?: "—", style = MaterialTheme.typography.bodySmall, color = FluentBlue)
                if (tea != null) Text(tea.name, style = MaterialTheme.typography.bodySmall, color = FluentMuted)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("${slot.resolvedStart()} – ${slot.resolvedEnd()}",
                    style = MaterialTheme.typography.bodySmall, color = FluentMuted)
                Text(slot.code, style = MaterialTheme.typography.labelSmall, color = FluentMuted)
            }
        }
    }
}

// ── Calendar grid ─────────────────────────────────────────────────────────────

@Composable
private fun CalendarGrid(
    slots: List<Schedule>, vm: AppViewModel, state: AppState,
    onAddAttendance: (Schedule) -> Unit
) {
    var editing by remember { mutableStateOf<Schedule?>(null) }
    var scale   by remember { mutableFloatStateOf(1f) }
    val scaledDayW = DAY_COL_W * scale
    val scaledCalH = CAL_BOX_HEIGHT_DP * scale

    val scrollH = rememberScrollState()
    val scrollV = rememberScrollState()

    val transformState = rememberTransformableState { zoom, _, _ ->
        scale = (scale * zoom).coerceIn(0.5f, 2.5f)
    }

    Column {
        // Day headers
        Row(Modifier.horizontalScroll(scrollH)) {
            Spacer(Modifier.width(TIME_COL_W.dp))
            DAYS.forEach { day ->
                Box(
                    Modifier.width(scaledDayW.dp).height(HEADER_H.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(day, style = MaterialTheme.typography.labelMedium,
                        color = FluentBlue, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        HorizontalDivider(color = FluentBorder)

        Row(Modifier.fillMaxSize().transformable(transformState)) {
            // Time column
            Box(
                Modifier.width(TIME_COL_W.dp).height(scaledCalH.dp).verticalScroll(scrollV)
            ) {
                TIME_TICKS.forEach { (label, yDpRaw) ->
                    if (label.endsWith(":00")) Text(
                        label,
                        style    = MaterialTheme.typography.labelSmall,
                        color    = FluentMuted,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(y = (yDpRaw * scale + CAL_V_PAD).dp)
                            .padding(end = 4.dp)
                    )
                }
            }

            Box(Modifier.fillMaxSize().horizontalScroll(scrollH).verticalScroll(scrollV)) {
                Row(Modifier.height(scaledCalH.dp)) {
                    DAYS.forEachIndexed { di, _ ->
                        val daySlots = slots.filter { it.day == di + 1 }
                        Box(Modifier.width(scaledDayW.dp).height(scaledCalH.dp)) {
                            TIME_TICKS.forEach { (label, yDpRaw) ->
                                val isHour = label.endsWith(":00")
                                HorizontalDivider(
                                    color     = if (isHour) FluentBorder else FluentBorder.copy(alpha = 0.4f),
                                    thickness = if (isHour) 0.8.dp else 0.4.dp,
                                    modifier  = Modifier.offset(y = (yDpRaw * scale + CAL_V_PAD).dp)
                                )
                            }
                            daySlots.forEach { slot ->
                                val startStr = slot.resolvedStart()
                                val endStr   = slot.resolvedEnd()
                                val yDp      = minuteOffsetDp(startStr) * scale + CAL_V_PAD
                                val hDp      = (durationDp(startStr, endStr) * scale).coerceAtLeast(28f)
                                val sub      = state.subjects.find { it.id == slot.subjectId }
                                    ?: state.subjects.find { s ->
                                        state.classes.find { it.id == slot.classId }?.subject == s.name
                                    }
                                val cl       = state.classes.find { it.id == slot.classId }
                                val colorIdx = state.subjects.indexOf(sub).takeIf { it >= 0 }
                                    ?: state.subjects.indexOfFirst { s ->
                                        state.classes.find { it.id == slot.classId }?.subject == s.name
                                    }.takeIf { it >= 0 } ?: 0
                                val blockColor = PALETTE[colorIdx % PALETTE.size]

                                Surface(
                                    onClick  = { editing = slot },
                                    shape    = RoundedCornerShape(8.dp),
                                    color    = blockColor.copy(alpha = 0.15f),
                                    modifier = Modifier
                                        .offset(y = yDp.dp)
                                        .width((scaledDayW - 4).dp)
                                        .height(hDp.dp)
                                        .padding(horizontal = 2.dp, vertical = 1.dp)
                                ) {
                                    Column(Modifier.padding(4.dp)) {
                                        Text(
                                            sub?.name ?: cl?.subject ?: "?",
                                            style      = MaterialTheme.typography.labelSmall,
                                            color      = blockColor,
                                            fontWeight = FontWeight.Bold,
                                            maxLines   = 1,
                                            overflow   = TextOverflow.Ellipsis
                                        )
                                        if (hDp > 40) Text(
                                            cl?.name ?: "?",
                                            style    = MaterialTheme.typography.labelSmall,
                                            color    = blockColor.copy(alpha = 0.8f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editing?.let { slot ->
        EditScheduleDialog(
            slot      = slot,
            state     = state,
            vm        = vm,
            onDismiss = { editing = null },
            onAddAtt  = { onAddAttendance(slot); editing = null }
        )
    }
}

// ── Dialogs ───────────────────────────────────────────────────────────────────

/**
 * 添加课程 — 重设计布局：
 * 行1: 班级(2/3) + 教师(1/3)
 * 行2: 科目 badge（自动）
 * 行3: 星期(1/3) + 课程编号(2/3)
 * 行4: 开始时间(1/3) + 课时时长 chips(2/3)
 */
@Composable
private fun AddScheduleDialog(state: AppState, vm: AppViewModel, onDismiss: () -> Unit) {
    var className   by remember { mutableStateOf(state.classes.firstOrNull()?.name ?: "") }
    var teacherName by remember { mutableStateOf("") }
    var day         by remember { mutableStateOf(DAYS[0]) }
    var startTime   by remember { mutableStateOf("08:00") }
    var endTime     by remember { mutableStateOf("09:30") }
    var code        by remember { mutableStateOf(genCode("SCH")) }

    val classSubject = state.classes.firstOrNull { it.name == className }?.subject ?: ""

    FluentDialog(title = "添加课程", onDismiss = onDismiss, onConfirm = {
        val cls = state.classes.firstOrNull { it.name == className } ?: return@FluentDialog
        val sId = state.subjects.firstOrNull { it.name == cls.subject }?.id
            ?: state.subjects.firstOrNull()?.id ?: return@FluentDialog
        val tId = state.teachers.firstOrNull { it.name == teacherName }?.id
        val d   = DAYS.indexOf(day) + 1
        vm.addSchedule(cls.id, sId, tId, d, startTime, endTime, code)
        onDismiss()
    }) {
        // 行1：班级 + 教师
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(2f)) {
                FormDropdown("班级", className, state.classes.map { it.name }) { className = it }
            }
            Box(Modifier.weight(1f)) {
                FormDropdown("教师", teacherName, listOf("") + state.teachers.map { it.name }) { teacherName = it }
            }
        }

        // 科目 badge
        if (classSubject.isNotBlank()) {
            Surface(shape = RoundedCornerShape(8.dp), color = FluentBlueLight) {
                Text(
                    "科目：$classSubject",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = FluentBlue,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        // 行2：星期 + 课程编号
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) {
                FormDropdown("星期", day, DAYS) { day = it }
            }
            Box(Modifier.weight(2f)) {
                FormTextField("课程编号", code, { code = it })
            }
        }

        // 行3：开始时间 + 课时时长
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.Top
        ) {
            Box(Modifier.weight(1f)) {
                StartTimeField(startTime) { newStart ->
                    val dur = minutesBetween(startTime, endTime).coerceAtLeast(30)
                    startTime = newStart
                    endTime   = addMinutesToTime(newStart, dur)
                }
            }
            Box(Modifier.weight(2f)) {
                InlineDurationPicker(startTime = startTime, endTime = endTime) { endTime = it }
            }
        }
    }
}

/**
 * 编辑课程 — 同上布局
 */
@Composable
private fun EditScheduleDialog(
    slot: Schedule, state: AppState, vm: AppViewModel,
    onDismiss: () -> Unit,
    onAddAtt: (() -> Unit)? = null
) {
    var className   by remember { mutableStateOf(state.classes.find { it.id == slot.classId }?.name ?: "") }
    var teacherName by remember { mutableStateOf(state.teachers.find { it.id == slot.teacherId }?.name ?: "") }
    var day         by remember { mutableStateOf(DAYS.getOrElse(slot.day - 1) { DAYS[0] }) }
    var startTime   by remember { mutableStateOf(slot.startTime) }
    var endTime     by remember { mutableStateOf(slot.endTime) }
    var code        by remember { mutableStateOf(slot.code) }
    var confirmDel  by remember { mutableStateOf(false) }

    FluentDialog(title = "编辑课程", onDismiss = onDismiss, onConfirm = {
        val cls = state.classes.firstOrNull { it.name == className } ?: return@FluentDialog
        val sId = state.subjects.firstOrNull { it.name == cls.subject }?.id
            ?: state.subjects.firstOrNull()?.id ?: return@FluentDialog
        val tId = state.teachers.firstOrNull { it.name == teacherName }?.id
        val d   = DAYS.indexOf(day) + 1
        vm.updateSchedule(slot.copy(
            classId   = cls.id,
            subjectId = sId,
            teacherId = tId,
            day       = d,
            startTime = startTime,
            endTime   = endTime,
            code      = code
        ))
        onDismiss()
    }) {
        // 行1：班级 + 教师
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(2f)) {
                FormDropdown("班级", className, state.classes.map { it.name }) { className = it }
            }
            Box(Modifier.weight(1f)) {
                FormDropdown("教师", teacherName, listOf("") + state.teachers.map { it.name }) { teacherName = it }
            }
        }

        // 行2：星期 + 课程编号
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) {
                FormDropdown("星期", day, DAYS) { day = it }
            }
            Box(Modifier.weight(2f)) {
                FormTextField("课程编号", code, { code = it })
            }
        }

        // 行3：开始时间 + 课时时长
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.Top
        ) {
            Box(Modifier.weight(1f)) {
                StartTimeField(startTime) { newStart ->
                    val dur = minutesBetween(startTime, endTime).coerceAtLeast(30)
                    startTime = newStart
                    endTime   = addMinutesToTime(newStart, dur)
                }
            }
            Box(Modifier.weight(2f)) {
                InlineDurationPicker(startTime = startTime, endTime = endTime) { endTime = it }
            }
        }

        if (onAddAtt != null) {
            OutlinedButton(
                onClick  = { onAddAtt() },
                shape    = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) { Text("+ 添加上课记录") }
        }

        if (!confirmDel) {
            OutlinedButton(
                onClick  = { confirmDel = true },
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = FluentRed),
                shape    = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) { Text("删除课程") }
        } else {
            Text("确定删除？", color = FluentRed)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { confirmDel = false }, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)) { Text("取消") }
                Button(
                    onClick = { vm.deleteSchedule(slot.id); onDismiss() },
                    colors  = ButtonDefaults.buttonColors(containerColor = FluentRed),
                    shape   = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) { Text("确认") }
            }
        }
    }
}

/**
 * 添加上课记录（从课表格子触发）— 重设计布局：
 * 行1: 科目+班级 badge
 * 行2: 教师（全宽）
 * 行3: 日期(1/2) + 开始时间(1/2)
 * 行4: 课时时长 chips（全宽）
 * 行5: 状态（全宽）
 * 行6: 课题 / 出勤学生 / 备注
 */
@Composable
private fun AddAttendanceFromScheduleDialog(
    slot: Schedule, state: AppState, vm: AppViewModel,
    onDismiss: () -> Unit, onSave: (Attendance) -> Unit
) {
    val today       = LocalDate.now()
    var date        by remember { mutableStateOf(today.toString()) }
    var teacherName by remember { mutableStateOf(state.teachers.find { it.id == slot.teacherId }?.name ?: "") }
    var startTime   by remember { mutableStateOf(slot.resolvedStart()) }
    var endTime     by remember { mutableStateOf(slot.resolvedEnd()) }
    var topic       by remember { mutableStateOf("") }
    var status      by remember { mutableStateOf("completed") }
    var notes       by remember { mutableStateOf("") }
    val allStudents = state.students.filter { s ->
        state.classes.find { it.id == slot.classId }?.let { s.classIds.contains(it.id) } == true
    }
    val checkedIds = remember { mutableStateListOf<Long>().also { it.addAll(allStudents.map { s -> s.id }) } }

    val subjectName = state.subjects.find { it.id == slot.subjectId }?.name
        ?: state.classes.find { it.id == slot.classId }?.subject ?: ""
    val className   = state.classes.find { it.id == slot.classId }?.name ?: ""

    FluentDialog(title = "添加上课记录", onDismiss = onDismiss, onConfirm = {
        val tId = state.teachers.firstOrNull { it.name == teacherName }?.id ?: slot.teacherId
        val cls = state.classes.find { it.id == slot.classId } ?: return@FluentDialog
        onSave(Attendance(
            id        = System.currentTimeMillis(),
            classId   = cls.id,
            subjectId = slot.subjectId,
            teacherId = tId,
            date      = date,
            startTime = startTime,
            endTime   = endTime,
            topic     = topic,
            status    = status,
            notes     = notes,
            attendees = checkedIds.toList(),
            code      = genCode("ATT")
        ))
    }) {
        // 科目 + 班级 badge
        if (subjectName.isNotBlank() || className.isNotBlank()) {
            Surface(shape = RoundedCornerShape(8.dp), color = FluentPurple.copy(alpha = 0.1f)) {
                Text(
                    buildString {
                        if (subjectName.isNotBlank()) append("科目：$subjectName")
                        if (subjectName.isNotBlank() && className.isNotBlank()) append("  ·  ")
                        if (className.isNotBlank()) append(className)
                    },
                    style      = MaterialTheme.typography.bodySmall,
                    color      = FluentPurple,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        // 教师
        FormDropdown("教师", teacherName, listOf("") + state.teachers.map { it.name }) { teacherName = it }

        // 日期 + 开始时间
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.Top
        ) {
            Box(Modifier.weight(1f)) {
                DatePickerField("日期", date) { date = it }
            }
            Box(Modifier.weight(1f)) {
                StartTimeField(startTime) { newStart ->
                    val dur = minutesBetween(startTime, endTime).coerceAtLeast(30)
                    startTime = newStart
                    endTime   = addMinutesToTime(newStart, dur)
                }
            }
        }

        // 课时时长
        InlineDurationPicker(startTime = startTime, endTime = endTime) { endTime = it }

        // 状态
        FormDropdown("状态", status, listOf("completed", "cancelled", "pending")) { status = it }

        // 课题
        FormTextField("课题", topic, { topic = it }, "本节课主题")

        // 出勤学生
        if (allStudents.isNotEmpty()) {
            SectionHeader("出勤学生")
            androidx.compose.foundation.layout.FlowRow(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                allStudents.forEach { s ->
                    val on = checkedIds.contains(s.id)
                    FilterChip(selected = on, onClick = {
                        if (on) checkedIds.remove(s.id) else checkedIds.add(s.id)
                    }, label = { Text(s.name) })
                }
            }
        }

        // 备注
        FormTextField("备注", notes, { notes = it }, "可选")
    }
}

// ── Time picker components ────────────────────────────────────────────────────

/**
 * 开始时间字段（带时钟 dial 选择器）。
 * 从 TimeRangeRow 中独立出来，供 Dialog 在 Row 中单独摆放。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StartTimeField(
    startTime: String,
    onStartChange: (String) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }

    OutlinedTextField(
        value         = startTime,
        onValueChange = onStartChange,
        label         = { Text("开始时间") },
        singleLine    = true,
        shape         = RoundedCornerShape(12.dp),
        modifier      = Modifier.fillMaxWidth(),
        trailingIcon  = {
            IconButton(onClick = { showPicker = true }) {
                Icon(Icons.Default.Schedule, null, tint = FluentBlue)
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = FluentBlue,
            unfocusedBorderColor = FluentBorder
        )
    )

    if (showPicker) {
        TimePickerDialog(
            initial   = startTime,
            onConfirm = { onStartChange(it); showPicker = false },
            onDismiss = { showPicker = false }
        )
    }
}

/**
 * 课时时长内联选择器（紧凑，无 Surface 背景块）。
 * 包含预设 chips + 手动时分输入 + 结束时间提示。
 */
@Composable
internal fun InlineDurationPicker(
    startTime: String,
    endTime: String,
    onEndChange: (String) -> Unit
) {
    var durMins by remember {
        mutableIntStateOf(minutesBetween(startTime, endTime).takeIf { it > 0 } ?: 60)
    }

    fun push(mins: Int) { onEndChange(addMinutesToTime(startTime.ifBlank { "08:00" }, mins)) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "课时时长",
            style      = MaterialTheme.typography.labelMedium,
            color      = FluentBlue,
            fontWeight = FontWeight.SemiBold
        )

        // Preset chips
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(60 to "1小时", 90 to "1.5小时", 120 to "2小时").forEach { (mins, label) ->
                FilterChip(
                    selected = durMins == mins,
                    onClick  = { durMins = mins; push(mins) },
                    label    = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    colors   = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FluentBlue,
                        selectedLabelColor     = Color.White
                    )
                )
            }
        }

        // 手动时分 + 结束时间提示
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            val h = durMins / 60
            val m = durMins % 60

            OutlinedTextField(
                value         = h.toString(),
                onValueChange = { raw ->
                    val newH = raw.filter { it.isDigit() }.toIntOrNull()?.coerceIn(0, 23) ?: 0
                    durMins = newH * 60 + m; push(durMins)
                },
                label       = { Text("时", style = MaterialTheme.typography.labelSmall) },
                singleLine  = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape       = RoundedCornerShape(8.dp),
                modifier    = Modifier.width(56.dp),
                textStyle   = MaterialTheme.typography.bodySmall,
                colors      = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = FluentBlue,
                    unfocusedBorderColor = FluentBorder
                )
            )

            OutlinedTextField(
                value         = m.toString(),
                onValueChange = { raw ->
                    val newM = raw.filter { it.isDigit() }.toIntOrNull()?.coerceIn(0, 59) ?: 0
                    durMins = h * 60 + newM; push(durMins)
                },
                label       = { Text("分", style = MaterialTheme.typography.labelSmall) },
                singleLine  = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape       = RoundedCornerShape(8.dp),
                modifier    = Modifier.width(56.dp),
                textStyle   = MaterialTheme.typography.bodySmall,
                colors      = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = FluentBlue,
                    unfocusedBorderColor = FluentBorder
                )
            )

            Text(
                "→ ${addMinutesToTime(startTime.ifBlank { "08:00" }, durMins)}",
                style = MaterialTheme.typography.labelSmall,
                color = FluentMuted
            )
        }
    }
}

/**
 * 原 TimeRangeRow — 保留供 AttendanceScreen.kt 继续 import 使用。
 */
@Composable
internal fun TimeRangeRow(
    startTime: String,
    endTime: String,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var durMins by remember {
        mutableIntStateOf(
            minutesBetween(startTime, endTime).takeIf { it > 0 } ?: 60
        )
    }
    fun pushEnd(start: String, mins: Int) { onEndChange(addMinutesToTime(start, mins)) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value         = startTime,
            onValueChange = { onStartChange(it); pushEnd(it, durMins) },
            label         = { Text("开始时间") },
            singleLine    = true,
            shape         = RoundedCornerShape(12.dp),
            modifier      = Modifier.fillMaxWidth(),
            trailingIcon  = {
                IconButton(onClick = { showStartPicker = true }) {
                    Icon(Icons.Default.Schedule, null, tint = FluentBlue)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = FluentBlue,
                unfocusedBorderColor = FluentBorder
            )
        )

        Surface(shape = RoundedCornerShape(12.dp), color = FluentBlueLight, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("课时时长", style = MaterialTheme.typography.labelMedium, color = FluentBlue, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(60 to "1小时", 90 to "1.5小时", 120 to "2小时").forEach { (mins, label) ->
                        FilterChip(
                            selected = durMins == mins,
                            onClick  = { durMins = mins; pushEnd(startTime.ifBlank { "08:00" }, mins) },
                            label    = { Text(label) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FluentBlue,
                                selectedLabelColor     = Color.White
                            )
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    val h = durMins / 60; val m = durMins % 60
                    OutlinedTextField(
                        value = h.toString(),
                        onValueChange = { raw ->
                            val newH = raw.filter { it.isDigit() }.toIntOrNull()?.coerceIn(0, 23) ?: 0
                            durMins = newH * 60 + m; pushEnd(startTime.ifBlank { "08:00" }, durMins)
                        },
                        label = { Text("时") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(8.dp), modifier = Modifier.width(64.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FluentBlue, unfocusedBorderColor = FluentBorder)
                    )
                    OutlinedTextField(
                        value = m.toString(),
                        onValueChange = { raw ->
                            val newM = raw.filter { it.isDigit() }.toIntOrNull()?.coerceIn(0, 59) ?: 0
                            durMins = h * 60 + newM; pushEnd(startTime.ifBlank { "08:00" }, durMins)
                        },
                        label = { Text("分") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(8.dp), modifier = Modifier.width(64.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FluentBlue, unfocusedBorderColor = FluentBorder)
                    )
                    Text("→ ${addMinutesToTime(startTime.ifBlank { "08:00" }, durMins)}",
                        style = MaterialTheme.typography.bodySmall, color = FluentMuted)
                }
            }
        }
    }

    if (showStartPicker) {
        TimePickerDialog(
            initial   = startTime,
            onConfirm = { onStartChange(it); pushEnd(it, durMins); showStartPicker = false },
            onDismiss = { showStartPicker = false }
        )
    }
}

/**
 * Clock-dial 时间选择弹窗。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val parts = initial.split(":").mapNotNull { it.toIntOrNull() }
    val pickerState = rememberTimePickerState(
        initialHour   = parts.getOrElse(0) { 8 },
        initialMinute = parts.getOrElse(1) { 0 },
        is24Hour      = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape            = RoundedCornerShape(20.dp),
        title            = { Text("选择开始时间", fontWeight = FontWeight.Bold) },
        text             = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = pickerState)
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm("%02d:%02d".format(pickerState.hour, pickerState.minute)) },
                shape   = RoundedCornerShape(12.dp),
                colors  = ButtonDefaults.buttonColors(containerColor = FluentBlue)
            ) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = FluentMuted) } }
    )
}

/**
 * 日期字段（带日历选择器）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DatePickerField(label: String, value: String, onChange: (String) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val epochMs: Long? = runCatching {
        java.time.LocalDate.parse(value).atStartOfDay(java.time.ZoneId.of("UTC")).toInstant().toEpochMilli()
    }.getOrNull()

    OutlinedTextField(
        value         = value,
        onValueChange = onChange,
        label         = { Text(label) },
        singleLine    = true,
        shape         = RoundedCornerShape(12.dp),
        modifier      = Modifier.fillMaxWidth(),
        trailingIcon  = {
            IconButton(onClick = { showPicker = true }) {
                Icon(Icons.Default.CalendarMonth, null, tint = FluentBlue)
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = FluentBlue,
            unfocusedBorderColor = FluentBorder
        )
    )

    if (showPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = epochMs)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton    = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { ms ->
                            val ld = java.time.Instant.ofEpochMilli(ms)
                                .atZone(java.time.ZoneId.of("UTC")).toLocalDate()
                            onChange(ld.toString())
                        }
                        showPicker = false
                    },
                    shape  = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FluentBlue)
                ) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("取消") } }
        ) { DatePicker(state = datePickerState) }
    }
}

// ── Bitmap export ─────────────────────────────────────────────────────────────

private val PALETTE = listOf(
    Color(0xFF2196F3), Color(0xFF4CAF50), Color(0xFFFF9800),
    Color(0xFF9C27B0), Color(0xFFE91E63), Color(0xFF00BCD4),
    Color(0xFF8BC34A), Color(0xFFFF5722)
)

internal fun renderScheduleBitmap(
    context: Context, slots: List<Schedule>, state: AppState, title: String
): Bitmap {
    val cellH = 60f; val headerH = 80f; val timeW = 80f
    val hours = (CAL_START_HOUR..CAL_END_HOUR).toList()
    val w = (timeW + DAY_COL_W * 7).toInt()
    val h = (headerH + hours.size * cellH).toInt()
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val cv  = Canvas(bmp)
    cv.drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply { color = android.graphics.Color.WHITE })

    val titlePaint = Paint().apply { color = android.graphics.Color.BLACK; textSize = 28f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
    cv.drawText(title, timeW, 40f, titlePaint)

    val dayPaint = Paint().apply { color = android.graphics.Color.DKGRAY; textSize = 22f; isAntiAlias = true }
    DAYS.forEachIndexed { i, d -> cv.drawText(d, timeW + i * DAY_COL_W + 8f, headerH - 10f, dayPaint) }

    val timePaint = Paint().apply { color = android.graphics.Color.GRAY; textSize = 18f; isAntiAlias = true }
    hours.forEachIndexed { i, hh ->
        cv.drawText("%02d:00".format(hh), 4f, headerH + i * cellH + 20f, timePaint)
        cv.drawLine(timeW, headerH + i * cellH, w.toFloat(), headerH + i * cellH,
            Paint().apply { color = android.graphics.Color.LTGRAY; strokeWidth = 0.5f })
    }

    val blockColors = listOf(
        android.graphics.Color.rgb(33, 150, 243), android.graphics.Color.rgb(76, 175, 80),
        android.graphics.Color.rgb(255, 152, 0),  android.graphics.Color.rgb(156, 39, 176),
        android.graphics.Color.rgb(233, 30, 99)
    )
    slots.forEach { slot ->
        val dayIdx   = (slot.day - 1).coerceIn(0, 6)
        val startMin = timeToMinutes(slot.resolvedStart()) - CAL_START_HOUR * 60
        val endMin   = timeToMinutes(slot.resolvedEnd())   - CAL_START_HOUR * 60
        val top      = headerH + startMin * (cellH / 60f)
        val bottom   = (headerH + endMin * (cellH / 60f)).coerceAtMost(h.toFloat())
        val left     = timeW + dayIdx * DAY_COL_W.toFloat()
        val right    = left + DAY_COL_W - 4f
        val subIdx   = state.subjects.indexOfFirst { it.id == slot.subjectId }.takeIf { it >= 0 } ?: 0
        cv.drawRoundRect(RectF(left, top, right, bottom), 8f, 8f,
            Paint().apply { color = blockColors[subIdx % blockColors.size]; alpha = 60 })
        val subName = state.subjects.find { it.id == slot.subjectId }?.name
            ?: state.classes.find { it.id == slot.classId }?.subject ?: "?"
        cv.drawText(subName, left + 4f, top + 18f,
            Paint().apply { color = blockColors[subIdx % blockColors.size]; textSize = 16f; isAntiAlias = true; typeface = Typeface.DEFAULT_BOLD })
    }
    return bmp
}
