package com.school.manager.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AppTypography = Typography(
    headlineLarge = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold,   lineHeight = 32.sp),
    headlineMedium= TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold,   lineHeight = 28.sp),
    headlineSmall = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold,lineHeight=24.sp),
    titleLarge    = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold,lineHeight=22.sp),
    titleMedium   = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold,lineHeight=20.sp),
    bodyLarge     = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal,  lineHeight=20.sp),
    bodyMedium    = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal,  lineHeight=18.sp),
    labelLarge    = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold,lineHeight=16.sp),
    labelMedium   = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium,  lineHeight=14.sp),
    labelSmall    = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium,  lineHeight=13.sp),
)
