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
private val CAL_TOTAL_HOURS          = CAL_END_HOUR - CAL_START_HOUR   // 14
private val CAL_TOTAL_HEIGHT_DP      = CAL_TOTAL_HOURS * DP_PER_HOUR   // 1120
private const val CAL_V_PAD          = 10   // extra dp top+bottom so 08:00/22:00 labels are not clipped
private val CAL_BOX_HEIGHT_DP        = CAL_TOTAL_HEIGHT_DP + CAL_V_PAD * 2

/**
 * Half-hour tick labels from 08:00 to 22:00, each paired with its dp y-offset
 * so any event at HH:mm is placed correctly regardless of value.
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

    // ── View mode: calendar | list ────────────────────────────────────────────
    var viewMode by remember { mutableStateOf("calendar") }  // "calendar" | "list"

    // ── Filter: all | teacher | student ──────────────────────────────────────
    var filterMode by remember { mutableStateOf("all") }
    var filterId   by remember { mutableLongStateOf(0L) }

    val slots = state.schedule.filter { slot ->
        when (filterMode) {
            "teacher" -> slot.teacherId == filterId
            "student" -> vm.student(filterId)?.classIds?.contains(slot.classId) == true
            else      -> true
        }
    }

    // ── JPG export via SAF ────────────────────────────────────────────────────
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

    Scaffold(
        topBar = {
            if (filterMode != "all" && filterId != 0L) {
                TopAppBar(
                    title = { Text(screenTitle, fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = FluentBlue,
                        titleContentColor = Color.White
                    ),
                    actions = {
                        IconButton(onClick = { filterMode = "all"; filterId = 0 }) {
                            Icon(Icons.Default.Close, "清除筛选", tint = Color.White)
                        }
                    }
                )
            }
        }
    ) { inner ->
        Box(modifier = Modifier
            .padding(top = inner.calculateTopPadding(), bottom = inner.calculateBottomPadding())
            .fillMaxSize()) {

            // ── Calendar or List view ──────────────────────────────────────
            if (viewMode == "calendar") {
                CalendarGrid(slots, vm, state, onSlotClick = { viewSlot = it })
            } else {
                ScheduleListView(slots, vm, onSlotClick = { viewSlot = it })
            }

            // ── Speed-dial FAB (bottom-right) ─────────────────────────
            var menuOpen by remember { mutableStateOf(false) }

            // Dim overlay when menu is open
            if (menuOpen) {
                Box(
                    Modifier.fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable(indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        ) { menuOpen = false }
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ── Expanded menu items ───────────────────────────────────
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
                        // ── View mode toggle ──────────────────────────────
                        SpeedDialItem(
                            if (viewMode == "calendar") "切换列表视图" else "切换日历视图",
                            if (viewMode == "calendar") Icons.Default.ViewList else Icons.Default.CalendarViewMonth,
                            FluentBlue
                        ) { viewMode = if (viewMode == "calendar") "list" else "calendar" }

                        HorizontalDivider(color = FluentBorder.copy(alpha = 0.4f))

                        // ── Filter: all ───────────────────────────────────
                        SpeedDialItem("全部课表", Icons.Default.CalendarMonth, FluentBlue,
                            selected = filterMode == "all"
                        ) { filterMode = "all"; filterId = 0; menuOpen = false }

                        // ── Filter: teacher – side popup ─────────────
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
                                DropdownMenuItem(
                                    text    = { Text("全部") },
                                    onClick = { filterMode = "all"; filterId = 0; teacherMenuOpen = false; menuOpen = false }
                                )
                                state.teachers.forEach { t ->
                                    DropdownMenuItem(
                                        text    = { Text(t.name) },
                                        onClick = { filterMode = "teacher"; filterId = t.id; teacherMenuOpen = false; menuOpen = false },
                                        leadingIcon = if (filterId == t.id && filterMode == "teacher") ({
                                            Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = FluentGreen)
                                        }) else null
                                    )
                                }
                            }
                        }

                        // ── Filter: student – side popup ──────────────────
                        var studentMenuOpen by remember { mutableStateOf(false) }
                        Box {
                            SpeedDialItem("按学生筛选", Icons.Default.Group, FluentOrange,
                                selected = filterMode == "student"
                            ) { studentMenuOpen = true }
                            DropdownMenu(
                                expanded         = studentMenuOpen,
                                onDismissRequest = { studentMenuOpen = false },
                                modifier = Modifier.heightIn(max = 300.dp)
                            ) {
                                DropdownMenuItem(
                                    text    = { Text("全部") },
                                    onClick = { filterMode = "all"; filterId = 0; studentMenuOpen = false; menuOpen = false }
                                )
                                state.students.forEach { s ->
                                    DropdownMenuItem(
                                        text    = { Text(s.name) },
                                        onClick = { filterMode = "student"; filterId = s.id; studentMenuOpen = false; menuOpen = false },
                                        leadingIcon = if (filterId == s.id && filterMode == "student") ({
                                            Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = FluentOrange)
                                        }) else null
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = FluentBorder.copy(alpha = 0.4f))

                        // ── Export JPG ────────────────────────────────────
                        SpeedDialItem("导出为图片", Icons.Default.Image, FluentPurple) {
                            menuOpen = false; exportJpg()
                        }

                        // ── Add schedule ──────────────────────────────────
                        SpeedDialItem("添加课程", Icons.Default.Add, FluentBlue) {
                            menuOpen = false; showAdd = true
                        }
                    }
                }

                // ── Main FAB ─────────────────────────────────────────────
                FloatingActionButton(
                    onClick        = { menuOpen = !menuOpen },
                    containerColor = FluentBlue,
                    contentColor   = Color.White,
                    shape          = androidx.compose.foundation.shape.CircleShape
                ) {
                    Icon(
                        if (menuOpen) Icons.Default.Close else Icons.Default.Menu,
                        contentDescription = if (menuOpen) "关闭菜单" else "打开菜单"
                    )
                }
            }
        }
    }

    // ── Dialogs ────────────────────────────────────────────────────────────────
    viewSlot?.let { slot ->
        val sub = vm.subject(slot.subjectId)
        val te  = vm.teacher(slot.teacherId)
        val cl  = vm.schoolClass(slot.classId)
        val lessonCount = state.attendance.count {
            it.subjectId == slot.subjectId && it.classId == slot.classId && it.status == "completed"
        }
        FluentDialog(title = "课程详情", onDismiss = { viewSlot = null }) {
            if (slot.code.isNotBlank()) DetailRow("编号", slot.code)
            DetailRow("班级",   cl?.name  ?: "─")
            DetailRow("科目",   sub?.name ?: "─")
            DetailRow("教师",   te?.name  ?: "─")
            DetailRow("星期",   DAYS.getOrNull(slot.day - 1) ?: "─")
            DetailRow("时间",   "${slot.resolvedStart()} – ${slot.resolvedEnd()}")
            DetailRow("已上课次", "$lessonCount 次")
            Spacer(Modifier.height(8.dp))
            Button(
                onClick  = { addAttendanceForSlot = slot; viewSlot = null },
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = FluentGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("添加上课记录")
            }
            OutlinedButton(
                onClick  = { editSlot = slot; viewSlot = null },
                shape    = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) { Text("✏️ 编辑课程") }
            OutlinedButton(
                onClick  = { vm.deleteSchedule(slot.id); viewSlot = null },
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = FluentRed),
                shape    = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) { Text("删除课程") }
        }
    }

    editSlot?.let { slot ->
        EditScheduleDialog(slot, state, vm, onDismiss = { editSlot = null })
    }

    addAttendanceForSlot?.let { slot ->
        val prefill = Attendance(
            id = 0L, classId = slot.classId, subjectId = slot.subjectId,
            teacherId = slot.teacherId, date = LocalDate.now().toString(),
            startTime = slot.resolvedStart(), endTime = slot.resolvedEnd(),
            topic = "", status = "completed", notes = "", attendees = emptyList()
        )
        AttendanceFormDialog(
            title     = "添加上课记录",
            initial   = prefill,
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

    // Toast
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
        // Sticky day-name header row (horizontal scroll only)
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

        // Scrollable body (both axes, shares scrollH with header)
        Box(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.horizontalScroll(scrollH).verticalScroll(scrollV)) {

                // ── Time-axis column ─────────────────────────────────────────
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

                // ── One column per weekday ───────────────────────────────────
                DAYS.forEachIndexed { di, _ ->
                    val daySlots = slots.filter { it.day == di + 1 }
                    Box(Modifier.width(DAY_COL_W.dp).height(CAL_BOX_HEIGHT_DP.dp)) {

                        // Grid lines at every 30-min tick
                        TIME_TICKS.forEach { (label, yDp) ->
                            val isHour = label.endsWith(":00")
                            HorizontalDivider(
                                color     = if (isHour) FluentBorder
                                            else FluentBorder.copy(alpha = 0.5f),
                                thickness = if (isHour) 0.8.dp else 0.4.dp,
                                modifier  = Modifier
                                    .align(Alignment.TopStart)
                                    .offset(y = (yDp + CAL_V_PAD).dp)
                            )
                        }
                        // Right border
                        Box(
                            Modifier.align(Alignment.TopEnd)
                                .width(0.5.dp)
                                .fillMaxHeight()
                                .background(FluentBorder)
                        )

                        // ── Events — absolute pixel position from any HH:mm ──
                        daySlots.forEach { slot ->
                            val startMins = timeToMinutes(slot.resolvedStart()) - CAL_START_HOUR * 60
                            val endMins   = timeToMinutes(slot.resolvedEnd())   - CAL_START_HOUR * 60
                            val yDp  = (startMins * DP_PER_HOUR / 60f).coerceAtLeast(0f)
                            val hDp  = ((endMins - startMins) * DP_PER_HOUR / 60f).coerceAtLeast(22f)

                            val sub   = vm.subject(slot.subjectId)
                            val cl    = vm.schoolClass(slot.classId)
                            val te    = vm.teacher(slot.teacherId)
                            val color = packedToColor(sub?.color ?: SUBJECT_COLORS[0])

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset(y = (yDp + CAL_V_PAD).dp)
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
                                    // Time range — always shown at top of block
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
                                        Text(cl?.name ?: "",
                                            style    = MaterialTheme.typography.labelSmall,
                                            color    = FluentMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis)
                                    }
                                    if (hDp > 62f && te != null) {
                                        Text(te.name,
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

// ── Bitmap renderer (programmatic Android Canvas) ──────────────────────────────

private fun renderScheduleBitmap(
    context: Context,
    slots: List<Schedule>,
    state: AppState,
    title: String = "课表"
): Bitmap {
    val dens   = context.resources.displayMetrics.density
    val dp     = { v: Float -> v * dens }
    val dpi    = { v: Float -> (v * dens).roundToInt() }

    val timeColPx = dpi(TIME_COL_W.toFloat())
    val dayColPx  = dpi(DAY_COL_W.toFloat())
    val titleH    = dpi(28f)                          // title row height
    val headerPx  = dpi(HEADER_H.toFloat() + 4)
    val vPadPx    = dpi(10f)                          // top+bottom padding matching CAL_V_PAD
    val hrPx      = dp(DP_PER_HOUR.toFloat())
    val bodyH     = (hrPx * CAL_TOTAL_HOURS).roundToInt() + vPadPx * 2
    val totalW    = timeColPx + dayColPx * DAYS.size
    val totalH    = titleH + headerPx + bodyH

    val bmp    = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    canvas.drawColor(android.graphics.Color.WHITE)

    // ── Title row ─────────────────────────────────────────────────────────────
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color     = 0xFF1A56DB.toInt()
        textSize  = dp(14f)
        typeface  = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(title, totalW / 2f, titleH / 2f + titlePaint.textSize * 0.35f, titlePaint)

    // ── Day header row ────────────────────────────────────────────────────────
    val hdrTop = titleH.toFloat()
    val hdrBot = (titleH + headerPx).toFloat()
    val hdrPaint = Paint().apply { color = 0xFF1A56DB.toInt() }
    canvas.drawRect(0f, hdrTop, totalW.toFloat(), hdrBot, hdrPaint)

    // Day name text
    val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color     = android.graphics.Color.WHITE
        textSize  = dp(12f)
        typeface  = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    DAYS.forEachIndexed { di, name ->
        val cx = (timeColPx + di * dayColPx + dayColPx / 2).toFloat()
        canvas.drawText(name, cx, hdrTop + headerPx / 2f + dp(5f), dayPaint)
    }

    // ── Calendar body base Y  (after title + header + top vPad) ─────────────
    val bodyBaseY = titleH + headerPx   // events/labels offset from here + vPadPx

    // Time axis background (light blue tint)
    canvas.drawRect(0f, hdrBot, timeColPx.toFloat(), totalH.toFloat(),
        Paint().apply { color = 0xFFF0F4FF.toInt() })

    // Grid lines + time labels
    val hourLinePaint = Paint().apply { color = 0xFFE5E7EB.toInt(); strokeWidth = dp(0.8f) }
    val halfLinePaint = Paint().apply { color = 0xFFE5E7EB.toInt(); alpha = 100; strokeWidth = dp(0.4f) }
    val hourTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1A56DB.toInt(); textSize = dp(10f); textAlign = Paint.Align.RIGHT
    }
    val halfTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF6B7280.toInt(); textSize = dp(9f); textAlign = Paint.Align.RIGHT
    }

    TIME_TICKS.forEach { (label, yDpFloat) ->
        val y = bodyBaseY + vPadPx + dp(yDpFloat)
        val isHour = label.endsWith(":00")
        canvas.drawLine(timeColPx.toFloat(), y, totalW.toFloat(), y,
            if (isHour) hourLinePaint else halfLinePaint)
        val tp = if (isHour) hourTextPaint else halfTextPaint
        // baseline centred on the grid line
        canvas.drawText(label, timeColPx - dp(4f), y + tp.textSize * 0.35f, tp)
    }

    // Vertical column separators
    val sepPaint = Paint().apply { color = 0xFFE5E7EB.toInt(); strokeWidth = dp(0.8f) }
    for (di in 0..DAYS.size) {
        val x = (timeColPx + di * dayColPx).toFloat()
        canvas.drawLine(x, hdrBot, x, totalH.toFloat(), sepPaint)
    }

    // Events
    val clsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF6B7280.toInt(); textSize = dp(9f) }
    slots.forEach { slot ->
        val startMins = timeToMinutes(slot.resolvedStart()) - CAL_START_HOUR * 60
        val endMins   = timeToMinutes(slot.resolvedEnd())   - CAL_START_HOUR * 60
        val yTop  = bodyBaseY + vPadPx + dp(startMins * DP_PER_HOUR / 60f)
        val yBot  = (bodyBaseY + vPadPx + dp(endMins * DP_PER_HOUR / 60f)).coerceAtLeast(yTop + dp(20f))
        val xLeft  = (timeColPx + (slot.day - 1) * dayColPx) + dp(2f)
        val xRight = (timeColPx + slot.day * dayColPx)        - dp(2f)

        val subj = state.subjects.find { it.id == slot.subjectId }
        val cls  = state.classes.find  { it.id == slot.classId }
        val argb = (subj?.color ?: SUBJECT_COLORS[0]).toInt()
        val rr   = android.graphics.Color.red(argb)
        val gg   = android.graphics.Color.green(argb)
        val bb   = android.graphics.Color.blue(argb)
        val rect = RectF(xLeft, yTop, xRight, yBot)
        val rad  = dp(6f)

        // Background fill
        canvas.drawRoundRect(rect, rad, rad,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.argb(46, rr, gg, bb) })
        // Border
        canvas.drawRoundRect(rect, rad, rad,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.argb(160, rr, gg, bb)
                style = Paint.Style.STROKE; strokeWidth = dp(1f)
            })
        // Time range (always first)
        val timeText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(200, rr, gg, bb); textSize = dp(9f)
        }
        val timeStr = "${slot.resolvedStart()} – ${slot.resolvedEnd()}"
        canvas.drawText(timeStr, xLeft + dp(4f), yTop + dp(3f) + timeText.textSize, timeText)

        // Subject name
        val evText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = argb; textSize = dp(10f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val nameY = yTop + dp(3f) + timeText.textSize + dp(2f) + evText.textSize
        if (nameY < yBot - dp(2f)) {
            canvas.drawText(subj?.name ?: "?", xLeft + dp(4f), nameY, evText)
        }
        if (yBot - yTop > dp(44f)) {
            canvas.drawText(cls?.name ?: "",
                xLeft + dp(4f), nameY + dp(2f) + clsPaint.textSize, clsPaint)
        }
    }
    return bmp
}

// ── Schedule list view ────────────────────────────────────────────────────────

@Composable
private fun ScheduleListView(
    slots: List<Schedule>,
    vm: AppViewModel,
    onSlotClick: (Schedule) -> Unit
) {
    val grouped = DAYS.mapIndexed { di, day ->
        day to slots.filter { it.day == di + 1 }
            .sortedBy { timeToMinutes(it.resolvedStart()) }
    }.filter { (_, list) -> list.isNotEmpty() }

    if (grouped.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无课程", style = MaterialTheme.typography.bodyLarge, color = FluentMuted)
        }
        return
    }

    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        grouped.forEach { (day, daySlots) ->
            item {
                // Day header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        Modifier.size(28.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(FluentBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(day.last().toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White)
                    }
                    Text(day,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = FluentBlue)
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = FluentBlue.copy(alpha = 0.25f))
                    Text("${daySlots.size} 节",
                        style = MaterialTheme.typography.labelSmall,
                        color = FluentMuted)
                }
            }
            daySlots.forEach { slot ->
                item(key = slot.id) {
                    val sub   = vm.subject(slot.subjectId)
                    val cl    = vm.schoolClass(slot.classId)
                    val te    = vm.teacher(slot.teacherId)
                    val color = packedToColor(sub?.color ?: SUBJECT_COLORS[0])
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(color.copy(alpha = 0.08f))
                            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .clickable { onSlotClick(slot) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Colour accent bar
                        Box(
                            Modifier.width(4.dp).height(44.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(color)
                        )
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(sub?.name ?: "未知科目",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold, color = color)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (cl != null) Text(cl.name,
                                    style = MaterialTheme.typography.bodySmall, color = FluentMuted)
                                if (te != null) Text(te.name,
                                    style = MaterialTheme.typography.bodySmall, color = FluentMuted)
                            }
                        }
                        // Time badge
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = color.copy(alpha = 0.15f)
                            ) {
                                Text("${slot.resolvedStart()}",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = color, fontWeight = FontWeight.Bold)
                            }
                            Text("↓ ${slot.resolvedEnd()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = FluentMuted)
                        }
                    }
                }
            }
        }
    }
}

// ── Dialogs & shared sub-composables ──────────────────────────────────────────

@Composable
private fun SingleChoiceRow(
    items: List<Pair<Long, String>>,
    selected: Long,
    onSelect: (Long) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items.forEach { (id, name) ->
            FilterChip(
                selected = selected == id,
                onClick  = { onSelect(id) },
                label    = { Text(name, style = MaterialTheme.typography.labelMedium) }
            )
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

    // Auto-fill class subject + headteacher when class is selected
    val selectedClass = state.classes.firstOrNull { it.name == className }
    val classSubject  = selectedClass?.subject ?: ""
    // Auto-fill teacher from headTeacherId when class changes (if teacher not manually set)
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
                     ?: state.subjects.firstOrNull()?.id ?: 1L
        if (day.isBlank()) return@FluentDialog
        val tId    = state.teachers.firstOrNull { it.name == teacherName }?.id
        val dayIdx = DAYS.indexOf(day) + 1
        if (dayIdx < 1) return@FluentDialog
        vm.addSchedule(cls.id, sId, tId, dayIdx, startTime.trim(), endTime.trim(), code.trim())
        onDismiss()
    }) {
        FormTextField("编号", code, { code = it }, "自动生成，可修改")
        FormDropdown("班级", className, state.classes.map { it.name }) { className = it }
        if (classSubject.isNotBlank()) {
            Text("  科目：$classSubject",
                style = MaterialTheme.typography.bodySmall,
                color = FluentPurple,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
        }
        FormDropdown("教师",   teacherName, state.teachers.map { it.name }) { teacherName = it }
        FormDropdown("星期",   day,         DAYS)                             { day         = it }
        TimeRangeRow(startTime, endTime,
            onStartChange = { startTime = it },
            onEndChange   = { endTime   = it })
    }
}

@Composable
private fun EditScheduleDialog(
    slot: Schedule, state: AppState, vm: AppViewModel, onDismiss: () -> Unit
) {
    var className   by remember { mutableStateOf(state.classes.firstOrNull   { it.id == slot.classId }?.name ?: "") }
    var teacherName by remember { mutableStateOf(state.teachers.firstOrNull  { it.id == slot.teacherId }?.name ?: "") }
    var day         by remember { mutableStateOf(DAYS.getOrNull(slot.day - 1) ?: DAYS[0]) }
    var startTime   by remember { mutableStateOf(slot.resolvedStart()) }
    var endTime     by remember { mutableStateOf(slot.resolvedEnd()) }
    var code        by remember { mutableStateOf(slot.code.ifBlank { genCode("SCH") }) }

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
                style = MaterialTheme.typography.bodySmall,
                color = FluentPurple,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
        }
        FormDropdown("教师",   teacherName, state.teachers.map { it.name }) { teacherName = it }
        FormDropdown("星期",   day,         DAYS)                            { day         = it }
        TimeRangeRow(startTime, endTime,
            onStartChange = { startTime = it },
            onEndChange   = { endTime   = it })
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

    // Parse current value to epoch millis for the picker
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

/** Scroll-wheel time picker row (HH:mm, 24h, no quick-fill chips) */
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

