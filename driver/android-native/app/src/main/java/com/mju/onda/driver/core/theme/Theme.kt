package com.mju.onda.driver.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = OndaColors.Primary,
    onPrimary = OndaColors.TextOnPrimary,
    secondary = OndaColors.Accent,
    background = OndaColors.Background,
    surface = OndaColors.Surface,
    onBackground = OndaColors.TextPrimary,
    onSurface = OndaColors.TextPrimary,
    error = OndaColors.Error,
    outline = OndaColors.Border,
)

@Composable
fun OndaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = OndaTypography,
        content = content,
    )
}
