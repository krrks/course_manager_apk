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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.school.manager.data.*
import com.school.manager.data.genCode
import com.school.manager.ui.components.*
import com.school.manager.ui.theme.*
import com.school.manager.viewmodel.AppViewModel
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

// ── Calendar layout constants ──────────────────────────────────────────────────
private const val DP_PER_HOUR        = 80
private const val TIME_COL_W         = 52
private const val DAY_COL_W          = 110
private const val HEADER_H           = 36
private val CAL_TOTAL_HOURS          = CAL_END_HOUR - CAL_START_HOUR
private val CAL_TOTAL_HEIGHT_DP      = CAL_TOTAL_HOURS * DP_PER_HOUR
private const val CAL_V_PAD          = 10
private val CAL_BOX_HEIGHT_DP        = CAL_TOTAL_HEIGHT_DP + CAL_V_PAD * 2

/**
 * Half-hour tick labels from CAL_START_HOUR to CAL_END_HOUR,
 * each paired with its dp y-offset (unscaled).
 */
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

// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ScheduleScreen(vm: AppViewModel, onOpenDrawer: () -> Unit = {}) {
    val state by vm.state.collectAsState()

    var viewMode    by remember { mutableStateOf("calendar") }
    var filterMode  by remember { mutableStateOf("all") }
    var filterId    by remember { mutableLongStateOf(0L) }
    var showAdd     by remember { mutableStateOf(false) }
    var menuOpen    by remember { mutableStateOf(false) }
    var toast       by remember { mutableStateOf<String?>(null) }

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
                            if (viewMode == "calendar") Icons.AutoMirrored.Filled.ViewList else Icons.Default.CalendarViewMonth,
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
                                expanded         = studentMenuOpen,
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
                        SpeedDialItem("添加课程", Icons.Default.Add, FluentBlue) { showAdd = true; menuOpen = false }

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

            if (viewMode == "calendar") {
                CalendarGrid(slots, vm, state) { slot ->
                    addAttendanceForSlot = slot
                }
            } else {
                ScheduleListView(slots, state, vm)
            }
        }
    }

    addAttendanceForSlot?.let { slot ->
        val cls = state.classes.find { it.id == slot.classId }
        val subId = state.subjects.find { it.name == cls?.subject }?.id ?: slot.subjectId
        AddAttendanceFromScheduleDialog(
            slot    = slot.copy(subjectId = subId),
            state   = state,
            vm      = vm,
            onDismiss = { addAttendanceForSlot = null },
            onSave = { a ->
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .transformable(state = transformState)
    ) {
        // ── Day-header row ────────────────────────────────────────────────────
        Row(Modifier.fillMaxWidth().height(HEADER_H.dp)) {
            Box(
                Modifier
                    .width(TIME_COL_W.dp)
                    .height(HEADER_H.dp)
                    .background(surfaceVar)
            )
            Row(
                Modifier
                    .weight(1f)
                    .horizontalScroll(scrollH)
            ) {
                DAYS.forEach { day ->
                    Box(
                        Modifier
                            .width(scaledDayW.dp)
                            .height(HEADER_H.dp)
                            .background(surfaceVar),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            day,
                            style      = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color      = FluentBlue
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = FluentBorder, thickness = 1.dp)

        // ── Body ──────────────────────────────────────────────────────────────
        Row(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .width(TIME_COL_W.dp)
                    .fillMaxHeight()
                    .verticalScroll(scrollV)
                    .background(surfaceVar)
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
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(y = yDp.dp)
                                .padding(end = 4.dp)
                        )
                    }
                }
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .horizontalScroll(scrollH)
                    .verticalScroll(scrollV)
            ) {
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

                                Box(
                                    Modifier
                                        .offset(y = yDp.dp)
                                        .height(hDp.dp)
                                        .fillMaxWidth()
                                        .padding(horizontal = 2.dp, vertical = 1.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(subColor.copy(alpha = 0.15f))
                                        .border(0.5.dp, subColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                        .clickable { onSlotClick(slot) }
                                        .padding(4.dp)
                                ) {
                                    val subjectName = sub?.name
                                        ?: cl?.subject?.takeIf { it.isNotBlank() }
                                        ?: "?"
                                    val te = state.teachers.find { it.id == slot.teacherId }
                                    Column {
                                        Text(
                                            subjectName,
                                            style      = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color      = subColor,
                                            maxLines   = 1,
                                            overflow   = TextOverflow.Ellipsis
                                        )
                                        if (cl != null) {
                                            Text(
                                                cl.name,
                                                style   = MaterialTheme.typography.labelSmall,
                                                color   = FluentMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (te != null) {
                                            Text(
                                                te.name,
                                                style   = MaterialTheme.typography.labelSmall,
                                                color   = FluentMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        // FIX: show start/end time on the event card
                                        // 修复：在课程块上显示具体起止时间
                                        if (startStr.isNotBlank() && endStr.isNotBlank()) {
                                            Text(
                                                "$startStr-$endStr",
                                                style   = MaterialTheme.typography.labelSmall,
                                                color   = subColor.copy(alpha = 0.8f),
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
    }
}

// ── List view ─────────────────────────────────────────────────────────────────

@Composable
private fun ScheduleListView(
    slots: List<Schedule>,
    state: AppState,
    vm: AppViewModel
) {
    var editing by remember { mutableStateOf<Schedule?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val grouped = slots.groupBy { it.day }.toSortedMap()
        grouped.forEach { (day, daySlots) ->
            item {
                Text(
                    DAYS.getOrElse(day - 1) { "?" },
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color      = FluentBlue,
                    modifier   = Modifier.padding(vertical = 4.dp)
                )
            }
            items(daySlots.sortedBy { timeToMinutes(it.resolvedStart()) }) { slot ->
                val sub = state.subjects.find { it.id == slot.subjectId }
                    ?: state.subjects.find { s ->
                        state.classes.find { it.id == slot.classId }?.subject == s.name
                    }
                val cl  = state.classes.find  { it.id == slot.classId }
                val te  = state.teachers.find { it.id == slot.teacherId }
                val colorIdx = state.subjects.indexOf(sub).takeIf { it >= 0 }
                    ?: (slot.classId % SUBJECT_COLORS.size).toInt()
                val subColor = Color(SUBJECT_COLORS[colorIdx % SUBJECT_COLORS.size])

                Surface(
                    shape           = RoundedCornerShape(12.dp),
                    color           = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier        = Modifier.fillMaxWidth().clickable { editing = slot }
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            Modifier
                                .size(8.dp, 40.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(subColor)
                        )
                        Column(Modifier.weight(1f)) {
                            val subjectName = sub?.name
                                ?: cl?.subject?.takeIf { it.isNotBlank() }
                                ?: "?"
                            val te2 = state.teachers.find { it.id == slot.teacherId }
                            Text(subjectName, fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "${cl?.name ?: "─"}  ·  ${te2?.name ?: "─"}",
                                style = MaterialTheme.typography.bodySmall, color = FluentMuted
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            val startStr = slot.resolvedStart()
                            val endStr   = slot.resolvedEnd()
                            if (startStr.isNotBlank()) {
                                Text(startStr, style = MaterialTheme.typography.labelMedium,
                                    color = FluentBlue, fontWeight = FontWeight.Bold)
                                if (endStr.isNotBlank())
                                    Text(endStr, style = MaterialTheme.typography.labelSmall,
                                        color = FluentMuted)
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }

    editing?.let { slot ->
        EditScheduleDialog(slot, state, vm, onDismiss = { editing = null })
    }
}

// ── Dialogs ───────────────────────────────────────────────────────────────────

@Composable
private fun AddScheduleDialog(state: AppState, vm: AppViewModel, onDismiss: () -> Unit) {
    var className   by remember { mutableStateOf(state.classes.firstOrNull()?.name ?: "") }
    var teacherName by remember { mutableStateOf("") }
    var day         by remember { mutableStateOf(DAYS[0]) }
    var startTime   by remember { mutableStateOf("08:00") }
    var endTime     by remember { mutableStateOf("08:45") }
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
        FormDropdown("教师（可选）", teacherName,
            listOf("") + state.teachers.map { it.name }) { teacherName = it }
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
        FormDropdown("教师（可选）", teacherName,
            listOf("") + state.teachers.map { it.name }) { teacherName = it }
        FormDropdown("星期", day, DAYS) { day = it }
        TimeRangeRow(startTime, endTime, { startTime = it }, { endTime = it })
        FormTextField("课程编号", code, { code = it })

        if (!confirmDel) {
            OutlinedButton(
                onClick = { confirmDel = true },
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
    val checkedIds  = remember { mutableStateListOf<Long>().also { it.addAll(allStudents.map { s -> s.id }) } }

    FluentDialog(title = "添加上课记录", onDismiss = onDismiss, onConfirm = {
        val tId = state.teachers.firstOrNull { it.name == teacherName }?.id ?: slot.teacherId
        onSave(Attendance(0, slot.classId, slot.subjectId, tId, date, 0,
            startTime, endTime, topic, status, notes, checkedIds.toList()))
    }) {
        DatePickerField("日期", date) { date = it }
        FormDropdown("教师", teacherName, state.teachers.map { it.name }) { teacherName = it }
        TimeRangeRow(startTime, endTime, { startTime = it }, { endTime = it })
        FormTextField("课题", topic, { topic = it }, "本次课程主题")
        FormDropdown("状态", status, listOf("completed","cancelled","pending")) { status = it }
        FormTextField("备注", notes, { notes = it })
        if (allStudents.isNotEmpty()) {
            Text("出勤学生（点击切换）",
                style = MaterialTheme.typography.labelMedium, color = FluentMuted,
                modifier = Modifier.padding(top = 4.dp))
            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                allStudents.forEach { s ->
                    val present = checkedIds.contains(s.id)
                    FilterChip(
                        selected = present,
                        onClick  = {
                            if (present) checkedIds.remove(s.id) else checkedIds.add(s.id)
                        },
                        label = { Text(s.name) },
                        leadingIcon = if (present) ({
                            Icon(Icons.Default.Check, null,
                                Modifier.size(FilterChipDefaults.IconSize))
                        }) else null
                    )
                }
            }
        }
    }
}

// ── Time picker helpers ───────────────────────────────────────────────────────

@Composable
internal fun TimeRangeRow(
    startTime: String,
    endTime: String,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit
) {
    var showStart by remember { mutableStateOf(false) }
    var showEnd   by remember { mutableStateOf(false) }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = startTime, onValueChange = onStartChange,
            label = { Text("开始") }, singleLine = true, shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f),
            trailingIcon = { IconButton(onClick = { showStart = true }) {
                Icon(Icons.Default.Schedule, null, tint = FluentBlue) } },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FluentBlue, unfocusedBorderColor = FluentBorder)
        )
        OutlinedTextField(
            value = endTime, onValueChange = onEndChange,
            label = { Text("结束") }, singleLine = true, shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f),
            trailingIcon = { IconButton(onClick = { showEnd = true }) {
                Icon(Icons.Default.Schedule, null, tint = FluentBlue) } },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FluentBlue, unfocusedBorderColor = FluentBorder)
        )
    }

    if (showStart) TimePickerDialog(startTime, { onStartChange(it); showStart = false }) { showStart = false }
    if (showEnd)   TimePickerDialog(endTime,   { onEndChange(it);   showEnd   = false }) { showEnd   = false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val parts   = initial.split(":").mapNotNull { it.toIntOrNull() }
    val selHour = remember { mutableIntStateOf(parts.getOrElse(0) { 8 }) }
    val selMin  = remember { mutableIntStateOf(parts.getOrElse(1) { 0 }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape  = RoundedCornerShape(20.dp),
        title  = { Text("选择时间", fontWeight = FontWeight.Bold) },
        text   = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimeWheelPicker(selHour, selMin)
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm("%02d:%02d".format(selHour.intValue, selMin.intValue)) },
                shape  = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FluentBlue)
            ) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = FluentMuted) } }
    )
}

/**
 * Wheel picker for hours (0-23) and minutes (0-59).
 *
 * FIX: the selected value now updates automatically when the user stops
 * scrolling — no need to tap an item to confirm selection.
 * 修复：滚动停止时自动吸附并选中最近的时间项，无需二次点击确认。
 */
@Composable
private fun TimeWheelPicker(selHour: MutableIntState, selMin: MutableIntState) {
    val itemH = 40.dp

    @Composable
    fun WheelColumn(
        count: Int,
        selected: Int,
        label: (Int) -> String,
        onSelect: (Int) -> Unit
    ) {
        val listState = rememberLazyListState(
            initialFirstVisibleItemIndex = selected.coerceIn(0, maxOf(0, count - 1))
        )

        // When the user lifts their finger and scrolling stops, snap to the
        // nearest item and notify the caller. A guard flag prevents the
        // programmatic animateScrollToItem from triggering another select cycle.
        // FIX: 用像素中点判断最近 item，不再依赖 offset > 20 的固定阈值。
        // 滚动停止后，若 firstVisibleItemScrollOffset >= 半个 itemH，
        // 则选下一个（fi+1），否则选当前（fi）。
        val itemHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) { itemH.toPx() }
        var suppressNext by remember { mutableStateOf(false) }
        LaunchedEffect(listState) {
            snapshotFlow { listState.isScrollInProgress }
                .distinctUntilChanged()
                .filter { !it }
                .collect {
                    if (suppressNext) { suppressNext = false; return@collect }
                    val fi      = listState.firstVisibleItemIndex
                    val offset  = listState.firstVisibleItemScrollOffset
                    val snapped = (if (offset * 2 >= itemHeightPx) fi + 1 else fi)
                        .coerceIn(0, count - 1)
                    onSelect(snapped)
                    suppressNext = true
                    listState.animateScrollToItem(snapped)
                }
        }

        Box(Modifier.height(itemH * 5).width(64.dp)) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                // 2 empty padding items top + count real items + 2 empty padding bottom
                items(count + 4) { i ->
                    val idx = i - 2
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(itemH)
                            .then(
                                if (idx in 0 until count)
                                    Modifier.clickable { onSelect(idx) }
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (idx in 0 until count) label(idx) else "",
                            style      = if (idx == selected) MaterialTheme.typography.titleMedium
                                         else MaterialTheme.typography.bodyMedium,
                            fontWeight = if (idx == selected) FontWeight.Bold else FontWeight.Normal,
                            color      = if (idx == selected) FluentBlue
                                         else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        )
                    }
                }
            }
            // Selection highlight bar
            Box(
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(itemH)
                    .background(FluentBlue.copy(alpha = 0.08f))
            )
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        WheelColumn(24, selHour.intValue, { "%02d".format(it) }) { selHour.intValue = it }
        Text(":", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
            color = FluentBlue)
        WheelColumn(60, selMin.intValue,  { "%02d".format(it) }) { selMin.intValue  = it }
    }
}

/** Date field with calendar picker dialog */
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
        trailingIcon  = { IconButton(onClick = { showPicker = true }) {
            Icon(Icons.Default.DateRange, null, tint = FluentBlue) } },
        colors        = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = FluentBlue,
            unfocusedBorderColor = FluentBorder
        )
    )

    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val ms = pickerState.selectedDateMillis
                    if (ms != null) {
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
//
// FIX 1: Time axis now uses CAL_START_HOUR..CAL_END_HOUR (08:00–22:00)
//         instead of the fixed PERIOD_TIMES list — all evening slots are visible.
//         修复1：时间轴改为连续时间段(08:00–22:00)，19:00后的内容不再被截断。
//
// FIX 2: Each course block now displays the start–end time string.
//         修复2：每个课程块上显示具体起止时间。

private fun renderScheduleBitmap(
    context: Context,
    slots: List<Schedule>,
    state: AppState,
    title: String
): Bitmap {
    val colW    = 160f
    val rowH    = 60f          // height per hour
    val headerH = 60f
    val timeW   = 72f
    val days    = DAYS

    // Full continuous hour range: CAL_START_HOUR (8) to CAL_END_HOUR (22)
    val totalHours = CAL_END_HOUR - CAL_START_HOUR  // 14 hours

    val w = (timeW + colW * days.size).toInt()
    val h = (headerH + rowH * totalHours + 40f).toInt()

    val bmp    = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    // Background
    val bg = Paint().apply { color = android.graphics.Color.WHITE }
    canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bg)

    // Title
    val titlePaint = Paint().apply {
        color       = android.graphics.Color.parseColor("#1A56DB")
        textSize    = 28f
        typeface    = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }
    canvas.drawText(title, 16f, 36f, titlePaint)

    // Header background
    val headerP = Paint().apply {
        color       = android.graphics.Color.parseColor("#EBF5FF")
        isAntiAlias = true
    }
    canvas.drawRect(0f, 40f, w.toFloat(), headerH, headerP)

    // Day name headers
    val dayTextP = Paint().apply {
        color       = android.graphics.Color.parseColor("#1A56DB")
        textSize    = 18f
        isAntiAlias = true
        textAlign   = Paint.Align.CENTER
    }
    days.forEachIndexed { i, d ->
        canvas.drawText(d, timeW + colW * i + colW / 2, headerH - 10f, dayTextP)
    }

    // Hour labels + horizontal grid lines
    val timeP = Paint().apply {
        color       = android.graphics.Color.GRAY
        textSize    = 14f
        isAntiAlias = true
        textAlign   = Paint.Align.RIGHT
    }
    val lineP = Paint().apply {
        color       = android.graphics.Color.parseColor("#E5E7EB")
        strokeWidth = 1f
    }
    val halfLineP = Paint().apply {
        color       = android.graphics.Color.parseColor("#F3F4F6")
        strokeWidth = 0.5f
    }
    for (hour in CAL_START_HOUR..CAL_END_HOUR) {
        val y = headerH + rowH * (hour - CAL_START_HOUR)
        canvas.drawText("%02d:00".format(hour), timeW - 6f, y + 14f, timeP)
        canvas.drawLine(0f, y, w.toFloat(), y, lineP)
        // Half-hour mark
        if (hour < CAL_END_HOUR) {
            val yHalf = y + rowH / 2
            canvas.drawLine(timeW, yHalf, w.toFloat(), yHalf, halfLineP)
        }
    }

    // Vertical column dividers
    for (col in 0..days.size) {
        canvas.drawLine(timeW + colW * col, 40f, timeW + colW * col, h.toFloat(), lineP)
    }

    // Course blocks
    val cellP    = Paint().apply { isAntiAlias = true }
    val cellText = Paint().apply {
        isAntiAlias = true; textSize = 14f
        color = android.graphics.Color.WHITE
    }
    val subText = Paint().apply {
        isAntiAlias = true; textSize = 11f
        color = android.graphics.Color.parseColor("#CCFFFFFF")
    }
    val timeText = Paint().apply {
        isAntiAlias = true; textSize = 11f
        color = android.graphics.Color.parseColor("#EEFFFFFF")
        typeface = Typeface.DEFAULT_BOLD
    }

    slots.forEach { slot ->
        val dayIdx   = slot.day - 1
        val startStr = slot.resolvedStart()
        val endStr   = slot.resolvedEnd()
        if (startStr.isBlank()) return@forEach

        val startMins = timeToMinutes(startStr) - CAL_START_HOUR * 60
        val endMins   = timeToMinutes(endStr.ifBlank { startStr }).let {
            if (endStr.isBlank()) startMins + 45 else it - CAL_START_HOUR * 60
        }
        if (startMins < 0) return@forEach

        val topY    = headerH + (startMins / 60f) * rowH
        val bottomY = (headerH + (endMins / 60f) * rowH).coerceAtLeast(topY + 28f)

        val sub      = state.subjects.find { it.id == slot.subjectId }
        val cl       = state.classes.find { it.id == slot.classId }
        val colorIdx = state.subjects.indexOf(sub).takeIf { it >= 0 }
            ?: (slot.classId % SUBJECT_COLORS.size).toInt()
        cellP.color = (SUBJECT_COLORS[colorIdx % SUBJECT_COLORS.size] or 0xFF000000).toInt()

        val left   = timeW + colW * dayIdx + 3f
        val right  = timeW + colW * (dayIdx + 1) - 3f
        canvas.drawRoundRect(RectF(left, topY + 2f, right, bottomY - 2f), 8f, 8f, cellP)

        val subjectName = sub?.name ?: cl?.subject?.takeIf { it.isNotBlank() } ?: "?"
        var textY = topY + 16f
        canvas.drawText(subjectName.take(6), left + 6f, textY, cellText)
        textY += 14f
        cl?.let {
            canvas.drawText(it.name.take(8), left + 6f, textY, subText)
            textY += 13f
        }
        // FIX: draw start–end time on the block
        // 修复：在课程块上绘制起止时间
        if (endStr.isNotBlank()) {
            canvas.drawText("$startStr–$endStr", left + 6f, textY, timeText)
        }
    }

    return bmp
}
