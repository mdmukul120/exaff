package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MukulDarkColorScheme = darkColorScheme(
    primary = MukulRedPrimary,
    onPrimary = Color.White,
    primaryContainer = MukulRedDark,
    onPrimaryContainer = Color.White,
    secondary = MukulRedGlowing,
    onSecondary = Color.White,
    tertiary = MukulGold,
    background = MukulDarkBg,
    onBackground = MukulTextPrimary,
    surface = MukulCardBg,
    onSurface = MukulTextPrimary,
    surfaceVariant = MukulSurfaceVariant,
    onSurfaceVariant = MukulTextSecondary,
    outline = MukulCardBorder,
)

private val MukulLightColorScheme = lightColorScheme(
    primary = MukulRedPrimary,
    onPrimary = Color.White,
    primaryContainer = MukulRedPrimary,
    onPrimaryContainer = Color.White,
    secondary = MukulRedGlowing,
    onSecondary = Color.White,
    tertiary = MukulGold,
    background = Color(0xFFF6F7FB),
    onBackground = Color(0xFF15171E),
    surface = Color.White,
    onSurface = Color(0xFF15171E),
    surfaceVariant = Color(0xFFEDEBF2),
    onSurfaceVariant = Color(0xFF5A5D6E),
    outline = Color(0xFFDFE1EB),
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) MukulDarkColorScheme else MukulLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
