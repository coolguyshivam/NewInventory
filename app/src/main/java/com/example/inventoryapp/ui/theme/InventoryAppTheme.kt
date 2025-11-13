package com.example.inventoryapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = White,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = PrimaryDark,
    secondary = Secondary,
    onSecondary = White,
    secondaryContainer = SecondaryLight,
    onSecondaryContainer = PrimaryDark,
    tertiary = Tertiary,
    onTertiary = White,
    background = BackgroundLight,
    onBackground = TextPrimary,
    surface = SurfaceLight,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = Error,
    onError = White,
    outline = TextSecondary.copy(alpha = 0.3f),
    outlineVariant = TextSecondary.copy(alpha = 0.15f),
)

private val DarkColors = darkColorScheme(
    primary = PrimaryDark_Theme,
    onPrimary = BackgroundDark,
    primaryContainer = PrimaryDark,
    onPrimaryContainer = PrimaryLight,
    secondary = SecondaryDark_Theme,
    onSecondary = BackgroundDark,
    secondaryContainer = PrimaryDark,
    onSecondaryContainer = SecondaryLight,
    tertiary = Tertiary,
    onTertiary = BackgroundDark,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    error = Error,
    onError = BackgroundDark,
    outline = TextSecondaryDark.copy(alpha = 0.3f),
    outlineVariant = TextSecondaryDark.copy(alpha = 0.15f),
)

@Composable
fun InventoryAppTheme(
    darkTheme: Boolean = false, // Can also use isSystemInDarkTheme()
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
