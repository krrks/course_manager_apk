package com.school.manager.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.school.manager.data.*
import com.school.manager.ui.components.*
import com.school.manager.ui.theme.*
import com.school.manager.viewmodel.AppViewModel

@Composable
fun ClassesScreen(vm: AppViewModel, onOpenDrawer: () -> Unit) {
    val state   by vm.state.collectAsState()
    var showAdd  by remember { mutableStateOf(false) }
    var viewing  by remember { mutableStateOf<SchoolClass?>(null) }
    var editing  by remember { mutableStateOf<SchoolClass?>(null) }
    var fGrade   by remember { mutableStateOf("") }
    var fTeacher by remember { mutableLongStateOf(0L) }
    var fSubject by remember { mutableLongStateOf(0L) }

    val filtered = state.classes.filter { cls ->
        (fGrade.isBlank() || cls.grade == fGrade) &&
        (fTeacher == 0L   || cls.headTeacherId == fTeacher) &&
        (fSubject == 0L   || cls.subjectId == fSubject)
    }

    Scaffold(
        floatingActionButton = {
            ScreenSpeedDialFab(
                addLabel     = "添加班级",
                addIcon      = Icons.Default.Add,
                onAdd        = { showAdd = true },
                onOpenDrawer = onOpenDrawer
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = inner.calculateTopPadding(), bottom = inner.calculateBottomPadding())
        ) {
            // ── 筛选行 ────────────────────────────────────────────────────
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DropdownFilterChip(
                    allLabel = "全部年级",
                    items    = GRADES.mapIndexed { i, g -> (i + 1).toLong() to g },
                    selected = GRADES.indexOf(fGrade).let { if (it < 0) 0L else (it + 1).toLong() }
                ) { idx -> fGrade = if (idx == 0L) "" else GRADES.getOrElse((idx - 1).toInt()) { "" } }
                if (state.teachers.isNotEmpty()) {
                    DropdownFilterChip(
                        allLabel = "全部教师",
                        items    = state.teachers.map { it.id to it.name },
                        selected = fTeacher
                    ) { fTeacher = it }
                }
                if (state.subjects.isNotEmpty()) {
                    DropdownFilterChip(
                        allLabel = "全部科目",
                        items    = state.subjects.map { it.id to it.name },
                        selected = fSubject
                    ) { fSubject = it }
                }
            }
            Text(
                text     = "共 ${filtered.size} 个班级",
                style    = MaterialTheme.typography.labelMedium,
                color    = FluentMuted,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
            LazyColumn(
                contentPadding      = PaddingValues(
                    top    = 4.dp,
                    bottom = 80.dp,
                    start  = 12.dp, end = 12.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier            = Modifier.fillMaxSize()
            ) {
                if (filtered.isEmpty()) {
                    item { EmptyState("🏫", "暂无班级") }
                } else {
                    items(filtered) { cls ->
                    val sts      = state.students.filter { s -> s.classIds.contains(cls.id) }
                    val gradeCol = gradeColor(cls.grade)
                    val subjectDisplay = cls.resolvedSubject(state.subjects)?.name
                    FluentCard(
                        accentColor = gradeCol,
                        modifier    = Modifier.fillMaxWidth(),
                        onClick     = { viewing = cls }
                    ) {
                        Column(
                            modifier            = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Text(cls.name,
                                    style      = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold)
                                ColorChip(cls.grade, gradeCol)
                            }
                            Text(
                                "教师：${state.teachers.find { it.id == cls.headTeacherId }?.name ?: "未设置"}" +
                                if (subjectDisplay != null) "   📚 $subjectDisplay" else "",
                                style = MaterialTheme.typography.bodyMedium, color = FluentMuted)
                            LinearProgressIndicator(
                                progress   = { if (cls.count > 0) sts.size.toFloat() / cls.count else 0f },
                                modifier   = Modifier.fillMaxWidth().height(4.dp),
                                color      = gradeCol,
                                trackColor = gradeCol.copy(alpha = 0.15f),
                            )
                            Text("${sts.size} / ${cls.count} 人",
                                style = MaterialTheme.typography.labelSmall, color = FluentMuted)
                        }
                    }
                }
            }
            }
        }
    }

    viewing?.let { cls ->
        ClassDetailDialog(cls, state, vm,
            onDismiss = { viewing = null },
            onEdit    = { editing = cls; viewing = null },
            onDelete  = { vm.deleteSchoolClass(cls.id); viewing = null }
        )
    }
    editing?.let { cls ->
        ClassFormDialog("编辑班级", cls, state, vm, onDismiss = { editing = null }) { updated ->
            vm.updateSchoolClass(updated); editing = null
        }
    }
    if (showAdd) {
        ClassFormDialog("添加班级", null, state, vm, onDismiss = { showAdd = false }) { c ->
            vm.addSchoolClass(c.name, c.grade, c.count, c.headTeacherId, c.subjectId, code = c.code)
            showAdd = false
        }
    }
}

