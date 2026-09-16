package com.daftari.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Blue = Color(0xFF0B63CE)
private val BlueDark = Color(0xFF004A9E)
private val LightScheme = lightColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEBFF),
    onPrimaryContainer = Color(0xFF001B3A),
    secondary = Color(0xFF35608D),
    background = Color(0xFFF7F9FC),
    surface = Color.White
)
private val DarkScheme = darkColorScheme(
    primary = Color(0xFF9CCBFF),
    primaryContainer = BlueDark,
    secondary = Color(0xFFA9C8EA)
)

@Composable
fun DaftariTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkScheme else LightScheme, typography = Typography(), content = content)
}
