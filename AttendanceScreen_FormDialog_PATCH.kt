// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 文件: AttendanceScreen.kt
// 操作: 以下内容替换文件中的 AttendanceFormDialog 整个函数
//       (从 "@Composable internal fun AttendanceFormDialog" 到其结束 "}")
//       文件其余所有代码保持不变，imports 也保持不变。
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
internal fun AttendanceFormDialog(
    title: String, initial: Attendance?,
    state: AppState, vm: AppViewModel,
    onDismiss: () -> Unit, onSave: (Attendance) -> Unit
) {
    var className   by remember { mutableStateOf(state.classes.firstOrNull { it.id == initial?.classId }?.name ?: "") }
    var teacherName by remember { mutableStateOf(state.teachers.firstOrNull { it.id == initial?.teacherId }?.name ?: "") }
    var date        by remember { mutableStateOf(initial?.date ?: LocalDate.now().toString()) }
    var startTime   by remember { mutableStateOf(initial?.resolvedStart() ?: "08:00") }
    var endTime     by remember { mutableStateOf(initial?.resolvedEnd()   ?: "08:45") }
    var topic       by remember { mutableStateOf(initial?.topic  ?: "") }
    var status      by remember { mutableStateOf(initial?.status ?: "completed") }
    var notes       by remember { mutableStateOf(initial?.notes  ?: "") }
    var attendees   by remember { mutableStateOf<List<Long>>(initial?.attendees ?: emptyList()) }
    var code        by remember { mutableStateOf(initial?.code?.ifBlank { null } ?: genCode("ATT")) }

    val selectedClass  = state.classes.firstOrNull { it.name == className }
    val classStudents  = state.students.filter { s -> s.classIds.contains(selectedClass?.id) }

    FluentDialog(title = title, onDismiss = onDismiss, onConfirm = {
        val cls  = state.classes.firstOrNull { it.name == className } ?: return@FluentDialog
        val sId  = state.subjects.firstOrNull { it.name == cls.subject }?.id
                   ?: initial?.subjectId ?: state.subjects.firstOrNull()?.id ?: 1L
        val tId  = state.teachers.firstOrNull { it.name == teacherName }?.id
        val newId = if (initial != null && initial.id != 0L) initial.id else System.currentTimeMillis()
        onSave(Attendance(newId, cls.id, sId, tId, date, 0, startTime, endTime,
                          topic, status, notes, attendees, code.trim().ifBlank { genCode("ATT") }))
    }) {

        // ── 行1：编号(1/2) + 状态(1/2) ───────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(Modifier.weight(1f)) {
                FormTextField("编号", code, { code = it }, "自动生成")
            }
            Box(Modifier.weight(1f)) {
                FormDropdown("状态", status, listOf("completed", "cancelled", "pending")) { status = it }
            }
        }

        // ── 行2：班级(1/2) + 教师(1/2) ───────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(Modifier.weight(1f)) {
                FormDropdown("班级", className, state.classes.map { it.name }) {
                    className = it; attendees = emptyList()
                }
            }
            Box(Modifier.weight(1f)) {
                FormDropdown("教师", teacherName, listOf("") + state.teachers.map { it.name }) { teacherName = it }
            }
        }

        // 科目 badge
        if (selectedClass?.subject?.isNotBlank() == true) {
            Surface(shape = RoundedCornerShape(8.dp), color = FluentPurple.copy(alpha = 0.1f)) {
                Text(
                    "科目：${selectedClass.subject}",
                    style      = MaterialTheme.typography.bodySmall,
                    color      = FluentPurple,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        // ── 行3：日期(1/2) + 开始时间(1/2) ───────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(Modifier.weight(1f)) {
                DatePickerField("日期", date) { date = it }
            }
            Box(Modifier.weight(1f)) {
                StartTimeField(startTime) { newStart ->
                    val dur = run {
                        val sm = startTime.split(":").let { p -> (p.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (p.getOrNull(1)?.toIntOrNull() ?: 0) }
                        val em = endTime.split(":").let   { p -> (p.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (p.getOrNull(1)?.toIntOrNull() ?: 0) }
                        (em - sm).coerceAtLeast(30)
                    }
                    startTime = newStart
                    val base  = newStart.split(":").let { p -> (p.getOrNull(0)?.toIntOrNull() ?: 8) * 60 + (p.getOrNull(1)?.toIntOrNull() ?: 0) }
                    val total = (base + dur).coerceIn(0, 23 * 60 + 59)
                    endTime   = "%02d:%02d".format(total / 60, total % 60)
                }
            }
        }

        // ── 课时时长（全宽紧凑 chips）────────────────────────────────────
        InlineDurationPicker(startTime = startTime, endTime = endTime) { endTime = it }

        // ── 课题 ─────────────────────────────────────────────────────────
        FormTextField("课题", topic, { topic = it }, "本节课主题")

        // ── 出勤学生 ─────────────────────────────────────────────────────
        if (classStudents.isNotEmpty()) {
            SectionHeader("出勤学生")
            androidx.compose.foundation.layout.FlowRow(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                classStudents.forEach { s ->
                    val on = attendees.contains(s.id)
                    FilterChip(selected = on, onClick = {
                        attendees = if (on) attendees.filter { it != s.id } else attendees + s.id
                    }, label = { Text(s.name) })
                }
            }
        }

        // ── 备注 ─────────────────────────────────────────────────────────
        FormTextField("备注", notes, { notes = it }, "可选")
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 同时在文件顶部 imports 区域（已有的 import 行之后）补充：
//
//   import com.school.manager.ui.screens.StartTimeField
//   import com.school.manager.ui.screens.InlineDurationPicker
//
// 如果编译器提示 "unresolved reference"，说明 internal 跨文件可见性
// 需要在 AttendanceScreen.kt 顶部手动加上这两行 import。
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
