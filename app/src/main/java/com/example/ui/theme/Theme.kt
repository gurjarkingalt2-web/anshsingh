package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JarvisColorScheme = darkColorScheme(
    primary = JarvisCyan,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF004D61),
    onPrimaryContainer = JarvisCyan,
    secondary = JarvisGold,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF533B00),
    onSecondaryContainer = JarvisGoldGlow,
    tertiary = JarvisCyanDark,
    background = JarvisBgDark,
    onBackground = JarvisTextPrimary,
    surface = JarvisSurfaceDark,
    onSurface = JarvisTextPrimary,
    surfaceVariant = JarvisSurfaceElevated,
    onSurfaceVariant = JarvisTextSecondary,
    outline = JarvisCardBorder,
    error = JarvisUrgentRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Jarvis defaults to sleek high-tech dark theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = JarvisColorScheme,
        typography = Typography,
        content = content
    )
}
