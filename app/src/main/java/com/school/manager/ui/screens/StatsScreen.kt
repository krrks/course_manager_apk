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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.school.manager.data.*
import com.school.manager.ui.components.*
import com.school.manager.ui.theme.*
import com.school.manager.viewmodel.AppViewModel

@Composable
fun StatsScreen(vm: AppViewModel) {
    val state by vm.state.collectAsState()

    var dim        by remember { mutableStateOf("teacher") }
    var fTeachers  by remember { mutableStateOf(emptySet<Long>()) }
    var fGrades    by remember { mutableStateOf(emptySet<String>()) }
    var fCountMin  by remember { mutableStateOf("") }
    var fCountMax  by remember { mutableStateOf("") }
    var showFilters by remember { mutableStateOf(true) }

    val minC = fCountMin.toIntOrNull() ?: Int.MIN_VALUE
    val maxC = fCountMax.toIntOrNull() ?: Int.MAX_VALUE

    fun classPassCount(cid: Long): Boolean {
        val cl = vm.schoolClass(cid) ?: return true
        return cl.count >= minC && cl.count <= maxC
    }

    val completed = state.attendance.filter { it.status == "completed" }

    // Filtered scope for summary cards
    val scopedRecs = completed.filter { r ->
        (fTeachers.isEmpty() || fTeachers.contains(r.teacherId)) &&
        (fGrades.isEmpty() || vm.schoolClass(r.classId)?.grade?.let { fGrades.contains(it) } == true) &&
        classPassCount(r.classId)
    }
    val totalLessons   = scopedRecs.size
    val totalHours     = String.format("%.1f", totalLessons * PERIOD_HOURS)
    val activeTeachers = scopedRecs.mapNotNull { it.teacherId }.toSet().size
    val activeStudents = scopedRecs.flatMap { it.attendees }.toSet().size

    // Teacher rows
    val teacherRows = state.teachers
        .filter { t -> fTeachers.isEmpty() || fTeachers.contains(t.id) }
        .map { t ->
            val recs = completed.filter { r ->
                r.teacherId == t.id &&
                (fGrades.isEmpty() || vm.schoolClass(r.classId)?.grade?.let { fGrades.contains(it) } == true) &&
                classPassCount(r.classId)
            }
            Triple(t, recs.size, (recs.size * PERIOD_HOURS))
        }
        .sortedByDescending { it.second }
    val maxTeacher = teacherRows.maxOfOrNull { it.second } ?: 1

    data class GradeRowData(val grade: String, val classCount: Int, val studentCount: Int, val enrollCount: Int, val lessonCount: Int, val hours: Float)
    // Grade rows
    val gradeRows: List<GradeRowData> = GRADES
        .filter { g -> fGrades.isEmpty() || fGrades.contains(g) }
        .mapNotNull { g ->
            val classes = state.classes.filter { c -> c.grade == g && c.count >= minC && c.count <= maxC &&
                (fTeachers.isEmpty() || state.schedule.any { sc -> sc.classId == c.id && fTeachers.contains(sc.teacherId) }) }
            if (classes.isEmpty()) return@mapNotNull null
            val recs = completed.filter { r ->
                classes.any { it.id == r.classId } &&
                (fTeachers.isEmpty() || fTeachers.contains(r.teacherId))
            }
            val studentCount = state.students.filter { it.grade == g }.size
            GradeRowData(g, classes.size, studentCount, classes.sumOf { it.count }, recs.size, recs.size * PERIOD_HOURS)
        }
    val maxGrade = gradeRows.maxOfOrNull { it.lessonCount } ?: 1

    // Student rows
    val studentRows = state.students
        .filter { s ->
            (fGrades.isEmpty() || fGrades.contains(s.grade)) &&
            (fCountMin.isBlank() && fCountMax.isBlank() || s.classIds.any { classPassCount(it) })
        }
        .map { s ->
            val recs = completed.filter { r ->
                r.attendees.contains(s.id) &&
                (fTeachers.isEmpty() || fTeachers.contains(r.teacherId)) &&
                classPassCount(r.classId) &&
                (fGrades.isEmpty() || vm.schoolClass(r.classId)?.grade?.let { fGrades.contains(it) } == true)
            }
            val subjects = recs.mapNotNull { vm.subject(it.subjectId) }.distinctBy { it.id }
            Pair(s, Triple(recs.size, recs.size * PERIOD_HOURS, subjects))
        }
        .sortedByDescending { it.second.first }
    val maxStudent = studentRows.maxOfOrNull { it.second.first } ?: 1

    val hasFilter = fTeachers.isNotEmpty() || fGrades.isNotEmpty() || fCountMin.isNotBlank() || fCountMax.isNotBlank()

    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Summary cards
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryCard("📋", "完成课次", "$totalLessons 节", FluentBlue, Modifier.weight(1f))
                SummaryCard("⏱️", "总课时", "$totalHours h", FluentGreen, Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryCard("👩‍🏫", "参与教师", "$activeTeachers 位", FluentPurple, Modifier.weight(1f))
                SummaryCard("🎒", "参与学生", "$activeStudents 人", FluentOrange, Modifier.weight(1f))
            }
        }

        // Filter panel
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("🔍 筛选条件（同时生效）", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row {
                            if (hasFilter) TextButton(onClick = { fTeachers = emptySet(); fGrades = emptySet(); fCountMin = ""; fCountMax = "" }) {
                                Text("清除", color = FluentRed, style = MaterialTheme.typography.labelMedium)
                            }
                            IconButton(onClick = { showFilters = !showFilters }) {
                                Icon(if (showFilters) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                            }
                        }
                    }
                    if (showFilters) {
                        // Teacher chips
                        Text("教师", style = MaterialTheme.typography.labelMedium, color = FluentMuted)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            state.teachers.forEach { t ->
                                val on = fTeachers.contains(t.id)
                                FilterChip(selected = on, onClick = { fTeachers = if (on) fTeachers - t.id else fTeachers + t.id }, label = { Text(t.name) })
                            }
                        }
                        // Grade chips
                        Text("年级", style = MaterialTheme.typography.labelMedium, color = FluentMuted)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            GRADES.forEach { g ->
                                val on = fGrades.contains(g)
                                FilterChip(selected = on, onClick = { fGrades = if (on) fGrades - g else fGrades + g }, label = { Text(g) })
                            }
                        }
                        // Count range
                        Text("班级编制人数范围", style = MaterialTheme.typography.labelMedium, color = FluentMuted)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(value = fCountMin, onValueChange = { fCountMin = it }, label = { Text("最小") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(10.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FluentBlue, unfocusedBorderColor = FluentBorder))
                            Text("─", color = FluentMuted)
                            OutlinedTextField(value = fCountMax, onValueChange = { fCountMax = it }, label = { Text("最大") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(10.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FluentBlue, unfocusedBorderColor = FluentBorder))
                        }
                    }
                }
            }
        }

        // Dimension tabs
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("teacher" to "按教师", "grade" to "按年级", "student" to "按学生").forEach { (key, label) ->
                    FilterChip(selected = dim == key, onClick = { dim = key }, label = { Text(label) }, modifier = Modifier.weight(1f))
                }
            }
        }

        // Teacher table
        if (dim == "teacher") {
            if (teacherRows.isEmpty()) {
                item { EmptyState("👩‍🏫", "无符合条件的数据") }
            } else {
                items(teacherRows) { (t, lessons, hours) ->
                    StatCard(
                        avatar = { AvatarCircle(t.name, FluentGreen, 40.dp) },
                        title  = t.name,
                        sub1   = "完成 $lessons 节 · ${String.format("%.1f", hours)} 小时",
                        progress = lessons.toFloat() / maxTeacher.coerceAtLeast(1),
                        color    = FluentGreen,
                        chips    = t.subjectIds.mapNotNull { vm.subject(it) }.map { it.name to packedToColor(it.color) }
                    )
                }
            }
        }

        // Grade table
        if (dim == "grade") {
            if (gradeRows.isEmpty()) {
                item { EmptyState("🏫", "无符合条件的数据") }
            } else {
                items(gradeRows) { r ->
                    StatCard(
                        avatar = { Box(Modifier.size(40.dp).background(FluentPurple.copy(alpha = 0.15f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Text(r.grade, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FluentPurple) } },
                        title  = r.grade,
                        sub1   = "${r.classCount}班 · 在籍${r.studentCount}人 · 编制${r.enrollCount}人",
                        sub2   = "完成 ${r.lessonCount} 节 · ${String.format("%.1f", r.hours)} 小时",
                        progress = r.lessonCount.toFloat() / maxGrade.coerceAtLeast(1),
                        color    = FluentPurple,
                        chips    = emptyList()
                    )
                }
            }
        }

        // Student table
        if (dim == "student") {
            if (studentRows.isEmpty()) {
                item { EmptyState("🎒", "无符合条件的数据") }
            } else {
                items(studentRows) { (s, stats) ->
                    val (lessons, hours, subjects) = stats
                    StatCard(
                        avatar = { AvatarCircle(s.name, if (s.gender == "男") FluentBlue else FluentPurple, 40.dp) },
                        title  = "${s.name}  ${s.grade}",
                        sub1   = "出勤 $lessons 节 · ${String.format("%.1f", hours)} 小时",
                        progress = lessons.toFloat() / maxStudent.coerceAtLeast(1),
                        color    = FluentOrange,
                        chips    = subjects.map { it.name to packedToColor(it.color) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(icon: String, label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(14.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(icon, fontSize = 22.sp)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = FluentMuted)
            Box(Modifier.fillMaxWidth().height(3.dp).background(color.copy(0.2f), RoundedCornerShape(2.dp))) {
                Box(Modifier.fillMaxWidth(0.6f).height(3.dp).background(color, RoundedCornerShape(2.dp)))
            }
        }
    }
}

@Composable
private fun StatCard(
    avatar: @Composable () -> Unit,
    title: String, sub1: String, sub2: String = "",
    progress: Float, color: Color,
    chips: List<Pair<String, Color>>
) {
    FluentCard(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            avatar()
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(sub1, style = MaterialTheme.typography.bodyMedium, color = FluentMuted)
                if (sub2.isNotBlank()) Text(sub2, style = MaterialTheme.typography.bodyMedium, color = color, fontWeight = FontWeight.SemiBold)
                FluentProgressBar(progress, color, Modifier.fillMaxWidth())
                if (chips.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        chips.take(4).forEach { (name, c) -> ColorChip(name, c) }
                    }
                }
            }
        }
    }
}
