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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.school.manager.data.*
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

    Scaffold { inner ->
        Box(modifier = Modifier.padding(inner).fillMaxSize()) {

            // ── Calendar (full screen) ─────────────────────────────────────
            CalendarGrid(slots, vm, state, onSlotClick = { viewSlot = it })

            // ── Secondary filter row (teacher/student names) ───────────────
            if (filterMode != "all") {
                val items = if (filterMode == "teacher")
                    state.teachers.map { it.id to it.name }
                else
                    state.students.map { it.id to it.name }
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 172.dp, bottom = 16.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items.forEach { (id, name) ->
                        FilterChip(
                            selected = filterId == id,
                            onClick  = { filterId = id },
                            label    = { Text(name, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
            }

            // ── Combined action panel (bottom-right) ──────────────────────
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Filter chips
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("all" to "全部", "teacher" to "按教师", "student" to "按学生").forEach { (mode, label) ->
                        FilterChip(
                            selected = filterMode == mode,
                            onClick  = { filterMode = mode; if (mode == "all") filterId = 0 },
                            label    = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                // Action buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallFloatingActionButton(
                        onClick        = { exportJpg() },
                        containerColor = FluentPurple,
                        contentColor   = Color.White,
                        shape          = RoundedCornerShape(12.dp)
                    ) { Icon(Icons.Default.Image, "导出图片") }

                    ExtendedFloatingActionButton(
                        onClick        = { showAdd = true },
                        icon           = { Icon(Icons.Default.Add, "添加") },
                        text           = { Text("添加课程") },
                        containerColor = FluentBlue,
                        contentColor   = Color.White,
                        shape          = RoundedCornerShape(16.dp)
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
        // Subject name
        val evText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = argb; textSize = dp(10f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(subj?.name ?: "?",
            xLeft + dp(4f), yTop + dp(4f) + evText.textSize, evText)
        if (yBot - yTop > dp(30f)) {
            canvas.drawText(cls?.name ?: "",
                xLeft + dp(4f), yTop + dp(4f) + evText.textSize + dp(2f) + clsPaint.textSize, clsPaint)
        }
    }
    return bmp
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
    var subjectName by remember { mutableStateOf("") }
    var teacherName by remember { mutableStateOf("") }
    var day         by remember { mutableStateOf("") }
    var startTime   by remember { mutableStateOf("08:00") }
    var endTime     by remember { mutableStateOf("09:00") }

    FluentDialog(title = "添加课程", onDismiss = onDismiss, onConfirm = {
        val cId    = state.classes.firstOrNull  { it.name == className }?.id  ?: return@FluentDialog
        val sId    = state.subjects.firstOrNull { it.name == subjectName }?.id ?: return@FluentDialog
        if (day.isBlank()) return@FluentDialog
        val tId    = state.teachers.firstOrNull { it.name == teacherName }?.id
        val dayIdx = DAYS.indexOf(day) + 1
        if (dayIdx < 1) return@FluentDialog
        // Validate time format loosely — accept any non-blank value
        val sTime = startTime.trim().ifBlank { "08:00" }
        val eTime = endTime.trim().ifBlank   { "09:00" }
        vm.addSchedule(cId, sId, tId, dayIdx, sTime, eTime)
        onDismiss()
    }) {
        FormDropdown("班级",   className,   state.classes.map   { it.name }) { className   = it }
        FormDropdown("科目",   subjectName, state.subjects.map  { it.name }) { subjectName = it }
        FormDropdown("教师",   teacherName, state.teachers.map  { it.name }) { teacherName = it }
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
    var className   by remember { mutableStateOf(state.classes.firstOrNull  { it.id == slot.classId }?.name ?: "") }
    var subjectName by remember { mutableStateOf(state.subjects.firstOrNull { it.id == slot.subjectId }?.name ?: "") }
    var teacherName by remember { mutableStateOf(state.teachers.firstOrNull { it.id == slot.teacherId }?.name ?: "") }
    var day         by remember { mutableStateOf(DAYS.getOrNull(slot.day - 1) ?: DAYS[0]) }
    var startTime   by remember { mutableStateOf(slot.resolvedStart()) }
    var endTime     by remember { mutableStateOf(slot.resolvedEnd()) }

    FluentDialog(title = "编辑课程", onDismiss = onDismiss, onConfirm = {
        val cId = state.classes.firstOrNull  { it.name == className }?.id  ?: return@FluentDialog
        val sId = state.subjects.firstOrNull { it.name == subjectName }?.id ?: return@FluentDialog
        val tId = state.teachers.firstOrNull { it.name == teacherName }?.id
        vm.updateSchedule(slot.copy(
            classId   = cId,
            subjectId = sId,
            teacherId = tId,
            day       = DAYS.indexOf(day) + 1,
            startTime = startTime.trim().ifBlank { "08:00" },
            endTime   = endTime.trim().ifBlank   { "09:00" }
        ))
        onDismiss()
    }) {
        FormDropdown("班级",   className,   state.classes.map   { it.name }) { className   = it }
        FormDropdown("科目",   subjectName, state.subjects.map  { it.name }) { subjectName = it }
        FormDropdown("教师",   teacherName, state.teachers.map  { it.name }) { teacherName = it }
        FormDropdown("星期",   day,         DAYS)                             { day         = it }
        TimeRangeRow(startTime, endTime,
            onStartChange = { startTime = it },
            onEndChange   = { endTime   = it })
    }
}

/** Free-text start/end time row with preset quick-fill chips */
@Composable
internal fun TimeRangeRow(
    startTime:     String,
    endTime:       String,
    onStartChange: (String) -> Unit,
    onEndChange:   (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value         = startTime,
                onValueChange = onStartChange,
                label         = { Text("开始时间") },
                placeholder   = { Text("08:00", color = FluentMuted) },
                shape         = RoundedCornerShape(12.dp),
                modifier      = Modifier.weight(1f),
                singleLine    = true,
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = FluentBlue,
                    unfocusedBorderColor = FluentBorder)
            )
            Text("─", color = FluentMuted)
            OutlinedTextField(
                value         = endTime,
                onValueChange = onEndChange,
                label         = { Text("结束时间") },
                placeholder   = { Text("09:00", color = FluentMuted) },
                shape         = RoundedCornerShape(12.dp),
                modifier      = Modifier.weight(1f),
                singleLine    = true,
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = FluentBlue,
                    unfocusedBorderColor = FluentBorder)
            )
        }
        Text("快速填入", style = MaterialTheme.typography.labelSmall, color = FluentMuted)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PERIOD_TIMES.zip(PERIOD_END_TIMES).forEach { (s, e) ->
                FilterChip(
                    selected = (startTime == s && endTime == e),
                    onClick  = { onStartChange(s); onEndChange(e) },
                    label    = { Text(s, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }
}
