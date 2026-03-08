package com.school.manager.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary          = FluentBlue,
    onPrimary        = androidx.compose.ui.graphics.Color.White,
    primaryContainer = FluentBlueLight,
    onPrimaryContainer = FluentBlueDark,
    secondary        = FluentGreen,
    tertiary         = FluentPurple,
    background       = FluentSurface,
    surface          = FluentCard,
    surfaceVariant   = FluentBlueLight,
    outline          = FluentBorder,
)

private val DarkColorScheme = darkColorScheme(
    primary          = FluentBlue,
    secondary        = FluentGreen,
    tertiary         = FluentPurple,
)

@Composable
fun SchoolManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AppTypography,
        content     = content
    )
}
