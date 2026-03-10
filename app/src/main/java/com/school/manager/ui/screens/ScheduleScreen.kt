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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
 * Half-hour tick labels from 08:00 to 22:00, each paired with its dp y-offset.
 */
private val TIME_TICKS: List<Pair<String, Float>> = buildList {
    for (h in CAL_START_HOUR..CAL_END_HOUR) {
        val yDp = (h - CAL_START_HOUR) * DP_PER_HOUR.toFloat()
        add("%02d:00".format(h) to yDp)
        if (h < CAL_END_HOUR) add("%02d:30".format(h) to (yDp + DP_PER_HOUR / 2f))
    }
}

@Composable
fun ScheduleScreen(vm: AppViewModel) {
    val state by vm.state.collectAsState()
    var showAdd              by remember { mutableStateOf(false) }
    var viewSlot             by remember { mutableStateOf<Schedule?>(null) }
    var editSlot             by remember { mutableStateOf<Schedule?>(null) }
    var addAttendanceForSlot by remember { mutableStateOf<Schedule?>(null) }
    var toast                by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    var viewMode   by remember { mutableStateOf("calendar") }
    var filterMode by remember { mutableStateOf("all") }
    var filterId   by remember { mutableLongStateOf(0L) }

    val slots = state.schedule.filter { slot ->
        when (filterMode) {
            "teacher" -> slot.teacherId == filterId
            "student" -> vm.student(filterId)?.classIds?.contains(slot.classId) == true
            else      -> true
        }
    }

    var pendingBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val saveBitmapLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/jpeg")
    ) { uri ->
        if (uri != null && pendingBitmap != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    pendingBitmap!!.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
                toast = "✅ 课表已保存"
            } catch (_: Exception) { toast = "❌ 保存失败，请重试" }
            pendingBitmap?.recycle()
            pendingBitmap = null
        }
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

    var menuOpen by remember { mutableStateOf(false) }

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
                            if (viewMode == "calendar") Icons.Default.ViewList else Icons.Default.CalendarViewMonth,
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
                    }
                }

                FloatingActionButton(
                    onClick        = { menuOpen = !menuOpen },
                    containerColor = if (menuOpen) FluentMuted else FluentBlue,
                    contentColor   = Color.White,
                    shape          = CircleShape
                ) { Icon(if (menuOpen) Icons.Default.Close else Icons.Default.MoreVert, null) }
            }
        }
    ) { inner ->
        Column(modifier = Modifier.padding(inner).fillMaxSize()) {
            if (filterMode != "all") {
                Surface(color = FluentBlue.copy(alpha = 0.1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(screenTitle, style = MaterialTheme.typography.labelLarge, color = FluentBlue, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { filterMode = "all"; filterId = 0 }) { Text("清除筛选") }
                    }
                }
            }
            when (viewMode) {
                "list"     -> ScheduleListView(slots, state, vm, { viewSlot = it })
                "calendar" -> CalendarGrid(slots, vm, state, { viewSlot = it })
            }
        }
    }

    viewSlot?.let { slot ->
        ScheduleDetailDialog(slot, state, vm,
            onDismiss = { viewSlot = null },
            onEdit    = { editSlot = slot; viewSlot = null },
            onDelete  = { vm.deleteSchedule(slot.id); viewSlot = null },
            onAddAttendance = { addAttendanceForSlot = slot; viewSlot = null }
        )
    }
    editSlot?.let { slot ->
        EditScheduleDialog(slot, state, vm, onDismiss = { editSlot = null })
    }
    addAttendanceForSlot?.let { slot ->
        AttendanceFormDialog(
            title = "从课程添加记录",
            initial = null,
            state = state,
            vm = vm,
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

// ── Calendar grid ──────────────────────────────────────────────────────────────

@Composable
private fun CalendarGrid(
    slots: List<Schedule>,
    vm: AppViewModel,
    state: AppState,
    onSlotClick: (Schedule) -> Unit
) {
    val scrollH = rememberScrollState()
    val scrollV = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.horizontalScroll(scrollH)) {
            Box(
                Modifier.width(TIME_COL_W.dp).height(HEADER_H.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            DAYS.forEach { day ->
                Box(
                    Modifier.width(DAY_COL_W.dp).height(HEADER_H.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(day, style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold, color = FluentBlue)
                }
            }
        }
        HorizontalDivider(color = FluentBorder, thickness = 1.dp)

        Box(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.horizontalScroll(scrollH).verticalScroll(scrollV)) {
                Box(
                    Modifier.width(TIME_COL_W.dp).height(CAL_BOX_HEIGHT_DP.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    TIME_TICKS.forEach { (label, yDp) ->
                        val isHour = label.endsWith(":00")
                        Text(
                            text  = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isHour) FluentBlue else FluentMuted,
                            fontWeight = if (isHour) FontWeight.Medium else FontWeight.Normal,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(y = (yDp + CAL_V_PAD - 6).dp)
                                .padding(end = 4.dp)
                        )
                    }
                }

                DAYS.forEachIndexed { di, _ ->
                    val daySlots = slots.filter { it.day == di + 1 }
                    Box(Modifier.width(DAY_COL_W.dp).height(CAL_BOX_HEIGHT_DP.dp)) {
                        TIME_TICKS.forEach { (label, yDp) ->
                            val isHour = label.endsWith(":00")
                            Divider(
                                color     = if (isHour) FluentBorder else FluentBorder.copy(alpha = 0.4f),
                                thickness = if (isHour) 0.8.dp else 0.4.dp,
                                modifier  = Modifier.offset(y = (yDp + CAL_V_PAD).dp)
                            )
                        }
                        daySlots.forEach { slot ->
                            val startStr = slot.resolvedStart()
                            val endStr   = slot.resolvedEnd()
                            val yDp      = minuteOffsetDp(startStr) + CAL_V_PAD
                            val hDp      = durationDp(startStr, endStr).coerceAtLeast(28f)
                            val sub      = state.subjects.find { it.id == slot.subjectId }
                                ?: state.subjects.find { s ->
                                    state.classes.find { it.id == slot.classId }?.subject == s.name
                                }
                            val cl       = state.classes.find { it.id == slot.classId }
                            val colorIdx = state.subjects.indexOf(sub).takeIf { it >= 0 }
                                ?: (slot.subjectId % SUBJECT_COLORS.size).toInt()
                            val color    = subjectColor(colorIdx)

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset(y = yDp.dp)
                                    .fillMaxWidth()
                                    .height(hDp.dp)
                                    .padding(horizontal = 2.dp, vertical = 1.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(color.copy(alpha = 0.18f))
                                    .border(1.dp, color.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                                    .clickable { onSlotClick(slot) }
                                    .padding(horizontal = 4.dp, vertical = 3.dp)
                            ) {
                                Column {
                                    Text("${slot.resolvedStart()} – ${slot.resolvedEnd()}",
                                        style      = MaterialTheme.typography.labelSmall,
                                        color      = color.copy(alpha = 0.85f),
                                        maxLines   = 1,
                                        overflow   = TextOverflow.Ellipsis)
                                    Text(sub?.name ?: "?",
                                        style      = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color      = color,
                                        maxLines   = 1,
                                        overflow   = TextOverflow.Ellipsis)
                                    if (hDp > 42f) {
                                        Text(cl?.name ?: "?",
                                            style    = MaterialTheme.typography.labelSmall,
                                            color    = FluentMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis)
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

// ── List view ──────────────────────────────────────────────────────────────────

@Composable
private fun ScheduleListView(
    slots: List<Schedule>,
    state: AppState,
    vm: AppViewModel,
    onClick: (Schedule) -> Unit
) {
    androidx.compose.foundation.lazy.LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (slots.isEmpty()) item { EmptyState("📅", "暂无课程") }
        else items(slots, key = { it.id }) { slot ->
            val sub   = state.subjects.find { it.id == slot.subjectId }
            val cl    = state.classes.find  { it.id == slot.classId }
            val te    = state.teachers.find { it.id == slot.teacherId }
            val color = subjectColor(state.subjects.indexOf(sub).coerceAtLeast(0))
            Surface(
                shape           = RoundedCornerShape(14.dp),
                shadowElevation = 2.dp,
                color           = MaterialTheme.colorScheme.surface,
                modifier        = Modifier.fillMaxWidth().clickable { onClick(slot) }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        Modifier.size(48.dp).clip(RoundedCornerShape(10.dp))
                            .background(color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(DAYS.getOrElse(slot.day - 1) { "?" },
                            style = MaterialTheme.typography.labelLarge,
                            color = color, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(sub?.name ?: cl?.subject ?: "?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold, color = color)
                        Text("${cl?.name ?: "?"} · ${te?.name ?: "─"}",
                            style = MaterialTheme.typography.bodySmall, color = FluentMuted)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(slot.resolvedStart(),
                            style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
                        Text("↓ ${slot.resolvedEnd()}",
                            style = MaterialTheme.typography.labelSmall, color = FluentMuted)
                    }
                }
            }
        }
    }
}

// ── Dialogs ────────────────────────────────────────────────────────────────────

@Composable
private fun ScheduleDetailDialog(
    slot: Schedule,
    state: AppState,
    vm: AppViewModel,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddAttendance: () -> Unit
) {
    val sub = state.subjects.find { it.id == slot.subjectId }
    val cl  = state.classes.find  { it.id == slot.classId }
    val te  = state.teachers.find { it.id == slot.teacherId }
    FluentDialog(title = "课程详情", onDismiss = onDismiss, onConfirm = onEdit, confirmLabel = "编辑") {
        DetailRow("编号",   slot.code)
        DetailRow("班级",   cl?.name  ?: "?")
        DetailRow("科目",   sub?.name ?: cl?.subject ?: "?")
        DetailRow("教师",   te?.name  ?: "─")
        DetailRow("星期",   DAYS.getOrElse(slot.day - 1) { "?" })
        DetailRow("开始",   slot.resolvedStart())
        DetailRow("结束",   slot.resolvedEnd())
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp)) {
            OutlinedButton(onClick = onAddAttendance, shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)) { Text("➕ 添加记录") }
            OutlinedButton(onClick = onDelete, shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = FluentRed),
                modifier = Modifier.weight(1f)) { Text("删除") }
        }
    }
}

@Composable
private fun AddScheduleDialog(state: AppState, vm: AppViewModel, onDismiss: () -> Unit) {
    var className   by remember { mutableStateOf("") }
    var teacherName by remember { mutableStateOf("") }
    var day         by remember { mutableStateOf("") }
    var startTime   by remember { mutableStateOf("08:00") }
    var endTime     by remember { mutableStateOf("09:00") }
    var code        by remember { mutableStateOf(genCode("SCH")) }

    val selectedClass = state.classes.firstOrNull { it.name == className }
    val classSubject  = selectedClass?.subject ?: ""
    LaunchedEffect(className) {
        val htId = selectedClass?.headTeacherId
        if (htId != null) {
            val htName = state.teachers.firstOrNull { it.id == htId }?.name
            if (htName != null) teacherName = htName
        }
    }

    FluentDialog(title = "添加课程", onDismiss = onDismiss, onConfirm = {
        val cls    = state.classes.firstOrNull { it.name == className } ?: return@FluentDialog
        val sId    = state.subjects.firstOrNull { it.name == cls.subject }?.id
                     ?: state.subjects.firstOrNull()?.id ?: return@FluentDialog
        val tId    = state.teachers.firstOrNull { it.name == teacherName }?.id
        val dayIdx = DAYS.indexOf(day) + 1
        if (dayIdx < 1) return@FluentDialog
        vm.addSchedule(cls.id, sId, tId, dayIdx, startTime.trim(), endTime.trim(), code.trim())
        onDismiss()
    }) {
        FormTextField("编号", code, { code = it }, "如: SCH001")
        FormDropdown("班级",  className, state.classes.map { it.name })  { className   = it }
        if (classSubject.isNotBlank()) {
            Text("  科目：$classSubject",
                style = MaterialTheme.typography.bodySmall, color = FluentPurple,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
        }
        FormDropdown("教师",  teacherName, state.teachers.map { it.name }) { teacherName = it }
        FormDropdown("星期",  day, DAYS)                                    { day         = it }
        TimeRangeRow(startTime, endTime, { startTime = it }, { endTime = it })
    }
}

@Composable
private fun EditScheduleDialog(slot: Schedule, state: AppState, vm: AppViewModel, onDismiss: () -> Unit) {
    var className   by remember { mutableStateOf(state.classes.firstOrNull  { it.id == slot.classId   }?.name ?: "") }
    var teacherName by remember { mutableStateOf(state.teachers.firstOrNull { it.id == slot.teacherId }?.name ?: "") }
    var day         by remember { mutableStateOf(DAYS.getOrElse(slot.day - 1) { "" }) }
    var startTime   by remember { mutableStateOf(slot.resolvedStart()) }
    var endTime     by remember { mutableStateOf(slot.resolvedEnd()) }
    var code        by remember { mutableStateOf(slot.code) }

    val selectedClass = state.classes.firstOrNull { it.name == className }
    val classSubject  = selectedClass?.subject ?: ""

    FluentDialog(title = "编辑课程", onDismiss = onDismiss, onConfirm = {
        val cls = state.classes.firstOrNull { it.name == className } ?: return@FluentDialog
        val sId = state.subjects.firstOrNull { it.name == cls.subject }?.id ?: slot.subjectId
        val tId = state.teachers.firstOrNull { it.name == teacherName }?.id
        vm.updateSchedule(slot.copy(
            classId   = cls.id,
            subjectId = sId,
            teacherId = tId,
            day       = DAYS.indexOf(day) + 1,
            startTime = startTime.trim(),
            endTime   = endTime.trim(),
            code      = code.trim().ifBlank { slot.code }
        ))
        onDismiss()
    }) {
        FormTextField("编号", code, { code = it }, "可修改")
        FormDropdown("班级",   className,   state.classes.map  { it.name }) { className   = it }
        if (classSubject.isNotBlank()) {
            Text("  科目：$classSubject",
                style = MaterialTheme.typography.bodySmall, color = FluentPurple,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
        }
        FormDropdown("教师",   teacherName, state.teachers.map { it.name }) { teacherName = it }
        FormDropdown("星期",   day,         DAYS)                            { day         = it }
        TimeRangeRow(startTime, endTime, { startTime = it }, { endTime = it })
    }
}

@Composable
private fun SpeedDialItem(
    label:    String,
    icon:     androidx.compose.ui.graphics.vector.ImageVector,
    color:    Color,
    selected: Boolean = false,
    onClick:  () -> Unit
) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape  = RoundedCornerShape(8.dp),
            color  = if (selected) color.copy(alpha = 0.15f)
                     else MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.clickable { onClick() }
        ) {
            Text(
                label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style    = MaterialTheme.typography.labelMedium,
                color    = if (selected) color else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
        SmallFloatingActionButton(
            onClick        = onClick,
            containerColor = if (selected) color else color.copy(alpha = 0.85f),
            contentColor   = Color.White,
            shape          = CircleShape
        ) { Icon(icon, null, modifier = Modifier.size(18.dp)) }
    }
}

/** Date field with calendar picker dialog */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun DatePickerField(label: String, value: String, onChange: (String) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }

    val epochMs: Long? = remember(value) {
        runCatching {
            val ld = java.time.LocalDate.parse(value)
            ld.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        }.getOrNull()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value         = value,
            onValueChange = onChange,
            label         = { Text(label) },
            placeholder   = { Text("yyyy-MM-dd", color = FluentMuted) },
            shape         = RoundedCornerShape(12.dp),
            modifier      = Modifier.weight(1f),
            singleLine    = true,
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = FluentBlue,
                unfocusedBorderColor = FluentBorder)
        )
        IconButton(onClick = { showPicker = true }) {
            Icon(Icons.Default.CalendarMonth, "选择日期", tint = FluentBlue)
        }
    }

    if (showPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = epochMs)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showPicker = false
                    state.selectedDateMillis?.let { ms ->
                        val ld = java.time.Instant.ofEpochMilli(ms)
                            .atZone(java.time.ZoneOffset.UTC).toLocalDate()
                        onChange(ld.toString())
                    }
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

/** Scroll-wheel time picker row (HH:mm, 24h) */
@Composable
internal fun TimeRangeRow(
    startTime:     String,
    endTime:       String,
    onStartChange: (String) -> Unit,
    onEndChange:   (String) -> Unit
) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.Top
    ) {
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally,
               verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("开始时间", style = MaterialTheme.typography.labelSmall, color = FluentMuted)
            TimeWheelPicker(startTime, onStartChange)
        }
        Text("─", color = FluentMuted, modifier = Modifier.padding(top = 32.dp))
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally,
               verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("结束时间", style = MaterialTheme.typography.labelSmall, color = FluentMuted)
            TimeWheelPicker(endTime, onEndChange)
        }
    }
}

/**
 * Single time wheel picker showing HH and mm columns with snap-scroll.
 *
 * FIX: contentPadding = PaddingValues(vertical = itemH) pushes item[N] to the
 * vertical centre of the viewport when the LazyColumn is snapped to index N.
 * Therefore the centred item == firstVisibleItemIndex (not +1), and we must
 * initialise / animate-scroll to `selected` (not `selected - 1`).
 */
@Composable
private fun TimeWheelPicker(value: String, onChange: (String) -> Unit) {
    val parts   = value.split(":").mapNotNull { it.toIntOrNull() }
    var selHour = remember(value) { mutableIntStateOf(parts.getOrElse(0) { 8 }.coerceIn(0, 23)) }
    var selMin  = remember(value) { mutableIntStateOf(parts.getOrElse(1) { 0 }.coerceIn(0, 59)) }

    LaunchedEffect(selHour.intValue, selMin.intValue) {
        onChange("%02d:%02d".format(selHour.intValue, selMin.intValue))
    }

    val itemH    = 36.dp
    val visItems = 3

    @Composable
    fun WheelColumn(count: Int, selected: Int, label: (Int) -> String, onSelect: (Int) -> Unit) {
        // ── FIX: scroll to `selected`, not `selected - 1` ──────────────────
        val listState     = rememberLazyListState(initialFirstVisibleItemIndex = selected.coerceAtLeast(0))
        val flingBehavior = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(listState)

        // Keep scroll position in sync when `selected` changes programmatically
        LaunchedEffect(selected) {
            listState.animateScrollToItem(selected.coerceAtLeast(0))
        }

        // ── FIX: centred item == firstVisibleItemIndex (no +1) ─────────────
        LaunchedEffect(listState.isScrollInProgress) {
            if (!listState.isScrollInProgress) {
                onSelect(listState.firstVisibleItemIndex.coerceIn(0, count - 1))
            }
        }

        Box(
            Modifier
                .width(48.dp)
                .height(itemH * visItems)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            LazyColumn(
                state         = listState,
                flingBehavior = flingBehavior,
                modifier      = Modifier.fillMaxSize(),
                // Top/bottom padding equal to one item height centres item[0]
                // in the viewport when scrolled to index 0.
                contentPadding = PaddingValues(vertical = itemH)
            ) {
                items(count) { i ->
                    Box(
                        Modifier.fillMaxWidth().height(itemH),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label(i),
                            style      = if (i == selected) MaterialTheme.typography.titleMedium
                                         else MaterialTheme.typography.bodyMedium,
                            fontWeight = if (i == selected) FontWeight.Bold else FontWeight.Normal,
                            color      = if (i == selected) FluentBlue
                                         else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        )
                    }
                }
            }
            // Highlight band for the centre slot
            Box(
                Modifier.align(Alignment.Center)
                    .fillMaxWidth().height(itemH)
                    .background(FluentBlue.copy(alpha = 0.08f))
            )
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        WheelColumn(24, selHour.intValue, { "%02d".format(it) }) { selHour.intValue = it }
        Text(":", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = FluentBlue)
        WheelColumn(60, selMin.intValue,  { "%02d".format(it) }) { selMin.intValue = it }
    }
}

// ── Bitmap export ──────────────────────────────────────────────────────────────

private fun renderScheduleBitmap(
    context: Context,
    slots: List<Schedule>,
    state: AppState,
    title: String
): Bitmap {
    fun dp(v: Float) = (v * context.resources.displayMetrics.density).roundToInt().toFloat()

    val timeColPx = dp(TIME_COL_W.toFloat()).toInt()
    val dayColPx  = dp(DAY_COL_W.toFloat()).toInt()
    val headerPx  = dp(HEADER_H.toFloat()).toInt()
    val titleH    = dp(32f).toInt()
    val vPadPx    = dp(CAL_V_PAD.toFloat()).toInt()
    val hourPx    = dp(DP_PER_HOUR.toFloat()).toInt()
    val totalH    = titleH + headerPx + (CAL_END_HOUR - CAL_START_HOUR) * hourPx + vPadPx * 2
    val totalW    = timeColPx + DAYS.size * dayColPx

    val bmp    = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    canvas.drawColor(android.graphics.Color.WHITE)

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color     = 0xFF1A56DB.toInt()
        textSize  = dp(14f)
        typeface  = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(title, totalW / 2f, titleH / 2f + titlePaint.textSize * 0.35f, titlePaint)

    val hdrPaint = Paint().apply { color = 0xFF1A56DB.toInt() }
    val hdrTop = titleH.toFloat(); val hdrBot = (titleH + headerPx).toFloat()
    canvas.drawRect(0f, hdrTop, totalW.toFloat(), hdrBot, hdrPaint)

    val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE; textSize = dp(12f)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER
    }
    DAYS.forEachIndexed { di, name ->
        val cx = (timeColPx + di * dayColPx + dayColPx / 2).toFloat()
        canvas.drawText(name, cx, hdrTop + headerPx / 2f + dp(5f), dayPaint)
    }

    canvas.drawRect(0f, hdrBot, timeColPx.toFloat(), totalH.toFloat(),
        Paint().apply { color = 0xFFF0F4FF.toInt() })

    val hourLinePaint = Paint().apply { color = 0xFFE5E7EB.toInt(); strokeWidth = dp(0.8f) }
    val halfLinePaint = Paint().apply { color = 0xFFE5E7EB.toInt(); alpha = 100; strokeWidth = dp(0.5f) }
    val timePaint     = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF6B7280.toInt(); textSize = dp(9f); textAlign = Paint.Align.RIGHT
    }

    for (h in CAL_START_HOUR..CAL_END_HOUR) {
        val y = (hdrBot + vPadPx + (h - CAL_START_HOUR) * hourPx).toFloat()
        canvas.drawLine(timeColPx.toFloat(), y, totalW.toFloat(), y, hourLinePaint)
        canvas.drawText("%02d:00".format(h), (timeColPx - dp(4f)), y + dp(3f), timePaint)
        if (h < CAL_END_HOUR) {
            val yh = y + hourPx / 2f
            canvas.drawLine(timeColPx.toFloat(), yh, totalW.toFloat(), yh, halfLinePaint)
        }
    }

    val bgPaints = SUBJECT_COLORS.map { c -> Paint().apply { color = (c.toInt() and 0x00FFFFFF) or 0x30000000 } }
    val txtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = dp(9f); color = android.graphics.Color.BLACK }

    slots.forEach { slot ->
        val di    = slot.day - 1
        val start = slot.resolvedStart(); val end = slot.resolvedEnd()
        val startMin = timeToMinutes(start) - CAL_START_HOUR * 60
        val endMin   = timeToMinutes(end)   - CAL_START_HOUR * 60
        val top      = hdrBot + vPadPx + startMin * hourPx / 60f
        val bot      = hdrBot + vPadPx + endMin   * hourPx / 60f
        val left     = (timeColPx + di * dayColPx + dp(2f))
        val right    = (timeColPx + (di + 1) * dayColPx - dp(2f))

        val sub      = state.subjects.find { it.id == slot.subjectId }
        val colorIdx = state.subjects.indexOf(sub).coerceAtLeast(0) % SUBJECT_COLORS.size
        canvas.drawRoundRect(RectF(left, top, right, bot), dp(4f), dp(4f), bgPaints[colorIdx])

        txtPaint.color = when (colorIdx) {
            0 -> 0xFF1A56DB.toInt(); 1 -> 0xFF0E9F6E.toInt(); 2 -> 0xFF7E3AF2.toInt()
            3 -> 0xFFFF5A1F.toInt(); 4 -> 0xFFE3A008.toInt(); 5 -> 0xFFE11D48.toInt()
            6 -> 0xFF0891B2.toInt(); else -> 0xFF65A30D.toInt()
        }
        val subName = sub?.name ?: state.classes.find { it.id == slot.classId }?.subject ?: "?"
        canvas.drawText(subName, left + dp(3f), top + dp(10f), txtPaint)
        val cl = state.classes.find { it.id == slot.classId }
        if ((bot - top) > dp(20f))
            canvas.drawText(cl?.name ?: "?", left + dp(3f), top + dp(20f), Paint(txtPaint).apply { color = 0xFF6B7280.toInt() })
    }

    return bmp
}
