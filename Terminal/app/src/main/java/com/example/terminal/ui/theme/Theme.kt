package com.example.terminal.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = TerminalPrimary,
    onPrimary = TerminalOnColor,
    primaryContainer = TerminalSurfaceVariant,
    onPrimaryContainer = TerminalOnColor,
    secondary = TerminalSecondary,
    onSecondary = TerminalOnColor,
    tertiary = TerminalTertiary,
    onTertiary = TerminalOnColor,
    background = TerminalBackgroundBottom,
    onBackground = TerminalOnColor,
    surface = TerminalSurface,
    onSurface = TerminalOnColor,
    surfaceVariant = TerminalSurfaceVariant,
    onSurfaceVariant = TerminalOnSurfaceVariant,
    outline = TerminalOutline,
    outlineVariant = TerminalOutline.copy(alpha = 0.6f)
)

private val LightColorScheme = lightColorScheme(
    primary = TerminalPrimary,
    onPrimary = TerminalOnColor,
    primaryContainer = TerminalSurfaceVariant,
    onPrimaryContainer = TerminalOnColor,
    secondary = TerminalSecondary,
    onSecondary = TerminalOnColor,
    tertiary = TerminalTertiary,
    onTertiary = TerminalOnColor,
    background = TerminalBackgroundBottom,
    onBackground = TerminalOnColor,
    surface = TerminalSurface,
    onSurface = TerminalOnColor,
    surfaceVariant = TerminalSurfaceVariant,
    onSurfaceVariant = TerminalOnSurfaceVariant,
    outline = TerminalOutline,
    outlineVariant = TerminalOutline.copy(alpha = 0.6f)
)

@Composable
fun TerminalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}