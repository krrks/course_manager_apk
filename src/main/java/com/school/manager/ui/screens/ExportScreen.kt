package com.school.manager.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.school.manager.ui.theme.*
import com.school.manager.viewmodel.AppViewModel

@Composable
fun ExportScreen(vm: AppViewModel) {
    val state   = vm.state.collectAsState().value
    val context = LocalContext.current
    var toast   by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(toast) {
        if (toast != null) {
            Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
            toast = null
        }
    }

    // ── JSON Export launcher ──────────────────────────────────────────────────
    var pendingJson by remember { mutableStateOf<String?>(null) }
    val createFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) try {
            context.contentResolver.openOutputStream(uri)
                ?.use { it.write(pendingJson!!.toByteArray(Charsets.UTF_8)) }
            toast = "✅ 导出成功"
        } catch (_: Exception) { toast = "❌ 导出失败，请重试" }
        pendingJson = null
    }
    fun exportWith(json: String, filename: String) { pendingJson = json; createFileLauncher.launch(filename) }

    // ── JSON Import launcher ──────────────────────────────────────────────────
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

    // ── ZIP Export launcher ───────────────────────────────────────────────────
    var pendingZipBytes by remember { mutableStateOf<ByteArray?>(null) }
    val createZipLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null && pendingZipBytes != null) try {
            context.contentResolver.openOutputStream(uri)
                ?.use { it.write(pendingZipBytes!!) }
            toast = "✅ ZIP 备份导出成功"
        } catch (_: Exception) { toast = "❌ ZIP 导出失败，请重试" }
        pendingZipBytes = null
    }

    // ── ZIP Import launcher ───────────────────────────────────────────────────
    var importZipBuffer by remember { mutableStateOf<ByteArray?>(null) }
    var showZipImportConfirm by remember { mutableStateOf(false) }
    val openZipLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                if (bytes == null || bytes.isEmpty()) toast = "❌ 文件为空或无法读取"
                else { importZipBuffer = bytes; showZipImportConfirm = true }
            } catch (_: Exception) { toast = "❌ 读取 ZIP 文件失败" }
        }
    }

    // ── Teacher filter for partial export ────────────────────────────────────
    var selectedTeacherId by remember { mutableStateOf(0L) }
    val selectedTeacher   = state.teachers.firstOrNull { it.id == selectedTeacherId }

    // ── JSON Import confirmation dialog ───────────────────────────────────────
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

    // ── ZIP Import confirmation dialog ────────────────────────────────────────
    if (showZipImportConfirm) {
        AlertDialog(
            onDismissRequest = { showZipImportConfirm = false },
            shape  = RoundedCornerShape(20.dp),
            title  = { Text("确认导入 ZIP 备份", fontWeight = FontWeight.Bold) },
            text   = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("将从 ZIP 备份中恢复所有数据（包含头像图片）。按 ID 合并：相同 ID 更新，新 ID 追加，原有记录不删除。",
                        style = MaterialTheme.typography.bodyMedium)
                    Text("⚠️ 如需完全覆盖，请先在系统设置中清除应用数据。",
                        style = MaterialTheme.typography.bodySmall, color = FluentOrange)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showZipImportConfirm = false
                        val bytes = importZipBuffer
                        importZipBuffer = null
                        if (bytes != null) {
                            val ok = vm.importFullBackupZip(context, bytes)
                            toast = if (ok) "✅ ZIP 备份恢复成功" else "❌ ZIP 格式不匹配，导入失败"
                        }
                    },
                    shape  = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FluentBlue)
                ) { Text("恢复备份") }
            },
            dismissButton = {
                TextButton(onClick = { showZipImportConfirm = false; importZipBuffer = null }) {
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

        // ════════════════════════════════════════════════════════════════════
        // ZIP 备份（推荐）
        // ════════════════════════════════════════════════════════════════════
        Text("📦 ZIP 完整备份（推荐）",
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("将课表、记录、教师、学生及所有头像图片打包成一个 ZIP 文件，可在新设备还原。",
            style = MaterialTheme.typography.bodyMedium, color = FluentMuted)

        IoCard(
            icon = Icons.Default.FolderZip, title = "导出 ZIP 完整备份", color = FluentBlue,
            subtitle = "包含全部数据 + 头像图片，格式：ZIP（state.json + avatars/）",
            actionLabel = "导出"
        ) {
            val bytes = vm.exportFullBackupZip(context)
            if (bytes != null) { pendingZipBytes = bytes; createZipLauncher.launch("school_backup.zip") }
            else toast = "❌ 生成备份失败"
        }

        IoCard(
            icon = Icons.Default.Unarchive, title = "导入 ZIP 完整备份", color = FluentOrange,
            subtitle = "选择由本应用导出的 ZIP 备份，按 ID 合并恢复数据与图片",
            actionLabel = "选择文件"
        ) { openZipLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) }

        HorizontalDivider(color = FluentBorder, modifier = Modifier.padding(vertical = 4.dp))

        // ════════════════════════════════════════════════════════════════════
        // JSON 导入（仅数据，不含图片）
        // ════════════════════════════════════════════════════════════════════
        Text("导入（JSON）", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("按 ID 合并导入备份 JSON 文件：已存在记录更新，新记录追加，不删除原有数据。",
            style = MaterialTheme.typography.bodyMedium, color = FluentMuted)
        IoCard(
            icon = Icons.Default.FileUpload, title = "合并导入 JSON", color = FluentGreen,
            subtitle = "选择由本应用导出的完整备份 JSON，按 ID 合并，不覆盖不相关记录",
            actionLabel = "选择文件"
        ) { openFileLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }

        HorizontalDivider(color = FluentBorder, modifier = Modifier.padding(vertical = 4.dp))

        // ════════════════════════════════════════════════════════════════════
        // 按教师筛选导出
        // ════════════════════════════════════════════════════════════════════
        Text("按教师筛选导出", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("导出课表或上课记录时可选择仅导出某位教师的数据",
            style = MaterialTheme.typography.bodyMedium, color = FluentMuted)

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
            items(state.teachers) { t ->
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

        // ════════════════════════════════════════════════════════════════════
        // JSON 导出（数据明细）
        // ════════════════════════════════════════════════════════════════════
        Text("导出（JSON）", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("以 JSON 格式将数据保存到您选择的位置。",
            style = MaterialTheme.typography.bodyMedium, color = FluentMuted)

        IoCard(
            icon = Icons.Default.Backup, title = "导出完整备份 JSON", color = FluentBlue,
            subtitle = "导出所有数据（可再次合并导入），格式：AppState JSON（不含图片）",
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

        Spacer(Modifier.height(32.dp))
    }
}

// ── Shared UI helper ──────────────────────────────────────────────────────────

@Composable
private fun IoCard(
    icon: ImageVector,
    title: String,
    color: androidx.compose.ui.graphics.Color,
    subtitle: String,
    actionLabel: String,
    onClick: () -> Unit
) {
    Card(
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, null, tint = color,
                modifier = Modifier.size(32.dp).padding(top = 2.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = FluentMuted)
                Button(
                    onClick = onClick,
                    shape  = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = color),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) { Text(actionLabel) }
            }
        }
    }
}
