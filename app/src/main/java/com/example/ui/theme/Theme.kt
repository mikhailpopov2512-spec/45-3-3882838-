package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

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

@Composable
fun HappVpnTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
