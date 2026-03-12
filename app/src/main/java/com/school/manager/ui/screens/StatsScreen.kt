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
fun StatsScreen(vm: AppViewModel, onOpenDrawer: () -> Unit = {}) {
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

    val scopedRecs = completed.filter { r ->
        (fTeachers.isEmpty() || fTeachers.contains(r.teacherId)) &&
        (fGrades.isEmpty() || vm.schoolClass(r.classId)?.grade?.let { fGrades.contains(it) } == true) &&
        classPassCount(r.classId)
    }
    val totalLessons   = scopedRecs.size
    val totalHours     = String.format("%.1f", totalLessons * PERIOD_HOURS)
    val activeTeachers = scopedRecs.mapNotNull { it.teacherId }.toSet().size
    val activeStudents = scopedRecs.flatMap { it.attendees }.toSet().size

    val teacherRows = state.teachers
        .filter { t -> fTeachers.isEmpty() || fTeachers.contains(t.id) }
        .map { t ->
            val recs = completed.filter { r ->
                r.teacherId == t.id &&
                (fGrades.isEmpty() || vm.schoolClass(r.classId)?.grade?.let { fGrades.contains(it) } == true) &&
                classPassCount(r.classId)
            }
            Triple(t, recs.size, (recs.size * PERIOD_HOURS))
        }.sortedByDescending { it.second }
    val maxTeacher = teacherRows.maxOfOrNull { it.second } ?: 1

    val gradeRows = GRADES.map { g ->
        val recs = completed.filter { r ->
            vm.schoolClass(r.classId)?.grade == g &&
            (fTeachers.isEmpty() || fTeachers.contains(r.teacherId)) &&
            classPassCount(r.classId)
        }
        Triple(g, recs.size, (recs.size * PERIOD_HOURS))
    }.filter { it.second > 0 }.sortedByDescending { it.second }
    val maxGrade = gradeRows.maxOfOrNull { it.second } ?: 1

    val studentRows = state.students.map { s ->
        val cnt = completed.count { r ->
            r.attendees.contains(s.id) &&
            (fTeachers.isEmpty() || fTeachers.contains(r.teacherId)) &&
            (fGrades.isEmpty() || vm.schoolClass(r.classId)?.grade?.let { fGrades.contains(it) } == true) &&
            classPassCount(r.classId)
        }
        s to cnt
    }.filter { it.second > 0 }.sortedByDescending { it.second }
    val maxStudent = studentRows.maxOfOrNull { it.second } ?: 1

    Scaffold(
        floatingActionButton = {
            // Stats screen has no add action — FAB opens nav only
            ScreenSpeedDialFab(onOpenDrawer = onOpenDrawer)
        }
    ) { inner ->
        LazyColumn(
            contentPadding      = PaddingValues(
                start  = 12.dp, end = 12.dp,
                top    = inner.calculateTopPadding() + 8.dp,
                bottom = inner.calculateBottomPadding() + 80.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Summary cards
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryCard("📚", "$totalLessons", "课次", FluentBlue,  Modifier.weight(1f))
                    SummaryCard("⏱️", totalHours,      "课时", FluentGreen, Modifier.weight(1f))
                    SummaryCard("👩‍🏫", "$activeTeachers","教师", FluentPurple,Modifier.weight(1f))
                    SummaryCard("👥", "$activeStudents","学生", FluentAmber, Modifier.weight(1f))
                }
            }

            // Filters toggle
            item {
                TextButton(onClick = { showFilters = !showFilters }) {
                    Text(if (showFilters) "▲ 收起筛选" else "▼ 展开筛选",
                        style = MaterialTheme.typography.labelMedium, color = FluentBlue)
                }
            }

            if (showFilters) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("教师筛选", style = MaterialTheme.typography.labelSmall, color = FluentMuted,
                            modifier = Modifier.padding(horizontal = 4.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            state.teachers.forEach { t ->
                                FilterChip(selected = fTeachers.contains(t.id), onClick = {
                                    fTeachers = if (fTeachers.contains(t.id)) fTeachers - t.id else fTeachers + t.id
                                }, label = { Text(t.name) })
                            }
                        }
                        Text("年级筛选", style = MaterialTheme.typography.labelSmall, color = FluentMuted,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            GRADES.forEach { g ->
                                FilterChip(selected = fGrades.contains(g), onClick = {
                                    fGrades = if (fGrades.contains(g)) fGrades - g else fGrades + g
                                }, label = { Text(g) })
                            }
                        }
                    }
                }
            }

            // Dimension tabs
            item {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("teacher" to "按教师", "grade" to "按年级", "student" to "按学生").forEach { (d, label) ->
                        FilterChip(selected = dim == d, onClick = { dim = d }, label = { Text(label) })
                    }
                }
            }

            when (dim) {
                "teacher" -> items(teacherRows) { (t, cnt, hrs) ->
                    StatCard(
                        avatar   = { AvatarWithImage(t.name,
                            if (t.gender == "男") FluentBlue else FluentPurple, 40.dp, t.avatarUri) },
                        title    = t.name,
                        sub1     = "${String.format("%.1f", hrs)} 课时",
                        sub2     = "$cnt 节课",
                        progress = cnt.toFloat() / maxTeacher,
                        color    = if (t.gender == "男") FluentBlue else FluentPurple,
                        chips    = emptyList()
                    )
                }
                "grade"   -> items(gradeRows) { (g, cnt, hrs) ->
                    StatCard(
                        avatar   = { AvatarCircle(g.take(1), FluentGreen, 40.dp) },
                        title    = g,
                        sub1     = "${String.format("%.1f", hrs)} 课时",
                        sub2     = "$cnt 节课",
                        progress = cnt.toFloat() / maxGrade,
                        color    = FluentGreen,
                        chips    = emptyList()
                    )
                }
                "student" -> items(studentRows) { (s, cnt) ->
                    StatCard(
                        avatar   = { AvatarWithImage(s.name,
                            if (s.gender == "男") FluentBlue else FluentPurple, 40.dp, s.avatarUri) },
                        title    = s.name,
                        sub1     = s.grade,
                        sub2     = "$cnt 节课",
                        progress = cnt.toFloat() / maxStudent,
                        color    = if (s.gender == "男") FluentBlue else FluentPurple,
                        chips    = s.classIds.mapNotNull { vm.schoolClass(it) }.map { it.name to FluentBlue }
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(icon: String, value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(14.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(icon, fontSize = 22.sp)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = FluentMuted)
            Box(Modifier.fillMaxWidth().height(3.dp)
                .background(color.copy(0.2f), RoundedCornerShape(2.dp))) {
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
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            avatar()
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(sub1, style = MaterialTheme.typography.bodyMedium, color = FluentMuted)
                if (sub2.isNotBlank())
                    Text(sub2, style = MaterialTheme.typography.bodyMedium, color = color,
                        fontWeight = FontWeight.SemiBold)
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