private fun gradeColor(grade: String) = when {
    grade.startsWith("高一") -> FluentBlue
    grade.startsWith("高二") -> FluentPurple
    grade.startsWith("高三") -> FluentOrange
    grade.startsWith("初一") -> FluentGreen
    grade.startsWith("初二") -> FluentTeal
    else                     -> FluentAmber
}

@Composable
private fun ClassDetailDialog(
    cls: SchoolClass, state: AppState, vm: AppViewModel,
    onDismiss: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit
) {
    val ht             = vm.teacher(cls.headTeacherId)
    val sts            = state.students.filter { s -> s.classIds.contains(cls.id) }
    val subjectDisplay = cls.resolvedSubject(state.subjects)?.name

    FluentDialog(title = "班级详情", onDismiss = onDismiss) {
        if (cls.code.isNotBlank()) DetailRow("编号", cls.code)
        DetailRow("班级名称", cls.name)

        if (subjectDisplay != null) {
            DetailRowPair("年级", cls.grade, "科目", subjectDisplay)
        } else {
            DetailRow("年级", cls.grade)
        }

        DetailRow("教师", ht?.name ?: "未设置")
        DetailRowPair("编制人数", "${cls.count} 人", "在籍学生", "${sts.size} 人")

        if (sts.isNotEmpty()) {
            SectionHeader("班级学生")
            androidx.compose.foundation.layout.FlowRow(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                sts.forEach { s -> ColorChip(s.name, FluentBlue) }
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
private fun ClassFormDialog(
    title: String, initial: SchoolClass?,
    state: AppState, vm: AppViewModel,
    onDismiss: () -> Unit, onSave: (SchoolClass) -> Unit
) {
    var name    by remember { mutableStateOf(initial?.name    ?: "") }
    var grade   by remember { mutableStateOf(initial?.grade   ?: GRADES[0]) }
    var count   by remember { mutableStateOf(initial?.count?.toString() ?: "") }
    var teacher by remember { mutableStateOf(
        state.teachers.firstOrNull { it.id == initial?.headTeacherId }?.name ?: "") }
    var code    by remember { mutableStateOf(initial?.code    ?: genCode("C")) }

    val initialSubject: Subject? = remember(state.subjects, initial) {
        initial?.let { cls -> state.subjects.find { it.id == cls.subjectId } }
    }
    var selectedSubjectId   by remember { mutableStateOf(initialSubject?.id) }
    val selectedSubjectName = state.subjects.find { it.id == selectedSubjectId }?.name ?: ""

    val initialStudentIds = remember(state.students, initial) {
        if (initial == null) emptySet()
        else state.students.filter { it.classIds.contains(initial.id) }.map { it.id }.toSet()
    }
    var selectedStudents  by remember { mutableStateOf(initialStudentIds) }
    var showStudentPicker by remember { mutableStateOf(false) }

    FluentDialog(title = title, onDismiss = onDismiss, onConfirm = {
        if (name.isNotBlank()) {
            val tId = state.teachers.firstOrNull { it.name == teacher }?.id
            val savedClass = SchoolClass(
                id            = initial?.id ?: System.currentTimeMillis(),
                name          = name.trim(),
                grade         = grade,
                count         = count.toIntOrNull() ?: 0,
                headTeacherId = tId,
                subjectId     = selectedSubjectId,
                code          = code.trim()
            )
            if (initial != null) {
                val classId = initial.id
                (selectedStudents - initialStudentIds).forEach { sId ->
                    val s = state.students.find { it.id == sId }
                    if (s != null && !s.classIds.contains(classId))
                        vm.updateStudent(s.copy(classIds = s.classIds + classId))
                }
                (initialStudentIds - selectedStudents).forEach { sId ->
                    val s = state.students.find { it.id == sId }
                    if (s != null)
                        vm.updateStudent(s.copy(classIds = s.classIds.filter { it != classId }))
                }
            }
            onSave(savedClass)
        }
    }) {
        // ── 行1: 班级名称 (2/3) + 班级编号 (1/3) ─────────────────────────
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.Top
        ) {
            Box(Modifier.weight(2f)) { FluentTextField("班级名称", name, { name = it }) }
            Box(Modifier.weight(1f)) { FluentTextField("编号",    code, { code = it }) }
        }

        // ── 行2: 年级 (1/2) + 编制人数 (1/2) ────────────────────────────
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.Top
        ) {
            Box(Modifier.weight(1f)) { DropdownField("年级", grade, GRADES) { grade = it } }
            Box(Modifier.weight(1f)) { FluentTextField("编制人数", count, { count = it }) }
        }

        // ── 行3: 教师 (1/2) + 科目 (1/2) ────────────────────────────────
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.Top
        ) {
            Box(Modifier.weight(1f)) {
                DropdownField("教师", teacher,
                    listOf("") + state.teachers.map { it.name }) { teacher = it }
            }
            Box(Modifier.weight(1f)) {
                if (state.subjects.isNotEmpty()) {
                    DropdownField(
                        label    = "科目",
                        selected = selectedSubjectName.ifBlank { "无科目" },
                        options  = listOf("无科目") + state.subjects.map { it.name }
                    ) { picked ->
                        selectedSubjectId = if (picked == "无科目") null
                                            else state.subjects.firstOrNull { it.name == picked }?.id
                    }
                } else {
                    // 无科目时占位，提示在下方显示
                    DropdownField("科目", "无科目", listOf("无科目")) {}
                }
            }
        }

        // 科目提示文字（行3下方，全宽）
        when {
            state.subjects.isEmpty() ->
                Text("⚠️ 暂无科目，请先在「科目管理」页面添加",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = FluentAmber,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
            selectedSubjectName.isBlank() ->
                Text("如需新增科目，请前往「科目管理」页面",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = FluentMuted,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
        }

        // ── 学生选择摘要行 ────────────────────────────────────────────────
        if (initial != null && state.students.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("班级学生", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("已选 ${selectedStudents.size} 人",
                        style = MaterialTheme.typography.labelMedium,
                        color = FluentBlue, fontWeight = FontWeight.SemiBold)
                }
                TextButton(
                    onClick        = { showStudentPicker = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text("选择", style = MaterialTheme.typography.labelMedium)
                }
            }

            // 已选学生姓名 chips（最多显示 8 个，超出显示 +N）
            if (selectedStudents.isNotEmpty()) {
                val selectedList = state.students.filter { it.id in selectedStudents }
                val displayList  = selectedList.take(8)
                val overflow     = selectedList.size - displayList.size
                androidx.compose.foundation.layout.FlowRow(
                    modifier              = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement   = Arrangement.spacedBy(4.dp)
                ) {
                    displayList.forEach { s -> ColorChip(s.name, FluentBlue) }
                    if (overflow > 0)
                        ColorChip("+$overflow 人", FluentMuted)
                }
            }
        }
    }

    // ── 学生选择 Sheet ────────────────────────────────────────────────────
    if (showStudentPicker && initial != null) {
        StudentPickerSheet(
            allStudents = state.students,
            selected    = selectedStudents,
            onConfirm   = { selectedStudents = it; showStudentPicker = false },
            onDismiss   = { showStudentPicker = false }
        )
    }
}

// ── Student picker bottom sheet ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentPickerSheet(
    allStudents: List<com.school.manager.data.Student>,
    selected: Set<Long>,
    onConfirm: (Set<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    var draft    by remember { mutableStateOf(selected) }
    var fGrade   by remember { mutableStateOf("") }
    var query    by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val filtered = allStudents.filter { s ->
        (fGrade.isBlank() || s.grade == fGrade) &&
        (query.isBlank()  || s.name.contains(query, ignoreCase = true))
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
                Text("选择学生",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                Button(
                    onClick = { onConfirm(draft) },
                    shape   = RoundedCornerShape(10.dp),
                    colors  = ButtonDefaults.buttonColors(containerColor = FluentBlue)
                ) { Text("确定（${draft.size} 人）") }
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
                label         = { Text("搜索姓名") },
                singleLine    = true,
                shape         = RoundedCornerShape(12.dp),
                modifier      = Modifier.fillMaxWidth(),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = FluentBlue,
                    unfocusedBorderColor = FluentBorder
                )
            )

            // ── 全选 / 取消全选 ───────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filteredIds = filtered.map { it.id }.toSet()
                val allChecked  = filteredIds.isNotEmpty() && filteredIds.all { it in draft }
                OutlinedButton(
                    onClick = { draft = if (allChecked) draft - filteredIds else draft + filteredIds },
                    shape   = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) { Text(if (allChecked) "取消全选" else "全选（${filtered.size} 人）") }
                if (draft.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { draft = emptySet() },
                        shape   = RoundedCornerShape(10.dp),
                        colors  = ButtonDefaults.outlinedButtonColors(contentColor = FluentRed),
                        modifier = Modifier.weight(1f)
                    ) { Text("清空已选") }
                }
            }

            // ── 学生列表 ──────────────────────────────────────────────────
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center) {
                    Text("没有符合条件的学生", color = FluentMuted,
                        style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(
                    modifier            = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(filtered, key = { it.id }) { s ->
                        val checked = s.id in draft
                        Row(
                            modifier          = Modifier
                                .fillMaxWidth()
                                .clickable { draft = if (checked) draft - s.id else draft + s.id }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked         = checked,
                                onCheckedChange = { draft = if (checked) draft - s.id else draft + s.id }
                            )
                            Text(s.name,
                                style    = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f))
                            ColorChip(s.grade,
                                if (s.gender == "男") FluentBlue else FluentPurple)
                            ColorChip(s.gender,
                                if (s.gender == "男") FluentBlue else FluentPurple)
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}
