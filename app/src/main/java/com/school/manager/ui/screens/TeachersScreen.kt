package com.school.manager.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.school.manager.data.*
import com.school.manager.ui.components.*
import com.school.manager.ui.theme.*
import com.school.manager.util.copyImageToAppStorage
import com.school.manager.viewmodel.AppViewModel

@Composable
fun TeachersScreen(vm: AppViewModel) {
    val state   by vm.state.collectAsState()
    var showAdd  by remember { mutableStateOf(false) }
    var viewing  by remember { mutableStateOf<Teacher?>(null) }
    var editing  by remember { mutableStateOf<Teacher?>(null) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon    = { Icon(Icons.Default.Add, "") },
                text    = { Text("添加教师") },
                containerColor = FluentBlue, contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }
    ) { inner ->
        LazyColumn(
            contentPadding = PaddingValues(inner.calculateTopPadding() + 12.dp, 12.dp, 12.dp, 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (state.teachers.isEmpty()) {
                item { EmptyState("👩‍🏫", "暂无教师") }
            } else {
                items(state.teachers) { t ->
                    // Derive classes this teacher teaches
                    val teacherClasses = state.classes.filter { c ->
                        state.schedule.any { s -> s.teacherId == t.id && s.classId == c.id }
                    }
                    FluentCard(modifier = Modifier.fillMaxWidth(), onClick = { viewing = t }) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AvatarWithImage(
                                name     = t.name,
                                color    = if (t.gender == "男") FluentBlue else FluentPurple,
                                size     = 48.dp,
                                imageUri = t.avatarUri
                            )
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(t.name, style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold)
                                    ColorChip(t.gender, if (t.gender == "男") FluentBlue else FluentPurple)
                                    if (t.code.isNotBlank()) {
                                        Text(t.code, style = MaterialTheme.typography.labelSmall,
                                            color = FluentMuted)
                                    }
                                }
                                Text(t.phone, style = MaterialTheme.typography.bodyMedium, color = FluentMuted)
                                if (teacherClasses.isNotEmpty()) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        teacherClasses.take(3).forEach { c ->
                                            ColorChip(c.subject.ifBlank { c.name }, FluentGreen)
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
            vm.addTeacher(t.name, t.gender, t.phone, t.subjectIds, t.avatarUri, t.code)
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
    FluentDialog(title = "教师详情", onDismiss = onDismiss) {
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.Center) {
            AvatarWithImage(
                name     = t.name,
                color    = if (t.gender == "男") FluentBlue else FluentPurple,
                size     = 72.dp,
                imageUri = t.avatarUri
            )
        }
        if (t.code.isNotBlank()) DetailRow("编号", t.code)
        DetailRow("姓名",     t.name)
        DetailRow("性别",     t.gender)
        DetailRow("手机",     t.phone)
        DetailRow("完成课次", "$lessonCount 节")
        if (teacherClasses.isNotEmpty()) {
            SectionHeader("任课班级")
            FlowRow(Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                teacherClasses.forEach { c ->
                    ColorChip(
                        if (c.subject.isNotBlank()) "${c.name}·${c.subject}" else c.name,
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
                id         = initial?.id ?: System.currentTimeMillis(),
                name       = name.trim(),
                gender     = gender,
                phone      = phone.trim(),
                subjectIds = initial?.subjectIds ?: emptyList(),
                avatarUri  = avatarUri,
                code       = code.trim().ifBlank { genCode("T") }
            ))
        }
    }) {
        // Avatar
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Box {
                AvatarWithImage(name = name.ifBlank { "?" },
                    color = if (gender == "男") FluentBlue else FluentPurple,
                    size = 72.dp, imageUri = avatarUri)
                Surface(shape = CircleShape, color = FluentBlue,
                    modifier = Modifier.size(24.dp).align(Alignment.BottomEnd)) {
                    IconButton(onClick = { launcher.launch("image/*") },
                        modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.CameraAlt, "选择头像",
                            tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
        if (avatarUri != null) {
            TextButton(onClick = { avatarUri = null }, modifier = Modifier.fillMaxWidth()) {
                Text("移除头像", color = FluentRed)
            }
        }
        FormTextField("编号", code, { code = it }, "自动生成，可修改")
        FormTextField("姓名", name, { name = it }, "教师姓名")
        FormDropdown("性别", gender, listOf("男", "女")) { gender = it }
        FormTextField("手机号", phone, { phone = it }, "联系方式")
    }
}
