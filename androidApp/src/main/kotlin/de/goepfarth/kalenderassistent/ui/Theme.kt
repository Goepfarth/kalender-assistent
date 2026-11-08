package de.goepfarth.kalenderassistent.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Graustufen und genau eine Akzentfarbe (PLAN.md, Abschnitt 11).
private val LightColors = lightColorScheme(
    primary = Color(0xFF3B5BDB),
    onPrimary = Color.White,
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1B1B1F),
    surface = Color(0xFFFAFAFA),
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFF0F0F2),
    onSurfaceVariant = Color(0xFF5F6368),
    outlineVariant = Color(0xFFE2E2E6),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF91A7FF),
    onPrimary = Color(0xFF0B1A4A),
    background = Color(0xFF121214),
    onBackground = Color(0xFFE4E4E8),
    surface = Color(0xFF121214),
    onSurface = Color(0xFFE4E4E8),
    surfaceVariant = Color(0xFF1E1E22),
    onSurfaceVariant = Color(0xFFA0A4AB),
    outlineVariant = Color(0xFF2C2C31),
)

@Composable
fun KalenderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
