package com.school.manager.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
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
import androidx.compose.ui.unit.*
import com.school.manager.data.*
import com.school.manager.ui.components.*
import com.school.manager.ui.theme.*
import com.school.manager.util.copyImageToAppStorage
import com.school.manager.viewmodel.AppViewModel

@Composable
fun StudentsScreen(vm: AppViewModel, onOpenDrawer: () -> Unit = {}) {
    val state    by vm.state.collectAsState()
    var showAdd   by remember { mutableStateOf(false) }
    var viewing   by remember { mutableStateOf<Student?>(null) }
    var editing   by remember { mutableStateOf<Student?>(null) }
    var fGrade    by remember { mutableStateOf("") }
    var fClass    by remember { mutableLongStateOf(0L) }

    val filtered = state.students.filter { s ->
        (fGrade.isBlank() || s.grade == fGrade) &&
        (fClass == 0L || s.classIds.contains(fClass))
    }

    Scaffold(
        floatingActionButton = {
            ScreenSpeedDialFab(
                addLabel     = "添加学生",
                addIcon      = Icons.Default.Add,
                onAdd        = { showAdd = true },
                onOpenDrawer = onOpenDrawer
            )
        }
    ) { inner ->
        Column(modifier = Modifier.fillMaxSize()
            .padding(top = inner.calculateTopPadding(), bottom = inner.calculateBottomPadding())) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DropdownFilterChip("全部年级",
                    items    = GRADES.mapIndexed { i, g -> (i + 1).toLong() to g },
                    selected = GRADES.indexOf(fGrade).let { if (it < 0) 0L else (it + 1).toLong() }
                ) { idx -> fGrade = if (idx == 0L) "" else GRADES.getOrElse((idx - 1).toInt()) { "" } }
                DropdownFilterChip("全部班级",
                    items    = state.classes.map { it.id to it.name },
                    selected = fClass) { fClass = it }
            }

            Text("共 ${filtered.size} 名学生", style = MaterialTheme.typography.labelMedium,
                color = FluentMuted, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

            LazyColumn(contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (filtered.isEmpty()) {
                    item { EmptyState("🎒", "暂无学生") }
                } else {
                    items(filtered) { s ->
                        val classes = s.classIds.mapNotNull { vm.schoolClass(it) }
                        FluentCard(modifier = Modifier.fillMaxWidth(), onClick = { viewing = s }) {
                            Row(modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                AvatarWithImage(name = s.name,
                                    color    = if (s.gender == "男") FluentBlue else FluentPurple,
                                    size     = 44.dp, imageUri = s.avatarUri)
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(s.name, style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold)
                                        ColorChip(s.grade, FluentGreen)
                                        ColorChip(s.gender, if (s.gender == "男") FluentBlue else FluentPurple)
                                    }
                                    Text("学号：${s.studentNo}", style = MaterialTheme.typography.bodyMedium,
                                        color = FluentMuted)
                                    if (classes.isNotEmpty()) {
                                        androidx.compose.foundation.layout.FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            classes.forEach { cl -> ColorChip(cl.name, FluentBlue) }
                                        }
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

    viewing?.let { s ->
        StudentDetailDialog(s, vm, state,
            onDismiss = { viewing = null },
            onEdit    = { editing = s; viewing = null },
            onDelete  = { vm.deleteStudent(s.id); viewing = null }
        )
    }
    editing?.let { s ->
        StudentFormDialog("编辑学生", s, state, vm, onDismiss = { editing = null }) { updated ->
            vm.updateStudent(updated); editing = null
        }
    }
    if (showAdd) {
        StudentFormDialog("添加学生", null, state, vm, onDismiss = { showAdd = false }) { s ->
            vm.addStudent(s.name, s.studentNo, s.gender, s.grade, s.classIds, s.avatarUri)
            showAdd = false
        }
    }
}

@Composable
private fun StudentDetailDialog(
    s: Student, vm: AppViewModel, state: AppState,
    onDismiss: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit
) {
    val classes  = s.classIds.mapNotNull { vm.schoolClass(it) }
    val attended = state.lessons.count { it.attendees.contains(s.id) && it.status == "completed" }

    FluentDialog(title = "学生详情", onDismiss = onDismiss) {
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.Center) {
            AvatarWithImage(name = s.name,
                color    = if (s.gender == "男") FluentBlue else FluentPurple,
                size     = 72.dp, imageUri = s.avatarUri)
        }
        DetailRow("姓名",   s.name)
        DetailRow("学号",   s.studentNo)

        // 性别 + 年级 — both short, pair them
        DetailRowPair("性别", s.gender, "年级", s.grade)

        DetailRow("出勤课次", "$attended 节")

        if (classes.isNotEmpty()) {
            SectionHeader("所在班级")
            androidx.compose.foundation.layout.FlowRow(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                classes.forEach { cl -> ColorChip(cl.name, FluentBlue) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onEdit, shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)) { Text("✏️ 编辑") }
            OutlinedButton(onClick = onDelete, shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = FluentRed),
                modifier = Modifier.weight(1f)) { Text("删除") }
        }
    }
}

@Composable
private fun StudentFormDialog(
    title: String, initial: Student?,
    state: AppState, vm: AppViewModel,
    onDismiss: () -> Unit, onSave: (Student) -> Unit
) {
    var name      by remember { mutableStateOf(initial?.name      ?: "") }
    var studentNo by remember { mutableStateOf(initial?.studentNo ?: "") }
    var gender    by remember { mutableStateOf(initial?.gender    ?: "男") }
    var grade     by remember { mutableStateOf(initial?.grade     ?: GRADES[0]) }
    var selClass  by remember { mutableStateOf(initial?.classIds?.toSet() ?: emptySet<Long>()) }
    var avatarUri by remember { mutableStateOf<String?>(initial?.avatarUri) }

    val context  = LocalContext.current
    val entityId = initial?.id ?: System.currentTimeMillis()

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) avatarUri = copyImageToAppStorage(context, uri, entityId)
    }

    FluentDialog(title = title, onDismiss = onDismiss, onConfirm = {
        if (name.isNotBlank()) {
            onSave(Student(
                id        = initial?.id ?: System.currentTimeMillis(),
                name      = name,
                studentNo = studentNo.ifBlank { "${System.currentTimeMillis()}" },
                gender    = gender,
                grade     = grade,
                classIds  = selClass.toList(),
                avatarUri = avatarUri
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
        FormTextField("姓名",  name,      { name      = it })
        FormTextField("学号",  studentNo, { studentNo = it }, "留空则自动生成")
        FormDropdown("性别", gender, listOf("男", "女")) { gender = it }
        FormDropdown("年级", grade,  GRADES)              { grade  = it }
        if (state.classes.isNotEmpty()) {
            SectionHeader("所在班级（可多选）")
            androidx.compose.foundation.layout.FlowRow(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement   = Arrangement.spacedBy(4.dp)) {
                state.classes.forEach { cls ->
                    val on = selClass.contains(cls.id)
                    FilterChip(selected = on, onClick = {
                        selClass = if (on) selClass - cls.id else selClass + cls.id
                    }, label = { Text(cls.name) })
                }
            }
        }
    }
}
