package com.bilal.humanactivityrecognition.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Blue600,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Indigo900,
    secondary = Emerald500,
    background = Slate50,
    surface = Color.White,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate500,
    error = Red500,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = Slate900,
    primaryContainer = Blue800,
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Emerald500,
    background = Slate900,
    surface = Slate800,
    surfaceVariant = Color(0xFF334155),
    error = Red500,
)

@Composable
fun HARTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
