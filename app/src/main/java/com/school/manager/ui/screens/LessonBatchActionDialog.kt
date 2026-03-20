package com.school.manager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.school.manager.data.*
import com.school.manager.ui.components.*
import com.school.manager.ui.theme.*
import com.school.manager.viewmodel.AppViewModel

/**
 * Unified batch action dialog operating on a fixed set of selected lesson IDs.
 * Three modes selected via FilterChip: 修改时间 / 修改状态 / 删除.
 * Keeps existing per-class BatchModifyDialog and BatchDeleteDialog untouched.
 */
@Composable
internal fun BatchActionDialog(
    selectedIds: Set<Long>,
    state: AppState,
    vm: AppViewModel,
    onDismiss: () -> Unit
) {
    var actionType      by remember { mutableStateOf("time") }
    var newStart        by remember { mutableStateOf("") }
    var newEnd          by remember { mutableStateOf("") }
    var newStatus       by remember { mutableStateOf("") }
    var deleteConfirmed by remember { mutableStateOf(false) }

    val count = selectedIds.size

    val confirmText = when (actionType) {
        "time"   -> "修改时间"
        "status" -> "修改状态"
        else     -> if (deleteConfirmed) "最终确认删除 $count 节" else "确认删除 $count 节"
    }

    FluentDialog(
        title       = "批量操作（已选 $count 节）",
        onDismiss   = onDismiss,
        confirmText = confirmText,
        onConfirm   = {
            when (actionType) {
                "time" -> {
                    if (newStart.isNotBlank()) {
                        state.lessons.filter { it.id in selectedIds }.forEach { l ->
                            vm.updateLesson(
                                l.copy(
                                    startTime = newStart,
                                    endTime   = newEnd.ifBlank { l.endTime }
                                ),
                                markModified = true
                            )
                        }
                        onDismiss()
                    }
                }
                "status" -> {
                    if (newStatus.isNotBlank()) {
                        state.lessons.filter { it.id in selectedIds }.forEach { l ->
                            vm.updateLesson(l.copy(status = newStatus), markModified = true)
                        }
                        onDismiss()
                    }
                }
                "delete" -> {
                    if (!deleteConfirmed) {
                        deleteConfirmed = true
                    } else {
                        selectedIds.forEach { id -> vm.deleteLesson(id) }
                        onDismiss()
                    }
                }
            }
        }
    ) {
        // ── Operation type selector ────────────────────────────────────────
        SectionHeader("选择操作")
        Row(
            Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = actionType == "time",
                onClick  = { actionType = "time"; deleteConfirmed = false },
                label    = { Text("修改时间") }
            )
            FilterChip(
                selected = actionType == "status",
                onClick  = { actionType = "status"; deleteConfirmed = false },
                label    = { Text("修改状态") }
            )
            FilterChip(
                selected = actionType == "delete",
                onClick  = { actionType = "delete"; deleteConfirmed = false },
                label    = { Text("删除") },
                colors   = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = FluentRed,
                    selectedLabelColor     = Color.White
                )
            )
        }

        HorizontalDivider(color = FluentBorder, modifier = Modifier.padding(horizontal = 16.dp))

        // ── Mode content ──────────────────────────────────────────────────
        when (actionType) {

            "time" -> {
                Text("选择修改后的上课时间",
                    style    = MaterialTheme.typography.bodySmall, color = FluentMuted,
                    modifier = Modifier.padding(horizontal = 16.dp))
                StartTimeCompact(newStart.ifBlank { "08:00" }) { newStart = it }
                if (newStart.isNotBlank()) {
                    DurationChipsCompact(
                        startTime = newStart,
                        endTime   = newEnd.ifBlank { addMinutesToTime(newStart, 45) }
                    ) { newEnd = it }
                }
            }

            "status" -> {
                Text("选择目标状态",
                    style    = MaterialTheme.typography.bodySmall, color = FluentMuted,
                    modifier = Modifier.padding(horizontal = 16.dp))
                androidx.compose.foundation.layout.FlowRow(
                    modifier              = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement   = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("pending", "completed", "absent", "cancelled", "postponed").forEach { s ->
                        FilterChip(
                            selected = newStatus == s,
                            onClick  = { newStatus = s },
                            label    = { Text(statusLabel(s)) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = statusColor(s),
                                selectedLabelColor     = Color.White
                            )
                        )
                    }
                }
            }

            "delete" -> {
                Surface(
                    shape    = RoundedCornerShape(8.dp),
                    color    = FluentRed.copy(0.1f),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Text("⚠️ 将永久删除 $count 节课次，此操作不可撤销！",
                        style    = MaterialTheme.typography.bodySmall, color = FluentRed,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("我确认要删除这 $count 节课次",
                        style    = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f))
                    Switch(checked = deleteConfirmed, onCheckedChange = { deleteConfirmed = it })
                }
            }
        }
    }
}
