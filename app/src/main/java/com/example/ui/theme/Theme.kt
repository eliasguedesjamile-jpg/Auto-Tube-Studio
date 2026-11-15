package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val StudioDarkColorScheme = darkColorScheme(
    primary = StudioGold,
    onPrimary = StudioOnGold,
    primaryContainer = Color(0xFF3A2E16),
    onPrimaryContainer = StudioGold,
    secondary = StudioBlue,
    onSecondary = Color(0xFF0A1B33),
    secondaryContainer = Color(0xFF1E2D45),
    onSecondaryContainer = StudioBlue,
    tertiary = StudioCrimson,
    onTertiary = Color(0xFF2B0B11),
    tertiaryContainer = Color(0xFF3E1C23),
    onTertiaryContainer = StudioCrimson,
    background = StudioBg,
    onBackground = StudioText,
    surface = StudioPanel,
    onSurface = StudioText,
    surfaceVariant = StudioPanelElevated,
    onSurfaceVariant = StudioMuted,
    outline = StudioBorder,
    outlineVariant = Color(0xFF232A39),
    error = StudioCrimson,
    onError = Color(0xFF2B0B11),
    errorContainer = Color(0xFF3E1C23),
    onErrorContainer = Color(0xFFFFB3BC)
)

private val StudioLightColorScheme = lightColorScheme(
    primary = LightGoldPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFBE9C7),
    onPrimaryContainer = Color(0xFF422A00),
    secondary = Color(0xFF285EA8),
    onSecondary = Color.White,
    tertiary = Color(0xFFB33242),
    onTertiary = Color.White,
    background = LightBg,
    onBackground = LightText,
    surface = LightPanel,
    onSurface = LightText,
    surfaceVariant = LightPanelElevated,
    onSurfaceVariant = LightMuted,
    outline = LightBorder,
    error = Color(0xFFB3261E),
    onError = Color.White
)

val StudioShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun AutoTubeStudioTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) StudioDarkColorScheme else StudioLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = StudioTypography,
        shapes = StudioShapes,
        content = content
    )
}
