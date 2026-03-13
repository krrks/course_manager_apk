package com.school.manager.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.school.manager.ui.theme.*

// ─── Fluent-Style Card ────────────────────────────────────────────────────────

@Composable
fun FluentCard(
    modifier: Modifier = Modifier,
    accentColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val cardMod = modifier
        .shadow(elevation = 2.dp, shape = shape, ambientColor = FluentBlue.copy(alpha = 0.06f))
        .clip(shape)
        .background(MaterialTheme.colorScheme.surface)
        .then(if (accentColor != null) Modifier.drawBehind {
            drawRect(color = accentColor, size = androidx.compose.ui.geometry.Size(6f, size.height))
        } else Modifier)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    Column(modifier = cardMod, content = content)
}

// ─── Section Header ───────────────────────────────────────────────────────────

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text     = title,
        style    = MaterialTheme.typography.labelMedium,
        color    = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

// ─── Colored Chip/Badge ───────────────────────────────────────────────────────

@Composable
fun ColorChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(20.dp), color = color.copy(alpha = 0.15f), modifier = modifier) {
        Text(text, color = color, style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

// ─── Detail Row ───────────────────────────────────────────────────────────────

@Composable
fun DetailRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = FluentMuted)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
    HorizontalDivider(color = FluentBorder, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
}

// ─── Avatar ───────────────────────────────────────────────────────────────────

@Composable
fun AvatarCircle(name: String, color: Color = FluentBlue, size: Dp = 40.dp) {
    val letter = name.firstOrNull()?.toString() ?: "?"
    Box(contentAlignment = Alignment.Center,
        modifier = Modifier.size(size).clip(CircleShape).background(color.copy(alpha = 0.2f))) {
        Text(letter, color = color, style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AvatarWithImage(name: String, color: Color = FluentBlue, size: Dp = 40.dp, imageUri: String? = null) {
    if (!imageUri.isNullOrBlank()) {
        var error by remember(imageUri) { mutableStateOf(false) }
        if (!error) {
            val model = if (imageUri.startsWith("/")) java.io.File(imageUri) else imageUri
            AsyncImage(model = model, contentDescription = name, contentScale = ContentScale.Crop,
                onError = { error = true }, modifier = Modifier.size(size).clip(CircleShape))
            return
        }
    }
    AvatarCircle(name, color, size)
}

// ─── Status Badge ─────────────────────────────────────────────────────────────

@Composable
fun StatusBadge(status: String) {
    val (label, color) = when (status) {
        "completed" -> "✅ 已完成" to FluentGreen
        "cancelled" -> "❌ 已取消" to FluentRed
        else        -> "⏳ 待上课" to FluentAmber
    }
    ColorChip(label, color)
}

// ─── Empty State ──────────────────────────────────────────────────────────────

@Composable
fun EmptyState(icon: String = "📭", text: String = "暂无数据", modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxWidth().padding(vertical = 48.dp)) {
        Text(icon, fontSize = 42.sp)
        Spacer(Modifier.height(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = FluentMuted)
    }
}

// ─── Fluent Progress Bar ──────────────────────────────────────────────────────

@Composable
fun FluentProgressBar(progress: Float, color: Color, modifier: Modifier = Modifier) {
    LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier.height(4.dp).clip(RoundedCornerShape(2.dp)),
        color = color, trackColor = color.copy(alpha = 0.15f))
}

// ─── Fluent Dialog ────────────────────────────────────────────────────────────

@Composable
fun FluentDialog(
    title: String, onDismiss: () -> Unit, onConfirm: (() -> Unit)? = null,
    confirmText: String = "保存", content: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(onDismissRequest = onDismiss, shape = RoundedCornerShape(20.dp),
        title = { Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
        text  = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
        },
        confirmButton = {
            if (onConfirm != null) {
                Button(onClick = onConfirm, shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FluentBlue)) { Text(confirmText) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = FluentMuted) } }
    )
}

// ─── Form Fields ──────────────────────────────────────────────────────────────

@Composable
fun FormTextField(label: String, value: String, onValueChange: (String) -> Unit,
                  placeholder: String = "", modifier: Modifier = Modifier) {
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) },
        placeholder = { Text(placeholder, color = FluentMuted) }, shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(), singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FluentBlue, unfocusedBorderColor = FluentBorder))
}

