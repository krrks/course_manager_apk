package com.school.manager

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Schedule   : Screen("schedule",   "课表",     Icons.Outlined.CalendarMonth)
    data object Attendance : Screen("attendance", "上课记录", Icons.AutoMirrored.Outlined.EventNote)
    data object Classes    : Screen("classes",    "班级",     Icons.Outlined.School)
    data object Teachers   : Screen("teachers",   "教师",     Icons.Outlined.Person)
    data object Students   : Screen("students",   "学生",     Icons.Outlined.Group)
    data object Stats      : Screen("stats",      "课时统计", Icons.Outlined.BarChart)
    data object Export     : Screen("export",     "设置",     Icons.Default.Settings)
}

val ALL_SCREENS = listOf(
    Screen.Schedule, Screen.Attendance, Screen.Classes,
    Screen.Teachers, Screen.Students, Screen.Stats, Screen.Export
)
