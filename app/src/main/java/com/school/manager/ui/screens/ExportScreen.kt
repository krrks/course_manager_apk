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

    LaunchedEffect(toast) {
        if (toast != null) { delay(2500); toast = null }
    }

    // ── JSON Export ────────────────────────────────────────────────────────────
    var pendingJson     by remember { mutableStateOf<String?>(null) }
    var jsonFilename    by remember { mutableStateOf("school_backup.json") }
    val createFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null && pendingJson != null) {
            try {
                context.contentResolver.openOutputStream(uri)
                    ?.use { it.write(pendingJson!!.toByteArray(Charsets.UTF_8)) }
                toast = "✅ JSON 导出成功"
            } catch (_: Exception) { toast = "❌ 导出失败，请重试" }
            pendingJson = null
        }
    }
    fun exportWith(json: String, filename: String) { pendingJson = json; createFileLauncher.launch(filename) }

    // ── JSON Import ────────────────────────────────────────────────────────────
    var importJsonBuffer  by remember { mutableStateOf<String?>(null) }
    var showImportConfirm by remember { mutableStateOf(false) }
    val openFileLauncher  = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val json = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)?.readText()
                if (json.isNullOrBlank()) toast = "❌ 文件为空或无法读取"
                else { importJsonBuffer = json; showImportConfirm = true }
            } catch (_: Exception) { toast = "❌ 读取文件失败" }
        }
    }

    // ── ZIP Export ─────────────────────────────────────────────────────────────
    var pendingZipBytes by remember { mutableStateOf<ByteArray?>(null) }
    var zipFilename     by remember { mutableStateOf("school_backup.zip") }
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

    // ── ZIP Import ─────────────────────────────────────────────────────────────
    var importZipBuffer     by remember { mutableStateOf<ByteArray?>(null) }
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

    // ── Teacher filter for partial JSON export ─────────────────────────────────
    var selectedTeacherId by remember { mutableStateOf(0L) }
    val selectedTeacher   = state.teachers.firstOrNull { it.id == selectedTeacherId }

    // ── JSON Import confirmation ───────────────────────────────────────────────
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
                Button(onClick = {
                    showImportConfirm = false
                    val ok = vm.mergeImport(importJsonBuffer ?: "")
                    importJsonBuffer = null
                    toast = if (ok) "✅ 合并导入成功" else "❌ 数据格式不匹配，导入失败"
                }, shape = RoundedCornerShape(12.dp),
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

    // ── ZIP Import confirmation ────────────────────────────────────────────────
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
                Button(onClick = {
                    showZipImportConfirm = false
                    val bytes = importZipBuffer; importZipBuffer = null
                    if (bytes != null) {
                        val ok = vm.importFullBackupZip(context, bytes)
                        toast = if (ok) "✅ ZIP 备份恢复成功" else "❌ ZIP 格式不匹配，导入失败"
                    }
                }, shape = RoundedCornerShape(12.dp),
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

    // ── Main content ───────────────────────────────────────────────────────────
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        // ════════════════════════════════════════════════════════════════════
        // ZIP 完整备份（推荐）
        // ════════════════════════════════════════════════════════════════════
        Text("📦 ZIP 完整备份（推荐）",
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("将课表、记录、教师、学生及所有头像图片打包成一个 ZIP 文件，可在新设备还原。",
            style = MaterialTheme.typography.bodyMedium, color = FluentMuted)

        // 文件名输入
        FilenameField("备份文件名", zipFilename, "school_backup.zip") { zipFilename = it.trim().ifBlank { "school_backup.zip" } }

        IoCard(
            icon = Icons.Default.FolderZip, title = "导出 ZIP 完整备份", color = FluentBlue,
            subtitle = "包含全部数据 + 头像图片（state.json + avatars/）"
        ) {
            val bytes = vm.exportFullBackupZip(context)
            if (bytes != null) { pendingZipBytes = bytes; createZipLauncher.launch(zipFilename) }
            else toast = "❌ 生成备份失败"
        }

        IoCard(
            icon = Icons.Default.Unarchive, title = "导入 ZIP 完整备份", color = FluentOrange,
            subtitle = "选择本应用导出的 ZIP 文件，按 ID 合并恢复数据与头像"
        ) { openZipLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) }

        HorizontalDivider(color = FluentBorder)

        // ════════════════════════════════════════════════════════════════════
        // JSON 纯数据导出
        // ════════════════════════════════════════════════════════════════════
        Text("🗂 JSON 数据备份",
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("仅导出结构化数据（不含头像图片），体积小，方便查阅和编辑。",
            style = MaterialTheme.typography.bodyMedium, color = FluentMuted)

        FilenameField("JSON 文件名", jsonFilename, "school_backup.json") { jsonFilename = it.trim().ifBlank { "school_backup.json" } }

        IoCard(
            icon = Icons.Default.Backup, title = "导出完整数据（JSON）", color = FluentBlue,
            subtitle = "全部科目、教师、班级、学生、课表、记录"
        ) { exportWith(vm.exportFullStateJson(), jsonFilename) }

        // 按教师筛选
        if (state.teachers.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("按教师筛选：", style = MaterialTheme.typography.bodySmall, color = FluentMuted)
                DropdownFilterChipLong(
                    allLabel = "全部",
                    items = state.teachers.map { it.id to it.name },
                    selected = selectedTeacherId
                ) { selectedTeacherId = it }
            }
        }

        IoCard(
            icon = Icons.Outlined.CalendarMonth, title = "导出课表（JSON）", color = FluentGreen,
            subtitle = if (selectedTeacher != null) "筛选：${selectedTeacher.name}" else "全部课表"
        ) { exportWith(vm.exportScheduleJson(selectedTeacherId.takeIf { it != 0L }), jsonFilename.replace(".json","_schedule.json")) }

        IoCard(
            icon = Icons.Outlined.EventNote, title = "导出上课记录（JSON）", color = FluentPurple,
            subtitle = if (selectedTeacher != null) "筛选：${selectedTeacher.name}" else "全部记录"
        ) { exportWith(vm.exportAttendanceJson(selectedTeacherId.takeIf { it != 0L }), jsonFilename.replace(".json","_attendance.json")) }

        IoCard(
            icon = Icons.Default.Upload, title = "导入 JSON 数据", color = FluentOrange,
            subtitle = "选择本应用导出的 JSON 文件，按 ID 合并"
        ) { openFileLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }

        // toast banner
        toast?.let { msg ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (msg.startsWith("✅")) FluentGreen.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(msg, modifier = Modifier.padding(12.dp),
                    color = if (msg.startsWith("✅")) FluentGreen else MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Medium)
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

// ── Shared helpers ─────────────────────────────────────────────────────────────

@Composable
private fun FilenameField(label: String, value: String, placeholder: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = { Text(label) },
        placeholder   = { Text(placeholder, color = FluentMuted) },
        singleLine    = true,
        shape         = RoundedCornerShape(12.dp),
        modifier      = Modifier.fillMaxWidth(),
        colors        = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = FluentBlue,
            unfocusedBorderColor = FluentBorder
        )
    )
}

@Composable
private fun IoCard(
    icon: ImageVector,
    title: String,
    color: Color,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        shape           = RoundedCornerShape(16.dp),
        color           = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier        = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier             = Modifier.padding(16.dp),
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.12f)) {
                Icon(icon, null, tint = color,
                    modifier = Modifier.padding(10.dp).size(24.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = FluentMuted)
            }
            Button(
                onClick = onClick,
                shape   = RoundedCornerShape(10.dp),
                colors  = ButtonDefaults.buttonColors(containerColor = color)
            ) { Text("执行", color = Color.White) }
        }
    }
}

@Composable
private fun DropdownFilterChipLong(
    allLabel: String,
    items: List<Pair<Long, String>>,
    selected: Long,
    onSelect: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val label = items.firstOrNull { it.first == selected }?.second ?: allLabel
    androidx.compose.material3.Box {
        FilterChip(selected = selected != 0L, onClick = { expanded = true }, label = { Text(label) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, Modifier.size(16.dp)) })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(allLabel) }, onClick = { onSelect(0L); expanded = false })
            items.forEach { (id, name) ->
                DropdownMenuItem(text = { Text(name) }, onClick = { onSelect(id); expanded = false })
            }
        }
    }
}
