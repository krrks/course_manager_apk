package com.school.manager.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.school.manager.data.*
import com.school.manager.ui.components.*
import com.school.manager.ui.theme.*
import com.school.manager.viewmodel.AppViewModel
import kotlinx.coroutines.delay

@Composable
fun ExportScreen(vm: AppViewModel, onOpenDrawer: () -> Unit = {}) {
    val context = LocalContext.current
    val state   by vm.state.collectAsState()
    var toast   by remember { mutableStateOf<String?>(null) }

    // Import flow: pick file → show preview dialog → user confirms → write to DB
    var pendingBytes  by remember { mutableStateOf<ByteArray?>(null) }
    var importPreview by remember { mutableStateOf<ImportResult?>(null) }

    // Filtered export filter state
    var fTeacherId by remember { mutableLongStateOf(0L) }
    var fClassId   by remember { mutableLongStateOf(0L) }
    var fFromDate  by remember { mutableStateOf("") }
    var fToDate    by remember { mutableStateOf("") }

    val appVersion = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "─"
        }.getOrDefault("─")
    }

    LaunchedEffect(toast) { if (toast != null) { delay(2500); toast = null } }

    // ── SAF launchers ──────────────────────────────────────────────────────
    var pendingFullBytes     by remember { mutableStateOf<ByteArray?>(null) }
    var pendingFilteredBytes by remember { mutableStateOf<ByteArray?>(null) }

    val saveFullLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null && pendingFullBytes != null) {
            runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(pendingFullBytes!!) } }
            toast = "✅ 完整备份已保存"
        }
    }
    val saveFilteredLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null && pendingFilteredBytes != null) {
            runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(pendingFilteredBytes!!) } }
            toast = "✅ 筛选备份已保存"
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val bytes = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()
        if (bytes == null) { toast = "❌ 读取文件失败"; return@rememberLauncherForActivityResult }
        pendingBytes  = bytes
        importPreview = vm.peekImportZip(bytes, context)
    }

    // ── Filtered lesson count (live preview) ───────────────────────────────
    val filteredCount = remember(state.lessons, fTeacherId, fClassId, fFromDate, fToDate) {
        state.lessons.count { l ->
            (fTeacherId == 0L       || l.effectiveTeacherId(state.classes) == fTeacherId) &&
            (fClassId   == 0L       || l.classId == fClassId) &&
            (fFromDate.isBlank()    || l.date >= fFromDate) &&
            (fToDate.isBlank()      || l.date <= fToDate)
        }
    }
    val hasFilter = fTeacherId != 0L || fClassId != 0L || fFromDate.isNotBlank() || fToDate.isNotBlank()

    Scaffold(floatingActionButton = { ScreenSpeedDialFab(onOpenDrawer = onOpenDrawer) }) { inner ->
        Column(
            modifier = Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    top    = inner.calculateTopPadding() + 8.dp,
                    bottom = inner.calculateBottomPadding() + 80.dp,
                    start  = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // ── Full export ───────────────────────────────────────────────
            SectionTitle("📦 完整备份", "全部数据 + 头像，适合换机迁移")
            IoCard(Icons.Default.FolderZip, "导出完整备份", FluentBlue,
                "${state.lessons.size} 节课 · ${state.students.size} 名学生") {
                val bytes = vm.exportFullZip(context)
                if (bytes != null) { pendingFullBytes = bytes; saveFullLauncher.launch("school_backup_full.zip") }
                else toast = "❌ 生成备份失败"
            }

            HorizontalDivider(color = FluentBorder)

            // ── Filtered export ───────────────────────────────────────────
            SectionTitle("🔍 筛选导出", "导出部分数据，不含头像，适合分享归档")

            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (state.teachers.isNotEmpty())
                    DropdownFilterChip("全部教师",
                        items    = state.teachers.map { it.id to it.name },
                        selected = fTeacherId) { fTeacherId = it }
                if (state.classes.isNotEmpty())
                    DropdownFilterChip("全部班级",
                        items    = state.classes.map { it.id to it.name },
                        selected = fClassId) { fClassId = it }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { DatePickerField("开始日期（可选）", fFromDate) { fFromDate = it } }
                Box(Modifier.weight(1f)) { DatePickerField("结束日期（可选）", fToDate)   { fToDate   = it } }
            }

            IoCard(Icons.Default.FilterList, "导出筛选数据", FluentPurple,
                if (hasFilter) "筛选后 $filteredCount 节课" else "请先设置至少一个筛选条件") {
                if (!hasFilter) { toast = "⚠️ 请先设置至少一个筛选条件"; return@IoCard }
                val bytes = vm.exportFilteredZip(
                    context   = context,
                    teacherId = fTeacherId.takeIf { it != 0L },
                    classId   = fClassId.takeIf   { it != 0L },
                    fromDate  = fFromDate.ifBlank { null },
                    toDate    = fToDate.ifBlank   { null }
                )
                if (bytes != null) { pendingFilteredBytes = bytes; saveFilteredLauncher.launch("school_backup_filtered.zip") }
                else toast = "❌ 生成失败"
            }

            HorizontalDivider(color = FluentBorder)

            // ── Import ────────────────────────────────────────────────────
            SectionTitle("📥 导入数据", "选择本应用导出的 ZIP，导入前显示预览")
            IoCard(Icons.Default.FileOpen, "选择 ZIP 文件", FluentAmber, "按 ID 合并，不丢失已有数据") {
                importLauncher.launch(arrayOf("application/zip", "*/*"))
            }

            HorizontalDivider(color = FluentBorder)

            // ── Danger zone ───────────────────────────────────────────────
            SectionTitle("⚠️ 危险操作", "")
            var confirmReset by remember { mutableStateOf(false) }
            if (!confirmReset) {
                IoCard(Icons.Default.DeleteForever, "重置为示例数据", Color(0xFFE53935),
                    "清空所有数据并恢复为预设示例（不可撤销）") { confirmReset = true }
            } else {
                Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFFFEBEE),
                    modifier = Modifier.fillMaxWidth()) {
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

            Spacer(Modifier.height(8.dp))
            Text("智慧课务管理  v$appVersion",
                style = MaterialTheme.typography.bodySmall, color = FluentMuted,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }

        // ── Toast ──────────────────────────────────────────────────────────
        toast?.let { msg ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                Surface(modifier = Modifier.padding(bottom = 100.dp).widthIn(max = 320.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = if (msg.startsWith("✅")) Color(0xFFE8F5E9)
                            else MaterialTheme.colorScheme.errorContainer,
                    shadowElevation = 4.dp) {
                    Text(msg, modifier = Modifier.padding(12.dp),
                        color = if (msg.startsWith("✅")) FluentGreen
                                else MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Medium)
                }
            }
        }
    }

    // ── Import preview dialog ──────────────────────────────────────────────
    importPreview?.let { preview ->
        ImportPreviewDialog(
            result    = preview,
            onDismiss = { importPreview = null; pendingBytes = null },
            onConfirm = {
                pendingBytes?.let { bytes ->
                    toast = if (vm.commitImportZip(bytes, context)) "✅ 导入成功" else "❌ 导入失败"
                }
                importPreview = null; pendingBytes = null
            }
        )
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    if (subtitle.isNotBlank())
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = FluentMuted)
}

@Composable
private fun IoCard(
    icon: ImageVector, title: String, color: Color,
    subtitle: String, onClick: () -> Unit
) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.12f)) {
                Icon(icon, null, tint = color, modifier = Modifier.padding(10.dp).size(24.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                if (subtitle.isNotBlank())
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = FluentMuted)
            }
            Button(onClick = onClick, shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = color)) {
                Text("执行", color = Color.White)
            }
        }
    }
}
