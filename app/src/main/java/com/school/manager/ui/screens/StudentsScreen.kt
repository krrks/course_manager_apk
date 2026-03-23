package com.school.manager.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
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
    var showClassPicker by remember { mutableStateOf(false) }

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
        // ── 头像 ──────────────────────────────────────────────────────────
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

        // ── Row 1: 姓名 (1/2) + 学号 (1/2) ──────────────────────────────
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.Top
        ) {
            Box(Modifier.weight(1f)) { FormTextField("姓名", name, { name = it }) }
            Box(Modifier.weight(1f)) { FormTextField("学号", studentNo, { studentNo = it }, "留空自动生成") }
        }

        // ── Row 2: 性别 (1/2) + 年级 (1/2) ──────────────────────────────
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.Top
        ) {
            Box(Modifier.weight(1f)) { FormDropdown("性别", gender, listOf("男", "女")) { gender = it } }
            Box(Modifier.weight(1f)) { FormDropdown("年级", grade, GRADES) { grade = it } }
        }

        // ── 班级选择摘要行 ────────────────────────────────────────────────
        if (state.classes.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("所在班级", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("已选 ${selClass.size} 个",
                        style = MaterialTheme.typography.labelMedium,
                        color = FluentBlue, fontWeight = FontWeight.SemiBold)
                }
                TextButton(
                    onClick        = { showClassPicker = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text("选择", style = MaterialTheme.typography.labelMedium)
                }
            }

            // 已选班级 chips（最多 6 个，超出显示 +N）
            if (selClass.isNotEmpty()) {
                val selectedList = state.classes.filter { it.id in selClass }
                val displayList  = selectedList.take(6)
                val overflow     = selectedList.size - displayList.size
                androidx.compose.foundation.layout.FlowRow(
                    modifier              = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement   = Arrangement.spacedBy(4.dp)
                ) {
                    displayList.forEach { cl -> ColorChip(cl.name, FluentBlue) }
                    if (overflow > 0) ColorChip("+$overflow 个", FluentMuted)
                }
            }
        }
    }

    // ── 班级选择 Sheet ────────────────────────────────────────────────────
    if (showClassPicker) {
        ClassPickerSheet(
            allClasses  = state.classes,
            allSubjects = state.subjects,
            selected    = selClass,
            onConfirm   = { selClass = it; showClassPicker = false },
            onDismiss   = { showClassPicker = false }
        )
    }
}

// ── Class picker bottom sheet ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClassPickerSheet(
    allClasses: List<SchoolClass>,
    allSubjects: List<Subject>,
    selected: Set<Long>,
    onConfirm: (Set<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    var draft      by remember { mutableStateOf(selected) }
    var fGrade     by remember { mutableStateOf("") }
    var query      by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val filtered = allClasses.filter { c ->
        (fGrade.isBlank() || c.grade == fGrade) &&
        (query.isBlank()  || c.name.contains(query, ignoreCase = true))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── 标题 + 确定 ────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("选择班级",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                Button(
                    onClick = { onConfirm(draft) },
                    shape   = RoundedCornerShape(10.dp),
                    colors  = ButtonDefaults.buttonColors(containerColor = FluentBlue)
                ) { Text("确定（${draft.size} 个）") }
            }

            HorizontalDivider(color = FluentBorder)

            // ── 年级筛选 chips ─────────────────────────────────────────────
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = fGrade.isBlank(),
                    onClick  = { fGrade = "" },
                    label    = { Text("全部") }
                )
                GRADES.forEach { g ->
                    FilterChip(
                        selected = fGrade == g,
                        onClick  = { fGrade = if (fGrade == g) "" else g },
                        label    = { Text(g) }
                    )
                }
            }

            // ── 搜索框 ────────────────────────────────────────────────────
            OutlinedTextField(
                value         = query,
                onValueChange = { query = it },
                label         = { Text("搜索班级名称") },
                singleLine    = true,
                shape         = RoundedCornerShape(12.dp),
                modifier      = Modifier.fillMaxWidth(),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = FluentBlue,
                    unfocusedBorderColor = FluentBorder
                )
            )

            // ── 全选 / 取消全选 / 清空 ────────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filteredIds = filtered.map { it.id }.toSet()
                val allChecked  = filteredIds.isNotEmpty() && filteredIds.all { it in draft }
                OutlinedButton(
                    onClick  = { draft = if (allChecked) draft - filteredIds else draft + filteredIds },
                    shape    = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) { Text(if (allChecked) "取消全选" else "全选（${filtered.size}）") }
                if (draft.isNotEmpty()) {
                    OutlinedButton(
                        onClick  = { draft = emptySet() },
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = FluentRed),
                        modifier = Modifier.weight(1f)
                    ) { Text("清空已选") }
                }
            }

            // ── 班级列表 ──────────────────────────────────────────────────
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center) {
                    Text("没有符合条件的班级", color = FluentMuted,
                        style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(
                    modifier            = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(filtered, key = { it.id }) { cls ->
                        val checked     = cls.id in draft
                        val subjectName = cls.resolvedSubject(allSubjects)?.name
                        Row(
                            modifier              = Modifier
                                .fillMaxWidth()
                                .clickable { draft = if (checked) draft - cls.id else draft + cls.id }
                                .padding(vertical = 4.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked         = checked,
                                onCheckedChange = { draft = if (checked) draft - cls.id else draft + cls.id }
                            )
                            Text(cls.name,
                                style    = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f))
                            ColorChip(cls.grade, FluentGreen)
                            if (subjectName != null) ColorChip(subjectName, FluentBlue)
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}
