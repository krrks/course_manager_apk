package com.school.manager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import com.school.manager.data.CAL_END_HOUR
import com.school.manager.data.CAL_START_HOUR
import com.school.manager.data.timeToMinutes
import com.school.manager.ui.components.ColorChip
import com.school.manager.ui.theme.*

// ── Calendar layout constants ─────────────────────────────────────────────────
internal const val DP_PER_HOUR = 80f
internal const val DP_PER_MIN  = DP_PER_HOUR / 60f
internal const val TIME_COL_W  = 52
internal const val DAY_COL_W   = 120
internal const val CAL_V_PAD   = 10f
internal val CAL_TOTAL_HEIGHT  get() = (CAL_END_HOUR - CAL_START_HOUR) * DP_PER_HOUR

internal fun minuteOffsetDp(hhmm: String): Float {
    if (hhmm.isBlank()) return 0f
    return ((timeToMinutes(hhmm) - CAL_START_HOUR * 60) * DP_PER_MIN).coerceAtLeast(0f)
}

internal fun durationDp(start: String, end: String): Float {
    if (start.isBlank() || end.isBlank()) return DP_PER_HOUR / 2f
    return (timeToMinutes(end) - timeToMinutes(start)).coerceAtLeast(10) * DP_PER_MIN
}

internal fun addMinutesToTime(hhmm: String, minutes: Int): String {
    val base  = if (hhmm.isBlank()) 8 * 60 else timeToMinutes(hhmm)
    val total = (base + minutes).coerceIn(0, 23 * 60 + 59)
    return "%02d:%02d".format(total / 60, total % 60)
}

internal fun minutesBetween(start: String, end: String): Int =
    if (start.isBlank() || end.isBlank()) 120
    else (timeToMinutes(end) - timeToMinutes(start)).coerceAtLeast(0)

// ── Status helpers ────────────────────────────────────────────────────────────
internal fun statusColor(status: String): Color = when (status) {
    "completed" -> FluentGreen
    "absent"    -> FluentAmber
    "cancelled" -> FluentRed
    "postponed" -> FluentMuted
    else        -> FluentBlue
}

internal fun statusLabel(status: String): String = when (status) {
    "completed" -> "✅ 已完成"
    "absent"    -> "⚠️ 缺席"
    "cancelled" -> "❌ 已取消"
    "postponed" -> "⏸ 已延期"
    else        -> "⏳ 待上课"
}

@Composable
internal fun StatusChip(status: String) = ColorChip(statusLabel(status), statusColor(status))

// ── View-switch icon row ──────────────────────────────────────────────────────
@Composable
internal fun ViewSwitchIcons(currentView: String, onViewChange: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        listOf(
            "week"  to Icons.Default.ViewWeek,
            "month" to Icons.Default.CalendarMonth,
            "day"   to Icons.Default.Today,
            "list"  to Icons.Default.ViewList
        ).forEach { (v, icon) ->
            IconButton(onClick = { onViewChange(v) }, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector        = icon,
                    contentDescription = null,
                    tint               = if (v == currentView) Color.White
                                         else Color.White.copy(alpha = 0.40f),
                    modifier           = Modifier.size(17.dp)
                )
            }
        }
    }
}

// ── Time / date pickers ───────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StartTimeCompact(startTime: String, onStartChange: (String) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    OutlinedTextField(
        value         = startTime,
        onValueChange = onStartChange,
        label         = { Text("时间") },
        singleLine    = true,
        shape         = RoundedCornerShape(12.dp),
        modifier      = Modifier.fillMaxWidth(),
        trailingIcon  = {
            IconButton(onClick = { showPicker = true }) {
                Icon(Icons.Default.Schedule, null, tint = FluentBlue, modifier = Modifier.size(18.dp))
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = FluentBlue, unfocusedBorderColor = FluentBorder)
    )
    if (showPicker) LessonTimePickerDialog(
        initial   = startTime,
        onConfirm = { onStartChange(it); showPicker = false },
        onDismiss = { showPicker = false }
    )
}

