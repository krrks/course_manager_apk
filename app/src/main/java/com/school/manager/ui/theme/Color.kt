package com.school.manager.ui.theme

import androidx.compose.ui.graphics.Color

// Fluent Design inspired palette
val FluentBlue       = Color(0xFF1A56DB)
val FluentBlueDark   = Color(0xFF1E3A8A)
val FluentBlueLight  = Color(0xFFEFF6FF)
val FluentGreenLight = Color(0xFFECFDF5)
val FluentGreen      = Color(0xFF0E9F6E)
val FluentPurple     = Color(0xFF7E3AF2)
val FluentOrange     = Color(0xFFFF5A1F)
val FluentAmber      = Color(0xFFE3A008)
val FluentRed        = Color(0xFFE11D48)
val FluentTeal       = Color(0xFF0891B2)
val FluentLime       = Color(0xFF65A30D)

val FluentSurface    = Color(0xFFF8FAFF)
val FluentCard       = Color(0xFFFFFFFF)
val FluentBorder     = Color(0xFFE5E7EB)
val FluentMuted      = Color(0xFF6B7280)

// Packed subject colors for mapping index -> Color
fun subjectColor(index: Int) = when (index % 8) {
    0 -> FluentBlue
    1 -> FluentGreen
    2 -> FluentPurple
    3 -> FluentOrange
    4 -> FluentAmber
    5 -> FluentRed
    6 -> FluentTeal
    else -> FluentLime
}

fun packedToColor(packed: Long): Color = Color(packed.toInt())
