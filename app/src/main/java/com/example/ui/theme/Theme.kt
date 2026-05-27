package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    secondary = CyberTeal,
    background = CyberBg,
    surface = CyberCard,
    onPrimary = CyberBg,
    onSecondary = CyberBg,
    onBackground = CyberLightGray,
    onSurface = CyberLightGray
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF45A29E),
    secondary = Color(0xFF337E7A),
    background = Color(0xFFF0F3F6), // Soft grey background from screenshot
    surface = Color(0xFFFFFFFF),    // Beautiful white card layers
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF2E3137), // Dark text for readability
    onSurface = Color(0xFF20232A)
)

@Composable
fun HappVpnTheme(isDarkTheme: Boolean = false, content: @Composable () -> Unit) {
    val colors = if (isDarkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
