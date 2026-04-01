package com.school.manager.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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

// ── Section card component ────────────────────────────────────────────────────

@Composable
private fun SectionCard(
    icon: ImageVector, iconColor: Color, title: String, subtitle: String,
    badge: String? = null, actionLabel: String = "执行",
    enabled: Boolean = true, onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(shape = RoundedCornerShape(12.dp), color = iconColor.copy(alpha = 0.12f)) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.padding(10.dp).size(24.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                    if (badge != null) {
                        Surface(shape = RoundedCornerShape(20.dp), color = iconColor.copy(alpha = 0.12f)) {
                            Text(badge, style = MaterialTheme.typography.labelSmall,
                                color = iconColor, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
                if (subtitle.isNotBlank())
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = FluentMuted)
            }
            Button(onClick = onClick, enabled = enabled, shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = iconColor)) {
                Text(actionLabel, color = Color.White)
            }
        }
    }
}

// ── Main screen ───────────────────────────────────────────────────────────────

@Composable
fun ExportScreen(
    vm: AppViewModel,
    onOpenDrawer: () -> Unit = {},
    onNavigateToGitHubSync: () -> Unit = {}
) {
    val context = LocalContext.current
    val state   by vm.state.collectAsState()
    var toast   by remember { mutableStateOf<String?>(null) }

    var pendingBytes  by remember { mutableStateOf<ByteArray?>(null) }
    var importPreview by remember { mutableStateOf<ImportResult?>(null) }
    var pendingFullBytes     by remember { mutableStateOf<ByteArray?>(null) }
    var pendingFilteredBytes by remember { mutableStateOf<ByteArray?>(null) }

    val appVersion = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "─" }.getOrDefault("─")
    }

    LaunchedEffect(toast) { if (toast != null) { delay(2500); toast = null } }

    // ── SAF launchers ─────────────────────────────────────────────────────────
    val saveFullLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null && pendingFullBytes != null) {
            runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(pendingFullBytes!!) } }
            toast = "✅ 完整备份已保存"; pendingFullBytes = null
        }
    }
    val saveFilteredLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null && pendingFilteredBytes != null) {
            runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(pendingFilteredBytes!!) } }
            toast = "✅ 课次备份已保存"; pendingFilteredBytes = null
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val bytes = runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
        if (bytes == null) { toast = "❌ 读取文件失败"; return@rememberLauncherForActivityResult }
        pendingBytes = bytes; importPreview = vm.peekImportZip(bytes, context)
    }

    val lessonCount   = state.lessons.size
    val kpCount       = state.knowledgePoints.size
    val customKpCount = state.knowledgePoints.count { it.isCustom }

    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(floatingActionButton = { ScreenSpeedDialFab(onOpenDrawer = onOpenDrawer) }) { inner ->
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(top = inner.calculateTopPadding() + 8.dp, bottom = inner.calculateBottomPadding() + 80.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Summary ───────────────────────────────────────────────────────
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBadge("📚", "$lessonCount 节课",     FluentBlue,   Modifier.weight(1f))
                StatBadge("💡", "$kpCount 知识点",       FluentPurple, Modifier.weight(1f))
                StatBadge("✏️", "$customKpCount 自定义", FluentAmber,  Modifier.weight(1f))
            }

            HorizontalDivider(color = FluentBorder)

            // ── GitHub 同步 ───────────────────────────────────────────────────
            SectionLabel("☁️ GitHub 数据同步")
            Text("将全部数据同步到你的私有 GitHub 仓库，支持多设备共享与灾难恢复",
                style = MaterialTheme.typography.bodySmall, color = FluentMuted,
                modifier = Modifier.padding(horizontal = 2.dp))
            SectionCard(icon = Icons.Default.Cloud, iconColor = Color(0xFF24292F),
                title = "GitHub 同步设置", subtitle = "推送 / 拉取 · 冲突检测 · 含头像",
                actionLabel = "前往") { onNavigateToGitHubSync() }

            HorizontalDivider(color = FluentBorder)

            // ── 完整备份 ──────────────────────────────────────────────────────
            SectionLabel("📦 完整备份")
            Text("包含所有数据：班级、教师、学生、课次、知识点（含自定义）和头像",
                style = MaterialTheme.typography.bodySmall, color = FluentMuted,
                modifier = Modifier.padding(horizontal = 2.dp))
            SectionCard(icon = Icons.Default.FolderZip, iconColor = FluentBlue,
                title = "导出完整备份",
                subtitle = "${state.lessons.size} 节课 · ${state.students.size} 名学生 · $kpCount 知识点",
                actionLabel = "导出") {
                val bytes = vm.exportFullZip(context)
                if (bytes != null) { pendingFullBytes = bytes; saveFullLauncher.launch("school_backup_full.zip") }
                else toast = "❌ 生成备份失败"
            }

            HorizontalDivider(color = FluentBorder)

            // ── 课次筛选备份 ──────────────────────────────────────────────────
            SectionLabel("🔍 课次筛选备份")
            Text("仅导出课次相关数据，不含知识点和头像，适合按教师或班级归档分享",
                style = MaterialTheme.typography.bodySmall, color = FluentMuted,
                modifier = Modifier.padding(horizontal = 2.dp))

            var fTeacherId by remember { mutableLongStateOf(0L) }
            var fClassId   by remember { mutableLongStateOf(0L) }
            var fFromDate  by remember { mutableStateOf("") }
            var fToDate    by remember { mutableStateOf("") }

            val filteredCount = remember(state.lessons, fTeacherId, fClassId, fFromDate, fToDate) {
                state.lessons.count { l ->
                    (fTeacherId == 0L || l.effectiveTeacherId(state.classes) == fTeacherId) &&
                    (fClassId   == 0L || l.classId == fClassId) &&
                    (fFromDate.isBlank() || l.date >= fFromDate) &&
                    (fToDate.isBlank()   || l.date <= fToDate)
                }
            }
            val hasFilter = fTeacherId != 0L || fClassId != 0L || fFromDate.isNotBlank() || fToDate.isNotBlank()

            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (state.teachers.isNotEmpty())
                            Box(Modifier.weight(1f)) { DropdownFilterChip("全部教师", state.teachers.map { it.id to it.name }, fTeacherId) { fTeacherId = it } }
                        if (state.classes.isNotEmpty())
                            Box(Modifier.weight(1f)) { DropdownFilterChip("全部班级", state.classes.map { it.id to it.name }, fClassId) { fClassId = it } }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) { DatePickerField("开始日期", fFromDate) { fFromDate = it } }
                        Box(Modifier.weight(1f)) { DatePickerField("结束日期", fToDate)   { fToDate   = it } }
                    }
                    Button(
                        onClick = {
                            if (!hasFilter) { toast = "⚠️ 请先设置至少一个筛选条件"; return@Button }
                            val bytes = vm.exportFilteredZip(context,
                                fTeacherId.takeIf { it != 0L }, fClassId.takeIf { it != 0L },
                                fFromDate.ifBlank { null }, fToDate.ifBlank { null })
                            if (bytes != null) { pendingFilteredBytes = bytes; saveFilteredLauncher.launch("school_backup_lessons.zip") }
                            else toast = "❌ 生成失败"
                        },
                        enabled = hasFilter, shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FluentPurple),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (hasFilter) "导出筛选课次（$filteredCount 节）" else "导出筛选课次（请先设置条件）", color = Color.White)
                    }
                }
            }

            HorizontalDivider(color = FluentBorder)

            // ── 导入 ──────────────────────────────────────────────────────────
            SectionLabel("📥 导入数据")
            Text("选择本应用导出的 ZIP 文件，导入前显示预览。导入模式为按 ID 合并。",
                style = MaterialTheme.typography.bodySmall, color = FluentMuted,
                modifier = Modifier.padding(horizontal = 2.dp))
            SectionCard(icon = Icons.Default.FileOpen, iconColor = FluentGreen,
                title = "选择 ZIP 文件导入", subtitle = "支持完整备份和课次备份两种格式",
                actionLabel = "选择") { importLauncher.launch(arrayOf("application/zip", "*/*")) }

            HorizontalDivider(color = FluentBorder)

            // ── 危险操作 ──────────────────────────────────────────────────────
            SectionLabel("⚠️ 危险操作")
            SectionCard(icon = Icons.Default.DeleteForever, iconColor = FluentRed,
                title = "清空所有数据",
                subtitle = "删除全部班级、教师、学生、课次及知识点数据，此操作不可撤销",
                actionLabel = "清空") { showDeleteDialog = true }

            Spacer(Modifier.height(4.dp))
            Text("智慧课务管理  v$appVersion",
                style = MaterialTheme.typography.bodySmall, color = FluentMuted,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }

        // ── Toast ──────────────────────────────────────────────────────────────
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

    // ── Import preview dialog ──────────────────────────────────────────────────
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

    // ── Delete all confirm dialog ──────────────────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text("确认清空", fontWeight = FontWeight.Bold) },
            text  = {
                Text("将删除全部班级、教师、学生、课次及知识点数据。\n\n此操作不可撤销。",
                    style = MaterialTheme.typography.bodyMedium)
            },
            confirmButton = {
                Button(onClick = { vm.deleteAllData(); showDeleteDialog = false },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FluentRed)) {
                    Text("确认清空", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消", color = FluentMuted) }
            }
        )
    }
}

// ── Small helpers ─────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun StatBadge(icon: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.08f), modifier = modifier) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(icon, fontSize = 18.sp)
            Text(label, style = MaterialTheme.typography.labelSmall, color = color,
                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        }
    }
}
