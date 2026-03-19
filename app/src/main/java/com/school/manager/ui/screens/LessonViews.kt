package com.school.manager.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    onClick: (Lesson) -> Unit
) {
    val monday = current.with(DayOfWeek.MONDAY)
    val days   = (0..6).map { monday.plusDays(it.toLong()) }
    val fmt    = DateTimeFormatter.ofPattern("M/d")

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
            Box(Modifier.width(TIME_COL_W.dp).verticalScroll(scrollV)
                .background(MaterialTheme.colorScheme.surfaceVariant)) {
                Box(Modifier.height((CAL_TOTAL_HEIGHT + CAL_V_PAD * 2).dp)) {
                    for (h in CAL_START_HOUR..CAL_END_HOUR) {
                        Text("%02d:00".format(h), style = MaterialTheme.typography.labelSmall,
                            color    = FluentMuted,
                            modifier = Modifier
                                .offset(y = ((h - CAL_START_HOUR) * DP_PER_HOUR + CAL_V_PAD - 7f).dp)
                                .padding(start = 4.dp))
                    }
                }
            }
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
                                .height((CAL_TOTAL_HEIGHT + CAL_V_PAD * 2).dp)
                                .border(0.5.dp, FluentBorder)
                        ) {
                            for (h in CAL_START_HOUR..CAL_END_HOUR) {
                                Box(Modifier
                                    .offset(y = ((h - CAL_START_HOUR) * DP_PER_HOUR + CAL_V_PAD).dp)
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
                                    .padding(4.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                        Text(subName, style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold, color = color, maxLines = 1)
                                        Text("${l.startTime}–${l.endTime}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = FluentMuted, maxLines = 1)
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
        LazyColumn(Modifier.fillMaxSize()) {
            val rows = ((offset + days) + 6) / 7
            items(rows) { row ->
                Row(Modifier.fillMaxWidth()) {
                    for (col in 0..6) {
                        val idx = row * 7 + col
                        val day = if (idx < offset || idx >= offset + days) null
                                  else first.plusDays((idx - offset).toLong())
                        Box(
                            Modifier.weight(1f).height(68.dp)
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
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ── Day view ──────────────────────────────────────────────────────────────────

@Composable
internal fun DayView(
    lessons: List<Lesson>, current: LocalDate, onDateChange: (LocalDate) -> Unit,
    state: AppState, progressMap: Map<Long, Pair<Int, Int>>,
    currentView: String, onViewChange: (String) -> Unit,
    onClick: (Lesson) -> Unit
) {
    val dow        = DAYS.getOrElse(current.dayOfWeek.value - 1) { "" }
    val dayLessons = lessons.filter { it.date == current.toString() }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier          = Modifier.fillMaxWidth().background(FluentBlue)
                                    .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onDateChange(current.minusDays(1)) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ChevronLeft, null, tint = Color.White)
            }
            Text(
                text      = "${current.monthValue}月${current.dayOfMonth}日  周$dow",
                color     = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                textAlign = TextAlign.Center, modifier = Modifier.weight(1f)
            )
            ViewSwitchIcons(currentView, onViewChange)
            IconButton(onClick = { onDateChange(current.plusDays(1)) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ChevronRight, null, tint = Color.White)
            }
        }

        val scrollV = rememberScrollState()
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.width(TIME_COL_W.dp).verticalScroll(scrollV)
                .background(MaterialTheme.colorScheme.surfaceVariant)) {
                Box(Modifier.height((CAL_TOTAL_HEIGHT + CAL_V_PAD * 2).dp)) {
                    for (h in CAL_START_HOUR..CAL_END_HOUR) {
                        Text("%02d:00".format(h), style = MaterialTheme.typography.labelSmall,
                            color    = FluentMuted,
                            modifier = Modifier
                                .offset(y = ((h - CAL_START_HOUR) * DP_PER_HOUR + CAL_V_PAD - 7f).dp)
                                .padding(start = 4.dp))
                    }
                }
            }
            Box(
                Modifier.weight(1f).verticalScroll(scrollV)
                    .height((CAL_TOTAL_HEIGHT + CAL_V_PAD * 2).dp)
                    .border(0.5.dp, FluentBorder)
            ) {
                for (h in CAL_START_HOUR..CAL_END_HOUR) {
                    Box(Modifier
                        .offset(y = ((h - CAL_START_HOUR) * DP_PER_HOUR + CAL_V_PAD).dp)
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
                        .padding(8.dp)
                    ) {
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
                                    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(0.1f)) {
                                        Text("已上 $done/$total 节",
                                            style      = MaterialTheme.typography.labelSmall,
                                            color      = color, fontWeight = FontWeight.SemiBold,
                                            modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
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
internal fun ListView(
    lessons: List<Lesson>, state: AppState,
    progressMap: Map<Long, Pair<Int, Int>>,
    currentView: String, onViewChange: (String) -> Unit,
    onLessonClick: (Lesson) -> Unit,
    onBatchModify: (Long) -> Unit,
    onBatchDelete: (Long) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier              = Modifier.fillMaxWidth().background(FluentBlue)
                                        .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("课次列表",
                color    = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                modifier = Modifier.padding(start = 8.dp))
            ViewSwitchIcons(currentView, onViewChange)
        }

        LazyColumn(
            contentPadding      = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier            = Modifier.weight(1f)
        ) {
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
                        Row(
                            Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
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
                                    Text("批量修改",
                                        style = MaterialTheme.typography.labelSmall, color = FluentBlue)
                                }
                                TextButton(onClick = { onBatchDelete(classId) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                                    Text("批量删除",
                                        style = MaterialTheme.typography.labelSmall, color = FluentRed)
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
}

@Composable
internal fun LessonCard(
    l: Lesson, state: AppState, accentColor: Color, onClick: (Lesson) -> Unit
) {
    FluentCard(accentColor = accentColor, modifier = Modifier.fillMaxWidth(),
        onClick = { onClick(l) }) {
        Row(Modifier.padding(12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(44.dp)) {
                val parts = l.date.split("-")
                Text(parts.getOrElse(2) { "?" },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, color = accentColor)
                Text("${parts.getOrElse(1) { "?" }}月",
                    style = MaterialTheme.typography.labelSmall, color = FluentMuted)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment     = Alignment.CenterVertically) {
                    Text(l.subjectName(state.classes, state.subjects),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, color = accentColor)
                    StatusChip(l.status)
                    if (l.isModified)
                        Surface(shape = RoundedCornerShape(4.dp), color = FluentAmber.copy(0.15f)) {
                            Text("已改", style = MaterialTheme.typography.labelSmall,
                                color    = FluentAmber,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                }
                if (l.topic.isNotBlank())
                    Text("📌 ${l.topic}",
                        style = MaterialTheme.typography.bodyMedium, color = FluentMuted)
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
