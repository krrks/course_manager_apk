package com.school.manager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.school.manager.data.*
import com.school.manager.ui.components.*
import com.school.manager.ui.theme.*

/**
 * Shown after the user picks a ZIP file, before any data is written.
 * Displays meta.json contents so the user can confirm what they are importing.
 */
@Composable
internal fun ImportPreviewDialog(
    result: ImportResult,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    when (result) {
        is ImportResult.Failure -> AlertDialog(
            onDismissRequest = onDismiss,
            shape            = RoundedCornerShape(20.dp),
            title            = { Text("无法读取备份", fontWeight = FontWeight.Bold) },
            text             = { Text(result.reason, color = FluentRed) },
            confirmButton    = {
                Button(onClick = onDismiss, shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FluentBlue)) {
                    Text("确定")
                }
            }
        )

        is ImportResult.Success -> AlertDialog(
            onDismissRequest = onDismiss,
            shape            = RoundedCornerShape(20.dp),
            title            = { Text("导入预览", fontWeight = FontWeight.Bold) },
            text             = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Filter badge — only shown for filtered exports
                    result.filter?.let { f ->
                        Surface(shape = RoundedCornerShape(8.dp),
                            color    = FluentAmber.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()) {
                            Text("筛选备份：${f.describe()}",
                                style    = MaterialTheme.typography.bodySmall,
                                color    = FluentAmber,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                        }
                    }

                    DetailRow("导出时间", result.exportedAt.take(10))
                    DetailRow("格式版本", "schema v${result.schemaVersion}")
                    HorizontalDivider(color = FluentBorder, thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 16.dp))

                    SectionHeader("包含数据")
                    DetailRow("科目", "${result.counts.subjects} 项")
                    DetailRow("教师", "${result.counts.teachers} 名")
                    DetailRow("班级", "${result.counts.classes} 个")
                    DetailRow("学生", "${result.counts.students} 名")
                    DetailRow("课次", "${result.counts.lessons} 节")

                    Surface(shape = RoundedCornerShape(8.dp),
                        color    = FluentBlueLight,
                        modifier = Modifier.fillMaxWidth()) {
                        Text("按 ID 合并：ID 相同的记录将被覆盖，其余追加。",
                            style    = MaterialTheme.typography.bodySmall,
                            color    = FluentBlueDark,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                    }
                }
            },
            confirmButton = {
                Button(onClick = onConfirm, shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FluentBlue)) {
                    Text("确认导入")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("取消", color = FluentMuted) }
            }
        )
    }
}
