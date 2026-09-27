package com.eva.evolutionaryai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val EvaDarkColors = darkColorScheme(
    primary = EvaAccent,
    onPrimary = EvaBackground,
    secondary = EvaAccentDim,
    background = EvaBackground,
    surface = EvaSurface,
    onBackground = EvaTextPrimary,
    onSurface = EvaTextPrimary,
    error = EvaError
)

@Composable
fun EvaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // EVA's interface is always the futuristic dark theme regardless of
    // system setting, by design (section 14 of the spec).
    MaterialTheme(
        colorScheme = EvaDarkColors,
        typography = EvaTypography,
        content = content
    )
}
