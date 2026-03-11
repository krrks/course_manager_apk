package com.school.manager.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
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
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

// ─── Colored Chip/Badge ───────────────────────────────────────────────────────

@Composable
fun ColorChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape  = RoundedCornerShape(20.dp),
        color  = color.copy(alpha = 0.15f),
        modifier = modifier
    ) {
        Text(
            text  = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

// ─── Detail Row ───────────────────────────────────────────────────────────────

@Composable
fun DetailRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = FluentMuted)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
    HorizontalDivider(color = FluentBorder, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
}

// ─── Avatar Circle ────────────────────────────────────────────────────────────

@Composable
fun AvatarCircle(
    name: String,
    color: Color = FluentBlue,
    size: Dp = 40.dp
) {
    val letter = name.firstOrNull()?.toString() ?: "?"
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.2f))
    ) {
        Text(
            text  = letter,
            color = color,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

// ─── Avatar With Image (shows photo if URI/path set, else initials) ──────────

@Composable
fun AvatarWithImage(
    name: String,
    color: Color = FluentBlue,
    size: Dp = 40.dp,
    imageUri: String? = null
) {
    if (!imageUri.isNullOrBlank()) {
        var error by remember(imageUri) { mutableStateOf(false) }
        if (!error) {
            val model = if (imageUri.startsWith("/")) java.io.File(imageUri) else imageUri
            AsyncImage(
                model           = model,
                contentDescription = name,
                contentScale    = ContentScale.Crop,
                onError         = { error = true },
                modifier        = Modifier.size(size).clip(CircleShape)
            )
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp)
    ) {
        Text(icon, fontSize = 42.sp)
        Spacer(Modifier.height(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = FluentMuted)
    }
}

// ─── Fluent Dialog ────────────────────────────────────────────────────────────

@Composable
fun FluentDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (() -> Unit)? = null,
    confirmText: String = "保存",
    content: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content
            )
        },
        confirmButton = {
            if (onConfirm != null) {
                Button(
                    onClick = onConfirm,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FluentBlue)
                ) { Text(confirmText) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = FluentMuted) }
        }
    )
}

// ─── Form Fields ──────────────────────────────────────────────────────────────

@Composable
fun FormTextField(label: String, value: String, onValueChange: (String) -> Unit, placeholder: String = "", modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, color = FluentMuted) },
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = FluentBlue,
            unfocusedBorderColor = FluentBorder
        )
    )
}

@Composable
fun FormDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            shape = RoundedCornerShape(12.dp),
            // FIX: replaced deprecated menuAnchor() with typed overload
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = FluentBlue,
                unfocusedBorderColor = FluentBorder
            )
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(opt) }, onClick = { onSelect(opt); expanded = false })
            }
        }
    }
}

// ─── Progress Bar ─────────────────────────────────────────────────────────────

@Composable
fun FluentProgressBar(value: Float, color: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier.height(7.dp).clip(RoundedCornerShape(4.dp)).background(color.copy(alpha = 0.15f))) {
        Box(modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(value.coerceIn(0f, 1f))
            .clip(RoundedCornerShape(4.dp))
            .background(color)
        )
    }
}