@Suppress("DEPRECATION")
@Composable
fun FormDropdown(label: String, selected: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(value = selected, onValueChange = {}, readOnly = true, label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().menuAnchor(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FluentBlue, unfocusedBorderColor = FluentBorder))
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(opt) }, onClick = { onSelect(opt); expanded = false })
            }
        }
    }
}

// ─── Dropdown Filter Chip (Long id) ──────────────────────────────────────────

@Composable
fun DropdownFilterChip(allLabel: String, items: List<Pair<Long, String>>,
                       selected: Long, onSelect: (Long) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = items.firstOrNull { it.first == selected }?.second ?: allLabel
    Box {
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

// ─── Shared SpeedDial FAB ─────────────────────────────────────────────────────
// Every screen uses ScreenSpeedDialFab to merge navigation + add into one FAB.

/** A single item in a speed-dial FAB row (label chip + small FAB). */
@Composable
fun SpeedDialItem(
    label: String, icon: ImageVector, color: Color,
    selected: Boolean = false, onClick: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(shape = RoundedCornerShape(8.dp),
            color = if (selected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp, modifier = Modifier.clickable { onClick() }) {
            Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) color else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
        }
        SmallFloatingActionButton(onClick = onClick,
            containerColor = if (selected) color else color.copy(alpha = 0.85f),
            contentColor = Color.White, shape = CircleShape) {
            Icon(icon, null, modifier = Modifier.size(18.dp))
        }
    }
}

/**
 * Speed-dial FAB that merges the add action and navigation drawer into one button.
 * Matches the ScheduleScreen FAB pattern.
 *
 * FIX: wrap the full-screen backdrop and the speed-dial Column in a single Box
 * so the Column is always pinned to Alignment.BottomEnd.  Previously the backdrop
 * Box and the Column were siblings at the Scaffold-FAB-slot root; the backdrop's
 * fillMaxSize caused the Column to be laid out at the top of the screen.
 */
@Composable
fun ScreenSpeedDialFab(
    addLabel: String? = null,
    addIcon: ImageVector = Icons.Default.Add,
    onAdd: (() -> Unit)? = null,
    onOpenDrawer: () -> Unit,
    extraItems: @Composable (ColumnScope.() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }

    // When expanded the outer Box grows to fill the screen so the transparent
    // backdrop can intercept outside taps; the Column is anchored to BottomEnd.
    // When collapsed the outer Box just wraps the FAB button itself.
    Box(
        modifier = if (expanded) Modifier.fillMaxSize() else Modifier.wrapContentSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        // Transparent full-screen backdrop — dismiss on outside tap
        if (expanded) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        indication        = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { expanded = false }
            )
        }

        // Speed-dial items + main FAB, always pinned bottom-right
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (expanded) {
                extraItems?.invoke(this)

                if (addLabel != null && onAdd != null) {
                    SpeedDialItem(addLabel, addIcon, FluentBlue) { onAdd(); expanded = false }
                }

                HorizontalDivider(color = FluentBorder.copy(alpha = 0.4f), modifier = Modifier.width(200.dp))

                SpeedDialItem("导航菜单", Icons.Default.Menu, FluentMuted) {
                    onOpenDrawer(); expanded = false
                }
            }

            FloatingActionButton(
                onClick        = { expanded = !expanded },
                containerColor = FluentBlue,
                contentColor   = Color.White,
                shape          = CircleShape
            ) {
                Icon(
                    if (expanded) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = if (expanded) "关闭" else "菜单"
                )
            }
        }
    }
}
