package com.school.manager

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.automirrored.outlined.MenuBook  // 新增这行
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Lesson : Screen("lesson", "课程日历", Icons.AutoMirrored.Outlined.EventNote)
    data object Classes : Screen("classes", "班级", Icons.Outlined.School)
    data object Teachers : Screen("teachers", "教师", Icons.Outlined.Person)
    data object Students : Screen("students", "学生", Icons.Outlined.Group)
    
    // ✅ 已修复：使用 AutoMirrored 版本
    data object Subjects : Screen("subjects", "科目管理", Icons.AutoMirrored.Outlined.MenuBook)
    
    data object KnowledgePoints : Screen("knowledge_points", "知识点", Icons.Outlined.Lightbulb)
    data object Stats : Screen("stats", "课时统计", Icons.Outlined.BarChart)
    data object Export : Screen("export", "设置", Icons.Default.Settings)
}

val ALL_SCREENS = listOf(
    Screen.Lesson, Screen.Classes, Screen.Teachers,
    Screen.Students, Screen.Subjects, Screen.KnowledgePoints,
    Screen.Stats, Screen.Export
)