package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.model.AppSettings

@Composable
fun LiquidMusicTheme(
    appSettings: AppSettings,
    content: @Composable () -> Unit,
) {
    val primaryColor = appSettings.selectedTheme.primaryColor
    
    val colorScheme = if (appSettings.isDarkMode) {
        darkColorScheme(
            primary = primaryColor,
            onPrimary = Color.Black,
            primaryContainer = primaryColor.copy(alpha = 0.2f),
            onPrimaryContainer = primaryColor,
            secondary = PurpleAccent,
            onSecondary = Color.White,
            tertiary = OrangeGlow,
            background = LiquidDarkBg,
            onBackground = GlassTextPrimary,
            surface = LiquidDarkSurface,
            onSurface = GlassTextPrimary,
            surfaceVariant = LiquidGlassCard,
            onSurfaceVariant = GlassTextSecondary,
            outline = LiquidGlassBorder
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            primaryContainer = primaryColor.copy(alpha = 0.2f),
            onPrimaryContainer = primaryColor,
            secondary = PurpleAccent,
            onSecondary = Color.White,
            tertiary = OrangeGlow,
            background = Color(0xFFF0F0F0),
            onBackground = Color(0xFF1E1E1E),
            surface = Color.White,
            onSurface = Color(0xFF1E1E1E),
            surfaceVariant = Color(0xFFE0E0E0),
            onSurfaceVariant = Color(0xFF555555),
            outline = Color(0xFFCCCCCC)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