/** Single time wheel picker showing HH and mm columns with snap-scroll */
@Composable
private fun TimeWheelPicker(value: String, onChange: (String) -> Unit) {
    val parts   = value.split(":").mapNotNull { it.toIntOrNull() }
    var selHour = remember(value) { mutableIntStateOf(parts.getOrElse(0) { 8 }.coerceIn(0, 23)) }
    var selMin  = remember(value) { mutableIntStateOf(parts.getOrElse(1) { 0 }.coerceIn(0, 59)) }

    // Emit combined value whenever either column changes
    LaunchedEffect(selHour.intValue, selMin.intValue) {
        onChange("%02d:%02d".format(selHour.intValue, selMin.intValue))
    }

    val itemH    = 36.dp
    val visItems = 3   // visible items (odd so selected is centred)

    @Composable
    fun WheelColumn(count: Int, selected: Int, label: (Int) -> String, onSelect: (Int) -> Unit) {
        val listState = rememberLazyListState(initialFirstVisibleItemIndex = maxOf(0, selected - 1))
        val flingBehavior = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(listState)

        LaunchedEffect(selected) {
            val target = (selected - 1).coerceAtLeast(0)
            listState.animateScrollToItem(target)
        }

        LaunchedEffect(listState.isScrollInProgress) {
            if (!listState.isScrollInProgress) {
                onSelect((listState.firstVisibleItemIndex + 1).coerceIn(0, count - 1))
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
                state       = listState,
                flingBehavior = flingBehavior,
                modifier    = Modifier.fillMaxSize(),
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
            // Highlight band
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
