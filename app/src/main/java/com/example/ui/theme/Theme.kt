package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CosmicDarkAccent,
    secondary = CosmicConnectedSec,
    tertiary = CosmicPingAmber,
    background = CosmicDarkBg,
    surface = CosmicDarkSurface,
    onBackground = CosmicTextWhite,
    onSurface = CosmicTextWhite
)

private val LightColorScheme = lightColorScheme(
    primary = SkyLightPrimary,
    secondary = SkyLightSec,
    tertiary = CosmicPingAmber,
    background = SkyLightBg,
    surface = SkyLightSurface,
    onBackground = SkyLightText,
    onSurface = SkyLightText
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