@Composable
internal fun DurationChipsCompact(
    startTime: String, endTime: String, onEndChange: (String) -> Unit
) {
    var durMins by remember {
        mutableIntStateOf(minutesBetween(startTime, endTime).takeIf { it > 0 } ?: 120)
    }
    fun push(m: Int) { onEndChange(addMinutesToTime(startTime.ifBlank { "08:00" }, m)) }
    val h = durMins / 60
    val m = durMins % 60

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        modifier              = Modifier.fillMaxWidth()
    ) {
        Text("时长", style = MaterialTheme.typography.labelMedium,
            color = FluentBlue, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(4.dp))
        OutlinedTextField(
            value         = h.toString(),
            onValueChange = { raw ->
                val newH = raw.filter { it.isDigit() }.toIntOrNull()?.coerceIn(0, 23) ?: h
                durMins = newH * 60 + m; push(durMins)
            },
            label           = { Text("时", style = MaterialTheme.typography.labelSmall) },
            singleLine      = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape           = RoundedCornerShape(8.dp),
            modifier        = Modifier.width(52.dp),
            textStyle       = MaterialTheme.typography.bodySmall,
            colors          = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FluentBlue, unfocusedBorderColor = FluentBorder)
        )
        OutlinedTextField(
            value         = "%02d".format(m),
            onValueChange = { raw ->
                val newM = raw.filter { it.isDigit() }.toIntOrNull()?.coerceIn(0, 59) ?: m
                durMins = h * 60 + newM; push(durMins)
            },
            label           = { Text("分", style = MaterialTheme.typography.labelSmall) },
            singleLine      = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape           = RoundedCornerShape(8.dp),
            modifier        = Modifier.width(52.dp),
            textStyle       = MaterialTheme.typography.bodySmall,
            colors          = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FluentBlue, unfocusedBorderColor = FluentBorder)
        )
        Spacer(Modifier.width(2.dp))
        FilledTonalIconButton(
            onClick  = { durMins = (durMins - 10).coerceAtLeast(10); push(durMins) },
            modifier = Modifier.size(32.dp),
            shape    = RoundedCornerShape(8.dp),
            colors   = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = FluentBorder, contentColor = FluentMuted)
        ) { Icon(Icons.Default.Remove, "-10分钟", Modifier.size(16.dp)) }
        FilledTonalIconButton(
            onClick  = { durMins = (durMins + 10).coerceAtMost(23 * 60); push(durMins) },
            modifier = Modifier.size(32.dp),
            shape    = RoundedCornerShape(8.dp),
            colors   = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = FluentBlueLight, contentColor = FluentBlue)
        ) { Icon(Icons.Default.Add, "+10分钟", Modifier.size(16.dp)) }
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

    OutlinedTextField(
        value         = value, onValueChange = onChange, label = { Text(label) },
        shape         = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
        singleLine    = true,
        trailingIcon  = {
            IconButton(onClick = { showPicker = true }) {
                Icon(Icons.Default.DateRange, null, tint = FluentBlue)
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = FluentBlue, unfocusedBorderColor = FluentBorder)
    )
    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton    = {
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
private fun LessonTimePickerDialog(
    initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit
) {
    val parts = initial.split(":").mapNotNull { it.toIntOrNull() }
    val ts    = rememberTimePickerState(
        initialHour   = parts.getOrElse(0) { 8 },
        initialMinute = parts.getOrElse(1) { 0 },
        is24Hour      = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        shape            = RoundedCornerShape(20.dp),
        title            = { Text("选择时间", fontWeight = FontWeight.Bold) },
        text             = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = ts)
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm("%02d:%02d".format(ts.hour, ts.minute)) },
                shape   = RoundedCornerShape(12.dp),
                colors  = ButtonDefaults.buttonColors(containerColor = FluentBlue)
            ) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = FluentMuted) } }
    )
}
