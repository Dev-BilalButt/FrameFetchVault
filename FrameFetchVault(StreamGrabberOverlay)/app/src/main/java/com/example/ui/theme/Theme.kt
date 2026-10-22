package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FrameFetchDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Color(0xFF001F26),
    primaryContainer = Color(0xFF003642),
    onPrimaryContainer = Color(0xFFB8F4FF),
    secondary = SignalEmerald,
    onSecondary = Color(0xFF00210E),
    secondaryContainer = Color(0xFF00391C),
    onSecondaryContainer = Color(0xFFB4FFD2),
    tertiary = AmberPulse,
    onTertiary = Color(0xFF261900),
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = CarbonSurface,
    onSurface = TextPrimary,
    surfaceVariant = GlassSurface,
    onSurfaceVariant = TextSecondary,
    outline = ElevatedBorder,
    error = CrimsonAlert,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FrameFetchDarkColorScheme,
        typography = Typography,
        content = content
    )
}
