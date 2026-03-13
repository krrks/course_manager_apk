package com.school.manager.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.school.manager.ui.components.*
import com.school.manager.ui.theme.*
import com.school.manager.viewmodel.AppViewModel
import kotlinx.coroutines.delay

// ── 版本常量（与 build.gradle.kts 保持同步）────────────────────────────────
private const val APP_VERSION = "1.5.0"

@Composable
fun ExportScreen(vm: AppViewModel, onOpenDrawer: () -> Unit = {}) {
    val context  = LocalContext.current
    val state    by vm.state.collectAsState()
    var toast    by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(toast) { if (toast != null) { delay(2500); toast = null } }

    var pendingJson  by remember { mutableStateOf<String?>(null) }
    var jsonFilename by remember { mutableStateOf("school_backup.json") }
    val createFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null && pendingJson != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { it.write(pendingJson!!.toByteArray()) }
                toast = "✅ 已导出 JSON"
            } catch (_: Exception) { toast = "❌ 导出失败" }
        }
    }
    fun exportWith(json: String, filename: String) { pendingJson = json; createFileLauncher.launch(filename) }

    val openFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: ""
                toast = if (vm.importMerge(json)) "✅ 导入成功" else "❌ 导入失败：格式错误"
            } catch (_: Exception) { toast = "❌ 读取文件失败" }
        }
    }

    var pendingZipBytes by remember { mutableStateOf<ByteArray?>(null) }
    var zipFilename     by remember { mutableStateOf("school_backup.zip") }
    val createZipLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null && pendingZipBytes != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { it.write(pendingZipBytes!!) }
                toast = "✅ ZIP 备份已保存"
            } catch (_: Exception) { toast = "❌ 保存失败" }
        }
    }
    val openZipLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@rememberLauncherForActivityResult
                toast = if (vm.importFullBackupZip(context, bytes)) "✅ ZIP 恢复成功" else "❌ 恢复失败"
            } catch (_: Exception) { toast = "❌ 读取文件失败" }
        }
    }

    var selectedTeacherId by remember { mutableLongStateOf(0L) }
    val selectedTeacher = state.teachers.firstOrNull { it.id == selectedTeacherId }

    Scaffold(
        floatingActionButton = {
            ScreenSpeedDialFab(onOpenDrawer = onOpenDrawer)
        }
    ) { inner ->
        Column(
            modifier = Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = inner.calculateTopPadding() + 8.dp,
                         bottom = inner.calculateBottomPadding() + 80.dp,
                         start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── ZIP backup ───────────────────────────────────────────────────
            Text("📦 ZIP 完整备份（推荐）",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("将课表、记录、教师、学生及所有头像图片打包成一个 ZIP 文件，可在新设备还原。",
                style = MaterialTheme.typography.bodyMedium, color = FluentMuted)
            FilenameField("备份文件名", zipFilename, "school_backup.zip") {
                zipFilename = it.trim().ifBlank { "school_backup.zip" }
            }
            IoCard(Icons.Default.FolderZip, "导出 ZIP 完整备份", FluentBlue,
                "包含全部数据 + 头像图片（state.json + avatars/）") {
                val bytes = vm.exportFullBackupZip(context)
                if (bytes != null) { pendingZipBytes = bytes; createZipLauncher.launch(zipFilename) }
                else toast = "❌ 生成备份失败"
            }
            IoCard(Icons.Default.Unarchive, "导入 ZIP 完整备份", FluentOrange,
                "选择本应用导出的 ZIP 文件，按 ID 合并恢复数据与头像") {
                openZipLauncher.launch(arrayOf("application/zip","application/octet-stream","*/*"))
            }

            HorizontalDivider(color = FluentBorder)

            // ── JSON backup ──────────────────────────────────────────────────
            Text("🗂 JSON 数据备份",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("仅导出结构化数据（不含头像图片），体积小，方便查阅和编辑。",
                style = MaterialTheme.typography.bodyMedium, color = FluentMuted)
            FilenameField("JSON 文件名", jsonFilename, "school_backup.json") {
                jsonFilename = it.trim().ifBlank { "school_backup.json" }
            }
            IoCard(Icons.Default.Backup, "导出完整数据（JSON）", FluentBlue,
                "全部科目、教师、班级、学生、课表、记录") {
                exportWith(vm.exportFullStateJson(), jsonFilename)
            }

            if (state.teachers.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("按教师筛选：", style = MaterialTheme.typography.bodySmall, color = FluentMuted)
                    DropdownFilterChip("全部",
                        items    = state.teachers.map { it.id to it.name },
                        selected = selectedTeacherId) { selectedTeacherId = it }
                }
            }

            IoCard(Icons.Outlined.CalendarMonth, "导出课表（JSON）", FluentGreen,
                if (selectedTeacher != null) "筛选：${selectedTeacher.name}" else "全部课表") {
                exportWith(vm.exportScheduleJson(selectedTeacherId.takeIf { it != 0L }),
                    jsonFilename.replace(".json","_schedule.json"))
            }
            IoCard(Icons.AutoMirrored.Outlined.EventNote, "导出上课记录（JSON）", FluentPurple,
                if (selectedTeacher != null) "筛选：${selectedTeacher.name}" else "全部记录") {
                exportWith(vm.exportAttendanceJson(selectedTeacherId.takeIf { it != 0L }),
                    jsonFilename.replace(".json","_attendance.json"))
            }

            HorizontalDivider(color = FluentBorder)

            // ── Import JSON ──────────────────────────────────────────────────
            Text("📥 导入数据",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            IoCard(Icons.Default.FileOpen, "导入 JSON 数据", FluentAmber,
                "选择本应用导出的 JSON 文件，按 ID 合并") {
                openFileLauncher.launch(arrayOf("application/json","text/plain","*/*"))
            }

            HorizontalDivider(color = FluentBorder)

            // ── Danger zone ──────────────────────────────────────────────────
            Text("⚠️ 危险操作",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            var confirmReset by remember { mutableStateOf(false) }
            if (!confirmReset) {
                IoCard(Icons.Default.DeleteForever, "重置为示例数据", Color(0xFFE53935),
                    "清空所有数据并恢复为预设示例（不可撤销）") { confirmReset = true }
            } else {
                Surface(shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFFEBEE), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("确定要清空所有数据吗？此操作不可撤销。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFB71C1C), fontWeight = FontWeight.Medium)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = { confirmReset = false },
                                modifier = Modifier.weight(1f)) { Text("取消") }
                            Button(onClick = { vm.resetToSampleData(); confirmReset = false },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))) {
                                Text("确认重置", color = Color.White)
                            }
                        }
                    }
                }
            }

            // ── 版本信息 ─────────────────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            Text(
                text = "智慧课务管理  v$APP_VERSION",
                style = MaterialTheme.typography.bodySmall,
                color = FluentMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ── Toast ────────────────────────────────────────────────────────────
        toast?.let { msg ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                Surface(modifier = Modifier.padding(bottom = 100.dp).widthIn(max = 320.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = if (msg.startsWith("✅")) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.errorContainer,
                    shadowElevation = 4.dp) {
                    Text(msg, modifier = Modifier.padding(12.dp),
                        color = if (msg.startsWith("✅")) FluentGreen else MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun FilenameField(label: String, value: String, placeholder: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) },
        placeholder = { Text(placeholder, color = FluentMuted) }, singleLine = true,
        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FluentBlue, unfocusedBorderColor = FluentBorder))
}

@Composable
private fun IoCard(icon: ImageVector, title: String,
                   color: Color, subtitle: String, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.12f)) {
                Icon(icon, null, tint = color, modifier = Modifier.padding(10.dp).size(24.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = FluentMuted)
            }
            Button(onClick = onClick, shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = color)) {
                Text("执行", color = Color.White) }
        }
    }
}
