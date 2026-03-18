package com.school.manager.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.school.manager.data.*
import com.school.manager.ui.components.*
import com.school.manager.ui.theme.*
import com.school.manager.util.copyImageToAppStorage
import com.school.manager.viewmodel.AppViewModel

@Composable
fun TeachersScreen(vm: AppViewModel, onOpenDrawer: () -> Unit) {
    val state   by vm.state.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var viewing by remember { mutableStateOf<Teacher?>(null) }
    var editing by remember { mutableStateOf<Teacher?>(null) }

    Scaffold(
        floatingActionButton = {
            ScreenSpeedDialFab(onOpenDrawer = onOpenDrawer)
        }
    ) { inner ->
        LazyColumn(
            contentPadding = PaddingValues(
                top    = inner.calculateTopPadding() + 8.dp,
                bottom = inner.calculateBottomPadding() + 80.dp,
                start  = 12.dp, end = 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (state.teachers.isEmpty()) {
                item { EmptyState("👩‍🏫", "暂无教师") }
            } else {
                items(state.teachers) { t ->
                    val teacherSubjects = state.subjects.filter { it.teacherId == t.id }
                    val lessonCount = state.attendance.count { it.teacherId == t.id && it.status == "completed" }
                    val teacherClasses = state.classes.filter { c ->
                        state.schedule.any { s -> s.teacherId == t.id && s.classId == c.id }
                    }

                    FluentCard(modifier = Modifier.fillMaxWidth(), onClick = { viewing = t }) {
                        Row(Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            AvatarWithImage(name = t.name,
                                color    = if (t.gender == "男") FluentBlue else FluentPurple,
                                size     = 52.dp, imageUri = t.avatarUri)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Text(t.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold)
                                    ColorChip(t.gender, if (t.gender == "男") FluentBlue else FluentPurple)
                                }
                                if (t.phone.isNotBlank())
                                    Text("📞 ${t.phone}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = FluentMuted)
                                Text("完成课次：$lessonCount",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = FluentMuted)
                                if (teacherSubjects.isNotEmpty() || teacherClasses.isNotEmpty()) {
                                    androidx.compose.foundation.layout.FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalArrangement   = Arrangement.spacedBy(4.dp)
                                    ) {
                                        teacherSubjects.take(3).forEach { sub ->
                                            ColorChip(sub.name, FluentBlue)
                                        }
                                        teacherClasses.take(3).forEach { c ->
                                            // subject name derived from FK only
                                            val subjectDisplay = c.resolvedSubject(state.subjects)?.name ?: c.name
                                            ColorChip(subjectDisplay, FluentGreen)
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

    viewing?.let { t ->
        TeacherDetailDialog(t, vm, state,
            onDismiss = { viewing = null },
            onEdit    = { editing = t; viewing = null },
            onDelete  = { vm.deleteTeacher(t.id); viewing = null }
        )
    }

    editing?.let { t ->
        TeacherFormDialog("编辑教师", t, state, vm, onDismiss = { editing = null }) { updated ->
            vm.updateTeacher(updated); editing = null
        }
    }

    if (showAdd) {
        TeacherFormDialog("添加教师", null, state, vm, onDismiss = { showAdd = false }) { t ->
            vm.addTeacher(t.name, t.gender, t.phone, avatarUri = t.avatarUri, code = t.code)
            showAdd = false
        }
    }
}

@Composable
private fun TeacherDetailDialog(
    t: Teacher, vm: AppViewModel, state: AppState,
    onDismiss: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit
) {
    val lessonCount = state.attendance.count { it.teacherId == t.id && it.status == "completed" }
    val teacherClasses = state.classes.filter { c ->
        state.schedule.any { s -> s.teacherId == t.id && s.classId == c.id }
    }
    val teacherSubjects = state.subjects.filter { it.teacherId == t.id }

    FluentDialog(title = "教师详情", onDismiss = onDismiss) {
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.Center) {
            AvatarWithImage(name = t.name,
                color    = if (t.gender == "男") FluentBlue else FluentPurple,
                size     = 72.dp, imageUri = t.avatarUri)
        }
        if (t.code.isNotBlank()) DetailRow("编号", t.code)
        DetailRow("姓名",     t.name)
        DetailRow("性别",     t.gender)
        DetailRow("手机",     t.phone)
        DetailRow("完成课次", "$lessonCount 节")
        if (teacherSubjects.isNotEmpty()) {
            SectionHeader("任教科目")
            androidx.compose.foundation.layout.FlowRow(Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                teacherSubjects.forEach { sub -> ColorChip(sub.name, FluentBlue) }
            }
        }
        if (teacherClasses.isNotEmpty()) {
            SectionHeader("任课班级")
            androidx.compose.foundation.layout.FlowRow(Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                teacherClasses.forEach { c ->
                    val subjectDisplay = c.resolvedSubject(state.subjects)?.name
                    ColorChip(
                        if (subjectDisplay != null) "${c.name}·$subjectDisplay" else c.name,
                        FluentGreen
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onEdit, shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)) { Text("✏️ 编辑") }
            OutlinedButton(onClick = onDelete, shape = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = FluentRed),
                modifier = Modifier.weight(1f)) { Text("删除") }
        }
    }
}

@Composable
private fun TeacherFormDialog(
    title: String, initial: Teacher?,
    state: AppState, vm: AppViewModel,
    onDismiss: () -> Unit, onSave: (Teacher) -> Unit
) {
    var name      by remember { mutableStateOf(initial?.name   ?: "") }
    var gender    by remember { mutableStateOf(initial?.gender ?: "男") }
    var phone     by remember { mutableStateOf(initial?.phone  ?: "") }
    var code      by remember { mutableStateOf(initial?.code   ?: genCode("T")) }
    var avatarUri by remember { mutableStateOf<String?>(initial?.avatarUri) }

    val context  = LocalContext.current
    val entityId = initial?.id ?: System.currentTimeMillis()

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) avatarUri = copyImageToAppStorage(context, uri, entityId)
    }

    FluentDialog(title = title, onDismiss = onDismiss, onConfirm = {
        if (name.isNotBlank()) {
            onSave(Teacher(
                id        = initial?.id ?: System.currentTimeMillis(),
                name      = name.trim(),
                gender    = gender,
                phone     = phone.trim(),
                avatarUri = avatarUri,
                code      = code.trim()
            ))
        }
    }) {
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.Center) {
            Box {
                AvatarWithImage(name = name.ifBlank { "?" },
                    color    = if (gender == "男") FluentBlue else FluentPurple,
                    size     = 72.dp, imageUri = avatarUri)
                SmallFloatingActionButton(onClick = { launcher.launch("image/*") },
                    modifier = Modifier.align(Alignment.BottomEnd),
                    containerColor = FluentBlue, contentColor = Color.White,
                    shape = androidx.compose.foundation.shape.CircleShape) {
                    Icon(Icons.Default.Edit, null, Modifier.size(14.dp))
                }
            }
        }
        FormTextField("姓名", name,  { name  = it })
        FormDropdown("性别", gender, listOf("男", "女")) { gender = it }
        FormTextField("手机", phone, { phone = it })
        FormTextField("教师编号", code, { code = it })
    }
}
