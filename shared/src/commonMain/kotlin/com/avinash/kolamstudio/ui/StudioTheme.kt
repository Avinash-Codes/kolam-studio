package com.avinash.kolamstudio.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors =
    lightColorScheme(
        primary = Color(0xFF8B392D),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFF5D7CC),
        onPrimaryContainer = Color(0xFF421810),
        secondary = Color(0xFF53614D),
        secondaryContainer = Color(0xFFDEE8D8),
        onSecondaryContainer = Color(0xFF1E2D1B),
        tertiary = Color(0xFFA1472D),
        background = Color(0xFFF7F3EB),
        onBackground = Color(0xFF2A211B),
        surface = Color(0xFFF7F3EB),
        onSurface = Color(0xFF2A211B),
        surfaceVariant = Color(0xFFF0E5D8),
        onSurfaceVariant = Color(0xFF625248),
        surfaceContainer = Color(0xFFF1E8DB),
        surfaceContainerLow = Color(0xFFFFFBF4),
        outline = Color(0xFF8A786B),
        outlineVariant = Color(0xFFDCCFC1),
    )

private val DarkColors =
    darkColorScheme(
        primary = Color(0xFFFFB5A5),
        onPrimary = Color(0xFF531D15),
        primaryContainer = Color(0xFF633127),
        onPrimaryContainer = Color(0xFFFFDAD0),
        secondary = Color(0xFFBDCEB4),
        secondaryContainer = Color(0xFF394638),
        onSecondaryContainer = Color(0xFFD9E8D4),
        tertiary = Color(0xFFFBB691),
        background = Color(0xFF201C18),
        onBackground = Color(0xFFF2E6D9),
        surface = Color(0xFF201C18),
        onSurface = Color(0xFFF2E6D9),
        surfaceVariant = Color(0xFF392E26),
        onSurfaceVariant = Color(0xFFD2BFB0),
        surfaceContainer = Color(0xFF2D251F),
        surfaceContainerLow = Color(0xFF251F1B),
        outline = Color(0xFFA99282),
        outlineVariant = Color(0xFF514237),
    )

@Composable
internal fun StudioTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors, content = content)
}
