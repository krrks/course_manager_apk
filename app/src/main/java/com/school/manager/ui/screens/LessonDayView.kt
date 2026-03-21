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
import java.time.LocalDate

// ── Day view ──────────────────────────────────────────────────────────────────

@Composable
internal fun DayView(
    lessons: List<Lesson>, current: LocalDate, onDateChange: (LocalDate) -> Unit,
    state: AppState, progressMap: Map<Long, Pair<Int, Int>>,
    currentView: String, onViewChange: (String) -> Unit,
    dpPerHour: Float,
    onClick: (Lesson) -> Unit
) {
    val dow        = DAYS.getOrElse(current.dayOfWeek.value - 1) { "" }
    val dayLessons = lessons.filter { it.date == current.toString() }
    val totalH     = calTotalHeight(dpPerHour)

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
            // Event area
            Box(
                Modifier.weight(1f).verticalScroll(scrollV)
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
                    val color   = statusColor(l.status)
                    val cls     = state.classes.find { it.id == l.classId }
                    val teacher = state.teachers.find { it.id == l.effectiveTeacherId(state.classes) }
                    val (done, total) = progressMap[l.classId] ?: (0 to 0)
                    Box(Modifier
                        .offset(y = (minuteOffsetDp(l.startTime, dpPerHour) + CAL_V_PAD).dp)
                        .fillMaxWidth()
                        .height(durationDp(l.startTime, l.endTime, dpPerHour).coerceAtLeast(56f).dp)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color.copy(0.15f))
                        .border(1.dp, color.copy(0.5f), RoundedCornerShape(8.dp))
                        .clickable { onClick(l) }
                        .padding(8.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            // Row 1: subject · class
                            Text("${l.subjectName(state.classes, state.subjects)} · ${cls?.name ?: "?"}",
                                style      = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold, color = color,
                                maxLines   = 1, overflow = TextOverflow.Ellipsis)
                            // Row 2: time + teacher
                            Text("${l.startTime} – ${l.endTime}  👩‍🏫${teacher?.name ?: "─"}",
                                style   = MaterialTheme.typography.labelSmall,
                                color   = FluentMuted, maxLines = 1)
                            // Row 3: status + progress
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment     = Alignment.CenterVertically) {
                                StatusChip(l.status)
                                if (total > 0)
                                    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(0.1f)) {
                                        Text("已上 $done/$total 节",
                                            style      = MaterialTheme.typography.labelSmall,
                                            color      = color, fontWeight = FontWeight.SemiBold,
                                            modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                    }
                            }
                            // Row 4: topic
                            if (l.topic.isNotBlank())
                                Text("📌 ${l.topic}",
                                    style   = MaterialTheme.typography.labelSmall,
                                    color   = FluentMuted, maxLines = 1,
                                    overflow = TextOverflow.Ellipsis)
                            // Row 5: attendees count
                            if (l.attendees.isNotEmpty())
                                Text("👥 ${l.attendees.size} 人出勤",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = FluentGreen)
                            // Row 6: notes preview
                            if (l.notes.isNotBlank())
                                Text("💬 ${l.notes}",
                                    style    = MaterialTheme.typography.labelSmall,
                                    color    = FluentMuted,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}
