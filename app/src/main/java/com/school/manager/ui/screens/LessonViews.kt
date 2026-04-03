package com.school.manager.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.school.manager.data.*
import com.school.manager.ui.components.*
import com.school.manager.ui.theme.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields

// ── Week view ─────────────────────────────────────────────────────────────────

@Composable
internal fun WeekView(
    lessons: List<Lesson>, current: LocalDate, onDateChange: (LocalDate) -> Unit,
    state: AppState, progressMap: Map<Long, Pair<Int, Int>>,
    currentView: String, onViewChange: (String) -> Unit,
    dpPerHour: Float,
    onClick: (Lesson) -> Unit
) {
    val monday = current.with(DayOfWeek.MONDAY)
    val days   = (0..6).map { monday.plusDays(it.toLong()) }
    val fmt    = DateTimeFormatter.ofPattern("M/d")
    val totalH = calTotalHeight(dpPerHour)

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier          = Modifier.fillMaxWidth().background(FluentBlue)
                                    .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onDateChange(current.minusWeeks(1)) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ChevronLeft, null, tint = Color.White)
            }
            val weekNum = current.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
            Text(
                text      = "第 $weekNum 周  ${monday.format(fmt)} – ${monday.plusDays(6).format(fmt)}",
                color     = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                maxLines  = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                modifier  = Modifier.weight(1f)
            )
            ViewSwitchIcons(currentView, onViewChange)
            IconButton(onClick = { onDateChange(current.plusWeeks(1)) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ChevronRight, null, tint = Color.White)
            }
        }

        val scrollV = rememberScrollState()
        Row(Modifier.fillMaxSize()) {
            // Time gutter
            Box(Modifier.width(TIME_COL_W.dp).verticalScroll(scrollV)
                .background(MaterialTheme.colorScheme.surfaceVariant)) {
                Box(Modifier.height((totalH + CAL_V_PAD * 2).dp)) {
                    for (h in CAL_START_HOUR..CAL_END_HOUR) {
                        Text("%02d:00".format(h), style = MaterialTheme.typography.labelSmall,
                            color    = FluentMuted,
                            modifier = Modifier
                                .offset(y = ((h - CAL_START_HOUR) * dpPerHour + CAL_V_PAD - 7f).dp)
                                .padding(start = 4.dp))
                    }
                }
            }
            // Day columns
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                days.forEach { day ->
                    val dayLessons = lessons.filter { it.date == day.toString() }
                    val isToday    = day == LocalDate.now()
                    Column(Modifier.width(DAY_COL_W.dp)) {
                        Box(
                            Modifier.fillMaxWidth()
                                .background(if (isToday) FluentBlue.copy(0.15f)
                                            else MaterialTheme.colorScheme.surfaceVariant)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${DAYS.getOrElse(day.dayOfWeek.value - 1) { "?" }}  ${day.format(fmt)}",
                                style      = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                color      = if (isToday) FluentBlue else FluentMuted
                            )
                        }
                        Box(
                            Modifier.width(DAY_COL_W.dp).verticalScroll(scrollV)
                                .height((totalH + CAL_V_PAD * 2).dp)
                                .border(0.5.dp, FluentBorder)
                        ) {
                            // Hour grid lines
                            for (h in CAL_START_HOUR..CAL_END_HOUR) {
                                Box(Modifier
                                    .offset(y = ((h - CAL_START_HOUR) * dpPerHour + CAL_V_PAD).dp)
                                    .fillMaxWidth().height(0.5.dp).background(FluentBorder))
                            }
                            // Lessons
                            dayLessons.forEach { l ->
                                if (l.startTime.isBlank()) return@forEach
                                val color    = statusColor(l.status)
                                val subName  = l.subjectName(state.classes, state.subjects)
                                val clsName  = state.classes.find { it.id == l.classId }?.name ?: ""
                                val (done, total) = progressMap[l.classId] ?: (0 to 0)
                                Box(Modifier
                                    .offset(y = (minuteOffsetDp(l.startTime, dpPerHour) + CAL_V_PAD).dp)
                                    .fillMaxWidth()
                                    .height(durationDp(l.startTime, l.endTime, dpPerHour).coerceAtLeast(42f).dp)
                                    .padding(horizontal = 2.dp, vertical = 1.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(color.copy(alpha = 0.15f))
                                    .border(1.dp, color.copy(0.5f), RoundedCornerShape(6.dp))
                                    .clickable { onClick(l) }
                                    .padding(4.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                        Text(subName,
                                            style      = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold, color = color,
                                            maxLines   = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${l.startTime}–${l.endTime}",
                                            style   = MaterialTheme.typography.labelSmall,
                                            color   = FluentMuted, maxLines = 1)
                                        // 班级名称优先，再显示上课次数
                                        if (clsName.isNotBlank())
                                            Text(clsName,
                                                style    = MaterialTheme.typography.labelSmall,
                                                color    = FluentMuted,
                                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (total > 0)
                                            Text("$done/$total",
                                                style      = MaterialTheme.typography.labelSmall,
                                                color      = color,
                                                fontWeight = FontWeight.SemiBold)
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
internal fun MonthView(
    lessons: List<Lesson>, current: LocalDate, onDateChange: (LocalDate) -> Unit,
    state: AppState, progressMap: Map<Long, Pair<Int, Int>>,
    currentView: String, onViewChange: (String) -> Unit,
    onClick: (Lesson) -> Unit
) {
    val ym     = YearMonth.from(current)
    val first  = ym.atDay(1)
    val offset = first.dayOfWeek.value % 7
    val days   = ym.lengthOfMonth()

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier          = Modifier.fillMaxWidth().background(FluentBlue)
                                    .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onDateChange(current.minusMonths(1)) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ChevronLeft, null, tint = Color.White)
            }
            Text(
                text      = "${current.year}年${current.monthValue}月",
                color     = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                textAlign = TextAlign.Center, modifier = Modifier.weight(1f)
            )
            ViewSwitchIcons(currentView, onViewChange)
            IconButton(onClick = { onDateChange(current.plusMonths(1)) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ChevronRight, null, tint = Color.White)
            }
        }

        Row(Modifier.fillMaxWidth()) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach { d ->
                Text(d, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall,
                    color = FluentMuted, textAlign = TextAlign.Center)
            }
        }
        val rows = ((offset + days) + 6) / 7
        Column(Modifier.fillMaxSize()) {
            repeat(rows) { row ->
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    for (col in 0..6) {
                        val idx = row * 7 + col
                        val day = if (idx < offset || idx >= offset + days) null
                                  else first.plusDays((idx - offset).toLong())
                        Box(
                            Modifier.weight(1f).fillMaxHeight()
                                .border(0.25.dp, FluentBorder)
                                .background(
                                    if (day == LocalDate.now()) FluentBlue.copy(0.08f)
                                    else Color.Transparent)
                                .clickable(enabled = day != null) { day?.let { onDateChange(it) } }
                                .padding(2.dp)
                        ) {
                            if (day != null) {
                                val dayLessons = lessons.filter { it.date == day.toString() }
                                Column {
                                    Text(day.dayOfMonth.toString(),
                                        style      = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (day == LocalDate.now()) FontWeight.Bold
                                                     else FontWeight.Normal,
                                        color      = if (day == LocalDate.now()) FluentBlue
                                                     else FluentMuted)
                                    dayLessons.take(3).forEach { l ->
                                        val color = statusColor(l.status)
                                        Text(l.subjectName(state.classes, state.subjects),
                                            style    = MaterialTheme.typography.labelSmall,
                                            color    = color, maxLines = 1,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(color.copy(0.12f))
                                                .padding(horizontal = 2.dp)
                                                .clickable { onClick(l) })
                                    }
                                    if (dayLessons.size > 3)
                                        Text("+${dayLessons.size - 3}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = FluentMuted)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
