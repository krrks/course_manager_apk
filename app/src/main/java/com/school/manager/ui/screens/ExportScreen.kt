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
    val context      = LocalContext.current
    var toast        by remember { mutableStateOf<String?>(null) }
    var pendingJson  by remember { mutableStateOf<String?>(null) }
    var showImportConfirm by remember { mutableStateOf(false) }
    var importJsonBuffer by remember { mutableStateOf<String?>(null) }

    // ── Export: SAF create file ───────────────────────────────────────────────
    val createFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null && pendingJson != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(pendingJson!!.toByteArray(Charsets.UTF_8))
                }
                toast = "✅ 导出成功"
            } catch (_: Exception) { toast = "❌ 导出失败，请重试" }
            pendingJson = null
        }
    }

    fun exportWith(json: String, filename: String) {
        pendingJson = json
        createFileLauncher.launch(filename)
    }

    // ── Import: SAF open file ─────────────────────────────────────────────────
    val openFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val json = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)?.readText()
                if (json.isNullOrBlank()) {
                    toast = "❌ 文件为空或无法读取"
                } else {
                    importJsonBuffer  = json
                    showImportConfirm = true
                }
            } catch (_: Exception) { toast = "❌ 读取文件失败" }
        }
    }

    // ── Import confirmation dialog ─────────────────────────────────────────────
    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false },
            shape  = RoundedCornerShape(20.dp),
            title  = { Text("确认导入", fontWeight = FontWeight.Bold) },
            text   = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("导入将替换当前全部数据（科目、教师、班级、学生、课表、上课记录）。",
                        style = MaterialTheme.typography.bodyMedium)
                    Text("⚠️ 此操作不可撤销，请确认已备份现有数据。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FluentRed, fontWeight = FontWeight.SemiBold)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showImportConfirm = false
                        val success = vm.importFullState(importJsonBuffer ?: "")
                        importJsonBuffer = null
                        toast = if (success) "✅ 导入成功" else "❌ 数据格式不匹配，导入失败"
                    },
                    shape  = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FluentBlue)
                ) { Text("确认导入") }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = false; importJsonBuffer = null }) {
                    Text("取消", color = FluentMuted)
                }
            }
        )
    }

    // ── Main content ─────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Import section ────────────────────────────────────────────────────
        Text("数据导入", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("从之前导出的完整备份 JSON 文件中恢复全部数据。",
            style = MaterialTheme.typography.bodyMedium, color = FluentMuted)

        IoCard(
            icon     = Icons.Default.FileUpload,
            title    = "从 JSON 文件导入",
            subtitle = "选择由本应用导出的「完整备份」JSON 文件，导入后将覆盖所有现有数据",
            color    = FluentOrange,
            actionLabel = "选择文件"
        ) {
            openFileLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
        }

        HorizontalDivider(color = FluentBorder, modifier = Modifier.padding(vertical = 4.dp))

        // ── Export section ────────────────────────────────────────────────────
        Text("数据导出", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("以 JSON 格式将数据保存到您选择的位置。",
            style = MaterialTheme.typography.bodyMedium, color = FluentMuted)

        IoCard(
            icon     = Icons.Default.Backup,
            title    = "导出完整备份",
            subtitle = "导出所有数据（可再次导入恢复），格式：AppState JSON",
            color    = FluentBlue,
            actionLabel = "导出"
        ) {
            exportWith(vm.exportFullStateJson(), "school_backup.json")
        }

        IoCard(
            icon     = Icons.Outlined.CalendarMonth,
            title    = "导出课表",
            subtitle = "包含所有排课（班级、科目、教师、时间）",
            color    = FluentGreen,
            actionLabel = "导出"
        ) {
            exportWith(vm.exportScheduleJson(), "schedule_export.json")
        }

        IoCard(
            icon     = Icons.Outlined.EventNote,
            title    = "导出上课记录",
            subtitle = "包含所有上课记录（日期、课题、状态、出勤人数）",
            color    = FluentPurple,
            actionLabel = "导出"
        ) {
            exportWith(vm.exportAttendanceJson(), "attendance_export.json")
        }

        // Info card
        Card(
            shape    = RoundedCornerShape(14.dp),
            colors   = CardDefaults.cardColors(containerColor = FluentBlueLight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment     = Alignment.Top
            ) {
                Icon(Icons.Default.Info, null, tint = FluentBlue, modifier = Modifier.size(20.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("使用说明", style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold, color = FluentBlue)
                    Text(
                        "「完整备份」格式支持导入还原。其他格式为可读性导出，适合 Excel 等工具处理，无法再次导入。",
                        style = MaterialTheme.typography.bodyMedium, color = FluentBlue
                    )
                }
            }
        }
    }

    // Toast
    toast?.let { msg ->
        LaunchedEffect(msg) { delay(2500); toast = null }
        Box(
            modifier         = Modifier.fillMaxSize().padding(bottom = 32.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF323232)) {
                Text(msg, color = Color.White,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    style    = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun IoCard(
    icon:        ImageVector,
    title:       String,
    subtitle:    String,
    color:       Color,
    actionLabel: String,
    onClick:     () -> Unit
) {
    FluentCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape    = RoundedCornerShape(12.dp),
                color    = color.copy(alpha = 0.12f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(26.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title,    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium,  color      = FluentMuted)
            }
            Button(
                onClick = onClick,
                shape   = RoundedCornerShape(10.dp),
                colors  = ButtonDefaults.buttonColors(containerColor = color),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(actionLabel, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
