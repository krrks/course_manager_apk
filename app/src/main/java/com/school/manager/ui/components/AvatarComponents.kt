package com.school.manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.school.manager.ui.theme.*

@Composable
fun AvatarCircle(name: String, color: androidx.compose.ui.graphics.Color = FluentBlue, size: Dp = 40.dp) {
    val letter = name.firstOrNull()?.toString() ?: "?"
    Box(
        contentAlignment = Alignment.Center,
        modifier         = Modifier.size(size).clip(CircleShape).background(color.copy(alpha = 0.2f))
    ) {
        Text(letter, color = color,
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AvatarWithImage(
    name: String,
    color: androidx.compose.ui.graphics.Color = FluentBlue,
    size: Dp = 40.dp,
    imageUri: String? = null
) {
    if (!imageUri.isNullOrBlank()) {
        var error by remember(imageUri) { mutableStateOf(false) }
        if (!error) {
            val model = if (imageUri.startsWith("/")) java.io.File(imageUri) else imageUri
            AsyncImage(
                model            = model,
                contentDescription = name,
                contentScale     = ContentScale.Crop,
                onError          = { error = true },
                modifier         = Modifier.size(size).clip(CircleShape)
            )
            return
        }
    }
    AvatarCircle(name, color, size)
}

@Composable
fun StatusBadge(status: String) {
    val (label, color) = when (status) {
        "completed" -> "✅ 已完成" to FluentGreen
        "cancelled" -> "❌ 已取消" to FluentRed
        else        -> "⏳ 待上课" to FluentAmber
    }
    ColorChip(label, color)
}
