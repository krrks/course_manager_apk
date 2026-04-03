package com.school.manager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.school.manager.ui.components.*
import com.school.manager.ui.theme.*
import com.school.manager.viewmodel.*
import kotlinx.coroutines.delay

@Composable
internal fun GitHubSyncScreen(
    appVm: AppViewModel,
    onBack: () -> Unit
) {
    val syncVm: GitHubSyncViewModel = viewModel()
    val status   by syncVm.status.collectAsState()
    val appState by appVm.state.collectAsState()
    val context  = LocalContext.current

    var urlInput       by remember { mutableStateOf(syncVm.savedUrl) }
    var tokenInput     by remember { mutableStateOf(syncVm.savedToken) }
    var toastMsg       by remember { mutableStateOf<String?>(null) }
    var showConflict   by remember { mutableStateOf(false) }
    var prevWasSyncing by remember { mutableStateOf(false) }
    val appVersion = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "" }.getOrDefault("") }

    LaunchedEffect(status) {
        when {
            status is SyncStatus.Conflict                -> showConflict = true
            status is SyncStatus.Error                   -> toastMsg = "❌ ${(status as SyncStatus.Error).message}"
            status is SyncStatus.Ready && prevWasSyncing -> toastMsg = "✅ 操作成功"
        }
        prevWasSyncing = status is SyncStatus.Syncing
    }
    LaunchedEffect(toastMsg) { if (toastMsg != null) { delay(3000); toastMsg = null } }

    val isConnected = status is SyncStatus.Ready || status is SyncStatus.Syncing || status is SyncStatus.Conflict
    val isBusy      = status is SyncStatus.Syncing

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                // ── Top bar ───────────────────────────────────────────────────
                Surface(color = FluentBlue, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = Color.White)
                        }
                        Text(
                            "GitHub 数据同步",
                            color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                        if (isBusy) {
                            CircularProgressIndicator(
                                color       = Color.White,
                                modifier    = Modifier.size(20.dp).padding(end = 8.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }

                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (isConnected) ConnectedBanner(
                        repoUrl   = syncVm.savedUrl,
                        lastSync  = (status as? SyncStatus.Ready)?.lastSync ?: syncVm.savedLastSync,
                        isSyncing = isBusy)

                    // ── Error banner ──────────────────────────────────────────
                    if (status is SyncStatus.Error) {
                        Surface(shape = RoundedCornerShape(10.dp), color = FluentRed.copy(.10f), modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, null, tint = FluentRed, modifier = Modifier.size(18.dp))
                                Text((status as SyncStatus.Error).message, style = MaterialTheme.typography.bodySmall, color = FluentRed, modifier = Modifier.weight(1f))
                                TextButton(onClick = { syncVm.restoreReady() }, contentPadding = PaddingValues(0.dp)) { Text("×", color = FluentRed, fontSize = 18.sp) }
                            }
                        }
                    }

                    // ── Connection form ───────────────────────────────────────
                    if (!isConnected) {
                        Text("连接到 GitHub 仓库", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                        FormTextField("仓库地址", urlInput, { urlInput = it },
                            "https://github.com/you/your-repo")
                        OutlinedTextField(
                            value                = tokenInput,
                            onValueChange        = { tokenInput = it },
                            label                = { Text("Personal Access Token (PAT)") },
                            placeholder          = { Text("ghp_xxxxxxxxxxxx", color = FluentMuted) },
                            singleLine           = true,
                            shape                = RoundedCornerShape(12.dp),
                            modifier             = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
                            colors               = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FluentBlue, unfocusedBorderColor = FluentBorder)
                        )
                        Button(
                            onClick  = {
                                syncVm.connect(urlInput, tokenInput) { err ->
                                    if (err != null) toastMsg = "❌ $err"
                                }
                            },
                            enabled  = urlInput.isNotBlank() && tokenInput.isNotBlank() && !isBusy,
                            shape    = RoundedCornerShape(10.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = FluentBlue),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("测试并连接") }
                    }

                    // ── Sync actions ──────────────────────────────────────────
                    if (isConnected) {
                        HorizontalDivider(color = FluentBorder)
                        Text("同步操作", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick  = { syncVm.push(appState, context, appVersion) },
                                enabled  = !isBusy,
                                shape    = RoundedCornerShape(10.dp),
                                colors   = ButtonDefaults.buttonColors(containerColor = FluentBlue),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp)); Text("推送到远端")
                            }
                            OutlinedButton(
                                onClick  = {
                                    syncVm.pull(context) { state, kpData ->
                                        state?.let { appVm.syncImportStateOnly(it) }
                                        kpData?.let { appVm.syncReplaceAllKps(it) }
                                    }
                                },
                                enabled  = !isBusy,
                                shape    = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp)); Text("从远端拉取")
                            }
                        }
                        Text(
                            "推送：所有数据（含课表及全部知识点）上传远端，内容无变化时自动跳过。\n" +
                            "拉取：下载并替换本地数据；知识点仅在远端有更新时下载。",
                            style = MaterialTheme.typography.bodySmall, color = FluentMuted
                        )

                        Spacer(Modifier.height(4.dp))
                        OutlinedButton(
                            onClick  = { syncVm.disconnect(); urlInput = ""; tokenInput = "" },
                            enabled  = !isBusy,
                            shape    = RoundedCornerShape(10.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = FluentRed),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("断开连接") }
                    }

                    // ── Info card ─────────────────────────────────────────────
                    Spacer(Modifier.height(4.dp))
                    Surface(shape = RoundedCornerShape(10.dp), color = FluentBlueLight,
                        modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "• 数据存储于仓库的 userdata_sync/ 目录\n" +
                            "• 需要 PAT 权限：repository → Contents → Read & Write\n" +
                            "• 全部知识点（含内置）随课表一起同步；内容无变化自动跳过\n" +
                            "• 状态/知识点数据均只在变化时才上传，减少 API 消耗",
                            style    = MaterialTheme.typography.bodySmall,
                            color    = FluentBlueDark,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // ── Toast overlay ─────────────────────────────────────────────────
            toastMsg?.let { msg ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    Surface(
                        modifier        = Modifier.padding(bottom = 24.dp).widthIn(max = 320.dp),
                        shape           = RoundedCornerShape(12.dp),
                        color           = if (msg.startsWith("✅")) Color(0xFFE8F5E9)
                                          else MaterialTheme.colorScheme.errorContainer,
                        shadowElevation = 4.dp
                    ) {
                        Text(msg, modifier = Modifier.padding(12.dp),
                            color      = if (msg.startsWith("✅")) FluentGreen
                                         else MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }

    // ── Conflict dialog ───────────────────────────────────────────────────────
    if (showConflict) {
        AlertDialog(
            onDismissRequest = { showConflict = false; syncVm.restoreReady() },
            shape            = RoundedCornerShape(20.dp),
            title            = { Text("⚠️ 数据冲突", fontWeight = FontWeight.Bold) },
            text             = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text((status as? SyncStatus.Conflict)?.detail
                        ?: "远端和本地均有改动，请选择处理方式。",
                        style = MaterialTheme.typography.bodyMedium)
                    Surface(shape = RoundedCornerShape(8.dp), color = FluentAmber.copy(.10f),
                        modifier = Modifier.fillMaxWidth()) {
                        Text("覆盖远端：以本地数据为准，远端改动将丢失。\n拉取远端：以远端数据为准，本地改动将丢失。",
                            style    = MaterialTheme.typography.bodySmall, color = FluentAmber,
                            modifier = Modifier.padding(10.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showConflict = false; syncVm.pushForce(appState, context, appVersion) },
                    shape   = RoundedCornerShape(12.dp),
                    colors  = ButtonDefaults.buttonColors(containerColor = FluentOrange)
                ) { Text("覆盖远端", color = Color.White) }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showConflict = false; syncVm.restoreReady() }) {
                        Text("取消", color = FluentMuted)
                    }
                    OutlinedButton(
                        onClick = {
                            showConflict = false
                            syncVm.pull(context) { state, kpData ->
                                state?.let { appVm.syncImportStateOnly(it) }
                                kpData?.let { appVm.syncReplaceAllKps(it) }
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("拉取远端") }
                }
            }
        )
    }
}

// ── Connected status banner ────────────────────────────────────────────────────

@Composable
private fun ConnectedBanner(repoUrl: String, lastSync: String?, isSyncing: Boolean) {
    Surface(shape = RoundedCornerShape(12.dp),
        color = if (isSyncing) FluentBlueLight else Color(0xFFECFDF5),
        modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            if (isSyncing) CircularProgressIndicator(color = FluentBlue, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            else Icon(Icons.Default.CheckCircle, null, tint = FluentGreen, modifier = Modifier.size(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(if (isSyncing) "同步中..." else "已连接",
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                    color = if (isSyncing) FluentBlue else FluentGreen)
                Text(repoUrl, style = MaterialTheme.typography.bodySmall, color = FluentMuted)
                if (!isSyncing && lastSync != null)
                    Text("最后同步: $lastSync", style = MaterialTheme.typography.labelSmall, color = FluentMuted)
            }
        }
    }
}
