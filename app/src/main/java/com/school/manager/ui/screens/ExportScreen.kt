package com.school.manager.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.school.manager.ui.components.*
import com.school.manager.ui.theme.*
import com.school.manager.viewmodel.AppViewModel
import kotlinx.coroutines.delay

@Composable
fun ExportScreen(vm: AppViewModel) {
    val context  = LocalContext.current
    val state    by vm.state.collectAsState()
    var toast    by remember { mutableStateOf<String?>(null) }

    // ── Pending export ────────────────────────────────────────────────────────
    var pendingJson by remember { mutableStateOf<String?>(null) }
    val createFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null && pendingJson != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { it.write(pendingJson!!.toByteArray(Charsets.UTF_8)) }
                toast = "✅ 导出成功"
            } catch (_: Exception) { toast = "❌ 导出失败，请重试" }
            pendingJson = null
        }
    }
    fun exportWith(json: String, filename: String) { pendingJson = json; createFileLauncher.launch(filename) }

    // ── Import ────────────────────────────────────────────────────────────────
    var importJsonBuffer by remember { mutableStateOf<String?>(null) }
    var showImportConfirm by remember { mutableStateOf(false) }
    val openFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val json = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)?.readText()
                if (json.isNullOrBlank()) toast = "❌ 文件为空或无法读取"
                else { importJsonBuffer = json; showImportConfirm = true }
            } catch (_: Exception) { toast = "❌ 读取文件失败" }
        }
    }

    // ── Teacher filter for partial export ────────────────────────────────────
    var selectedTeacherId by remember { mutableStateOf(0L) }
    val selectedTeacher   = state.teachers.firstOrNull { it.id == selectedTeacherId }

    // ── Import confirmation dialog ────────────────────────────────────────────
    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false },
            shape  = RoundedCornerShape(20.dp),
            title  = { Text("确认合并导入", fontWeight = FontWeight.Bold) },
            text   = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("将按记录 ID 合并导入：相同 ID 的记录将被更新，新 ID 的记录将被追加。现有记录不会被删除。",
                        style = MaterialTheme.typography.bodyMedium)
                    Text("⚠️ 如需完全恢复，请先清空应用数据后再导入。",
                        style = MaterialTheme.typography.bodySmall, color = FluentOrange)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showImportConfirm = false
                        val ok = vm.mergeImport(importJsonBuffer ?: "")
                        importJsonBuffer = null
                        toast = if (ok) "✅ 合并导入成功" else "❌ 数据格式不匹配，导入失败"
                    },
                    shape  = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FluentBlue)
                ) { Text("合并导入") }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = false; importJsonBuffer = null }) {
                    Text("取消", color = FluentMuted)
                }
            }
        )
    }

    // ── Main content ──────────────────────────────────────────────────────────
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Import section ────────────────────────────────────────────────────
        Text("导入", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("按 ID 合并导入备份 JSON 文件：已存在记录更新，新记录追加，不删除原有数据。",
            style = MaterialTheme.typography.bodyMedium, color = FluentMuted)
        IoCard(
            icon = Icons.Default.FileUpload, title = "合并导入", color = FluentOrange,
            subtitle = "选择由本应用导出的完整备份 JSON，按 ID 合并，不覆盖不相关记录",
            actionLabel = "选择文件"
        ) { openFileLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }

        HorizontalDivider(color = FluentBorder, modifier = Modifier.padding(vertical = 4.dp))

        // ── Teacher filter ────────────────────────────────────────────────────
        Text("按教师筛选导出", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("导出课表或上课记录时可选择仅导出某位教师的数据",
            style = MaterialTheme.typography.bodyMedium, color = FluentMuted)

        // Teacher selector chips
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedTeacherId == 0L,
                    onClick  = { selectedTeacherId = 0L },
                    label    = { Text("全部教师") }
                )
            }
            items(state.teachers.size) { idx ->
                val t = state.teachers[idx]
                FilterChip(
                    selected = selectedTeacherId == t.id,
                    onClick  = { selectedTeacherId = t.id },
                    label    = { Text(t.name) }
                )
            }
        }

        if (selectedTeacher != null) {
            Card(
                shape  = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = FluentGreenLight)
            ) {
                Text("已选教师：${selectedTeacher.name}${if (selectedTeacher.code.isNotBlank()) "（${selectedTeacher.code}）" else ""}",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium, color = FluentGreen,
                    fontWeight = FontWeight.SemiBold)
            }
        }

        HorizontalDivider(color = FluentBorder, modifier = Modifier.padding(vertical = 4.dp))

        // ── Export section ────────────────────────────────────────────────────
        Text("导出", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("以 JSON 格式将数据保存到您选择的位置。",
            style = MaterialTheme.typography.bodyMedium, color = FluentMuted)

        IoCard(
            icon = Icons.Default.Backup, title = "导出完整备份", color = FluentBlue,
            subtitle = "导出所有数据（可再次合并导入），格式：AppState JSON",
            actionLabel = "导出"
        ) { exportWith(vm.exportFullStateJson(), "school_backup.json") }

        val scheduleLabel = if (selectedTeacher != null) "导出课表（${selectedTeacher.name}）" else "导出课表（全部）"
        IoCard(
            icon = Icons.Outlined.CalendarMonth, title = scheduleLabel, color = FluentGreen,
            subtitle = "包含排课信息（班级、科目、教师、时间）",
            actionLabel = "导出"
        ) {
            val tag = selectedTeacher?.name?.replace(" ", "_") ?: "all"
            exportWith(vm.exportScheduleJson(selectedTeacherId.takeIf { it != 0L }),
                "schedule_${tag}.json")
        }

        val attendLabel = if (selectedTeacher != null) "导出上课记录（${selectedTeacher.name}）" else "导出上课记录（全部）"
        IoCard(
            icon = Icons.Outlined.EventNote, title = attendLabel, color = FluentPurple,
            subtitle = "包含上课记录（日期、课题、状态、出勤人数）",
            actionLabel = "导出"
        ) {
            val tag = selectedTeacher?.name?.replace(" ", "_") ?: "all"
            exportWith(vm.exportAttendanceJson(selectedTeacherId.takeIf { it != 0L }),
                "attendance_${tag}.json")
        }

        // Info card
        Card(
            shape  = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = FluentBlueLight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Info, null, tint = FluentBlue, modifier = Modifier.size(20.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("使用说明", style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold, color = FluentBlue)
                    Text("「完整备份」格式支持按 ID 合并导入。课表/上课记录导出为可读性 JSON，适合 Excel 等工具处理，无法再次导入。",
                        style = MaterialTheme.typography.bodyMedium, color = FluentBlue)
                }
            }
        }
    }

    // Toast
    toast?.let { msg ->
        LaunchedEffect(msg) { delay(2500); toast = null }
        Box(modifier = Modifier.fillMaxSize().padding(bottom = 32.dp),
            contentAlignment = Alignment.BottomCenter) {
            Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF323232)) {
                Text(msg, color = Color.White,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun IoCard(
    icon: ImageVector, title: String, subtitle: String,
    color: Color, actionLabel: String, onClick: () -> Unit
) {
    FluentCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.12f),
                modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(26.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title,    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium,  color = FluentMuted)
            }
            Button(onClick = onClick, shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = color),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
                Text(actionLabel, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
