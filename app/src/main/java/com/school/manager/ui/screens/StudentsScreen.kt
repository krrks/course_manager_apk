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
fun StudentsScreen(vm: AppViewModel) {
    val state   by vm.state.collectAsState()
    var showAdd  by remember { mutableStateOf(false) }
    var viewing  by remember { mutableStateOf<Student?>(null) }
    var editing  by remember { mutableStateOf<Student?>(null) }
    var search   by remember { mutableStateOf("") }
    var fGrade   by remember { mutableStateOf("") }
    var fClass   by remember { mutableLongStateOf(0L) }

    val filtered = state.students.filter { s ->
        (search.isBlank() || s.name.contains(search) || s.studentNo.contains(search)) &&
        (fGrade.isBlank() || s.grade == fGrade) &&
        (fClass == 0L || s.classIds.contains(fClass))
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon = { Icon(Icons.Default.Add, "") },
                text = { Text("添加学生") },
                containerColor = FluentBlue, contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }
    ) { inner ->
        Column(modifier = Modifier.padding(inner).fillMaxSize()) {

            OutlinedTextField(
                value = search, onValueChange = { search = it },
                placeholder = { Text("搜索姓名或学号…", color = FluentMuted) },
                leadingIcon  = { Icon(Icons.Default.Search, null) },
                singleLine   = true,
                shape        = RoundedCornerShape(14.dp),
                modifier     = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = FluentBlue,
                    unfocusedBorderColor = FluentBorder
                )
            )

            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DropdownFilterChip("全部年级", GRADES.map { it to it }, fGrade) { fGrade = it }
                DropdownFilterChip(
                    "全部班级",
                    state.classes.map { it.id.toString() to it.name },
                    fClass.toString()
                ) { fClass = it.toLongOrNull() ?: 0L }
            }

            Text("共 ${filtered.size} 名学生",
                style    = MaterialTheme.typography.labelMedium,
                color    = FluentMuted,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

            LazyColumn(
                contentPadding      = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (filtered.isEmpty()) {
                    item { EmptyState("🎒", "暂无学生") }
                } else {
                    items(filtered) { s ->
                        val classes = s.classIds.mapNotNull { vm.schoolClass(it) }
                        FluentCard(modifier = Modifier.fillMaxWidth(), onClick = { viewing = s }) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AvatarWithImage(
                                    name     = s.name,
                                    color    = if (s.gender == "男") FluentBlue else FluentPurple,
                                    size     = 44.dp,
                                    imageUri = s.avatarUri
                                )
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(s.name, style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold)
                                        ColorChip(s.grade, FluentGreen)
                                        ColorChip(s.gender, if (s.gender == "男") FluentBlue else FluentPurple)
                                    }
                                    Text("学号：${s.studentNo}",
                                        style = MaterialTheme.typography.bodyMedium, color = FluentMuted)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        classes.forEach { cl -> ColorChip(cl.name, FluentBlue) }
                                    }
                                }
                            }
                        }
                    }
                }
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
    val attended = state.attendance.count { it.attendees.contains(s.id) && it.status == "completed" }

    FluentDialog(title = "学生详情", onDismiss = onDismiss) {
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.Center) {
            AvatarWithImage(
                name     = s.name,
                color    = if (s.gender == "男") FluentBlue else FluentPurple,
                size     = 72.dp,
                imageUri = s.avatarUri
            )
        }
        DetailRow("姓名", s.name)
        DetailRow("学号", s.studentNo)
        DetailRow("性别", s.gender)
        DetailRow("年级", s.grade)
        DetailRow("出勤课次", "$attended 节")
        if (classes.isNotEmpty()) {
            SectionHeader("所在班级")
            FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            avatarUri = copyImageToAppStorage(context, uri, entityId)
        }
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
        // Avatar picker
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                AvatarWithImage(
                    name     = name.ifBlank { "?" },
                    color    = if (gender == "男") FluentBlue else FluentPurple,
                    size     = 72.dp,
                    imageUri = avatarUri
                )
                Surface(
                    shape    = CircleShape,
                    color    = FluentBlue,
                    modifier = Modifier.size(24.dp).align(Alignment.BottomEnd)
                ) {
                    IconButton(
                        onClick  = { launcher.launch("image/*") },
                        modifier = Modifier.size(24.dp)
                    ) {
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

        FormTextField("姓名", name, { name = it }, "学生姓名")
        FormTextField("学号", studentNo, { studentNo = it }, "学号")
        FormDropdown("性别", gender, listOf("男", "女")) { gender = it }
        FormDropdown("年级", grade, GRADES) { grade = it }
        SectionHeader("所在班级（可多选）")
        FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            state.classes.forEach { cl ->
                val on = selClass.contains(cl.id)
                FilterChip(selected = on, onClick = {
                    selClass = if (on) selClass - cl.id else selClass + cl.id
                }, label = { Text(cl.name) })
            }
        }
    }
}

@Composable
private fun DropdownFilterChip(
    allLabel: String,
    items: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val label = items.firstOrNull { it.first == selected }?.second ?: allLabel
    Box {
        FilterChip(
            selected     = selected.isNotBlank() && selected != "0",
            onClick      = { expanded = true },
            label        = { Text(label) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, Modifier.size(16.dp)) }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(allLabel) }, onClick = { onSelect(""); expanded = false })
            items.forEach { (key, name) ->
                DropdownMenuItem(text = { Text(name) }, onClick = { onSelect(key); expanded = false })
            }
        }
    }
}
