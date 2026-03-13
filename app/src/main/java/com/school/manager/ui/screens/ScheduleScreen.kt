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

// ── Helpers for duration arithmetic ───────────────────────────────────────────

/** Add [minutes] to a HH:MM string, returns clamped HH:MM result. */
private fun addMinutesToTime(hhmm: String, minutes: Int): String {
    val base  = if (hhmm.isBlank()) 8 * 60 else timeToMinutes(hhmm)
    val total = (base + minutes).coerceIn(0, 23 * 60 + 59)
    return "%02d:%02d".format(total / 60, total % 60)
}

/** Minutes between two HH:MM strings (≥ 0). */
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
                                expanded = teacherMenuOpen,
                                onDismissRequest = { teacherMenuOpen = false },
                                modifier = Modifier.heightIn(max = 300.dp)
                            ) {
                                DropdownMenuItem(text = { Text("全部") },
                                    onClick = { filterMode = "all"; filterId = 0; teacherMenuOpen = false; menuOpen = false })
                                state.teachers.forEach { t ->
                                    DropdownMenuItem(text = { Text(t.name) },
                                        onClick = { filterMode = "teacher"; filterId = t.id; teacherMenuOpen = false; menuOpen = false })
                                }
                            }
                        }

                        var studentMenuOpen by remember { mutableStateOf(false) }
                        Box {
                            SpeedDialItem("按学生筛选", Icons.Default.Group, FluentPurple,
                                selected = filterMode == "student"
                            ) { studentMenuOpen = true }
                            DropdownMenu(
                                expanded = studentMenuOpen,
                                onDismissRequest = { studentMenuOpen = false },
                                modifier = Modifier.heightIn(max = 300.dp)
                            ) {
                                DropdownMenuItem(text = { Text("全部") },
                                    onClick = { filterMode = "all"; filterId = 0; studentMenuOpen = false; menuOpen = false })
                                state.students.forEach { s ->
                                    DropdownMenuItem(text = { Text(s.name) },
                                        onClick = { filterMode = "student"; filterId = s.id; studentMenuOpen = false; menuOpen = false })
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

    // ── FIX 1: pass slot directly — slot.subjectId is already correct.
    // Previously: val subId = state.subjects.find { it.name == cls?.subject }?.id ?: slot.subjectId
    // That looked up the subject by the CLASS's subject field, which can differ from the slot's subject.
    addAttendanceForSlot?.let { slot ->
        AddAttendanceFromScheduleDialog(
            slot      = slot,
            state     = state,
            vm        = vm,
            onDismiss = { addAttendanceForSlot = null },
            onSave    = { a ->
                vm.addAttendance(a.classId, a.subjectId, a.teacherId, a.date,
                    a.startTime, a.endTime, a.topic, a.status, a.notes, a.attendees)
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

// ── Calendar grid ─────────────────────────────────────────────────────────────

@Composable
private fun CalendarGrid(
    slots: List<Schedule>,
    vm: AppViewModel,
    state: AppState,
    onSlotClick: (Schedule) -> Unit
) {
    val scrollH = rememberScrollState()
    val scrollV = rememberScrollState()

    var scale by remember { mutableFloatStateOf(1f) }
    val transformState = rememberTransformableState { zoomChange, _, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 3f)
    }

    val scaledDayW = (DAY_COL_W * scale).roundToInt()
    val scaledCalH = (CAL_BOX_HEIGHT_DP * scale).roundToInt()
    val surfaceVar = MaterialTheme.colorScheme.surfaceVariant

    Column(modifier = Modifier.fillMaxSize().transformable(state = transformState)) {
        Row(Modifier.fillMaxWidth().height(HEADER_H.dp)) {
            Box(Modifier.width(TIME_COL_W.dp).height(HEADER_H.dp).background(surfaceVar))
            Row(Modifier.weight(1f).horizontalScroll(scrollH)) {
                DAYS.forEach { day ->
                    Box(
                        Modifier.width(scaledDayW.dp).height(HEADER_H.dp).background(surfaceVar),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(day, style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold, color = FluentBlue)
                    }
                }
            }
        }
        HorizontalDivider(color = FluentBorder, thickness = 1.dp)

        Row(Modifier.fillMaxSize()) {
            Box(
                Modifier.width(TIME_COL_W.dp).fillMaxHeight()
                    .verticalScroll(scrollV).background(surfaceVar)
            ) {
                Box(Modifier.height(scaledCalH.dp)) {
                    TIME_TICKS.forEach { (label, yDpRaw) ->
                        val isHour = label.endsWith(":00")
                        val yDp = (yDpRaw * scale + CAL_V_PAD - 6).toInt()
                        Text(
                            text  = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isHour) FluentBlue else FluentMuted,
                            fontWeight = if (isHour) FontWeight.Medium else FontWeight.Normal,
                            modifier = Modifier.align(Alignment.TopEnd).offset(y = yDp.dp).padding(end = 4.dp)
                        )
                    }
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
                                    ?: (slot.classId % SUBJECT_COLORS.size).toInt()
                                val subColor = Color(SUBJECT_COLORS[colorIdx % SUBJECT_COLORS.size])
                                val te       = state.teachers.find { it.id == slot.teacherId }

                                Box(
                                    Modifier
                                        .offset(y = yDp.dp)
                                        .padding(horizontal = 2.dp)
                                        .fillMaxWidth()
                                        .height(hDp.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(subColor.copy(alpha = 0.15f))
                                        .clickable { onSlotClick(slot) }
                                ) {
                                    Column(Modifier.padding(4.dp)) {
                                        Text(sub?.name ?: cl?.subject ?: "?",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold, color = subColor,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (te != null)
                                            Text(te.name, style = MaterialTheme.typography.labelSmall,
                                                color = FluentMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (startStr.isNotBlank() && endStr.isNotBlank())
                                            Text("$startStr-$endStr",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = subColor.copy(alpha = 0.8f),
                                                maxLines = 1, overflow = TextOverflow.Ellipsis)
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

// ── List view ─────────────────────────────────────────────────────────────────

@Composable
private fun ScheduleListView(slots: List<Schedule>, state: AppState, vm: AppViewModel) {
    var editing by remember { mutableStateOf<Schedule?>(null) }

    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val grouped = slots.groupBy { it.day }.toSortedMap()
        grouped.forEach { (day, daySlots) ->
            item {
                Text(DAYS.getOrElse(day - 1) { "?" },
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                    color = FluentBlue, modifier = Modifier.padding(vertical = 4.dp))
            }
            items(daySlots.sortedBy { timeToMinutes(it.resolvedStart()) }) { slot ->
                val sub = state.subjects.find { it.id == slot.subjectId }
                    ?: state.subjects.find { s ->
                        state.classes.find { it.id == slot.classId }?.subject == s.name
                    }
                val cl  = state.classes.find  { it.id == slot.classId }
                val colorIdx = state.subjects.indexOf(sub).takeIf { it >= 0 }
                    ?: (slot.classId % SUBJECT_COLORS.size).toInt()
                val subColor = Color(SUBJECT_COLORS[colorIdx % SUBJECT_COLORS.size])

                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth().clickable { editing = slot }) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(8.dp, 40.dp).clip(RoundedCornerShape(4.dp)).background(subColor))
                        Column(Modifier.weight(1f)) {
                            val subjectName = sub?.name ?: cl?.subject?.takeIf { it.isNotBlank() } ?: "?"
                            val te2 = state.teachers.find { it.id == slot.teacherId }
                            Text(subjectName, fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge)
                            Text("${cl?.name ?: "─"}  ·  ${te2?.name ?: "─"}",
                                style = MaterialTheme.typography.bodySmall, color = FluentMuted)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            val startStr = slot.resolvedStart()
                            val endStr   = slot.resolvedEnd()
                            if (startStr.isNotBlank()) {
                                Text(startStr, style = MaterialTheme.typography.labelMedium,
                                    color = FluentBlue, fontWeight = FontWeight.Bold)
                                if (endStr.isNotBlank())
                                    Text(endStr, style = MaterialTheme.typography.labelSmall, color = FluentMuted)
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }

    editing?.let { slot -> EditScheduleDialog(slot, state, vm, onDismiss = { editing = null }) }
}

// ── Dialogs ───────────────────────────────────────────────────────────────────

@Composable
private fun AddScheduleDialog(state: AppState, vm: AppViewModel, onDismiss: () -> Unit) {
    var className   by remember { mutableStateOf(state.classes.firstOrNull()?.name ?: "") }
    var teacherName by remember { mutableStateOf("") }
    var day         by remember { mutableStateOf(DAYS[0]) }
    var startTime   by remember { mutableStateOf("08:00") }
    var endTime     by remember { mutableStateOf("09:00") }
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
        FormDropdown("班级", className, state.classes.map { it.name }) { className = it }
        if (classSubject.isNotBlank())
            Text("科目：$classSubject", style = MaterialTheme.typography.bodySmall, color = FluentMuted)
        FormDropdown("教师（可选）", teacherName, listOf("") + state.teachers.map { it.name }) { teacherName = it }
        FormDropdown("星期", day, DAYS) { day = it }
        TimeRangeRow(startTime, endTime, { startTime = it }, { endTime = it })
        FormTextField("课程编号", code, { code = it })
    }
}

@Composable
private fun EditScheduleDialog(
    slot: Schedule, state: AppState, vm: AppViewModel, onDismiss: () -> Unit
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
        vm.updateSchedule(slot.copy(classId = cls.id, subjectId = sId,
            teacherId = tId, day = d, startTime = startTime, endTime = endTime, code = code))
        onDismiss()
    }) {
        FormDropdown("班级", className, state.classes.map { it.name }) { className = it }
        FormDropdown("教师（可选）", teacherName, listOf("") + state.teachers.map { it.name }) { teacherName = it }
        FormDropdown("星期", day, DAYS) { day = it }
        TimeRangeRow(startTime, endTime, { startTime = it }, { endTime = it })
        FormTextField("课程编号", code, { code = it })

        if (!confirmDel) {
            OutlinedButton(onClick = { confirmDel = true },
                colors  = ButtonDefaults.outlinedButtonColors(contentColor = FluentRed),
                shape   = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) { Text("删除课程") }
        } else {
            Text("确定删除？", color = FluentRed)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { confirmDel = false }, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)) { Text("取消") }
                Button(onClick = { vm.deleteSchedule(slot.id); onDismiss() },
                    colors = ButtonDefaults.buttonColors(containerColor = FluentRed),
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) { Text("确认") }
            }
        }
    }
}

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

    // Show the subject name for clarity
    val subjectName = state.subjects.find { it.id == slot.subjectId }?.name
        ?: state.classes.find { it.id == slot.classId }?.subject ?: ""

    FluentDialog(title = "添加上课记录", onDismiss = onDismiss, onConfirm = {
        val tId = state.teachers.firstOrNull { it.name == teacherName }?.id ?: slot.teacherId
        val cls = state.classes.find { it.id == slot.classId } ?: return@FluentDialog
        onSave(Attendance(
            id        = System.currentTimeMillis(),
            classId   = cls.id,
            subjectId = slot.subjectId,   // ← use slot's own subjectId, not class.subject
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
        if (subjectName.isNotBlank())
            Text("科目：$subjectName", style = MaterialTheme.typography.bodyMedium,
                color = FluentBlue, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 4.dp))
        FormDropdown("教师", teacherName, listOf("") + state.teachers.map { it.name }) { teacherName = it }
        DatePickerField("日期", date) { date = it }
        TimeRangeRow(startTime, endTime, { startTime = it }, { endTime = it })
        FormDropdown("状态", status, listOf("completed", "cancelled", "pending")) { status = it }
        FormTextField("课题", topic, { topic = it }, "本节课主题")
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
        FormTextField("备注", notes, { notes = it }, "可选")
    }
}

// ── Time picker helpers ───────────────────────────────────────────────────────

/**
 * FIX 2: Start time + duration row.
 *
 * Replaces the old start/end twin-field row.
 * - Shows a single "开始时间" field with clock-dial picker.
 * - Shows preset duration chips: 1小时 / 1.5小时 / 2小时
 * - Shows manual hour + minute number inputs.
 * - Automatically computes and writes back endTime via [onEndChange].
 *
 * [onStartChange] AND [onEndChange] are both called whenever start or duration changes,
 * so callers keep using the same two-callback signature as before.
 */
@Composable
internal fun TimeRangeRow(
    startTime: String,
    endTime: String,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit
) {
    var showStartPicker by remember { mutableStateOf(false) }

    // Duration in minutes — derived once from start/end, then owned locally.
    var durMins by remember {
        mutableIntStateOf(
            if (startTime.isNotBlank() && endTime.isNotBlank())
                minutesBetween(startTime, endTime).takeIf { it > 0 } ?: 60
            else 60
        )
    }

    // Whenever start or duration changes, recompute endTime and notify parent.
    fun pushEnd(start: String, mins: Int) {
        onEndChange(addMinutesToTime(start, mins))
    }

    // Displayed end time (for the hint label)
    val computedEnd = addMinutesToTime(startTime.ifBlank { "08:00" }, durMins)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

        // ── Start time ────────────────────────────────────────────────────────
        OutlinedTextField(
            value = startTime, onValueChange = {
                onStartChange(it)
                pushEnd(it, durMins)
            },
            label    = { Text("开始时间") },
            singleLine = true,
            shape    = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { showStartPicker = true }) {
                    Icon(Icons.Default.Schedule, null, tint = FluentBlue)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = FluentBlue,
                unfocusedBorderColor = FluentBorder
            )
        )

        // ── Duration ──────────────────────────────────────────────────────────
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = FluentBlueLight,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("课时时长", style = MaterialTheme.typography.labelMedium,
                    color = FluentBlue, fontWeight = FontWeight.SemiBold)

                // Preset chips
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

                // Manual input: hours + minutes
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    val h = durMins / 60
                    val m = durMins % 60

                    OutlinedTextField(
                        value = h.toString(),
                        onValueChange = { raw ->
                            val newH = raw.filter { it.isDigit() }.toIntOrNull()?.coerceIn(0, 23) ?: h
                            durMins = newH * 60 + m
                            pushEnd(startTime.ifBlank { "08:00" }, durMins)
                        },
                        label         = { Text("小时") },
                        singleLine    = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape         = RoundedCornerShape(10.dp),
                        modifier      = Modifier.weight(1f),
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = FluentBlue,
                            unfocusedBorderColor = FluentBorder
                        )
                    )

                    Text("时", style = MaterialTheme.typography.bodyMedium, color = FluentMuted)

                    OutlinedTextField(
                        value = m.toString(),
                        onValueChange = { raw ->
                            val newM = raw.filter { it.isDigit() }.toIntOrNull()?.coerceIn(0, 59) ?: m
                            durMins = h * 60 + newM
                            pushEnd(startTime.ifBlank { "08:00" }, durMins)
                        },
                        label         = { Text("分钟") },
                        singleLine    = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape         = RoundedCornerShape(10.dp),
                        modifier      = Modifier.weight(1f),
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = FluentBlue,
                            unfocusedBorderColor = FluentBorder
                        )
                    )

                    Text("分", style = MaterialTheme.typography.bodyMedium, color = FluentMuted)
                }

                // Computed end time hint
                Text(
                    "结束时间：$computedEnd",
                    style = MaterialTheme.typography.labelSmall,
                    color = FluentBlue.copy(alpha = 0.7f)
                )
            }
        }
    }

    if (showStartPicker) {
        TimePickerDialog(
            initial   = startTime,
            onConfirm = { newStart ->
                onStartChange(newStart)
                pushEnd(newStart, durMins)
                showStartPicker = false
            },
            onDismiss = { showStartPicker = false }
        )
    }
}

/**
 * Clock-dial time picker dialog (Material 3 TimePicker, 24h mode).
 * Hour ring first → auto-advances to minute ring → confirm.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val parts = initial.split(":").mapNotNull { it.toIntOrNull() }
    val state = rememberTimePickerState(
        initialHour   = parts.getOrElse(0) { 8 },
        initialMinute = parts.getOrElse(1) { 0 },
        is24Hour      = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text("选择开始时间", fontWeight = FontWeight.Bold) },
        text  = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = state)
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm("%02d:%02d".format(state.hour, state.minute)) },
                shape   = RoundedCornerShape(12.dp),
                colors  = ButtonDefaults.buttonColors(containerColor = FluentBlue)
            ) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = FluentMuted) } }
    )
}

/** Date field with calendar picker dialog. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DatePickerField(label: String, value: String, onChange: (String) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val epochMs: Long? = runCatching {
        val parts = value.split("-")
        val y = parts[0].toInt(); val m = parts[1].toInt(); val d = parts[2].toInt()
        java.util.Calendar.getInstance().apply { set(y, m - 1, d) }.timeInMillis
    }.getOrNull()
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = epochMs)

    OutlinedTextField(
        value         = value,
        onValueChange = onChange,
        label         = { Text(label) },
        shape         = RoundedCornerShape(12.dp),
        modifier      = Modifier.fillMaxWidth(),
        singleLine    = true,
        trailingIcon  = {
            IconButton(onClick = { showPicker = true }) {
                Icon(Icons.Default.DateRange, null, tint = FluentBlue)
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = FluentBlue,
            unfocusedBorderColor = FluentBorder
        )
    )

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

// ── Bitmap export ─────────────────────────────────────────────────────────────

private fun renderScheduleBitmap(
    context: Context,
    slots: List<Schedule>,
    state: AppState,
    title: String
): Bitmap {
    val colW    = 160f
    val rowH    = 60f
    val headerH = 60f
    val timeW   = 72f
    val days    = DAYS
    val totalHours = CAL_END_HOUR - CAL_START_HOUR

    val w = (timeW + colW * days.size).toInt()
    val h = (headerH + rowH * totalHours + 40f).toInt()

    val bmp    = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    val bg = Paint().apply { color = android.graphics.Color.WHITE }
    canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bg)

    val titlePaint = Paint().apply {
        color = android.graphics.Color.parseColor("#1A56DB"); textSize = 28f
        typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true
    }
    canvas.drawText(title, 16f, 36f, titlePaint)

    val headerP = Paint().apply { color = android.graphics.Color.parseColor("#EBF5FF"); isAntiAlias = true }
    canvas.drawRect(0f, 40f, w.toFloat(), headerH, headerP)

    val dayTextP = Paint().apply {
        color = android.graphics.Color.parseColor("#1A56DB"); textSize = 18f
        isAntiAlias = true; textAlign = Paint.Align.CENTER
    }
    days.forEachIndexed { i, d ->
        canvas.drawText(d, timeW + colW * i + colW / 2, headerH - 10f, dayTextP)
    }

    val timeP = Paint().apply {
        color = android.graphics.Color.GRAY; textSize = 14f
        isAntiAlias = true; textAlign = Paint.Align.RIGHT
    }
    val lineP     = Paint().apply { color = android.graphics.Color.parseColor("#E5E7EB"); strokeWidth = 1f }
    val halfLineP = Paint().apply { color = android.graphics.Color.parseColor("#F3F4F6"); strokeWidth = 0.5f }

    for (hour in CAL_START_HOUR..CAL_END_HOUR) {
        val y = headerH + rowH * (hour - CAL_START_HOUR)
        canvas.drawText("%02d:00".format(hour), timeW - 6f, y + 14f, timeP)
        canvas.drawLine(0f, y, w.toFloat(), y, lineP)
        if (hour < CAL_END_HOUR) canvas.drawLine(timeW, y + rowH / 2, w.toFloat(), y + rowH / 2, halfLineP)
    }
    for (col in 0..days.size)
        canvas.drawLine(timeW + colW * col, 40f, timeW + colW * col, h.toFloat(), lineP)

    val blockPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    val textBlockP = Paint().apply { color = android.graphics.Color.WHITE; textSize = 14f; isAntiAlias = true }
    val timeBlockP = Paint().apply { color = android.graphics.Color.WHITE; textSize = 11f; isAntiAlias = true; alpha = 200 }

    slots.groupBy { it.day }.forEach { (day, daySlots) ->
        val x = timeW + colW * (day - 1)
        daySlots.forEach { slot ->
            val startStr = slot.resolvedStart()
            val endStr   = slot.resolvedEnd()
            if (startStr.isBlank()) return@forEach
            val yTop    = headerH + rowH * (timeToMinutes(startStr) - CAL_START_HOUR * 60) / 60f
            val yBottom = if (endStr.isBlank()) yTop + rowH * 0.75f
                          else headerH + rowH * (timeToMinutes(endStr) - CAL_START_HOUR * 60) / 60f
            val sub      = state.subjects.find { it.id == slot.subjectId }
                ?: state.subjects.find { s ->
                    state.classes.find { it.id == slot.classId }?.subject == s.name
                }
            val cl       = state.classes.find { it.id == slot.classId }
            val colorIdx = (state.subjects.indexOf(sub).takeIf { it >= 0 }
                ?: (slot.classId % SUBJECT_COLORS.size).toInt())
            blockPaint.color = SUBJECT_COLORS[colorIdx % SUBJECT_COLORS.size].toInt()
            canvas.drawRoundRect(RectF(x + 2f, yTop + 2f, x + colW - 2f, yBottom - 2f), 6f, 6f, blockPaint)
            canvas.drawText(sub?.name ?: cl?.subject ?: "?", x + 8f, yTop + 20f, textBlockP)
            if (startStr.isNotBlank() && endStr.isNotBlank())
                canvas.drawText("$startStr-$endStr", x + 8f, yTop + 36f, timeBlockP)
        }
    }

    return bmp
}
